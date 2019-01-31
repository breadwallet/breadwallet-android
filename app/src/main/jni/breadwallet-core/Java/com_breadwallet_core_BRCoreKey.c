//  Created by Ed Gamble on 1/23/2018
//  Copyright (c) 2018 breadwallet LLC.
//
//  Permission is hereby granted, free of charge, to any person obtaining a copy
//  of this software and associated documentation files (the "Software"), to deal
//  in the Software without restriction, including without limitation the rights
//  to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
//  copies of the Software, and to permit persons to whom the Software is
//  furnished to do so, subject to the following conditions:
//
//  The above copyright notice and this permission notice shall be included in
//  all copies or substantial portions of the Software.
//
//  THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
//  IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
//  FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
//  AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
//  LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
//  OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
//  THE SOFTWARE.

#include "BRCoreJni.h"
#include <BRKey.h>
#include <BRAddress.h>
#include <stdlib.h>
#include <malloc.h>
#include <assert.h>
#include <BRBase58.h>
#include <BRBIP39Mnemonic.h>
#include <BRBIP38Key.h>
#include <BRBIP32Sequence.h>
#include "com_breadwallet_core_BRCoreKey.h"

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    getSecret
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreKey_getSecret
        (JNIEnv *env, jobject thisObject) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    size_t secretLen = sizeof (UInt256);

    jbyteArray result = (*env)->NewByteArray(env, (jsize) secretLen);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) secretLen,
                               (const jbyte *) key->secret.u8);
    return result;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    getPubKey
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreKey_getPubKey
        (JNIEnv *env, jobject thisObject) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    // Actually get the pubKey...
    BRKeyPubKey(key, NULL, 0);

    // ... now copy it.
    size_t pubKeyLen = 65 * sizeof(uint8_t);
    jbyteArray result = (*env)->NewByteArray(env, (jsize) pubKeyLen);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) pubKeyLen,
                               (const jbyte *) key->pubKey);
    return result;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    getCompressed
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_breadwallet_core_BRCoreKey_getCompressed
        (JNIEnv *env, jobject thisObject) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);
    return (jint) key->compressed;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    getPrivKey
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_BRCoreKey_getPrivKey
        (JNIEnv *env, jobject thisObject) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    size_t privKeyLen = (size_t) BRKeyPrivKey(key, NULL, 0);
    char privKey[privKeyLen + 1];
    BRKeyPrivKey(key, privKey, privKeyLen);
    privKey[privKeyLen] = '\0';

    return (*env)->NewStringUTF (env, privKey);
}

// Unused
/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    getBase58EncodedPublicKey
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCoreKey_getBase58EncodedPublicKey
        (JNIEnv *env, jobject thisObject) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    // Extract pubKey
    size_t len = BRKeyPubKey(key, NULL, 0);
    uint8_t pubKey[len];
    BRKeyPubKey(key, &pubKey, len);

    // Encode pubKey
    size_t strLen = BRBase58Encode(NULL, 0, pubKey, len);
    char base58string[strLen];
    BRBase58Encode(base58string, strLen, pubKey, len);

    return (*env)->NewStringUTF (env, base58string);
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    getSeedFromPhrase
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreKey_getSeedFromPhrase
        (JNIEnv *env, jclass thisClass, jbyteArray phrase) {

    jbyte *bytePhrase = (*env)->GetByteArrayElements(env, phrase, 0);
    UInt512 key = UINT512_ZERO;
    char *charPhrase = (char *) bytePhrase;
    BRBIP39DeriveKey(key.u8, charPhrase, NULL);

    jbyteArray result = (*env)->NewByteArray(env, (jsize) sizeof(key));
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) sizeof(key), (jbyte *) &key);

    return result;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    getAuthPrivKeyForAPI
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCoreKey_getAuthPrivKeyForAPI
        (JNIEnv *env, jclass thisClass, jbyteArray seed) {
    //__android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "getAuthPrivKeyForAPI");
    jbyte *bytesSeed = (*env)->GetByteArrayElements(env, seed, 0);
    size_t seedLen = (size_t) (*env)->GetArrayLength(env, seed);
    //__android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "seedLen: %d", (int) seedLen);

    BRKey key;
    BRBIP32APIAuthKey(&key, bytesSeed, seedLen);
    char rawKey[BRKeyPrivKey(&key, NULL, 0)];
    BRKeyPrivKey(&key, rawKey, sizeof(rawKey));

    //    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "rawKey: %s", rawKey);
    jbyteArray result = (*env)->NewByteArray(env, (jsize) sizeof(rawKey));
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) sizeof(rawKey), (jbyte *) rawKey);
    return result;

}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    getAuthPublicKeyForAPI
 * Signature: ([B)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_BRCoreKey_getAuthPublicKeyForAPI
        (JNIEnv *env, jclass thisClass, jbyteArray privKey) {
    //__android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "getAuthPublicKeyForAPI");
    jbyte *bytePrivKey = (*env)->GetByteArrayElements(env, privKey, 0);
    BRKey key;
    BRKeySetPrivKey(&key, (const char *) bytePrivKey);

    size_t len = BRKeyPubKey(&key, NULL, 0);
    uint8_t pubKey[len];
    BRKeyPubKey(&key, &pubKey, len);
    size_t strLen = BRBase58Encode(NULL, 0, pubKey, len);
    char base58string[strLen];
    BRBase58Encode(base58string, strLen, pubKey, len);

    return (*env)->NewStringUTF(env, base58string);
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    decryptBip38Key
 * Signature: (Ljava/lang/String;Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_BRCoreKey_decryptBip38Key
        (JNIEnv *env, jclass thisClass, jstring privKey, jstring pass) {
    //__android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "decryptBip38Key");

    BRKey key;
    const char *rawPrivKey = (*env)->GetStringUTFChars(env, privKey, NULL);
    const char *rawPass = (*env)->GetStringUTFChars(env, pass, NULL);
    int result = BRKeySetBIP38Key(&key, rawPrivKey, rawPass);

    if (result) {
        char pk[BRKeyPrivKey(&key, NULL, 0)];

        BRKeyPrivKey(&key, pk, sizeof(pk));
        return (*env)->NewStringUTF(env, pk);
    } else return (*env)->NewStringUTF(env, "");
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    createJniCoreKey
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCoreKey_createJniCoreKey
        (JNIEnv *env, jclass thisClass) {
    BRKey *key = (BRKey *) calloc (1, sizeof(BRKey));
    return (jlong) key;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    createCoreKeyForBIP32
 * Signature: ([BJJ)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreKey_createCoreKeyForBIP32
        (JNIEnv *env, jclass thisClass,
         jbyteArray seedByteArray,
         jlong chain,
         jlong index) {
    BRKey *key = (BRKey *) calloc (1, sizeof(BRKey));

    size_t seedLen = (size_t) (*env)->GetArrayLength (env, seedByteArray);
    const void *seed = (const void *) (*env)->GetByteArrayElements (env, seedByteArray, 0);

    BRBIP32PrivKey (key, seed, seedLen, (uint32_t) chain, (uint32_t) index);
    return (jlong) key;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    setPrivKey
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_breadwallet_core_BRCoreKey_setPrivKey
        (JNIEnv *env, jobject thisObject, jstring privKeyString) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);
    const char *privKey = (*env)->GetStringUTFChars (env, privKeyString, 0);

    return (jboolean) (1 == BRKeySetPrivKey(key, privKey)
                       ? JNI_TRUE
                       : JNI_FALSE);

}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    setSecret
 * Signature: ([BZ)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_BRCoreKey_setSecret
        (JNIEnv *env, jobject thisObject, jbyteArray secretByteArray, jboolean compressed) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    const char *secretKey = (const char *)
            (*env)->GetByteArrayElements(env, secretByteArray, 0);

    return (jboolean) (1 == BRKeySetSecret(key, (const UInt256 *) secretKey, JNI_TRUE == compressed)
                       ? JNI_TRUE
                       : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    compactSign
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreKey_compactSign
        (JNIEnv *env, jobject thisObject, jbyteArray dataByteArray) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    uint8_t *data = (uint8_t *) (*env)->GetByteArrayElements(env, dataByteArray, 0);
    UInt256 md32 = UInt256Get(data);

    size_t sigLen = BRKeyCompactSign(key, NULL, 0, md32);
    uint8_t compactSig[sigLen];
    sigLen = BRKeyCompactSign(key, compactSig, sizeof(compactSig), md32);

    jbyteArray result = (*env)->NewByteArray(env, (jsize) sigLen);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) sigLen, (const jbyte *) compactSig);

    return result;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    encryptNative
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreKey_encryptNative
        (JNIEnv *env, jobject thisObject, jbyteArray dataByteArray, jbyteArray nonceByteArray) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    jbyte *data = (*env)->GetByteArrayElements(env, dataByteArray, NULL);
    jsize dataSize = (*env)->GetArrayLength(env, dataByteArray);

    jbyte *nonce = (*env)->GetByteArrayElements(env, nonceByteArray, NULL);
    jsize nonceSize = (*env)->GetArrayLength(env, nonceByteArray);

    uint8_t out[16 + dataSize];

    size_t outSize = BRChacha20Poly1305AEADEncrypt(out, sizeof(out), key,
                                                   (uint8_t *) nonce,
                                                   (uint8_t *) data,
                                                   (size_t) dataSize,
                                                   NULL,
                                                   0);

    jbyteArray result = (*env)->NewByteArray(env, (jsize) outSize);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) outSize, (const jbyte *) out);

    (*env)->ReleaseByteArrayElements(env, dataByteArray, data, 0);
    (*env)->ReleaseByteArrayElements(env, nonceByteArray, nonce, 0);

    return result;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    decryptNative
 * Signature: ([B[B)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreKey_decryptNative
        (JNIEnv *env, jobject thisObject, jbyteArray dataByteArray, jbyteArray nonceByteArray) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    jbyte *data = (*env)->GetByteArrayElements(env, dataByteArray, NULL);
    jsize dataSize = (*env)->GetArrayLength(env, dataByteArray);

    jbyte *nonce = (*env)->GetByteArrayElements(env, nonceByteArray, NULL);
    jsize nonceSize = (*env)->GetArrayLength(env, nonceByteArray);

    uint8_t out[dataSize];

    size_t outSize = BRChacha20Poly1305AEADDecrypt(out, sizeof(out), key,
                                                   (uint8_t *) nonce,
                                                   (uint8_t *) data,
                                                   (size_t) (dataSize),
                                                   NULL,
                                                   0);

    if (sizeof(out) == 0) return NULL;

    jbyteArray result = (*env)->NewByteArray(env, (jsize) outSize);
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) outSize, (const jbyte *) out);

    (*env)->ReleaseByteArrayElements(env, dataByteArray, data, 0);
    (*env)->ReleaseByteArrayElements(env, nonceByteArray, nonce, 0);

    return result;
}
/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    address
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCoreKey_address
        (JNIEnv *env, jobject thisObject) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    BRAddress address = BR_ADDRESS_NONE;
    BRKeyAddress (key, address.s, sizeof(address));
    assert(address.s[0] != '\0');

    return (*env)->NewStringUTF(env, address.s);
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    isValidBitcoinPrivateKey
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_BRCoreKey_isValidBitcoinPrivateKey
        (JNIEnv *env, jclass thisClass, jstring stringObject) {
    const char *privKey = (*env)->GetStringUTFChars(env, stringObject, NULL);
    int result = BRPrivKeyIsValid(privKey);

    (*env)->ReleaseStringUTFChars(env, stringObject, privKey);
    return (jboolean) (1 == result ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    isValidBitcoinBIP38Key
 * Signature: (Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL Java_com_breadwallet_core_BRCoreKey_isValidBitcoinBIP38Key
        (JNIEnv *env, jclass thisClass, jstring stringObject) {
    const char *privKey = (*env)->GetStringUTFChars(env, stringObject, 0);
    int result = BRBIP38KeyIsValid(privKey);

    (*env)->ReleaseStringUTFChars(env, stringObject, privKey);
    return (jboolean) (1 == result ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    encodeSHA256
 * Signature: (Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreKey_encodeSHA256
        (JNIEnv *env, jclass thisClass,
         jstring messageString) {
    uint8_t md[32];

    size_t      messageLen = (size_t) (*env)->GetStringLength (env, messageString);
    const void *message    = (*env)->GetStringUTFChars (env, messageString, 0);

    BRSHA256 (md, message, messageLen);

    jbyteArray result = (*env)->NewByteArray (env, 32);
    (*env)->SetByteArrayRegion (env, result, 0, 32, (const jbyte *) md);

    return result;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    sign
 * Signature: ([B)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreKey_sign
        (JNIEnv *env, jobject thisObject,
         jbyteArray messageDigestByteArray) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    size_t messageDigestLen = (size_t) (*env)->GetArrayLength (env, messageDigestByteArray);
    assert (32 == messageDigestLen);
    const uint8_t *messageDigest = (const uint8_t *) (*env)->GetByteArrayElements (env, messageDigestByteArray, 0);

    UInt256 md = UInt256Get(messageDigest);

    uint8_t signature[256];
    size_t signatureLen = BRKeySign(key, signature, sizeof(signature), md);
    assert (signatureLen <= 256);

    jobject result = (*env)->NewByteArray (env, (jsize) signatureLen);
    (*env)->SetByteArrayRegion (env, result, 0, (jsize) signatureLen, (const jbyte *) signature);

    return result;
}

/*
 * Class:     com_breadwallet_core_BRCoreKey
 * Method:    verify
 * Signature: ([B[B)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_BRCoreKey_verify
        (JNIEnv *env, jobject thisObject,
         jbyteArray messageDigestByteArray,
         jbyteArray signatureByteArray) {
    BRKey *key = (BRKey *) getJNIReference(env, thisObject);

    size_t messageDigestLen = (size_t) (*env)->GetArrayLength(env, messageDigestByteArray);
    assert (32 == messageDigestLen);
    UInt256 *messageDigest = (UInt256 *) (*env)->GetByteArrayElements(env, messageDigestByteArray, 0);

    size_t signatureLen = (size_t) (*env)->GetArrayLength(env, signatureByteArray);
    const void *signature = (const void *) (*env)->GetByteArrayElements(env, signatureByteArray, 0);

    return (jboolean) (1 == BRKeyVerify(key, *messageDigest, signature, signatureLen)
                       ? JNI_TRUE
                       : JNI_FALSE);
}
