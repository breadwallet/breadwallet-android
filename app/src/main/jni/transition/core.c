//
// Created by Mihail Gutan on 9/24/15.
//
#include "core.h"
#include "../breadwallet-core/BRPaymentProtocol.h"

JNIEXPORT void Java_com_breadwallet_presenter_activities_MainActivity_sendMethodCallBack
        (JNIEnv *env, jobject obj) {
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid = (*env)->GetMethodID(env, cls, "callback", "()V");
    if (mid == 0)
        return;
    (*env)->CallVoidMethod(env, obj, mid);
};

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodePhrase
        (JNIEnv *env, jobject obj, jbyteArray seed, jbyteArray wordList) {
//
//    jboolean b;
//    int wordLen = (*env)->GetArrayLength(env, wordList);
//    int seedLen = (*env)->GetArrayLength(env, seed);
//    char *buff[wordLen];
//    char seed_buff[seedLen];
//
//    char *phrase[seedLen]; // wrong ! check later
//
//    jbyte *byte1 = (*env)->GetByteArrayElements(env, wordList, &b);
//    buff = (char *) byte1;
//    (*env)->ReleaseByteArrayElements(env, wordList, b, 0);
//
//
//    jbyte *seed_byte = (*env)->GetByteArrayElements(env, wordList, &b);
//    seed_buff = seed_byte;
//    (*env)->ReleaseByteArrayElements(env, seed, b, JNI_ABORT);
//
//    size_t byte_size = BRBIP39Encode(phrase,) continue later

}

JNIEXPORT jbyteArray Java_com_breadwallet_tools_AddressReader_getCertificatesFromPaymentRequest
        (JNIEnv *env, jobject obj, jbyteArray certs) {

    int certsLength = (*env)->GetArrayLength(env, certs);
    char *buff[certsLength];

//    BRPaymentProtocolRequestCert //TODO fix this shit please when the implementation is ready!

}