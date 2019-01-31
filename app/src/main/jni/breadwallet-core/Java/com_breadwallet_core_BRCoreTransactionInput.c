//  Created by Ed Gamble on 1/31/2018
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
#include <malloc.h>
#include <assert.h>
#include <string.h>
#include <BRInt.h>
#include <BRTransaction.h>
#include "com_breadwallet_core_BRCoreTransactionInput.h"

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    createTransactionInput
 * Signature: ([BJJ[B[BJ)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_createTransactionInput
        (JNIEnv *env, jclass thisClass, jbyteArray hashByteArray, jlong index, jlong amount,
         jbyteArray scriptByteArray,
         jbyteArray signatureByteArray,
         jlong sequence) {
    BRTxInput *input = (BRTxInput *) calloc(1, sizeof(BRTxInput));

    size_t hashLen = (size_t) (*env)->GetArrayLength(env, hashByteArray);
    const uint8_t *hashData = (const uint8_t *) (*env)->GetByteArrayElements(env, hashByteArray, 0);
    assert (32 == hashLen);

    input->txHash = UInt256Get((const void *) hashData);
    input->index = (uint32_t) index;
    input->amount = (uint32_t) amount;

    // script
    input->script = NULL;
    size_t scriptLen = (size_t) (*env)->GetArrayLength(env, scriptByteArray);
    const uint8_t *script = (const uint8_t *)
            (0 == scriptLen
             ? NULL
             : (*env)->GetByteArrayElements(env, scriptByteArray, 0));
    BRTxInputSetScript(input, script, scriptLen);

    // signature
    input->signature = NULL;
    size_t signatureLen = (size_t) (*env)->GetArrayLength(env, signatureByteArray);
    const uint8_t *signature = (const uint8_t *)
            (0 == signatureLen
             ? NULL
             : (*env)->GetByteArrayElements(env, signatureByteArray, 0));
    BRTxInputSetSignature(input, signature, signatureLen);

    input->sequence = (uint32_t) (sequence == -1 ? TXIN_SEQUENCE : sequence);

    return (jlong) input;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    getAddress
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_getAddress
        (JNIEnv *env, jobject thisObject) {
    BRTxInput *input = (BRTxInput *) getJNIReference (env, thisObject);
    
    size_t addressLen = sizeof (input->address);
    char address[1 + addressLen];
    memcpy (address, input->address, addressLen);
    address[addressLen] = '\0';

    return (*env)->NewStringUTF (env, address);
}

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    setAddress
 * Signature: (Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_setAddress
        (JNIEnv *env, jobject thisObject , jstring addressObject) {
    BRTxInput *input = (BRTxInput *) getJNIReference (env, thisObject);
    
    size_t addressLen = sizeof (input->address);

    size_t addressDataLen = (size_t) (*env)->GetStringLength (env, addressObject);
    const jchar *addressData = (*env)->GetStringChars (env, addressObject, 0);
    assert (addressDataLen <= addressLen);

    memset (input->address, '\0', addressLen);
    memcpy (input->address, addressData, addressDataLen);

}

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    getHash
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_getHash
        (JNIEnv *env, jobject thisObject) {
    BRTxInput *input = (BRTxInput *) getJNIReference (env, thisObject);

    size_t hashLen = sizeof (UInt256);
    jbyteArray hashByteArray = (*env)->NewByteArray (env, hashLen);
    (*env)->SetByteArrayRegion (env, hashByteArray, 0, (jsize) hashLen, 
                                (const jbyte *) input->txHash.u8);
    
    return hashByteArray;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    getIndex
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_getIndex
        (JNIEnv *env, jobject thisObject) {
    BRTxInput *input = (BRTxInput *) getJNIReference (env, thisObject);
    return (jlong) input->index;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    getAmount
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_getAmount
        (JNIEnv *env, jobject thisObject) {
    BRTxInput *input = (BRTxInput *) getJNIReference (env, thisObject);
    return (jlong) input->amount;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    getScript
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_getScript
        (JNIEnv *env, jobject thisObject) {
    BRTxInput *input = (BRTxInput *) getJNIReference (env, thisObject);
    
    jbyteArray scriptByteArray = (*env)->NewByteArray (env, (jsize) input->scriptLen);
    (*env)->SetByteArrayRegion (env, scriptByteArray, 0, (jsize) input->scriptLen,
                                (const jbyte *) input->script);
    
    return scriptByteArray;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    getSignature
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_getSignature
        (JNIEnv *env, jobject thisObject) {
    BRTxInput *input = (BRTxInput *) getJNIReference (env, thisObject);

    jbyteArray signatureByteArray = (*env)->NewByteArray (env, (jsize) input->sigLen);
    (*env)->SetByteArrayRegion (env, signatureByteArray, 0, (jsize) input->sigLen,
                                (const jbyte *) input->signature);

    return signatureByteArray;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransactionInput
 * Method:    getSequence
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransactionInput_getSequence
        (JNIEnv *env, jobject thisObject) {
    BRTxInput *input = (BRTxInput *) getJNIReference (env, thisObject);
    return (jlong) input->sequence;
}
