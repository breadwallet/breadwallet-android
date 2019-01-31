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

#include <stdlib.h>
#include <malloc.h>
#include <string.h>
#include <BRBIP32Sequence.h>
#include <assert.h>
#include "BRBIP39Mnemonic.h"
#include "BRBIP32Sequence.h"
#include "BRCoreJni.h"
#include "com_breadwallet_core_BRCoreMasterPubKey.h"

/*
 * Class:     com_breadwallet_core_BRCoreMasterPubKey
 * Method:    serialize
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCoreMasterPubKey_serialize
        (JNIEnv *env, jobject thisObject) {
    BRMasterPubKey *key = (BRMasterPubKey *) getJNIReference (env, thisObject);

    jbyteArray result = (*env)->NewByteArray (env, (jsize) sizeof(BRMasterPubKey));
    (*env)->SetByteArrayRegion(env, result, 0, (jsize) sizeof(BRMasterPubKey), (jbyte *) key);

    return result;
}


/*
 * Class:     com_breadwallet_core_BRCoreMasterPubKey
 * Method:    getPubKey
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCoreMasterPubKey_getPubKey
        (JNIEnv *env, jobject thisObject) {
    BRMasterPubKey *key = (BRMasterPubKey *) getJNIReference (env, thisObject);

    jsize      pubKeyLen = sizeof(key->pubKey);
    jbyteArray pubKey    = (*env)->NewByteArray (env, pubKeyLen);
    (*env)->SetByteArrayRegion (env, pubKey, 0, pubKeyLen, (const jbyte *) key->pubKey);

    return pubKey;
}

/*
 * Class:     com_breadwallet_core_BRCoreMasterPubKey
 * Method:    createPubKey
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCoreMasterPubKey_createPubKey
        (JNIEnv *env, jobject thisObject) {
    BRMasterPubKey *mpk = (BRMasterPubKey *) getJNIReference (env, thisObject);

    // Fill pubKey from MPK
    uint8_t pubKey[33];
    BRBIP32PubKey (pubKey, sizeof(pubKey), *mpk, 0, 0);

    // Allocate and fill BRKey
    BRKey *key = (BRKey *) calloc (1, sizeof (BRKey));
    BRKeySetPubKey(key, pubKey, sizeof(pubKey));

    return (jlong) key;
}

JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCoreMasterPubKey_createJniCoreMasterPubKeyFromPhrase
        (JNIEnv *env, jclass thisClass,
         jbyteArray phrase) {

    // Get the phraseBytes
    jsize phraseLength = (*env)->GetArrayLength (env, phrase);
    jbyte *phraseBytes = (*env)->GetByteArrayElements (env, phrase, 0);

    // The upcoming call to BRBIP39DeriveKey() calls strlen() on the phrase; THUS, a proper
    // zero terminated C-string is required!  There will certainly be trouble if the byte[]
    // phrase has a some randomly placed '0' in it.  Really, a 'String' should be passed to
    // this function.
    //
    // This conversion might not be required if `phrase` is a 'null-terminated byte array'.  But
    // that is a dangerous assumption if violated (buffer overflow errors).
    char phraseString[1 + phraseLength];
    memcpy (phraseString, phraseBytes, phraseLength);
    phraseString[phraseLength] = '\0';

    // UInt512 seed = UINT512_ZERO;
    // BRMasterPubKey mpk = BR_MASTER_PUBKEY_NONE;
    // BRBIP39DeriveKey(seed.u8, "axis husband project any sea patch drip tip spirit tide bring belt", NULL);
    // mpk = BRBIP32MasterPubKey(&seed, sizeof(seed));

    //(*env)->GetArrayLength(env, phrase);
    //jbyte *bytePhrase = (*env)->GetByteArrayElements(env, phrase, 0);
    //UInt512 key = UINT512_ZERO;
    //char *charPhrase = (char *) bytePhrase;
    // BRBIP39DeriveKey(key.u8, charPhrase, NULL);
    // BRMasterPubKey pubKey = BRBIP32MasterPubKey(key.u8, sizeof(key));

    //    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "getMasterPubKey");
//    (*env)->GetArrayLength(env, phrase);
//    jbyte *bytePhrase = (*env)->GetByteArrayElements(env, phrase, 0);
//    UInt512 key = UINT512_ZERO;
//    char *charPhrase = (char *) bytePhrase;
//    BRBIP39DeriveKey(key.u8, charPhrase, NULL);
//    BRMasterPubKey pubKey = BRBIP32MasterPubKey(key.u8, sizeof(key));
//    size_t pubKeySize = sizeof(pubKey);
//    jbyte *pubKeyBytes = (jbyte *) &pubKey;
//    jbyteArray result = (*env)->NewByteArray(env, (jsize) pubKeySize);
//    (*env)->SetByteArrayRegion(env, result, 0, (jsize) pubKeySize, (const jbyte *) pubKeyBytes);
//    (*env)->ReleaseByteArrayElements(env, phrase, bytePhrase, JNI_ABORT);
//    //release everything

    // Derive a UInt512 'BIP39' seed from the phraseString.
    UInt512 seed = UINT512_ZERO;
    BRBIP39DeriveKey(seed.u8, phraseString, NULL);

    BRMasterPubKey pubKey = BRBIP32MasterPubKey(&seed, sizeof(seed));

    // Allocate, then fill, our BRMasterPubKey result with the computed pubKey
    BRMasterPubKey *resKey = (BRMasterPubKey *) calloc (1, sizeof (BRMasterPubKey));
    *resKey = pubKey;

    return (jlong) resKey;
}

/*
 * Class:     com_breadwallet_core_BRCoreMasterPubKey
 * Method:    createJniCoreMasterPubKeyFromPubKey
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCoreMasterPubKey_createJniCoreMasterPubKeyFromSerialization
        (JNIEnv *env, jclass thisClass,
         jbyteArray serialization) {
    jsize serializationLength = (*env)->GetArrayLength (env, serialization);
    jbyte *serializationBytes = (*env)->GetByteArrayElements (env, serialization, 0);
    assert (serializationLength == sizeof(BRMasterPubKey));

    BRMasterPubKey *key = (BRMasterPubKey *) calloc (1, sizeof (BRMasterPubKey));
    memcpy(key, serializationBytes, sizeof(BRMasterPubKey));

    return (jlong) key;
}

/*
 * Class:     com_breadwallet_core_BRCoreMasterPubKey
 * Method:    bip32BitIDKey
 * Signature: ([BILjava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreMasterPubKey_bip32BitIDKey
        (JNIEnv *env, jclass thisClass, jbyteArray seed, jint index, jstring strUri) {
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

/*
 * Class:     com_breadwallet_core_BRCoreMasterPubKey
 * Method:    validateRecoveryPhrase
 * Signature: ([Ljava/lang/String;Ljava/lang/String;)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_BRCoreMasterPubKey_validateRecoveryPhrase
        (JNIEnv *env, jclass thisClass, jobjectArray stringArray, jstring jPhrase) {
    int wordsCount = (*env)->GetArrayLength(env, stringArray);
    char *wordList[wordsCount];

    for (int i = 0; i < wordsCount; i++) {
        jstring string = (jstring) (*env)->GetObjectArrayElement(env, stringArray, i);
        const char *rawString = (*env)->GetStringUTFChars(env, string, 0);

        wordList[i] = malloc(strlen(rawString) + 1);
        strcpy(wordList[i], rawString);
        (*env)->ReleaseStringUTFChars(env, string, rawString);
        (*env)->DeleteLocalRef(env, string);
    }

    const char *str = (*env)->GetStringUTFChars(env, jPhrase, NULL);
    int result = BRBIP39PhraseIsValid((const char **) wordList, str);

    (*env)->ReleaseStringUTFChars(env, jPhrase, str);

    return (jboolean) (result ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_BRCoreMasterPubKey
 * Method:    generatePaperKey
 * Signature: ([B[Ljava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreMasterPubKey_generatePaperKey
        (JNIEnv *env, jclass thisClass, jbyteArray seed, jobjectArray stringArray) {

    int wordsCount = (*env)->GetArrayLength(env, stringArray);
    int seedLength = (*env)->GetArrayLength(env, seed);
    const char **wordList = (const char **) calloc (wordsCount, sizeof (char*));
    assert(seedLength == 16);
    assert(wordsCount == 2048);

    // Copy stringArray elements into workList as char*
    for (int i = 0; i < wordsCount; i++) {
        jstring string = (jstring) (*env)->GetObjectArrayElement(env, stringArray, i);
        const char *rawString = (*env)->GetStringUTFChars(env, string, 0);

        wordList[i] = rawString;
        (*env)->DeleteLocalRef(env, string);
    }

    // Encode into 'result'
    jbyte *byteSeed = (*env)->GetByteArrayElements(env, seed, 0);
    size_t size = BRBIP39Encode(NULL, 0, wordList, (uint8_t *) byteSeed, (size_t) seedLength);
    char result[size];

    size = BRBIP39Encode(result, sizeof(result), wordList, (const uint8_t *) byteSeed,
                         (size_t) seedLength);

    // Release the UTF strings from wordList
    for (int i = 0; i < wordsCount; i++) {
        jstring string = (jstring) (*env)->GetObjectArrayElement(env, stringArray, i);
        (*env)->ReleaseStringUTFChars(env, string, wordList[i]);
        (*env)->DeleteLocalRef(env, string);
    }

    if (NULL != wordList) free (wordList);

    // Return byte[] of 'result'
    jbyteArray bytePhrase = NULL;

    if (size > 0) {
        // The 'result' size INCLUDES the NULL terminator; we don't want that.
        bytePhrase = (*env)->NewByteArray(env, (int) (size - 1));
        (*env)->SetByteArrayRegion(env, bytePhrase, 0, (int) (size - 1), (jbyte *) result);

    }

    return bytePhrase;
}
