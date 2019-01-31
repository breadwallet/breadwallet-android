//
//  BREthereumNodeManager.c
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

#include <pthread.h>
#include <errno.h>
#include "BREthereumNodeManager.h"
#include "BREthereumNode.h"
#include "BRArray.h"
#include "BREthereumNetwork.h"

typedef struct {

    //The array of ethereum node
    BREthereumNode* nodes;
    
    //The array of pointers that point to the nodes that
    // are connected in the "nodes" field
    BREthereumNode** connectedNodes;

   // A lock for the manager context
   pthread_mutex_t lock;
   
}BREthereumNodeManagerContext;

#define ETHEREUMN_PEER_MAX_CONNECTIONS 5
#define BOOTSTRAP_NODE_IDX 0

const BREthereumPeer _bootstrap_peer = {UINT128_ZERO, 30303, "eth-mainnet.breadwallet.com", -1, UINT512_ZERO};

BREthereumNodeManager ethereumNodeManagerCreate(BREthereumNetwork network,
                                                BREthereumAccount account,
                                                BREthereumBlock block,
                                                size_t blockCount,
                                                BREthereumPeerInfo peers[],
                                                size_t peersCount) {

    BREthereumNodeManagerContext* manager= (BREthereumNodeManagerContext*) calloc(1, sizeof (BREthereumNodeManagerContext));
    return (BREthereumNodeManager)manager;
    /*
    if(manager != NULL) {
        array_new(manager->nodes, ETHEREUMN_PEER_MAX_CONNECTIONS);
        array_new(manager->connectedNodes, ETHEREUMN_PEER_MAX_CONNECTIONS);
        array_add(manager->connectedNodes, NULL);
        pthread_mutex_init(&manager->lock, NULL);
        return (BREthereumNodeManager)manager;
    }
    return NULL;
    */
}
void ethereumNodeMangerRelease(BREthereumNodeManager manager) {
    assert(manager != NULL);
    BREthereumNodeManagerContext* ctx = (BREthereumNodeManagerContext*) manager;
    //array_free(ctx->nodes);
    //array_free(ctx->connectedNodes);
    free(ctx);
}
BREthereumNodeManagerStatus ethereumNodeManagerStatus(BREthereumNodeManager manager){
    assert(manager != NULL);
    return BRE_MANAGER_DISCONNECTED;
    /*BREthereumNodeManagerContext* ctx = (BREthereumNodeManagerContext*) manager;
    BREthereumNodeManagerStatus retStatus = BRE_MANAGER_DISCONNECTED;
    pthread_mutex_lock(&ctx->lock);
    BREthereumNode* bootstrapNode = ctx->connectedNodes[BOOTSTRAP_NODE_IDX];
    if( bootstrapNode != NULL ) {
        BREthereumNodeStatus status = ethereumNodeStatus(*bootstrapNode);
        switch (status) {
        case BRE_NODE_ERROR:
        {
            retStatus = BRE_MANAGER_ERROR;
        }
        break;
        case BRE_NODE_CONNECTED:
        {
            retStatus = BRE_MANAGER_CONNECTED;
        }
        break;
        case BRE_NODE_PERFORMING_HANDSHAKE:
        case BRE_NODE_CONNECTING:
        {
            retStatus = BRE_MANAGER_CONNECTING;
        }
        break;
        case BRE_NODE_DISCONNECTED:
        default:
        {
            retStatus = BRE_MANAGER_DISCONNECTED;
        }
        break;
        }
    }
    pthread_mutex_unlock(&ctx->lock);
    return retStatus;
    */
}
void ethereumNodeMangerConnect(BREthereumNodeManager manager) {
    assert(manager != NULL);
    /* BREthereumNodeManagerContext* ctx = (BREthereumNodeManagerContext*) manager;
    pthread_mutex_lock(&ctx->lock);
    BREthereumNode* bootstrapNodePtr = ctx->connectedNodes[BOOTSTRAP_NODE_IDX];
    if(bootstrapNodePtr == NULL) {
        BREthereumNode bootstrapNode = ethereumNodeCreate(_bootstrap_peer, ETHEREUM_BOOLEAN_TRUE);
        if(bootstrapNode != NULL){
            array_insert(ctx->nodes, BOOTSTRAP_NODE_IDX, bootstrapNode);
            array_insert(ctx->connectedNodes, BOOTSTRAP_NODE_IDX, ctx->nodes);
            ethereumNodeConnect(bootstrapNode);
        }
    }
    pthread_mutex_unlock(&ctx->lock);
    */
}
void ethereumNodeMangerDisconnect(BREthereumNodeManager manager) {

    assert(manager != NULL);
    /*
    BREthereumNodeManagerContext* ctx = (BREthereumNodeManagerContext*) manager;
    pthread_mutex_lock(&ctx->lock);
    BREthereumNode* bootstrapNodePtr = ctx->connectedNodes[BOOTSTRAP_NODE_IDX];
    if(bootstrapNodePtr != NULL && ethereumNodeStatus(*bootstrapNodePtr) != BRE_NODE_DISCONNECTED){
        ethereumNodeDisconnect(*bootstrapNodePtr);
    }
    pthread_mutex_unlock(&ctx->lock);
    */
}
size_t ethereumNodeMangerPeerCount(BREthereumNodeManager manager) {

    return 0;
    /*
    BREthereumNodeManagerContext* ctx = (BREthereumNodeManagerContext*) manager;
    size_t count = 0;
    
    pthread_mutex_lock(&ctx->lock);
    count = array_count(ctx->connectedNodes);
    pthread_mutex_unlock(&ctx->lock);
    
    return count;
    */
}
BREthereumBoolean ethereumNodeManagerSubmitTransaction(BREthereumNodeManager manager,
                                                       const BREthereumTransaction transaction,
                                                       const int requestId) {
    
    return ETHEREUM_BOOLEAN_FALSE;
}

void ethereumNodeManagerSetCallbacks(BREthereumNodeManager manager,
                                     BREthereumNodeMangerInfo info,
                                     BRNodeManagerTransactionStatus funcTransStatus,
                                     BRNodeManagerBlocks funcNewBlocks,
                                     BRNodeManagerPeers funcNewPeers) {
    
    //TODO: Implement function
}

void ethereumNodeManagerGetTransaction(BREthereumNodeManager manager,
                                       const int requestId,
                                       BRLESGetTransactions callback) {
    //TODO: Implement function 
}
