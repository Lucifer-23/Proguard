package com.kun.brother.proguard;

import com.kun.brother.proguard.utils.AES;
import com.kun.brother.proguard.utils.Zip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.FilenameFilter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.RandomAccessFile;

public class Main {

    private static String product = AES.PRODUCT_SANDBOX;

    public static void main(String[] args) throws Exception {
        /**
         * 1、制作只包含解密代码的dex文件
         */
        File classesDex = jar2dex();

        AES.init(AES.getPwd(product));
        deleteFile(new File("proxy-guard-tools/apk/" + product));

        File targetApkRootDir = new File("proxy-guard-tools/target/" + product);
        File[] productFlavorDirs = targetApkRootDir.listFiles();
        for (File productFlavor : productFlavorDirs) {
            File productFlavorRelease = new File(productFlavor.getAbsolutePath(), "release");
            if (productFlavorRelease.exists()) {
                File[] outputs = productFlavorRelease.listFiles();
                for (File file : outputs) {
                    if (file.getName().endsWith("apk")) {
                        String flavor = productFlavor.getName();
                        File apkTemp = new File("proxy-guard-tools/apk/" + product + File.separator + flavor + "/temp");
                        String newApkOriginPath = "proxy-guard-tools/apk/"
                                + product
                                + File.separator
                                + flavor
                                + File.separator
                                + file.getName();

                        /**
                         * 2、加密目标apk中所有dex文件
                         */
                        dexProguard(file, apkTemp);

                        /**
                         * 3、把classes.dex放入apk解压目录再压缩成apk
                         */
                        File unSignedApk = repackApk(classesDex, apkTemp, newApkOriginPath);

                        /**
                         * 4、对齐与签名
                         */
                        resignApk(flavor, newApkOriginPath, unSignedApk);
                    }
                }
            }
        }
    }

    private static File jar2dex() throws Exception {
        // 1.1 解压aar获得classes.jar
        File aarFile = new File("proxy-guard-tools/dependencies/" + product + "/proxy-guard-core-release.aar");
        File aarTemp = new File("proxy-guard-tools/build/" + product + "/proxy-guard-tools-temp");
        Zip.unZip(aarFile, aarTemp);

        // 1.2 执行dx命令将jar变成dex文件
        //     windows:cmd /c linux/mac不需要(md /c)
        File classesJar = new File(aarTemp, "classes.jar");
        File classesDex = new File(aarTemp, "classes.dex");

        Process process = Runtime.getRuntime()
                .exec("cmd /c dx --dex --output "
                        + classesDex.getAbsolutePath()
                        + " "
                        + classesJar.getAbsolutePath());
        process.waitFor();

        // 失败
        if (process.exitValue() != 0) {
            throw new RuntimeException("dex error: " + process.exitValue());
        }

        return classesDex;
    }

    private static void dexProguard(File originApk,
                                    File apkTemp) throws Exception {
        // 2.1 解压apk获得所有的dex文件
        Zip.unZip(originApk, apkTemp);

        // 获得所有的dex
        File[] dexFiles = apkTemp.listFiles(new FilenameFilter() {
            @Override
            public boolean accept(File file, String s) {
                return s.endsWith(".dex");
            }
        });

        for (File dex : dexFiles) {
            // 读取文件数据
            byte[] bytes = getBytes(dex);
            // 加密
            byte[] encrypt = AES.encrypt(bytes);
            // 写到指定目录
            FileOutputStream fos =
                    new FileOutputStream(new File(apkTemp, "kun_brother-" + dex.getName()));
            fos.write(encrypt);
            fos.flush();
            fos.close();
            dex.delete();
        }
    }

    private static File repackApk(File classesDex,
                                  File apkTemp,
                                  String newApkOriginPath) throws Exception {
        /**
         * 3、把classes.dex放入apk解压目录在压缩成apk
         */
        // classesDex.renameTo(new File(apkTemp, "classes.dex"));
        copy(classesDex, new File(apkTemp, "classes.dex"));
        File unSignedApk = new File(newApkOriginPath.replace(".apk",
                "_unsigned.apk"));
        Zip.zip(apkTemp, unSignedApk);

        return unSignedApk;
    }

    private static void resignApk(String productFlavor,
                                  String newApkOriginPath,
                                  File unSignedApk) throws Exception {
        // 4.1 对齐
        // 26.0.2不认识-p参数zipalign -v -p 4 my-app-unsigned.apk my-app-unsigned-aligned.apk

        File alignedApk = new File(newApkOriginPath.replace(".apk",
                "_unsigned_aligned.apk"));
        Process process = Runtime.getRuntime().exec("cmd /c zipalign -f 4 "
                + unSignedApk.getAbsolutePath()
                + " "
                + alignedApk.getAbsolutePath());
        process.waitFor();
        // 失败
        if (process.exitValue() != 0) {
            throw new RuntimeException("zipalign error");
        }

        // 4.2 签名
        // apksigner sign --ks jks文件地址 --ks-key-alias 别名 --ks-pass pass:jsk密码
        // --key-pass pass:别名密码 --out out.apk in.apk

        File signedConfig = new File("proxy-guard-tools/dependencies/" + product + "/configs/config" + productFlavor + ".gradle");
        BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(signedConfig)));
        String keyAlias = "";
        String ksPass = "";
        String keyPass = "";
        String line = "";
        while ((line = br.readLine()) != null) {
            if (line.contains("KEY_ALIAS")) {
                keyAlias = fetchValue(line);
            } else if (line.contains("STORE_PASSWORD")) {
                ksPass = fetchValue(line);
            } else if (line.contains("KEY_PASSWORD")) {
                keyPass = fetchValue(line);
            }
        }

        File signedApk = new File(newApkOriginPath.replace(".apk",
                "_signed_aligned.apk"));
        File jks = new File("proxy-guard-tools/dependencies/" + product + "/configs/sign/" + productFlavor + ".jks");

        process = Runtime.getRuntime().exec("cmd /c apksigner sign --ks "
                + jks.getAbsolutePath()
                + " --ks-key-alias "
                + keyAlias
                + " --ks-pass pass:"
                + ksPass
                + " --key-pass pass:"
                + keyPass
                + " --out "
                + signedApk.getAbsolutePath()
                + " "
                + alignedApk.getAbsolutePath());

        process.waitFor();

        // 失败
        if (process.exitValue() != 0) {
            throw new RuntimeException("apksigner error");
        }
    }

    public static byte[] getBytes(File file) throws Exception {
        RandomAccessFile r = new RandomAccessFile(file, "r");
        byte[] buffer = new byte[(int) r.length()];
        r.readFully(buffer);
        r.close();
        return buffer;
    }

    private static void deleteFile(File file) {
        if (file.isDirectory()) {
            File[] files = file.listFiles();
            for (File f : files) {
                deleteFile(f);
            }
            file.delete();
        } else {
            file.delete();
        }
    }

    private static String fetchValue(String keyValue) {
        return keyValue.substring(keyValue.indexOf("'") + 1, keyValue.length() - 1);
    }

    public static void copy(File target, File destination) throws IOException {
        InputStream input = null;
        OutputStream output = null;
        try {
            input = new FileInputStream(target);
            output = new FileOutputStream(destination);
            byte[] buf = new byte[1024];
            int bytesRead;
            while ((bytesRead = input.read(buf)) != -1) {
                output.write(buf, 0, bytesRead);
            }
        } finally {
            input.close();
            output.close();
        }
    }
}