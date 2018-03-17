//
// Created by Mihail Gutan on 10/9/16.
//

#include <jni.h>
#include <BRInt.h>
#include <BRKey.h>
#include <android/log.h>
#include <BRCrypto.h>
#include <BRAddress.h>
#include <assert.h>
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

JNIEXPORT jboolean JNICALL Java_com_jniwrappers_BRKey_setPrivKey(
        JNIEnv *env,
        jobject thiz,
        jbyteArray privKey) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "key is not set yet, _key: %s", _key.secret);

    jbyte *bytePrivKey = (*env)->GetByteArrayElements(env, privKey, 0);
    int res = BRKeySetPrivKey(&_key, (const char *) bytePrivKey);
    if (res)
        return JNI_TRUE;
    else
        return JNI_FALSE;
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "key is set, _key: %s", _key.secret);
}

JNIEXPORT void JNICALL Java_com_jniwrappers_BRKey_setSecret(
        JNIEnv *env,
        jobject thiz,
        jbyteArray privKey) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "key is not set yet, _key: %s", _key.secret);

    jbyte *bytePrivKey = (*env)->GetByteArrayElements(env, privKey, 0);
    int res = BRKeySetSecret(&_key, (const UInt256 *) bytePrivKey, 1);

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "key is set, res: %d", res);
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
                                                   (uint8_t *) byteData, (size_t) dataSize, NULL,
                                                   0);

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

    size_t outSize = BRChacha20Poly1305AEADDecrypt(out, sizeof(out), &_key, (uint8_t *) byteNonce,
                                                   (uint8_t *) byteData,
                                                   (size_t) (dataSize), NULL, 0);
    if (sizeof(out) == 0) return NULL;
    jbyteArray result = (*env)->NewByteArray(env, (jsize) outSize);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) outSize, (const jbyte *) out);

    (*env)->ReleaseByteArrayElements(env, data, byteData, 0);
    (*env)->ReleaseByteArrayElements(env, nonce, byteNonce, 0);
    return result;
}


JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRKey_address(JNIEnv *env, jobject thiz) {
    BRAddress address = BR_ADDRESS_NONE;
    BRKeyAddress(&_key, address.s, sizeof(address));
    assert(address.s[0] != '\0');
    return (*env)->NewStringUTF(env, address.s);
}
