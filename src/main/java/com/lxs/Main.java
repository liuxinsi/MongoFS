package com.lxs;

import com.lxs.mongofs.MongoAccessor;
import com.lxs.mongofs.MongoFileSystem;
import com.mongodb.MongoClient;
import com.mongodb.MongoCredential;
import com.mongodb.ServerAddress;
import net.fusejna.FuseException;
import org.apache.commons.cli.CommandLine;
import org.apache.commons.cli.CommandLineParser;
import org.apache.commons.cli.DefaultParser;
import org.apache.commons.cli.HelpFormatter;
import org.apache.commons.cli.Options;
import org.apache.commons.cli.ParseException;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * @author liuxinsi
 * @mail akalxs@gmail.com
 */
public class Main {

    public static void main(String[] args) throws FuseException {
        // parse command line
        Options options = new Options()
                .addOption("m", "mountPath", true, "mount path")
                .addOption("l", "mongo.url", true, "server:port to connect to。e.g. 127.0.0.1:27004")
                .addOption("d", "mongo.db", true, "database name")
                .addOption("u", "mongo.user", false, "username for mongodb authentication")
                .addOption("p", "mongo.pwd", false, "password for mongodb authentication")
                .addOption("h", "help");

        CommandLineParser clp = new DefaultParser();
        CommandLine cl = null;
        try {
            cl = clp.parse(options, args);
        } catch (ParseException e) {
            System.err.println(e.getMessage());
            System.exit(-1);
        }

        if (cl.hasOption("h") || !cl.hasOption("m") || !cl.hasOption("l") || !cl.hasOption("d")) {
            new HelpFormatter().printHelp("MongoFS", options);
            return;
        }


        // init mongo client
        ServerAddress mongoAddress = new ServerAddress(cl.getOptionValue("l"));
        String dbName = cl.getOptionValue("d");

        MongoClient mongoClient;
        if (cl.hasOption("u") && cl.hasOption("p")) {
            List<MongoCredential> li = new ArrayList<>(1);
            li.add(MongoCredential.createCredential(cl.getOptionValue("u"), dbName, cl.getOptionValue("p").toCharArray()));
            mongoClient = new MongoClient(mongoAddress, li);
        } else {
            mongoClient = new MongoClient(mongoAddress);
        }
        MongoAccessor ma = new MongoAccessor(mongoClient, dbName);

        // mount
        String mount = cl.getOptionValue("m");
        if (!Files.exists(Paths.get(mount))) {
            try {
                Files.createDirectory(Paths.get(mount));
            } catch (IOException e) {
                System.err.println("create " + mount + " ex:" + e.getMessage());
                e.printStackTrace();
            }
        }
        new MongoFileSystem(ma).log().mount(mount);
    }
}
