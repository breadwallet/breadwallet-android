//
// Created by Mihail Gutan on 12/4/15.
//

#ifndef BREADWALLET_WALLET_H
#define BREADWALLET_WALLET_H

#include "jni.h"
#include "BRPeerManager.h"
//#include "WalletCallbacks.h"
#include "BRInt.h"
#include "BRBIP39Mnemonic.h"
#include <android/log.h>
#include "BRWallet.h"
#include "BRBIP32Sequence.h"
#include "BRTransaction.h"

BRWallet *wallet;
BRMasterPubKey pubKey;

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodeSeed(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jbyteArray seed,
                                                                            jobjectArray stringArray);

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_createWallet(JNIEnv *env,
                                                                              jobject thiz,
                                                                              jobjectArray transactions,
                                                                              size_t transactions_count);

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jstring phrase);

const void *theSeed(void *info, const char *authPrompt, uint64_t amount, size_t *seedLen);

#endif //BREADWALLET_WALLET_H


