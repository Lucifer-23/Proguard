package com.kun.brother.proguard;

import com.kun.brother.proguard.utils.AES;
import com.kun.brother.proguard.utils.Zip;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.InputStreamReader;
import java.net.Proxy;

public class Signature {

    private static String product = AES.PRODUCT_EGGPLANT;

    public static void main(String[] args) throws Exception {

        File targetApkRootDir = new File("proxy-guard-tools\\target\\" + product);
        File[] productFlavorDirs = targetApkRootDir.listFiles();
        for (File productFlavor : productFlavorDirs) {
            File productFlavorRelease = new File(productFlavor.getAbsolutePath(), "release");
            if (productFlavorRelease.exists()) {
                String flavor = productFlavor.getName();
                File[] outputs = productFlavorRelease.listFiles();
                if (outputs.length > 1) {
                    deleteFile(new File("proxy-guard-tools\\apk\\"
                            + product
                            + File.separator
                            + "signature"
                            + File.separator
                            + flavor));
                }
                for (File file : outputs) {
                    if (file.getName().endsWith("apk")) {
                        String newApkOriginPath = "proxy-guard-tools\\apk\\"
                                + product
                                + File.separator
                                + "signature"
                                + File.separator
                                + flavor
                                + File.separator
                                + file.getName();

                        /**
                         * 4、对齐与签名
                         */
                        resignApk(flavor, newApkOriginPath, file);
                    }
                }
            }
        }
    }

    private static void resignApk(String productFlavor,
                                  String newApkOriginPath,
                                  File unSignedApk) throws Exception {
        // 4.1 对齐
        // 26.0.2不认识-p参数zipalign -v -p 4 my-app-unsigned.apk my-app-unsigned-aligned.apk
        File alignedApk = new File(newApkOriginPath.replace(".apk",
                "_unsigned_aligned.apk"));
        // 创建目录
        if (!alignedApk.getParentFile().exists()) {
            alignedApk.getParentFile().mkdirs();
        }
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
        File signedConfig = new File("proxy-guard-tools\\dependencies\\" + product + "\\configs\\config" + productFlavor + ".gradle");
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

        File signedApk = new File(alignedApk.getAbsolutePath().replace("_unsigned_aligned.apk",
                "_signed_aligned.apk"));

        File jks = new File("proxy-guard-tools\\dependencies\\" + product + "\\configs\\sign\\" + productFlavor + ".jks");

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
}