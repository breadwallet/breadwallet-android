//
// Created by Mihail Gutan on 12/11/15.
//

#ifndef BREADWALLET_PEERMANAGERCALLBACKS_H
#define BREADWALLET_PEERMANAGERCALLBACKS_H

#include "jni.h"
#include "BRInt.h"
#include "BRTransaction.h"

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_setPeerManagerCallBacks(JNIEnv *env,
                                                                                       jobject thiz,
                                                                                       jbyteArray peerManager);

void syncStarted(void *info);

void syncSucceded(void *info);

void syncFailed(void *info, BRPeerManagerError error);

void txStatusUpdate(void *info);

void saveBlocks(void *info, const BRMerkleBlock blocks[], size_t count);

void savePeers(void *info, const BRPeer peers[], size_t count);

int networkIsReachable(void *info);

#endif //BREADWALLET_PEERMANAGERCALLBACKS_H
