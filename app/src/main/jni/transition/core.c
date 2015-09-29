//
// Created by Mihail Gutan on 9/24/15.
//
#include "stdio.h"
#include "core.h"
JNIEXPORT void Java_com_breadwallet_presenter_activities_MainActivity_sendMethodCallBack
        (JNIEnv *env, jobject obj){
    jclass cls = (*env)->GetObjectClass(env, obj);
    jmethodID mid = (*env)->GetMethodID(env, cls, "callback", "()V");
    if (mid == 0)
        return;
    (*env)->CallVoidMethod(env, obj, mid);
};

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodePhrase
        (JNIEnv *env, jobject obj, jbyteArray seed){

}