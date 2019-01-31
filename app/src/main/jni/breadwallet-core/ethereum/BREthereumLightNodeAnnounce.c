//
//  BREthereumLightNodeAnnouncer.c
//  BRCore
//
//  Created by Ed Gamble on 5/7/18.
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

#include <stdlib.h>
#include <stdio.h>
#include <string.h>
#include <pthread.h>
#include "BRArray.h"
#include "BREthereumPrivate.h"
#include "BREthereumLightNodePrivate.h"


//
//
//
extern void
lightNodeAnnounceBalance (BREthereumLightNode node,
                          BREthereumWalletId wid,
                          const char *balance,
                          int rid) {
    BREthereumStatus eventStatus = SUCCESS;
    const char *eventErrorDescription = NULL;

    // Passed in `balance` can be base 10 or 16.  Let UInt256Prase decide.
    BRCoreParseStatus status;
    UInt256 value = createUInt256Parse(balance, 0, &status);

    if (CORE_PARSE_OK != status) {
        eventStatus = ERROR_NUMERIC_PARSE;
    }
    else {
        pthread_mutex_lock(&node->lock);

        BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
        if (NULL == wallet) {
            eventStatus = ERROR_UNKNOWN_WALLET;
        }
        else {
            BREthereumAmount amount = (AMOUNT_ETHER == walletGetAmountType(wallet)
                                       ? amountCreateEther(etherCreate(value))
                                       : amountCreateToken(
                                                           createTokenQuantity(walletGetToken(wallet), value)));
            walletSetBalance(wallet, amount);
        }
        pthread_mutex_unlock(&node->lock);
    }

    lightNodeListenerAnnounceWalletEvent(node, wid, WALLET_EVENT_BALANCE_UPDATED,
                                         eventStatus,
                                         eventErrorDescription);
}

extern void
lightNodeAnnounceGasPrice(BREthereumLightNode node,
                          BREthereumWalletId wid,
                          const char *gasPrice,
                          int rid) {
    BREthereumStatus eventStatus = SUCCESS;
    const char *eventErrorDescription = NULL;

    BRCoreParseStatus status;
    UInt256 amount = createUInt256Parse(gasPrice, 0, &status);

    if (CORE_PARSE_OK != status) {
        eventStatus = ERROR_NUMERIC_PARSE;
    }
    else {

        pthread_mutex_lock(&node->lock);
        BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
        if (NULL == wallet) {
            eventStatus = ERROR_UNKNOWN_WALLET;
        }
        else {
            walletSetDefaultGasPrice(wallet, gasPriceCreate(etherCreate(amount)));
        }
        pthread_mutex_unlock(&node->lock);
    }

    lightNodeListenerAnnounceWalletEvent(node, wid, WALLET_EVENT_DEFAULT_GAS_PRICE_UPDATED,
                                         eventStatus,
                                         eventErrorDescription);
}

extern void
lightNodeAnnounceGasEstimate (BREthereumLightNode node,
                              BREthereumWalletId wid,
                              BREthereumTransactionId tid,
                              const char *gasEstimate,
                              int rid) {
    BREthereumStatus eventStatus = SUCCESS;
    const char *eventErrorDescription = NULL;

    BRCoreParseStatus status = CORE_PARSE_OK;
    UInt256 gas = createUInt256Parse(gasEstimate, 0, &status);

    if (CORE_PARSE_OK != status ||
        0 != gas.u64[1] || 0 != gas.u64[2] || 0 != gas.u64[3]) {
        eventStatus = ERROR_NUMERIC_PARSE;
    }
    else {

        pthread_mutex_lock(&node->lock);
        BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
        if (NULL == transaction) {
            eventStatus = ERROR_UNKNOWN_TRANSACTION;
        }
        else {
            transactionSetGasEstimate(transaction, gasCreate(gas.u64[0]));
        }
        pthread_mutex_unlock(&node->lock);
    }

    lightNodeListenerAnnounceTransactionEvent(node, wid, tid,
                                              TRANSACTION_EVENT_GAS_ESTIMATE_UPDATED,
                                              eventStatus,
                                              eventErrorDescription);
}

extern void
lightNodeAnnounceBlockNumber (BREthereumLightNode node,
                              const char *strBlockNumber,
                              int rid) {
    uint64_t blockNumber = strtoull(strBlockNumber, NULL, 0);
    lightNodeUpdateBlockHeight(node, blockNumber);
}

extern void
lightNodeAnnounceNonce (BREthereumLightNode node,
                        const char *strAddress,
                        const char *strNonce,
                        int rid) {
    uint64_t nonce = strtoull (strNonce, NULL, 0);
    BREthereumAddress address = accountGetPrimaryAddress (lightNodeGetAccount(node));
    assert (ETHEREUM_BOOLEAN_IS_TRUE (addressHasString(address, strAddress)));
    addressSetNonce(address, nonce, ETHEREUM_BOOLEAN_FALSE);
}

extern void
lightNodeAnnounceSubmitTransaction(BREthereumLightNode node,
                                   BREthereumWalletId wid,
                                   BREthereumTransactionId tid,
                                   const char *strHash,
                                   int id) {
    BREthereumStatus eventStatus = SUCCESS;
    const char *eventErrorDescription = NULL;

    pthread_mutex_lock(&node->lock);
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);
    if (NULL == wallet) {
        eventStatus = ERROR_UNKNOWN_WALLET;
    }
    else if (NULL == transaction) {
        eventStatus = ERROR_UNKNOWN_TRANSACTION;
    }
    else {
        walletTransactionSubmitted(wallet, transaction, hashCreate(strHash));
    }
    pthread_mutex_unlock(&node->lock);

    lightNodeListenerAnnounceTransactionEvent(node, wid, tid, TRANSACTION_EVENT_SUBMITTED,
                                              eventStatus,
                                              eventErrorDescription);
}

static BREthereumBlock
lightNodeAnnounceBlock(BREthereumLightNode node,
                       const char *strBlockNumber,
                       const char *strBlockHash,
                       const char *strBlockTimestamp) {
    // Build a block-ish
    BREthereumHash blockHash = hashCreate (strBlockHash);
    BREthereumBlock block = lightNodeLookupBlockByHash(node, blockHash);
    if (NULL == block) {
        uint64_t blockNumber = strtoull(strBlockNumber, NULL, 0);
        uint64_t blockTimestamp = strtoull(strBlockTimestamp, NULL, 0);

        block = createBlockMinimal(blockHash, blockNumber, blockTimestamp);
        lightNodeInsertBlock(node, block);

        BREthereumTransactionId bid = lightNodeLookupBlockId(node, block);
        lightNodeListenerAnnounceBlockEvent(node, bid, BLOCK_EVENT_CREATED, SUCCESS, NULL);
    }
    else {
        // We already have this block.
        // TODO: Assert on {number, timestamp}?
    }

    return block;
}

static int
lightNodeDataIsEmpty (BREthereumLightNode node, const char *data) {
    return NULL == data || 0 == strcmp ("", data) || 0 == strcmp ("0x", data);
}

static BREthereumToken
lightNodeAnnounceToken (BREthereumLightNode node,
                        const char *target,
                        const char *contract,
                        const char *data) {
    // If `data` is anything besides "0x", then we have a contract function call.  At that point
    // it seems we need to process `data` to extract the 'function + args' and then, if the
    // function is 'transfer() token' we can then and only then conclude that we have a token

    if (lightNodeDataIsEmpty(node, data)) return NULL;

    // There is contract data; see if it is a ERC20 function.
    BREthereumContractFunction function = contractLookupFunctionForEncoding(contractERC20, data);

    // Not an ERC20 token
    if (NULL == function) return NULL;

    // See if we have an existing token.
    BREthereumToken token = tokenLookup(target);
    if (NULL == token) token = tokenLookup(contract);

    // We found a token...
    if (NULL != token) return token;

    // ... we didn't find a token - we should create is dynamically.
    fprintf (stderr, "Ignoring transaction for unknown ERC20 token at '%s'", target);
    return NULL;
}

static BREthereumWallet
lightNodeAnnounceWallet(BREthereumLightNode node,
                        BREthereumToken token) {
    BREthereumWalletId wid = (NULL == token
                              ? lightNodeGetWallet(node)
                              : lightNodeGetWalletHoldingToken(node, token));
    return lightNodeLookupWallet(node, wid);
}

extern void
lightNodeAnnounceTransaction(BREthereumLightNode node,
                             int id,
                             const char *hashString,
                             const char *from,
                             const char *to,
                             const char *contract,
                             const char *amountString, // value
                             const char *gasLimitString,
                             const char *gasPriceString,
                             const char *data,
                             const char *strNonce,
                             const char *strGasUsed,
                             const char *blockNumber,
                             const char *blockHash,
                             const char *strBlockConfirmations,
                             const char *strBlockTransactionIndex,
                             const char *blockTimestamp,
                             const char *isError) {
    BREthereumTransactionId tid = -1;

    BREthereumAddress primaryAddress = accountGetPrimaryAddress(node->account);

    assert (ETHEREUM_BOOLEAN_IS_TRUE(addressHasString(primaryAddress, from))
            || ETHEREUM_BOOLEAN_IS_TRUE(addressHasString(primaryAddress, to)));

    // primaryAddress is either the transaction's `source` or `target`.
    BREthereumBoolean isSource = addressHasString(primaryAddress, from);

    // Get the nonceValue
    uint64_t nonce = strtoull(strNonce, NULL, 10); // TODO: Assumes `nonce` is uint64_t; which it is for now

    pthread_mutex_lock(&node->lock);

    // Find or create a block.  No point in doing this until we have a transaction of interest
    BREthereumBlock block = lightNodeAnnounceBlock(node, blockNumber, blockHash, blockTimestamp);
    assert (NULL != block);

    // The transaction's index within the block.
    unsigned int blockTransactionIndex = (unsigned int) strtoul(strBlockTransactionIndex, NULL, 10);

    // All transactions apply to the ETH wallet.
    BREthereumWallet wallet = node->walletHoldingEther;
    BREthereumWalletId wid = lightNodeLookupWalletId(node, wallet);

    // Get the transaction's hash.
    BREthereumHash hash = hashCreate(hashString);

    // Look for a pre-existing transaction
    BREthereumTransaction transaction = walletGetTransactionByHash(wallet, hash);

    // If we did not have a transaction for 'hash' it might be (might likely be) a newly submitted
    // transaction that we are holding but that doesn't have a hash yet.  This will *only* apply
    // if we are the source.
    if (NULL == transaction && ETHEREUM_BOOLEAN_IS_TRUE(isSource))
        transaction = walletGetTransactionByNonce(wallet, primaryAddress, nonce);

    // If we still don't have a transaction (with 'hash' or 'nonce'); then create one.
    if (NULL == transaction) {
        // TODO: Handle Status Error
        BRCoreParseStatus status;

        BREthereumAddress sourceAddr =
        (ETHEREUM_BOOLEAN_IS_TRUE(isSource) ? primaryAddress : createAddress(from));

        BREthereumAddress targetAddr =
        (ETHEREUM_BOOLEAN_IS_TRUE(isSource) ? createAddress(to) : primaryAddress);

        // Get the amount; this will be '0' if this is a token transfer
        BREthereumAmount amount =
        amountCreateEther(etherCreate(createUInt256Parse(amountString, 10, &status)));

        // Easily extract the gasPrice and gasLimit.
        BREthereumGasPrice gasPrice =
        gasPriceCreate(etherCreate(createUInt256Parse(gasPriceString, 10, &status)));

        BREthereumGas gasLimit =
        gasCreate(strtoull(gasLimitString, NULL, 0));

        // Finally, get ourselves a transaction.
        transaction = transactionCreate(sourceAddr,
                                        targetAddr,
                                        amount,
                                        gasPrice,
                                        gasLimit,
                                        nonce);

        // With a new transaction:
        //
        //   a) add to the light node
        tid = lightNodeInsertTransaction(node, transaction);
        //
        //  b) add to the wallet
        walletHandleTransaction(wallet, transaction);
        //
        //  c) announce the wallet update
        lightNodeListenerAnnounceTransactionEvent(node, wid, tid,
                                                  TRANSACTION_EVENT_ADDED,
                                                  SUCCESS, NULL);
        //
        //  d) announce as submitted (=> there is a hash, submitted by 'us' or 'them')
        walletTransactionSubmitted(wallet, transaction, hash);

    }
    if (-1 == tid)
        tid = lightNodeLookupTransactionId(node, transaction);

    BREthereumGas gasUsed = gasCreate(strtoull(strGasUsed, NULL, 0));

    // TODO: Process 'state' properly - errors?

    // Get the current status.
    BREthereumTransactionStatus status = transactionGetStatus(transaction);

    // Update the status as blocked
    if (TRANSACTION_BLOCKED != status)
        walletTransactionBlocked(wallet, transaction, gasUsed,
                                 blockGetNumber(block),
                                 blockGetTimestamp(block),
                                 blockTransactionIndex);

    // Announce a transaction event.  If already 'BLOCKED', then update CONFIRMATIONS.
    lightNodeListenerAnnounceTransactionEvent(node, wid, tid,
                                              (TRANSACTION_BLOCKED == status
                                               ? TRANSACTION_EVENT_BLOCK_CONFIRMATIONS_UPDATED
                                               : TRANSACTION_EVENT_BLOCKED),
                                              SUCCESS,
                                              NULL);

    // Hmmm...
    pthread_mutex_unlock(&node->lock);
}

//  {
//    "blockNumber":"1627184",
//    "timeStamp":"1516477482",
//    "hash":     "0x4f992a47727f5753a9272abba36512c01e748f586f6aef7aed07ae37e737d220",
//    "blockHash":"0x0ef0110d68ee3af220e0d7c10d644fea98252180dbfc8a94cab9f0ea8b1036af",
//    "transactionIndex":"3",
//    "from":"0x0ea166deef4d04aaefd0697982e6f7aa325ab69c",
//    "to":"0xde0b295669a9fd93d5f28d9ec85e40f4cb697bae",
//    "nonce":"118",
//    "value":"11113000000000",
//    "gas":"21000",
//    "gasPrice":"21000000000",
//    "isError":"0",
//    "txreceipt_status":"1",
//    "input":"0x",
//    "contractAddress":"",
//    "cumulativeGasUsed":"106535",
//    "gasUsed":"21000",
//    "confirmations":"339050"}

/**
 *
 *
 */
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
                      const char *strBlockTimestamp) {
    BREthereumTransactionId tid = -1;

    pthread_mutex_lock(&node->lock);

    // Token of interest
    BREthereumToken token = tokenLookup(strContract);
    if (NULL == token) { pthread_mutex_unlock(&node->lock); return; } // uninteresting token

    // Event of interest
    BREthereumContractEvent event = contractLookupEventForTopic (contractERC20, arrayTopics[0]);
    if (NULL == event || event != eventERC20Transfer) { pthread_mutex_unlock(&node->lock); return; }; // uninteresting event

    // Find or create a block.  No point in doing this until we have a transaction of interest
    const char *strBlockHash = strHash;  // TODO: actual hash argument; utterly wrong.
    BREthereumBlock block = lightNodeAnnounceBlock(node, strBlockNumber, strBlockHash, strBlockTimestamp);
    assert (NULL != block);

    unsigned int blockTransactionIndex = (unsigned int) strtoul(strBlockTransactionIndex, NULL, 0);

    // Wallet for token
    BREthereumWallet wallet = lightNodeAnnounceWallet(node, token);
    BREthereumWalletId wid = lightNodeLookupWalletId(node, wallet);

    // Existing transaction
    BREthereumHash hash = hashCreate(strHash);
    BREthereumTransaction transaction = walletGetTransactionByHash(wallet, hash);

    BREthereumGas gasUsed = gasCreate(strtoull(strGasUsed, NULL, 0));

    // Create a token transaction
    if (NULL == transaction) {

        // Parse the topic data - we fake it becasue we 'know' topics indices
        BREthereumAddress sourceAddr = createAddress(
                                                     eventERC20TransferDecodeAddress(event, arrayTopics[1]));
        BREthereumAddress targetAddr = createAddress(
                                                     eventERC20TransferDecodeAddress(event, arrayTopics[2]));

        BRCoreParseStatus status = CORE_PARSE_OK;

        BREthereumAmount amount =
        amountCreateToken(createTokenQuantity(token, eventERC20TransferDecodeUInt256(event,
                                                                                     strData,
                                                                                     &status)));

        BREthereumGasPrice gasPrice =
        gasPriceCreate(etherCreate(createUInt256Parse(strGasPrice, 0, &status)));

        transaction = transactionCreate(sourceAddr, targetAddr, amount, gasPrice, gasUsed, 0);

        // With a new transaction:
        //
        //   a) add to the light node
        tid = lightNodeInsertTransaction(node, transaction);
        //
        //  b) add to the wallet
        walletHandleTransaction(wallet, transaction);
        //
        //  c) announce the wallet update
        lightNodeListenerAnnounceTransactionEvent(node, wid, tid, TRANSACTION_EVENT_ADDED, SUCCESS, NULL);

        //
        //  d) announce as submitted.
        walletTransactionSubmitted(wallet, transaction, hash);

    }

    if (-1 == tid)
        tid = lightNodeLookupTransactionId(node, transaction);

    // TODO: Process 'state' properly - errors?

    // Get the current status.
    BREthereumTransactionStatus status = transactionGetStatus(transaction);

    // Update the status as blocked
    if (TRANSACTION_BLOCKED != status)
        walletTransactionBlocked(wallet, transaction, gasUsed,
                                 blockGetNumber(block),
                                 blockGetTimestamp(block),
                                 blockTransactionIndex);

    // Announce a transaction event.  If already 'BLOCKED', then update CONFIRMATIONS.
    lightNodeListenerAnnounceTransactionEvent(node, wid, tid,
                                              (TRANSACTION_BLOCKED == status
                                               ? TRANSACTION_EVENT_BLOCK_CONFIRMATIONS_UPDATED
                                               : TRANSACTION_EVENT_BLOCKED),
                                              SUCCESS,
                                              NULL);

    // Hmmmm...
    pthread_mutex_unlock(&node->lock);
}


//http://ropsten.etherscan.io/api?module=logs&action=getLogs&fromBlock=0&toBlock=latest&address=0x722dd3f80bac40c951b51bdd28dd19d435762180&topic0=0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef&topic1=0x000000000000000000000000bDFdAd139440D2Db9BA2aa3B7081C2dE39291508&topic1_2_opr=or&topic2=0x000000000000000000000000bDFdAd139440D2Db9BA2aa3B7081C2dE39291508
//{
//    "status":"1",
//    "message":"OK",
//    "result":[
//              {
//              "address":"0x722dd3f80bac40c951b51bdd28dd19d435762180",
//              "topics":["0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
//                        "0x0000000000000000000000000000000000000000000000000000000000000000",
//                        "0x000000000000000000000000bdfdad139440d2db9ba2aa3b7081c2de39291508"],
//              "data":"0x0000000000000000000000000000000000000000000000000000000000002328",
//              "blockNumber":"0x1e487e",
//              "timeStamp":"0x59fa1ac9",
//              "gasPrice":"0xba43b7400",
//              "gasUsed":"0xc64e",
//              "logIndex":"0x",
//              "transactionHash":"0xa37bd8bd8b1fa2838ef65aec9f401f56a6279f99bb1cfb81fa84e923b1b60f2b",
//              "transactionIndex":"0x"},
//
//              {
//              "address":"0x722dd3f80bac40c951b51bdd28dd19d435762180",
//              "topics":["0xddf252ad1be2c89b69c2b068fc378daa952ba7f163c4a11628f55a4df523b3ef",
//                        "0x000000000000000000000000bdfdad139440d2db9ba2aa3b7081c2de39291508",
//                        "0x0000000000000000000000006c0fe9f8f018e68e2f0bee94ab41b75e71df094d"],
//              "data":"0x00000000000000000000000000000000000000000000000000000000000003e8",
//              "blockNumber":"0x1e54a5",
//              "timeStamp":"0x59fac771",
//              "gasPrice":"0x4a817c800",
//              "gasUsed":"0xc886",
//              "logIndex":"0x",
//              "transactionHash":"0x393927b491208dd8c7415cd749a2559b345d47c800a5adfa8e3bd5307acb7de0",
//              "transactionIndex":"0x1"},
//
//
//              ...
//              }
