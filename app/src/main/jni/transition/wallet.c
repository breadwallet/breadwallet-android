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
    char *wordList[wordsCount];
    for (int i = 0; i < wordsCount; i++) {
        jstring string = (jstring) (*env)->GetObjectArrayElement(env, stringArray, i);
        const char *rawString = (*env)->GetStringUTFChars(env, string, 0);
        wordList[i] = rawString;
        (*env)->ReleaseStringUTFChars(env, string, rawString);
        (*env)->DeleteLocalRef(env,string);
        // Don't forget to call `ReleaseStringUTFChars` when you're done.
    }
    jbyte *byteSeed = (*env)->GetByteArrayElements(env, seed, 0);
    char *theSeed = byteSeed;
    int size_needed = BRBIP39Encode(NULL, 0, wordList, theSeed, seedLength);
    char *result;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Need to print : %s", wordList[1]);
//    int buf = BRBIP39Encode(result, size_needed, wordList, theSeed, seedLength);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Need to print : %d", buf);
    jstring stringPhrase = (*env)->NewStringUTF(env, result);
    return stringPhrase;
}


