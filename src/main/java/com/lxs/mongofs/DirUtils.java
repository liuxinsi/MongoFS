package com.lxs.mongofs;

/**
 * @author liuxinsi
 * @mail akalxs@gmail.com
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

    public static String getFileName(String fileName) {
        if (fileName.equals("/")) {
            return fileName;
        }

        if (fileName.endsWith("/")) {
            fileName = fileName.substring(0, fileName.lastIndexOf("/"));
        }
        return fileName.substring(fileName.lastIndexOf("/") + 1);
    }
}
