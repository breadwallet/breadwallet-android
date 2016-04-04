//
// Created by Mihail Gutan on 12/11/15.
//
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
static JavaVM *_jvm;
static BRPeer *_peers;
static size_t _blocksCounter = 0;
static size_t _peersCounter = 0;
static jclass _peerManagerClass;
static size_t _managerNewCounter = 0;
static jclass _blockClass;
static jclass _peerClass;
static JNIEnv *_env;

JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM* vm, void* reserved){

    if ((*vm)->GetEnv(vm, &_env, JNI_VERSION_1_6) != JNI_OK) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncStarted");
       return -1;
    }

    jclass tmpBlockClass = (*_env)->FindClass(_env, "com/breadwallet/presenter/entities/BlockEntity");
    _blockClass = (jclass)(*_env)->NewGlobalRef(_env, tmpBlockClass);
    jclass tmpPeerClass = (*_env)->FindClass(_env, "com/breadwallet/presenter/entities/PeerEntity");
    _peerClass = (jclass)(*_env)->NewGlobalRef(_env, tmpPeerClass);

    return JNI_VERSION_1_6;
}

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

}

static void syncSucceeded(void *info) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncSucceeded: # of tx: %d",
                        (int) BRWalletTransactions(_wallet, NULL, 0) );
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncSucceeded", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

}

static void syncFailed(void *info, int error) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncFailed");
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _peerManagerClass, "syncFailed", "()V");
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _peerManagerClass, mid);

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


static void saveBlocks(void *info, BRMerkleBlock *blocks[], size_t count) {
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

    (*_jvm)->DetachCurrentThread(_jvm);

}

static void savePeers(void *info, const BRPeer peers[], size_t count) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "savePeers");

    JNIEnv *env = getEnv();

    jmethodID mid = (*env)->GetStaticMethodID(env, _peerManagerClass, "savePeers", "([Lcom/breadwallet/presenter/entities/PeerEntity;)V");
    //call java methods
    if(count != 1){
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "deleting %d peers", count);
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
    }
    (*env)->CallStaticVoidMethod(env, _peerManagerClass, mid, peersObjects);

    (*_jvm)->DetachCurrentThread(_jvm);
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
                                                                          jint earliestKeyTime,
                                                                          jint blocksCount,
                                                                          jint peersCount) {
    jint rs = (*env)->GetJavaVM(env, &_jvm);

    jclass walletManagerCLass = (*env)->FindClass(env, "com/breadwallet/wallet/BRPeerManager");
    _peerManagerClass = (jclass) (*env)->NewGlobalRef(env, (jobject) walletManagerCLass);

    if (rs != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "WARNING, GetJavaVM is not JNI_OK");
    }

    if (_wallet == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: blocks");
        return;
    }

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "blocksCount: %d", blocksCount);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "peersCount: %d", peersCount);

    if (!_peerManager) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRPeerManagerNew called: %d", ++_managerNewCounter);
        _peerManager = BRPeerManagerNew(_wallet, earliestKeyTime != 0 ? earliestKeyTime
                                                                  : BIP39_CREATION_TIME,
                                    blocksCount == 0 ? NULL : _blocks,
                                    blocksCount, peersCount == 0 ? NULL : _peers,
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

    if(!BRPeerMangerIsConnected(_peerManager))
        BRPeerManagerConnect(_peerManager);
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz) {
    if(_peerManager) BRPeerManagerConnect(_peerManager);
}


//Call multiple times with all the blocks from the DB
JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env,
                                                                  jobject thiz,
                                                                  jbyteArray block,
                                                                  int blockHeight) {
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

JNIEXPORT jboolean JNICALL Java_com_breadwallet_wallet_BRPeerManager_isCreated
        (JNIEnv *env, jobject obj) {

    int result = _peerManager;
    return result ? JNI_TRUE : JNI_FALSE;

}

JNIEXPORT jint Java_com_breadwallet_wallet_BRPeerManager_getEstimatedBlockHeight(JNIEnv *env,
                                                                               jobject thiz) {
//    int estimatedBlockHeight = BRPeerManagerEstimatedBlockHeight(_peerManager);
//    int lastBlockHeight = BRPeerManagerLastBlockHeight(_peerManager);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "estimatedBlockHeight: %d, "
//            "lastBlockHeight: %d", estimatedBlockHeight, lastBlockHeight);
//    int result = 0;
//    if(estimatedBlockHeight >= INT32_MAX){
//        result = lastBlockHeight;
//    } else {
//        result = estimatedBlockHeight;
//    }
//
//    return (jint) result;
    return (jint) BRPeerManagerEstimatedBlockHeight(_peerManager);
}


