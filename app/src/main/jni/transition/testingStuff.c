#include <jni.h>
#include "stdio.h"
#include "string.h"

//#define DEBUG_TAG "NDK_AndroidNDK1SampleActivity"
JNIEXPORT jstring Java_com_breadwallet_presenter_activities_MainActivity_messageFromNativeCode
        (JNIEnv *env, jobject this, jstring logThis) {
    const char *nativeString = (*env)->GetStringUTFChars(env, logThis, 0);
    return (*env)->NewStringUTF(env, nativeString);
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_wallet
        (JNIEnv *env, jobject obj) {
    //test
    struct wallet {
        int account_number;
        char *first_name;
        char *last_name;
        float balance;
    };
    struct wallet wallet1;
    wallet1.account_number = 123;
    wallet1.balance = 52415.21;
    wallet1.first_name = "Mihail";
    wallet1.last_name = "Gutan";
    size_t wallet_size = sizeof(wallet1);

    char raw_wallet[wallet_size];
    memcpy(raw_wallet, &wallet1, wallet_size);

    jbyteArray arr = (*env)->NewByteArray(env, wallet_size);
    (*env)->SetByteArrayRegion(env, arr, 0, wallet_size, raw_wallet);
    return arr;
}

