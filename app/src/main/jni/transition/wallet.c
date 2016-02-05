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
static BRMasterPubKey _pubKey;
static BRTransaction **_transactions;
static size_t _transactionsCounter = 0;

static jobject getWalletInstance() {
    JNIEnv *env;
    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &env, NULL);

    jclass clazz = (*env)->FindClass(env, "com/breadwallet/wallet/BRWalletManager");
    jfieldID instanceFid = (*env)->GetStaticFieldID(env, clazz, "instance",
                                                    "Lcom/breadwallet/wallet/BRWalletManager;");

    jobject instance;
    if (instanceFid == NULL) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "instanceFid is null!!!! returning ");
        return NULL;
    }
    instance = (*env)->GetStaticObjectField(env, clazz, instanceFid);
    if (instance == NULL) {
        instance = (*env)->AllocObject(env, clazz);
        (*env)->SetObjectField(env, clazz, instanceFid, instance);
    }

    return instance;
}

static void balanceChanged(void *info, uint64_t balance) {
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
//                        "balanceChanged: %d", balance);
    JNIEnv *globalEnv;
    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = getWalletInstance();
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
//    __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ",
//                        ">>>>>>>>>>>>tx->version before: %d", tx->version);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from createWallet: ",
//                        "******TX ADDED CALLBACK******: %d", BRTransactionIsSigned(tx));
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = getWalletInstance();
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxAdded", "([BJJJ)V");
    //call java methods
    __android_log_print(ANDROID_LOG_ERROR, "******TX ADDED CALLBACK AFTER PARSE******: ", "BRWalletAmountReceivedFromTx: %d, ",
                        BRWalletAmountReceivedFromTx(_wallet, tx));

    uint8_t buf[BRTransactionSerialize(tx, NULL, 0)];
    size_t len = BRTransactionSerialize(tx, buf, sizeof(buf));

//    __android_log_print(ANDROID_LOG_ERROR, "******TX ADDED CALLBACK AFTER PARSE******: ", "BRWalletAmountReceivedFromTx: %d, ",
//                        BRWalletAmountReceivedFromTx(_wallet, tmpTx));

//    int i =0;
//    __android_log_print(ANDROID_LOG_ERROR, "FROM C: START OF BYTE PRINTING","");
//    while (i < len)
//    {
//        __android_log_print(ANDROID_LOG_ERROR, "byte: ", "%d", buf[i]);//%02X
//        printBits((unsigned)buf[i]);
//        i++;
//    }
//    __android_log_print(ANDROID_LOG_ERROR, "FROM C: END OF BYTE PRINTING","");
    uint64_t fee = BRWalletFeeForTx(_wallet, tx) == -1? 0 : BRWalletFeeForTx(_wallet, tx);
    jlong amount;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "fee: %d", fee);
    if(BRWalletAmountSentByTx(_wallet, tx)==0){
        amount = BRWalletAmountReceivedFromTx(_wallet, tx);
    } else {
        amount = (BRWalletAmountSentByTx(_wallet, tx) - BRWalletAmountReceivedFromTx(_wallet, tx) - fee) * -1;
    }

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "blockHeight: %d, timestamp: %d bytes: %d",
                        tx->blockHeight, tx->timestamp, len);
    jbyteArray result = (*globalEnv)->NewByteArray(globalEnv, len);
    (*globalEnv)->SetByteArrayRegion(globalEnv, result, 0, len, buf);
    (*globalEnv)->CallVoidMethod(globalEnv, entity, mid, result, (jlong) tx->blockHeight, (jlong) tx->timestamp, amount);
}

static void txUpdated(void *info, const UInt256 txHashes[], size_t count, uint32_t blockHeight,
                      uint32_t timestamp) {
    //TODO method broken, finish method!
    JNIEnv *globalEnv;
    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txUpdated");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = getWalletInstance();
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
    jobject entity = getWalletInstance();
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxDeleted", "([B)V");
    //call java methods
    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodeSeed(JNIEnv *env, jobject thiz,
                                                                         jbyteArray seed,
                                                                         jobjectArray stringArray) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "encodeSeed");
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
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "createWallet");
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
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "CREATING WALLET FROM TXS - txCount: %d", txCount);
        _wallet = BRWalletNew(_transactions, txCount, _pubKey, NULL, theSeed);
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "CREATING EMPTY WALLET");
        _wallet = BRWalletNew(NULL, 0, _pubKey, NULL, theSeed);
    }
    BRWalletSetCallbacks(_wallet, NULL, balanceChanged, txAdded, txUpdated, txDeleted);
    free(_transactions);

//    __android_log_print(ANDROID_LOG_ERROR, "WALLET CREATED:Tx count from the wallet is: ", "%d", BRWalletTransactions(_wallet, NULL, 0));
//    BRAddress addr[20];
//    BRWalletUnusedAddrs(wallet, &addr, 20, 1);
//    for (int i = 0; i < 20; i++){
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRAddress: %s", &addr[i].s);
//    }
    //create class
    jclass clazz = (*env)->FindClass(env, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = getWalletInstance();
    jmethodID mid = (*env)->GetMethodID(env, clazz, "onBalanceChanged", "(J)V");
    //call java methods
    (*env)->CallVoidMethod(env, entity, mid, BRWalletBalance(_wallet));
//
//    if (_transactions) free(_transactions);
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_getMasterPubKey(JNIEnv *env,
                                                                                  jobject thiz,
                                                                                  jstring phrase) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getMasterPubKey");
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
    BRTransaction *tmpTx = BRTransactionParse((uint8_t*)byteTx, txLength);

//    int i = 0;
//    __android_log_print(ANDROID_LOG_ERROR, "FROM C: START OF BYTE PRINTING","");
//    while (i < txLength)
//    {
//        __android_log_print(ANDROID_LOG_ERROR, "byte: ", "%u", (unsigned) byteTx[i]); //%02X
//        i++;
//    }
//    __android_log_print(ANDROID_LOG_ERROR, "FROM C: END OF BYTE PRINTING","");
//


//    __android_log_print(ANDROID_LOG_ERROR, "******TX ADDED CALLBACK AFTER PARSE FROM SQLite******: ",
//                        "BRWalletAmountReceivedFromTx: %d, ", BRWalletAmountReceivedFromTx(_wallet, tmpTx));

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletTransactionIsValid(_wallet, tmpTx): %d",
                        BRWalletTransactionIsValid(_wallet, tmpTx));
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletBalanceAfterTx(_wallet, tmpTx): %d",
//                        BRWalletBalanceAfterTx(_wallet, tmpTx));
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountSentByTx(_wallet, tmpTx): %d",
//                        BRWalletAmountSentByTx(_wallet, tmpTx));
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "uint256_hex_encode(tmpTx->txHash): %s",
                            uint256_hex_encode(tmpTx->txHash));
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountReceivedFromTx(_wallet, tmpTx): %d",
//                        BRWalletAmountReceivedFromTx(_wallet, tmpTx));
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "_transactionsCounter: %d", _transactionsCounter);
    _transactions[_transactionsCounter++] = tmpTx;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_createTxArrayWithCount(JNIEnv *env,
                                                                                jobject thiz,
                                                                                size_t txCount){
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "createTxArrayWithCount: %d", txCount);

    _transactions = calloc(txCount, sizeof(BRTransaction));
    _transactionsCounter = 0;

    // need to call free(transactions);
}

JNIEXPORT jstring Java_com_breadwallet_wallet_BRWalletManager_getReceiveAddress(JNIEnv *env,
                                                                                   jobject thiz){
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getReceiveAddress");
    if(_wallet == NULL) return "";
    BRAddress receiveAddress = BRWalletReceiveAddress(_wallet);
    jstring result = (*env)->NewStringUTF(env,receiveAddress.s);
    return result;

}

JNIEXPORT jobjectArray Java_com_breadwallet_wallet_BRWalletManager_getTransactions(JNIEnv *env,
                                                                                   jobject thiz){

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getTransactions: BRTxs - %d", BRWalletTransactions(_wallet, NULL, 0));
    if(_wallet == NULL) return NULL;
    if(BRWalletTransactions(_wallet, NULL, 0) == 0) return NULL;
//    __android_log_print(ANDROID_LOG_ERROR, "***LOLOLOLOLOLOLO*********: ", "BRWalletTransactions(_wallet, NULL, 0): %d", BRWalletTransactions(_wallet, NULL, 0));
    //Retrieve the txs array
    BRTransaction *transactions_sqlite[BRWalletTransactions(_wallet, NULL, 0)];
    size_t temp = sizeof(transactions_sqlite)/sizeof(*transactions_sqlite);
        __android_log_print(ANDROID_LOG_ERROR, "THIS IS THE TEMP: ", "temp: %d", temp);

    size_t txCount = BRWalletTransactions(_wallet, transactions_sqlite, temp);

//    __android_log_print(ANDROID_LOG_ERROR, "***LOLOLOLOLOLOLO*********: ", "txCount: %d", txCount);

//    return NULL;
    //Find the class and populate the array of objects of this class
    jclass txClass = (*env)->FindClass(env, "com/breadwallet/presenter/entities/TransactionListItem");
    jobjectArray transactionObjects = (*env)->NewObjectArray(env, txCount, txClass, 0);

    for (int i = 0; i < txCount; i++) {

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

//        if(BRWalletAmountReceivedFromTx(_wallet, transactions_sqlite[i]) == 0 && BRWalletAmountSentByTx(_wallet, transactions_sqlite[i])==0) continue;

        //TODO populate the constructor
        jlong JtimeStamp = transactions_sqlite[i]->timestamp;
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->timestamp: %d", transactions_sqlite[i]->timestamp);

        jlong JblockHeight = transactions_sqlite[i]->blockHeight;
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->blockHeight: %d", transactions_sqlite[i]->blockHeight);

        jbyteArray JtxHash = (*env)->NewByteArray(env, sizeof(transactions_sqlite[i]->txHash));
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "uint256_hex_encode(transactions_sqlite[i]->txHash)h: %s",
//                            uint256_hex_encode(transactions_sqlite[i]->txHash));
        (*env)->SetByteArrayRegion(env, JtxHash, 0, sizeof(transactions_sqlite[i]->txHash), &transactions_sqlite[i]->txHash);

        jlong Jsent = (jlong) BRWalletAmountSentByTx(_wallet, transactions_sqlite[i]);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountSentByTx(wallet, transactions_sqlite[i]): %d",
//         BRWalletAmountSentByTx(_wallet, transactions_sqlite[i]));

        jlong Jreceived = (jlong) BRWalletAmountReceivedFromTx(_wallet, transactions_sqlite[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountReceivedFromTx(): %d",
        BRWalletAmountReceivedFromTx(_wallet, transactions_sqlite[i]));

        jlong Jfee = (jlong) BRWalletFeeForTx(_wallet, transactions_sqlite[i]);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletFeeForTx(wallet, transactions_sqlite[i]): %d",
//                BRWalletFeeForTx(_wallet, transactions_sqlite[i]));

        jstring Jto = (*env)->NewStringUTF(env, transactions_sqlite[i]->outputs[0].address);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->outputs[0].address: %s",
//                            transactions_sqlite[i]->outputs[0].address);

        jstring Jfrom = (*env)->NewStringUTF(env, transactions_sqlite[i]->inputs[0].address);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->inputs[0].address: %s",
//                            transactions_sqlite[i]->inputs[0].address);
        jlong JbalanceAfterTx = (jlong) BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]: %d",
//                            BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]));
        //long timeStamp, long blockHeight, byte[] hash, long sent, long received, long fee, String to, String fromvector

//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "call Constructor with i: %d", i);

        jobject txObject = (*env)->NewObject(env, txClass, txObjMid, JtimeStamp, JblockHeight, JtxHash, Jsent, Jreceived, Jfee, Jto, Jfrom, JbalanceAfterTx);
        (*env)->SetObjectArrayElement(env, transactionObjects, txCount - 1 - i, txObject);
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
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "r = %d", r);
    return r != 0 ? rawString : "";
}


//TODO delete this testing method
void printBits(unsigned int num){
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "\n\n");
    while (num) {
        if (num & 1)
            __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "1");
        else
            __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "0");

        num >>= 1;
    }
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "\n\n");
}
