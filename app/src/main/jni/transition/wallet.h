//  wallet.h
//
//  Created by Mihail Gutan on 12/4/15.
//  Copyright (c) 2015 breadwallet LLC
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

#include "jni.h"
#include "BRInt.h"
#include "BRWallet.h"

#ifndef BREADWALLET_WALLET_H
#define BREADWALLET_WALLET_H

extern BRWallet *_wallet;
extern jclass _walletManagerClass;

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodeSeed(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jbyteArray seed,
                                                                            jobjectArray stringArray);

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_createWallet(JNIEnv *env,
                                                                        jobject thiz,
                                                                        size_t txCount,
                                                                        jbyteArray bytePubKey);

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jstring phrase);

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_putTransaction(JNIEnv *env,
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

JNIEXPORT jdouble JNICALL Java_com_breadwallet_wallet_BRWalletManager_getMinOutputAmount
        (JNIEnv *env, jobject obj) ;

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
                                                                   jstring address,
                                                                   jlong amount,
                                                                   jstring strSeed);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_transactionIsVerified
        (JNIEnv *env, jobject obj, jstring txHash);

JNIEXPORT jlong JNICALL Java_com_breadwallet_wallet_BRWalletManager_getMaxOutputAmount
        (JNIEnv *env, jobject obj);

JNIEXPORT jlong Java_com_breadwallet_wallet_BRWalletManager_localAmount(JNIEnv *env, jobject thiz,
                                                                        jlong amount,
                                                                        double price);

JNIEXPORT jlong Java_com_breadwallet_wallet_BRWalletManager_bitcoinAmount(JNIEnv *env, jobject thiz,
                                                                          jlong localAmount,
                                                                          double price);

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_walletFreeEverything(JNIEnv *env, jobject thiz);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_validateRecoveryPhrase
                                                                        (JNIEnv *env, jobject obj,
                                                                        jobjectArray stringArray,
                                                                        jstring jPhrase);

JNIEXPORT jstring Java_com_breadwallet_wallet_BRWalletManager_getFirstAddress(JNIEnv *env,
                                                                         jobject thiz,
                                                                         jbyteArray bytePubKey);

//const void *theSeed(void *info, const char *authPrompt, uint64_t amount, size_t *seedLen);

#endif //BREADWALLET_WALLET_H


