//
// Created by Mihail Gutan on 10/9/16.
//

#include <jni.h>
#include <BRInt.h>
#include <BRKey.h>
#include <android/log.h>
#include "JNIKey.h"

static BRKey _key;

JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRKey_compactSign(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data){
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "compactSign: %s", u256_hex_encode(_key.secret));
    jbyte *byteData = (*env)->GetByteArrayElements(env, data, 0);

    uint8_t *uintData = (uint8_t *) byteData;
    UInt256 md32 = UInt256Get(uintData);

    size_t sigLen = BRKeyCompactSign(&_key, NULL, 0, md32);
    uint8_t compactSig[sigLen];
    sigLen = BRKeyCompactSign(&_key, compactSig, sizeof(compactSig), md32);

    jbyteArray result = (*env)->NewByteArray(env, (jsize) sigLen);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) sigLen, (const jbyte *) compactSig);
    return result;
}
JNIEXPORT void JNICALL Java_com_jniwrappers_BRKey_setPrivKey(
        JNIEnv *env,
        jobject thiz,
        jbyteArray privKey){

    jbyte *bytePrivKey = (*env)->GetByteArrayElements(env, privKey, 0);
    int res = BRKeySetPrivKey(&_key, (const char *) bytePrivKey);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "setPrivKey: %s", u256_hex_encode(_key.secret));
}
