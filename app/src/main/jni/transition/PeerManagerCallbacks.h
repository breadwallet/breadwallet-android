//
// Created by Mihail Gutan on 12/11/15.
//

#ifndef BREADWALLET_PEERMANAGERCALLBACKS_H
#define BREADWALLET_PEERMANAGERCALLBACKS_H

#include "jni.h"
#include "BRInt.h"
#include "BRPeer.h"
#include "BRMerkleBlock.h"
#include "BRPeerManager.h"
#include "BRTransaction.h"
#include "jni.h"

JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_setPeerManagerCallbacks(JNIEnv *env,
                                                                                       jobject thiz,
                                                                                       jbyteArray peerManager);

#endif //BREADWALLET_PEERMANAGERCALLBACKS_H
