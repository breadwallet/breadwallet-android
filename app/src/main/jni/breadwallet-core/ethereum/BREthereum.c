//
//  BREthereum
//  breadwallet-core Ethereum
//
//  Created by ebg on 4/17/18.
//  Copyright (c) 2018 breadwallet LLC
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

#include <string.h>
#include <assert.h>
#include "BREthereum.h"
#include "BREthereumTransaction.h"
#include "BREthereumBlock.h"
#include "BREthereumWallet.h"
#include "BREthereumLightNode.h"

//
//
//
extern BREthereumLightNode
ethereumCreate(BREthereumNetwork network,
               const char *paperKey) {
    return createLightNode (network, createAccount(paperKey));
}

extern BREthereumLightNode
ethereumCreateWithPublicKey(BREthereumNetwork network,
                            const BRKey publicKey) { // 65 byte, 0x04-prefixed, uncompressed public key
    return createLightNode (network, createAccountWithPublicKey (publicKey));
}

extern BREthereumBoolean
ethereumConnect(BREthereumLightNode node,
                BREthereumClient client) {
    return lightNodeConnect(node, client);
}

extern BREthereumBoolean
ethereumDisconnect (BREthereumLightNode node) {
    return lightNodeDisconnect(node);
}

extern BREthereumAccountId
ethereumGetAccount(BREthereumLightNode node) {
    return 0;
}

extern char *
ethereumGetAccountPrimaryAddress(BREthereumLightNode node) {
    return addressAsString (accountGetPrimaryAddress(lightNodeGetAccount(node)));
}

extern BRKey // key.pubKey
ethereumGetAccountPrimaryAddressPublicKey(BREthereumLightNode node) {
    return accountGetPrimaryAddressPublicKey(lightNodeGetAccount(node));
}

extern BRKey
ethereumGetAccountPrimaryAddressPrivateKey(BREthereumLightNode node,
                                           const char *paperKey) {
    return accountGetPrimaryAddressPrivateKey (lightNodeGetAccount(node), paperKey);

}

extern BREthereumNetwork
ethereumGetNetwork (BREthereumLightNode node) {
    return lightNodeGetNetwork(node);
}


extern BREthereumWalletId
ethereumGetWallet(BREthereumLightNode node) {
    return lightNodeGetWallet(node);
}

extern BREthereumWalletId
ethereumGetWalletHoldingToken(BREthereumLightNode node,
                              BREthereumToken token) {
    return lightNodeGetWalletHoldingToken(node, token);
}

extern uint64_t
ethereumWalletGetDefaultGasLimit(BREthereumLightNode node,
                                 BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    return walletGetDefaultGasLimit(wallet).amountOfGas;
}

extern void
ethereumWalletSetDefaultGasLimit(BREthereumLightNode node,
                                 BREthereumWalletId wid,
                                 uint64_t gasLimit) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    lightNodeWalletSetDefaultGasLimit(node, wallet, gasCreate(gasLimit));
}

extern uint64_t
ethereumWalletGetGasEstimate(BREthereumLightNode node,
                             BREthereumWalletId wid,
                             BREthereumTransactionId tid) {
    //  BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return transactionGetGasEstimate(transaction).amountOfGas;
}

extern void
ethereumWalletSetDefaultGasPrice(BREthereumLightNode node,
                                 BREthereumWalletId wid,
                                 BREthereumEtherUnit unit,
                                 uint64_t value) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    lightNodeWalletSetDefaultGasPrice (node, wallet, gasPriceCreate(etherCreateNumber (value, unit)));
}

extern uint64_t
ethereumWalletGetDefaultGasPrice(BREthereumLightNode node,
                                 BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumGasPrice gasPrice = walletGetDefaultGasPrice(wallet);
    return (gtUInt256 (gasPrice.etherPerGas.valueInWEI, createUInt256(UINT64_MAX))
            ? 0
            : gasPrice.etherPerGas.valueInWEI.u64[0]);
}

extern BREthereumAmount
ethereumWalletGetBalance(BREthereumLightNode node,
                         BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    return walletGetBalance(wallet);
}

extern char *
ethereumWalletGetBalanceEther(BREthereumLightNode node,
                              BREthereumWalletId wid,
                              BREthereumEtherUnit unit) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumAmount balance = walletGetBalance(wallet);
    return (AMOUNT_ETHER == amountGetType(balance)
            ? etherGetValueString(balance.u.ether, unit)
            : NULL);
}

extern char *
ethereumWalletGetBalanceTokenQuantity(BREthereumLightNode node,
                                      BREthereumWalletId wid,
                                      BREthereumTokenQuantityUnit unit) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumAmount balance = walletGetBalance(wallet);
    return (AMOUNT_TOKEN == amountGetType(balance)
            ? tokenQuantityGetValueString(balance.u.tokenQuantity, unit)
            : NULL);
}

extern BREthereumEther
ethereumWalletEstimateTransactionFee(BREthereumLightNode node,
                                     BREthereumWalletId wid,
                                     BREthereumAmount amount,
                                     int *overflow) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    return walletEstimateTransactionFee(wallet, amount, overflow);
}

extern BREthereumTransactionId
ethereumWalletCreateTransaction(BREthereumLightNode node,
                                BREthereumWalletId wid,
                                const char *recvAddress,
                                BREthereumAmount amount) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    return lightNodeWalletCreateTransaction(node, wallet, recvAddress, amount);
}

extern void // status, error
ethereumWalletSignTransaction(BREthereumLightNode node,
                              BREthereumWalletId wid,
                              BREthereumTransactionId tid,
                              const char *paperKey) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    lightNodeWalletSignTransactionWithPaperKey(node, wallet, transaction, paperKey);
}

extern void // status, error
ethereumWalletSignTransactionWithPrivateKey(BREthereumLightNode node,
                                            BREthereumWalletId wid,
                                            BREthereumTransactionId tid,
                                            BRKey privateKey) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    lightNodeWalletSignTransaction(node, wallet, transaction, privateKey);
}

extern void // status, error
ethereumWalletSubmitTransaction(BREthereumLightNode node,
                                BREthereumWalletId wid,
                                BREthereumTransactionId tid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    lightNodeWalletSubmitTransaction(node, wallet, transaction);
}

//
//
//
extern BREthereumTransactionId *
ethereumWalletGetTransactions(BREthereumLightNode node,
                              BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    return lightNodeWalletGetTransactions(node, wallet);
}

extern int
ethereumWalletGetTransactionCount(BREthereumLightNode node,
                                  BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    return lightNodeWalletGetTransactionCount(node, wallet);
}

extern BREthereumBoolean
ethereumWalletHoldsToken(BREthereumLightNode node,
                         BREthereumWalletId wid,
                         BREthereumToken token) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    return (NULL != wallet && token == walletGetToken(wallet)
            ? ETHEREUM_BOOLEAN_TRUE
            : ETHEREUM_BOOLEAN_FALSE);
}

extern BREthereumToken
ethereumWalletGetToken(BREthereumLightNode node,
                       BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    return (NULL != wallet
            ? walletGetToken(wallet)
            : NULL);
}

//
// Block
//
extern uint64_t
ethereumGetBlockHeight (BREthereumLightNode node) {
    return lightNodeGetBlockHeight(node);
}


extern uint64_t
ethereumBlockGetNumber (BREthereumLightNode node,
                        BREthereumBlockId bid) {
    BREthereumBlock block = lightNodeLookupBlock(node, bid);
    return blockGetNumber(block);
}

extern uint64_t
ethereumBlockGetTimestamp (BREthereumLightNode node,
                           BREthereumBlockId bid) {
    BREthereumBlock block = lightNodeLookupBlock(node, bid);
    return blockGetTimestamp(block);
}

extern char *
ethereumBlockGetHash (BREthereumLightNode node,
                      BREthereumBlockId bid) {
    BREthereumBlock block = lightNodeLookupBlock(node, bid);
    return hashAsString (blockGetHash(block));
}

//
// Transaction
//
extern char *
ethereumTransactionGetRecvAddress(BREthereumLightNode node,
                                  BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return addressAsString(transactionGetTargetAddress(transaction));
}

extern char * // sender, source
ethereumTransactionGetSendAddress(BREthereumLightNode node,
                                  BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return addressAsString(transactionGetSourceAddress(transaction));
}

extern char *
ethereumTransactionGetHash(BREthereumLightNode node,
                           BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return hashAsString (transactionGetHash(transaction));
}

extern char *
ethereumTransactionGetAmountEther(BREthereumLightNode node,
                                  BREthereumTransactionId tid,
                                  BREthereumEtherUnit unit) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    BREthereumAmount amount = transactionGetAmount(transaction);
    return (AMOUNT_ETHER == amountGetType(amount)
            ? etherGetValueString(amountGetEther(amount), unit)
            : "");
}

extern char *
ethereumTransactionGetAmountTokenQuantity(BREthereumLightNode node,
                                          BREthereumTransactionId tid,
                                          BREthereumTokenQuantityUnit unit) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    BREthereumAmount amount = transactionGetAmount(transaction);
    return (AMOUNT_TOKEN == amountGetType(amount)
            ? tokenQuantityGetValueString(amountGetTokenQuantity(amount), unit)
            : "");
}

extern BREthereumAmount
ethereumTransactionGetAmount(BREthereumLightNode node,
                             BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return transactionGetAmount(transaction);
}

extern BREthereumAmount
ethereumTransactionGetGasPriceToo(BREthereumLightNode node,
                                  BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    BREthereumGasPrice gasPrice = transactionGetGasPrice(transaction);
    return amountCreateEther (gasPrice.etherPerGas);
}

extern char *
ethereumTransactionGetGasPrice(BREthereumLightNode node,
                               BREthereumTransactionId tid,
                               BREthereumEtherUnit unit) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    BREthereumGasPrice gasPrice = transactionGetGasPrice(transaction);
    return etherGetValueString(gasPrice.etherPerGas, unit);
}

extern uint64_t
ethereumTransactionGetGasLimit(BREthereumLightNode node,
                               BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return transactionGetGasLimit(transaction).amountOfGas;
}

extern uint64_t
ethereumTransactionGetGasUsed(BREthereumLightNode node,
                              BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    BREthereumGas gasUsed;
    return (transactionExtractBlocked(transaction, &gasUsed, NULL, NULL, NULL)
            ? gasUsed.amountOfGas
            : 0);
}

extern uint64_t
ethereumTransactionGetNonce(BREthereumLightNode node,
                            BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return transactionGetNonce(transaction);
}

extern uint64_t
ethereumTransactionGetBlockNumber(BREthereumLightNode node,
                                  BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    uint64_t blockNumber;
    return (transactionExtractBlocked(transaction, NULL, &blockNumber, NULL, NULL)
            ? blockNumber
            : 0);
}

extern uint64_t
ethereumTransactionGetBlockTimestamp(BREthereumLightNode node,
                                     BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    uint64_t blockTimestamp;
    return (transactionExtractBlocked(transaction, NULL, NULL, &blockTimestamp, NULL)
            ? blockTimestamp
            : 0);
}

extern uint64_t
ethereumTransactionGetBlockConfirmations(BREthereumLightNode node,
                                         BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);

    uint64_t blockNumber = 0;
    return (transactionExtractBlocked(transaction, NULL, &blockNumber, NULL, NULL)
            ? (lightNodeGetBlockHeight(node) - blockNumber)
            : 0);
}

extern BREthereumBoolean
ethereumTransactionIsConfirmed(BREthereumLightNode node,
                               BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return transactionIsConfirmed(transaction);
}

extern BREthereumBoolean
ethereumTransactionIsSubmitted(BREthereumLightNode node,
                               BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    return transactionIsSubmitted(transaction);
}

extern BREthereumBoolean
ethereumTransactionHoldsToken(BREthereumLightNode node,
                              BREthereumTransactionId tid,
                              BREthereumToken token) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    assert (NULL != transaction);
    return (token == transactionGetToken(transaction)
            ? ETHEREUM_BOOLEAN_TRUE
            : ETHEREUM_BOOLEAN_FALSE);
}

extern BREthereumToken
ethereumTransactionGetToken(BREthereumLightNode node,
                            BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    assert (NULL != transaction);
    return transactionGetToken(transaction);
}

extern BREthereumEther
ethereumTransactionGetFee(BREthereumLightNode node,
                          BREthereumTransactionId tid,
                          int *overflow) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    assert (NULL != transaction);
    return transactionGetFee(transaction, overflow);
}

//
// Amount
//
extern BREthereumAmount
ethereumCreateEtherAmountString(BREthereumLightNode node,
                                const char *number,
                                BREthereumEtherUnit unit,
                                BRCoreParseStatus *status) {
    return amountCreateEther (etherCreateString(number, unit, status));
}

extern BREthereumAmount
ethereumCreateEtherAmountUnit(BREthereumLightNode node,
                              uint64_t amountInUnit,
                              BREthereumEtherUnit unit) {
    return amountCreateEther (etherCreateNumber(amountInUnit, unit));
}

extern BREthereumAmount
ethereumCreateTokenAmountString(BREthereumLightNode node,
                                BREthereumToken token,
                                const char *number,
                                BREthereumTokenQuantityUnit unit,
                                BRCoreParseStatus *status) {
    return amountCreateTokenQuantityString(token, number, unit, status);
}

extern char *
ethereumCoerceEtherAmountToString(BREthereumLightNode node,
                                  BREthereumEther ether,
                                  BREthereumEtherUnit unit) {
    return etherGetValueString(ether, unit);
}

extern char *
ethereumCoerceTokenAmountToString(BREthereumLightNode node,
                                  BREthereumTokenQuantity token,
                                  BREthereumTokenQuantityUnit unit) {
    return tokenQuantityGetValueString(token, unit);
}
