//
// Created by Mihail Gutan on 12/11/15.
//
#include "PeerManagerCallbacks.h"

void syncStarted(void *info) {

}

void syncSucceeded(void *info) {

}

void syncFailed(void *info, int error) {

}

void txStatusUpdate(void *info) {

}

void txRejected(void *info, int rescanRecommended){

}

void saveBlocks(void *info, const BRMerkleBlock blocks[], size_t count) {

}

void savePeers(void *info, const BRPeer peers[], size_t count) {

}

int networkIsReachable(void *info) {

}

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_setPeerManagerCallbacks(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jbyteArray peerManagerBytes) {
    jbyte *bytePeerManager = (*env)->GetByteArrayElements(env, peerManagerBytes, 0);
    BRPeerManager *peerManager = (BRPeerManager*) bytePeerManager;
    BRPeerManagerSetCallbacks(peerManager, NULL, syncStarted, syncSucceeded, syncFailed,
                              txStatusUpdate, txRejected, saveBlocks, savePeers, networkIsReachable);

}


