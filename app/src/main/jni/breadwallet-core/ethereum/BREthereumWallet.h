//
//  BBREthereumWallet.h
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 2/21/2018.
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

#ifndef BR_Ethereum_Wallet_H
#define BR_Ethereum_Wallet_H

#include "BREthereumEther.h"
#include "BREthereumGas.h"
#include "BREthereumAmount.h"
#include "BREthereumAccount.h"
#include "BREthereumNetwork.h"
#include "BREthereumTransaction.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * An EthereumWallet holds ETH or ERC20 Tokens.
 *
 * GetEtherPerHolding
 * SetEtherPerHolding
 * GetHoldingValueInEther
 *
 * GetLocalCurrentyPerHolding
 * SetLocalCurrencyPerHolding
 * GetHoldingValueInLocalCurrency
 */
typedef struct BREthereumWalletRecord *BREthereumWallet;

/**
 * Create a wallet holding ETH; will use the account's primary address.
 *
 * @param account
 * @return
 */
extern BREthereumWallet
walletCreate(BREthereumAccount account,
             BREthereumNetwork network);

/**
 * Create a wallet holding ETH; will use `address` as the wallet's address.  The provided address
 * must be owned by `account`.
 *
 * @param account
 * @param address
 * @return
 */
extern BREthereumWallet
walletCreateWithAddress(BREthereumAccount account,
                        BREthereumAddress address,
                        BREthereumNetwork network);

/**
 * Create a Wallet holding Token.
 *
 * @param account
 * @param token
 * @return
 */
extern BREthereumWallet
walletCreateHoldingToken(BREthereumAccount account,
                         BREthereumNetwork network,
                         BREthereumToken token);

/**
 * Estimate the transaction fee (in Ether) for transferring amount.  The estimate uses
 * the wallet's default gasPrice and the amount's default gas.
 */
extern BREthereumEther
walletEstimateTransactionFee (BREthereumWallet wallet,
                              BREthereumAmount amount,
                              int *overflow);

/**
 * Estimate the transaction fee (in Ether) for transferring amount.
 */
extern BREthereumEther
walletEstimateTransactionFeeDetailed (BREthereumWallet wallet,
                                      BREthereumAmount amount,
                                      BREthereumGasPrice price,
                                      BREthereumGas gas,
                                      int *overflow);

/**
 *
 * @param wallet
 * @param recvAddress
 * @param amount
 * @return
 */
extern BREthereumTransaction
walletCreateTransaction(BREthereumWallet wallet,
                        BREthereumAddress recvAddress,
                        BREthereumAmount amount);

/**
 *
 * You will have all sorts of problems with `nonce`...

 *   1) It needs to be derived from and consistent with the wallet's address nonce.
 *         walletSignTransaction() - the first point where the nonce is used - will fatal.
 *   2) If you create a transaction, thereby using/incrementing a nonce, but then don't submit
 *         the transaction, then *all* subsequent transaction will be pended *forever*.
 *
 * @warn If you create it, you must submit it.
 *
 * @param wallet
 * @param recvAddress
 * @param amount
 * @param gasPrice
 * @param gasLimit
 * @param nonce
 * @return
 */
extern BREthereumTransaction
walletCreateTransactionDetailed(BREthereumWallet wallet,
                                BREthereumAddress recvAddress,
                                BREthereumAmount amount,
                                BREthereumGasPrice gasPrice,
                                BREthereumGas gasLimit,
                                uint64_t nonce);

extern void
walletSignTransaction(BREthereumWallet wallet,
                      BREthereumTransaction transaction,
                      const char *paperKey);

extern void
walletSignTransactionWithPrivateKey(BREthereumWallet wallet,
                                    BREthereumTransaction transaction,
                                    BRKey privateKey);

/**
 * For `transaction`, get the 'signed transaction data' suitable for use in the RPC-JSON Ethereum
 * method `eth_sendRawTransaction` (once hex-encoded).
 *
 * @param wallet
 * @param transaction
 * @return
 */
extern BRRlpData  // uint8_t, EthereumByteArray
walletGetRawTransaction(BREthereumWallet wallet,
                        BREthereumTransaction transaction);

/**
 * For `transaction`, get the `signed transation data`, encoded in hex with an optional prefix
 * (typically "0x"), suitable for use in the RPC-JSON Ethereum method 'eth_sendRawTransaction"
 *
 */
extern char *
walletGetRawTransactionHexEncoded (BREthereumWallet wallet,
                                   BREthereumTransaction transaction,
                                   const char *prefix);

/**
 *
 */
extern BREthereumAddress
walletGetAddress (BREthereumWallet wallet);

/**
 * The wallet's amount type: ETHER or TOKEN
 */
extern BREthereumAmountType
walletGetAmountType (BREthereumWallet wallet);

/**
 * The wallet's token or NULL if the wallet holds ETHER.
 */
extern BREthereumToken
walletGetToken (BREthereumWallet wallet);

/**
 * The wallet's balance
 */
extern BREthereumAmount
walletGetBalance (BREthereumWallet wallet);

/**
 * Get the wallet's default Gas Limit.
 *
 * @param wallet
 * @return
 */
extern BREthereumGas
walletGetDefaultGasLimit(BREthereumWallet wallet);

/**
 * Set the wallet's default Gas Limit.  When creating a transaction, unless otherwise specified,
 * this GasLimit is used.  The default value depends on the wallet type: ETH, ERC20 Token or
 * Contract; therefore, set it carefully.
 *
 * @param wallet
 * @param gasLimit
 */
extern void
walletSetDefaultGasLimit(BREthereumWallet wallet, BREthereumGas gasLimit);

/**
 * Gets the wallet's default Gas Price.
 *
 * @param wallet
 * @return
 */
extern BREthereumGasPrice
walletGetDefaultGasPrice(BREthereumWallet wallet);

/**
 * Sets the wallets' default Gas Price.
 *
 * @param wallet
 * @param gasPrice
 */
extern void
walletSetDefaultGasPrice(BREthereumWallet wallet, BREthereumGasPrice gasPrice);

//
// Transactions
//
typedef int (*BREthereumTransactionPredicate) (void *context, BREthereumTransaction transaction, unsigned int index);
typedef void (*BREthereumTransactionWalker) (void *context, BREthereumTransaction transaction, unsigned int index);

extern int
transactionPredicateAny (void *ignore,
                         BREthereumTransaction transaction,
                         unsigned int index);

extern int
transactionPredicateStatus (BREthereumTransactionStatus status,
                            BREthereumTransaction transaction,
                            unsigned int index);

extern void
walletWalkTransactions (BREthereumWallet wallet,
                        void *context,
                        BREthereumTransactionPredicate predicate,
                        BREthereumTransactionWalker walker);

extern BREthereumTransaction
walletGetTransactionByHash (BREthereumWallet wallet,
                            BREthereumHash hash);

extern BREthereumTransaction
walletGetTransactionByNonce (BREthereumWallet wallet,
                             BREthereumAddress sourceAddress,
                             uint64_t nonce);

extern BREthereumTransaction
walletGetTransactionByIndex(BREthereumWallet wallet,
                            uint64_t index);

extern unsigned long
walletGetTransactionCount (BREthereumWallet wallet);
    
#ifdef __cplusplus
}
#endif

#endif /* BR_Ethereum_Wallet_H */
