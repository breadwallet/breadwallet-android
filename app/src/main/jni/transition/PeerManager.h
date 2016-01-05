//
// Created by Mihail Gutan on 12/11/15.
//

#ifndef BREADWALLET_PEERMANAGER_H

#include "jni.h"
#include "BRPeerManager.h"
#include "BRPeer.h"
#include "WalletCallbacks.h"
#include "BRInt.h"
#include <android/log.h>
#include "BRMerkleBlock.h"
#include "BRWallet.h"

#define BREADWALLET_PEERMANAGER_H

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRPeerManager_connect(JNIEnv *env, jobject thiz,
                                                                       jbyteArray walletBuff,
                                                                       jlong earliestKeyTime,
                                                                       jobjectArray blocks,
                                                                       jlong blocksCount,
                                                                       jobjectArray peers,
                                                                       jlong peersCount);


#endif //BREADWALLET_PEERMANAGER_H
