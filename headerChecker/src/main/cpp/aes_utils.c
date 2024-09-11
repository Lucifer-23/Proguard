#include <stdio.h>
#include "aes.h"
#include "aes_utils.h"
#include "hex_utils.h"

#define BLOCKLEN 16

static const unsigned char HEX[16] = {0x10, 0x01, 0x02, 0x03, 0x04, 0x05, 0x06, 0x07,
                                      0x08, 0x09, 0x0a, 0x0b, 0x0c, 0x0d, 0x0e, 0x0f};

static inline uint8_t *getPaddingInput(const char *input) {
    int inLength = (int) strlen(input); // 输入的长度
    int remainder = inLength % BLOCKLEN;
    uint8_t *paddingInput = NULL;
    int group = inLength / BLOCKLEN;
    size_t size = (size_t) (BLOCKLEN * (group + 1));

    paddingInput = (uint8_t *) malloc(size + 1);

    int dif = (int) size - inLength;
    for (int i = 0; i < size; i++) {
        if (i < inLength) {
            paddingInput[i] = (uint8_t) input[i];
        } else {
            if (remainder == 0) {
                // 刚好是16倍数, 就填充16个16 (0x10)
                paddingInput[i] = HEX[0];
            } else {
                // 如果不足16位 少多少位就补几个几
                paddingInput[i] = HEX[dif];
            }
        }
    }
    paddingInput[size] = '\0';
    return paddingInput;
}


static inline int findPaddingIndex(uint8_t *str, size_t length) {
    int i, k;
    for (i = 0; i < length; i++) {
        char c = str[i];
        if ('\0' != c) {
            for (k = 0; k < 16; ++k) {
                if (HEX[k] == c) {
                    return i;
                }
            }
        }
    }
    return i;
}

static inline void removePadding(uint8_t *out, size_t length) {
    int index = findPaddingIndex(out, length);

    if (index < length) {
        memset(out + index, '\0', length - index);
    }
}


/**
 * AES加密, CBC, PKCS5Padding
 */
char *AES_128_CBC_PKCS5_Encrypt(const char *input) {
    const uint8_t *AES_KEY = getKey();
    const uint8_t *AES_IV = getIV();

    uint8_t *paddingInput = getPaddingInput(input);
    size_t paddingInputLength = strlen((char *) paddingInput);
    unsigned char *out = (unsigned char *) malloc(paddingInputLength);
    AES_CBC_encrypt_buffer(paddingInput, out, (uint32_t) paddingInputLength, AES_KEY, AES_IV);
    char *hexEn = hex_encode(out, paddingInputLength);

    free(paddingInput);
    free(out);
    free((void *) AES_KEY);
    free((void *) AES_IV);

    return hexEn;
}