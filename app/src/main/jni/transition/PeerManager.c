//
//  PeerManager.c
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

#include "PeerManager.h"
#include "BRPeer.h"
#include "BRBIP39Mnemonic.h"
#include "BRInt.h"
#include <android/log.h>
#include "BRMerkleBlock.h"
#include "BRWallet.h"
#include "wallet.h"
#include <pthread.h>
#include <BRBase58.h>
#include <stdio.h>
#include <assert.h>
#include <unistd.h>
#include <arpa/inet.h>

#define fprintf(...) __android_log_print(ANDROID_LOG_ERROR, "bread", _va_rest(__VA_ARGS__, NULL))

static BRMerkleBlock **_blocks;
BRPeerManager *_peerManager;
static JavaVM *_jvmPM;
static BRPeer *_peers;
static size_t _blocksCounter = 0;
static size_t _peersCounter = 0;
static jclass _peerManagerClass;
static size_t _managerNewCounter = 0;
static jclass _blockClass;
static jclass _peerClass;

static JNIEnv *getEnv() {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "getEnv peerManager");
    if (!_jvmPM) return NULL;

    JNIEnv *env;
    int status = (*_jvmPM)->GetEnv(_jvmPM, (void **) &env, JNI_VERSION_1_6);

    if (status < 0) {
        status = (*_jvmPM)->AttachCurrentThread(_jvmPM, &env, NULL);
        if (status < 0) return NULL;
    }
    return env;
}

static void syncStarted(void *info) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "syncStarted");
    if (!_peerManager) return;

    JNIEnv *env = getEnv();
    jmethodID mid;

    if (!env) return;

    //call java methods
    mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "syncStarted", "()V");
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid);
}

static void syncStopped(void *info, int error) {
    if (!_peerManager) return;

    JNIEnv *env = getEnv();
    jmethodID mid;

    if (!env) return;
    if (error) {
        mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "syncFailed", "()V");
    } else {
        mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "syncSucceeded", "()V");
    }
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid);
}

//static void syncSucceeded(void *info) {
//    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "syncSucceeded: # of tx: %d",
//                        (int) BRWalletTransactions(_wallet, NULL, 0));
//    if (!_peerManager) return;
//
//    JNIEnv *env = getEnv();
//    jmethodID mid;
//
//    if (!env) return;
//
//    //call java methods
//    mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "syncSucceeded", "()V");
//    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid);
//}
//
//static void syncFailed(void *info, int error) {
//    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "syncFailed");
//    if (!_peerManager) return;
//
//    JNIEnv *env = getEnv();
//    jmethodID mid;
//
//    if (!env) return;
//
//    //call java methods
//    mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "syncFailed", "()V");
//    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid);
//}

static void txStatusUpdate(void *info) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "txStatusUpdate");
    if (!_peerManager) return;

    JNIEnv *env = getEnv();
    jmethodID mid;

    if (!env) {
        return;
    }

    //call java methods
    mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "txStatusUpdate", "()V");
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid);
}

static void saveBlocks(void *info, int replace, BRMerkleBlock *blocks[], size_t count) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "saveBlocks");
    if (!_peerManager) return;

    JNIEnv *env = getEnv();
    jmethodID mid;

    if (!env) return;

//    if (count != 1) {
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "deleting %zu blocks", count);
//        mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "deleteBlocks", "()V");
//        (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid);
//    }

    //call java methods

    //Find the class and populate the array of objects of this class
    jobjectArray blockObjectArray = (*env)->NewObjectArray(env, (jsize) count, _blockClass, 0);

    for (size_t i = 0; i < count; i++) {
        if (!_peerManager) return;

        uint8_t buf[BRMerkleBlockSerialize(blocks[i], NULL, 0)];
        size_t len = BRMerkleBlockSerialize(blocks[i], buf, sizeof(buf));
        jbyteArray result = (*env)->NewByteArray(env, (jsize) len);
        jobject blockObject;

        (*env)->SetByteArrayRegion(env, result, 0, (jsize) len, (jbyte *) buf);
        mid = (*env)->GetMethodID(env, _blockClass, "<init>", "([BI)V");
        blockObject = (*env)->NewObject(env, _blockClass, mid, result, blocks[i]->height);
        (*env)->SetObjectArrayElement(env, blockObjectArray, (jsize) i, blockObject);
        (*env)->DeleteLocalRef(env, result);
        (*env)->DeleteLocalRef(env, blockObject);
    }

    mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "saveBlocks",
                                    "([Lcom/breadwallet/presenter/entities/BlockEntity;Z)V");
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, blockObjectArray, replace ? JNI_TRUE : JNI_FALSE);
}

static void savePeers(void *info, int replace, const BRPeer peers[], size_t count) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "savePeers");
    if (!_peerManager) return;

    JNIEnv *env = getEnv();
    jmethodID mid;

    if (!env) return;

//    //call java methods
//    if (count != 1) {
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "deleting %d peers", count);
//        mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "deletePeers", "()V");
//        (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid);
//    }

    jobjectArray peerObjectArray = (*env)->NewObjectArray(env, (jsize) count, _peerClass, 0);

    for (int i = 0; i < count; i++) {
        if (!_peerManager) return;

        jobject peerObject;
        jbyteArray peerAddress = (*env)->NewByteArray(env, sizeof(peers[i].address));
        jbyteArray peerPort = (*env)->NewByteArray(env, sizeof(peers[i].port));
        jbyteArray peerTimeStamp = (*env)->NewByteArray(env, sizeof(peers[i].timestamp));

        (*env)->SetByteArrayRegion(env, peerAddress, 0, sizeof(peers[i].address),
                                   (jbyte *) &peers[i].address);
        (*env)->SetByteArrayRegion(env, peerPort, 0, sizeof(peers[i].port),
                                   (jbyte *) &peers[i].port);
        (*env)->SetByteArrayRegion(env, peerTimeStamp, 0, sizeof(peers[i].timestamp),
                                   (jbyte *) &peers[i].timestamp);
        mid = (*env)->GetMethodID(env, _peerClass, "<init>", "([B[B[B)V");
        peerObject = (*env)->NewObject(env, _peerClass, mid, peerAddress, peerPort, peerTimeStamp);
        (*env)->SetObjectArrayElement(env, peerObjectArray, i, peerObject);
        (*env)->DeleteLocalRef(env, peerAddress);
        (*env)->DeleteLocalRef(env, peerPort);
        (*env)->DeleteLocalRef(env, peerTimeStamp);
        (*env)->DeleteLocalRef(env, peerObject);
    }

    mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "savePeers",
                                    "([Lcom/breadwallet/presenter/entities/PeerEntity;Z)V");
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, peerObjectArray, replace ? JNI_TRUE : JNI_FALSE);
}

static int networkIsReachable(void *info) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "networkIsReachable");

    JNIEnv *env = getEnv();
    jmethodID mid;
    jboolean isNetworkOn;

    if (!env) return 0;

    //call java methods
    mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "networkIsReachable", "()Z");
    isNetworkOn = (*env)->CallStaticBooleanMethod(env, _peerManagerClass, mid);
    return (isNetworkOn == JNI_TRUE) ? 1 : 0;
}

static void threadCleanup(void *info) {
    if (_jvmPM)
        (*_jvmPM)->DetachCurrentThread(_jvmPM);
}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_rescan(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "rescan");
    if (!_peerManager)
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "rescan: peerManager is NULL!!!!!!!");
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "is Connected: %d",
//                        BRPeerManagerIsConnected(_peerManager));
    if (_peerManager) BRPeerManagerRescan(_peerManager);
}

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_create(JNIEnv *env, jobject thiz,
                                                 int earliestKeyTime,
                                                 int blocksCount, int peersCount) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ",
                        "create| blocksCount: %d, peersCount: %d, earliestKeyTime: %d",
                        blocksCount, peersCount, earliestKeyTime);

    jint rs = (*env)->GetJavaVM(env, &_jvmPM);
    jclass peerManagerClass = (*env)->FindClass(env, "com/breadwallet/wallet/BRPeerManager");
    jclass blockClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/BlockEntity");
    jclass peerClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/PeerEntity");

    _peerManagerClass = (jclass) (*env)->NewGlobalRef(env, (jobject) peerManagerClass);
    _blockClass = (jclass) (*env)->NewGlobalRef(env, (jobject) blockClass);
    _peerClass = (jclass) (*env)->NewGlobalRef(env, (jobject) peerClass);

    if (rs != JNI_OK)
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "WARNING, GetJavaVM is not JNI_OK");
    if (_wallet == NULL)
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: _wallet");

    if (!_peerManager) {
        assert(_wallet);
        if (peersCount > 0)
            assert(_peers);
        if (blocksCount > 0)
            assert(_blocks);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRPeerManagerNew called: %zu",
                            ++_managerNewCounter);
        if (earliestKeyTime < BIP39_CREATION_TIME) earliestKeyTime = BIP39_CREATION_TIME;
        __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "earliestKeyTime: %d",
                            earliestKeyTime);
        _peerManager = BRPeerManagerNew(_wallet, (uint32_t) earliestKeyTime, _blocks,
                                        (size_t) blocksCount,
                                        _peers, (size_t) peersCount);
        BRPeerManagerSetCallbacks(_peerManager, NULL, syncStarted, syncStopped,
                                  txStatusUpdate,
                                  saveBlocks, savePeers, networkIsReachable, threadCleanup);
    }

    if (_peerManager == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: _peerManager");
        return;
    }
    jmethodID mid;

    //call java methods
    mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "updateLastBlockHeight", "(I)V");
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid,
                                 (jint) BRPeerManagerLastBlockHeight(_peerManager));
}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env,
                                                                         jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "connect");
    if (_peerManager) BRPeerManagerConnect(_peerManager);
}


//Call multiple times with all the blocks from the DB
JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env, jobject thiz, jbyteArray block,
                                                   int blockHeight) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "putBlock");

    if (!_blocks) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", " >>>>>>  _blocks is NULL");
        return;
    }

    BRMerkleBlock *b;
    int blockLength = (*env)->GetArrayLength(env, block);
    jbyte *blockBytes = (*env)->GetByteArrayElements(env, block, 0);

    assert(blockBytes != NULL);
    if (!blockBytes) return;
    b = BRMerkleBlockParse((const uint8_t *) blockBytes, (size_t) blockLength);
    if (!b) return;
    b->height = (uint32_t) blockHeight;
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "adding a block: blockhight: %d", b->height);
    _blocks[_blocksCounter++] = b;
}

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_createBlockArrayWithCount(JNIEnv *env, jobject thiz,
                                                                    size_t blockCount) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ",
                        "block array created with count: %zu", blockCount);
    _blocks = calloc(blockCount, sizeof(*_blocks));
    _blocksCounter = 0;
    // need to call free();
}

//Call multiple times with all the peers from the DB
JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_putPeer(JNIEnv *env, jobject thiz, jbyteArray peerAddress,
                                                  jbyteArray peerPort, jbyteArray peerTimeStamp) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "putPeer");

    if (!_peers) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", " >>>>>>  _peers is NULL");
        return;
    }

    BRPeer p;
    jbyte *byteAddr = (*env)->GetByteArrayElements(env, peerAddress, 0);
    jbyte *bytePort = (*env)->GetByteArrayElements(env, peerPort, 0);
    jbyte *byteStamp = (*env)->GetByteArrayElements(env, peerTimeStamp, 0);

    p.address = *(UInt128 *) byteAddr;
    p.port = *(uint16_t *) bytePort;
    p.timestamp = *(uint64_t *) byteStamp;
    p.services = SERVICES_NODE_NETWORK;
    p.flags = 0;
    _peers[_peersCounter++] = p;
}

JNIEXPORT void JNICALL
Java_com_breadwallet_wallet_BRPeerManager_createPeerArrayWithCount(JNIEnv *env, jobject thiz,
                                                                   size_t peerCount) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "peer array created with count: %zu",
                        peerCount);
    _peers = calloc(peerCount, sizeof(BRPeer));
    _peersCounter = 0;
    // need to call free();
}

JNIEXPORT jdouble JNICALL
Java_com_breadwallet_wallet_BRPeerManager_syncProgress(JNIEnv *env, jobject thiz, int startHeight) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncProgress");
    if (!_peerManager || !_wallet) return 0;
    return (jdouble) BRPeerManagerSyncProgress(_peerManager, (uint32_t) startHeight);
    // need to call free();
}

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRPeerManager_getCurrentBlockHeight(JNIEnv *env,
                                                                                       jobject thiz) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getCurrentBlockHeight");
    if (!_peerManager) return 0;
    return (jint) BRPeerManagerLastBlockHeight(_peerManager);
}

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_isCreated(JNIEnv *env,
                                                                               jobject obj) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "peerManager isCreated %s",
                        _peerManager ? "yes" : "no");
    return (jboolean) ((_peerManager) ? JNI_TRUE : JNI_FALSE);
}

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_isConnected(JNIEnv *env,
                                                                                 jobject obj) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "isConnected");
    return (jboolean) (_peerManager && BRPeerManagerIsConnected(_peerManager));
}

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRPeerManager_getEstimatedBlockHeight(
        JNIEnv *env, jobject thiz) {
//    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "getEstimatedBlockHeight");
    if (!_peerManager || !_wallet) return 0;
    return (jint) BRPeerManagerEstimatedBlockHeight(_peerManager);
}
JNIEXPORT jstring JNICALL Java_com_breadwallet_wallet_BRPeerManager_getCurrentPeerName(
        JNIEnv *env, jobject thiz) {
//    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "getEstimatedBlockHeight");
    if (!_peerManager || !_wallet) return 0;

    return  (*env)->NewStringUTF(env, BRPeerManagerDownloadPeerName(_peerManager));
}

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_setFixedPeer(
        JNIEnv *env, jobject thiz, jstring node, jint port) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "updateFixedPeer");
    if (!_peerManager) return JNI_FALSE;
    const char *host = (*env)->GetStringUTFChars(env, node, NULL);
    UInt128 address = UINT128_ZERO;
    uint16_t _port = (uint16_t) port;
    if (strlen(host) != 0) {
        struct in_addr addr;
        if (inet_pton(AF_INET, host, &addr) != 1) return JNI_FALSE;
        address.u16[5] = 0xffff;
        address.u32[3] = addr.s_addr;
        if (port == 0) _port = STANDARD_PORT;
    } else {
        _port = 0;
    }

    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "BRPeerManagerSetFixedPeer: %s:%d",
                        host, _port);
    BRPeerManagerSetFixedPeer(_peerManager, address, _port);
    return JNI_TRUE;
}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_peerManagerFreeEverything(
        JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "peerManagerFreeEverything");

    if (_peerManager) {
        BRPeerManagerDisconnect(_peerManager);
        if (_peerManager) {
            BRPeerManagerFree(_peerManager);
            _peerManager = NULL;
        }
    }

    if (_blocks) {
        free(_blocks);
        _blocks = NULL;
    }

    if (_peers != NULL) {
        free(_peers);
        _peers = NULL;
    }
}
