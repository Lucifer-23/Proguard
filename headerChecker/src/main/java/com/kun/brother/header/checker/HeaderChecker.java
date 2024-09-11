package com.kun.brother.header.checker;

public class HeaderChecker {
    static {
        System.loadLibrary("headerCheckerLib");
    }

    public static native String checkNull(String datas, Object object);
}