//
// Created by Mihail Gutan on 12/11/15.
//
#include "PeerManager.h"

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz,
                                                                 jbyteArray walletBuff,
                                                                 jlong earliestKeyTime,
                                                                 jobjectArray blocksBuffers,
                                                                 jlong blocksCount,
                                                                 jobjectArray peersBuffers,
                                                                 jlong peersCount) {
    //create wallet
    jbyte *byteWallet = (*env)->GetByteArrayElements(env, walletBuff, 0);
    BRWallet *wallet = byteWallet;

    //create blocks
    BRMerkleBlock blocks[blocksCount];
    for (int i = 0; i < blocksCount; i++) {
        BRMerkleBlock block = (*env)->GetObjectArrayElement(env, blocksBuffers, i);
        jbyte *buffB = (*env)->GetDirectBufferAddress(env, block);
        blocks[i] = buffB;
    }
    if (blocks == NULL){
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: %s", "blocks");
        return;
    }

    //create peers
    BRPeer peers[peersCount];
    for (int i = 0; i < peersCount; i++) {
        BRPeer peer = (*env)->GetObjectArrayElement(env, peersBuffers, i);
        jbyte *buffP = (*env)->GetDirectBufferAddress(env, peer);
        peers[i] = buffP;
    }
    if (peers == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: %s", "peers");
        return;
    }

    BRPeerManager *peerManager = BRPeerManagerNew(wallet, earliestKeyTime,
                                                  blocks,
                                                  blocksCount, peers,
                                                  peersCount);
    if(peerManager == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "NULL: %s", "peerManager");
        return;
    }

    int size = sizeof(peerManager);
    jbyteArray bytePeerManager = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env, bytePeerManager, 0, size, peerManager);
    return bytePeerManager;
}
