//
// Created by Mihail Gutan on 12/4/15.
//
#include "WalletCallbacks.h"
#include "BRInt.h"
#include "BRTransaction.h"
#include "BRWallet.h"
#include <android/log.h>
#include "wallet.h"
#include "BRPeerManager.h"
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
    const UInt256 secret = uint256_hex_decode("0000000000000000000000000000000000000000000000000000000000000001");
    BRKey k;
    BRAddress addr, recvAddr = BRWalletReceiveAddress(_wallet);
    BRTransaction *tx;
    int r = 1;
    BRKeySetSecret(&k, &secret, 1);
    BRKeyAddress(&k, addr.s, sizeof(addr));

    tx = BRWalletCreateTransaction(_wallet, 0, addr.s);
    if (tx) r = 0;

    tx = BRWalletCreateTransaction(_wallet, SATOSHIS, addr.s);
    if (tx) r = 0;

    uint8_t inScript[BRAddressScriptPubKey(NULL, 0, addr.s)];
    size_t inScriptLen = BRAddressScriptPubKey(inScript, sizeof(inScript), addr.s);
    uint8_t outScript[BRAddressScriptPubKey(NULL, 0, recvAddr.s)];
    size_t outScriptLen = BRAddressScriptPubKey(outScript, sizeof(outScript), recvAddr.s);

    tx = BRTransactionNew();
    BRTransactionAddInput(tx, UINT256_ZERO, 0, inScript, inScriptLen, NULL, 0, TXIN_SEQUENCE);
    BRTransactionAddOutput(tx, SATOSHIS, outScript, outScriptLen);
    BRWalletRegisterTransaction(_wallet, tx); // test adding unsigned tx
    if (BRWalletBalance(_wallet) != 0) r = 0;

    BRTransactionSign(tx, &k, 1);
    BRWalletRegisterTransaction(_wallet, tx);
    if (BRWalletBalance(_wallet) != SATOSHIS) r = 0;

    tx = BRWalletCreateTransaction(_wallet, SATOSHIS*2, addr.s);
    if (tx) r = 0;

    tx = BRWalletCreateTransaction(_wallet, SATOSHIS/2, addr.s);
    if (! tx) r = 0;

    if (tx) BRWalletSignTransaction(_wallet, tx, NULL);
    if (tx && ! BRTransactionIsSigned(tx)) r = 0;

    if (tx) BRWalletRegisterTransaction(_wallet, tx);
    if (tx && BRWalletBalance(_wallet) + BRWalletFeeForTx(_wallet, tx) != SATOSHIS/2) r = 0;
    printf("\n");

    if (tx) BRTransactionFree(tx);

}

void Java_com_breadwallet_wallet_BRWalletManager_testTransactionAdding(JNIEnv *env,
                                                                     jobject thiz) {
    const UInt256 secret = uint256_hex_decode("0000000000000000000000000000000000000000000000000000000000000001");
    BRKey k;
    BRAddress addr, recvAddr = BRWalletReceiveAddress(_wallet);
    BRTransaction *tx;

    BRKeySetSecret(&k, &secret, 1);
    BRKeyAddress(&k, addr.s, sizeof(addr));

    tx = BRWalletCreateTransaction(_wallet, 0, addr.s);

    tx = BRWalletCreateTransaction(_wallet, SATOSHIS, addr.s);

    uint8_t inScript[BRAddressScriptPubKey(NULL, 0, addr.s)];
    size_t inScriptLen = BRAddressScriptPubKey(inScript, sizeof(inScript), addr.s);
    uint8_t outScript[BRAddressScriptPubKey(NULL, 0, recvAddr.s)];
    size_t outScriptLen = BRAddressScriptPubKey(outScript, sizeof(outScript), recvAddr.s);

    tx = BRTransactionNew();
    BRTransactionAddInput(tx, UINT256_ZERO, 0, inScript, inScriptLen, NULL, 0, TXIN_SEQUENCE);
    BRTransactionAddOutput(tx, SATOSHIS, outScript, outScriptLen);
    BRWalletRegisterTransaction(_wallet, tx); // test adding unsigned tx

    BRTransactionSign(tx, &k, 1);
    BRWalletRegisterTransaction(_wallet, tx);

    __android_log_print(ANDROID_LOG_ERROR, "****IMPORTANT****: ", "the tx sign is: %d", BRTransactionIsSigned(tx));

    BRWalletFree(_wallet);

}
