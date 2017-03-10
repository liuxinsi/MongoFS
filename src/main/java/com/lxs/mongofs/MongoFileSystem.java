package com.lxs.mongofs;

import com.mongodb.gridfs.GridFSDBFile;
import net.fusejna.DirectoryFiller;
import net.fusejna.ErrorCodes;
import net.fusejna.FuseFilesystem;
import net.fusejna.StructFuseFileInfo;
import net.fusejna.StructStat;
import net.fusejna.types.TypeMode;
import net.fusejna.util.FuseFilesystemAdapterFull;
import org.apache.commons.io.IOUtils;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * @author liuxinsi
 */
public class MongoFileSystem extends FuseFilesystemAdapterFull {
    private MongoAccessor mongoAccessor;
    private Map<String, String> fileIndex = new HashMap<>();
    private Set<String> dirIndex = new HashSet<>();
    private Map<String, List<byte[]>> fileWriteBuf = new HashMap<>();

    public MongoFileSystem(MongoAccessor mongoAccessor) {
        this.mongoAccessor = mongoAccessor;

        // root path
        dirIndex.add("/");
    }

    @Override
    public int access(String path, int access) {
        return 0;
    }

    @Override
    public int create(String path, TypeMode.ModeWrapper mode, StructFuseFileInfo.FileInfoWrapper info) {
        String id = mongoAccessor.save(new byte[0], path);
        fileIndex.put(path, id);
        mode.setMode(TypeMode.NodeType.FILE, true, true, true);
        return 0;
    }

    @Override
    public int getattr(String path, StructStat.StatWrapper stat) {
        if (fileIndex.containsKey(path)) {
            GridFSDBFile dbFile = mongoAccessor.get(path);
            stat.setMode(TypeMode.NodeType.FILE).size(dbFile.getLength());
            return 0;
        } else if (dirIndex.contains(path)) {
            stat.setMode(TypeMode.NodeType.DIRECTORY);
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int mkdir(String path, TypeMode.ModeWrapper mode) {
        dirIndex.add(path);
        return 0;
    }

    @Override
    public int open(final String path, final StructFuseFileInfo.FileInfoWrapper info) {
        return 0;
    }

    @Override
    public int read(String path, ByteBuffer buffer, long size, long offset, StructFuseFileInfo.FileInfoWrapper info) {
        if (fileIndex.containsKey(path)) {
            GridFSDBFile fsdbFile = mongoAccessor.get(path);

            try {
                byte[] data = IOUtils.toByteArray(fsdbFile.getInputStream());
                if ((offset + size) > data.length) {
                    buffer.put(data, (int) offset, data.length - (int) offset);
                    return (int) size;
                } else {
                    buffer.put(data, (int) offset, (int) size);
                    return (int) size;
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            return 0;
        } else if (dirIndex.contains(path)) {
            return -ErrorCodes.EISDIR();
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int readdir(String path, DirectoryFiller filler) {
        if (fileIndex.containsKey(path)) {
            return -ErrorCodes.ENOTDIR();
        } else if (dirIndex.contains(path)) {
            Set<String> filtedDirSet = dirIndex
                    .stream()
                    .filter(s -> {
                        String dir = DirUtils.getDir(s);
                        return dir.equals(path);
                    }).collect(Collectors.toSet());

            filler.add(filtedDirSet);
            filler.add(mongoAccessor.loadFiles(path));
            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int rename(String path, String newName) {
        if (fileIndex.containsKey(path)) {
            if (fileIndex.containsKey(newName)) {
                mongoAccessor.delete(fileIndex.get(newName));
            }

            mongoAccessor.updateFileName(fileIndex.get(path), newName);
            String id = fileIndex.remove(path);
            fileIndex.put(newName, id);
            return 0;
        } else if (dirIndex.contains(path)) {
            dirIndex.remove(path);
            dirIndex.add(newName);
            return 0;
        }
        return -ErrorCodes.ENOTDIR();
    }

    @Override
    public int rmdir(final String path) {
        if (fileIndex.containsKey(path)) {
            return -ErrorCodes.ENOTDIR();
        } else if (dirIndex.contains(path)) {
            dirIndex.remove(path);


            return 0;
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int truncate(String path, long offset) {
        if (fileIndex.containsKey(path)) {
            GridFSDBFile fsdbFile = mongoAccessor.get(path);
            ByteBuffer newContents = ByteBuffer.allocate((int) offset);
            byte[] bytesRead = new byte[(int) offset];
            try {
                try (InputStream in = fsdbFile.getInputStream()) {
                    in.read(bytesRead);
                    newContents.put(bytesRead);
                }
            } catch (IOException e) {
                e.printStackTrace();
            }

            mongoAccessor.delete(fileIndex.get(path));
            mongoAccessor.save(newContents.array(), path);
            return 0;
        } else if (dirIndex.contains(path)) {
            return -ErrorCodes.EISDIR();
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int unlink(String path) {
        if (fileIndex.containsKey(path)) {
            mongoAccessor.delete(fileIndex.get(path));
            fileIndex.remove(path);
        } else if (dirIndex.contains(path)) {
            dirIndex.remove(path);
        }
        return 0;
    }

    @Override
    public int write(String path, ByteBuffer buf, long bufSize, long writeOffset,
                     StructFuseFileInfo.FileInfoWrapper wrapper) {
        if (fileIndex.containsKey(path)) {
            //cache write bytes
            byte[] b = new byte[(int) bufSize];
            buf.get(b);

            if (fileWriteBuf.containsKey(path)) {
                fileWriteBuf.get(path).add(b);
            } else {
                List<byte[]> bytes = new ArrayList<>();
                bytes.add(b);
                fileWriteBuf.put(path, bytes);
            }
            return (int) bufSize;

            // get file from mongodb
//            GridFSDBFile fsdbFile = mongoAccessor.get(path);
//            ByteBuffer bb = null;
//            try {
//                try (InputStream in = fsdbFile.getInputStream()) {
//                    bb = ByteBuffer.wrap(IOUtils.toByteArray(in));
//                }
//            } catch (IOException e) {
//                // todo logging this shit
//                e.printStackTrace();
//            }
//
//            // new buf
//            int maxWriteIndex = (int) (writeOffset + bufSize);
//            byte[] bytesToWrite = new byte[(int) bufSize];
//            if (maxWriteIndex > bb.capacity()) {
//                final ByteBuffer newContents = ByteBuffer.allocate(maxWriteIndex);
//                newContents.put(bb);
//                bb = newContents;
//            }
//            buf.get(bytesToWrite, 0, (int) bufSize);
//            bb.position((int) writeOffset);
//            bb.put(bytesToWrite);
//            bb.position(0);
//
//            // delete old file
//            mongoAccessor.delete(fileIndex.get(path));
//            // replace it
//            String id = mongoAccessor.save(bb.array(), path);
//            fileIndex.put(path, id);
//            return (int) bufSize;
        } else if (dirIndex.contains(path)) {
            return -ErrorCodes.EISDIR();
        }
        return -ErrorCodes.ENOENT();
    }

    @Override
    public int release(String path, StructFuseFileInfo.FileInfoWrapper info) {
        if (!fileWriteBuf.containsKey(path)) {
            return super.release(path, info);

        }

        byte[] data = new byte[0];
        try {
            try (ByteArrayOutputStream bao = new ByteArrayOutputStream()) {
                List<byte[]> byteList = fileWriteBuf.remove(path);
                for (byte[] bytes : byteList) {
                    bao.write(bytes);
                }
                data = bao.toByteArray();
            }
        } catch (IOException e) {
            // todo logging this shit
            e.printStackTrace();
        }

        // delete old file
        mongoAccessor.delete(fileIndex.get(path));
        // replace it
        String id = mongoAccessor.save(data, path);
        fileIndex.put(path, id);
        return 0;
    }

    public FuseFilesystem log() {
        return super.log(true);
    }
}
