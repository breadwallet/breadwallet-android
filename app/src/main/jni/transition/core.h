//
// Created by Mihail Gutan on 9/24/15.
//
#include "jni.h"

#ifndef BREADWALLET_CORE_H
#define BREADWALLET_CORE_H

//JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodePhrase
//        (JNIEnv *env, jobject obj, jbyteArray seed, jbyteArray wordList);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_tools_security_RequestHandler_validateAddress
        (JNIEnv *env, jobject obj, jstring address);

jbyteArray Java_com_breadwallet_tools_security_RequestHandler_parsePaymentRequest
        (JNIEnv *env, jobject obj, jbyteArray payment);

jbyteArray Java_com_breadwallet_tools_security_RequestHandler_getCertificatesFromPaymentRequest
        (JNIEnv *env, jobject obj, jbyteArray payment, jint index);

JNIEXPORT void JNICALL Java_com_breadwallet_presenter_activities_MainActivity_clearCMemory(
        JNIEnv *env, jobject obj);

JNIEXPORT void JNICALL Java_com_breadwallet_presenter_activities_MainActivity_cTests(JNIEnv *env,
                                                                                     jobject obj);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_tools_security_RequestHandler_validateAddress
        (JNIEnv *env, jobject obj, jstring address);


#endif //BREADWALLET_CORE_H