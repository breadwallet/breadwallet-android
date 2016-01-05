//
// Created by Mihail Gutan on 12/4/15.
//
#include "WalletCallbacks.h"
JNIEnv *globalEnv;


//void BRWalletSetCallbacks(BRWallet *wallet, void *info,
//void (*balanceChanged)(void *info, uint64_t balance),
//void (*txAdded)(void *info, BRTransaction *tx),
//void (*txUpdated)(void *info, const UInt256 txHash[], size_t count, uint32_t blockHeight,
//                  uint32_t timestamp),
//void (*txDeleted)(void *info, UInt256 txHash))

static void balanceChanged(void *info, uint64_t balance) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "balanceChanged");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onBalanceChanged", "(J)V");
    //call java methods
    (*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

static void txAdded(void *info, BRTransaction *tx) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txAdded");
//    //create class
//    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
//    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
//    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxAdded", "([B)V");
//    //call java methods
//    jbyteArray result = (*globalEnv)->NewByteArray(globalEnv, sizeof(tx));
//    (*globalEnv)->SetByteArrayRegion(globalEnv, result, 0, sizeof(result), (jbyte *) tx);
//    (*globalEnv)->CallVoidMethod(globalEnv, entity, mid, result);
}

static void txUpdated(void *info, const UInt256 txHashes[], size_t count, uint32_t blockHeight,
                      uint32_t timestamp) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txUpdated");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxUpdated", "([B)V");
    //call java methods
    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

static void txDeleted(void *info, UInt256 txHash) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "txDeleted");
    //create class
    jclass clazz = (*globalEnv)->FindClass(globalEnv, "com/breadwallet/wallet/BRWalletManager");
    jobject entity = (*globalEnv)->AllocObject(globalEnv, clazz);
    jmethodID mid = (*globalEnv)->GetMethodID(globalEnv, clazz, "onTxDeleted", "([B)V");
    //call java methods
    //(*globalEnv)->CallVoidMethod(globalEnv, entity, mid, balance);
}

void Java_com_breadwallet_wallet_BRWalletManager_setCallbacks(JNIEnv *env,
                                                              jobject thiz,
                                                              jbyteArray walletBuff) {
    globalEnv = env;
    jbyte *byteWallet = (*env)->GetByteArrayElements(env, walletBuff, 0);
    BRWallet *wallet = (BRWallet*)byteWallet;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "setCallbacks");
    //set the Wallet callbacks
    BRWalletSetCallbacks(wallet, NULL, balanceChanged, txAdded, txUpdated, txDeleted);

    int a = 10;
    int balance = 480000;
//    balanceChanged(a, balance);
}

//TODO TESTING ONLY, SHOULD BE REMOVED
void Java_com_breadwallet_wallet_BRWalletManager_testWalletCallbacks(JNIEnv *env,
                                                                     jobject thiz) {
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 1");
    int r = 1;
    size_t seedLen = 0;
    const void *seed = theSeed(NULL, NULL, 0, &seedLen);
    if (!seed) return;
    BRMasterPubKey mpk = BRBIP32MasterPubKey(seed, seedLen);
    BRWallet *w = BRWalletNew(NULL, 0, mpk, NULL, theSeed);
    const UInt256 secret = *(UInt256 *) "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\x01";
    BRKey k;
    BRAddress addr, recvAddr = BRWalletReceiveAddress(w);
    BRKeySetSecret(&k, &secret, 1);
    BRKeyAddress(&k, addr.s, sizeof(addr));
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 2");
    uint8_t inScript[BRAddressScriptPubKey(NULL, 0, addr.s)];
    size_t inScriptLen = BRAddressScriptPubKey(inScript, sizeof(inScript), addr.s);
    uint8_t outScript[BRAddressScriptPubKey(NULL, 0, recvAddr.s)];
    size_t outScriptLen = BRAddressScriptPubKey(outScript, sizeof(outScript), recvAddr.s);
    BRTransaction *tx = BRTransactionNew();
    BRTransactionAddInput(tx, UINT256_ZERO, 0, inScript, inScriptLen, NULL, 0, TXIN_SEQUENCE);
    BRTransactionAddOutput(tx, SATOSHIS, outScript, outScriptLen);
    BRTransactionSign(tx, &k, 1);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 3");
    BRWalletRegisterTransaction(w, tx);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 3b");
    if (BRWalletBalance(w) != SATOSHIS) r = 0;
    tx = BRWalletCreateTransaction(w, SATOSHIS / 2, addr.s);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 3c");
    if (!tx) r = 0;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 4");

    if (tx) BRWalletSignTransaction(w, tx, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 5");
    if (tx && !BRTransactionIsSigned(tx)) r = 0;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 6");

    if (tx) BRWalletRegisterTransaction(w, tx);

    if (tx && BRWalletBalance(w) + BRWalletFeeForTx(w, tx) != SATOSHIS / 2) r = 0;

    BRWalletFree(w);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks finish");

}


