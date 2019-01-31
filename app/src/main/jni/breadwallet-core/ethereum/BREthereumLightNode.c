//
//  BREthereumLightNode
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 3/5/18.
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
#include <assert.h>
#include <string.h>
#include <stdarg.h>
#include <stdio.h>  // sprintf
#include <pthread.h>
#include <unistd.h>
#include "BRArray.h"
#include <BRBIP39Mnemonic.h>

#include "BREthereumPrivate.h"
#include "BREthereumLightNodePrivate.h"
#include "event/BREvent.h"
#include "BREthereum.h"
#include "BREthereumLightNode.h"


//
// Light Node Client
//
extern BREthereumClient
ethereumClientCreate(BREthereumClientContext context,
                     BREthereumClientHandlerGetBalance funcGetBalance,
                     BREthereumClientHandlerGetGasPrice funcGetGasPrice,
                     BREthereumClientHandlerEstimateGas funcEstimateGas,
                     BREthereumClientHandlerSubmitTransaction funcSubmitTransaction,
                     BREthereumClientHandlerGetTransactions funcGetTransactions,
                     BREthereumClientHandlerGetLogs funcGetLogs,
                     BREthereumClientHandlerGetBlockNumber funcGetBlockNumber,
                     BREthereumClientHandlerGetNonce funcGetNonce) {

    BREthereumClient client;
    client.funcContext = context;
    client.funcGetBalance = funcGetBalance;
    client.funcGetGasPrice = funcGetGasPrice;
    client.funcEstimateGas = funcEstimateGas;
    client.funcSubmitTransaction = funcSubmitTransaction;
    client.funcGetTransactions = funcGetTransactions;
    client.funcGetLogs = funcGetLogs;
    client.funcGetBlockNumber = funcGetBlockNumber;
    client.funcGetNonce = funcGetNonce;
    return client;
}

//
// Light Node
//
extern BREthereumLightNode
createLightNode (BREthereumNetwork network,
                 BREthereumAccount account) {
    BREthereumLightNode node = (BREthereumLightNode) calloc (1, sizeof (struct BREthereumLightNodeRecord));
    node->state = LIGHT_NODE_CREATED;
    node->type = FIXED_LIGHT_NODE_TYPE;
    node->network = network;
    node->account = account;
    array_new(node->wallets, DEFAULT_WALLET_CAPACITY);
    array_new(node->transactions, DEFAULT_TRANSACTION_CAPACITY);
    array_new(node->blocks, DEFAULT_BLOCK_CAPACITY);
    array_new(node->listeners, DEFAULT_LISTENER_CAPACITY);

    {
        pthread_mutexattr_t attr;
        pthread_mutexattr_init(&attr);
        pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);

        pthread_mutex_init(&node->lock, &attr);
        pthread_mutexattr_destroy(&attr);
    }

    // Create and then start the eventHandler
    node->handlerForListener = eventHandlerCreate(listenerEventTypes, listenerEventTypesCount);
    eventHandlerStart(node->handlerForListener);
    
    // Create a default ETH wallet; other wallets will be created 'on demand'
    node->walletHoldingEther = walletCreate(node->account,
                                            node->network);
    lightNodeInsertWallet(node, node->walletHoldingEther);

    return node;
}

extern BREthereumAccount
lightNodeGetAccount (BREthereumLightNode node) {
    return node->account;
}

extern BREthereumNetwork
lightNodeGetNetwork (BREthereumLightNode node) {
    return node->network;
}

//
// Listener
//
extern BREthereumListenerId
lightNodeAddListener (BREthereumLightNode node,
                      BREthereumListenerContext context,
                      BREthereumListenerWalletEventHandler walletEventHandler,
                      BREthereumListenerBlockEventHandler blockEventHandler,
                      BREthereumListenerTransactionEventHandler transactionEventHandler) {
    BREthereumListenerId lid = -1;
    BREthereumLightNodeListener listener;

    listener.context = context;
    listener.walletEventHandler = walletEventHandler;
    listener.blockEventHandler = blockEventHandler;
    listener.transactionEventHandler = transactionEventHandler;

    pthread_mutex_lock(&node->lock);
    array_add (node->listeners, listener);
    lid = (BREthereumListenerId) (array_count (node->listeners) - 1);
    pthread_mutex_unlock(&node->lock);

    return lid;
}

extern BREthereumBoolean
lightNodeHasListener (BREthereumLightNode node,
                      BREthereumListenerId lid) {
    return (0 <= lid && lid < array_count(node->listeners)
        && NULL != node->listeners[lid].context
        && (NULL != node->listeners[lid].walletEventHandler ||
            NULL != node->listeners[lid].blockEventHandler  ||
            NULL != node->listeners[lid].transactionEventHandler)
            ? ETHEREUM_BOOLEAN_TRUE
            : ETHEREUM_BOOLEAN_FALSE);
}

extern BREthereumBoolean
lightNodeRemoveListener (BREthereumLightNode node,
                         BREthereumListenerId lid) {
    if (0 <= lid && lid < array_count(node->listeners)) {
        memset (&node->listeners[lid], 0, sizeof (BREthereumLightNodeListener));
        return ETHEREUM_BOOLEAN_TRUE;
    }
    return ETHEREUM_BOOLEAN_FALSE;
}

//
// Connect // Disconnect
//
#define PTHREAD_STACK_SIZE (512 * 1024)
#define PTHREAD_SLEEP_SECONDS (15)

static BREthereumClient nullClient;

typedef void* (*ThreadRoutine) (void*);

static void *
lightNodeThreadRoutine (BREthereumLightNode node) {
    node->state = LIGHT_NODE_CONNECTED;

    while (1) {
        if (LIGHT_NODE_DISCONNECTING == node->state) break;
        pthread_mutex_lock(&node->lock);

        lightNodeUpdateBlockNumber(node);
        lightNodeUpdateNonce(node);

        // We'll query all transactions for this node's account.  That will give us a shot at
        // getting the nonce for the account's address correct.  We'll save all the transactions and
        // then process them into wallet as wallets exist.
        lightNodeUpdateTransactions(node);

        // Similarly, we'll query all logs for this node's account.  We'll process these into
        // (token) transactions and associate with their wallet.
        lightNodeUpdateLogs(node, -1, eventERC20Transfer);

        // For all the known wallets, get their balance.
        for (int i = 0; i < array_count(node->wallets); i++)
            lightNodeUpdateWalletBalance (node, i);

        pthread_mutex_unlock(&node->lock);

        if (LIGHT_NODE_DISCONNECTING == node->state) break;
        if (1 == sleep (PTHREAD_SLEEP_SECONDS)) {}
    }

    node->state = LIGHT_NODE_DISCONNECTED;
    
    // TODO: This was needed, but I forgot why.
    //     node->type = NODE_TYPE_NONE;
    
    pthread_detach(node->thread);
    return NULL;
}

extern BREthereumBoolean
lightNodeConnect(BREthereumLightNode node,
                 BREthereumClient client) {
    pthread_attr_t attr;

    switch (node->state) {
        case LIGHT_NODE_CONNECTING:
        case LIGHT_NODE_CONNECTED:
        case LIGHT_NODE_DISCONNECTING:
            return ETHEREUM_BOOLEAN_FALSE;

        case LIGHT_NODE_CREATED:
        case LIGHT_NODE_DISCONNECTED:
        case LIGHT_NODE_ERRORED: {
            if (0 != pthread_attr_init(&attr)) {
                // Unable to initialize attr
                node->state = LIGHT_NODE_ERRORED;
                return ETHEREUM_BOOLEAN_FALSE;
            } else if (0 != pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED) ||
                       0 != pthread_attr_setstacksize(&attr, PTHREAD_STACK_SIZE)) {
                // Unable to fully setup the thread w/ task
                node->state = LIGHT_NODE_ERRORED;
                pthread_attr_destroy(&attr);
                return ETHEREUM_BOOLEAN_FALSE;
            }
            else {
                // CORE-41: Get the client set before lightNodeThreadRoutine(node) runs
                node->client = client;
                // CORE-92, DROID-634: Get the state set to avoid a race with pthread_create().
                node->state = LIGHT_NODE_CONNECTING;
                if  (0 != pthread_create(&node->thread, &attr, (ThreadRoutine) lightNodeThreadRoutine, node)) {
                    node->client = nullClient;
                    node->state = LIGHT_NODE_ERRORED;
                    pthread_attr_destroy(&attr);
                    return ETHEREUM_BOOLEAN_FALSE;
                }
            }

            // Running
            return ETHEREUM_BOOLEAN_TRUE;
        }
    }
}

extern BREthereumBoolean
lightNodeDisconnect (BREthereumLightNode node) {
    node->state = LIGHT_NODE_DISCONNECTING;
    return ETHEREUM_BOOLEAN_TRUE;
}

//
// Wallet Lookup & Insert
//
extern BREthereumWallet
lightNodeLookupWallet(BREthereumLightNode node,
                      BREthereumWalletId wid) {
    BREthereumWallet wallet = NULL;

    pthread_mutex_lock(&node->lock);
    wallet = (0 <= wid && wid < array_count(node->wallets)
              ? node->wallets[wid]
              : NULL);
    pthread_mutex_unlock(&node->lock);
    return wallet;
}

extern BREthereumWalletId
lightNodeLookupWalletId(BREthereumLightNode node,
                        BREthereumWallet wallet) {
    BREthereumWalletId wid = -1;

    pthread_mutex_lock(&node->lock);
    for (int i = 0; i < array_count (node->wallets); i++)
        if (wallet == node->wallets[i]) {
            wid = i;
            break;
        }
    pthread_mutex_unlock(&node->lock);
    return wid;
}

extern void
lightNodeInsertWallet (BREthereumLightNode node,
                       BREthereumWallet wallet) {
    BREthereumWalletId wid = -1;
    pthread_mutex_lock(&node->lock);
    array_add (node->wallets, wallet);
    wid = (BREthereumWalletId) (array_count(node->wallets) - 1);
    pthread_mutex_unlock(&node->lock);
    lightNodeListenerAnnounceWalletEvent(node, wid, WALLET_EVENT_CREATED, SUCCESS, NULL);
}

//
// Wallet (Actions)
//
extern BREthereumWalletId
lightNodeGetWallet(BREthereumLightNode node) {
    return lightNodeLookupWalletId (node, node->walletHoldingEther);
}

extern BREthereumWalletId
lightNodeGetWalletHoldingToken(BREthereumLightNode node,
                               BREthereumToken token) {
    BREthereumWalletId wid = -1;

    pthread_mutex_lock(&node->lock);
    for (int i = 0; i < array_count(node->wallets); i++)
        if (token == walletGetToken(node->wallets[i])) {
            wid = i;
            break;
        }

    if (-1 == wid) {
        BREthereumWallet wallet = walletCreateHoldingToken(node->account,
                                                           node->network,
                                                           token);
        lightNodeInsertWallet(node, wallet);
        wid = lightNodeLookupWalletId(node, wallet);
    }

    pthread_mutex_unlock(&node->lock);
    return wid;
}


extern BREthereumTransactionId
lightNodeWalletCreateTransaction(BREthereumLightNode node,
                                 BREthereumWallet wallet,
                                 const char *recvAddress,
                                 BREthereumAmount amount) {
    BREthereumTransactionId tid = -1;
    BREthereumWalletId wid = -1;

    pthread_mutex_lock(&node->lock);

    BREthereumTransaction transaction =
      walletCreateTransaction(wallet, createAddress(recvAddress), amount);

    tid = lightNodeInsertTransaction(node, transaction);
    wid = lightNodeLookupWalletId(node, wallet);

    pthread_mutex_unlock(&node->lock);

    lightNodeListenerAnnounceTransactionEvent(node, wid, tid, TRANSACTION_EVENT_CREATED, SUCCESS, NULL);
    lightNodeListenerAnnounceTransactionEvent(node, wid, tid, TRANSACTION_EVENT_ADDED, SUCCESS, NULL);

    return tid;
}

extern void // status, error
lightNodeWalletSignTransaction(BREthereumLightNode node,
                               BREthereumWallet wallet,
                               BREthereumTransaction transaction,
                               BRKey privateKey) {
    walletSignTransactionWithPrivateKey(wallet, transaction, privateKey);
    lightNodeListenerAnnounceTransactionEvent(node,
                                              lightNodeLookupWalletId(node, wallet),
                                              lightNodeLookupTransactionId(node, transaction),
                                              TRANSACTION_EVENT_SIGNED,
                                              SUCCESS,
                                              NULL);
}

extern void // status, error
lightNodeWalletSignTransactionWithPaperKey(BREthereumLightNode node,
                                           BREthereumWallet wallet,
                                           BREthereumTransaction transaction,
                                           const char *paperKey) {
    walletSignTransaction(wallet, transaction, paperKey);
    lightNodeListenerAnnounceTransactionEvent(node,
                                              lightNodeLookupWalletId(node, wallet),
                                              lightNodeLookupTransactionId(node, transaction),
                                              TRANSACTION_EVENT_SIGNED,
                                              SUCCESS,
                                              NULL);
}

extern void // status, error
lightNodeWalletSubmitTransaction(BREthereumLightNode node,
                                 BREthereumWallet wallet,
                                 BREthereumTransaction transaction) {
    char *rawTransaction = walletGetRawTransactionHexEncoded(wallet, transaction, "0x");

    switch (node->type) {
        case NODE_TYPE_LES:
            // TODO: Fall-through on error, perhaps

        case NODE_TYPE_JSON_RPC: {
            node->client.funcSubmitTransaction
                    (node->client.funcContext,
                     node,
                     lightNodeLookupWalletId(node, wallet),
                     lightNodeLookupTransactionId(node, transaction),
                     rawTransaction,
                     ++node->requestId);

            break;
        }

        case NODE_TYPE_NONE:
            break;
    }
    free(rawTransaction);
}

extern BREthereumTransactionId *
lightNodeWalletGetTransactions(BREthereumLightNode node,
                               BREthereumWallet wallet) {
    pthread_mutex_lock(&node->lock);

    unsigned long count = walletGetTransactionCount(wallet);
    BREthereumTransactionId *transactions = calloc (count + 1, sizeof (BREthereumTransactionId));

    for (unsigned long index = 0; index < count; index++) {
        transactions [index] = lightNodeLookupTransactionId(node, walletGetTransactionByIndex(wallet, index));
    }
    transactions[count] = -1;

    pthread_mutex_unlock(&node->lock);
    return transactions;
}

extern int
lightNodeWalletGetTransactionCount(BREthereumLightNode node,
                                   BREthereumWallet wallet) {
    int count = -1;

    pthread_mutex_lock(&node->lock);
    if (NULL != wallet) count = (int) walletGetTransactionCount(wallet);
    pthread_mutex_unlock(&node->lock);

    return count;
}

extern void
lightNodeWalletSetDefaultGasLimit(BREthereumLightNode node,
                                  BREthereumWallet wallet,
                                  BREthereumGas gasLimit) {
    walletSetDefaultGasLimit(wallet, gasLimit);
    lightNodeListenerAnnounceWalletEvent(node,
                                         lightNodeLookupWalletId(node, wallet),
                                         WALLET_EVENT_DEFAULT_GAS_LIMIT_UPDATED,
                                         SUCCESS,
                                         NULL);
}

extern void
lightNodeWalletSetDefaultGasPrice(BREthereumLightNode node,
                                  BREthereumWallet wallet,
                                  BREthereumGasPrice gasPrice) {
    walletSetDefaultGasPrice(wallet, gasPrice);
    lightNodeListenerAnnounceWalletEvent(node,
                                         lightNodeLookupWalletId(node, wallet),
                                         WALLET_EVENT_DEFAULT_GAS_PRICE_UPDATED,
                                         SUCCESS,
                                         NULL);
}

//
// Blocks
//
extern BREthereumBlock
lightNodeLookupBlockByHash(BREthereumLightNode node,
                           const BREthereumHash hash) {
    BREthereumBlock block = NULL;

    pthread_mutex_lock(&node->lock);
    for (int i = 0; i < array_count(node->blocks); i++)
        if (ETHEREUM_COMPARISON_EQ == hashCompare(hash, blockGetHash(node->blocks[i]))) {
            block = node->blocks[i];
            break;
        }
    pthread_mutex_unlock(&node->lock);
    return block;
}

extern BREthereumBlock
lightNodeLookupBlock(BREthereumLightNode node,
                     BREthereumBlockId bid) {
    BREthereumBlock block = NULL;

    pthread_mutex_lock(&node->lock);
    block = (0 <= bid && bid < array_count(node->blocks)
                   ? node->blocks[bid]
                   : NULL);
    pthread_mutex_unlock(&node->lock);
    return block;
}

extern BREthereumBlockId
lightNodeLookupBlockId (BREthereumLightNode node,
                        BREthereumBlock block) {
    BREthereumBlockId bid = -1;

    pthread_mutex_lock(&node->lock);
    for (int i = 0; i < array_count(node->blocks); i++)
        if (block == node->blocks[i]) {
            bid = i;
            break;
        }
    pthread_mutex_unlock(&node->lock);
    return bid;
}

extern void
lightNodeInsertBlock (BREthereumLightNode node,
                      BREthereumBlock block) {
    BREthereumBlockId bid = -1;
    pthread_mutex_lock(&node->lock);
    array_add(node->blocks, block);
    bid = (BREthereumBlockId) (array_count(node->blocks) - 1);
    pthread_mutex_unlock(&node->lock);
    lightNodeListenerAnnounceBlockEvent(node, bid, BLOCK_EVENT_CREATED, SUCCESS, NULL);
}





extern uint64_t
lightNodeGetBlockHeight(BREthereumLightNode node) {
    return node->blockHeight;
}

extern void
lightNodeUpdateBlockHeight(BREthereumLightNode node,
                           uint64_t blockHeight) {
    if (blockHeight > node->blockHeight)
        node->blockHeight = blockHeight;
}

//
// Transactions Lookup & Insert
//
extern BREthereumTransaction
lightNodeLookupTransaction(BREthereumLightNode node,
                           BREthereumTransactionId tid) {
    BREthereumTransaction transaction = NULL;

    pthread_mutex_lock(&node->lock);
    transaction = (0 <= tid && tid < array_count(node->transactions)
                   ? node->transactions[tid]
                   : NULL);
    pthread_mutex_unlock(&node->lock);
    return transaction;
}

extern BREthereumTransactionId
lightNodeLookupTransactionId(BREthereumLightNode node,
                           BREthereumTransaction transaction) {
    BREthereumTransactionId tid = -1;

    pthread_mutex_lock(&node->lock);
    for (int i = 0; i < array_count(node->transactions); i++)
        if (transaction == node->transactions[i]) {
            tid = i;
            break;
        }
    pthread_mutex_unlock(&node->lock);
    return tid;
}

extern BREthereumTransactionId
lightNodeInsertTransaction (BREthereumLightNode node,
                            BREthereumTransaction transaction) {
    BREthereumTransactionId tid;

    pthread_mutex_lock(&node->lock);
    array_add (node->transactions, transaction);
    tid = (BREthereumTransactionId) (array_count(node->transactions) - 1);
    pthread_mutex_unlock(&node->lock);

    return tid;
}

static void
lightNodeDeleteTransaction (BREthereumLightNode node,
                             BREthereumTransaction transaction) {
    BREthereumTransactionId tid = lightNodeLookupTransactionId(node, transaction);

    // Remove from any (and all - should be but one) wallet
    for (int wid = 0; wid < array_count(node->wallets); wid++)
        if (walletHasTransaction(node->wallets[wid], transaction)) {
            walletUnhandleTransaction(node->wallets[wid], transaction);
            lightNodeListenerAnnounceTransactionEvent(node, wid, tid, TRANSACTION_EVENT_REMOVED, SUCCESS, NULL);
        }

    // Null the node's `tid` - MUST NOT array_rm() as all `tid` holders will be dead.
    node->transactions[tid] = NULL;
}

//
// Updates
//
#if defined(SUPPORT_JSON_RPC)

extern void
lightNodeUpdateBlockNumber (BREthereumLightNode node) {
    if (LIGHT_NODE_CONNECTED != node->state) return;
    switch (node->type) {
        case NODE_TYPE_LES:
            // TODO: Fall-through on error, perhaps

        case NODE_TYPE_JSON_RPC:
            node->client.funcGetBlockNumber
                    (node->client.funcContext,
                    node,
                    ++node->requestId);
            break;

        case NODE_TYPE_NONE:
            break;
    }
}

extern void
lightNodeUpdateNonce (BREthereumLightNode node) {
    if (LIGHT_NODE_CONNECTED != node->state) return;
    switch (node->type) {
        case NODE_TYPE_LES:
            // TODO: Fall-through on error, perhaps

        case NODE_TYPE_JSON_RPC: {
            char *address = addressAsString(accountGetPrimaryAddress(node->account));

            node->client.funcGetNonce
            (node->client.funcContext,
             node,
             address,
             ++node->requestId);

            free (address);
            break;
        }
        case NODE_TYPE_NONE:
            break;
    }
}

/**
 *
 * @param node
 */
extern void
lightNodeUpdateTransactions (BREthereumLightNode node) {
    if (LIGHT_NODE_CONNECTED != node->state) {
        // Nothing to announce
        return;
    }
    switch (node->type) {
        case NODE_TYPE_LES:
            // TODO: Fall-through on error, perhaps

        case NODE_TYPE_JSON_RPC: {
            char *address = addressAsString(accountGetPrimaryAddress(node->account));
            
            node->client.funcGetTransactions
            (node->client.funcContext,
             node,
             address,
             ++node->requestId);
            
            free (address);
            break;
        }

        case NODE_TYPE_NONE:
            break;
    }
}

//
// Logs
//

/**
 *
 * @param node
 */
static const char *
lightNodeGetWalletContractAddress (BREthereumLightNode node, BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    if (NULL == wallet) return NULL;

    BREthereumToken token = walletGetToken(wallet);
    return (NULL == token ? NULL : tokenGetAddress(token));
}

extern void
lightNodeUpdateLogs (BREthereumLightNode node,
                     BREthereumWalletId wid,
                     BREthereumContractEvent event) {
    if (LIGHT_NODE_CONNECTED != node->state) {
        // Nothing to announce
        return;
    }
    switch (node->type) {
        case NODE_TYPE_LES:
            // TODO: Fall-through on error, perhaps

        case NODE_TYPE_JSON_RPC: {
            char *address = addressAsString(accountGetPrimaryAddress(node->account));
            char *encodedAddress =
                    eventERC20TransferEncodeAddress (event, address);
            const char *contract =lightNodeGetWalletContractAddress(node, wid);

            node->client.funcGetLogs
            (node->client.funcContext,
             node,
             contract,
             encodedAddress,
             eventGetSelector(event),
             ++node->requestId);

            free (encodedAddress);
            free (address);
            break;
        }

        case NODE_TYPE_NONE:
            break;
    }
}

extern void
lightNodeUpdateWalletBalance(BREthereumLightNode node,
                             BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);

    if (NULL == wallet) {
        lightNodeListenerAnnounceWalletEvent(node, wid, WALLET_EVENT_BALANCE_UPDATED,
                                             ERROR_UNKNOWN_WALLET,
                                             NULL);

    } else if (LIGHT_NODE_CONNECTED != node->state) {
        lightNodeListenerAnnounceWalletEvent(node, wid, WALLET_EVENT_BALANCE_UPDATED,
                                             ERROR_NODE_NOT_CONNECTED,
                                             NULL);
    } else {
        switch (node->type) {
            case NODE_TYPE_LES:
            case NODE_TYPE_JSON_RPC: {
                char *address = addressAsString(walletGetAddress(wallet));

                node->client.funcGetBalance
                        (node->client.funcContext,
                         node,
                         wid,
                         address,
                         ++node->requestId);

                free(address);
                break;
            }

            case NODE_TYPE_NONE:
                break;
        }
    }
}

extern void
lightNodeUpdateTransactionGasEstimate (BREthereumLightNode node,
                                       BREthereumWalletId wid,
                                       BREthereumTransactionId tid) {
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, tid);

    if (NULL == transaction) {
        lightNodeListenerAnnounceTransactionEvent(node, wid, tid,
                                                  TRANSACTION_EVENT_GAS_ESTIMATE_UPDATED,
                                                  ERROR_UNKNOWN_WALLET,
                                                  NULL);

    } else if (LIGHT_NODE_CONNECTED != node->state) {
        lightNodeListenerAnnounceTransactionEvent(node, wid, tid,
                                                  TRANSACTION_EVENT_GAS_ESTIMATE_UPDATED,
                                                  ERROR_NODE_NOT_CONNECTED,
                                                  NULL);
    } else {
        switch (node->type) {
            case NODE_TYPE_LES:
            case NODE_TYPE_JSON_RPC: {
                // This will be ZERO if transaction amount is in TOKEN.
                BREthereumEther amountInEther = transactionGetEffectiveAmountInEther(transaction);
                char *to = (char *) addressAsString(transactionGetTargetAddress(transaction));
                char *amount = coerceString(amountInEther.valueInWEI, 16);
                char *data = (char *) transactionGetData(transaction);

                node->client.funcEstimateGas
                        (node->client.funcContext,
                         node,
                         wid,
                         tid,
                         to,
                         amount,
                         data,
                         ++node->requestId);

                free(to);
                free(amount);

                if (NULL != data && '\0' != data[0])
                    free(data);

                break;
            }
                assert (0);

            case NODE_TYPE_NONE:
                break;
        }
    }
}

extern void
lightNodeUpdateWalletDefaultGasPrice (BREthereumLightNode node,
                                      BREthereumWalletId wid) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);

    if (NULL == wallet) {
        lightNodeListenerAnnounceWalletEvent(node, wid, WALLET_EVENT_DEFAULT_GAS_PRICE_UPDATED,
                                             ERROR_UNKNOWN_WALLET,
                                             NULL);

    } else if (LIGHT_NODE_CONNECTED != node->state) {
        lightNodeListenerAnnounceWalletEvent(node, wid, WALLET_EVENT_DEFAULT_GAS_PRICE_UPDATED,
                                             ERROR_NODE_NOT_CONNECTED,
                                             NULL);
    } else {
        switch (node->type) {
            case NODE_TYPE_LES:
            case NODE_TYPE_JSON_RPC: {
                node->client.funcGetGasPrice
                        (node->client.funcContext,
                         node,
                         wid,
                         ++node->requestId);
                break;
            }

            case NODE_TYPE_NONE:
                break;
        }
    }
}

extern void
lightNodeFillTransactionRawData(BREthereumLightNode node,
                                BREthereumWalletId wid,
                                BREthereumTransactionId transactionId,
                                uint8_t **bytesPtr, size_t *bytesCountPtr) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, transactionId);
    
    assert (NULL != bytesCountPtr && NULL != bytesPtr);
    assert (ETHEREUM_BOOLEAN_IS_TRUE (transactionIsSigned(transaction)));
    
    BRRlpData rawTransactionData =
    walletGetRawTransaction(wallet, transaction);
    
    *bytesCountPtr = rawTransactionData.bytesCount;
    *bytesPtr = (uint8_t *) malloc (*bytesCountPtr);
    memcpy (*bytesPtr, rawTransactionData.bytes, *bytesCountPtr);
}

extern const char *
lightNodeGetTransactionRawDataHexEncoded(BREthereumLightNode node,
                                         BREthereumWalletId wid,
                                         BREthereumTransactionId transactionId,
                                         const char *prefix) {
    BREthereumWallet wallet = lightNodeLookupWallet(node, wid);
    BREthereumTransaction transaction = lightNodeLookupTransaction(node, transactionId);
    
    return walletGetRawTransactionHexEncoded(wallet, transaction, prefix);
}

#if 0
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

        block = createBlock(blockHash, blockNumber, blockTimestamp);
        lightNodeInsertBlock(node, block);

        BREthereumTransactionId bid = lightNodeLookupBlockId(node, block);
        lightNodeListenerAnnounceBlockEvent(node, bid, BLOCK_EVENT_CREATED, SUCCESS, NULL);
    }
    else {
        // We already have this block.
        // TODO: Assert on {number, timestamp}?
    }

    free (blockHash);
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

    // If `isSource` then the nonce is 'ours'.
    if (ETHEREUM_BOOLEAN_IS_TRUE(isSource) && nonce >= addressGetNonce(primaryAddress))
        addressSetNonce(primaryAddress, nonce + 1);  // next

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

    // See if the block confirmations have changed.
    uint64_t blockConfirmations = strtoull(strBlockConfirmations, NULL, 0);
    // There is an implied update to the node's block height
    lightNodeUpdateBlockHeight(node, blockGetNumber(block) + blockConfirmations);

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
    const char *strBlockHash = strBlockNumber;  // TODO: actual hash argument
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

    // Try hard to figure out the confirmations.
    lightNodeUpdateBlockHeight(node, blockGetNumber(block));

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
        BREthereumHash hash = hashCreate(strHash);
        walletTransactionSubmitted(wallet, transaction, hash);
        free(hash);
    }
    pthread_mutex_unlock(&node->lock);

    lightNodeListenerAnnounceTransactionEvent(node, wid, tid, TRANSACTION_EVENT_SUBMITTED,
                                              eventStatus,
                                              eventErrorDescription);
}
#endif

#endif // ETHEREUM_LIGHT_NODE_USE_JSON_RPC

