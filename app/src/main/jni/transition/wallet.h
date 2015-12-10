//
// Created by Mihail Gutan on 12/4/15.
//

#ifndef BREADWALLET_WALLET_H
#define BREADWALLET_WALLET_H

#include "jni.h"
#include "BRPeerManager.h"
#include "WalletCallbacks.h"
#include "BRBIP39Mnemonic.h"
#include <android/log.h>
#include "BRWallet.h"
#include "BRBIP32Sequence.h"

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRTestWallet_initWallet(JNIEnv *env, jobject thiz);
JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRTestWallet_encodeSeed(JNIEnv *env, jobject thiz,
               jbyteArray seed, jobjectArray stringArray);
JNIEXPORT jboolean Java_com_breadwallet_wallet_BRTestWallet_createWallet(JNIEnv *env, jobject thiz,
                jobject buffer);
JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRTestWallet_getMasterPubKey(JNIEnv *env, jobject thiz,
                jstring phrase);


#endif //BREADWALLET_WALLET_H


