//
// Created by Mihail Gutan on 12/4/15.
//

#include "wallet.h"
#include "BRPeerManager.h"
//#include "WalletCallbacks.h"
#include "BRBIP39Mnemonic.h"
#include <android/log.h>
#include "BRBIP32Sequence.h"
#include "BRTransaction.h"

static JavaVM *jvm;
BRWallet *wallet;
static BRMasterPubKey pubKey;
static BRTransaction **transactions;
static size_t transactionsCounter = 0;

static void balanceChanged(void *info, uint64_t balance) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                        "balanceChanged: %d", balance);
    JNIEnv *globalEnv;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "balanceChanged");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onBalanceChanged", "(J)V");
//    uint64_t walletBalance = BRWalletBalance(wallet);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
//                        "BRWalletBalance(wallet): %d", BRWalletBalance(wallet));
    //call java methods
    (*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

static void txAdded(void *info, BRTransaction *tx) {
    JNIEnv *globalEnv;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txAdded");
    __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ",
                        ">>>>>>>>>>>>tx->version before: %d", tx->version);

    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxAdded", "([BJJ)V");
    //call java methods
    uint8_t buf[BRTransactionSerialize(tx, NULL, 0)];
    size_t len = BRTransactionSerialize(tx, buf, sizeof(buf));

//    int i =0;
//    __android_log_print(ANDROID_LOG_ERROR, "START OF BYTE PRINTING","");
//    while (i < len)
//    {
//        __android_log_print(ANDROID_LOG_ERROR, "byte: ", "%02X", (int)buf[i]);
//        i++;
//    }
//    __android_log_print(ANDROID_LOG_ERROR, "END OF BYTE PRINTING","");

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "blockHeight: %d, timestamp: %d bytes: %d",
                        tx->blockHeight, tx->timestamp, len);
    jbyteArray result = (*globalEnv)->NewByteArray(globalEnv, len);
    (*globalEnv)->SetByteArrayRegion(globalEnv, result, 0, len, (jbyte *) buf);
    (*globalEnv)->CallVoidMethod(globalEnv, entity, mid, result, (jlong) tx->blockHeight, (jlong) tx->timestamp);
}

static void txUpdated(void *info, const UInt256 txHashes[], size_t count, uint32_t blockHeight,
                      uint32_t timestamp) {
    //TODO method broken, finish method!
    JNIEnv *globalEnv;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txUpdated");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxUpdated", "([B)V");
    //call java methods
    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

static void txDeleted(void *info, UInt256 txHash) {
    //TODO method broken, finish method!
    JNIEnv *globalEnv;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txDeleted");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxDeleted", "([B)V");
    //call java methods
    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodeSeed(JNIEnv *env, jobject thiz,
                                                                         jbyteArray seed,
                                                                         jobjectArray stringArray) {

    int wordsCount = (*env)->GetArrayLength(env, stringArray);
    int seedLength = (*env)->GetArrayLength(env, seed);
    const char *wordList[wordsCount];
    for (int i = 0; i < wordsCount; i++) {
        jstring string = (jstring) (*env)->GetObjectArrayElement(env, stringArray, i);
        char *rawString = (*env)->GetStringUTFChars(env, string, 0);
        wordList[i] = rawString;
        (*env)->DeleteLocalRef(env, string);
        // Don't forget to call `ReleaseStringUTFChars` when you're done.
    }
    jbyte *byteSeed = (*env)->GetByteArrayElements(env, seed, 0);
    char *theSeed = byteSeed;
    char result[BRBIP39Encode(NULL, 0, wordList, theSeed, seedLength)];
    size_t len = BRBIP39Encode(result, sizeof(result), wordList, theSeed, seedLength);
    jbyte *phraseJbyte = (const jbyte *) result;
    int size = sizeof(result);
    jbyteArray bytePhrase = (*env)->NewByteArray(env, size);
    (*env)->SetByteArrayRegion(env, bytePhrase, 0, size, phraseJbyte);
    return bytePhrase;
}

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_createWallet(JNIEnv *env,
                                                                        jobject thiz,
                                                                        size_t txCount,
                                                                        jbyteArray bytePubKey,
                                                                        int r) {
    if(r){
        int pkLength = (*env)->GetArrayLength(env, bytePubKey);
        jbyte *bytePk = (*env)->GetByteArrayElements(env, bytePubKey, 0);
        pubKey = *(BRMasterPubKey *)bytePk;
    }

    if(wallet) BRWalletFree(wallet);
    jint rs = (*env)->GetJavaVM(env, &jvm);
    if (rs != JNI_OK){
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, GetJavaVM is not JNI_OK");
    }
    int pubKeySize = sizeof(pubKey);
    if(pubKeySize < 5) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, pubKey is corrupt!");
        return;
    }
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Pubkey: %s", pubKey.pubKey);

//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txCount: %d", txCount);
    if (txCount > 0) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "CREATING WALLET FROM TXS");
        wallet = BRWalletNew(transactions, txCount, pubKey, NULL, theSeed);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "CREATING EMPTY WALLET");
        wallet = BRWalletNew(NULL, 0, pubKey, NULL, theSeed);
    }
    BRWalletSetCallbacks(wallet, NULL, balanceChanged, txAdded, txUpdated, txDeleted);

//    BRAddress addr[20];
//
//    BRWalletUnusedAddrs(wallet, &addr, 20, 1);
//    for (int i = 0; i < 20; i++){
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRAddress: %s", &addr[i].s);
//    }
    //create class
    jclass clazz = (*env)->FindClass(env, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*env)->AllocObject(env, clazz);
    jmethodID mid = (*env)->GetMethodID(env, clazz, "onBalanceChanged", "(J)V");
    //call java methods
    (*env)->CallVoidMethod(env, entity, mid, BRWalletBalance(wallet));

    if (transactions) free(transactions);
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jstring phrase) {
    char *rawPhrase = (*env)->GetStringUTFChars(env, phrase, 0);
    UInt512 key = UINT512_ZERO;
    BRBIP39DeriveKey(key.u8, rawPhrase, NULL);
    pubKey = BRBIP32MasterPubKey(key.u8, sizeof(key));
    size_t pubKeySize = sizeof(pubKey);
    jbyteArray result = (*env)->NewByteArray(env, pubKeySize);
    (*env)->SetByteArrayRegion(env, result, 0, pubKeySize, (jbyte *) &pubKey);

    (*env)->ReleaseStringUTFChars(env, phrase, rawPhrase);
    return result;
}

//Call multiple times with all the transactions from the DB
JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_putTransaction(JNIEnv *env,
                                                                                 jobject thiz,
                                                                                 jbyteArray transaction) {
    int txLength = (*env)->GetArrayLength(env, transaction);
    jbyte *byteTx = (*env)->GetByteArrayElements(env, transaction, 0);
    BRTransaction *tmpTx = BRTransactionParse(byteTx, txLength);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "adding a transaction: blockhight: %d, "
            "transactionCounter: %d", tmpTx->blockHeight, transactionsCounter);
    transactions[transactionsCounter++] = tmpTx;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_createTxArrayWithCount(JNIEnv *env,
                                                                                jobject thiz,
                                                                                size_t txCount){

    transactions = calloc(txCount, sizeof(*transactions));

    // need to call free(transactions);
}

JNIEXPORT jstring Java_com_breadwallet_wallet_BRWalletManager_getReceiveAddress(JNIEnv *env,
                                                                                   jobject thiz){
    if(wallet == NULL) return "";
    BRAddress receiveAddress = BRWalletReceiveAddress(wallet);
    jstring result = (*env)->NewStringUTF(env,receiveAddress.s);
    return result;

}

JNIEXPORT jobjectArray Java_com_breadwallet_wallet_BRWalletManager_getTransactions(JNIEnv *env,
                                                                                   jobject thiz){
    if(wallet == NULL) return NULL;

    //Retrieve the txs array
    BRTransaction *transactions[BRWalletTransactions(wallet, NULL, 0)];
    size_t txCount = BRWalletTransactions(wallet, transactions, sizeof(transactions)/sizeof(*transactions));

    //Find the class and populate the array of objects of this class
    jclass txClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/TransactionListItem");
    jobjectArray transactionObjects = (*env)->NewObjectArray(env, txCount, txClass, 0);
    for (int i = 0; i < txCount; ++i) {

        jmethodID txObjMid = (*env)->GetMethodID(env, txClass, "<init>", "(JJ[BJJJLjava/lang/String;Ljava/lang/String;)V");

        //typedef struct {
        //    UInt256 txHash;
        //    uint32_t version;
        //    size_t inCount;
        //    BRTxInput *inputs;
        //    size_t outCount;
        //    BRTxOutput *outputs;
        //    uint32_t lockTime;
        //    uint32_t blockHeight;
        //    uint32_t timestamp; // time interval since unix epoch
        //} BRTransaction;

        //TODO populate the constructor
        jlong JtimeStamp = transactions[i]->timestamp;
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions[i]->timestamp: %d", transactions[i]->timestamp);

        jlong JblockHeight = transactions[i]->blockHeight;
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions[i]->blockHeight: %d", transactions[i]->blockHeight);

        jbyteArray JtxHash = (*env)->NewByteArray(env,sizeof(transactions[i]->txHash));
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "uint256_hex_encode(transactions[i]->txHash)h: %s",
                            uint256_hex_encode(transactions[i]->txHash));
        (*env)->SetByteArrayRegion(env, JtxHash, 0, sizeof(transactions[i]->txHash), &transactions[i]->txHash);

        jlong Jsent = (jlong) BRWalletAmountSentByTx(wallet, transactions[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountSentByTx(wallet, transactions[i]): %d",
         BRWalletAmountSentByTx(wallet, transactions[i]));

        jlong Jreceived = (jlong) BRWalletAmountReceivedFromTx(wallet, transactions[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountReceivedFromTx(wallet, transactions[i]): %d",
        BRWalletAmountReceivedFromTx(wallet, transactions[i]));

        jlong Jfee = (jlong) BRWalletFeeForTx(wallet, transactions[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletFeeForTx(wallet, transactions[i]): %d",
                BRWalletFeeForTx(wallet, transactions[i]));

        jstring Jto = (*env)->NewStringUTF(env, transactions[i]->outputs[0].address);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions[i]->outputs[0].address: %s",
                            transactions[i]->outputs[0].address);

        jstring Jfrom = (*env)->NewStringUTF(env, transactions[i]->inputs[0].address);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions[i]->inputs[0].address: %s",
                            transactions[i]->inputs[0].address);
        //long timeStamp, long blockHeight, byte[] hash, long sent, long received, long fee, String to, String fromvector

        jobject txObject = (*env)->NewObject(env, txClass, txObjMid, JtimeStamp, JblockHeight, JtxHash, Jsent, Jreceived, Jfee, Jto, Jfrom);
        (*env)->SetObjectArrayElement(env,transactionObjects, i, txObject);
    }
    return transactionObjects;
}

const void *theSeed(void *info, const char *authPrompt, uint64_t amount, size_t *seedLen) {
    JNIEnv *env;
    jint rs = (*jvm)->AttachCurrentThread(jvm, &env, NULL);

    jclass clazz = (*env)->FindClass(env, "com/breadwallet/tools/security/KeyStoreManager");
    jmethodID midGetSeed = (*env)->GetStaticMethodID(env, clazz, "getSeed", "()Ljava/lang/String;");
    //call java methods
    jstring jStringSeed = (jstring) (*env)->CallStaticObjectMethod(env, clazz, midGetSeed);
    const char *rawString = (*env)->GetStringUTFChars(env, jStringSeed, 0);
    int r = strcmp(rawString, "none");
    size_t theSize = (*env)->GetStringUTFLength(env, jStringSeed);
    *seedLen = r != 0 ? theSize : 0;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "r = %d", r);
    return r != 0 ? rawString : "";
}
