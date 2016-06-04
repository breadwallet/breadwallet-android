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
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getEnv peerManager");
    if (!_jvmPM) return NULL;
    JNIEnv *env;
    int status = (*_jvmPM)->GetEnv(_jvmPM, (void **) &env, JNI_VERSION_1_6);
    if (status < 0) {
        status = (*_jvmPM)->AttachCurrentThread(_jvmPM, &env, NULL);
        if (status < 0) {
            return NULL;
        }
    }
    return env;
}

static void syncStarted(void *info) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncStarted");
    if (!_peerManager) return;
    JNIEnv *globalEnv = getEnv();
    if (!globalEnv) return;

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncStarted",
                                                    "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

}

static void syncSucceeded(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncSucceeded: # of tx: %d",
                        (int) BRWalletTransactions(_wallet, NULL, 0));
    if (!_peerManager) return;

    JNIEnv *globalEnv = getEnv();
    if (!globalEnv) return;
    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncSucceeded",
                                                    "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

}

static void syncFailed(void *info, int error) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncFailed");
    if (!_peerManager) return;
    JNIEnv *globalEnv = getEnv();
    if (!globalEnv) return;
    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncFailed",
                                                    "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);
    (*_jvmPM)->DetachCurrentThread(_jvmPM);

}

static void txStatusUpdate(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txStatusUpdate");
    if (!_peerManager) return;

    JNIEnv *globalEnv = getEnv();
    if (!globalEnv) return;
    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "txStatusUpdate",
                                                    "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

    (*_jvmPM)->DetachCurrentThread(_jvmPM);

}


static void saveBlocks(void *info, BRMerkleBlock *blocks[], size_t count) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "saveBlocks");
    if (!_peerManager) return;

    JNIEnv *env = getEnv();
    if (!env) return;
    jmethodID mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "saveBlocks",
                                              "([Lcom/breadwallet/presenter/entities/BlockEntity;)V");
    if (count != 1) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "deleting %zu blocks", count);
        jmethodID delete_mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "deleteBlocks",
                                                         "()V");
        (*env)->CallStaticVoidMethod(env, _peerManagerClass, delete_mid);
    }
    //call java methods

    //Find the class and populate the array of objects of this class

    jobjectArray blocksObjects = (*env)->NewObjectArray(env, (jsize) count, _blockClass, 0);

    for (int i = 0; i < count; i++) {
        if (!_peerManager) return;
        jmethodID blockObjMid = (*env)->GetMethodID(env, _blockClass, "<init>", "([BI)V");

        uint8_t buf[BRMerkleBlockSerialize(blocks[i], NULL, 0)];
        size_t len = BRMerkleBlockSerialize(blocks[i], buf, sizeof(buf));
        jbyteArray result = (*env)->NewByteArray(env, (jsize) len);
        (*env)->SetByteArrayRegion(env, result, 0, (jsize) len, (jbyte *) buf);

        jobject blockObject = (*env)->NewObject(env, _blockClass, blockObjMid, result,
                                                blocks[i]->height);
        (*env)->SetObjectArrayElement(env, blocksObjects, i, blockObject);

        (*env)->DeleteLocalRef(env, result);
        (*env)->DeleteLocalRef(env, blockObject);
    }

    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, blocksObjects);

    (*_jvmPM)->DetachCurrentThread(_jvmPM);

}

static void savePeers(void *info, const BRPeer peers[], size_t count) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "savePeers");
    if (!_peerManager) return;

    JNIEnv *env = getEnv();
    if (!env) return;
    jmethodID mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "savePeers",
                                              "([Lcom/breadwallet/presenter/entities/PeerEntity;)V");
    //call java methods
    if (count != 1) {
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "deleting %d peers", count);
        jmethodID delete_mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "deletePeers",
                                                         "()V");
        (*env)->CallStaticVoidMethod(env, _peerManagerClass, delete_mid);
    }

    jobjectArray peersObjects = (*env)->NewObjectArray(env, (jsize) count, _peerClass, 0);

    for (int i = 0; i < count; i++) {
        if (!_peerManager) return;
        jmethodID peerObjMid = (*env)->GetMethodID(env, _peerClass, "<init>", "([B[B[B)V");

        jbyteArray peerAddress = (*env)->NewByteArray(env, sizeof(peers[i].address));
        (*env)->SetByteArrayRegion(env, peerAddress, 0, sizeof(peers[i].address),
                                   (jbyte *) &peers[i].address);

        jbyteArray peerPort = (*env)->NewByteArray(env, sizeof(peers[i].port));
        (*env)->SetByteArrayRegion(env, peerPort, 0, sizeof(peers[i].port),
                                   (jbyte *) &peers[i].port);

        jbyteArray peerTimeStamp = (*env)->NewByteArray(env, sizeof(peers[i].timestamp));
        (*env)->SetByteArrayRegion(env, peerTimeStamp, 0, sizeof(peers[i].timestamp),
                                   (jbyte *) &peers[i].timestamp);

//        (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, peerAddress, peerPort, peerTimeStamp);
        jobject peerObject = (*env)->NewObject(env, _peerClass, peerObjMid, peerAddress, peerPort,
                                               peerTimeStamp);
        (*env)->SetObjectArrayElement(env, peersObjects, i, peerObject);

        (*env)->DeleteLocalRef(env, peerAddress);
        (*env)->DeleteLocalRef(env, peerPort);
        (*env)->DeleteLocalRef(env, peerTimeStamp);
        (*env)->DeleteLocalRef(env, peerObject);
    }
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, peersObjects);

    (*_jvmPM)->DetachCurrentThread(_jvmPM);
}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_rescan(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "rescan");
    if (_peerManager)
        BRPeerManagerRescan(_peerManager);
}

static int networkIsReachable(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "networkIsReachable");
    JNIEnv *globalEnv = getEnv();
    if (!globalEnv) return 0;
    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass,
                                                    "networkIsReachable", "()Z");
    //call java methods
    jboolean isNetworkOn = (*globalEnv)->CallStaticBooleanMethod(globalEnv, _peerManagerClass, mid);

    return (isNetworkOn == JNI_TRUE) ? 1 : 0;
}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_createAndConnect(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  int earliestKeyTime,
                                                                                  int blocksCount,
                                                                                  int peersCount) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                        "createAndConnect| blocksCount: %d, peersCount: %d", blocksCount,
                        peersCount);
    jint rs = (*env)->GetJavaVM(env, &_jvmPM);

    jclass walletManagerCLass = (*env)->FindClass(env, "com/breadwallet/wallet/BRPeerManager");
    _peerManagerClass = (jclass) (*env)->NewGlobalRef(env, (jobject) walletManagerCLass);

    jclass tmpBlockClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/BlockEntity");
    _blockClass = (jclass) (*env)->NewGlobalRef(env, (jobject) tmpBlockClass);
    jclass tmpPeerClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/PeerEntity");
    _peerClass = (jclass) (*env)->NewGlobalRef(env, (jobject) tmpPeerClass);

    if (rs != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "WARNING, GetJavaVM is not JNI_OK");
    }

    if (_wallet == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: _wallet");
    }

    if (!_peerManager) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRPeerManagerNew called: %zu",
                            ++_managerNewCounter);
        _peerManager = BRPeerManagerNew(_wallet, (uint32_t) (earliestKeyTime > BIP39_CREATION_TIME
                                                             ? earliestKeyTime
                                                             : BIP39_CREATION_TIME), _blocks,
                                        (size_t) blocksCount, _peers, (size_t) peersCount);
        BRPeerManagerSetCallbacks(_peerManager, NULL, syncStarted, syncSucceeded, syncFailed,
                                  txStatusUpdate, saveBlocks, savePeers,
                                  networkIsReachable);
    }
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "earliestKeyTime: %d",
                        earliestKeyTime);

    if (_peerManager == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: _peerManager");
        return;
    }

    BRPeerManagerConnect(_peerManager);
}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env,
                                                                         jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "connect");
    if (_peerManager) {
        BRPeerManagerConnect(_peerManager);
    }
}


//Call multiple times with all the blocks from the DB
JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env,
                                                                          jobject thiz,
                                                                          jbyteArray block,
                                                                          int blockHeight) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "putBlock");
    if (!_blocks) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", " >>>>>>  _blocks is NULL");
        return;
    }
    int bkLength = (*env)->GetArrayLength(env, block);
    jbyte *byteBk = (*env)->GetByteArrayElements(env, block, 0);
    BRMerkleBlock *tmpBk = BRMerkleBlockParse((const uint8_t *) byteBk, (size_t) bkLength);
    tmpBk->height = (uint32_t) blockHeight;

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "adding a block: blockhight: %d",
                        tmpBk->height);
    _blocks[_blocksCounter++] = tmpBk;
}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_createBlockArrayWithCount(
        JNIEnv *env,
        jobject thiz,
        size_t bkCount) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                        "block array created with count: %zu", bkCount);
    _blocks = calloc(bkCount, sizeof(*_blocks));
    // need to call free();
}

//Call multiple times with all the peers from the DB
JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_putPeer(JNIEnv *env,
                                                                         jobject thiz,
                                                                         jbyteArray peerAddress,
                                                                         jbyteArray peerPort,
                                                                         jbyteArray peerTimeStamp) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "putPeer");
    if (!_peers) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", " >>>>>>  _peers is NULL");
        return;
    }
//    int addrLength = (*env)->GetArrayLength(env, peerAddress);
    jbyte *byteAddr = (*env)->GetByteArrayElements(env, peerAddress, 0);

//    int portLength = (*env)->GetArrayLength(env, peerPort);
    jbyte *bytePort = (*env)->GetByteArrayElements(env, peerPort, 0);

//    int stampLength = (*env)->GetArrayLength(env, peerTimeStamp);
    jbyte *byteStamp = (*env)->GetByteArrayElements(env, peerTimeStamp, 0);

    BRPeer tmpPr;
    tmpPr.address = *(UInt128 *) byteAddr;
    tmpPr.port = *(uint16_t *) bytePort;
    tmpPr.timestamp = *(uint64_t *) byteStamp;
    tmpPr.services = SERVICES_NODE_NETWORK;
    tmpPr.flags = 0;

    _peers[_peersCounter++] = tmpPr;

//    (jbyte *)&tmpPr.address;
//    sizeof(tmpPr.address);
//
//            BRMerkleBlockParse(byteBk, bkLength);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "adding a block: blockhight: %d, "
//            "transactionCounter: %d", tmpTx->blockHeight, _transactionsCounter);

}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_createPeerArrayWithCount(
        JNIEnv *env,
        jobject thiz,
        size_t prCount) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "peer array created with count: %zu",
                        prCount);
    _peers = calloc(prCount, sizeof(BRPeer));
    // need to call free();
}

JNIEXPORT jdouble JNICALL Java_com_breadwallet_wallet_BRPeerManager_syncProgress(JNIEnv *env,
                                                                                 jobject thiz) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncProgress");

    if (!_peerManager || !_wallet) return 0;

    return (jdouble) BRPeerManagerSyncProgress(_peerManager);
    // need to call free();
}

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRPeerManager_getCurrentBlockHeight(JNIEnv *env,
                                                                                       jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getCurrentBlockHeight");
    if (!_peerManager) return 0;
    return (jint) BRPeerManagerLastBlockHeight(_peerManager);

}

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_isCreated
        (JNIEnv *env, jobject obj) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "peerManager isCreated %s",
                        _peerManager ? "yes" : "no");
    return (jboolean) (_peerManager ? JNI_TRUE : JNI_FALSE);

}

JNIEXPORT jint JNICALL Java_com_breadwallet_wallet_BRPeerManager_getEstimatedBlockHeight(
        JNIEnv *env,
        jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getEstimatedBlockHeight");
    if (!_peerManager || !_wallet) return 0;
    return (jint) BRPeerManagerEstimatedBlockHeight(_peerManager);
}

JNIEXPORT void JNICALL Java_com_breadwallet_wallet_BRPeerManager_peerManagerFreeEverything(
        JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "peerManagerFreeEverything");
    if (_peerManager) {
        BRPeerManagerDisconnect(_peerManager);
        BRPeerManagerFree(_peerManager);
        _peerManager = NULL;
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

//JNIEXPORT void JNICALL
//Java_com_breadwallet_presenter_activities_IntroActivity_testCore(JNIEnv *env, jobject instance) {
//    BRKey key;
//    BRKeySetPrivKey(&key, "5Kb8kLf9zgWQnogidDA76MzPL6TsZZY36hWXMssSzNydYXYB9KF");
//}
