//
//  core.c
//
//  Created by Mihail Gutan on 9/24/2015.
//  Copyright (c) 2015 breadwallet LLC
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


#include "core.h"
#include "wallet.h"
#include <stdio.h>
#include <android/log.h>
#include <BRPaymentProtocol.h>
#include <BRBIP39Mnemonic.h>
#include "BRPaymentProtocol.h"

//
// Created by Mihail Gutan on 9/24/15.
//
const int TEST_REQ = 0;

JNIEXPORT jobject JNICALL
Java_com_breadwallet_tools_security_RequestHandler_parsePaymentRequest(JNIEnv *env, jobject obj,
                                                                       jbyteArray payment) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "parsePaymentRequest");
    if (!payment) return NULL;

    //create class
    jclass clazz = (*env)->FindClass(env,
                                     "com/breadwallet/presenter/entities/PaymentRequestWrapper");
    jobject entity = (*env)->AllocObject(env, clazz);
    jfieldID jerror = (*env)->GetFieldID(env, clazz, "error", "I");
    size_t requestLength = (*env)->GetArrayLength(env, payment);

    if (requestLength > 50000) {
        (*env)->SetIntField(env, entity, jerror, 4);
        return entity;
    }

    jbyte *bytePayment = (*env)->GetByteArrayElements(env, payment, 0);

    if (!bytePayment) {
        (*env)->SetIntField(env, entity, jerror, 6);
        return entity;
    }

    BRPaymentProtocolRequest *nativeRequest = BRPaymentProtocolRequestParse(
            (const uint8_t *) bytePayment,
            requestLength);

    if (!nativeRequest) {
        (*env)->SetIntField(env, entity, jerror, 7);
        return entity;
    }

    size_t outputsCount = nativeRequest->details->outCount;
    uint64_t totalAmount = 0;
    jobjectArray stringArray = (jobjectArray) (*env)->NewObjectArray(env, (jsize) outputsCount,
                                                                     (*env)->FindClass(env,
                                                                                       "java/lang/String"),
                                                                     (*env)->NewStringUTF(env, ""));

    for (size_t i = 0; i < outputsCount; i++) {
        BRTxOutput *op = &nativeRequest->details->outputs[i];

        (*env)->SetObjectArrayElement(env, stringArray, i, (*env)->NewStringUTF(env, op->address));
        totalAmount += op->amount;
    }

    __android_log_print(ANDROID_LOG_DEBUG, "parsePaymentRequest",
                        "nativeRequest->details->network: %s, "
                                "BITCOIN_TESTNET: %d", nativeRequest->details->network,
                        BITCOIN_TESTNET);

    if ((strcmp(nativeRequest->details->network, "main") == 0 && BITCOIN_TESTNET == 1) ||
        (strcmp(nativeRequest->details->network, "main") != 0 && BITCOIN_TESTNET == 0)) {
        (*env)->SetIntField(env, entity, jerror, 3);
        return entity;
    }

    //Create serialized paymentProtocolPayment
    BRAddress changeAddress = BRWalletReceiveAddress(_wallet);
    BRTransaction *tx = BRWalletCreateTxForOutputs(_wallet, nativeRequest->details->outputs,
                                                   nativeRequest->details->outCount);

    if (!tx) {
        (*env)->SetIntField(env, entity, jerror, 1);
        return entity;
    }

    uint64_t feeForTx = BRWalletFeeForTx(_wallet, tx);
    uint64_t amountToBeSent =
            BRWalletAmountSentByTx(_wallet, tx) - BRWalletAmountReceivedFromTx(_wallet, tx) -
            feeForTx;


    if (amountToBeSent != totalAmount) {
        (*env)->SetIntField(env, entity, jerror, 5);
        return entity;
    }

    BRPaymentProtocolPayment *paymentProtocolPayment;
    paymentProtocolPayment = BRPaymentProtocolPaymentNew(nativeRequest->details->merchantData,
                                                         nativeRequest->details->merchDataLen,
                                                         &tx, 1, &totalAmount, &changeAddress, 1,
                                                         nativeRequest->details->memo);
    uint8_t buf[BRPaymentProtocolPaymentSerialize(paymentProtocolPayment, NULL, 0)];
    size_t len = BRPaymentProtocolPaymentSerialize(paymentProtocolPayment, buf, sizeof(buf));

    //serialized transaction
    uint8_t txBuf[BRTransactionSerialize(tx, NULL, 0)];
    size_t txLen = BRTransactionSerialize(tx, txBuf, sizeof(txBuf));
    jbyteArray serializedTx = (*env)->NewByteArray(env, (jsize) txLen);
    (*env)->SetByteArrayRegion(env, serializedTx, 0, (jsize) txLen, (jbyte *) txBuf);

    //paymentProtocolPayment
    jbyte *bytesPaymentProtocolPayment = (jbyte *) buf;
    size_t bytesPaymentProtocolPaymentSize = len;
    jbyteArray byteArrayPaymentProtocolPayment = (*env)->NewByteArray(env,
                                                                      (jsize) bytesPaymentProtocolPaymentSize);
    (*env)->SetByteArrayRegion(env, byteArrayPaymentProtocolPayment, 0,
                               (jsize) bytesPaymentProtocolPaymentSize,
                               bytesPaymentProtocolPayment);

    //signature
    jbyte *bytesSignature = (jbyte *) nativeRequest->signature;
    size_t bytesSignatureSize = nativeRequest->sigLen;
    jbyteArray byteArraySignature = (*env)->NewByteArray(env, (jsize) bytesSignatureSize);
    (*env)->SetByteArrayRegion(env, byteArraySignature, 0, (jsize) bytesSignatureSize,
                               bytesSignature);

    //pkiData
    jbyte *bytesPkiData = (jbyte *) nativeRequest->pkiData;
    size_t pkiDataSize = nativeRequest->pkiDataLen;
    jbyteArray byteArrayPkiData = (*env)->NewByteArray(env, (jsize) pkiDataSize);
    (*env)->SetByteArrayRegion(env, byteArrayPkiData, 0, (jsize) pkiDataSize, bytesPkiData);

    //merchantData
    jbyte *bytesMerchantData = (jbyte *) nativeRequest->details->merchantData;
    size_t merchantDataSize = nativeRequest->details->merchDataLen;
    jbyteArray byteArrayMerchantData = (*env)->NewByteArray(env, (jsize) merchantDataSize);
    (*env)->SetByteArrayRegion(env, byteArrayMerchantData, 0, (jsize) merchantDataSize,
                               bytesMerchantData);

    //fields
    jfieldID pkiTypeField = (*env)->GetFieldID(env, clazz, "pkiType", "Ljava/lang/String;");
    jfieldID networkField = (*env)->GetFieldID(env, clazz, "network", "Ljava/lang/String;");
    jfieldID timeField = (*env)->GetFieldID(env, clazz, "time", "J");
    jfieldID expiresField = (*env)->GetFieldID(env, clazz, "expires", "J");
    jfieldID memoField = (*env)->GetFieldID(env, clazz, "memo", "Ljava/lang/String;");
    jfieldID paymentURLField = (*env)->GetFieldID(env, clazz, "paymentURL", "Ljava/lang/String;");
    jfieldID addresses = (*env)->GetFieldID(env, clazz, "addresses", "[Ljava/lang/String;");
    jfieldID amount = (*env)->GetFieldID(env, clazz, "amount", "J");
    jfieldID fee = (*env)->GetFieldID(env, clazz, "fee", "J");

    //methods id
    jmethodID midByteSignature = (*env)->GetMethodID(env, clazz, "byteSignature", "([B)V");
    jmethodID midPkiData = (*env)->GetMethodID(env, clazz, "pkiData", "([B)V");
    jmethodID midMerchantData = (*env)->GetMethodID(env, clazz, "merchantData", "([B)V");
    jmethodID midPayment = (*env)->GetMethodID(env, clazz, "payment", "([B)V");
    jmethodID midSerializedTx = (*env)->GetMethodID(env, clazz, "serializedTx", "([B)V");

    //set java fields
    (*env)->SetObjectField(env, entity, pkiTypeField,
                           (*env)->NewStringUTF(env, nativeRequest->pkiType));
    (*env)->SetObjectField(env, entity, networkField,
                           (*env)->NewStringUTF(env, nativeRequest->details->network));
    (*env)->SetLongField(env, entity, timeField, (jlong) nativeRequest->details->time);
    (*env)->SetLongField(env, entity, expiresField, (jlong) nativeRequest->details->expires);
    (*env)->SetObjectField(env, entity, memoField,
                           (*env)->NewStringUTF(env, nativeRequest->details->memo));
    (*env)->SetObjectField(env, entity, paymentURLField,
                           (*env)->NewStringUTF(env, nativeRequest->details->paymentURL));
    (*env)->SetObjectField(env, entity, addresses, stringArray);
    (*env)->SetLongField(env, entity, amount, (jlong) amountToBeSent);
    (*env)->SetLongField(env, entity, fee, (jlong) feeForTx);

    //call java methods
    (*env)->CallVoidMethod(env, entity, midByteSignature, byteArraySignature);
    (*env)->CallVoidMethod(env, entity, midPkiData, byteArrayPkiData);
    (*env)->CallVoidMethod(env, entity, midMerchantData, byteArrayPkiData);
    (*env)->CallVoidMethod(env, entity, midPayment, byteArrayPaymentProtocolPayment);
    (*env)->CallVoidMethod(env, entity, midSerializedTx, serializedTx);

    //release stuff
    (*env)->ReleaseByteArrayElements(env, payment, bytePayment, JNI_COMMIT);
    BRPaymentProtocolPaymentFree(paymentProtocolPayment);
    return entity;
}

JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_tools_security_RequestHandler_getCertificatesFromPaymentRequest(JNIEnv *env,
                                                                                     jobject obj,
                                                                                     jbyteArray payment,
                                                                                     jint index) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "getCertificatesFromPaymentRequest");
    if (!payment) return NULL;

    //create the BRPaymentProtocolRequest
    size_t requestLength = (size_t) (*env)->GetArrayLength(env, payment);
    jbyte *bytePayment = (*env)->GetByteArrayElements(env, payment, 0);

    if (!bytePayment) return NULL;

    BRPaymentProtocolRequest *nativeRequest = BRPaymentProtocolRequestParse(
            (const uint8_t *) bytePayment,
            requestLength);
    //testing the raw request example!!!!!!

    if (!nativeRequest) return NULL;

    //get certificate
    uint8_t buf[BRPaymentProtocolRequestCert(nativeRequest, NULL, 0, (size_t) index)];
    size_t length = BRPaymentProtocolRequestCert(nativeRequest, buf, sizeof(buf), (size_t) index);

    //convert it to jbyteArray
    jbyte *certJbyte = (jbyte *) buf;
    jbyteArray result = (*env)->NewByteArray(env, (jsize) length);

    (*env)->SetByteArrayRegion(env, result, 0, (jsize) length, (const jbyte *) certJbyte);
    //release everything
    (*env)->ReleaseByteArrayElements(env, payment, bytePayment, JNI_COMMIT);
    BRPaymentProtocolRequestFree(nativeRequest);
    return result;
}

JNIEXPORT jstring JNICALL
Java_com_breadwallet_tools_security_RequestHandler_parsePaymentACK(JNIEnv *env, jobject obj,
                                                                   jbyteArray paymentACK) {
    __android_log_print(ANDROID_LOG_DEBUG, "Message from C: ", "parsePaymentACK");
    if (!paymentACK) return NULL;

    size_t requestLength = (size_t) (*env)->GetArrayLength(env, paymentACK);
    jbyte *bytePayment = (*env)->GetByteArrayElements(env, paymentACK, 0);

    if (!bytePayment) return NULL;

    BRPaymentProtocolACK *ack = BRPaymentProtocolACKParse((const uint8_t *) bytePayment,
                                                          requestLength);
    if (!ack || !ack->memo) return NULL;
    return (*env)->NewStringUTF(env, ack->memo);
}
