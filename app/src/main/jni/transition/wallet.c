//
// Created by Mihail Gutan on 12/4/15.
//

#include "wallet.h"

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodeSeed(JNIEnv *env, jobject thiz,
                                                                         jbyteArray seed,
                                                                         jobjectArray stringArray) {

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
        (*env)->DeleteLocalRef(env, string);
        // Don't forget to call `ReleaseStringUTFChars` when you're done.
    }
    jbyte *byteSeed = (*env)->GetByteArrayElements(env, seed, 0);
    char *theSeed = byteSeed;
    char result[BRBIP39Encode(NULL, 0, wordList, theSeed, seedLength)];
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "words number : %s", wordList[83]);
    size_t len = BRBIP39Encode(result, sizeof(result), wordList, theSeed, seedLength);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Need to print : %d", len);
//    jstring stringPhrase = (*env)->NewStringUTF(env, result);
    jbyte *phraseJbyte = (const jbyte *) result;
    int size = sizeof(result);
    jbyteArray bytePhrase = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env, bytePhrase, 0, size, phraseJbyte);
    return bytePhrase;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_createWallet(JNIEnv *env,
                                                                           jobject thiz,
                                                                           jbyteArray buffer) {
    int pubKeyLength = (*env)->GetArrayLength(env, buffer);
    jbyte *pubKeyBytes = (*env)->GetByteArrayElements(env, buffer, 0);
    BRMasterPubKey *pubKey = (BRMasterPubKey*) pubKeyBytes;

//    size_t seedLen = 0;
//    const void *seed = theSeed(NULL, NULL, 0, &seedLen);
//    BRMasterPubKey mpk = BRBIP32MasterPubKey(seed, seedLen);

//    BRTransaction *tx = BRTransactionNew();

//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Before creating the fucking wallet");
    BRWallet *wallet = BRWalletNew(NULL, 0, *pubKey, NULL, theSeed);

    __android_log_print(ANDROID_LOG_ERROR, "Wallet created! ", "wallet balance : %d",
                        BRWalletBalance(wallet));

    size_t walletSize = sizeof(wallet);
    jbyteArray result = (*env)->NewByteArray(env, walletSize);
    (*env)->SetByteArrayRegion(env, result, 0, walletSize, (jbyte *) wallet);
    return result;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jstring phrase) {
    char *rawPhrase = (*env)->GetStringUTFChars(env, phrase, 0);
    UInt512 key = UINT512_ZERO;
    BRBIP39DeriveKey(key.u8, rawPhrase, NULL);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Priv Key : %d",
//                        sizeof(key));
    BRMasterPubKey pubKey = BRBIP32MasterPubKey(key.u8, sizeof(key));
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Pub Key : %d",
//                        sizeof(pubKey.fingerPrint));
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Pub Key : %d",
//                        sizeof(pubKey.chainCode));
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Pub Key : %d",
//                        sizeof(pubKey.pubKey));
    size_t pubKeySize = sizeof(pubKey);
    jbyteArray result = (*env)->NewByteArray(env, pubKeySize);
    (*env)->SetByteArrayRegion(env, result, 0, pubKeySize, (jbyte *) &pubKey);

    (*env)->ReleaseStringUTFChars(env, phrase, rawPhrase);
    return result;
}

const void *theSeed(void *info, const char *authPrompt, uint64_t amount, size_t *seedLen) {
    *seedLen = 0;
    return "";
}







