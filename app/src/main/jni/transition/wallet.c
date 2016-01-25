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

static JavaVM *_jvm;
BRWallet *_wallet;
//int BITCOIN_TESTNET = 1;
static BRMasterPubKey _pubKey;
static BRTransaction **_transactions;
static size_t _transactionsCounter = 0;

static void balanceChanged(void *info, uint64_t balance) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                        "balanceChanged: %d", balance);
    JNIEnv *globalEnv;
    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
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
    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
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
    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
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
    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
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
        _pubKey = *(BRMasterPubKey *)bytePk;
    }

//    if(_wallet) BRWalletFree(_wallet);
    jint rs = (*env)->GetJavaVM(env, &_jvm);
    if (rs != JNI_OK){
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, GetJavaVM is not JNI_OK");
    }
    int pubKeySize = sizeof(_pubKey);
    if(pubKeySize < 5) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, pubKey is corrupt!");
        return;
    }
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Pubkey: %s", pubKey.pubKey);

//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txCount: %d", txCount);
    if (txCount > 0) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "CREATING WALLET FROM TXS");
        _wallet = BRWalletNew(_transactions, txCount, _pubKey, NULL, theSeed);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "CREATING EMPTY WALLET");
        _wallet = BRWalletNew(NULL, 0, _pubKey, NULL, theSeed);
    }
    BRWalletSetCallbacks(_wallet, NULL, balanceChanged, txAdded, txUpdated, txDeleted);

    __android_log_print(ANDROID_LOG_ERROR, "WALLET CREATED:Tx count from the wallet is: ", "%d", BRWalletTransactions(_wallet, NULL, 0));

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
    (*env)->CallVoidMethod(env, entity, mid, BRWalletBalance(_wallet));

//    if (_transactions) free(_transactions);
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jstring phrase) {
    char *rawPhrase = (*env)->GetStringUTFChars(env, phrase, 0);
    UInt512 key = UINT512_ZERO;
    BRBIP39DeriveKey(key.u8, rawPhrase, NULL);
    _pubKey = BRBIP32MasterPubKey(key.u8, sizeof(key));
    size_t pubKeySize = sizeof(_pubKey);
    jbyteArray result = (*env)->NewByteArray(env, pubKeySize);
    (*env)->SetByteArrayRegion(env, result, 0, pubKeySize, (jbyte *) &_pubKey);

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
            "transactionCounter: %d", tmpTx->blockHeight, _transactionsCounter);
    _transactions[_transactionsCounter++] = tmpTx;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_createTxArrayWithCount(JNIEnv *env,
                                                                                jobject thiz,
                                                                                size_t txCount){

    _transactions = calloc(txCount, sizeof(BRTransaction));

    // need to call free(transactions);
}

JNIEXPORT jstring Java_com_breadwallet_wallet_BRWalletManager_getReceiveAddress(JNIEnv *env,
                                                                                   jobject thiz){
    if(_wallet == NULL) return "";
    BRAddress receiveAddress = BRWalletReceiveAddress(_wallet);
    jstring result = (*env)->NewStringUTF(env,receiveAddress.s);
    return result;

}

JNIEXPORT jobjectArray Java_com_breadwallet_wallet_BRWalletManager_getTransactions(JNIEnv *env,
                                                                                   jobject thiz){
    if(_wallet == NULL) return NULL;

    //Retrieve the txs array
    BRTransaction *transactions_sqlite[BRWalletTransactions(_wallet, NULL, 0)];
    size_t txCount = BRWalletTransactions(_wallet, transactions_sqlite, sizeof(transactions_sqlite)/sizeof(*transactions_sqlite));

    //Find the class and populate the array of objects of this class
    jclass txClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/TransactionListItem");
    jobjectArray transactionObjects = (*env)->NewObjectArray(env, txCount, txClass, 0);
    for (int i = 0; i < txCount; ++i) {

        jmethodID txObjMid = (*env)->GetMethodID(env, txClass, "<init>", "(JJ[BJJJLjava/lang/String;Ljava/lang/String;J)V");

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
        jlong JtimeStamp = transactions_sqlite[i]->timestamp;
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->timestamp: %d", transactions_sqlite[i]->timestamp);

        jlong JblockHeight = transactions_sqlite[i]->blockHeight;
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->blockHeight: %d", transactions_sqlite[i]->blockHeight);

        jbyteArray JtxHash = (*env)->NewByteArray(env,sizeof(transactions_sqlite[i]->txHash));
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "uint256_hex_encode(transactions_sqlite[i]->txHash)h: %s",
                            uint256_hex_encode(transactions_sqlite[i]->txHash));
        (*env)->SetByteArrayRegion(env, JtxHash, 0, sizeof(transactions_sqlite[i]->txHash), &transactions_sqlite[i]->txHash);

        jlong Jsent = (jlong) BRWalletAmountSentByTx(_wallet, transactions_sqlite[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountSentByTx(wallet, transactions_sqlite[i]): %d",
         BRWalletAmountSentByTx(_wallet, transactions_sqlite[i]));

        jlong Jreceived = (jlong) BRWalletAmountReceivedFromTx(_wallet, transactions_sqlite[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountReceivedFromTx(wallet, transactions_sqlite[i]): %d",
        BRWalletAmountReceivedFromTx(_wallet, transactions_sqlite[i]));

        jlong Jfee = (jlong) BRWalletFeeForTx(_wallet, transactions_sqlite[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletFeeForTx(wallet, transactions_sqlite[i]): %d",
                BRWalletFeeForTx(_wallet, transactions_sqlite[i]));

        jstring Jto = (*env)->NewStringUTF(env, transactions_sqlite[i]->outputs[0].address);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->outputs[0].address: %s",
                            transactions_sqlite[i]->outputs[0].address);

        jstring Jfrom = (*env)->NewStringUTF(env, transactions_sqlite[i]->inputs[0].address);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->inputs[0].address: %s",
                            transactions_sqlite[i]->inputs[0].address);
        jlong JbalanceAfterTx = (jlong) BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]: %d",
                            BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]));
        //long timeStamp, long blockHeight, byte[] hash, long sent, long received, long fee, String to, String fromvector

        jobject txObject = (*env)->NewObject(env, txClass, txObjMid, JtimeStamp, JblockHeight, JtxHash, Jsent, Jreceived, Jfee, Jto, Jfrom, JbalanceAfterTx);
        (*env)->SetObjectArrayElement(env,transactionObjects, i, txObject);
    }
    return transactionObjects;
}

const void *theSeed(void *info, const char *authPrompt, uint64_t amount, size_t *seedLen) {
    JNIEnv *env;
    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &env, NULL);

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
