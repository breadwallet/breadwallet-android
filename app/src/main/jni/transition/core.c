#include <jni.h>
#include "breadwallet-core/BRPaymentProtocol.h"
#include <android/log.h>

//
// Created by Mihail Gutan on 9/24/15.
//

JNIEXPORT jobject Java_com_breadwallet_tools_security_RequestHandler_parsePaymentRequest
        (JNIEnv *env, jobject obj, jbyteArray payment) {

    BRPaymentProtocolRequest *nativeRequest;
    int requestLength = (*env)->GetArrayLength(env, payment);
    jbyte* bytePayment = (*env)->GetByteArrayElements(env, payment, 0);
    nativeRequest = BRPaymentProtocolRequestParse(bytePayment, requestLength);

//    jbyteArray result = buf1;
//    jbyte* bytesResult = (jbyte*)nativeRequest;
//    int size = sizeof(nativeRequest);
//    jbyteArray result = (*env)->NewByteArray(env, size);
//    __android_log_write(ANDROID_LOG_ERROR, ">>>>>>MESSAGE FROM C: ", nativeRequest->pkiType);
//    (*env)->SetByteArrayRegion(env,result, 0, size, bytesResult);
//    (*env)->ReleaseByteArrayElements(env, payment, bytePayment, JNI_ABORT);
//    (*env)->ReleaseByteArrayElements(env, result, bytesResult, JNI_ABORT);
//    return result;

    jbyte* bytesResult = (jbyte*) nativeRequest->signature;
    int size = sizeof(nativeRequest->signature);
    jbyteArray result = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env,result, 0, size, bytesResult);

    jclass clazz = (*env)->FindClass(env, "com/breadwallet/tools/security/PaymentRequestEntity");
    jobject entity = (*env)->AllocObject(env,clazz);
    jfieldID pkiType = (*env)->GetFieldID(env,clazz, "pkiType", "Ljava/lang/String;");
    jmethodID mid = (*env)->GetMethodID(env, clazz, "byteSignature", "([B)V");

    (*env)->SetObjectField(env,entity, pkiType, (*env)->NewStringUTF(env, nativeRequest->pkiType));

    (*env)->CallVoidMethod(env, entity, mid, result);
    (*env)->ReleaseByteArrayElements(env, payment, bytePayment, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, result, bytesResult, JNI_ABORT);
    BRPaymentProtocolRequestFree(nativeRequest);

    return entity;
}

JNIEXPORT jobject JNICALL Java_com_breadwallet_tools_security_RequestHandler_getCertificatesFromPaymentRequest
        (JNIEnv *env, jobject obj, jbyteArray payment, jint index){
    //create the BRPaymentProtocolRequest
    BRPaymentProtocolRequest *nativeRequest;
    int requestLength = (*env)->GetArrayLength(env, payment);
    jbyte* bytePayment = (*env)->GetByteArrayElements(env, payment, 0);
    nativeRequest = BRPaymentProtocolRequestParse(bytePayment, requestLength);
    //get certificate
    uint8_t buf[BRPaymentProtocolRequestCert(nativeRequest, NULL, 0, index)];
    BRPaymentProtocolRequestCert(nativeRequest, buf, sizeof(buf), index);
    //convert it to jbyteArray
    jbyte* certJbyte = (jbyte*)buf;
    int size = sizeof(buf);
    jbyteArray result = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env,result, 0, size, certJbyte);
    //release everything
    (*env)->ReleaseByteArrayElements(env, result, certJbyte, JNI_ABORT);
    (*env)->ReleaseByteArrayElements(env, payment, bytePayment, JNI_ABORT);
    BRPaymentProtocolRequestFree(nativeRequest);

    return result;
}

//JNIEXPORT void Java_com_breadwallet_presenter_activities_MainActivity_sendMethodCallBack
//        (JNIEnv *env, jobject obj) {
//    jclass cls = (*env)->GetObjectClass(env, obj);
//    jmethodID mid = (*env)->GetMethodID(env, cls, "callback", "()V");
//    if (mid == 0)
//        return;
//    (*env)->CallVoidMethod(env, obj, mid);
//}

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
