//
// Created by Mihail Gutan on 12/11/15.
//
#include "jni.h"
#include "BRInt.h"
#include "BRPeerManager.h"

#ifndef BREADWALLET_PEERMANAGER_H
#define BREADWALLET_PEERMANAGER_H

extern BRPeerManager *_peerManager;

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createAndConnect(JNIEnv *env, jobject thiz,
                                                                          jint earliestKeyTime,
                                                                          int blocksCount,
                                                                          int peersCount);

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz);

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_rescan(JNIEnv *env, jobject thiz);

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env,
                                                                  jobject thiz,
                                                                  jbyteArray block,
                                                                  int blockHeight);

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

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_isCreated
        (JNIEnv *env, jobject obj);

JNIEXPORT jdouble Java_com_breadwallet_wallet_BRPeerManager_syncProgress(JNIEnv *env,
                                                                         jobject thiz);

JNIEXPORT jint Java_com_breadwallet_wallet_BRPeerManager_getCurrentBlockHeight(JNIEnv *env,
                                                                               jobject thiz);

JNIEXPORT jint Java_com_breadwallet_wallet_BRPeerManager_getEstimatedBlockHeight(JNIEnv *env,
                                                                                 jobject thiz);


#endif //BREADWALLET_PEERMANAGER_H
