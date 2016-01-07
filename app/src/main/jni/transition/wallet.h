//
// Created by Mihail Gutan on 12/4/15.
//
#include "jni.h"
#include "BRInt.h"
#include "BRWallet.h"
#ifndef BREADWALLET_WALLET_H
#define BREADWALLET_WALLET_H


extern BRWallet *wallet;

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


