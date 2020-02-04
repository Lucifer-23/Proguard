package com.kun.brother.proguard.utils;

import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.SecretKeySpec;

public class AES {

    /**
     * 相关产品对应的类别常量
     * */
    // 沙盒产品
    public static final String PRODUCT_SANDBOX      = "SandBox";
    // 茄子产品
    public static final String PRODUCT_EGGPLANT     = "Eggplant";

    /**
     * 相关产品对应的密码常量
     * */
    // SandBox
    public static final String PWD_PRODUCT_SANDBOD      = "C9h1Cwk7NgOt6J25";
    // Eggplant
    public static final String PWD_PRODUCT_EGGPLANT     = "Tc3pH76AnvrmemkB";

    // 填充方式
    private static final String algorithmStr = "AES/ECB/PKCS5Padding";

    private static Cipher encryptCipher;
    private static Cipher decryptCipher;

    public static void init(String password) {
        try {
            // 生成一个实现指定转换的Cipher对象。
            encryptCipher = Cipher.getInstance(algorithmStr);
            decryptCipher = Cipher.getInstance(algorithmStr);
            byte[] keyStr = password.getBytes();
            SecretKeySpec key = new SecretKeySpec(keyStr, "AES");
            encryptCipher.init(Cipher.ENCRYPT_MODE, key);
            decryptCipher.init(Cipher.DECRYPT_MODE, key);
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
    }

    public static byte[] encrypt(byte[] content) {
        try {
            byte[] result = encryptCipher.doFinal(content);
            return result;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static byte[] decrypt(byte[] content) {
        try {
            byte[] result = decryptCipher.doFinal(content);
            return result;
        } catch (IllegalBlockSizeException e) {
            e.printStackTrace();
        } catch (BadPaddingException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static String getPwd(String product) {
        String pwd = "";
        switch (product) {
            case PRODUCT_SANDBOX:
                pwd = PWD_PRODUCT_SANDBOD;
                break;
            case PRODUCT_EGGPLANT:
                pwd = PWD_PRODUCT_EGGPLANT;
                break;
        }
        return pwd;
    }
}