//
// Created by Mihail Gutan on 12/4/15.
//

#include "wallet.h"

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRTestWallet_initWallet(JNIEnv *env, jobject thiz){
    //create a new wallet
    setCallbacks(env);
}

JNIEXPORT jstring Java_com_breadwallet_wallet_BRTestWallet_encodeSeed(JNIEnv *env, jobject thiz,
             jbyteArray seed, jobjectArray stringArray){

    int wordsCount = (*env)->GetArrayLength(env, stringArray);
    int seedLength = (*env)->GetArrayLength(env, seed);
    const char *wordList[wordsCount];
    for (int i = 0; i < wordsCount; i++) {
        jstring string = (jstring) (*env)->GetObjectArrayElement(env, stringArray, i);
        char *rawString = (*env)->GetStringUTFChars(env, string, 0);
        wordList[i] = rawString;
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "current string : %s", wordList[i]);
//        (*env)->ReleaseStringUTFChars(env, string, rawString);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "current string : %s", wordList[i]);
        (*env)->DeleteLocalRef(env,string);
        // Don't forget to call `ReleaseStringUTFChars` when you're done.
    }
    jbyte *byteSeed = (*env)->GetByteArrayElements(env, seed, 0);
    char *theSeed = byteSeed;
    char result[BRBIP39Encode(NULL, 0, wordList, theSeed, seedLength)];
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "words number : %s", wordList[83]);
    size_t len = BRBIP39Encode(result, sizeof(result), wordList, theSeed, seedLength);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Need to print : %d", len);
    jstring stringPhrase = (*env)->NewStringUTF(env, result);
    return stringPhrase;
}


