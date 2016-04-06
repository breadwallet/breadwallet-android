//
// Created by Mihail Gutan on 12/4/15.
//
//extern int BITCOIN_TESTNET = 1;
#include "jni.h"
#include "BRInt.h"
#include "BRWallet.h"

#ifndef BREADWALLET_WALLET_H
#define BREADWALLET_WALLET_H


extern BRWallet *_wallet;

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodeSeed(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jbyteArray seed,
                                                                            jobjectArray stringArray);

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_createWallet(JNIEnv *env,
                                                                        jobject thiz,
                                                                        size_t txCount,
                                                                        jbyteArray bytePubKey,
                                                                        int r);

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jstring phrase);

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_putTransaction(JNIEnv *env,
                                                                                jobject thiz,
                                                                                jbyteArray transaction,
                                                                                jlong blockHeight,
                                                                                jlong timeStamp);


JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_createTxArrayWithCount(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  int txCount);
JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_validateAddress
        (JNIEnv *env, jobject obj, jstring address);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_addressContainedInWallet
        (JNIEnv *env, jobject obj, jstring address);

JNIEXPORT jlong JNICALL Java_com_breadwallet_wallet_BRWalletManager_getMinOutputAmount
        (JNIEnv *env, jobject obj);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_addressIsUsed
        (JNIEnv *env, jobject obj, jstring address);

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRWalletManager_feeForTransaction
        (JNIEnv *env, jobject obj, jstring address, jlong amount);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_isCreated
        (JNIEnv *env, jobject obj);

JNIEXPORT jstring Java_com_breadwallet_wallet_BRWalletManager_getReceiveAddress(JNIEnv *env,
                                                                                jobject thiz);

JNIEXPORT jobjectArray Java_com_breadwallet_wallet_BRWalletManager_getTransactions(JNIEnv *env,
                                                                                   jobject thiz);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_tryTransaction
        (JNIEnv *env, jobject obj, jstring address, jlong amount);

JNIEXPORT jboolean Java_com_breadwallet_wallet_BRWalletManager_pay(JNIEnv *env, jobject thiz,
                                                                   jstring address, jlong amount,
                                                                   jstring strSeed);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_transactionIsVerified
        (JNIEnv *env, jobject obj, jstring txHash);

JNIEXPORT jlong JNICALL Java_com_breadwallet_wallet_BRWalletManager_getMaxOutputAmount
        (JNIEnv *env, jobject obj);

const void *theSeed(void *info, const char *authPrompt, uint64_t amount, size_t *seedLen);

#endif //BREADWALLET_WALLET_H


