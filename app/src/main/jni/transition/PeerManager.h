//
// Created by Mihail Gutan on 12/11/15.
//
#include "jni.h"
#include "BRInt.h"

#ifndef BREADWALLET_PEERMANAGER_H


#define BREADWALLET_PEERMANAGER_H

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz,
                                                                 jlong earliestKeyTime,
                                                                 jlong blocksCount,
                                                                 jlong peersCount);

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env,
                                                                        jobject thiz,
                                                                        jbyteArray block);

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createBlockArrayWithCount(
        JNIEnv *env,
        jobject thiz,
        size_t bkCount);

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putPeer(JNIEnv *env,
                                                                       jobject thiz,
                                                                       jbyteArray peerAddress,
                                                                       jbyteArray peerPort,
                                                                       jbyteArray peerTimeStamp);

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createPeerArrayWithCount(JNIEnv *env,
                                                                                        jobject thiz,
                                                                                        size_t prCount);


#endif //BREADWALLET_PEERMANAGER_H
