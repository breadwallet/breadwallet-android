//
// Created by Mihail Gutan on 12/4/15.
//

#include "wallet.h"
#include "PeerManager.h"
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
static jclass _walletManagerClass;

static JNIEnv* getEnv() {
    JNIEnv *env;
    int status = (*_jvm)->GetEnv(_jvm,(void**)&env, JNI_VERSION_1_6);
    if(status < 0) {
        status = (*_jvm)->AttachCurrentThread(_jvm, &env, NULL);
        if(status < 0) {
            return NULL;
        }
    }
    return env;
}

//callback for tx publishing
void callback(void *info, int error){
    if(error){
        __android_log_print(ANDROID_LOG_ERROR, "Message from callback: ", "publishing Failed!");
    } else {
        __android_log_print(ANDROID_LOG_ERROR, "Message from callback: ", "publishing Succeeded!");
    }
}

static void balanceChanged(void *info, uint64_t balance) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "balanceChanged: %d", balance);
    JNIEnv *globalEnv = getEnv();

    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _walletManagerClass, "onBalanceChanged", "(J)V");
//    uint64_t walletBalance = BRWalletBalance(wallet);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
//                        "BRWalletBalance(wallet): %d", BRWalletBalance(wallet));
    //call java methods
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _walletManagerClass, mid, balance);

    (*_jvm)->DetachCurrentThread(_jvm);
}

static void txAdded(void *info, BRTransaction *tx) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txAdded");
    JNIEnv *globalEnv = getEnv();
    jmethodID mid = (*globalEnv)->GetStaticMethodID(globalEnv, _walletManagerClass, "onTxAdded", "([BJJJ)V");
    //call java methods
    __android_log_print(ANDROID_LOG_ERROR, "******TX ADDED CALLBACK AFTER PARSE******: ",
                        "BRWalletAmountReceivedFromTx: %d, ",
                        BRWalletAmountReceivedFromTx(_wallet, tx));
    uint8_t buf[BRTransactionSerialize(tx, NULL, 0)];
    size_t len = BRTransactionSerialize(tx, buf, sizeof(buf));
    uint64_t fee = BRWalletFeeForTx(_wallet, tx) == -1 ? 0 : BRWalletFeeForTx(_wallet, tx);
    jlong amount;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "fee: %d", fee);
    if (BRWalletAmountSentByTx(_wallet, tx) == 0) {
        amount = BRWalletAmountReceivedFromTx(_wallet, tx);
    } else {
        amount = (BRWalletAmountSentByTx(_wallet, tx) - BRWalletAmountReceivedFromTx(_wallet, tx) - fee) * -1;
    }
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
//                        "blockHeight: %d, timestamp: %d bytes: %d",
//                        tx->blockHeight, tx->timestamp, len);
    jbyteArray result = (*globalEnv)->NewByteArray(globalEnv, len);
    (*globalEnv)->SetByteArrayRegion(globalEnv, result, 0, len, buf);
    (*globalEnv)->CallStaticVoidMethod(globalEnv, _walletManagerClass, mid, result, (jlong) tx->blockHeight,
                                 (jlong) tx->timestamp, amount);

    (*_jvm)->DetachCurrentThread(_jvm);

}

static void txUpdated(void *info, const UInt256 txHashes[], size_t count, uint32_t blockHeight,
                      uint32_t timestamp) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txUpdated");
//    //TODO method broken, finish method!
//    JNIEnv *globalEnv;
//    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
//
//    //create class
//    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
//    jobject entity = getWalletInstance();
//    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxUpdated", "([B)V");
//    //call java methods
//    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
//    (*_jvm)->DetachCurrentThread(_jvm);
}

static void txDeleted(void *info, UInt256 txHash) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txDeleted");
//    //TODO method broken, finish method!
//    JNIEnv *globalEnv;
//    jint rs = (*_jvm)->AttachCurrentThread(_jvm, &globalEnv, NULL);
//
//    //create class
//    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
//    jobject entity = getWalletInstance();
//    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxDeleted", "([B)V");
//    //call java methods
//    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
//    (*_jvm)->DetachCurrentThread(_jvm);
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_encodeSeed(JNIEnv *env,
                                                                            jobject thiz,
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
    if (r) {
        int pkLength = (*env)->GetArrayLength(env, bytePubKey);
        jbyte *bytePk = (*env)->GetByteArrayElements(env, bytePubKey, 0);
        _pubKey = *(BRMasterPubKey *) bytePk;
    }

    jint rs = (*env)->GetJavaVM(env, &_jvm); // cache the JavaVM pointer
//    //replace with one of your classes in the line below
    jclass peerManagerCLass = (*env)->FindClass(env,"com/breadwallet/wallet/BRWalletManager");
//    jclass classClass = (*env)->GetObjectClass(env,randomClass);
//    jclass classLoaderClass = (*env)->FindClass(env,"java/lang/ClassLoader");
//    jmethodID getClassLoaderMethod = (*env)->GetMethodID(env,classClass, "getClassLoader",
//                                                         "()Ljava/lang/ClassLoader;");
//    jobject local_gClassLoader = (*env)->CallObjectMethod(env,randomClass, getClassLoaderMethod);
//    _gClassLoader = (*env)->NewGlobalRef(env, local_gClassLoader);
//
//    jmethodID local_gFindClassMethod = (*env)->GetMethodID(env,classLoaderClass, "findClass",
//                                                         "(Ljava/lang/String;)Ljava/lang/Class;");
//    _gFindClassMethod = (jmethodID) (*env)->NewGlobalRef(env, local_gFindClassMethod);

    _walletManagerClass = (jclass) (*env)->NewGlobalRef(env, (jobject) peerManagerCLass);

//    if(_wallet) BRWalletFree(_wallet);


    if (rs != JNI_OK) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "WARNING, GetJavaVM is not JNI_OK");
    }
    int pubKeySize = sizeof(_pubKey);
    if (pubKeySize < 5) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "WARNING, pubKey is corrupt!");
        return;
    }
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "Pubkey: %s", pubKey.pubKey);
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txCount: %d", txCount);

    if (txCount > 0) {
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "CREATING WALLET FROM TXS - txCount: %d", txCount);
        _wallet = BRWalletNew(_transactions, txCount, _pubKey, NULL, theSeed);
    } else {
        __android_log_print(ANDROID_LOG_INFO, "Message from C: ", "CREATING EMPTY WALLET");
        _wallet = BRWalletNew(NULL, 0, _pubKey, NULL, theSeed);
    }
    BRWalletSetCallbacks(_wallet, NULL, balanceChanged, txAdded, txUpdated, txDeleted);
//    free(_transactions);

    //create class
    jclass clazz = (*env)->FindClass(env, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = thiz;
    jmethodID mid = (*env)->GetStaticMethodID(env, clazz, "onBalanceChanged", "(J)V");
    //call java methods
    (*env)->CallStaticVoidMethod(env, clazz, mid, BRWalletBalance(_wallet));
//    balanceChanged(NULL, BRWalletBalance(_wallet));
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
//    int txLength = (*env)->GetArrayLength(env, transaction);
//    jbyte *byteTx = (*env)->GetByteArrayElements(env, transaction, 0);
//    BRTransaction *tmpTx = BRTransactionParse((uint8_t *) byteTx, txLength);

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

//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
//                        "BRWalletTransactionIsValid(_wallet, tmpTx): %d",
//                        BRWalletTransactionIsValid(_wallet, tmpTx));
////    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletBalanceAfterTx(_wallet, tmpTx): %d",
////                        BRWalletBalanceAfterTx(_wallet, tmpTx));
////    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountSentByTx(_wallet, tmpTx): %d",
////                        BRWalletAmountSentByTx(_wallet, tmpTx));
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
//                        "uint256_hex_encode(tmpTx->txHash): %s",
//                        uint256_hex_encode(tmpTx->txHash));
////    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountReceivedFromTx(_wallet, tmpTx): %d",
////                        BRWalletAmountReceivedFromTx(_wallet, tmpTx));
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "_transactionsCounter: %d",
//                        _transactionsCounter);
//    _transactions[_transactionsCounter++] = tmpTx;
}

JNIEXPORT jbyteArray Java_com_breadwallet_wallet_BRWalletManager_createTxArrayWithCount(JNIEnv *env,
                                                                                        jobject thiz,
                                                                                        size_t txCount) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "createTxArrayWithCount: %d",
                        txCount);
//
//    _transactions = calloc(txCount, sizeof(BRTransaction));
//    _transactionsCounter = 0;

    // need to call free(transactions);
}

JNIEXPORT jstring Java_com_breadwallet_wallet_BRWalletManager_getReceiveAddress(JNIEnv *env,
                                                                                jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getReceiveAddress");
    if (_wallet == NULL) return "";
    BRAddress receiveAddress = BRWalletReceiveAddress(_wallet);
    jstring result = (*env)->NewStringUTF(env, receiveAddress.s);
    return result;

}

JNIEXPORT jobjectArray Java_com_breadwallet_wallet_BRWalletManager_getTransactions(JNIEnv *env,
                                                                                   jobject thiz) {

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "getTransactions: BRTxs - %d",
                        BRWalletTransactions(_wallet, NULL, 0));
    if (_wallet == NULL) return NULL;
    if (BRWalletTransactions(_wallet, NULL, 0) == 0) return NULL;
//    __android_log_print(ANDROID_LOG_ERROR, "***LOLOLOLOLOLOLO*********: ", "BRWalletTransactions(_wallet, NULL, 0): %d", BRWalletTransactions(_wallet, NULL, 0));
    //Retrieve the txs array
    BRTransaction *transactions_sqlite[BRWalletTransactions(_wallet, NULL, 0)];
    size_t temp = sizeof(transactions_sqlite) / sizeof(*transactions_sqlite);
    __android_log_print(ANDROID_LOG_ERROR, "THIS IS THE TEMP: ", "temp: %d", temp);

    size_t txCount = BRWalletTransactions(_wallet, transactions_sqlite, temp);

//    __android_log_print(ANDROID_LOG_ERROR, "***LOLOLOLOLOLOLO*********: ", "txCount: %d", txCount);

//    return NULL;
    //Find the class and populate the array of objects of this class
    jclass txClass = (*env)->FindClass(env,
                                       "com/breadwallet/presenter/entities/TransactionListItem");
    jobjectArray transactionObjects = (*env)->NewObjectArray(env, txCount, txClass, 0);

    for (int i = 0; i < txCount; i++) {

        jmethodID txObjMid = (*env)->GetMethodID(env, txClass, "<init>",
                                                 "(JJ[BJJJ[Ljava/lang/String;[Ljava/lang/String;J[J)V");

//      if(BRWalletAmountReceivedFromTx(_wallet, transactions_sqlite[i]) == 0 && BRWalletAmountSentByTx(_wallet, transactions_sqlite[i])==0) continue;

        jlong JtimeStamp = transactions_sqlite[i]->timestamp;
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->timestamp: %d", transactions_sqlite[i]->timestamp);

        jlong JblockHeight = transactions_sqlite[i]->blockHeight;
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "transactions_sqlite[i]->blockHeight: %d", transactions_sqlite[i]->blockHeight);

        jbyteArray JtxHash = (*env)->NewByteArray(env, sizeof(transactions_sqlite[i]->txHash));
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "uint256_hex_encode(transactions_sqlite[i]->txHash)h: %s",
//                            uint256_hex_encode(transactions_sqlite[i]->txHash));
        (*env)->SetByteArrayRegion(env, JtxHash, 0, sizeof(transactions_sqlite[i]->txHash),
                                   &transactions_sqlite[i]->txHash);

        jlong Jsent = (jlong) BRWalletAmountSentByTx(_wallet, transactions_sqlite[i]);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletAmountSentByTx(wallet, transactions_sqlite[i]): %d",
//         BRWalletAmountSentByTx(_wallet, transactions_sqlite[i]));

        jlong Jreceived = (jlong) BRWalletAmountReceivedFromTx(_wallet, transactions_sqlite[i]);
        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ",
                            "BRWalletAmountReceivedFromTx(): %d",
                            BRWalletAmountReceivedFromTx(_wallet, transactions_sqlite[i]));

        jlong Jfee = (jlong) BRWalletFeeForTx(_wallet, transactions_sqlite[i]);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletFeeForTx(wallet, transactions_sqlite[i]): %d",
//                BRWalletFeeForTx(_wallet, transactions_sqlite[i]));



        int outCountTemp = transactions_sqlite[i]->outCount;
        jlongArray JoutAmounts = (*env)->NewLongArray(env, outCountTemp);

        jobjectArray JtoAddresses = (*env)->NewObjectArray(env, outCountTemp, (*env)->FindClass(env,
                                                                                                "java/lang/String"),
                                                           0);
        for (int j = 0; j < outCountTemp; j++) {
            jstring str = (*env)->NewStringUTF(env, transactions_sqlite[i]->outputs[j].address);
            (*env)->SetObjectArrayElement(env, JtoAddresses, j, str);
            (*env)->SetLongArrayRegion(env, JoutAmounts, j, 1,
                                       (const jlong *) &transactions_sqlite[i]->outputs[j].amount);
        }
//        jstring Jto = (*env)->NewStringUTF(env, transactions_sqlite[i]->outputs[0].address);

//        unsigned int* cIntegers = getFromSomewhere();
//        int elements = sizeof(cIntegers) / sizeof(int);
//
//        jfieldID jLongArrayId = env->GetFieldID(javaClass, "longArray", "[J");
//        jlongArray jLongArray = (jlongArray) env->GetObjectField(javaObject, jLongArrayId);
//        for (unsigned int i = 0; i < elements; ++i) {
//            unsigned int cInteger = cIntegers[i];
//            long cLong = doSomehowConvert(cInteger);
//            env->SetLongArrayElement(jLongArray, i, (jlong) cLong);
//        }
        int inCountTemp = transactions_sqlite[i]->inCount;
        jobjectArray JfromAddresses = (*env)->NewObjectArray(env, inCountTemp,
                                                             (*env)->FindClass(env,
                                                                               "java/lang/String"),
                                                             0);
        for (int j = 0; j < inCountTemp; j++) {
            jstring str = (*env)->NewStringUTF(env, transactions_sqlite[i]->inputs[j].address);
            (*env)->SetObjectArrayElement(env, JfromAddresses, j, str);
        }

//      jstring Jfrom = (*env)->NewStringUTF(env, transactions_sqlite[i]->inputs[0].address);

        jlong JbalanceAfterTx = (jlong) BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]);
//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]: %d",
//                            BRWalletBalanceAfterTx(_wallet, transactions_sqlite[i]));
        //long timeStamp, long blockHeight, byte[] hash, long sent, long received, long fee, String to, String fromvector

//        __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "call Constructor with i: %d", i);

        jobject txObject = (*env)->NewObject(env, txClass, txObjMid, JtimeStamp, JblockHeight,
                                             JtxHash, Jsent, Jreceived, Jfee, JtoAddresses,
                                             JfromAddresses,
                                             JbalanceAfterTx, JoutAmounts);
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
    (*_jvm)->DetachCurrentThread(_jvm);
    return r != 0 ? rawString : "";
}

JNIEXPORT void Java_com_breadwallet_wallet_BRWalletManager_pay(JNIEnv *env, jobject thiz,
                                                               jstring address, jlong amount) {
    char *rawAddress = (*env)->GetStringUTFChars(env, address, 0);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "SENDING: address %s , amount %d",
                        rawAddress, amount);

    const char *addr;
    addr = (char *) (*env)->GetStringUTFChars(env, address, NULL);

    BRTransaction *tx = BRWalletCreateTransaction(_wallet, (uint64_t) amount, addr);
    int sign_result = BRWalletSignTransaction(_wallet, tx, NULL);
    BRPeerManagerPublishTx(_peerManager, tx, NULL, callback);

}

//TODO delete this testing method
void printBits(unsigned int num) {
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
