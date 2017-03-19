package com.lxs.mongofs;

import com.mongodb.BasicDBObject;
import com.mongodb.DB;
import com.mongodb.DBCollection;
import com.mongodb.MongoClient;
import com.mongodb.gridfs.GridFS;
import com.mongodb.gridfs.GridFSDBFile;
import com.mongodb.gridfs.GridFSInputFile;
import org.bson.types.ObjectId;

import java.util.List;
import java.util.stream.Collectors;

/**
 * @author liuxinsi
 * @mail akalxs@gmail.com
 */
public class MongoAccessor {
    private MongoClient mongoClient;
    private String dbName;
    private DB db;
    private final Object lock = new Object();

    public MongoAccessor(MongoClient mongoClient, String dbName) {
        this.mongoClient = mongoClient;
        this.dbName = dbName;
    }

    private DB getDatabase() {
        if (db == null) {
            synchronized (lock) {
                if (db == null) {
                    this.db = new DB(mongoClient, dbName);
                }
            }
        }
        return db;
    }

    public String save(byte[] data, String fileName) {
        GridFS fs = new GridFS(getDatabase());
        GridFSInputFile inputFile = fs.createFile(data);
        inputFile.setFilename(fileName);
        inputFile.setMetaData(new BasicDBObject("dir", DirUtils.getDir(fileName)));
        inputFile.getMetaData().put("fuse", true);
        inputFile.save();
        return inputFile.getId().toString();
    }

    public List<String> loadFileNames(String dir) {
        GridFS fs = new GridFS(getDatabase());

        BasicDBObject query = new BasicDBObject();
        query.put("metadata.fuse", true);
        query.put("metadata.dir", dir);
        return fs.find(query)
                .stream()
                .map(gridFSDBFile -> DirUtils.getFileName(gridFSDBFile.getFilename()))
                .collect(Collectors.toList());
    }

    public GridFSDBFile get(String fileName) {
        GridFS fs = new GridFS(getDatabase());
        return fs.findOne(fileName);
    }

    public void delete(String id) {
        GridFS fs = new GridFS(getDatabase());
        fs.remove(new ObjectId(id));
    }

    public void updateFileName(String id, String newName) {
        DBCollection coll = getDatabase().getCollection("fs.files");
        BasicDBObject query = new BasicDBObject("_id", new ObjectId(id));

        coll.update(query, new BasicDBObject("$set", new BasicDBObject("filename", newName)));

        String dir = DirUtils.getDir(newName);
        BasicDBObject db = new BasicDBObject(new BasicDBObject("metadata.dir", dir));
        coll.update(query, new BasicDBObject("$set", db));
    }
}
