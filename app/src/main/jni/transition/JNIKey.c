//
// Created by Mihail Gutan on 10/9/16.
//

#include <jni.h>
#include <BRInt.h>
#include <BRKey.h>
#include "JNIKey.h"

JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRKey_compactSign(
        JNIEnv *env,
        jobject thiz,
        jbyteArray privKey, jbyteArray data){

    jbyte *bytePrivKey = (*env)->GetByteArrayElements(env, privKey, 0);
    jbyte *byteData = (*env)->GetByteArrayElements(env, data, 0);
    BRKey key;
    int res = BRKeySetPrivKey(&key, (const char *) bytePrivKey);
    uint8_t *uintData = (uint8_t *) byteData;
    UInt256 md32 = UInt256Get(uintData);

    size_t sigLen = BRKeyCompactSign(&key, NULL, 0, md32);
    uint8_t compactSig[sigLen];
    sigLen = BRKeyCompactSign(&key, compactSig, sizeof(compactSig), md32);

    jbyteArray result = (*env)->NewByteArray(env, sigLen);
    (*env)->SetByteArrayRegion(env, result, 0, sigLen, (const jbyte *) compactSig);
    return result;
}
