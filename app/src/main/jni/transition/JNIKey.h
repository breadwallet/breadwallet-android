//
// Created by Mihail Gutan on 10/9/16.
//

#ifndef BREADWALLET_JNIKEY_H
#define BREADWALLET_JNIKEY_H

JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRKey_compactSign(
        JNIEnv *env,
        jobject thiz,
        jbyteArray privKey, jbyteArray data);

#endif //BREADWALLET_JNIKEY_H
