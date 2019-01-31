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
#include <stdlib.h>
#include <assert.h>
#include "BRAddress.h"
#include "com_breadwallet_core_BRCoreAddress.h"
#include "bcash/BRBCashAddr.h"

/*
 * Class:     com_breadwallet_core_BRCoreAddress
 * Method:    createCoreAddress
 * Signature: (Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCoreAddress_createCoreAddress
        (JNIEnv *env, jclass thisClass, jstring stringObject) {
    BRAddress *address = (BRAddress *) calloc (1, sizeof (BRAddress));

    // If given NULL, just return an empty address
    if ((*env)->IsSameObject (env, stringObject, NULL))
        return (jlong) address;

    // ... otherwise fill in address
    size_t stringLen = (size_t) (*env)->GetStringLength (env, stringObject);
    size_t stringLenMax = sizeof (address->s) - 1;

    // Do not overflow address->s
    if (stringLen > stringLenMax)
        stringLen = stringLenMax;

    const char *stringChars = (const char *) (*env)->GetStringUTFChars (env, stringObject, 0);
    memcpy(address->s, stringChars, stringLen);

    return (jlong) address;
}

/*
 * Class:     com_breadwallet_core_BRCoreAddress
 * Method:    createCoreAddressFromScriptPubKey
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreAddress_createCoreAddressFromScriptPubKey
        (JNIEnv *env, jclass thisClass, jbyteArray scriptByteArray) {
    BRAddress *address = (BRAddress *) calloc (1, sizeof (BRAddress));

    size_t scriptLen = (size_t) (*env)->GetArrayLength (env, scriptByteArray);
    const uint8_t *script = (const uint8_t *) (*env)->GetByteArrayElements (env, scriptByteArray, 0);

    // TODO: Error handling
    BRAddressFromScriptPubKey(address->s, sizeof(address->s), script, scriptLen);

    return (jlong) address;
}

/*
 * Class:     com_breadwallet_core_BRCoreAddress
 * Method:    createCoreAddressFromScriptSignature
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreAddress_createCoreAddressFromScriptSignature
        (JNIEnv *env, jclass thisClass, jbyteArray scriptByteArray) {
    BRAddress *address = (BRAddress *) calloc(1, sizeof(BRAddress));

    size_t scriptLen = (size_t) (*env)->GetArrayLength(env, scriptByteArray);
    const uint8_t *script = (const uint8_t *) (*env)->GetByteArrayElements(env, scriptByteArray, 0);

    // TODO: Error handling
    BRAddressFromScriptSig(address->s, sizeof(address->s), script, scriptLen);

    return (jlong) address;
}

/*
 * Class:     com_breadwallet_core_BRCoreAddress
 * Method:    stringify
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCoreAddress_stringify
        (JNIEnv *env, jobject thisObject) {
    BRAddress *address = (BRAddress *) getJNIReference (env, thisObject);
    return (*env)->NewStringUTF (env, address->s);
}

/*
 * Class:     com_breadwallet_core_BRCoreAddress
 * Method:    isValid
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_BRCoreAddress_isValid
        (JNIEnv *env, jobject thisObject) {
    BRAddress *address = (BRAddress *) getJNIReference(env, thisObject);
    return (jboolean) (BRAddressIsValid(address->s)
                       ? JNI_TRUE
                       : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_BRCoreAddress
 * Method:    getPubKeyScript
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreAddress_getPubKeyScript
        (JNIEnv *env, jobject thisObject) {
    BRAddress *address = (BRAddress *) getJNIReference(env, thisObject);

    size_t pubKeyLen = BRAddressScriptPubKey(NULL, 0, address->s);
    uint8_t pubKey[pubKeyLen];
    BRAddressScriptPubKey(pubKey, pubKeyLen, address->s);

    jbyteArray result = (*env)->NewByteArray (env, (jsize) pubKeyLen);
    (*env)->SetByteArrayRegion (env, result, 0, (jsize) pubKeyLen, (const jbyte *) pubKey);

    return result;
}

/*
 * Class:     com_breadwallet_core_BRCoreAddress
 * Method:    bcashDecodeBitcoin
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_BRCoreAddress_bcashDecodeBitcoin
        (JNIEnv *env, jclass thisClass, jstring bcashAddrString ) {
    const char *bcashAddr = (*env)->GetStringUTFChars (env, bcashAddrString, 0);

    char bitcoinAddr[36 + 1];

    // returns the number of bytes written to bitcoinAddr36 (maximum of 36)
    size_t bitcoinAddrLen = BRBCashAddrDecode (bitcoinAddr, bcashAddr);
    bitcoinAddr[bitcoinAddrLen] = '\0';

    return (*env)->NewStringUTF (env, bitcoinAddr);
}

/*
 * Class:     com_breadwallet_core_BRCoreAddress
 * Method:    bcashEncodeBitcoin
 * Signature: (Ljava/lang/String;)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_BRCoreAddress_bcashEncodeBitcoin
        (JNIEnv *env, jclass thisClass, jstring bitcoinAddrString) {
    const char *bitcoinAddr = (*env)->GetStringUTFChars (env, bitcoinAddrString, 0);

    char bcashAddr[55 + 1];

    // returns the number of bytes written to bCashAddr55 (maximum of 55)

    size_t bcashAddrLen = BRBCashAddrEncode(bcashAddr, bitcoinAddr);
    bcashAddr[bcashAddrLen] = '\0';

    return (*env)->NewStringUTF (env, bcashAddr);
}
