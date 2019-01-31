//
//  BREthereum
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 2/24/18.
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

#ifndef BR_Ethereum_H
#define BR_Ethereum_H

#define SUPPORT_JSON_RPC

#include <stdint.h>
#include "BRKey.h"
#include "BREthereumEther.h"
#include "BREthereumGas.h"
#include "BREthereumAmount.h"
#include "BREthereumNetwork.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * An Ethereum Light Node
 *
 */
typedef struct BREthereumLightNodeRecord *BREthereumLightNode;

// Opaque Pointers
typedef int32_t BREthereumTransactionId;
typedef int32_t BREthereumAccountId;
typedef int32_t BREthereumWalletId;
typedef int32_t BREthereumBlockId;
typedef int32_t BREthereumListenerId;

//
// Errors - Right Up Front - 'The Emperor Has No Clothes' ??
//
typedef enum {
    SUCCESS,

    // Reference access
    ERROR_UNKNOWN_NODE,
    ERROR_UNKNOWN_TRANSACTION,
    ERROR_UNKNOWN_ACCOUNT,
    ERROR_UNKNOWN_WALLET,
    ERROR_UNKNOWN_BLOCK,
    ERROR_UNKNOWN_LISTENER,

    // Node
    ERROR_NODE_NOT_CONNECTED,

    // Transaction
    ERROR_TRANSACTION_X,

    // Acount
    // Wallet
    // Block
    // Listener

    // Numeric
    ERROR_NUMERIC_PARSE,

} BREthereumStatus;

//
// Listener
//
typedef enum {
    WALLET_EVENT_CREATED = 0,
    WALLET_EVENT_BALANCE_UPDATED,
    WALLET_EVENT_DEFAULT_GAS_LIMIT_UPDATED,
    WALLET_EVENT_DEFAULT_GAS_PRICE_UPDATED,
    WALLET_EVENT_DELETED
} BREthereumWalletEvent;

#define WALLET_NUMBER_OF_EVENTS  (1 + WALLET_EVENT_DELETED)

typedef enum {
    BLOCK_EVENT_CREATED = 0,
    BLOCK_EVENT_DELETED
} BREthereumBlockEvent;

#define BLOCK_NUMBER_OF_EVENTS (1 + BLOCK_EVENT_DELETED)

typedef enum {
    // Added/Removed from Wallet
    TRANSACTION_EVENT_ADDED = 0,
    TRANSACTION_EVENT_REMOVED,

    // Transaction State
    TRANSACTION_EVENT_CREATED,
    TRANSACTION_EVENT_SIGNED,
    TRANSACTION_EVENT_SUBMITTED,
    TRANSACTION_EVENT_BLOCKED,  // aka confirmed
    TRANSACTION_EVENT_ERRORED,


    TRANSACTION_EVENT_GAS_ESTIMATE_UPDATED,
    TRANSACTION_EVENT_BLOCK_CONFIRMATIONS_UPDATED
} BREthereumTransactionEvent;

#define TRANSACTION_NUMBER_OF_EVENTS (1 + TRANSACTION_EVENT_BLOCK_CONFIRMATIONS_UPDATED)

typedef void *BREthereumListenerContext;

typedef void (*BREthereumListenerWalletEventHandler)(BREthereumListenerContext context,
                                                     BREthereumLightNode node,
                                                     BREthereumWalletId wid,
                                                     BREthereumWalletEvent event,
                                                     BREthereumStatus status,
                                                     const char *errorDescription);

typedef void (*BREthereumListenerBlockEventHandler)(BREthereumListenerContext context,
                                                    BREthereumLightNode node,
                                                    BREthereumBlockId bid,
                                                    BREthereumBlockEvent event,
                                                    BREthereumStatus status,
                                                    const char *errorDescription);

typedef void (*BREthereumListenerTransactionEventHandler)(BREthereumListenerContext context,
                                                          BREthereumLightNode node,
                                                          BREthereumWalletId wid,
                                                          BREthereumTransactionId tid,
                                                          BREthereumTransactionEvent event,
                                                          BREthereumStatus status,
                                                          const char *errorDescription);

//
// BREthereumClient
//
// Type definitions for callback functions.  When configuring a LightNode these functions must be
// provided.  A LightNode has limited cababilities; these callbacks provide data back into the
// LightNode.
//
    typedef void *BREthereumClientContext;

    typedef void
    (*BREthereumClientHandlerGetBalance) (BREthereumClientContext context,
                                          BREthereumLightNode node,
                                          BREthereumWalletId wid,
                                          const char *address,
                                          int rid);
    
    typedef void
    (*BREthereumClientHandlerGetGasPrice) (BREthereumClientContext context,
                                           BREthereumLightNode node,
                                           BREthereumWalletId wid,
                                           int rid);
    
    typedef void
    (*BREthereumClientHandlerEstimateGas) (BREthereumClientContext context,
                                           BREthereumLightNode node,
                                           BREthereumWalletId wid,
                                           BREthereumTransactionId tid,
                                           const char *to,
                                           const char *amount,
                                           const char *data,
                                           int rid);
    
    typedef void
    (*BREthereumClientHandlerSubmitTransaction) (BREthereumClientContext context,
                                                 BREthereumLightNode node,
                                                 BREthereumWalletId wid,
                                                 BREthereumTransactionId tid,
                                                 const char *transaction,
                                                 int rid);
    
    typedef void
    (*BREthereumClientHandlerGetTransactions) (BREthereumClientContext context,
                                               BREthereumLightNode node,
                                               const char *address,
                                               int rid);
    
    typedef void
    (*BREthereumClientHandlerGetLogs) (BREthereumClientContext context,
                                       BREthereumLightNode node,
                                       const char *contract,
                                       const char *address,
                                       const char *event,
                                       int rid);

    typedef void
    (*BREthereumClientHandlerGetBlockNumber) (BREthereumClientContext context,
                                              BREthereumLightNode node,
                                              int rid);

    typedef void
    (*BREthereumClientHandlerGetNonce) (BREthereumClientContext context,
                                        BREthereumLightNode node,
                                        const char *address,
                                        int rid);

//
// Light Node Configuration
//
// Used to configure a light node appropriately for JSON_RPC or LES.  Starts with a
// BREthereumNetwork (one of ethereum{Mainnet,Testnet,Rinkeby} and then specifics for the
// type.
//
typedef struct {
    BREthereumClientContext funcContext;
    BREthereumClientHandlerGetBalance funcGetBalance;
    BREthereumClientHandlerGetGasPrice funcGetGasPrice;
    BREthereumClientHandlerEstimateGas funcEstimateGas;
    BREthereumClientHandlerSubmitTransaction funcSubmitTransaction;
    BREthereumClientHandlerGetTransactions funcGetTransactions;
    BREthereumClientHandlerGetLogs funcGetLogs;
    BREthereumClientHandlerGetBlockNumber funcGetBlockNumber;
    BREthereumClientHandlerGetNonce funcGetNonce;
} BREthereumClient;


//
// Configuration
//

/**
 * Create a Client
 */
extern BREthereumClient
ethereumClientCreate(BREthereumClientContext context,
                     BREthereumClientHandlerGetBalance funcGetBalance,
                     BREthereumClientHandlerGetGasPrice functGetGasPrice,
                     BREthereumClientHandlerEstimateGas funcEstimateGas,
                     BREthereumClientHandlerSubmitTransaction funcSubmitTransaction,
                     BREthereumClientHandlerGetTransactions funcGetTransactions,
                     BREthereumClientHandlerGetLogs funcGetLogs,
                     BREthereumClientHandlerGetBlockNumber funcGetBlockNumber,
                     BREthereumClientHandlerGetNonce funcGetNonce);

/**
 * Install 'wordList' as the default BIP39 Word List.  THIS IS SHARED MEMORY; DO NOT FREE wordList.
 *
 * @param wordList
 * @param wordListLength
 * @return
 */
extern int
installSharedWordList (const char *wordList[], int wordListLength);

//
// Light Node
//

/**
 * Create a LightNode managing the account associated with the paperKey.  (The `paperKey` must
 * use words from the defaul wordList (Use installSharedWordList).  The `paperKey` is used for
 * BIP-32 generation of keys; the same paper key must be used when signing transactions for
 * this node's account.
 */
extern BREthereumLightNode
ethereumCreate(BREthereumNetwork network,
               const char *paperKey);

/**
 * Create a LightNode managing the account associated with the publicKey.  Public key is a
 * 0x04-prefixed, 65-byte array in BRKey - as returned by
 * ethereumGetAccountPrimaryAddressPublicKey().
 */
extern BREthereumLightNode
ethereumCreateWithPublicKey(BREthereumNetwork network,
                            const BRKey publicKey);

/**
 * Create an Ethereum Account using `paperKey` for BIP-32 generation of keys.  The same paper key
 * must be used when signing transactions for this account.
 */
extern BREthereumAccountId
ethereumGetAccount(BREthereumLightNode node);

/**
 * Get the primary address for `account`.  This is the '0x'-prefixed, 40-char, hex encoded
 * string.  The returned char* is newly allocated, on each call - you MUST free() it.
 */
extern char *
ethereumGetAccountPrimaryAddress(BREthereumLightNode node);

/**
 * Get the public key for `account`.  This is a 0x04-prefixed, 65-byte array.  You own this
 * memory and you MUST free() it.
 */
extern BRKey
ethereumGetAccountPrimaryAddressPublicKey(BREthereumLightNode node);

/**
 * Get the private key for `account`.
 */
extern BRKey
ethereumGetAccountPrimaryAddressPrivateKey(BREthereumLightNode node,
                                           const char *paperKey);


/**
 * Create Ether from a string representing a number.  The string can *only* contain digits and
 * a single decimal point.  No '-', no '+' no 'e' (exponents).
 *
 * @param node
 * @param number the amount as a decimal (base10) number.
 * @param unit the number's unit - typically ETH, GWEI or WEI.
 * @param status This MUST NOT BE NULL. If assigned anything but OK, the return Ether is 0.  In
 *        practice you must reference `status` otherwise you'll have unknown errors with 0 ETH.
 */
extern BREthereumAmount
ethereumCreateEtherAmountString(BREthereumLightNode node,
                                const char *number,
                                BREthereumEtherUnit unit,
                                BRCoreParseStatus *status);

/**
 * Create Ether from a 'smallish' number and a unit
 *
 */
extern BREthereumAmount
ethereumCreateEtherAmountUnit(BREthereumLightNode node,
                              uint64_t amountInUnit,
                              BREthereumEtherUnit unit);

extern BREthereumAmount
ethereumCreateTokenAmountString(BREthereumLightNode node,
                                BREthereumToken token,
                                const char *number,
                                BREthereumTokenQuantityUnit unit,
                                BRCoreParseStatus *status);

/**
 * Convert `ether` to a char* in `unit`.  Caller owns the result and must call free()
 */
extern char *
ethereumCoerceEtherAmountToString(BREthereumLightNode node,
                                  BREthereumEther ether,
                                  BREthereumEtherUnit unit);

extern char *
ethereumCoerceTokenAmountToString(BREthereumLightNode node,
                                  BREthereumTokenQuantity token,
                                  BREthereumTokenQuantityUnit unit);

/**
 * Connect to the Ethereum Network;
 *
 * @param node
 * @return
 */
extern BREthereumBoolean
ethereumConnect(BREthereumLightNode node,
                BREthereumClient client);

extern BREthereumBoolean
ethereumDisconnect (BREthereumLightNode node);

extern BREthereumNetwork
ethereumGetNetwork (BREthereumLightNode node);

//
// Wallet
//
/**
 * Get the wallet for `account` holding ETHER.  This wallet is created, along with the account,
 * when a light node itself is created.
 *
 * @param node
 * @return
 */
extern BREthereumWalletId
ethereumGetWallet(BREthereumLightNode node);

/**
 * Get the wallet holding `token`.  If none exists, create one and return it.

 * @param node
 * @param token
 * @return
 */
extern BREthereumWalletId
ethereumGetWalletHoldingToken(BREthereumLightNode node,
                              BREthereumToken token);

extern uint64_t
ethereumWalletGetDefaultGasLimit(BREthereumLightNode node,
                                 BREthereumWalletId wid);

extern void
ethereumWalletSetDefaultGasLimit(BREthereumLightNode node,
                                 BREthereumWalletId wid,
                                 uint64_t gasLimit);

extern uint64_t
ethereumWalletGetGasEstimate(BREthereumLightNode node,
                             BREthereumWalletId wid,
                             BREthereumTransactionId transaction);

extern void
ethereumWalletSetDefaultGasPrice(BREthereumLightNode node,
                                 BREthereumWalletId wid,
                                 BREthereumEtherUnit unit,
                                 uint64_t value);

// Returns the ETH/GAS price in WEI.  IF the value is too large to express in WEI as a uint64_t
// then ZERO is returned.  Caution warranted.
extern uint64_t
ethereumWalletGetDefaultGasPrice(BREthereumLightNode node,
                                 BREthereumWalletId wid);

extern BREthereumAmount
ethereumWalletGetBalance(BREthereumLightNode node,
                         BREthereumWalletId wid);

extern char *
ethereumWalletGetBalanceEther(BREthereumLightNode node,
                              BREthereumWalletId wid,
                              BREthereumEtherUnit unit);
extern char *
ethereumWalletGetBalanceTokenQuantity(BREthereumLightNode node,
                                      BREthereumWalletId wid,
                                      BREthereumTokenQuantityUnit unit);

extern BREthereumEther
ethereumWalletEstimateTransactionFee(BREthereumLightNode node,
                                     BREthereumWalletId wid,
                                     BREthereumAmount amount,
                                     int *overflow);

/**
 * Create a transaction to transfer `amount` from `wallet` to `recvAddrss`.
 *
 * @param node
 * @param wallet the wallet
 * @param recvAddress A '0x' prefixed, strlen 42 Ethereum address.
 * @param amount to transfer
 * @return
 */
extern BREthereumTransactionId
ethereumWalletCreateTransaction(BREthereumLightNode node,
                                BREthereumWalletId wid,
                                const char *recvAddress,
                                BREthereumAmount amount);

/**
 * Sign the transaction using the wallet's account (for the sender's address).  The paperKey
 * is used to 'lookup' the private key.
 *
 * @param node
 * @param wallet
 * @param transaction
 * @param paperKey
 */
extern void // status, error
ethereumWalletSignTransaction(BREthereumLightNode node,
                              BREthereumWalletId wid,
                              BREthereumTransactionId tid,
                              const char *paperKey);

extern void // status, error
ethereumWalletSignTransactionWithPrivateKey(BREthereumLightNode node,
                                            BREthereumWalletId wid,
                                            BREthereumTransactionId tid,
                                            BRKey privateKey);

extern void // status, error
ethereumWalletSubmitTransaction(BREthereumLightNode node,
                                BREthereumWalletId wid,
                                BREthereumTransactionId tid);

/**
 * Returns a -1 terminated array of transaction identifiers.
 */
extern BREthereumTransactionId *
ethereumWalletGetTransactions(BREthereumLightNode node,
                              BREthereumWalletId wid);

/**
 * Returns -1 on invalid wid
 */
extern int // TODO: What in invalid wid?
ethereumWalletGetTransactionCount(BREthereumLightNode node,
                                  BREthereumWalletId wid);

/**
 * Token can be NULL => holds Ether
 *
 * @param node
 * @param wid
 * @param token
 * @return
 */
extern BREthereumBoolean
ethereumWalletHoldsToken(BREthereumLightNode node,
                         BREthereumWalletId wid,
                         BREthereumToken token);

extern BREthereumToken
ethereumWalletGetToken(BREthereumLightNode node,
                       BREthereumWalletId wid);

//
// Block
//
extern uint64_t
ethereumGetBlockHeight (BREthereumLightNode node);

extern uint64_t
ethereumBlockGetNumber (BREthereumLightNode node,
                        BREthereumBlockId bid);

extern uint64_t
ethereumBlockGetTimestamp (BREthereumLightNode node,
                        BREthereumBlockId bid);

extern char *
ethereumBlockGetHash (BREthereumLightNode node,
                      BREthereumBlockId bid);

//
//
// Transaction
//
//
extern char * // receiver, target
ethereumTransactionGetRecvAddress(BREthereumLightNode node,
                                  BREthereumTransactionId tid);

extern char * // sender, source
ethereumTransactionGetSendAddress(BREthereumLightNode node,
                                  BREthereumTransactionId tid);

extern char *
ethereumTransactionGetHash(BREthereumLightNode node,
                           BREthereumTransactionId tid);

extern char *
ethereumTransactionGetAmountEther(BREthereumLightNode node,
                                  BREthereumTransactionId tid,
                                  BREthereumEtherUnit unit);

extern char *
ethereumTransactionGetAmountTokenQuantity(BREthereumLightNode node,
                                          BREthereumTransactionId tid,
                                          BREthereumTokenQuantityUnit unit);

extern BREthereumAmount
ethereumTransactionGetAmount(BREthereumLightNode node,
                             BREthereumTransactionId tid);

extern BREthereumAmount
ethereumTransactionGetGasPriceToo(BREthereumLightNode node,
                                  BREthereumTransactionId tid);

extern char *
ethereumTransactionGetGasPrice(BREthereumLightNode node,
                               BREthereumTransactionId tid,
                               BREthereumEtherUnit unit);

extern uint64_t
ethereumTransactionGetGasLimit(BREthereumLightNode node,
                               BREthereumTransactionId tid);

extern uint64_t
ethereumTransactionGetGasUsed(BREthereumLightNode node,
                              BREthereumTransactionId tid);

extern uint64_t
ethereumTransactionGetNonce(BREthereumLightNode node,
                            BREthereumTransactionId transaction);

extern uint64_t
ethereumTransactionGetBlockNumber(BREthereumLightNode node,
                                  BREthereumTransactionId tid);

extern uint64_t
ethereumTransactionGetBlockTimestamp(BREthereumLightNode node,
                                     BREthereumTransactionId tid);

extern uint64_t
ethereumTransactionGetBlockConfirmations(BREthereumLightNode node,
                                         BREthereumTransactionId tid);

extern BREthereumBoolean
ethereumTransactionIsConfirmed(BREthereumLightNode node,
                               BREthereumTransactionId tid);

extern BREthereumBoolean
ethereumTransactionIsSubmitted(BREthereumLightNode node,
                               BREthereumTransactionId tid);

extern BREthereumBoolean
ethereumTransactionHoldsToken(BREthereumLightNode node,
                              BREthereumTransactionId tid,
                              BREthereumToken token);

extern BREthereumToken
ethereumTransactionGetToken(BREthereumLightNode node,
                            BREthereumTransactionId tid);

extern BREthereumEther
ethereumTransactionGetFee(BREthereumLightNode node,
                          BREthereumTransactionId tid,
                          int *overflow);

// Pending

/**
 * Light Node Transaction Status - these are Ethereum defined.
 */
typedef enum {
    NODE_TRANSACTION_STATUS_Unknown  = 0,  // (0): transaction is unknown
    NODE_TRANSACTION_STATUS_Queued   = 1,  // (1): transaction is queued (not processable yet)
    NODE_TRANSACTION_STATUS_Pending  = 2,  // (2): transaction is pending (processable)
    NODE_TRANSACTION_STATUS_Included = 3,  // (3): transaction is already included in the canonical chain. data contains an RLP-encoded [blockHash: B_32, blockNumber: P, txIndex: P] structure
    NODE_TRANSACTION_STATUS_Error    = 4   // (4): transaction sending failed. data contains a text error message
} BREthereumLightNodeTransactionStatus;


// ===================================
//
// Temporary
//
//
#if defined(SUPPORT_JSON_RPC)

extern void
lightNodeUpdateBlockNumber (BREthereumLightNode node);

extern void
lightNodeUpdateNonce (BREthereumLightNode node);

/**
 * Update the transactions for the node's account.  A JSON_RPC light node will call out to
 * BREthereumClientHandlerGetTransactions which is expected to query all transactions associated with the
 * accounts address and then the call out is to call back the 'announce transaction' callback.
 */
extern void
lightNodeUpdateTransactions (BREthereumLightNode node);

extern void
lightNodeUpdateLogs (BREthereumLightNode node,
                     BREthereumWalletId wid,
                     BREthereumContractEvent event);

//
// Wallet Updates
//
extern void
lightNodeUpdateWalletBalance (BREthereumLightNode node,
                              BREthereumWalletId wid);

extern void
lightNodeUpdateTransactionGasEstimate (BREthereumLightNode node,
                                       BREthereumWalletId wid,
                                       BREthereumTransactionId tid);

extern void
lightNodeUpdateWalletDefaultGasPrice (BREthereumLightNode node,
                                      BREthereumWalletId wid);


/**
 * Return the serialized raw data for `transaction`.  The value `*bytesPtr` points to a byte array;
 * the callee OWNs that byte array (and thus must call free).  The value `*bytesCountPtr` hold
 * the size of the byte array.
 *
 * @param node
 * @param transaction
 * @param bytesPtr
 * @param bytesCountPtr
 */

extern void
lightNodeFillTransactionRawData(BREthereumLightNode node,
                                BREthereumWalletId wid,
                                BREthereumTransactionId tid,
                                uint8_t **bytesPtr,
                                size_t *bytesCountPtr);

extern const char *
lightNodeGetTransactionRawDataHexEncoded(BREthereumLightNode node,
                                         BREthereumWalletId wid,
                                         BREthereumTransactionId tid,
                                         const char *prefix);

extern void
lightNodeAnnounceBlockNumber (BREthereumLightNode node,
                              const char *blockNumber,
                              int rid);

extern void
lightNodeAnnounceNonce (BREthereumLightNode node,
                        const char *strAddress,
                        const char *strNonce,
                        int rid);


// Some JSON_RPC call will occur to get all transactions associated with an account.  We'll
// process these transactions into the LightNode (associated with a wallet).  Thereafter
// a 'light node client' can get the announced transactions using non-JSON_RPC interfaces.
extern void
lightNodeAnnounceTransaction(BREthereumLightNode node,
                             int id,
                             const char *hash,
                             const char *from,
                             const char *to,
                             const char *contract,
                             const char *amount, // value
                             const char *gasLimit,
                             const char *gasPrice,
                             const char *data,
                             const char *nonce,
                             const char *gasUsed,
                             const char *blockNumber,
                             const char *blockHash,
                             const char *blockConfirmations,
                             const char *blockTransactionIndex,
                             const char *blockTimestamp,
                             // cumulative gas used,
                             // confirmations
                             // txreceipt_status
                             const char *isError);

extern void
lightNodeAnnounceLog (BREthereumLightNode node,
                      int id,
                      const char *strHash,
                      const char *strContract,
                      int topicCount,
                      const char **arrayTopics,
                      const char *strData,
                      const char *strGasPrice,
                      const char *strGasUsed,
                      const char *strLogIndex,
                      const char *strBlockNumber,
                      const char *strBlockTransactionIndex,
                      const char *strBlockTimestamp);

extern void
lightNodeAnnounceBalance (BREthereumLightNode node,
                          BREthereumWalletId wid,
                          const char *balance,
                          int rid);

extern void
lightNodeAnnounceGasPrice(BREthereumLightNode node,
                          BREthereumWalletId wid,
                          const char *gasEstimate,
                          int id);

extern void
lightNodeAnnounceGasEstimate (BREthereumLightNode node,
                              BREthereumWalletId wid,
                              BREthereumTransactionId tid,
                              const char *gasEstimate,
                              int rid);

extern void
lightNodeAnnounceSubmitTransaction(BREthereumLightNode node,
                                   BREthereumWalletId wid,
                                   BREthereumTransactionId tid,
                                   const char *hash,
                                   int rid);

#endif // ETHEREUM_LIGHT_NODE_USE_JSON_RPC

// ==========================
//
// Temporary
//
// ====================
//
// Listener
//
extern BREthereumListenerId
lightNodeAddListener (BREthereumLightNode node,
                      BREthereumListenerContext context,
                      BREthereumListenerWalletEventHandler walletEventHandler,
                      BREthereumListenerBlockEventHandler blockEventHandler,
                      BREthereumListenerTransactionEventHandler transactionEventHandler);

extern BREthereumBoolean
lightNodeHasListener (BREthereumLightNode node,
                      BREthereumListenerId lid);

extern BREthereumBoolean
lightNodeRemoveListener (BREthereumLightNode node,
                         BREthereumListenerId lid);

#ifdef __cplusplus
}
#endif

#endif // BR_Ethereum_H
