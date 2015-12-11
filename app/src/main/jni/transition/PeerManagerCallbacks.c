//
// Created by Mihail Gutan on 12/11/15.
//
JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRPeerManager_setCallBacks(JNIEnv *env,
                                                                            jobject thiz,
                                                                            jbyteArray peerManager) {
    jbyte *bytePeerManager = (*env)->GetByteArrayElements(env, peerManager, 0);
    BRPeerManager *peerManager = bytePeerManager;
    BRPeerManagerSetCallbacks(peerManager, NULL, &syncStarted, &syncSucceded, &syncFailed,
                              &txStatusUpdate, &saveBlocks, &savePeers, &networkIsReachable);

}

void syncStarted(void *info) {

}

void syncSucceded(void *info) {

}

void syncFailed(void *info, BRPeerManagerError error) {

}

void txStatusUpdate(void *info) {

}

void saveBlocks(void *info, const BRMerkleBlock blocks[], size_t count) {

}

void savePeers(void *info, const BRPeer peers[], size_t count) {

}

int networkIsReachable(void *info) {

}

