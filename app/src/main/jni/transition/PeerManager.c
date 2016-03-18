//
// Created by Mihail Gutan on 12/11/15.
//
#include "PeerManager.h"
#include "BRPeer.h"
#include "WalletCallbacks.h"
#include "BRInt.h"
#include <android/log.h>
#include "BRMerkleBlock.h"
#include "BRWallet.h"
#include "wallet.h"
#include <pthread.h>

static BRMerkleBlock **_blocks;
BRPeerManager *_peerManager;
static JavaVM *_jvm;
static BRPeer *_peers;
static size_t _blocksCounter = 0;
static size_t _peersCounter = 0;
static jclass _peerManagerClass;

static JNIEnv* getEnv() {
    JNIEnv *env;
    int status = (*_jvm)->GetEnv(_jvm,(void**)&env, JNI_VERSION_1_6);
    if(status < 0) {
        status = (*_jvm)->AttachCurrentThread(_jvm, &env, NULL);
        if(status < 0) {
            return NULL;
        }
    }
    return env;
}


static void syncStarted(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncStarted");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncStarted", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

    //TODO destroy the _lock
}

static void syncSucceded(void *info) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncSucceded: # of tx: %d",
                        (int) BRWalletTransactions(_wallet, NULL, 0) );
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncSucceded", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);


}

static void syncFailed(void *info, int error) {


    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncFailed");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncFailed", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

    (*_jvm)->DetachCurrentThread(_jvm);


}

static void txStatusUpdate(void *info) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txStatusUpdate");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "txStatusUpdate", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

    (*_jvm)->DetachCurrentThread(_jvm);

}

static void txRejected(void *info, int rescanRecommended) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txRejected");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "txRejected", "(I)V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid, rescanRecommended);

    (*_jvm)->DetachCurrentThread(_jvm);
}


JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_rescan(JNIEnv *env, jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "rescan");
    if(_peerManager)
        BRPeerManagerRescan(_peerManager);
}

static void saveBlocks(void *info, BRMerkleBlock *blocks[], size_t count) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "saveBlocks");

//    JNIEnv *globalEnv = getEnv();
//    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "saveBlocks", "([B)V");
//    //call java methods
//
//    for (int i = 0; i < count; i++) {
//        uint8_t buf[BRMerkleBlockSerialize(blocks[i], NULL, 0)];
//        size_t len = BRMerkleBlockSerialize(blocks[i], buf, sizeof(buf));
//        jbyteArray result = (*globalEnv)->NewByteArray(globalEnv, len);
//        (*globalEnv)->SetByteArrayRegion(globalEnv, result, 0, len, (jbyte *) buf);
//        (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid, result);
//    }
//    (*_jvm)->DetachCurrentThread(_jvm);

}

static void savePeers(void *info, const BRPeer peers[], size_t count) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "savePeers");

//    JNIEnv *globalEnv = getEnv();
//
//    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "savePeers", "([B[B[B)V");
//    //call java methods
//
//    for (int i = 0; i < count; i++) {
//
//        jbyteArray peerAddress = (*globalEnv)->NewByteArray(globalEnv, sizeof(peers[i].address));
//        (*globalEnv)->SetByteArrayRegion(globalEnv, peerAddress, 0, sizeof(peers[i].address), (jbyte *) &peers[i].address);
//
//        jbyteArray peerPort = (*globalEnv)->NewByteArray(globalEnv, sizeof(peers[i].port));
//        (*globalEnv)->SetByteArrayRegion(globalEnv, peerPort, 0, sizeof(peers[i].port), (jbyte *) &peers[i].port);
//
//        jbyteArray peerTimeStamp = (*globalEnv)->NewByteArray(globalEnv, sizeof(peers[i].timestamp));
//        (*globalEnv)->SetByteArrayRegion(globalEnv, peerTimeStamp, 0, sizeof(peers[i].timestamp), (jbyte *) &peers[i].timestamp);
//
//        (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid, peerAddress, peerPort, peerTimeStamp);
//    }
//    (*_jvm)->DetachCurrentThread(_jvm);
}

static int networkIsReachable(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "networkIsReachable");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "networkIsReachable", "()Z");
    //call java methods
    jboolean isNetworkOn = (*globalEnv)->CallStaticBooleanMethod(globalEnv, _peerManagerClass, mid);

    return isNetworkOn == JNI_TRUE ? 1 : 0;
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz,
                                                                 jint earliestKeyTime,
                                                                 int blocksCount,
                                                                 int peersCount) {
    jint rs = (*env)->GetJavaVM(env, &_jvm);

    jclass walletManagerCLass = (*env)->FindClass(env,"com/breadwallet/wallet/BRPeerManager");
    _peerManagerClass = (jclass) (*env)->NewGlobalRef(env, (jobject) walletManagerCLass);

    if (rs != JNI_OK){
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, GetJavaVM is not JNI_OK");
    }

    if (_wallet == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: blocks");
        return;
    }

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "blocksCount: %d", blocksCount);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "peersCount: %d", peersCount);

    _peerManager = BRPeerManagerNew(_wallet,  (uint32_t) earliestKeyTime,
                                                  blocksCount == 0 ? NULL : _blocks,
                                                  blocksCount, peersCount == 0 ? NULL : _peers,
                                                  peersCount);
    //TESTING ONLY
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "earliestKeyTime: %d",  (int) earliestKeyTime);
//    _peerManager = BRPeerManagerNew(_wallet, (uint32_t) earliestKeyTime, NULL,0, NULL, 0);

    if (_peerManager == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: _peerManager");
        return;
    }

    BRPeerManagerSetCallbacks(_peerManager, NULL, syncStarted, syncSucceded, syncFailed,
                              txStatusUpdate, txRejected, saveBlocks, savePeers,
                              networkIsReachable);
    BRPeerManagerConnect(_peerManager);
}

//Call multiple times with all the blocks from the DB
JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env,
                                                                  jobject thiz,
                                                                  jbyteArray block) {
    int bkLength = (*env)->GetArrayLength(env, block);
    jbyte *byteBk = (*env)->GetByteArrayElements(env, block, 0);
    BRMerkleBlock *tmpBk = BRMerkleBlockParse(byteBk, bkLength);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "adding a block: blockhight: %d, "
//            "transactionCounter: %d", tmpTx->blockHeight, _transactionsCounter);
    _blocks[_blocksCounter++] = tmpBk;
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createBlockArrayWithCount(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   size_t bkCount) {
    _blocks = calloc(bkCount, sizeof(BRMerkleBlock));
    // need to call free();
}

//Call multiple times with all the peers from the DB
JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putPeer(JNIEnv *env,
                                                                 jobject thiz,
                                                                 jbyteArray peerAddress,
                                                                 jbyteArray peerPort,
                                                                 jbyteArray peerTimeStamp) {
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
    _peers = calloc(prCount, sizeof(BRPeer));
    // need to call free();
}

JNIEXPORT jdouble Java_com_breadwallet_wallet_BRPeerManager_syncProgress(JNIEnv *env,
                                                                      jobject thiz) {
    if (_peerManager == NULL) return 4;

    return (jdouble) BRPeerManagerSyncProgress(_peerManager);
    // need to call free();
}

JNIEXPORT jint Java_com_breadwallet_wallet_BRPeerManager_getCurrentBlockHeight(JNIEnv *env,
                                                                         jobject thiz) {
    return (jint) BRPeerManagerLastBlockHeight(_peerManager);

}
