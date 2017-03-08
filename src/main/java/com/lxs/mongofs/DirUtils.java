package com.lxs.mongofs;

/**
 * @author liuxinsi
 */
public final class DirUtils {
    public static String getDir(String fileName) {
        if (fileName.equals("/")) {
            return fileName;
        }

        if (fileName.endsWith("/")) {
            fileName = fileName.substring(0, fileName.lastIndexOf("/"));
        }
        String dir = fileName.substring(0, fileName.lastIndexOf("/"));
        return "".equals(dir) ? "/" : dir;
    }
}
