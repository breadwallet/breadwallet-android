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

#ifdef __cplusplus
extern "C" {
#endif

extern BRWallet *_wallet;
extern jclass _walletManagerClass;

JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_wallet_BRWalletManager_encodeSeed(JNIEnv *env, jobject thiz,
                                                       jbyteArray seed,
                                                       jobjectArray stringArray);

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRWalletManager_createWallet(JNIEnv *env, jobject thiz,
                                                         size_t txCount,
                                                         jbyteArray bytePubKey);

JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env, jobject thiz,
                                                            jbyteArray phrase);

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRWalletManager_putTransaction(JNIEnv *env, jobject thiz,
                                                           jbyteArray transaction,
                                                           jlong blockHeight,
                                                           jlong timeStamp);

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRWalletManager_createTxArrayWithCount(JNIEnv *env,
                                                                   jobject thiz,
                                                                   int txCount);

JNIEXPORT jboolean JNICALL
Java_com_breadwallet_wallet_BRWalletManager_validateAddress(JNIEnv *env, jobject obj,
                                                            jstring address);

JNIEXPORT jboolean JNICALL
Java_com_breadwallet_wallet_BRWalletManager_addressContainedInWallet(JNIEnv *env,
                                                                     jobject obj,
                                                                     jstring address);

JNIEXPORT jlong JNICALL Java_com_breadwallet_wallet_BRWalletManager_getMinOutputAmount(JNIEnv *env,
                                                                                       jobject obj);

JNIEXPORT jlong JNICALL
Java_com_breadwallet_wallet_BRWalletManager_getMinOutputAmountRequested(JNIEnv *env,
                                                                        jobject obj);

JNIEXPORT jboolean JNICALL
Java_com_breadwallet_wallet_BRWalletManager_addressIsUsed(JNIEnv *env, jobject obj,
                                                          jstring address);

JNIEXPORT jint JNICALL
Java_com_breadwallet_wallet_BRWalletManager_feeForTransaction(JNIEnv *env, jobject obj,
                                                              jstring address,
                                                              jlong amount);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_isCreated(JNIEnv *env,
                                                                                 jobject obj);

JNIEXPORT jstring JNICALL Java_com_breadwallet_wallet_BRWalletManager_getReceiveAddress(JNIEnv *env,
                                                                                        jobject thiz);

JNIEXPORT jobjectArray JNICALL Java_com_breadwallet_wallet_BRWalletManager_getTransactions(
        JNIEnv *env, jobject thiz);

JNIEXPORT jobject JNICALL
Java_com_breadwallet_wallet_BRWalletManager_tryTransaction(JNIEnv *env, jobject obj,
                                                           jstring jAddress, jlong jAmount);

JNIEXPORT jboolean JNICALL
Java_com_breadwallet_wallet_BRWalletManager_transactionIsVerified(JNIEnv *env, jobject obj,
                                                                  jstring txHash);

JNIEXPORT jlong JNICALL Java_com_breadwallet_wallet_BRWalletManager_getMaxOutputAmount(JNIEnv *env,
                                                                                       jobject obj);

JNIEXPORT jlong JNICALL
Java_com_breadwallet_wallet_BRWalletManager_localAmount(JNIEnv *env, jobject thiz,
                                                        jlong amount, double price);

JNIEXPORT jlong JNICALL
Java_com_breadwallet_wallet_BRWalletManager_bitcoinAmount(JNIEnv *env, jobject thiz,
                                                          jlong localAmount, double price);

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRWalletManager_walletFreeEverything(JNIEnv *env,
                                                                                        jobject thiz);

JNIEXPORT jboolean JNICALL
Java_com_breadwallet_wallet_BRWalletManager_validateRecoveryPhrase(JNIEnv *env, jobject obj,
                                                                   jobjectArray stringArray,
                                                                   jstring jPhrase);

JNIEXPORT jstring JNICALL
Java_com_breadwallet_wallet_BRWalletManager_getFirstAddress(JNIEnv *env, jobject thiz,
                                                            jbyteArray bytePubKey);

JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_wallet_BRWalletManager_publishSerializedTransaction(JNIEnv *env,
                                                                         jobject thiz,
                                                                         jbyteArray serializedTransaction,
                                                                         jbyteArray phrase);

JNIEXPORT jlong JNICALL Java_com_breadwallet_wallet_BRWalletManager_getTotalSent(JNIEnv *env,
                                                                                 jobject obj);

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRWalletManager_setFeePerKb(JNIEnv *env,
                                                                               jobject obj,
                                                                               jlong fee,
                                                                               jboolean ignore);

JNIEXPORT jboolean JNICALL
Java_com_breadwallet_wallet_BRWalletManager_isValidBitcoinPrivateKey(JNIEnv *env,
                                                                     jobject instance,
                                                                     jstring key);

JNIEXPORT jboolean JNICALL
Java_com_breadwallet_wallet_BRWalletManager_isValidBitcoinBIP38Key(JNIEnv *env,
                                                                   jobject instance,
                                                                   jstring key);

JNIEXPORT jstring JNICALL
Java_com_breadwallet_wallet_BRWalletManager_getAddressFromPrivKey(JNIEnv *env,
                                                                  jobject instance,
                                                                  jstring key);

JNIEXPORT jstring JNICALL
Java_com_breadwallet_wallet_BRWalletManager_decryptBip38Key(JNIEnv *env, jobject instance,
                                                            jstring privKey,
                                                            jstring pass);

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRWalletManager_createInputArray(JNIEnv *env,
                                                                                    jobject thiz);

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRWalletManager_addInputToPrivKeyTx(JNIEnv *env, jobject thiz,
                                                                jbyteArray hash, int vout,
                                                                jbyteArray script,
                                                                jlong amount);

JNIEXPORT jobject  JNICALL Java_com_breadwallet_wallet_BRWalletManager_getPrivKeyObject(JNIEnv *env,
                                                                                        jobject thiz);

JNIEXPORT jboolean JNICALL
Java_com_breadwallet_wallet_BRWalletManager_confirmKeySweep(JNIEnv *env, jobject thiz,
                                                            jbyteArray tx, jstring privKey);

JNIEXPORT jstring JNICALL
Java_com_breadwallet_wallet_BRWalletManager_reverseTxHash(JNIEnv *env, jobject thiz,
                                                          jstring txHash);

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRWalletManager_getTxCount(JNIEnv *env,
                                                                              jobject thiz);

JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_wallet_BRWalletManager_getAuthPrivKeyForAPI(
        JNIEnv *env,
        jobject thiz,
        jbyteArray phrase);

JNIEXPORT jstring JNICALL Java_com_breadwallet_wallet_BRWalletManager_getAuthPublicKeyForAPI(
        JNIEnv *env,
        jobject thiz,
        jbyteArray privkey);

JNIEXPORT jstring JNICALL Java_com_breadwallet_wallet_BRWalletManager_getSeedFromPhrase(
        JNIEnv *env,
        jobject thiz,
        jbyteArray phrase);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRWalletManager_isTestNet(JNIEnv *env,
                                                                                 jobject thiz);

JNIEXPORT jlong JNICALL
Java_com_breadwallet_wallet_BRWalletManager_feeForTransactionAmount(JNIEnv *env, jobject obj,
                                                                    jlong amount);

JNIEXPORT jstring JNICALL
Java_com_breadwallet_wallet_BRWalletManager_txHashSha256Hex(JNIEnv *env, jobject thiz,
                                                            jstring txHash);

JNIEXPORT jlong JNICALL Java_com_breadwallet_wallet_BRWalletManager_getBCashBalance(
        JNIEnv *env,
        jobject thiz,
        jbyteArray bytePubKey);

JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_wallet_BRWalletManager_sweepBCash(JNIEnv *env,
                                                                                    jobject thiz,
                                                                                    jbyteArray bytePubKey,
                                                                                    jstring address,
                                                                                    jbyteArray phrase);

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRWalletManager_getTxSize(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jbyteArray serializedTransaction);

#ifdef __cplusplus
}
#endif

#endif //BREADWALLET_WALLET_H