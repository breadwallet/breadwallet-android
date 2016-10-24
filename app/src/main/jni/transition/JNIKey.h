//
// Created by Mihail Gutan on 10/9/16.
//

#ifndef BREADWALLET_JNIKEY_H
#define BREADWALLET_JNIKEY_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRKey_compactSign(
        JNIEnv *env,
        jobject thiz,
        jbyteArray data);

JNIEXPORT void JNICALL Java_com_jniwrappers_BRKey_setPrivKey(
        JNIEnv *env,
        jobject thiz,
        jbyteArray privKey);

#ifdef __cplusplus
}
#endif

#endif //BREADWALLET_JNIKEY_H
