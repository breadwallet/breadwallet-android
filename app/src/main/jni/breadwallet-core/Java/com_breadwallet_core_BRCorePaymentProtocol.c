//  Created by Ed Gamble on 1/30/2018
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

#include <BRPaymentProtocol.h>
#include <stdlib.h>
#include <malloc.h>
#include <assert.h>
#include "BRCoreJni.h"
#include "com_breadwallet_core_BRCorePaymentProtocol.h"

//
// Statically Initialize Java References
//
jclass transactionClass;
jmethodID transactionConstructor;

jclass transactionInputClass;
jmethodID transactionInputConstructor;

jclass transactionOutputClass;
jmethodID transactionOutputConstructor;

static void commonStaticInitialize(JNIEnv *env) {
    //
    transactionClass = (*env)->FindClass(env, "com/breadwallet/core/BRCoreTransaction");
    assert (NULL != transactionClass);
    transactionClass = (*env)->NewGlobalRef(env, transactionClass);

    transactionConstructor = (*env)->GetMethodID(env, transactionClass, "<init>", "(J)V");
    assert (NULL != transactionConstructor);

    //
    transactionInputClass = (*env)->FindClass(env, "com/breadwallet/core/BRCoreTransactionInput");
    assert (NULL != transactionInputClass);
    transactionInputClass = (*env)->NewGlobalRef(env, transactionInputClass);

    transactionInputConstructor = (*env)->GetMethodID(env, transactionInputClass, "<init>", "(J)V");
    assert (NULL != transactionInputConstructor);

    //
    transactionOutputClass = (*env)->FindClass(env, "com/breadwallet/core/BRCoreTransactionOutput");
    assert(NULL != transactionOutputClass);
    transactionOutputClass = (*env)->NewGlobalRef(env, transactionOutputClass);

    transactionOutputConstructor = (*env)->GetMethodID(env, transactionOutputClass, "<init>", "(J)V");
    assert (NULL != transactionOutputConstructor);
}

// ======================
//
// Request
//


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getNetwork
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getNetwork
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);
    return (*env)->NewStringUTF (env, request->details->network);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getOutputs
 * Signature: ()[Lcom/breadwallet/core/BRCoreTransactionOutput;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getOutputs
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);

    size_t outputCount = request->details->outCount;

    jobjectArray outputs = (*env)->NewObjectArray (env, outputCount, transactionOutputClass, 0);

    for (int i = 0; i < outputCount; i++) {
        BRTxOutput *output = (BRTxOutput *) calloc (1, sizeof (BRTxOutput));
        transactionOutputCopy (output, &request->details->outputs[i]);

        jobject outputObject = (*env)->NewObject (env, transactionOutputClass, transactionOutputConstructor, (jlong) output);
        (*env)->SetObjectArrayElement (env, outputs, i, outputObject);

        (*env)->DeleteLocalRef (env, outputObject);
    }

    return outputs;

}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getTime
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getTime
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);
    return (jlong) request->details->time;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getExpires
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getExpires
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);
    return (jlong) request->details->expires;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getMemo
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getMemo
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);
    return (*env)->NewStringUTF (env, request->details->memo);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getPaymentURL
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getPaymentURL
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);
    return (*env)->NewStringUTF (env, request->details->paymentURL);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getMerchantData
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getMerchantData
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);

    jbyteArray merchantData = (*env)->NewByteArray (env, (jsize) request->details->merchDataLen);
    (*env)->SetByteArrayRegion (env, merchantData, 0, (jsize) request->details->merchDataLen,
                                (const jbyte *) request->details->merchantData);
    return merchantData;
}


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getVersion
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getVersion
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);
    return request->version;
}


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getPKIType
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getPKIType
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);
    return (*env)->NewStringUTF (env, request->pkiType);
}


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getPKIData
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getPKIData
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);

    jbyteArray pkiData = (*env)->NewByteArray (env, (jsize) request->pkiDataLen);
    (*env)->SetByteArrayRegion (env, pkiData, 0, (jsize) request->pkiDataLen,
                                (const jbyte *) request->pkiData);
    return pkiData;
}


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getSignature
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getSignature
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);

    jbyteArray signatureData = (*env)->NewByteArray (env, (jsize) request->sigLen);
    (*env)->SetByteArrayRegion (env, signatureData, 0, (jsize) request->sigLen,
                                (const jbyte *) request->signature);
    return signatureData;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getDigest
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getDigest
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);

    size_t digestCount = BRPaymentProtocolRequestDigest (request, NULL, 0);
    jbyteArray digestData = (*env)->NewByteArray (env, digestCount);
    uint8_t *digest = (uint8_t *) (*env)->GetByteArrayElements (env, digestData, 0);

    BRPaymentProtocolRequestDigest (request, digest, digestCount);
    (*env)->SetByteArrayRegion (env, digestData, 0, digestCount, (const jbyte *) digest);
    return digestData;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    getCerts
 * Signature: ()[[B
 */
JNIEXPORT jobjectArray JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolRequest_getCerts
        (JNIEnv *env, jobject thisObject){
    BRPaymentProtocolRequest *request =
            (BRPaymentProtocolRequest *) getJNIReference(env, thisObject);

    size_t numberOfCerts = 0;
    while (0 != BRPaymentProtocolRequestCert (request, NULL, 0, numberOfCerts))
        numberOfCerts++;

    jbyteArray byteArray  = (*env)->NewByteArray (env, 0);
    jclass byteArrayClass = (*env)->GetObjectClass (env, byteArray);
    (*env)->DeleteLocalRef (env, byteArray);

    jobjectArray result = (*env)->NewObjectArray (env, (jsize) numberOfCerts, byteArrayClass, 0);

    (*env)->DeleteLocalRef (env, byteArrayClass);

    for (size_t index = 0; index < numberOfCerts; index++) {
        size_t certLen = (size_t) BRPaymentProtocolRequestCert (request, NULL, 0, index);
        jbyteArray certByteArray = (*env)->NewByteArray (env, (jsize) certLen);
        uint8_t *certData = (uint8_t *) (*env)->GetByteArrayElements (env, certByteArray, 0);

        BRPaymentProtocolRequestCert (request, certData, certLen, index);
        (*env)->SetByteArrayRegion (env, certByteArray, 0, (jsize) certLen, (jbyte *) certData);

        (*env)->SetObjectArrayElement (env, result, (jsize) index, certByteArray);
        (*env)->DeleteLocalRef (env, certByteArray);
    }

    return result;

}


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    createPaymentProtocolRequest
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_createPaymentProtocolRequest
        (JNIEnv *env, jclass thisClass, jbyteArray dataByteArray) {

    size_t dataLen = (*env)->GetArrayLength(env, dataByteArray);
    const uint8_t *data = (uint8_t *) (*env)->GetByteArrayElements(env, dataByteArray, 0);
    return (jlong) BRPaymentProtocolRequestParse(data, dataLen);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    serialize
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_serialize
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request = (BRPaymentProtocolRequest *) getJNIReference (env, thisObject);

    size_t dataLen = BRPaymentProtocolRequestSerialize (request, NULL, 0);
    uint8_t *data = (uint8_t *) malloc (dataLen);
    BRPaymentProtocolRequestSerialize (request, data, dataLen);

    jbyteArray dataByteArray = (*env)->NewByteArray (env, dataLen);
    (*env)->SetByteArrayRegion (env, dataByteArray, 0, dataLen, (jbyte *) data);

    return dataByteArray;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    disposeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolRequest_disposeNative
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolRequest *request = (BRPaymentProtocolRequest *) getJNIReference (env, thisObject);
    if (NULL != request) BRPaymentProtocolRequestFree(request);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolRequest
 * Method:    initializeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolRequest_initializeNative
        (JNIEnv *env, jclass thisClass) {
    commonStaticInitialize(env);
}

// ======================
//
// Payment
//
/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolPayment
 * Method:    getMerchantData
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolPayment_getMerchantData
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolPayment *payment =
            (BRPaymentProtocolPayment *) getJNIReference (env, thisObject);

    jbyteArray merchantData = (*env)->NewByteArray (env, (jsize) payment->merchDataLen);
    (*env)->SetByteArrayRegion (env, merchantData, 0, (jsize) payment->merchDataLen,
                                (const jbyte *) payment->merchantData);
    return merchantData;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolPayment
 * Method:    getTransactions
 * Signature: ()[Lcom/breadwallet/core/BRCoreTransaction;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolPayment_getTransactions
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolPayment *payment =
            (BRPaymentProtocolPayment *) getJNIReference (env, thisObject);

    size_t objectCount = payment->txCount;

    jobjectArray objects = (*env)->NewObjectArray (env, objectCount, transactionClass, 0);

    for (int i = 0; i < objectCount; i++) {
        BRTransaction *transaction = BRTransactionCopy (payment->transactions[i]);

        jobject object = (*env)->NewObject (env, transactionClass, transactionConstructor, (jlong) transaction);
        (*env)->SetObjectArrayElement (env, objects, i, object);

        (*env)->DeleteLocalRef (env, object);
    }

    return objects;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolPayment
 * Method:    getRefundTo
 * Signature: ()[Lcom/breadwallet/core/BRCoreTransactionOutput;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolPayment_getRefundTo
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolPayment *payment =
            (BRPaymentProtocolPayment *) getJNIReference (env, thisObject);

    size_t objectCount = payment->refundToCount;

    jobjectArray objects = (*env)->NewObjectArray (env, objectCount, transactionOutputClass, 0);

    for (int i = 0; i < objectCount; i++) {
        BRTxOutput *target = (BRTxOutput *) calloc (1, sizeof (BRTxOutput));
        transactionOutputCopy (target, &payment->refundTo[i]);

        jobject object = (*env)->NewObject (env, transactionOutputClass, transactionOutputConstructor, (jlong) target);
        (*env)->SetObjectArrayElement (env, objects, i, object);

        (*env)->DeleteLocalRef (env, object);
    }

    return objects;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolPayment
 * Method:    getMerchantMemo
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolPayment_getMerchantMemo
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolPayment *payment =
            (BRPaymentProtocolPayment *) getJNIReference (env, thisObject);
    return (*env)->NewStringUTF (env, payment->memo);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolPayment
 * Method:    createPaymentProtocolPayment
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolPayment_createPaymentProtocolPayment
        (JNIEnv *env, jclass thisClass, jbyteArray dataByteArray)  {
    size_t dataLen = (*env)->GetArrayLength(env, dataByteArray);
    const uint8_t *data = (uint8_t *) (*env)->GetByteArrayElements(env, dataByteArray, 0);
    return (jlong) BRPaymentProtocolPaymentParse (data, dataLen);
}
/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolPayment
 * Method:    serialize
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolPayment_serialize
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolPayment *payment =
            (BRPaymentProtocolPayment *) getJNIReference(env, thisObject);

    size_t dataLen = BRPaymentProtocolPaymentSerialize (payment, NULL, 0);
    uint8_t *data = (uint8_t *) malloc (dataLen);
    BRPaymentProtocolPaymentSerialize (payment, data, dataLen);

    jbyteArray dataByteArray = (*env)->NewByteArray (env, dataLen);
    (*env)->SetByteArrayRegion (env, dataByteArray, 0, dataLen, (jbyte *) data);

    return dataByteArray;
}


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolPayment
 * Method:    disposeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolPayment_disposeNative
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolPayment *payment =
            (BRPaymentProtocolPayment *) getJNIReference (env, thisObject);
    if (NULL != payment) BRPaymentProtocolPaymentFree (payment);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolPayment
 * Method:    initializeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolPayment_initializeNative
        (JNIEnv *env, jclass thisClass) {
    commonStaticInitialize(env);
}

// ======================
//
// Payment ACK
//

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    getCustomerMemo
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolACK_getCustomerMemo
        (JNIEnv *env, jobject thisObject)  {
    BRPaymentProtocolACK *ack = (BRPaymentProtocolACK *) getJNIReference (env, thisObject);
    return (*env)->NewStringUTF (env, ack->memo);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    getMerchantData
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolACK_getMerchantData
        (JNIEnv *env, jobject thisObject)  {
    BRPaymentProtocolACK *ack = (BRPaymentProtocolACK *) getJNIReference (env, thisObject);

    jbyteArray merchantData = (*env)->NewByteArray (env, (jsize) ack->payment->merchDataLen);
    (*env)->SetByteArrayRegion (env, merchantData, 0, (jsize) ack->payment->merchDataLen,
                                (const jbyte *) ack->payment->merchantData);
    return merchantData;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    getTransactions
 * Signature: ()[Lcom/breadwallet/core/BRCoreTransaction;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolACK_getTransactions
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolACK *ack = (BRPaymentProtocolACK *) getJNIReference (env, thisObject);
    BRPaymentProtocolPayment *payment = ack->payment;

    size_t objectCount = payment->txCount;

    jobjectArray objects = (*env)->NewObjectArray (env, objectCount, transactionClass, 0);

    for (int i = 0; i < objectCount; i++) {
        BRTransaction *transaction = BRTransactionCopy (payment->transactions[i]);

        jobject object = (*env)->NewObject (env, transactionClass, transactionConstructor, (jlong) transaction);
        (*env)->SetObjectArrayElement (env, objects, i, object);

        (*env)->DeleteLocalRef (env, object);
    }

    return objects;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    getRefundTo
 * Signature: ()[Lcom/breadwallet/core/BRCoreTransactionOutput;
 */
JNIEXPORT jobjectArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolACK_getRefundTo
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolACK *ack = (BRPaymentProtocolACK *) getJNIReference (env, thisObject);
    BRPaymentProtocolPayment *payment = ack->payment;

    size_t objectCount = payment->refundToCount;

    jobjectArray objects = (*env)->NewObjectArray (env, objectCount, transactionOutputClass, 0);

    for (int i = 0; i < objectCount; i++) {
        BRTxOutput *target = (BRTxOutput *) calloc (1, sizeof (BRTxOutput));
        transactionOutputCopy (target, &payment->refundTo[i]);

        jobject object = (*env)->NewObject (env, transactionOutputClass, transactionOutputConstructor, (jlong) target);
        (*env)->SetObjectArrayElement (env, objects, i, object);

        (*env)->DeleteLocalRef (env, object);
    }

    return objects;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    getMerchantMemo
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolACK_getMerchantMemo
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolACK *ack = (BRPaymentProtocolACK *) getJNIReference (env, thisObject);
    BRPaymentProtocolPayment *payment = ack->payment;

    return (*env)->NewStringUTF (env, payment->memo);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    createPaymentProtocolACK
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolACK_createPaymentProtocolACK
        (JNIEnv *env, jclass thisClass, jbyteArray dataByteArray) {
    size_t dataLen = (*env)->GetArrayLength(env, dataByteArray);
    const uint8_t *data = (uint8_t *) (*env)->GetByteArrayElements(env, dataByteArray, 0);
    return (jlong) BRPaymentProtocolACKParse (data, dataLen);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    serialize
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolACK_serialize
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolACK *ack = (BRPaymentProtocolACK *) getJNIReference (env, thisObject);

    size_t dataLen = BRPaymentProtocolACKSerialize (ack, NULL, 0);
    uint8_t *data = (uint8_t *) malloc (dataLen);
    BRPaymentProtocolACKSerialize (ack, data, dataLen);

    jbyteArray dataByteArray = (*env)->NewByteArray (env, dataLen);
    (*env)->SetByteArrayRegion (env, dataByteArray, 0, dataLen, (jbyte *) data);

    return dataByteArray;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    disposeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolACK_disposeNative
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolACK *ack = (BRPaymentProtocolACK *) getJNIReference (env, thisObject);
    if (NULL != ack) BRPaymentProtocolACKFree (ack);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolACK
 * Method:    initializeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolACK_initializeNative
        (JNIEnv *env, jclass thisClass) {
    commonStaticInitialize(env);
}

// ======================
//
// Invoice Request
//
/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    getSenderPublicKeyReference
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_getSenderPublicKeyReference
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference (env, thisObject);

    BRKey *key = (BRKey *) malloc (sizeof (BRKey));
    memcpy (key, &request->senderPubKey, sizeof (BRKey));
    return (jlong)key;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    getAmount
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_getAmount
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference (env, thisObject);
    return (jlong) request->amount;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    getPKIType
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_getPKIType
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference (env, thisObject);
    return (*env)->NewStringUTF (env, request->pkiType);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    getPKIData
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_getPKIData
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference (env, thisObject);

    jbyteArray data = (*env)->NewByteArray (env, (jsize) request->pkiDataLen);
    (*env)->SetByteArrayRegion (env, data, 0, (jsize) request->pkiDataLen,
                                (const jbyte *) request->pkiData);
    return data;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    getMemo
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_getMemo
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference (env, thisObject);
    return (*env)->NewStringUTF (env, request->memo);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    getNotifyURL
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_getNotifyURL
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference (env, thisObject);
    return (*env)->NewStringUTF (env, request->notifyUrl);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    getSignature
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_getSignature
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference (env, thisObject);

    jbyteArray data = (*env)->NewByteArray (env, (jsize) request->sigLen);
    (*env)->SetByteArrayRegion (env, data, 0, (jsize) request->sigLen,
                                (const jbyte *) request->signature);
    return data;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    createPaymentProtocolInvoiceRequest
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_createPaymentProtocolInvoiceRequest
        (JNIEnv *env, jclass thisClass, jbyteArray dataByteArray) {
    size_t dataLen = (*env)->GetArrayLength(env, dataByteArray);
    const uint8_t *data = (uint8_t *) (*env)->GetByteArrayElements(env, dataByteArray, 0);
    return (jlong) BRPaymentProtocolInvoiceRequestParse (data, dataLen);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    createPaymentProtocolInvoiceRequestFull
 * Signature: (Lcom/breadwallet/core/BRCoreKey;JLjava/lang/String;[BLjava/lang/String;Ljava/lang/String;[B)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_createPaymentProtocolInvoiceRequestFull
        (JNIEnv *env, jclass thisClass,
         jobject senderPublicKey, jlong amount,
         jstring pkiTypeString, jbyteArray pkiDataByteArray,
         jstring memoString, jstring notifyURLString, jbyteArray signatureByteArray) {
    BRKey *senderKey       = (BRKey *) getJNIReference (env, senderPublicKey);
    const char    *pkiType = (*env)->IsSameObject (env, pkiTypeString, NULL)
                             ? NULL
                             : (const char    *) (*env)->GetStringChars (env, pkiTypeString, 0);

    uint8_t *pkiData = (*env)->IsSameObject (env, pkiDataByteArray, NULL)
                             ? NULL
                             : (uint8_t *) (*env)->GetByteArrayElements (env, pkiDataByteArray, 0);

    const char    *memo    = (*env)->IsSameObject (env, memoString, NULL)
                             ? NULL
                             : (const char    *) (*env)->GetStringChars (env, memoString, 0);

    const char    *notify  = (*env)->IsSameObject (env, notifyURLString, NULL)
                             ? NULL
                             : (const char    *) (*env)->GetStringChars (env, notifyURLString, 0);

    const uint8_t *sig     = (*env)->IsSameObject (env, signatureByteArray, NULL)
                             ? NULL
                             : (const uint8_t *) (*env)->GetByteArrayElements (env, signatureByteArray, 0);

    size_t pkiDataLen = (size_t) ((*env)->IsSameObject (env, pkiDataByteArray, NULL)
                        ? 0
                        : (*env)->GetArrayLength (env, pkiDataByteArray));

    size_t sigLen     = (size_t) ((*env)->IsSameObject (env, signatureByteArray, NULL)
                        ? 0
                        : (*env)->GetArrayLength (env, signatureByteArray));

    return (jlong) BRPaymentProtocolInvoiceRequestNew (senderKey, (uint64_t) amount,
                                                       pkiType, pkiData, pkiDataLen,
                                                       memo, notify,
                                                       sig, sigLen);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    serialize
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_serialize
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference(env, thisObject);

    size_t dataLen = BRPaymentProtocolInvoiceRequestSerialize (request, NULL, 0);
    uint8_t *data = (uint8_t *) malloc (dataLen);
    BRPaymentProtocolInvoiceRequestSerialize (request, data, dataLen);

    jbyteArray dataByteArray = (*env)->NewByteArray (env, dataLen);
    (*env)->SetByteArrayRegion (env, dataByteArray, 0, dataLen, (jbyte *) data);

    return dataByteArray;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest
 * Method:    disposeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolInvoiceRequest_disposeNative
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolInvoiceRequest *request =
            (BRPaymentProtocolInvoiceRequest *) getJNIReference (env, thisObject);
    if (NULL != request) BRPaymentProtocolInvoiceRequestFree (request);
}

// ======================
//
// Message
//

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    getMessageTypeValue
 * Signature: ()I
 */
JNIEXPORT jint JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolMessage_getMessageTypeValue
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolMessage *message =
            (BRPaymentProtocolMessage *) getJNIReference (env, thisObject);
    return (jint) message->msgType;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    getMessage
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolMessage_getMessage
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolMessage *message =
            (BRPaymentProtocolMessage *) getJNIReference(env, thisObject);

    jbyteArray data = (*env)->NewByteArray(env, (jsize) message->msgLen);
    (*env)->SetByteArrayRegion(env, data, 0, (jsize) message->msgLen,
                               (const jbyte *) message->message);
    return data;
}


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    getStatusCode
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolMessage_getStatusCode
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolMessage *message =
            (BRPaymentProtocolMessage *) getJNIReference (env, thisObject);
    return (jlong) message->statusCode;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    getStatusMessage
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolMessage_getStatusMessage
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolMessage *message =
            (BRPaymentProtocolMessage *) getJNIReference (env, thisObject);
    return (*env)->NewStringUTF (env, message->statusMsg);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    getIdentifier
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolMessage_getIdentifier
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolMessage *message =
            (BRPaymentProtocolMessage *) getJNIReference (env, thisObject);

    jbyteArray data = (*env)->NewByteArray(env, (jsize) message->identLen);
    (*env)->SetByteArrayRegion(env, data, 0, (jsize) message->identLen,
                               (const jbyte *) message->identifier);
    return data;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    createPaymentProtocolMessage
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolMessage_createPaymentProtocolMessage
        (JNIEnv *env, jclass thisClass, jbyteArray dataByteArray) {
    size_t dataLen = (*env)->GetArrayLength(env, dataByteArray);
    const uint8_t *data = (uint8_t *) (*env)->GetByteArrayElements(env, dataByteArray, 0);
    return (jlong) BRPaymentProtocolMessageParse (data, dataLen);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    createPaymentProtocolMessageFull
 * Signature: (I[BJLjava/lang/String;[B)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolMessage_createPaymentProtocolMessageFull
        (JNIEnv *env, jclass thisClass,
         jint messageType,
         jbyteArray messageByteArray,
         jlong statusCode,
         jstring statusMessageString,
         jbyteArray identifierByteArray) {
    const uint8_t *message = (const uint8_t *) (*env)->GetByteArrayElements (env, messageByteArray, 0);
    const char    *status  = (const char    *) (*env)->GetStringChars (env, statusMessageString, 0);
    const uint8_t *ident   = (const uint8_t *) (*env)->GetByteArrayElements (env, identifierByteArray, 0);

    size_t messageLen = (size_t) (*env)->GetArrayLength (env, messageByteArray);
    size_t identLen   = (size_t) (*env)->GetArrayLength (env, identifierByteArray);

    return (jlong) BRPaymentProtocolMessageNew(messageType, message, messageLen,
                                               statusCode, status,
                                               ident, identLen);
}


/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    serialize
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolMessage_serialize
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolMessage *message =
            (BRPaymentProtocolMessage *) getJNIReference(env, thisObject);

    size_t dataLen = BRPaymentProtocolMessageSerialize (message, NULL, 0);
    uint8_t *data = (uint8_t *) malloc (dataLen);
    BRPaymentProtocolMessageSerialize (message, data, dataLen);

    jbyteArray dataByteArray = (*env)->NewByteArray (env, dataLen);
    (*env)->SetByteArrayRegion (env, dataByteArray, 0, dataLen, (jbyte *) data);

    return dataByteArray;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolMessage
 * Method:    disposeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolMessage_disposeNative
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolMessage *message =
            (BRPaymentProtocolMessage *) getJNIReference (env, thisObject);
    if (NULL != message) BRPaymentProtocolMessageFree(message);
}

// ======================
//
// Encrypted Message
//
/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    getMessage
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_getMessage
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);

    jbyteArray data = (*env)->NewByteArray(env, (jsize) message->msgLen);
    (*env)->SetByteArrayRegion(env, data, 0, (jsize) message->msgLen,
                               (const jbyte *) message->message);
    return data;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    getReceiverPublicKeyReference
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_getReceiverPublicKeyReference
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);

    BRKey *key = (BRKey *) malloc (sizeof (BRKey));
    memcpy (key, &message->receiverPubKey, sizeof (BRKey));
    return (jlong) key;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    getSenderPublicKeyReference
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_getSenderPublicKeyReference
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);

    BRKey *key = (BRKey *) malloc (sizeof (BRKey));
    memcpy (key, &message->senderPubKey, sizeof (BRKey));
    return (jlong) key;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    getNonce
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_getNonce
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);
    return (jlong) message->nonce;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    getSignature
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_getSignature
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);

    jbyteArray data = (*env)->NewByteArray (env, (jsize) message->sigLen);
    (*env)->SetByteArrayRegion (env, data, 0, (jsize) message->sigLen,
                                (const jbyte *) message->signature);
    return data;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    getIdentifier
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_getIdentifier
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);

    jbyteArray data = (*env)->NewByteArray(env, (jsize) message->identLen);
    (*env)->SetByteArrayRegion(env, data, 0, (jsize) message->identLen,
                               (const jbyte *) message->identifier);
    return data;

}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    getStatusCode
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_getStatusCode
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);
    return message->statusCode;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    getStatusMessage
 * Signature: ()Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_getStatusMessage
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);
    return (*env)->NewStringUTF (env, message->statusMsg);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    createPaymentProtocolEncryptedMessage
 * Signature: ([B)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_createPaymentProtocolEncryptedMessage
        (JNIEnv *env, jclass thisClass, jbyteArray dataByteArray) {
    size_t dataLen = (*env)->GetArrayLength(env, dataByteArray);
    const uint8_t *data = (uint8_t *) (*env)->GetByteArrayElements(env, dataByteArray, 0);
    return (jlong) BRPaymentProtocolEncryptedMessageParse (data, dataLen);
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    serialize
 * Signature: ()[B
 */
JNIEXPORT jbyteArray JNICALL Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_serialize
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);

    size_t dataLen = BRPaymentProtocolEncryptedMessageSerialize (message, NULL, 0);
    uint8_t *data = (uint8_t *) malloc (dataLen);
    BRPaymentProtocolEncryptedMessageSerialize (message, data, dataLen);

    jbyteArray dataByteArray = (*env)->NewByteArray (env, dataLen);
    (*env)->SetByteArrayRegion (env, dataByteArray, 0, dataLen, (jbyte *) data);

    return dataByteArray;
}

/*
 * Class:     com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage
 * Method:    disposeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_BRCorePaymentProtocolEncryptedMessage_disposeNative
        (JNIEnv *env, jobject thisObject) {
    BRPaymentProtocolEncryptedMessage *message =
            (BRPaymentProtocolEncryptedMessage *) getJNIReference(env, thisObject);
    if (NULL != message) BRPaymentProtocolEncryptedMessageFree(message);
}

