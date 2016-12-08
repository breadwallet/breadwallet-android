//
// Created by Mihail Gutan on 10/9/16.
//

#include <jni.h>
#include <BRInt.h>
#include <BRKey.h>
#include <android/log.h>
#include <BRCrypto.h>
#include "JNIKey.h"

static BRKey _key;

JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRKey_compactSign(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "compactSign, _key: %s", _key.secret);
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
        jbyteArray privKey) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "key is not set yet, _key: %s", _key.secret);

    jbyte *bytePrivKey = (*env)->GetByteArrayElements(env, privKey, 0);
    int res = BRKeySetPrivKey(&_key, (const char *) bytePrivKey);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "key is set, _key: %s", _key.secret);
}

JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRKey_encryptNative(JNIEnv *env, jobject thiz,
                                                                      jbyteArray data,
                                                                      jbyteArray nonce) {
    jbyte *byteData = (*env)->GetByteArrayElements(env, data, NULL);
    jbyte *byteNonce = (*env)->GetByteArrayElements(env, nonce, NULL);
    jsize dataSize = (*env)->GetArrayLength(env, data);
    jsize nonceSize = (*env)->GetArrayLength(env, nonce);

    uint8_t out[16 + dataSize];

    size_t outSize = BRChacha20Poly1305AEADEncrypt(out, sizeof(out), &_key, (uint8_t *) byteNonce,
                                                   (uint8_t *) byteData, (size_t) dataSize, NULL, 0);

    jbyteArray result = (*env)->NewByteArray(env, (jsize) outSize);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) outSize, (const jbyte *) out);

    (*env)->ReleaseByteArrayElements(env, data, byteData, 0);
    (*env)->ReleaseByteArrayElements(env, nonce, byteNonce, 0);
    return result;
}

JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRKey_decryptNative(JNIEnv *env, jobject thiz,
                                                                      jbyteArray data,
                                                                      jbyteArray nonce) {
    jbyte *byteData = (*env)->GetByteArrayElements(env, data, NULL);
    jbyte *byteNonce = (*env)->GetByteArrayElements(env, nonce, NULL);
    jsize dataSize = (*env)->GetArrayLength(env, data);
    jsize nonceSize = (*env)->GetArrayLength(env, nonce);

    uint8_t out[dataSize];

    size_t outSize = BRChacha20Poly1305AEADDecrypt(out, sizeof(out), &_key, (uint8_t *) byteNonce, (uint8_t *) byteData,
                                                   (size_t) (dataSize), NULL, 0);
    jbyteArray result = (*env)->NewByteArray(env, (jsize) outSize);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) outSize, (const jbyte *) out);

    (*env)->ReleaseByteArrayElements(env, data, byteData, 0);
    (*env)->ReleaseByteArrayElements(env, nonce, byteNonce, 0);
    return result;
}

//// chacha20-poly1305 authenticated encryption with associated data (AEAD): https://tools.ietf.org/html/rfc7539
//size_t BRChacha20Poly1305AEADEncrypt(void *out, size_t outLen, const void *key32, const void *nonce12,
//                                     const void *data, size_t dataLen, const void *ad, size_t adLen);
//
//size_t BRChacha20Poly1305AEADDecrypt(void *out, size_t outLen, const void *key32, const void *nonce12,
//                                     const void *data, size_t dataLen, const void *ad, size_t adLen);
