package com.missile.codec.demo.util;

import android.os.Environment;

import java.io.File;


public class StorageUtil {
    public static String getDirName() {
        return "SCamera";
    }

    public static String getSDPath() {
        // 判断是否挂载
        if (Environment.getExternalStorageState().equals(Environment.MEDIA_MOUNTED)) {
            return Environment.getExternalStorageDirectory().getAbsolutePath();
        }
        return Environment.getRootDirectory().getAbsolutePath();
    }

    public static String getImagePath() {
        return getSDPath() + "/" + getDirName() + "/image/";
    }

    public static String getVideoPath() {
        return getSDPath() + /*"/" + getDirName() +*/ "/video/";
    }

    public static boolean checkDirExist(String path) {
        File mDir = new File(path);
        if (!mDir.exists()) {
            return mDir.mkdirs();
        }
        return true;
    }
}
