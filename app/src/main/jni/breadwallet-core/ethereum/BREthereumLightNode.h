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

#ifndef BR_Ethereum_Light_Node_H
#define BR_Ethereum_Light_Node_H

#include <stdint.h>
#include "BREthereum.h"
#include "BREthereumAccount.h"
#include "BREthereumTransaction.h"
#include "BREthereumBlock.h"
#include "BREthereumWallet.h"

#ifdef __cplusplus
extern "C" {
#endif

//
// Two types of LightNode - JSON_RPC or LES (Light Ethereum Subprotocol).  For a LES LightNode
// some of the Client callbacks will only be used as a fallback.
//
typedef enum {
    NODE_TYPE_NONE,
    NODE_TYPE_JSON_RPC,
    NODE_TYPE_LES
} BREthereumType;

#define FIXED_LIGHT_NODE_TYPE   (NODE_TYPE_LES)

extern BREthereumLightNode
createLightNode (BREthereumNetwork network,
                 BREthereumAccount account);

extern BREthereumAccount
lightNodeGetAccount (BREthereumLightNode node);

extern BREthereumNetwork
lightNodeGetNetwork (BREthereumLightNode node);

//
// Connect & Disconnect
//
extern BREthereumBoolean
lightNodeConnect(BREthereumLightNode node,
                 BREthereumClient client);

extern BREthereumBoolean
lightNodeDisconnect (BREthereumLightNode node);

//
// {Wallet, Block, Transaction} Lookup
//
extern BREthereumWallet
lightNodeLookupWallet(BREthereumLightNode node,
                      BREthereumWalletId wid);

extern BREthereumBlock
lightNodeLookupBlock(BREthereumLightNode node,
                     BREthereumBlockId bid);

extern BREthereumBlock
lightNodeLookupBlockByHash(BREthereumLightNode node,
                           const BREthereumHash hash);

extern BREthereumTransaction
lightNodeLookupTransaction(BREthereumLightNode node,
                           BREthereumTransactionId tid);

//
// Wallet
//
extern BREthereumWalletId
lightNodeGetWallet(BREthereumLightNode node);

extern BREthereumWalletId
lightNodeGetWalletHoldingToken(BREthereumLightNode node,
                              BREthereumToken token);

extern BREthereumTransactionId
lightNodeWalletCreateTransaction(BREthereumLightNode node,
                                BREthereumWallet wallet,
                                const char *recvAddress,
                                BREthereumAmount amount);

extern void // status, error
lightNodeWalletSignTransaction(BREthereumLightNode node,
                               BREthereumWallet wallet,
                               BREthereumTransaction transaction,
                               BRKey privateKey);

extern void // status, error
lightNodeWalletSignTransactionWithPaperKey(BREthereumLightNode node,
                               BREthereumWallet wallet,
                               BREthereumTransaction transaction,
                               const char *paperKey);

extern void // status, error
lightNodeWalletSubmitTransaction(BREthereumLightNode node,
                                 BREthereumWallet wallet,
                                 BREthereumTransaction transaction);

extern BREthereumTransactionId *
lightNodeWalletGetTransactions(BREthereumLightNode node,
                                BREthereumWallet wallet);

extern int
lightNodeWalletGetTransactionCount(BREthereumLightNode node,
                                   BREthereumWallet wallet);

extern void
lightNodeWalletSetDefaultGasLimit(BREthereumLightNode node,
                                 BREthereumWallet wallet,
                                 BREthereumGas gasLimit);

extern void
lightNodeWalletSetDefaultGasPrice(BREthereumLightNode node,
                                  BREthereumWallet wallet,
                                  BREthereumGasPrice gasPrice);

//
// Block
//
extern uint64_t
lightNodeGetBlockHeight(BREthereumLightNode node);

extern void
lightNodeUpdateBlockHeight(BREthereumLightNode node,
                           uint64_t blockHeight);

#ifdef __cplusplus
}
#endif

#endif //BR_Ethereum_Light_Node_H
