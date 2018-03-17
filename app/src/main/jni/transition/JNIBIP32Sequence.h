//
// Created by Mihail Gutan on 1/24/17.
//

#ifndef BREADWALLET_JNIBIP32SEQUENCE_H
#define BREADWALLET_JNIBIP32SEQUENCE_H

#ifdef __cplusplus
extern "C" {
#endif


JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRBIP32Sequence_bip32BitIDKey(JNIEnv *env,
                                                                             jobject thiz,
                                                                             jbyteArray seed,
                                                                             jint index,
                                                                             jstring strUri);


#ifdef __cplusplus
}
#endif

#endif //BREADWALLET_JNIBIP32SEQUENCE_H
