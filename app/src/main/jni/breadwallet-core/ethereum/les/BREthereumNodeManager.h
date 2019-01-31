//
//  BREthereumNodeManager.h
//  breadwallet-core Ethereum
//
//  Created by Lamont Samuels on 4/19/18.
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

#ifndef BR_Ethereum_NodeManager_h
#define BR_Ethereum_NodeManager_h

#include "BREthereumNode.h"
#include "BREthereumNetwork.h"
#include "BREthereumTransaction.h"
#include "BREthereumBlock.h"
#include "BREthereumLES.h"
#include <inttypes.h>


#ifdef __cplusplus
extern "C" {
#endif

typedef struct BREthereumNodeMangerContext* BREthereumNodeManager;

/**
 * Ehtereum Manager Node Connection Status - Connection states for Manager
 */
typedef enum {
    BRE_MANAGER_ERROR = -1,
    BRE_MANAGER_DISCONNECTED,  //Manager is not connected to any remote node(s)
    BRE_MANAGER_CONNECTING,    //Manager is trying to connect to remote node(s)
    BRE_MANAGER_CONNECTED      //Manager is connected to a remote node(s) 
} BREthereumNodeManagerStatus;


/**
 *  Current status of the sent/queried transactions
 */
typedef enum {
    BRE_LES_TRANSACTION_STATUS_Unknown  = 0,  // (0): transaction is unknown
    BRE_LES_TRANSACTION_STATUS_Queued   = 1,  // (1): transaction is queued (not processable yet)
    BRE_LES_TRANSACTION_STATUS_Pending  = 2,  // (2): transaction is pending (processable)
    BRE_LES_TRANSACTION_STATUS_Included = 3,  // (3): transaction is already included in the canonical chain. data contains an RLP-encoded [blockHash: B_32, blockNumber: P, txIndex: P] structure
    BRE_LES_TRANSACTION_STATUS_Error    = 4   // (4): transaction sending failed. data contains a text error message
} BREthereumLESTransactionStatus;

/**
 *
 */
typedef struct {
    UInt128 address;    //ipv6 address of the peer
    uint16_t port;      //port number of peer connection
    uint64_t timestamp; // timestamp reported by peer
    uint8_t flags;      //scratch variable
}BREthereumPeerInfo;


//
// Ethereum Node Manager management functions
//

// Callback definitions for the Manager
typedef void* BREthereumNodeMangerInfo;
typedef void (*BRNodeManagerTransactionStatus)(BREthereumNodeMangerInfo info,
                                               BREthereumTransaction transaction,
                                               BREthereumLESTransactionStatus status);
    
typedef void (*BRNodeManagerBlocks)(BREthereumNodeMangerInfo info,
                                    BREthereumBlock blocks[],
                                    size_t blocksCount);
    
typedef void (*BRNodeManagerPeers)(BREthereumNodeMangerInfo info,
                                   BREthereumPeerInfo peers[],
                                   size_t peersCount);
    

/**
 * Creates a new Ethereum Node manager.
 * @post: Must be released by a calling ethereumNodeManagerRelease(manager)
 * @param network - the Ethereum network to connect to remote peers
 * @param account - The account of interest
 * @param blocks - known blocks that are of interest to the account
 * @param blocksCount - the number of blocks in the blcoks argument.
 * @param peers - known peers that are reliable and should try to connect to first
 * @param peersCount - the number of peers in the peers argument
 */
extern BREthereumNodeManager ethereumNodeManagerCreate(BREthereumNetwork network,
                                                       BREthereumAccount account,
                                                       BREthereumBlock block,
                                                       size_t blockCount, 
                                                       BREthereumPeerInfo peers[],
                                                       size_t peersCount);

/**
 * Sets the callbacks for the ethereum node manager
 * @pre: Set callbacks once before calling ethereumNodeMangerConnect()
 * @param info - a BREthereumNodeMangerInfo (i.e., a void pointer) that will be passed along with each callback call
 * @param funcTransStatus - called when transaction status may have changed such as when a new block arrives
 * @param funcNewBlocks - called when blocks that are of interest to the account
 * @param funcNewPeers - called when peers should be saved to the persistent store, for
 */
extern void ethereumNodeManagerSetCallbacks(BREthereumNodeManager manager,
                                            BREthereumNodeMangerInfo info,
                                            BRNodeManagerTransactionStatus funcTransStatus,
                                            BRNodeManagerBlocks funcNewBlocks,
                                            BRNodeManagerPeers funcNewPeers);
    
/**
 * Frees the memory assoicated with the given node manager.
 * @param manager - the node manager to release
 */
extern void ethereumNodeManagerRelease(BREthereumNodeManager manager);

/**
 * Determines whether one of the nodes is connected to a remote node
 * @param manager - the node manager
 * @return  the status of the node manager
 */
extern BREthereumNodeManagerStatus ethereumNodeManagerStatus(BREthereumNodeManager manager);
 
 /**
  * Connects to the ethereum peer-to-peer network.
  * @param manager - the node manager context
  */
extern void ethereumNodeMangerConnect(BREthereumNodeManager manager);

/**
 * Disconnects from the ethereum peer-to-peer network.
 * @param manager - the node manager context
 */
extern void ethereumNodeManagerDisconnect(BREthereumNodeManager manager);
 
/**
 * Determines the number of remote peers that are successfully connected to the manager
 * @param manager - the node manager context
 * @return the number of connected remote peers
 */
extern size_t ethereumNodeMangerPeerCount(BREthereumNodeManager manager);


//
// Ethereum Node Manager LES types and functions
//

//Callback definitions for the LES functions
typedef void (*BRLESGetTransactions)(BREthereumTransaction* transactions,
                                     size_t transactionsSize,
                                     unsigned int requestId);
    
    
/**
 * Requets a remote peer to submit a transaction into its transaction pool and relay them to the ETH network.
 * @param transaction - the tranaction to submit
 * @param requestId - a unique id for this transaction submission.
 * @return ETHEREUM_BOOLEAN_TRUE, if the transaction was successfully submited to the peer. Otherwise, ETHEREUM_BOOLEAN_FALSE is returned on error.
 */
extern BREthereumBoolean ethereumNodeManagerSubmitTransaction(BREthereumNodeManager manager,
                                                              const BREthereumTransaction transaction,
                                                              const int requestId);


/**
 * Requets a remote peer to retrieve transactions from the remote-peer
 * @param a unique id for this transaction request.
 * @param callback - the callback function which will be caleld once the transactions are received from the network
 */
extern void ethereumNodeManagerGetTransaction(BREthereumNodeManager manager,
                                              const int requestId,
                                              BRLESGetTransactions callback);


#ifdef __cplusplus
}
#endif

#endif /* BR_Ethereum_NodeManager_h */
