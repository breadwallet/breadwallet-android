//
// Created by Mihail Gutan on 12/11/15.
//
#include "jni.h"
#ifndef BREADWALLET_PEERMANAGERCALLBACKS_H
#define BREADWALLET_PEERMANAGERCALLBACKS_H


JNIEXPORT void Java_com_breadwallet_wallet_BRPeerManager_setPeerManagerCallbacks(JNIEnv *env,
                                                                                       jobject thiz,
                                                                                       jbyteArray peerManager);

#endif //BREADWALLET_PEERMANAGERCALLBACKS_H
