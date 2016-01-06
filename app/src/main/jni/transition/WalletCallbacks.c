//
// Created by Mihail Gutan on 12/4/15.
//
#include "WalletCallbacks.h"
//void Java_com_breadwallet_wallet_BRWalletManager_setCallbacks(JNIEnv *env,
//                                                              jobject thiz,
//                                                              jbyteArray walletBuff) {
//    globalEnv = env;
//    jbyte *byteWallet = (*env)->GetByteArrayElements(env, walletBuff, 0);
//    BRWallet *wallet = (BRWallet *) byteWallet;
//    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "setCallbacks");
//    //set the Wallet callbacks
//    BRWalletSetCallbacks(wallet, NULL, balanceChanged, txAdded, txUpdated, txDeleted);
//
//    int a = 10;
//    int balance = 480000;
////    balanceChanged(a, balance);
//}

//TODO TESTING ONLY, SHOULD BE REMOVED
void Java_com_breadwallet_wallet_BRWalletManager_testWalletCallbacks(JNIEnv *env,
                                                                     jobject thiz) {
//    BRWallet *w;
//    int walletLength = (*env)->GetArrayLength(env, wallet);
//    jbyte *byteWallet = (*env)->GetByteArrayElements(env, wallet, 0);
//    w = (BRWallet*) byteWallet;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 1");
    int r = 1;
    const UInt256 secret = *(UInt256 *)
            "\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\0\x01";
    BRKey k;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 1b");
    BRAddress addr, recvAddr = BRWalletReceiveAddress(wallet);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 1c");
    BRKeySetSecret(&k, &secret, 1);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 1d");
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
    BRWalletRegisterTransaction(wallet, tx);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 3b");
    if (BRWalletBalance(wallet) != SATOSHIS) r = 0;
    tx = BRWalletCreateTransaction(wallet, SATOSHIS / 2, addr.s);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 3c");
    if (!tx) r = 0;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 4");

    if (tx) BRWalletSignTransaction(wallet, tx, NULL);
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 5");
    if (tx && !BRTransactionIsSigned(tx)) r = 0;
    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks 6");

    if (tx) BRWalletRegisterTransaction(wallet, tx);

    if (tx && BRWalletBalance(wallet) + BRWalletFeeForTx(wallet, tx) != SATOSHIS / 2) r = 0;

    __android_log_print(ANDROID_LOG_ERROR, "Message from C: ", "testWalletCallbacks finish");

}
