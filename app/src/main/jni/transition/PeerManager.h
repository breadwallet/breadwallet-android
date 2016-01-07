//
// Created by Mihail Gutan on 12/11/15.
//
#include "jni.h"
#ifndef BREADWALLET_PEERMANAGER_H


#define BREADWALLET_PEERMANAGER_H

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz,
                                                                       jbyteArray walletBuff,
                                                                       jlong earliestKeyTime,
                                                                       jobjectArray blocks,
                                                                       jlong blocksCount,
                                                                       jobjectArray peers,
                                                                       jlong peersCount);


#endif //BREADWALLET_PEERMANAGER_H
