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

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRTestWallet_initWallet(JNIEnv *env, jobject thiz);
JNIEXPORT jstring Java_com_breadwallet_wallet_BRTestWallet_encodeSeed(JNIEnv *env, jobject thiz,
               jbyteArray seed, jobjectArray stringArray);

#endif //BREADWALLET_WALLET_H


