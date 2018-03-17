//
// Created by Mihail Gutan on 10/11/16.
//

#include <jni.h>
#include <stdint.h>
#include <BRBase58.h>
#include "JNIBase58.h"

JNIEXPORT jstring JNICALL Java_com_jniwrappers_BRBase58_base58Encode(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data) {
    jbyte *byteData = (*env)->GetByteArrayElements(env, data, 0);
    size_t dataLen = (size_t) (*env)->GetArrayLength(env, data);

    size_t base58len = BRBase58Encode(NULL, 0, (const uint8_t *) byteData, dataLen);
    char base58string[base58len];
    BRBase58Encode(base58string, base58len, (const uint8_t *) byteData, dataLen);
    return (*env)->NewStringUTF(env, base58string);
}
