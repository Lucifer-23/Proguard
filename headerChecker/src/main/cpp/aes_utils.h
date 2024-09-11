#ifndef AESTEST_AES_UTILS_H
#define AESTEST_AES_UTILS_H

#include <stdint.h>
#include <string.h>
#include <stdlib.h>
#include "junk.h"

#define AES_128_CBC_PKCS5_Encrypt  ll11l1l1ll
#define getKey                     ll11lll1l1
#define getIV                      ll11l1l1l1
#define getPaddingInput            ll11l1l11l
#define findPaddingIndex           lll1l1l1l1
#define removePadding              ll11l1llll

#ifdef __cplusplus
extern "C" {
#endif

/** AES加密, CBC, PKCS5Padding */
char *AES_128_CBC_PKCS5_Encrypt(const char *input);

// key: goodl-aes-key123
static const uint8_t *getKey() {
    _JUNK_FUN_0
    const int len = 16;
    uint8_t *src = (uint8_t *) malloc(len + 1);

    for (int i = 0; i < len; ++i) {
        switch (i) {
            case 0:  src[i] = 'g'; break;
            case 1:  src[i] = 'o'; break;
            case 2:  src[i] = 'o'; _JUNK_FUN_1 break;
            case 3:  src[i] = 'd'; break;
            case 4:  src[i] = 'l'; break;
            case 5:  src[i] = '-'; break;
            case 6:  src[i] = 'a'; break;
            case 7:  src[i] = 'e'; break;
            case 8:  src[i] = 's'; break;
            case 9:  src[i] = '-'; _JUNK_FUN_2 break;
            case 10: src[i] = 'k'; break;
            case 11: src[i] = 'e'; break;
            case 12: src[i] = 'y'; break;
            case 13: src[i] = '1'; break;
            case 14: src[i] = '2'; break;
            case 15: src[i] = '3'; break;
        }
    }
    src[len] = '\0';
    _JUNK_FUN_1
    return src;
}

// iv: goodl-aes-iv1234
static const uint8_t *getIV() {
    const int len = 16;
    _JUNK_FUN_2
    uint8_t *src = (uint8_t *) malloc(len + 1);

    for (int i = 0; i < len; ++i) {
        switch (i) {
            case 0:  src[i] = 'g'; _JUNK_FUN_0 break;
            case 1:  src[i] = 'o'; break;
            case 2:  src[i] = 'o'; break;
            case 3:  src[i] = 'd'; break;
            case 4:  src[i] = 'l'; break;
            case 5:  src[i] = '-'; break;
            case 6:  src[i] = 'a'; break;
            case 7:  src[i] = 'e'; break;
            case 8:  src[i] = 's'; break;
            case 9:  src[i] = '-'; break;
            case 10: src[i] = 'i'; break;
            case 11: src[i] = 'v'; _JUNK_FUN_3 break;
            case 12: src[i] = '1'; break;
            case 13: src[i] = '2'; break;
            case 14: src[i] = '3'; break;
            case 15: src[i] = '4'; break;
        }
    }
    src[len] = '\0';
    _JUNK_FUN_2
    return src;
}

#ifdef __cplusplus
}
#endif

#endif //AESTEST_AES_UTILS_H