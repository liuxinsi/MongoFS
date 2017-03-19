# [Experiment]MongoFS
Expose MongoDB GridFS as files

## Inspiration
* [fuse-jna](https://github.com/EtiennePerot/fuse-jna)
* [Java-FUSE-Mirror-File-System](https://github.com/Syed-Rahman-Mashwani/Java-FUSE-Mirror-File-System)

## Usage
```
usage: MongoFS
 -d,--mongo.db <arg>    database name
 -h                     help
 -l,--mongo.url <arg>   server:port to connect to。e.g. 127.0.0.1:27004
 -m,--mountPath <arg>   mount path
 -p,--mongo.pwd         password for mongodb authentication
 -u,--mongo.user        username for mongodb authentication
```

#### Run
```
java -jar mongofs.jar -m /mongfs_dir -l mongodb:27017 -d test2
```
When it's completed,do something in mongfs_dir,it will map to mongodb grid file system。


#### OR
```
docker-compose up
```
Some problem with docker,I try to figure out。