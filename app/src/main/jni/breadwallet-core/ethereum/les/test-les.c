//
//  test-les.h
//  breadwallet-core Ethereum
//
//  Created by Lamont Samuels on 4/16/18.
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

#include <stdio.h>
#include <stdlib.h>
#include <string.h>
#include <inttypes.h>
#include <errno.h>
#include <time.h>
#include <unistd.h>
#include <arpa/inet.h>
#include <assert.h>
#include <regex.h>
#include "test-les.h"
#include "BRInt.h"
#include "BREthereumNode.h"
#include "BREthereumNodeEventHandler.h"
#include "BREthereumNodeManager.h"
#include "BREthereumNetwork.h"
// LES Tests



void runEthereumNodeEventHandlerTests() {

    BREthereumNodeEvent event, out;
    BREthereumNodeEventHandler handler = ethereumNodeEventHandlerCreate();
    assert(handler != NULL);
    assert(ethereumNodeEventHandlerSize(handler) == 0);
    ethereumNodeEventHandlerDequeue(handler, &out);
    assert(ethereumNodeEventHandlerSize(handler) == 0);
    assert(ETHEREUM_BOOLEAN_IS_FALSE(ethereumNodeEventHandlerHasEvent(handler,BRE_NODE_EVENT_SUBMIT_TRANSACTION)));

    
    event.type = BRE_NODE_EVENT_FREE;
    ethereumNodeEventHandlerEnqueue(handler, event);
    assert(ethereumNodeEventHandlerSize(handler) == 1);
    event.type = BRE_NODE_EVENT_DISCONNECT;
    ethereumNodeEventHandlerEnqueue(handler, event);
    assert(ethereumNodeEventHandlerSize(handler) == 2);
    event.type = BRE_NODE_EVENT_FREE;
    ethereumNodeEventHandlerEnqueue(handler, event);
    assert(ethereumNodeEventHandlerSize(handler) == 3);
    
    ethereumNodeEventHandlerDequeue(handler, &out);
    assert(ethereumNodeEventHandlerSize(handler) == 2);
    assert(out.type == BRE_NODE_EVENT_FREE);
    
    assert(ETHEREUM_BOOLEAN_IS_FALSE(ethereumNodeEventHandlerHasEvent(handler,BRE_NODE_EVENT_SUBMIT_TRANSACTION)));
    
    event.type = BRE_NODE_EVENT_SUBMIT_TRANSACTION;
    uint8_t handler_trans[] = {97,98,99,100};
    event.u.submit_transaction.transaction = handler_trans;
    event.u.submit_transaction.size = sizeof(handler_trans);
    ethereumNodeEventHandlerEnqueue(handler, event);
    assert(ethereumNodeEventHandlerSize(handler) == 3);
    uint8_t handler_trans2[] = {65,66,67};
    event.u.submit_transaction.transaction = handler_trans2;
    event.u.submit_transaction.size = sizeof(handler_trans2);
    ethereumNodeEventHandlerEnqueue(handler, event);
    assert(ethereumNodeEventHandlerSize(handler) == 4);
    
    assert(ETHEREUM_BOOLEAN_IS_TRUE(ethereumNodeEventHandlerHasEvent(handler,BRE_NODE_EVENT_SUBMIT_TRANSACTION)));

    
    size_t count = 4;
    for(int idx= 0; idx <7; ++idx){
        event.type = BRE_NODE_EVENT_FREE;
        ethereumNodeEventHandlerEnqueue(handler, event);
        assert(ethereumNodeEventHandlerSize(handler) == ++count);
    }
    
    ethereumNodeEventHandlerDequeue(handler, &out);
    assert(ethereumNodeEventHandlerSize(handler) == 10);
    assert(out.type == BRE_NODE_EVENT_DISCONNECT);
    
    ethereumNodeEventHandlerDequeue(handler, &out);
    assert(ethereumNodeEventHandlerSize(handler) == 9);
    assert(out.type == BRE_NODE_EVENT_FREE);
    
    ethereumNodeEventHandlerDequeue(handler, &out);
    assert(ethereumNodeEventHandlerSize(handler) == 8);
    assert(out.type == BRE_NODE_EVENT_SUBMIT_TRANSACTION);
    assert(out.u.submit_transaction.size == 4);
    assert(out.u.submit_transaction.transaction[0] == 97);
    assert(out.u.submit_transaction.transaction[1] == 98);
    assert(out.u.submit_transaction.transaction[2] == 99);
    assert(out.u.submit_transaction.transaction[3] == 100);
    
    ethereumNodeEventHandlerDequeue(handler, &out);
    assert(ethereumNodeEventHandlerSize(handler) == 7);
    assert(out.type == BRE_NODE_EVENT_SUBMIT_TRANSACTION);
    assert(out.u.submit_transaction.size == 3);
    assert(out.u.submit_transaction.transaction[0] == 65);
    assert(out.u.submit_transaction.transaction[1] == 66);
    assert(out.u.submit_transaction.transaction[2] == 67);

    count = 7;
    for(int idx= 0; idx <7; ++idx){
        ethereumNodeEventHandlerDequeue(handler, &out);
        assert(out.type == BRE_NODE_EVENT_FREE);
        assert(ethereumNodeEventHandlerSize(handler) == --count);
    }
    assert(ethereumNodeEventHandlerSize(handler) == 0);
    ethereumNodeEventHandlerDequeue(handler, &out);
    assert(ethereumNodeEventHandlerSize(handler) == 0);
    
    ethereumNodeEventHandlerRelease(handler); 
}

void runEthereumManagerTests() {

    //TODO: Write tests for EthereumManagerTests;
    //BREthereumNodeManager manager = ethereumNodeManagerCreate(ethereumMainnet);
    //assert(manager != NULL);
    //ethereumNodeMangerConnect(manager);
    //Wait for up to 24 seconds, to give the node manager time to connect to a remote node.
    sleep(24);
    
    

}

void runEthereumNodeTests() {
    
    UInt128 address = UINT128_ZERO;
    uint16_t port = 0;
    struct in_addr addr;
    
     if (inet_pton(AF_INET, "127.0.0.1", &addr) != 1) {
        fprintf(stderr, "***FAILED*** %s: Could not convert address tro AF_INET\n", __func__);
        assert(0);
     }
    address.u16[5] = 0xffff;
    address.u32[3] = addr.s_addr;
    
    BREthereumPeer remotePeer1 = {address,port};
    BREthereumPeer remotePeer2 = {address,port};
    BREthereumNode node = ethereumNodeCreate(remotePeer1, ETHEREUM_BOOLEAN_TRUE);
    BREthereumNode node2 = ethereumNodeCreate(remotePeer2, ETHEREUM_BOOLEAN_FALSE);
    
    ethereumNodeConnect(node);
    ethereumNodeConnect(node2);

    //Wait for up to 24 seconds, to give the nodes time to start and find each other to perform the handshake;
    sleep(24);

    assert(ethereumNodeStatus(node) == BRE_NODE_CONNECTED);
    assert(ethereumNodeStatus(node2) == BRE_NODE_CONNECTED);

}

void runLEStests(void) {

   // runEthereumNodeTests();
    runEthereumNodeEventHandlerTests();

}

