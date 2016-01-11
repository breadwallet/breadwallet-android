//
// Created by Mihail Gutan on 12/4/15.
//

#include "wallet.h"
#include "BRPeerManager.h"
//#include "WalletCallbacks.h"
#include "BRBIP39Mnemonic.h"
#include <android/log.h>
#include "BRBIP32Sequence.h"
#include "BRTransaction.h"

static JavaVM *jvm;
BRWallet *wallet;
static BRMasterPubKey pubKey;
static BRTransaction **transactions;
static size_t transactionsCounter = 0;

static void balanceChanged(void *info, uint64_t balance) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                        "balanceChanged: %d", balance);
    JNIEnv *globalEnv;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "balanceChanged");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onBalanceChanged", "(J)V");
//    uint64_t walletBalance = BRWalletBalance(wallet);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
//                        "BRWalletBalance(wallet): %d", BRWalletBalance(wallet));
    //call java methods
    (*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

static void txAdded(void *info, BRTransaction *tx) {
    JNIEnv *globalEnv;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txAdded");
    __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ",
                        ">>>>>>>>>>>>tx->version before: %d", tx->version);

    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxAdded", "([BJJ)V");
    //call java methods
    uint8_t buf[BRTransactionSerialize(tx, NULL, 0)];
    size_t len = BRTransactionSerialize(tx, buf, sizeof(buf));

//    int i =0;
//    __android_log_print(ANDROID_LOG_ERROR, "START OF BYTE PRINTING","");
//    while (i < len)
//    {
//        __android_log_print(ANDROID_LOG_ERROR, "byte: ", "%02X", (int)buf[i]);
//        i++;
//    }
//    __android_log_print(ANDROID_LOG_ERROR, "END OF BYTE PRINTING","");

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "blockHeight: %d, timestamp: %d bytes: %d",
                        tx->blockHeight, tx->timestamp, len);
    jbyteArray result = (*globalEnv)->NewByteArray(globalEnv, len);
    (*globalEnv)->SetByteArrayRegion(globalEnv, result, 0, len, (jbyte *) buf);
    (*globalEnv)->CallVoidMethod(globalEnv, entity, mid, result, (jlong) tx->blockHeight, (jlong) tx->timestamp);
}

static void txUpdated(void *info, const UInt256 txHashes[], size_t count, uint32_t blockHeight,
                      uint32_t timestamp) {
    JNIEnv *globalEnv;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txUpdated");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxUpdated", "([B)V");
    //call java methods
    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

static void txDeleted(void *info, UInt256 txHash) {
    JNIEnv *globalEnv;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txDeleted");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxDeleted", "([B)V");
    //call java methods
    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

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

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_createWallet(JNIEnv *env,
                                                                        jobject thiz,
                                                                        size_t txCount) {
    jint rs = (*env)->GetJavaVM(env, &jvm);
    if (rs != JNI_OK){
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, GetJavaVM is not JNI_OK");
    }
    int pubKeySize = sizeof(pubKey);
    if(pubKeySize < 10) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, pubKey is corrupt!");
        return;
    }
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txCount: %d", txCount);
    if (txCount > 0) {
        wallet = BRWalletNew(transactions, txCount, pubKey, NULL, theSeed);
    } else {
        wallet = BRWalletNew(NULL, 0, pubKey, NULL, theSeed);
    }
    BRWalletSetCallbacks(wallet, NULL, balanceChanged, txAdded, txUpdated, txDeleted);

    //create class
    jclass clazz = (*env)->FindClass(env, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*env)->AllocObject(env, clazz);
    jmethodID mid = (*env)->GetMethodID(env, clazz, "onBalanceChanged", "(J)V");
//    uint64_t walletBalance = BRWalletBalance(wallet);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
//                        "BRWalletBalance(wallet): %d", BRWalletBalance(wallet));
    //call java methods
    (*env)->CallVoidMethod(env, entity, mid, BRWalletBalance(wallet));

    if (transactions) free(transactions);

//    __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ", "2");
//
//    if (txCount > 0) {
//        BRTransaction *txs[transactions_count];
//        for (int i = 0; i < transactions_count; i++) {
//            jobject txObject = (*env)->GetObjectArrayElement(env, transactions, i);
//            jbyte *buffTx = (*env)->GetDirectBufferAddress(env, txObject);
//            size_t txLength = (*env)->GetDirectBufferCapacity(env,txObject);
////            int i =0;
////            __android_log_print(ANDROID_LOG_ERROR, "START OF BYTE PRINTING","");
////            char *test = buffTx;
////            while (i < txLength) {
////                __android_log_print(ANDROID_LOG_ERROR, "byte: ", "%02X", (int) ( test[i]));
////                i++;
////            }
////            __android_log_print(ANDROID_LOG_ERROR, "END OF BYTE PRINTING","");
//            BRTransaction *tmpTx = BRTransactionParse((uint8_t*) buffTx, txLength);
//            __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ",
//                                "tmpTx.length: %d", txLength);
//            txs[i] = tmpTx;
//            __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ",
//                                "tmpTx->version SHOULD NOT WORK: %d", txs[i]->version);
//
//        }
//        int txLength = (*env)->GetArrayLength(env, transaction);
//        jbyte *byteTx = (*env)->GetByteArrayElements(env, transaction, 0);
//        BRTransaction *tmpTx = BRTransactionParse(byteTx, txLength);
//
//        txs[0] = tmpTx;
//        __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ", "2");
//        wallet = BRWalletNew(txs, transactions_count, pubKey, NULL, theSeed);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ", "3");
//    } else {
//        wallet = BRWalletNew(NULL, 0, pubKey, NULL, theSeed);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ", "4");
//    }
//    BRWalletSetCallbacks(wallet, NULL, balanceChanged, txAdded, txUpdated, txDeleted);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ", "5");
//
//    size_t seedSize;
//    theSeed(NULL, NULL, 50, &seedSize);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "After calling the fucking theSeed function");
//    __android_log_print(ANDROID_LOG_ERROR, "Wallet created! ", "wallet balance : %d",
//                        BRWalletBalance(wallet));
//    size_t walletSize = sizeof(*wallet);
//    jbyteArray result = (*env)->NewByteArray(env, walletSize);
//
//    (*env)->SetByteArrayRegion(env, result, 0, walletSize, (jbyte *) wallet);
//    return result;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jstring phrase) {
    char *rawPhrase = (*env)->GetStringUTFChars(env, phrase, 0);
    UInt512 key = UINT512_ZERO;
    BRBIP39DeriveKey(key.u8, rawPhrase, NULL);
    pubKey = BRBIP32MasterPubKey(key.u8, sizeof(key));
    size_t pubKeySize = sizeof(pubKey);
    jbyteArray result = (*env)->NewByteArray(env, pubKeySize);
    (*env)->SetByteArrayRegion(env, result, 0, pubKeySize, (jbyte *) &pubKey);

    (*env)->ReleaseStringUTFChars(env, phrase, rawPhrase);
    return result;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_putTransaction(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jbyteArray transaction) {
    int txLength = (*env)->GetArrayLength(env, transaction);
    jbyte *byteTx = (*env)->GetByteArrayElements(env, transaction, 0);
    BRTransaction *tmpTx = BRTransactionParse(byteTx, txLength);

    transactions[transactionsCounter++] = tmpTx;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_createTxArrayWithCount(JNIEnv *env,
                                                                                jobject thiz,
                                                                                size_t txCount){
    transactions = calloc(txCount, sizeof(*transactions));

    // need to call free(transactions);
}

const void *theSeed(void *info, const char *authPrompt, uint64_t amount, size_t *seedLen) {
    JNIEnv *env;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &env, NULL);

    jclass clazz = (*env)->FindClass(env, "com/breadwallet/tools/security/KeyStoreManager");
    jmethodID midGetSeed = (*env)->GetStaticMethodID(env, clazz, "getSeed", "()Ljava/lang/String;");
    //call java methods
    jstring jStringSeed = (jstring) (*env)->CallStaticObjectMethod(env, clazz, midGetSeed);
    if(!jStringSeed) return NULL;
    const char *rawString = (*env)->GetStringUTFChars(env, jStringSeed, 0);

    size_t size = sizeof(rawString);
    *seedLen = size;
    return rawString;
}
