//
//  PeerManager.h
//
//  Created by Mihail Gutan on 12/11/15.
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
#include "BRPeerManager.h"

#ifndef BREADWALLET_PEERMANAGER_H
#define BREADWALLET_PEERMANAGER_H

#ifdef __cplusplus
extern "C" {
#endif

extern BRPeerManager *_peerManager;

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_create(JNIEnv *env, jobject thiz,
                                                 int earliestKeyTime,
                                                 int blocksCount, int peersCount);

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_rescan(JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env, jobject thiz,
                                                   jbyteArray block, int blockHeight);

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_createBlockArrayWithCount(JNIEnv *env,
                                                                    jobject thiz,
                                                                    size_t blockCount);

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_putPeer(JNIEnv *env, jobject thiz,
                                                  jbyteArray peerAddress,
                                                  jbyteArray peerPort,
                                                  jbyteArray peerTimeStamp);

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_createPeerArrayWithCount(JNIEnv *env,
                                                                   jobject thiz,
                                                                   size_t peerCount);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_isCreated(JNIEnv *env,
                                                                               jobject obj);

JNIEXPORT jdouble JNICALL
Java_com_breadwallet_wallet_BRPeerManager_syncProgress(JNIEnv *env, jobject thiz,
                                                       int startHeight);

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRPeerManager_getCurrentBlockHeight(JNIEnv *env,
                                                                                       jobject thiz);

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRPeerManager_getEstimatedBlockHeight(
        JNIEnv *env, jobject thiz);

JNIEXPORT jlong JNICALL Java_com_breadwallet_wallet_BRPeerManager_getLastBlockTimestamp(
        JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_peerManagerFreeEverything(
        JNIEnv *env, jobject thiz);

JNIEXPORT void JNICALL Java_com_breadwallet_presenter_activities_IntroActivity_testCore(JNIEnv *env,
                                                                                        jobject instance);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_isConnected(JNIEnv *env,
                                                                                 jobject obj);

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRPeerManager_getRelayCount(JNIEnv *env,
                                                                               jobject thiz,
                                                                               jbyteArray txHash);

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_setFixedPeer(
        JNIEnv *env, jobject thiz, jstring node, jint port);

JNIEXPORT jstring JNICALL Java_com_breadwallet_wallet_BRPeerManager_getCurrentPeerName(
        JNIEnv *env, jobject thiz);

#ifdef __cplusplus
}
#endif

#endif //BREADWALLET_PEERMANAGER_H
