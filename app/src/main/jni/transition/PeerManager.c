//
// Created by Mihail Gutan on 12/11/15.
//
#include "PeerManager.h"
#include "BRPeerManager.h"
#include "BRPeer.h"
#include "WalletCallbacks.h"
#include "BRInt.h"
#include <android/log.h>
#include "BRMerkleBlock.h"
#include "BRWallet.h"
#include "wallet.h"

static BRMerkleBlock **_blocks;
static BRPeerManager *_peerManager;
static JavaVM *_jvm;
static BRPeer *_peers;
static size_t _blocksCounter = 0;
static size_t _peersCounter = 0;

//static jclass getPeerManagerClass(){
////    jthrowable myExc = (*env)->ExceptionOccurred(env);
////    if(myExc){
////        __android_log_print(ANDROID_LOG_ERROR, "getPeerManagerClass: ",
////                            "Exception occured! ");
////    } else {
////        __android_log_print(ANDROID_LOG_ERROR, "getPeerManagerClass: ",
////                            "Exception did not occur! ");
////    }
//    return ;;
//}
//
//static void updateEnv(){
//    if(_globalEnv == NULL){
//        __android_log_print(ANDROID_LOG_ERROR, "updateEnv: ",
//                            "_globalEnv is NULL");
//        jint rs = (*_jvm)->AttachCurrentThread(_jvm, &_globalEnv, NULL);
//    } else {
//        __android_log_print(ANDROID_LOG_ERROR, "updateEnv: ",
//                            "_globalEnv is NULL");
//    }
//}
//
//static jobject getPeerManagerInstance() {
//
////    updateEnv();
//
////    jclass clazz = getPeerManagerClass(_globalEnv);
////    jfieldID instanceFid = (*_globalEnv)->GetStaticFieldID(_globalEnv, clazz, "instance",
////                                                    "Lcom/breadwallet/wallet/BRPeerManager;");
////    jobject instance;
////    if (instanceFid == NULL) {
////        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
////                            "instanceFid is null!!!! returning ");
////        return NULL;
////    }
////    instance = (*_globalEnv)->GetStaticObjectField(_globalEnv, clazz, instanceFid);
////    if (instance == NULL) {
////        instance = (*_globalEnv)->AllocObject(_globalEnv, clazz);
////        (*_globalEnv)->SetStaticObjectField(_globalEnv, clazz, instanceFid, instance);
////    }
//
//    return _peerManagerInstance;
//}

static void syncStarted(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncStarted");
//    jobject instance = _peerManagerInstance;
//    if(instance == NULL){
//        __android_log_print(ANDROID_LOG_ERROR, "syncStarted ", "instance is NULL");
//    }
//    jclass clazz = _peerManagerClass;
//    if(clazz == NULL){
//        __android_log_print(ANDROID_LOG_ERROR, "syncStarted ", "clazz is NULL");
//    }
//    jmethodID mid = (*_globalEnv)->GetMethodID(_globalEnv, clazz, "syncStarted", "()V");
////    uint64_t walletBalance = BRWalletBalance(wallet);
////    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
////                        "BRWalletBalance(wallet): %d", BRWalletBalance(wallet));
//    //call java methods
//    (*_globalEnv)->CallVoidMethod(_globalEnv, instance, mid);

}

static void syncSucceeded(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncSucceeded" );
//    JNIEnv *globalEnv;
//    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
//    updateEnv();
}

static void syncFailed(void *info, int error) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "syncFailed");
//    JNIEnv *globalEnv;
//    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
//    updateEnv();
}

static void txStatusUpdate(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txStatusUpdate");
//    JNIEnv *globalEnv;
//    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
//    updateEnv();
}

static void txRejected(void *info, int rescanRecommended) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txRejected");
//    JNIEnv *globalEnv;
//    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
//    updateEnv();
}

static void saveBlocks(void *info, BRMerkleBlock *blocks[], size_t count) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "saveBlocks");

//    JNIEnv *globalEnv;
//    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
//    updateEnv();
//    create class
//    jclass clazz = _peerManagerClass;
//    jobject entity = _peerManagerInstance;
//    jmethodID mid = (*_globalEnv)->GetMethodID(_globalEnv, clazz, "saveBlocks", "([B)V");
//    //call java methods
//
//    for (int i = 0; i < count; i++) {
//        uint8_t buf[BRMerkleBlockSerialize(blocks[i], NULL, 0)];
//        size_t len = BRMerkleBlockSerialize(blocks[i], buf, sizeof(buf));
//        jbyteArray result = (*_globalEnv)->NewByteArray(_globalEnv, len);
//        (*_globalEnv)->SetByteArrayRegion(_globalEnv, result, 0, len, (jbyte *) buf);
//        (*_globalEnv)->CallVoidMethod(_globalEnv, entity, mid, result);
//    }

}

static void savePeers(void *info, const BRPeer peers[], size_t count) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "savePeers");
////    JNIEnv *globalEnv;
////    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
////    updateEnv();
//    //create class
//    jclass clazz = _peerManagerClass;
//    jobject entity = _peerManagerInstance;
//    jmethodID mid = (*_globalEnv)->GetMethodID(_globalEnv, clazz, "savePeers", "([B[B[B)V");
//    //call java methods
//
//    for (int i = 0; i < count; i++) {
//
//        jbyteArray peerAddress = (*_globalEnv)->NewByteArray(_globalEnv, sizeof(peers[i].address));
//        (*_globalEnv)->SetByteArrayRegion(_globalEnv, peerAddress, 0, sizeof(peers[i].address), (jbyte *) &peers[i].address);
//
//        jbyteArray peerPort = (*_globalEnv)->NewByteArray(_globalEnv, sizeof(peers[i].port));
//        (*_globalEnv)->SetByteArrayRegion(_globalEnv, peerPort, 0, sizeof(peers[i].port), (jbyte *) &peers[i].port);
//
//        jbyteArray peerTimeStamp = (*_globalEnv)->NewByteArray(_globalEnv, sizeof(peers[i].timestamp));
//        (*_globalEnv)->SetByteArrayRegion(_globalEnv, peerTimeStamp, 0, sizeof(peers[i].timestamp), (jbyte *) &peers[i].timestamp);
//
//        (*_globalEnv)->CallVoidMethod(_globalEnv, entity, mid, peerAddress, peerPort, peerTimeStamp);
//    }
}

static int networkIsReachable(void *info) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "networkIsReachable");
    return 1;
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz,
                                                                 jint earliestKeyTime,
                                                                 jlong blocksCount,
                                                                 jlong peersCount) {
    jint rs = (*env)->GetJavaVM(env, &_jvm);
    if (rs != JNI_OK){
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, GetJavaVM is not JNI_OK");
    }

    if (_wallet == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: %s", "blocks");
        return;
    }

//    _peerManager = BRPeerManagerNew(_wallet, earliestKeyTime,
//                                                  blocksCount == 0 ? NULL : _blocks,
//                                                  blocksCount, peersCount == 0 ? NULL : _peers,
//                                                  peersCount);
    //TESTING ONLY
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "earliestKeyTime: %d",  earliestKeyTime);
    _peerManager = BRPeerManagerNew(_wallet, (uint32_t) earliestKeyTime, NULL,0, NULL, 0);

    if (_peerManager == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: ", "_peerManager");
        return;
    }


    BRPeerManagerSetCallbacks(_peerManager, NULL, syncStarted, syncSucceeded, syncFailed,
                              txStatusUpdate, txRejected, saveBlocks, savePeers,
                              networkIsReachable);
    BRPeerManagerConnect(_peerManager);
}

//Call multiple times with all the blocks from the DB
JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putBlock(JNIEnv *env,
                                                                  jobject thiz,
                                                                  jbyteArray block) {
//    int bkLength = (*env)->GetArrayLength(env, block);
//    jbyte *byteBk = (*env)->GetByteArrayElements(env, block, 0);
//    BRMerkleBlock *tmpBk = BRMerkleBlockParse(byteBk, bkLength);
////    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "adding a block: blockhight: %d, "
////            "transactionCounter: %d", tmpTx->blockHeight, _transactionsCounter);
//    _blocks[_blocksCounter++] = tmpBk;
}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createBlockArrayWithCount(JNIEnv *env,
                                                                                   jobject thiz,
                                                                                   size_t bkCount) {
//    _blocks = calloc(bkCount, sizeof(BRMerkleBlock));
    // need to call free();
}

//Call multiple times with all the peers from the DB
JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_putPeer(JNIEnv *env,
                                                                 jobject thiz,
                                                                 jbyteArray peerAddress,
                                                                 jbyteArray peerPort,
                                                                 jbyteArray peerTimeStamp) {
//    int addrLength = (*env)->GetArrayLength(env, peerAddress);
//    jbyte *byteAddr = (*env)->GetByteArrayElements(env, peerAddress, 0);
//
//    int portLength = (*env)->GetArrayLength(env, peerPort);
//    jbyte *bytePort = (*env)->GetByteArrayElements(env, peerPort, 0);
//
//    int stampLength = (*env)->GetArrayLength(env, peerTimeStamp);
//    jbyte *byteStamp = (*env)->GetByteArrayElements(env, peerTimeStamp, 0);
//
//    BRPeer tmpPr;
//    tmpPr.address = *(UInt128 *) byteAddr;
//    tmpPr.port = *(uint16_t *) bytePort;
//    tmpPr.timestamp = *(uint64_t *) byteStamp;
//    tmpPr.services = SERVICES_NODE_NETWORK;
//    tmpPr.flags = 0;
//
//    _peers[_peersCounter++] = tmpPr;
//
////    (jbyte *)&tmpPr.address;
////    sizeof(tmpPr.address);
////
////            BRMerkleBlockParse(byteBk, bkLength);
////    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "adding a block: blockhight: %d, "
////            "transactionCounter: %d", tmpTx->blockHeight, _transactionsCounter);

}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_createPeerArrayWithCount(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  size_t prCount) {
//    _peers = calloc(prCount, sizeof(BRPeer));
    // need to call free();
}

JNIEXPORT jdouble Java_com_breadwallet_wallet_BRPeerManager_syncProgress(JNIEnv *env,
                                                                      jobject thiz) {
    if (_peerManager == NULL) return 4;

    return (jdouble) BRPeerManagerSyncProgress(_peerManager);
    // need to call free();
}
