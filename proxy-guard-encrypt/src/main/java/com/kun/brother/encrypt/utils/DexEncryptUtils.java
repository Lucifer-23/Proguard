package com.kun.brother.encrypt.utils;

import java.io.File;

public class DexEncryptUtils {

    static {
//        System.setProperty("java.library.path", ".");
        File file = new File("./");
        System.out.println("path: " + file.getAbsolutePath());
        System.loadLibrary("kun_brother_encrypt");
    }

    public static native byte[] encrypt(byte[] data);
}