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

#include <BRTransaction.h>
#include <stdlib.h>
#include <assert.h>
#include <malloc.h>
#include <BRInt.h>
#include "BRCoreJni.h"
#include "com_breadwallet_core_BRCoreTransaction.h"

//
// Statically Initialize Java References
//
jclass transactionInputClass;
jmethodID transactionInputConstructor;

jclass transactionOutputClass;
jmethodID transactionOutputConstructor;

static char *JNI_TRANSACTION_IS_REGISTERED_NAME = "isRegistered";
static char *JNI_TRANSACTION_IS_REGISTERED_TYPE = "Z";

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getHash
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCoreTransaction_getHash
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);

    UInt256 transactionHash = transaction->txHash;

    jbyteArray hashByteArray = (*env)->NewByteArray (env, sizeof (UInt256));
    (*env)->SetByteArrayRegion (env, hashByteArray, 0, sizeof (UInt256), (const jbyte *) transactionHash.u8);

    return hashByteArray;
}


/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getVersion
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransaction_getVersion
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    return (jlong) transaction->version;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getInputs
 * Signature: ()[Lcom/breadwallet/core/BRCoreTransactionInput;
 */
JNIEXPORT jobjectArray JNICALL Java_com_breadwallet_core_BRCoreTransaction_getInputs
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);

    size_t inputCount = transaction->inCount;

    jobjectArray inputs = (*env)->NewObjectArray (env, inputCount, transactionInputClass, 0);

    for (int i = 0; i < inputCount; i++) {
        BRTxInput *input = (BRTxInput *) calloc (1, sizeof (BRTxInput));
        transactionInputCopy(input, &transaction->inputs[i]);

        jobject inputObject = (*env)->NewObject (env, transactionInputClass, transactionInputConstructor, (jlong) input);
        (*env)->SetObjectArrayElement (env, inputs, i, inputObject);

        (*env)->DeleteLocalRef (env, inputObject);
    }

    return inputs;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getOutputs
 * Signature: ()[Lcom/breadwallet/core/BRCoreTransactionOutput;
 */
JNIEXPORT jobjectArray JNICALL Java_com_breadwallet_core_BRCoreTransaction_getOutputs
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);

    size_t outputCount = transaction->outCount;

    jobjectArray outputs = (*env)->NewObjectArray (env, outputCount, transactionOutputClass, 0);

    for (int i = 0; i < outputCount; i++) {
        BRTxOutput *output = (BRTxOutput *) calloc (1, sizeof (BRTxOutput));
        transactionOutputCopy(output, &transaction->outputs[i]);

        jobject outputObject = (*env)->NewObject (env, transactionOutputClass, transactionOutputConstructor, (jlong) output);
        (*env)->SetObjectArrayElement (env, outputs, i, outputObject);

        (*env)->DeleteLocalRef (env, outputObject);
    }

    return outputs;
}


/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getLockTime
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransaction_getLockTime
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    return (jlong) transaction->lockTime;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    setLockTime
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCoreTransaction_setLockTime
        (JNIEnv *env, jobject thisObject, jlong lockTime) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    transaction->lockTime = (uint32_t) lockTime;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getBlockHeight
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransaction_getBlockHeight
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    return (jlong) transaction->blockHeight;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getTimestamp
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCoreTransaction_getTimestamp
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    return (jlong) transaction->timestamp;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    setTimestamp
 * Signature: (J)V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_BRCoreTransaction_setTimestamp
        (JNIEnv *env, jobject thisObject, jlong timestamp) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    transaction->timestamp = (uint32_t) timestamp;
}


/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    serialize
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCoreTransaction_serialize
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);

    size_t byteArraySize = BRTransactionSerialize(transaction, NULL, 0);
    jbyteArray  byteArray         = (*env)->NewByteArray (env, (jsize) byteArraySize);
    jbyte      *byteArrayElements = (*env)->GetByteArrayElements (env, byteArray, JNI_FALSE);

    BRTransactionSerialize(transaction, (uint8_t *) byteArrayElements, byteArraySize);

    // Ensure ELEMENTS 'written' back to byteArray
    (*env)->ReleaseByteArrayElements (env, byteArray, byteArrayElements, JNI_COMMIT);

    return byteArray;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    addInput
 * Signature: (Lcom/breadwallet/core/BRCoreTransactionInput;)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCoreTransaction_addInput
        (JNIEnv *env, jobject thisObject, jobject transactionInputObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    BRTxInput *input = (BRTxInput *) getJNIReference (env, transactionInputObject);

    BRTransactionAddInput (transaction, input->txHash, input->index, input->amount,
                           input->script, input->scriptLen,
                           input->signature, input->sigLen,
                           input->sequence);
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    addOutput
 * Signature: (Lcom/breadwallet/core/BRCoreTransactionOutput;)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCoreTransaction_addOutput
        (JNIEnv *env, jobject thisObject, jobject transactionOutputObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference(env, thisObject);
    BRTxOutput *output = (BRTxOutput *) getJNIReference(env, transactionOutputObject);

    BRTransactionAddOutput(transaction, output->amount,
                           output->script, output->scriptLen);
}


/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    shuffleOutputs
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_BRCoreTransaction_shuffleOutputs
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    BRTransactionShuffleOutputs (transaction);
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getSize
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransaction_getSize
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    return (jlong) BRTransactionSize (transaction);
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getStandardFee
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCoreTransaction_getStandardFee
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    return (jlong) BRTransactionStandardFee (transaction);
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    isSigned
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_BRCoreTransaction_isSigned
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    assert (NULL != transaction);
    return (jboolean) BRTransactionIsSigned (transaction);
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    sign
 * Signature: ([Lcom/breadwallet/core/BRCoreKey;I)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCoreTransaction_sign
        (JNIEnv *env, jobject thisObject, jobjectArray keyObjectArray, jint forkId) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);

    size_t keyCount = (*env)->GetArrayLength (env, keyObjectArray);
    BRKey *keys = (BRKey *) calloc (keyCount, sizeof (BRKey));

    for (int index = 0; index < keyCount; index++) {
        jobject keyObject = (*env)->GetObjectArrayElement (env, keyObjectArray, index);
        keys[index] = *(BRKey *) getJNIReference (env, keyObject);

        (*env)->DeleteLocalRef (env, keyObject);
    }
    BRTransactionSign(transaction, forkId, keys, keyCount);

    if (NULL == keys) free (keys);
    return;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    isStandard
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL Java_com_breadwallet_core_BRCoreTransaction_isStandard
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);
    return (jboolean) BRTransactionIsStandard (transaction);
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getReverseHash
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_BRCoreTransaction_getReverseHash
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);

    UInt256 txid = transaction->txHash;
    UInt256 reversedHash = UInt256Reverse(txid);
    return (*env)->NewStringUTF(env, u256hex(reversedHash));
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    getMinOutputAmount
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransaction_getMinOutputAmount
        (JNIEnv *env, jclass thisClass) {
    return TX_MIN_OUTPUT_AMOUNT;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    disposeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCoreTransaction_disposeNative
        (JNIEnv *env, jobject thisObject) {
    BRTransaction *transaction = (BRTransaction *) getJNIReference (env, thisObject);

    // See BRCoreTransaction.isRegistered and dispose().  We will not free a
    // transaction that is registered with the Core.
    if (NULL != transaction) BRTransactionFree(transaction);

}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    initializeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_BRCoreTransaction_initializeNative
        (JNIEnv *env, jclass thisClass) {
    transactionInputClass = (*env)->FindClass(env, "com/breadwallet/core/BRCoreTransactionInput");
    assert (NULL != transactionInputClass);
    transactionInputClass = (*env)->NewGlobalRef (env, transactionInputClass);

    transactionInputConstructor = (*env)->GetMethodID(env, transactionInputClass, "<init>", "(J)V");
    assert (NULL != transactionInputConstructor);

    transactionOutputClass = (*env)->FindClass(env, "com/breadwallet/core/BRCoreTransactionOutput");
    assert(NULL != transactionOutputClass);
    transactionOutputClass = (*env)->NewGlobalRef (env, transactionOutputClass);

    transactionOutputConstructor = (*env)->GetMethodID(env, transactionOutputClass, "<init>", "(J)V");
    assert (NULL != transactionOutputConstructor);
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    createJniCoreTransaction
 * Signature: ([BJJ)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCoreTransaction_createJniCoreTransaction
        (JNIEnv *env, jclass thisClass,
         jbyteArray transactionByteArray,
         jlong blockHeight,
         jlong timestamp) {

    // static native long createJniCoreTransaction (byte[] buffer, long blockHeight, long timeStamp);
    size_t transactionSize = (size_t) (*env)->GetArrayLength (env, transactionByteArray);
    const uint8_t *transactionData = (const uint8_t *) (*env)->GetByteArrayElements (env, transactionByteArray, 0);

    BRTransaction *transaction = BRTransactionParse(transactionData, transactionSize);
    assert (NULL != transaction);

    transaction->blockHeight = (uint32_t) blockHeight;
    transaction->timestamp =(uint32_t) timestamp;

    return (jlong) transaction;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    createJniCoreTransactionSerialized
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransaction_createJniCoreTransactionSerialized
        (JNIEnv *env, jclass thisClass, jbyteArray transactionByteArray) {

    // static native long createJniCoreTransaction (byte[] buffer, long blockHeight, long timeStamp);
    size_t transactionSize = (size_t) (*env)->GetArrayLength (env, transactionByteArray);
    const uint8_t *transactionData = (const uint8_t *) (*env)->GetByteArrayElements (env, transactionByteArray, 0);

    BRTransaction *transaction = BRTransactionParse(transactionData, transactionSize);
    assert (NULL != transaction);

    return (jlong) transaction;
}

/*
 * Class:     com_breadwallet_core_BRCoreTransaction
 * Method:    createJniCoreTransactionEmpty
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreTransaction_createJniCoreTransactionEmpty
        (JNIEnv *env, jclass thisClass) {
    return (jlong) BRTransactionNew();
}
