
package com.jb.zcamera.gallery.util;

import android.util.Log;

import java.io.File;
import java.io.IOException;

/**
 * 目录工具类
 * 
 * @author oujingwen
 */
public class DirectoryUtil {

    private static final String TAG = "DirectoryUtil";

    public static final String NOMEDIA_FILENAME = ".nomedia";

    /**
     * 获取目录，不存在则创建并在该目录下生成.nomedia文件
     * 
     * @param path 目录路径
     * @return 成功则返回{@link File}对象，不成功则返回{@link null}
     */
    public static File getOrMakeNomediaDir(String path) {
        File dir = null;

        try {
            dir = new File(path);

            if (dir.isFile())
                if (!dir.delete())
                    return null;

            if (!dir.exists()) {
                if (!dir.mkdirs())
                    return null;
                File file = new File(path, NOMEDIA_FILENAME);
                try {
                    file.createNewFile();
                } catch (IOException e) {
                    Log.e(TAG, "", e);
                }
            }
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
            return null;
        }

        return dir;
    }

    public static File createNomediaFile(String path) {
        try {
            File dir = new File(path);

            if (dir.isFile())
                    return null;

            if (dir.exists()) {
                File file = new File(path, NOMEDIA_FILENAME);
                file.createNewFile();
                return file;
            }
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
            return null;
        }

        return null;
    }

    /**
     * 获取目录，不存在则创建
     * 
     * @param path 目录路径
     * @return 成功则返回{@link File}对象，不成功则返回{@link null}
     */
    public static File getOrMakeDir(String path) {
        File dir = null;

        try {
            dir = new File(path);

            if (dir.isFile())
                if (!dir.delete())
                    return null;

            if (!dir.exists())
                if (!dir.mkdirs())
                    return null;
        } catch (Throwable tr) {
            Log.e(TAG, "", tr);
            return null;
        }

        return dir;
    }
    
    /**
     * Deletes all files and subdirectories under "dir".
     * @param dir Directory to be deleted
     * @return boolean Returns "true" if all deletions were successful.
     *                 If a deletion fails, the method stops attempting to
     *                 delete and returns "false".
     */
    public static boolean deleteDir(File dir) {

        if (dir.isDirectory()) {
            String[] children = dir.list();
            for (int i=0; i<children.length; i++) {
                boolean success = deleteDir(new File(dir, children[i]));
                if (!success) {
                    return false;
                }
            }
        }

        // The directory is now empty so now it can be smoked
        return dir.delete();
    }
}
