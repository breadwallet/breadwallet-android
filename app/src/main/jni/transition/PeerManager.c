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

static JNIEnv* getEnv() {
    JNIEnv *env;
    int status = (*_jvmPM)->GetEnv(_jvmPM,(void**)&env, JNI_VERSION_1_6);
    if(status < 0) {
        status = (*_jvmPM)->AttachCurrentThread(_jvmPM, &env, NULL);
        if(status < 0) {
            return NULL;
        }
    }
    return env;
}


static void syncStarted(void *info) {

    if(!_peerManager) return;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncStarted");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncStarted", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

}

static void syncSucceeded(void *info) {
    if(!_peerManager) return;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncSucceeded: # of tx: %d",
                        (int) BRWalletTransactions(_wallet, NULL, 0) );
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncSucceeded", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

}

static void syncFailed(void *info, int error) {

    if(!_peerManager) return;

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncFailed");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncFailed", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

    (*_jvmPM)->DetachCurrentThread(_jvmPM);

}

static void txStatusUpdate(void *info) {
    if(!_peerManager) return;

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txStatusUpdate");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "txStatusUpdate", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

    (*_jvmPM)->DetachCurrentThread(_jvmPM);

}

static void txRejected(void *info, int rescanRecommended) {
    if(!_peerManager) return;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txRejected");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "txRejected", "(I)V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid, rescanRecommended);

    (*_jvmPM)->DetachCurrentThread(_jvmPM);
}


static void saveBlocks(void *info, BRMerkleBlock *blocks[], size_t count) {
    if(!_peerManager) return;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "saveBlocks");

    JNIEnv *env = getEnv();
    jmethodID mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "saveBlocks", "([Lcom/breadwallet/presenter/entities/BlockEntity;)V");
    if(count != 1){
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "deleting %d blocks", count);
        jmethodID delete_mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "deleteBlocks", "()V");
        (*env)->CallStaticVoidMethod(env, _peerManagerClass, delete_mid);
    }
    //call java methods

    //Find the class and populate the array of objects of this class

    jobjectArray blocksObjects = (*env)->NewObjectArray(env, count, _blockClass, 0);

    for (int i = 0; i < count; i++) {

        jmethodID blockObjMid = (*env)->GetMethodID(env, _blockClass, "<init>", "([BI)V");

        uint8_t buf[BRMerkleBlockSerialize(blocks[i], NULL, 0)];
        size_t len = BRMerkleBlockSerialize(blocks[i], buf, sizeof(buf));
        jbyteArray result = (*env)->NewByteArray(env, len);
        (*env)->SetByteArrayRegion(env, result, 0, len, (jbyte *) buf);

        jobject blockObject = (*env)->NewObject(env, _blockClass, blockObjMid, result, blocks[i]->height);
        (*env)->SetObjectArrayElement(env, blocksObjects, i, blockObject);

        (*env)->DeleteLocalRef(env, result);
        (*env)->DeleteLocalRef(env, blockObject);
    }

    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, blocksObjects);

    (*_jvmPM)->DetachCurrentThread(_jvmPM);

}

static void savePeers(void *info, const BRPeer peers[], size_t count) {
    if(!_peerManager) return;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "savePeers");

    JNIEnv *env = getEnv();

    jmethodID mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "savePeers", "([Lcom/breadwallet/presenter/entities/PeerEntity;)V");
    //call java methods
    if(count != 1){
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "deleting %d peers", count);
        jmethodID delete_mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "deletePeers", "()V");
        (*env)->CallStaticVoidMethod(env, _peerManagerClass, delete_mid);
    }

    jobjectArray peersObjects = (*env)->NewObjectArray(env, count, _peerClass, 0);

    for (int i = 0; i < count; i++) {

        jmethodID peerObjMid = (*env)->GetMethodID(env, _peerClass, "<init>", "([B[B[B)V");

        jbyteArray peerAddress = (*env)->NewByteArray(env, sizeof(peers[i].address));
        (*env)->SetByteArrayRegion(env, peerAddress, 0, sizeof(peers[i].address), (jbyte *) &peers[i].address);

        jbyteArray peerPort = (*env)->NewByteArray(env, sizeof(peers[i].port));
        (*env)->SetByteArrayRegion(env, peerPort, 0, sizeof(peers[i].port), (jbyte *) &peers[i].port);

        jbyteArray peerTimeStamp = (*env)->NewByteArray(env, sizeof(peers[i].timestamp));
        (*env)->SetByteArrayRegion(env, peerTimeStamp, 0, sizeof(peers[i].timestamp), (jbyte *) &peers[i].timestamp);

//        (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, peerAddress, peerPort, peerTimeStamp);
        jobject peerObject = (*env)->NewObject(env, _peerClass, peerObjMid, peerAddress, peerPort, peerTimeStamp);
        (*env)->SetObjectArrayElement(env, peersObjects, i, peerObject);

        (*env)->DeleteLocalRef(env, peerAddress);
        (*env)->DeleteLocalRef(env, peerPort);
        (*env)->DeleteLocalRef(env, peerTimeStamp);
        (*env)->DeleteLocalRef(env, peerObject);
    }
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, peersObjects);

    (*_jvmPM)->DetachCurrentThread(_jvmPM);
}

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_rescan(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "rescan");
    if(_peerManager)
        BRPeerManagerRescan(_peerManager);
}

static int networkIsReachable(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "networkIsReachable");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "networkIsReachable", "()Z");
    //call java methods
    jboolean isNetworkOn = (*globalEnv)->CallStaticBooleanMethod(globalEnv, _peerManagerClass, mid);

    return isNetworkOn == JNI_TRUE ? 1 : 0;
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createAndConnect(JNIEnv *env, jobject thiz,
                                                                          int earliestKeyTime,
                                                                          int blocksCount,
                                                                          int peersCount) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "blocksCount: %d", blocksCount);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "peersCount: %d", peersCount);
    jint rs = (*env)->GetJavaVM(env, &_jvmPM);

    jclass walletManagerCLass = (*env)->FindClass(env, "com/breadwallet/wallet/BRPeerManager");
    _peerManagerClass = (jclass) (*env)->NewGlobalRef(env, (jobject) walletManagerCLass);

    jclass tmpBlockClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/BlockEntity");
    _blockClass = (jclass)(*env)->NewGlobalRef(env, (jobject) tmpBlockClass);
    jclass tmpPeerClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/PeerEntity");
    _peerClass = (jclass)(*env)->NewGlobalRef(env, (jobject) tmpPeerClass);

    if (rs != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "WARNING, GetJavaVM is not JNI_OK");
    }

    if (_wallet == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: _wallet");
    }

    if (!_peerManager) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRPeerManagerNew called: %d", ++_managerNewCounter);
        _peerManager = BRPeerManagerNew(_wallet, earliestKeyTime > BIP39_CREATION_TIME ? earliestKeyTime
                                                                  : BIP39_CREATION_TIME,
                                    blocksCount == 0 || !_blocks ? NULL : _blocks,
                                    blocksCount, peersCount == 0 || !_peers ? NULL : _peers,
                                    peersCount);
        BRPeerManagerSetCallbacks(_peerManager, NULL, syncStarted, syncSucceeded, syncFailed,
                                  txStatusUpdate, txRejected, saveBlocks, savePeers,
                                  networkIsReachable);
    }
    //TESTING ONLY
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "earliestKeyTime: %d",  (int) earliestKeyTime);
//    _peerManager = BRPeerManagerNew(_wallet, (uint32_t) earliestKeyTime, NULL,0, NULL, 0);

    if (_peerManager == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: _peerManager");
        return;
    }

    if(!BRPeerManagerIsConnected(_peerManager))
        BRPeerManagerConnect(_peerManager);
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz) {
    if(_peerManager && _wallet) BRPeerManagerConnect(_peerManager);
}


//Call multiple times with all the blocks from the DB
JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env,
                                                                  jobject thiz,
                                                                  jbyteArray block,
                                                                  int blockHeight) {
    if(!_blocks) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", " >>>>>>  _blocks is NULL");
        return;
    }
    int bkLength = (*env)->GetArrayLength(env, block);
    jbyte *byteBk = (*env)->GetByteArrayElements(env, block, 0);
    BRMerkleBlock *tmpBk = BRMerkleBlockParse((const uint8_t *) byteBk, bkLength);
    tmpBk->height = blockHeight;

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "adding a block: blockhight: %d", tmpBk->height);
    _blocks[_blocksCounter++] = tmpBk;
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createBlockArrayWithCount(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   size_t bkCount) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "block array created with count: %d", bkCount);
    _blocks = calloc(bkCount, sizeof(BRMerkleBlock));
    // need to call free();
}

//Call multiple times with all the peers from the DB
JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putPeer(JNIEnv *env,
                                                                 jobject thiz,
                                                                 jbyteArray peerAddress,
                                                                 jbyteArray peerPort,
                                                                 jbyteArray peerTimeStamp) {
    if(! _peers) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", " >>>>>>  _peers is NULL");
        return;
    }
    int addrLength = (*env)->GetArrayLength(env, peerAddress);
    jbyte *byteAddr = (*env)->GetByteArrayElements(env, peerAddress, 0);

    int portLength = (*env)->GetArrayLength(env, peerPort);
    jbyte *bytePort = (*env)->GetByteArrayElements(env, peerPort, 0);

    int stampLength = (*env)->GetArrayLength(env, peerTimeStamp);
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

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createPeerArrayWithCount(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  size_t prCount) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "peer array created with count: %d",prCount);
    _peers = calloc(prCount, sizeof(BRPeer));
    // need to call free();
}

JNIEXPORT jdouble Java_com_breadwallet_wallet_BRPeerManager_syncProgress(JNIEnv *env,
                                                                      jobject thiz) {

    if(!_peerManager || !_wallet) return 0;

    return (jdouble) BRPeerManagerSyncProgress(_peerManager);
    // need to call free();
}

JNIEXPORT jint Java_com_breadwallet_wallet_BRPeerManager_getCurrentBlockHeight(JNIEnv *env,
                                                                         jobject thiz) {
    if(!_peerManager) return 0;
     return (jint) BRPeerManagerLastBlockHeight(_peerManager);

}

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_isCreated
        (JNIEnv *env, jobject obj) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "peerManager isCreated %s", _peerManager ? "yes" : "no");
    int result = _peerManager;
    return result ? JNI_TRUE : JNI_FALSE;

}

JNIEXPORT jint Java_com_breadwallet_wallet_BRPeerManager_getEstimatedBlockHeight(JNIEnv *env,
                                                                               jobject thiz) {
    if(!_peerManager || !_wallet) return 0;
    return (jint) BRPeerManagerEstimatedBlockHeight(_peerManager);
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_peerManagerFreeEverything(JNIEnv *env, jobject thiz) {
    if(_peerManager){
        BRPeerManagerDisconnect(_peerManager);
        BRPeerManagerFree(_peerManager);
    }
    if(_blocks)
        free(_blocks);
    if(_peers)
        free(_peers);
    JNIEnv *tempEnv = getEnv();
    _peerManager = NULL;

}


