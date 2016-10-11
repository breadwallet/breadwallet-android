//
// Created by Mihail Gutan on 10/11/16.
//

#ifndef BREADWALLET_JNIBASE58_H
#define BREADWALLET_JNIBASE58_H

JNIEXPORT jstring JNICALL Java_com_jniwrappers_BRBase58_base58Encode(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data);

#endif //BREADWALLET_JNIBASE58_H
