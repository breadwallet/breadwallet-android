#include <jni.h>
#include "breadwallet-core/BRPaymentProtocol.h"
//#include <android/log.h>

//
// Created by Mihail Gutan on 9/24/15.
//

JNIEXPORT jbyteArray Java_com_breadwallet_tools_security_RequestHandler_getCertificatesFromPaymentRequest
        (JNIEnv *env, jobject obj, jbyteArray payment, jint index) {

    BRPaymentProtocolRequest *nativeRequest;
    int requestLength = (*env)->GetArrayLength(env, payment);
    jbyte* bytePayment = (*env)->GetByteArrayElements(env, payment, 0);
    nativeRequest = BRPaymentProtocolRequestParse(bytePayment, requestLength);
    BRPaymentProtocolRequestFree(nativeRequest);
//    char buff = bytePayment;
//    nativeRequest = (BRPaymentProtocolRequest*) buff;

//    uint8_t buf1[BRPaymentProtocolRequestCert(nativeRequest, NULL, 0, index)];
//    BRPaymentProtocolRequestCert(nativeRequest, buf1, sizeof(buf1), index);

//    jbyteArray result = buf1;
    jbyte* bytesResult = (jbyte*)nativeRequest;
    int size = sizeof(nativeRequest);
    jbyteArray result = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env,result, 0, size, bytesResult);
    (*env)->ReleaseByteArrayElements(env, payment, bytePayment, JNI_ABORT);
    return result;

}

JNIEXPORT void Java_com_breadwallet_presenter_activities_MainActivity_sendMethodCallBack
        (JNIEnv *env, jobject obj) {
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid = (*env)->GetMethodID(env, cls, "callback", "()V");
    if (mid == 0)
        return;
    (*env)->CallVoidMethod(env, obj, mid);
}

//JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodePhrase
//        (JNIEnv *env, jobject obj, jbyteArray seed, jbyteArray wordList) {
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
//
//}
