//
// Created by Mihail Gutan on 1/24/17.
//

#include <jni.h>
#include <BRKey.h>
#include <BRBIP32Sequence.h>
#include "JNIBIP32Sequence.h"

JNIEXPORT jbyteArray JNICALL Java_com_jniwrappers_BRBIP32Sequence_bip32BitIDKey(JNIEnv *env, jobject thiz,
                                                                      jbyteArray seed, jint index, jstring strUri) {
    int seedLength = (*env)->GetArrayLength(env, seed);
    const char *uri = (*env)->GetStringUTFChars(env, strUri, NULL);
    jbyte *byteSeed = (*env)->GetByteArrayElements(env, seed, 0);
    BRKey key;

    BRBIP32BitIDKey(&key, byteSeed, (size_t) seedLength, (uint32_t) index, uri);

    char rawKey[BRKeyPrivKey(&key, NULL, 0)];
    BRKeyPrivKey(&key, rawKey, sizeof(rawKey));
    jbyteArray result = (*env)->NewByteArray(env, (jsize) sizeof(rawKey));
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) sizeof(rawKey), (jbyte *) rawKey);

    return result;
}