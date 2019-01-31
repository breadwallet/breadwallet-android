//  Created by Ed Gamble on 3/7/2018
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
#include <BRBIP39Mnemonic.h>
#include <BRKey.h>
#include <BREthereum.h>

#include "com_breadwallet_core_ethereum_BREthereumLightNode.h"

//
// Forward Declarations
//
static void
clientGetBalance(BREthereumClientContext context,
                 BREthereumLightNode node,
                 BREthereumWalletId wid,
                 const char *address,
                 int id);

static void
clientGetGasPrice(BREthereumClientContext context,
                  BREthereumLightNode node,
                  BREthereumWalletId wid,
                  int id);

static void
clientEstimateGas(BREthereumClientContext context,
                  BREthereumLightNode node,
                  BREthereumWalletId wid,
                  BREthereumTransactionId tid,
                  const char *to,
                  const char *amount,
                  const char *data,
                  int id);

static void
clientSubmitTransaction(BREthereumClientContext context,
                        BREthereumLightNode node,
                        BREthereumWalletId wid,
                        BREthereumTransactionId tid,
                        const char *transaction,
                        int id);

static void
clientGetTransactions(BREthereumClientContext context,
                      BREthereumLightNode node,
                      const char *account,
                      int id);

static void
clientGetLogs(BREthereumClientContext context,
              BREthereumLightNode node,
              const char *contract,
              const char *address,
              const char *event,
              int rid);

static void
clientGetBlockNumber(BREthereumClientContext context,
                     BREthereumLightNode node,
                     int id);

static void
clientGetNonce(BREthereumClientContext context,
               BREthereumLightNode node,
               const char *address,
               int id);

//
// Forward Declarations - Listener
//
static void
listenerWalletEventHandler(BREthereumListenerContext context,
                           BREthereumLightNode node,
                           BREthereumWalletId wid,
                           BREthereumWalletEvent event,
                           BREthereumStatus status,
                           const char *errorDescription);


static void
listenerBlockEventHandler(BREthereumListenerContext context,
                          BREthereumLightNode node,
                          BREthereumBlockId bid,
                          BREthereumBlockEvent event,
                          BREthereumStatus status,
                          const char *errorDescription);

static void
listenerTransactionEventHandler(BREthereumListenerContext context,
                                BREthereumLightNode node,
                                BREthereumWalletId wid,
                                BREthereumTransactionId tid,
                                BREthereumTransactionEvent event,
                                BREthereumStatus status,
                                const char *errorDescription);

static jstring
asJniString(JNIEnv *env, char *string) {
    jstring result = (*env)->NewStringUTF(env, string);
    free(string);
    return result;
}

//
// Statically Initialize Java References
//

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAddListener
 * Signature: (Lcom/breadwallet/core/ethereum/BREthereumLightNode/Listener;)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAddListener
        (JNIEnv *env, jobject thisObject, jobject listenerObject) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    jobject listener = (*env)->NewGlobalRef(env, thisObject); // listenerObject);

    lightNodeAddListener(node,
                         listener,
                         listenerWalletEventHandler,
                         listenerBlockEventHandler,
                         listenerTransactionEventHandler);
}

///*
// * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
// * Method:    jniCreateLightNodeLES
// * Signature: (Lcom/breadwallet/core/ethereum/BREthereumLightNode/Client;JLjava/lang/String;[Ljava/lang/String;)J
// */
//JNIEXPORT jlong JNICALL
//Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniCreateLightNodeLES
//        (JNIEnv *env, jclass thisClass,
//         jobject clientObject,
//         jlong network,
//         jstring paperKeyString,
//         jobjectArray wordsArrayObject) {
//
//    // Install the wordList
//    int wordsCount = (*env)->GetArrayLength(env, wordsArrayObject);
//    assert (BIP39_WORDLIST_COUNT == wordsCount);
//    char *wordList[wordsCount];
//
//    for (int i = 0; i < wordsCount; i++) {
//        jstring string = (jstring) (*env)->GetObjectArrayElement(env, wordsArrayObject, i);
//        const char *rawString = (*env)->GetStringUTFChars(env, string, 0);
//
//        wordList[i] = strdup (rawString);
//
//        (*env)->ReleaseStringUTFChars(env, string, rawString);
//        (*env)->DeleteLocalRef(env, string);
//    }
//
//    installSharedWordList((const char **) wordList, BIP39_WORDLIST_COUNT);
//
//    const char *paperKey = (*env)->GetStringUTFChars (env, paperKeyString, 0);
//
//    BREthereumConfiguration configuration =
//            ethereumConfigurationCreateLES((BREthereumNetwork) network, 0);
//
//    BREthereumLightNode node = ethereumCreate(configuration, paperKey);
//
//    (*env)->ReleaseStringUTFChars (env, paperKeyString, paperKey);
//    return (jlong) node;
//}
//
///*
// * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
// * Method:    jniCreateLightNodeLES_PublicKey
// * Signature: (Lcom/breadwallet/core/ethereum/BREthereumLightNode/Client;J[B)J
// */
//JNIEXPORT jlong JNICALL
//Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniCreateLightNodeLES_1PublicKey
//        (JNIEnv *env, jclass thisClass, jobject clientObject, jlong network, jbyteArray publicKey) {
//
//    assert (65 == (*env)->GetArrayLength(env, publicKey));
//
//     BREthereumConfiguration configuration =
//             ethereumConfigurationCreateLES((BREthereumNetwork) network, 0);
//
//    jbyte *publicKeyBytes = (*env)->GetByteArrayElements(env, publicKey, 0);
//    BRKey key;
//
//    memcpy (key.pubKey, publicKeyBytes, 65);
//    BREthereumLightNode node =
//            ethereumCreateWithPublicKey(configuration, key);
//
//    (*env)->ReleaseByteArrayElements(env, publicKey, publicKeyBytes, 0);
//    return (jlong) node;
//}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniCreateLightNode
 * Signature: (Lcom/breadwallet/core/ethereum/BREthereumLightNode/Client;JLjava/lang/String;[Ljava/lang/String;)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniCreateLightNode
        (JNIEnv *env, jclass thisClass,
         jobject clientObject,
         jlong network,
         jstring paperKeyString,
         jobjectArray wordsArrayObject) {

    // Install the wordList
    int wordsCount = (*env)->GetArrayLength(env, wordsArrayObject);
    assert (BIP39_WORDLIST_COUNT == wordsCount);
    char *wordList[wordsCount];

    for (int i = 0; i < wordsCount; i++) {
        jstring string = (jstring) (*env)->GetObjectArrayElement(env, wordsArrayObject, i);
        const char *rawString = (*env)->GetStringUTFChars(env, string, 0);

        wordList[i] = strdup (rawString);

        (*env)->ReleaseStringUTFChars(env, string, rawString);
        (*env)->DeleteLocalRef(env, string);
    }

    installSharedWordList((const char **) wordList, BIP39_WORDLIST_COUNT);

    // Get a global reference to client; ensure the client exists in callback threads.
    jobject client = (*env)->NewGlobalRef (env, clientObject);

    const char *paperKey = (*env)->GetStringUTFChars (env, paperKeyString, 0);

    BREthereumLightNode node = ethereumCreate((BREthereumNetwork) network, paperKey);

    (*env)->ReleaseStringUTFChars (env, paperKeyString, paperKey);
    return (jlong) node;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniCreateLightNode_PublicKey
 * Signature: (Lcom/breadwallet/core/ethereum/BREthereumLightNode/Client;J[B)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniCreateLightNode_1PublicKey
    (JNIEnv *env, jclass thisClass, jobject clientObject, jlong network, jbyteArray publicKey) {

    assert (65 == (*env)->GetArrayLength(env, publicKey));

    // Get a global reference to client; ensure the client exists in callback threads.
    jobject client = (*env)->NewGlobalRef(env, clientObject);

    jbyte *publicKeyBytes = (*env)->GetByteArrayElements(env, publicKey, 0);
    BRKey key;

    memcpy (key.pubKey, publicKeyBytes, 65);
    BREthereumLightNode node =
            ethereumCreateWithPublicKey((BREthereumNetwork) network, key);


    (*env)->ReleaseByteArrayElements(env, publicKey, publicKeyBytes, 0);
    return (jlong) node;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniLightNodeGetAccount
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniLightNodeGetAccount
    (JNIEnv *env, jobject thisObject) {
  BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumGetAccount(node);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniLightNodeGetWallet
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniLightNodeGetWallet
        (JNIEnv *env, jobject thisObject) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumGetWallet(node);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniLightNodeGetWalletToken
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniLightNodeGetWalletToken
        (JNIEnv *env, jobject thisObject, jlong tokenId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumGetWalletHoldingToken(node, (BREthereumToken) tokenId);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniLightNodeWalletGetToken
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniLightNodeWalletGetToken
        (JNIEnv *env, jobject thisObject, jlong wid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumWalletGetToken(node, (BREthereumWalletId) wid);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniGetAccountPrimaryAddress
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniGetAccountPrimaryAddress
        (JNIEnv *env, jobject thisObject, jlong account) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    char *addressChars = ethereumGetAccountPrimaryAddress(node);
    jstring addressObject = (*env)->NewStringUTF(env, addressChars);
    free(addressChars);

    return addressObject;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniGetAccountPrimaryAddressPublicKey
 * Signature: (J)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniGetAccountPrimaryAddressPublicKey
        (JNIEnv *env, jobject thisObject, jlong account) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    BRKey key = ethereumGetAccountPrimaryAddressPublicKey(node);
    jbyteArray publicKey = (*env)->NewByteArray (env, 65);
    (*env)->SetByteArrayRegion (env, publicKey, 0, 65, (const jbyte *) key.pubKey);

    return publicKey;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniGetAccountPrimaryAddressPrivateKey
 * Signature: (JLjava/lang/String;)[B
 */
JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniGetAccountPrimaryAddressPrivateKey
        (JNIEnv *env, jobject thisObject,
         jlong account,
         jstring paperKeyString) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    const char *paperKey = (*env)->GetStringUTFChars(env, paperKeyString, 0);
    BRKey key = ethereumGetAccountPrimaryAddressPrivateKey(node, paperKey);
    (*env)->ReleaseStringUTFChars(env, paperKeyString, paperKey);

    jbyteArray privateKey = (*env)->NewByteArray(env, sizeof(BRKey));
    (*env)->SetByteArrayRegion(env, privateKey, 0, sizeof(BRKey), (const jbyte *) &key);

    return privateKey;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniGetWalletBalance
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniGetWalletBalance
        (JNIEnv *env, jobject thisObject, jlong wid, jlong unit) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    BREthereumAmount balance = ethereumWalletGetBalance(node, wid);

    char *number = (AMOUNT_ETHER == amountGetType(balance)
                    ? etherGetValueString(balance.u.ether, unit)
                    : tokenQuantityGetValueString(balance.u.tokenQuantity, unit));

    jstring result = (*env)->NewStringUTF(env, number);
    free(number);
    return result;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniEstimateWalletGasPrice
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniEstimateWalletGasPrice
        (JNIEnv *env, jobject thisObject, jlong wid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    lightNodeUpdateWalletDefaultGasPrice
            (node,
             (BREthereumWalletId) wid);
}


/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniForceWalletBalanceUpdate
 * Signature: (J)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniForceWalletBalanceUpdate
        (JNIEnv *env, jobject thisObject, jlong wid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    lightNodeUpdateWalletBalance
            (node,
             (BREthereumWalletId) wid);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniWalletGetDefaultGasPrice
 * Signature: (J)J
 */
JNIEXPORT jlong
JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniWalletGetDefaultGasPrice
        (JNIEnv *env, jobject thisObject,
         jlong wid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return ethereumWalletGetDefaultGasPrice(node, wid);

}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniWalletSetDefaultGasPrice
 * Signature: (JJ)V
 */
JNIEXPORT void
JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniWalletSetDefaultGasPrice
        (JNIEnv *env, jobject thisObject,
         jlong wid,
         jlong value) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    ethereumWalletSetDefaultGasPrice(node, wid, WEI, value);

}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniWalletGetDefaultGasLimit
 * Signature: (J)J
 */
JNIEXPORT jlong
JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniWalletGetDefaultGasLimit
        (JNIEnv *env, jobject thisObject,
         jlong wid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return ethereumWalletGetDefaultGasLimit (node, wid);

}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniWalletSetDefaultGasLimit
 * Signature: (JJ)V
 */
JNIEXPORT void
JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniWalletSetDefaultGasLimit
        (JNIEnv *env, jobject thisObject,
         jlong wid,
         jlong value) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    ethereumWalletSetDefaultGasLimit(node, wid, value);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniForceTransactionUpdate
 * Signature: ()V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniForceTransactionUpdate
        (JNIEnv *env, jobject thisObject) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    lightNodeUpdateTransactions(node);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAnnounceTransaction
 * Signature: (ILjava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAnnounceTransaction
        (JNIEnv *env, jobject thisObject,
         jint id,
         jstring hashObject,
         jstring toObject,
         jstring fromObject,
         jstring contractObject,
         jstring amountObject,
         jstring gasLimitObject,
         jstring gasPriceObject,
         jstring dataObject,
         jstring nonceObject,
         jstring gasUsedObject,
         jstring blockNumberObject,
         jstring blockHashObject,
         jstring blockConfirmationsObject,
         jstring blockTransactionIndexObject,
         jstring blockTimestampObject,
         jstring isErrorObject) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    const char *hash = (*env)->GetStringUTFChars(env, hashObject, 0);
    const char *to = (*env)->GetStringUTFChars(env, toObject, 0);
    const char *from = (*env)->GetStringUTFChars(env, fromObject, 0);
    const char *contract = (*env)->GetStringUTFChars(env, contractObject, 0);
    const char *amount = (*env)->GetStringUTFChars(env, amountObject, 0);
    const char *gasLimit = (*env)->GetStringUTFChars(env, gasLimitObject, 0);
    const char *gasPrice = (*env)->GetStringUTFChars(env, gasPriceObject, 0);
    const char *data = (*env)->GetStringUTFChars(env, dataObject, 0);
    const char *nonce = (*env)->GetStringUTFChars(env, nonceObject, 0);
    const char *gasUsed = (*env)->GetStringUTFChars(env, gasUsedObject, 0);
    const char *blockNumber = (*env)->GetStringUTFChars(env, blockNumberObject, 0);
    const char *blockHash = (*env)->GetStringUTFChars(env, blockHashObject, 0);
    const char *blockConfirmation = (*env)->GetStringUTFChars(env, blockConfirmationsObject, 0);
    const char *blockTransactionIndex = (*env)->GetStringUTFChars(env, blockTransactionIndexObject, 0);
    const char *blockTimestamp = (*env)->GetStringUTFChars(env, blockTimestampObject, 0);
    const char *isError = (*env)->GetStringUTFChars(env, isErrorObject, 0);

    lightNodeAnnounceTransaction(node, id,
                                 hash, to, from, contract,
                                 amount, gasLimit, gasPrice,
                                 data, nonce, gasUsed,
                                 blockNumber, blockHash, blockConfirmation, blockTransactionIndex,
                                 blockTimestamp,
                                 isError);

    (*env)->ReleaseStringUTFChars(env, hashObject, hash);
    (*env)->ReleaseStringUTFChars(env, toObject, to);
    (*env)->ReleaseStringUTFChars(env, fromObject, from);
    (*env)->ReleaseStringUTFChars(env, contractObject, contract);
    (*env)->ReleaseStringUTFChars(env, amountObject, amount);
    (*env)->ReleaseStringUTFChars(env, gasLimitObject, gasLimit);
    (*env)->ReleaseStringUTFChars(env, gasPriceObject, gasPrice);
    (*env)->ReleaseStringUTFChars(env, dataObject, data);
    (*env)->ReleaseStringUTFChars(env, nonceObject, nonce);
    (*env)->ReleaseStringUTFChars(env, gasUsedObject, gasUsed);
    (*env)->ReleaseStringUTFChars(env, blockNumberObject, blockNumber);
    (*env)->ReleaseStringUTFChars(env, blockHashObject, blockHash);
    (*env)->ReleaseStringUTFChars(env, blockConfirmationsObject, blockConfirmation);
    (*env)->ReleaseStringUTFChars(env, blockTransactionIndexObject, blockTransactionIndex);
    (*env)->ReleaseStringUTFChars(env, blockTimestampObject, blockTimestamp);
    (*env)->ReleaseStringUTFChars(env, isErrorObject, isError);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAnnounceLog
 * Signature: (ILjava/lang/String;Ljava/lang/String;[Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAnnounceLog
        (JNIEnv *env, jobject thisObject,
         jint id,
         jstring hashObject,
         jstring contractObject,
         jobjectArray topicsArray,
         jstring dataObject,
         jstring gasPriceObject,
         jstring gasUsedObject,
         jstring logIndexObject,
         jstring blockNumberObject,
         jstring blockTransactionIndexObject,
         jstring blockTimestampObject) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    size_t topicsCount = (size_t) (*env)->GetArrayLength(env, topicsArray);
    const char *topics[topicsCount];

    for (int i = 0; i < topicsCount; i++) {
        jstring topic = (*env)->GetObjectArrayElement(env, topicsArray, i);
        topics[i] = (*env)->GetStringUTFChars(env, topic, 0);
        (*env)->DeleteLocalRef(env, topic);
    }

    const char *hash = (*env)->GetStringUTFChars(env, hashObject, 0);
    const char *contract = (*env)->GetStringUTFChars(env, contractObject, 0);
    const char *data = (*env)->GetStringUTFChars(env, dataObject, 0);
    const char *gasPrice = (*env)->GetStringUTFChars(env, gasPriceObject, 0);
    const char *gasUsed = (*env)->GetStringUTFChars(env, gasUsedObject, 0);
    const char *logIndex = (*env)->GetStringUTFChars(env, logIndexObject, 0);
    const char *blockNumber = (*env)->GetStringUTFChars(env, blockNumberObject, 0);
    const char *blockTransactionIndex = (*env)->GetStringUTFChars(env, blockTransactionIndexObject, 0);
    const char *blockTimestamp = (*env)->GetStringUTFChars(env, blockTimestampObject, 0);

    lightNodeAnnounceLog(node, id,
                         hash, contract,
                         topicsCount,
                         topics,
                         data, gasPrice, gasUsed,
                         logIndex,
                         blockNumber, blockTransactionIndex, blockTimestamp);

    (*env)->ReleaseStringUTFChars(env, hashObject, hash);
    (*env)->ReleaseStringUTFChars(env, contractObject, contract);
    (*env)->ReleaseStringUTFChars(env, dataObject, data);
    (*env)->ReleaseStringUTFChars(env, gasPriceObject, gasPrice);
    (*env)->ReleaseStringUTFChars(env, gasUsedObject, gasUsed);
    (*env)->ReleaseStringUTFChars(env, logIndexObject, logIndex);
    (*env)->ReleaseStringUTFChars(env, blockNumberObject, blockNumber);
    (*env)->ReleaseStringUTFChars(env, blockTransactionIndexObject, blockTransactionIndex);
    (*env)->ReleaseStringUTFChars(env, blockTimestampObject, blockTimestamp);

    for (int i = 0; i < topicsCount; i++) {
        jstring topic = (*env)->GetObjectArrayElement(env, topicsArray, i);
        (*env)->ReleaseStringUTFChars(env, topic, topics[i]);
        (*env)->DeleteLocalRef(env, topic);
    }
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAnnounceBalance
 * Signature: (ILjava/lang/String;I)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAnnounceBalance
        (JNIEnv *env, jobject thisObject,
         jint wid,
         jstring balanceString,
         jint rid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    const char *balance = (*env)->GetStringUTFChars(env, balanceString, 0);
    lightNodeAnnounceBalance(node, wid, balance, rid);

    (*env)->ReleaseStringUTFChars (env, balanceString, balance);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAnnounceGasPrice
 * Signature: (ILjava/lang/String;I)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAnnounceGasPrice
        (JNIEnv *env, jobject thisObject,
         jint wid,
         jstring gasPrice,
         jint rid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    const char *strGasPrice = (*env)->GetStringUTFChars (env, gasPrice, 0);
    lightNodeAnnounceGasPrice(node,
                              (BREthereumWalletId) wid,
                              strGasPrice,
                              rid);
    (*env)->ReleaseStringUTFChars (env, gasPrice, strGasPrice);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAnnounceGasEstimate
 * Signature: (IILjava/lang/String;I)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAnnounceGasEstimate
        (JNIEnv *env, jobject thisObject,
         jint wid,
         jint tid,
         jstring gasEstimate,
         jint rid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    const char *strGasEstimate = (*env)->GetStringUTFChars(env, gasEstimate, 0);
    lightNodeAnnounceGasEstimate(node,
                                 wid,
                                 tid,
                                 strGasEstimate,
                                 rid);
    (*env)->ReleaseStringUTFChars(env, gasEstimate, strGasEstimate);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAnnounceSubmitTransaction
 * Signature: (ILjava/lang/String;I)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAnnounceSubmitTransaction
        (JNIEnv *env, jobject thisObject,
         jint wid,
         jint tid,
         jstring hash,
         jint rid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    const char *hashStr = (*env)->GetStringUTFChars (env, hash, 0);
    lightNodeAnnounceSubmitTransaction(node, wid, tid, hashStr, rid);
    (*env)->ReleaseStringUTFChars (env, hash, hashStr);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAnnounceBlockNumber
 * Signature: (Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAnnounceBlockNumber
        (JNIEnv *env, jobject thisObject,
         jstring blockNumber,
         jint rid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    const char *strBlockNumber = (*env)->GetStringUTFChars(env, blockNumber, 0);
    lightNodeAnnounceBlockNumber(node, strBlockNumber, rid);
    (*env)->ReleaseStringUTFChars(env, blockNumber, strBlockNumber);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniAnnounceNonce
 * Signature: (Ljava/lang/String;Ljava/lang/String;I)V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniAnnounceNonce
        (JNIEnv *env, jobject thisObject,
         jstring address,
         jstring nonce,
         jint rid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    const char *strAddress = (*env)->GetStringUTFChars(env, address, 0);
    const char *strNonce = (*env)->GetStringUTFChars(env, nonce, 0);
    lightNodeAnnounceNonce(node, strAddress, strNonce, rid);
    (*env)->ReleaseStringUTFChars(env, address, strAddress);
    (*env)->ReleaseStringUTFChars(env, nonce, strNonce);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniCreateTransaction
 * Signature: (JLjava/lang/String;Ljava/lang/String;J)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniCreateTransaction
        (JNIEnv *env, jobject thisObject,
         jlong wid,
         jstring toObject,
         jstring amountObject,
         jlong amountUnit) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    BREthereumToken token = ethereumWalletGetToken(node, wid);

    // Get an actual Amount
    BRCoreParseStatus status = CORE_PARSE_OK;
    const char *amountChars = (*env)->GetStringUTFChars(env, amountObject, 0);
    BREthereumAmount amount = NULL == token
                              ? amountCreateEtherString(amountChars, amountUnit, &status)
                              : amountCreateTokenQuantityString(token, amountChars, amountUnit,
                                                                &status);
    (*env)->ReleaseStringUTFChars (env, amountObject, amountChars);

    const char *to = (*env)->GetStringUTFChars(env, toObject, 0);
    BREthereumTransactionId tid =
            ethereumWalletCreateTransaction(node,
                                            (BREthereumWalletId) wid,
                                            to,
                                            amount);
    (*env)->ReleaseStringUTFChars(env, toObject, to);
    return (jlong) tid;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniSignTransaction
 * Signature: (JJLjava/lang/String;)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniSignTransaction
        (JNIEnv *env, jobject thisObject,
         jlong walletId,
         jlong transactionId,
         jstring paperKeyString) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    const char *paperKey = (*env)->GetStringUTFChars (env, paperKeyString, 0);
    ethereumWalletSignTransaction(node,
                                  (BREthereumWalletId) walletId,
                                  (BREthereumTransactionId) transactionId,
				  paperKey);
    (*env)->ReleaseStringUTFChars(env, paperKeyString, paperKey);
}


/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniSignTransactionWithPrivateKey
 * Signature: (JJ[B)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniSignTransactionWithPrivateKey
        (JNIEnv *env, jobject thisObject,
         jlong walletId,
         jlong transactionId,
         jbyteArray privateKeyByteArray) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    BRKey *key = (BRKey *) (*env)->GetByteArrayElements (env, privateKeyByteArray, 0);

    ethereumWalletSignTransactionWithPrivateKey(node,
                                                (BREthereumWalletId) walletId,
                                                (BREthereumTransactionId) transactionId,
                                                *key);

    (*env)->ReleaseByteArrayElements (env, privateKeyByteArray, (jbyte*) key, 0);
}



/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniSubmitTransaction
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniSubmitTransaction
        (JNIEnv *env, jobject thisObject,
         jlong walletId,
         jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    ethereumWalletSubmitTransaction(node,
                                    (BREthereumWalletId) walletId,
                                    (BREthereumTransactionId) transactionId);
}


/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniGetTransactions
 * Signature: (J)[J
 */
JNIEXPORT jlongArray JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniGetTransactions
        (JNIEnv *env, jobject thisObject,
         jlong wid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    int count = ethereumWalletGetTransactionCount(node, wid);
    assert (-1 != count);

    // uint32_t array - need a long
    BREthereumTransactionId *transactionIds =
            ethereumWalletGetTransactions(node, (BREthereumWalletId) wid);

    jlong ids[count];
    for (int i = 0; i < count; i++) ids[i] = (jlong) transactionIds[i];

    jlongArray transactions = (*env)->NewLongArray (env, (jsize) count);
    (*env)->SetLongArrayRegion (env, transactions, 0, (jsize) count, (jlong*) ids);

    free (transactionIds);
    return transactions;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetAmount
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetAmount
        (JNIEnv *env, jobject thisObject,
         jlong tid,
         jlong unit) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    return asJniString(env,
                       (ETHEREUM_BOOLEAN_TRUE == ethereumTransactionHoldsToken(node, tid, NULL)
                        ? ethereumTransactionGetAmountEther(node, tid,
                                                            (BREthereumEtherUnit) unit)
                        : ethereumTransactionGetAmountTokenQuantity(node, tid,
                                                                    (BREthereumTokenQuantityUnit) unit)));
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetFee
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetFee
        (JNIEnv *env, jobject thisObject,
         jlong tid,
         jlong unit) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    int overflow = 0;
    BREthereumEther fee = ethereumTransactionGetFee(node,
                                                    (BREthereumTransactionId) tid,
                                                    &overflow);

    // Return the FEE in `resultUnit`
    char *feeString = (0 != overflow
                       ? ""
                       : ethereumCoerceEtherAmountToString(node, fee,
                                                           (BREthereumEtherUnit) unit));
    jstring result = (*env)->NewStringUTF(env, feeString);
    if (0 != strcmp("", feeString)) free(feeString);

    return result;
}


/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionHasToken
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionHasToken
        (JNIEnv *env, jobject thisObject,
         jlong tid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jboolean) (ETHEREUM_BOOLEAN_FALSE == ethereumTransactionHoldsToken(node, tid, NULL)
                       ? JNI_TRUE
                       : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionEstimateGas
 * Signature: (JJ)V
 */
JNIEXPORT void JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionEstimateGas
        (JNIEnv *env, jobject thisObject,
         jlong walletId,
         jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    lightNodeUpdateTransactionGasEstimate
            (node,
             (BREthereumWalletId) walletId,
             (BREthereumTransactionId) transactionId);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionEstimateFee
 * Signature: (JLjava/lang/String;JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionEstimateFee
        (JNIEnv *env, jobject thisObject,
         jlong wid,
         jstring amountString,
         jlong amountUnit,
         jlong resultUnit) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    int overflow;
    const char *number = (*env)->GetStringUTFChars(env, amountString, 0);
    BRCoreParseStatus status;

    // Get the `amount` as ETHER or TOKEN QUANTITY
    BREthereumToken token = ethereumWalletGetToken(node, (BREthereumWalletId) wid);
    BREthereumAmount amount = (NULL == token
                               ? ethereumCreateEtherAmountString(node, number,
                                                                 (BREthereumEtherUnit) amountUnit,
                                                                 &status)
                               : ethereumCreateTokenAmountString(node, token, number,
                                                                 (BREthereumTokenQuantityUnit) amountUnit,
                                                                 &status));
    (*env)->ReleaseStringUTFChars(env, amountString, number);

    // Get the estimated FEE
    BREthereumEther fee = ethereumWalletEstimateTransactionFee(node,
                                                               (BREthereumWalletId) wid,
                                                               amount, &overflow);

    // Return the FEE in `resultUnit`
    char *feeString = (status != CORE_PARSE_OK || 0 != overflow
                       ? ""
                       : ethereumCoerceEtherAmountToString(node, fee,
                                                           (BREthereumEtherUnit) resultUnit));

    jstring result = (*env)->NewStringUTF(env, feeString);
    if (0 != strcmp("", feeString)) free(feeString);

    return result;
}


/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionSourceAddress
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionSourceAddress
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return asJniString(env, ethereumTransactionGetSendAddress
            (node,
             (BREthereumTransactionId) transactionId));
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionTargetAddress
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionTargetAddress
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return asJniString(env, ethereumTransactionGetRecvAddress
            (node,
             (BREthereumTransactionId) transactionId));
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetHash
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetHash
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return asJniString(env, ethereumTransactionGetHash
            (node,
             (BREthereumTransactionId) transactionId));
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetGasPrice
 * Signature: (JJ)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetGasPrice
        (JNIEnv *env, jobject thisObject, jlong transactionId, jlong unit) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return asJniString(env, ethereumTransactionGetGasPrice
            (node,
             (BREthereumTransactionId) transactionId,
             (BREthereumEtherUnit) unit));
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetGasLimit
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetGasLimit
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumTransactionGetGasLimit
            (node,
             (BREthereumTransactionId) transactionId);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetGasUsed
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetGasUsed
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumTransactionGetGasUsed
            (node,
             (BREthereumTransactionId) transactionId);

}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetNonce
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetNonce
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumTransactionGetNonce
            (node,
             (BREthereumTransactionId) transactionId);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetBlockNumber
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetBlockNumber
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumTransactionGetBlockNumber
            (node,
             (BREthereumTransactionId) transactionId);

}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetBlockTimestamp
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetBlockTimestamp
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumTransactionGetBlockTimestamp
            (node,
             (BREthereumTransactionId) transactionId);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetBlockConfirmations
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetBlockConfirmations
        (JNIEnv *env, jobject thisObject, jlong tid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumTransactionGetBlockConfirmations
            (node,
             (BREthereumTransactionId) tid);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionGetToken
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionGetToken
        (JNIEnv *env, jobject thisObject, jlong tid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumTransactionGetToken(node, tid);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionIsConfirmed
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionIsConfirmed
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jboolean) (ETHEREUM_BOOLEAN_TRUE ==
                               ethereumTransactionIsConfirmed
                                       (node,
                                        (BREthereumTransactionId) transactionId)
                       ? JNI_TRUE
                       : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniTransactionIsSubmitted
 * Signature: (J)Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniTransactionIsSubmitted
        (JNIEnv *env, jobject thisObject, jlong transactionId) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jboolean) (ETHEREUM_BOOLEAN_TRUE ==
                       ethereumTransactionIsSubmitted
                               (node,
                                (BREthereumTransactionId) transactionId)
                       ? JNI_TRUE
                       : JNI_FALSE);

}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniLightNodeGetBlockHeight
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniLightNodeGetBlockHeight
        (JNIEnv *env, jobject thisObject) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return (jlong) ethereumGetBlockHeight(node);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniBlockGetNumber
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniBlockGetNumber
        (JNIEnv *env, jobject thisObject, jlong bid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return ethereumBlockGetNumber(node, bid);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniBlockGetTimestamp
 * Signature: (J)J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniBlockGetTimestamp
        (JNIEnv *env, jobject thisObject, jlong bid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    return ethereumBlockGetTimestamp(node, bid);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniBlockGetHash
 * Signature: (J)Ljava/lang/String;
 */
JNIEXPORT jstring JNICALL Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniBlockGetHash
        (JNIEnv *env, jobject thisObject, jlong bid) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);
    char *hash = ethereumBlockGetHash(node, bid);
    jstring result = (*env)->NewStringUTF (env, hash);
    free (hash);
    return result;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniLightNodeConnect
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniLightNodeConnect
        (JNIEnv *env, jobject thisObject) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);


    jobject context = (*env)->NewGlobalRef(env, thisObject); // listenerObject);

    // If node is JSON_RPC
    BREthereumClient client =
            ethereumClientCreate
                    ((BREthereumClientContext) context,
                     clientGetBalance,
                     clientGetGasPrice,
                     clientEstimateGas,
                     clientSubmitTransaction,
                     clientGetTransactions,
                     clientGetLogs,
                     clientGetBlockNumber,
                     clientGetNonce);


    return (jboolean) (ETHEREUM_BOOLEAN_TRUE == ethereumConnect(node, client)
                       ? JNI_TRUE
                       : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    jniLightNodeDisconnect
 * Signature: ()Z
 */
JNIEXPORT jboolean JNICALL
Java_com_breadwallet_core_ethereum_BREthereumLightNode_jniLightNodeDisconnect
        (JNIEnv *env, jobject thisObject) {
    BREthereumLightNode node = (BREthereumLightNode) getJNIReference(env, thisObject);

    // TODO: Hopefully
    (*env)->DeleteGlobalRef (env, thisObject);

    return (jboolean) (ETHEREUM_BOOLEAN_TRUE == ethereumDisconnect(node) ? JNI_TRUE : JNI_FALSE);
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumLightNode
 * Method:    initializeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL 
Java_com_breadwallet_core_ethereum_BREthereumLightNode_initializeNative
  (JNIEnv *env, jclass thisClass) {
}

//
// JSON RPC Callback
//
static jmethodID
lookupListenerMethod (JNIEnv *env, jobject listener, char *name, char *type) {
    // Class with desired method.
    jclass listenerClass = (*env)->GetObjectClass(env, listener);

    // Method, if found.
    jmethodID listenerMethod = (*env)->GetMethodID(env, listenerClass, name, type);

    // Clean up and return.
    (*env)->DeleteLocalRef (env, listenerClass);
    return listenerMethod;
}


static void
clientGetBalance(BREthereumClientContext context,
                 BREthereumLightNode node,
                 BREthereumWalletId wid,
                 const char *account,
                 int id) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    // String getBalance (int id, String account);
    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineGetBalance",
                                 "(ILjava/lang/String;I)V");
    assert (NULL != listenerMethod);

    jobject accountObject = (*env)->NewStringUTF(env, account);

    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           (jint) wid,
                           accountObject,
                           (jint) id);

    (*env)->DeleteLocalRef(env, listener);
    (*env)->DeleteLocalRef(env, accountObject);
}

static void
clientGetGasPrice(BREthereumClientContext context,
                  BREthereumLightNode node,
                  BREthereumWalletId wid,
                  int id) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    //String getGasPrice (int id);
    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineGetGasPrice",
                                 "(II)V");
    assert (NULL != listenerMethod);

    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           (jint) wid,
                           (jint) id);

    (*env)->DeleteLocalRef(env, listener);
}

static void
clientEstimateGas(BREthereumClientContext context, BREthereumLightNode node,
                  BREthereumWalletId wid,
                  BREthereumTransactionId tid,
                  const char *to,
                  const char *amount,
                  const char *data,
                  int id) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    // String getGasEstimate (int id, String to, String amount, String data);
    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineGetGasEstimate",
                                 "(IILjava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    assert (NULL != listenerMethod);

    jobject toObject = (*env)->NewStringUTF(env, to);
    jobject amountObject = (*env)->NewStringUTF(env, amount);
    jobject dataObject = (*env)->NewStringUTF(env, data);

    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           (jint) wid,
                           (jint) tid,
                           toObject,
                           amountObject,
                           dataObject,
                           (jint) id);

    (*env)->DeleteLocalRef(env, dataObject);
    (*env)->DeleteLocalRef(env, amountObject);
    (*env)->DeleteLocalRef(env, toObject);
    (*env)->DeleteLocalRef(env, listener);
}

static void
clientSubmitTransaction(BREthereumClientContext context,
                        BREthereumLightNode node,
                        BREthereumWalletId wid,
                        BREthereumTransactionId tid,
                        const char *transaction,
                        int id) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    // String submitTransaction (int id, String rawTransaction);
    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineSubmitTransaction",
                                 "(IILjava/lang/String;I)V");
    assert (NULL != listenerMethod);

    jobject transactionObject = (*env)->NewStringUTF(env, transaction);

    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           (jint) wid,
                           (jint) tid,
                           transactionObject,
                           (jint) id);

    (*env)->DeleteLocalRef(env, transactionObject);
    (*env)->DeleteLocalRef(env, listener);
}

static void
clientGetTransactions(BREthereumClientContext context,
                      BREthereumLightNode node,
                      const char *address,
                      int id) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

//    jmethodID assignNodeMethod =
//            lookupListenerMethod(env, listener,
//            "assignNode",
//            "(Lcom/breadwallet/core/ethereum/BREthereumLightNode;)V");
//    assert (NULL != assignNodeMethod);
////    (*env)->CallVoidMethod (env, listener, assignNodeMethod, damn);

    // void getTransactions(int id, String account);
    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineGetTransactions",
                                 "(Ljava/lang/String;I)V");
    assert (NULL != listenerMethod);

    jobject addressObject = (*env)->NewStringUTF(env, address);

    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           addressObject,
                           (jint) id);

    (*env)->DeleteLocalRef(env, addressObject);
    (*env)->DeleteLocalRef(env, listener);
}

static void
clientGetLogs(BREthereumClientContext context,
              BREthereumLightNode node,
              const char *contract,
              const char *address,
              const char *event,
              int rid) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    // void getTransactions(int id, String account);
    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineGetLogs",
                                 "(Ljava/lang/String;Ljava/lang/String;Ljava/lang/String;I)V");
    assert (NULL != listenerMethod);

    jobject contractObject = (*env)->NewStringUTF(env, contract);
    jobject addressObject = (*env)->NewStringUTF(env, address);
    jobject eventObject = (*env)->NewStringUTF(env, event);

    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           contractObject,
                           addressObject,
                           eventObject,
                           (jint) rid);

    (*env)->DeleteLocalRef(env, eventObject);
    (*env)->DeleteLocalRef(env, addressObject);
    (*env)->DeleteLocalRef(env, contractObject);
    (*env)->DeleteLocalRef(env, listener);
}

static void
clientGetBlockNumber(BREthereumClientContext context,
                     BREthereumLightNode node,
                     int id) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineGetBlockNumber",
                                 "(I)V");
    assert (NULL != listenerMethod);

    (*env)->CallVoidMethod(env, listener, listenerMethod,
                            (jint) id);

    (*env)->DeleteLocalRef(env, listener);
}

static void
clientGetNonce(BREthereumClientContext context,
               BREthereumLightNode node,
               const char *address,
               int id) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineGetNonce",
                                 "(Ljava/lang/String;I)V");
    assert (NULL != listenerMethod);

    jobject addressObject = (*env)->NewStringUTF(env, address);

    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           addressObject,
                           (jint) id);

    (*env)->DeleteLocalRef(env, addressObject);
    (*env)->DeleteLocalRef(env, listener);

}

//
// Listener Callbacks
//
static void
listenerWalletEventHandler(BREthereumListenerContext context,
                           BREthereumLightNode node,
                           BREthereumWalletId wid,
                           BREthereumWalletEvent event,
                           BREthereumStatus status,
                           const char *errorDescription) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineWalletEvent",
                                 "(IIILjava/lang/String;)V");
    assert (NULL != listenerMethod);

    jstring errorDescriptionString = (NULL == errorDescription
                                      ? NULL
                                      : (*env)->NewStringUTF(env, errorDescription));

    // Callback
    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           (jint) wid,
                           (jint) event,
                           (jint) status,
                           errorDescriptionString);

    // Cleanup
    if (NULL != errorDescriptionString) (*env)->DeleteLocalRef(env, errorDescriptionString);
    (*env)->DeleteLocalRef(env, listener);
}


static void
listenerBlockEventHandler(BREthereumListenerContext context,
                          BREthereumLightNode node,
                          BREthereumBlockId bid,
                          BREthereumBlockEvent event,
                          BREthereumStatus status,
                          const char *errorDescription) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineBlockEvent",
                                 "(IIILjava/lang/String;)V");
    assert (NULL != listenerMethod);

    jstring errorDescriptionString = (NULL == errorDescription
                                      ? NULL
                                      : (*env)->NewStringUTF(env, errorDescription));

    // Callback
    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           (jint) bid,
                           (jint) event,
                           (jint) status,
                           errorDescriptionString);

    // Cleanup
    if (NULL != errorDescriptionString) (*env)->DeleteLocalRef(env, errorDescriptionString);
    (*env)->DeleteLocalRef(env, listener);
}

static void
listenerTransactionEventHandler(BREthereumListenerContext context,
                                BREthereumLightNode node,
                                BREthereumWalletId wid,
                                BREthereumTransactionId tid,
                                BREthereumTransactionEvent event,
                                BREthereumStatus status,
                                const char *errorDescription) {
    JNIEnv *env = getEnv();
    if (NULL == env) return;

    jobject listener = (*env)->NewLocalRef(env, (jobject) context);
    if ((*env)->IsSameObject(env, listener, NULL)) return; // GC reclaimed

    jmethodID listenerMethod =
            lookupListenerMethod(env, listener,
                                 "trampolineTransactionEvent",
                                 "(IIIILjava/lang/String;)V");
    assert (NULL != listenerMethod);

    jstring errorDescriptionString = (NULL == errorDescription
                                      ? NULL
                                      : (*env)->NewStringUTF(env, errorDescription));

    // Callback
    (*env)->CallVoidMethod(env, listener, listenerMethod,
                           (jint) wid,
                           (jint) tid,
                           (jint) event,
                           (jint) status,
                           errorDescriptionString);

    // Cleanup
    if (NULL != errorDescriptionString) (*env)->DeleteLocalRef(env, errorDescriptionString);
    (*env)->DeleteLocalRef(env, listener);
}
