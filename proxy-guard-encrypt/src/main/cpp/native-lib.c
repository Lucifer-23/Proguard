#include <jni.h>
#include <stdio.h>
#include <android/log.h>
#include <malloc.h>
#include <string.h>
#include <openssl/evp.h>
#include <openssl/aes.h>

// 密钥
static uint8_t *userkey = "C9h1Cwk7NgOt6J25";

#define LOGI(...) __android_log_print(ANDROID_LOG_INFO, "NDK", __VA_ARGS__)

JNIEXPORT jbyteArray JNICALL
Java_com_kun_brother_encrypt_utils_DexEncryptUtils_encrypt(JNIEnv *env, jobject instance,
                                                 jbyteArray encrypt_) {
    jbyte *src = (*env)->GetByteArrayElements(env, encrypt_, NULL);
    int src_len = (*env)->GetArrayLength(env, encrypt_);

    // 加密
    // 加解密的上下文
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    int outlen = src_len + AES_BLOCK_SIZE - src_len % AES_BLOCK_SIZE;;
    // 初始化上下文设置解码参数
    EVP_EncryptInit_ex(ctx, EVP_aes_128_ecb(), NULL, userkey, NULL);

    // 密文比明文长，所以肯定能保存下所有的明文
    uint8_t *out = malloc(src_len);
    // 数据置空
    memset(out, 0, src_len);
    int len;
    // 加密
    EVP_EncryptUpdate(ctx, out, &outlen, src, src_len);
    len = outlen;
    // 解密剩余的所有数据校验
    EVP_EncryptFinal_ex(ctx, out + outlen, &outlen);
    len += outlen;
    EVP_CIPHER_CTX_free(ctx);

    // 返回加密后的byte数组
    jbyteArray byte_array_result = (*env)->NewByteArray(env, outlen);
    (*env)->SetByteArrayRegion(env, byte_array_result, 0, outlen, out);

    // 释放资源
    free(out);
    (*env)->ReleaseByteArrayElements(env, encrypt_, src, 0);

    return byte_array_result;
}