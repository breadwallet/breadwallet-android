//
//  BREthereumLightNodeListener.c
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

#include "BRArray.h"
#include "BREthereumLightNodePrivate.h"

//
// Wallet Event
//
typedef struct {
    BREvent base;
    BREthereumLightNode node;
    BREthereumWalletId wid;
    BREthereumWalletEvent event;
    BREthereumStatus status;
    const char *errorDescription;
} BREthereumListenerWalletEvent;

#define LISTENER_WALLET_EVENT_INITIALIZER(node, wid, vent, status, desc)  \
{ { NULL, &listenerWalletEventType }, (node), (wid), (event), (status), (desc) }

static void
lightNodeListenerWalletEventDispatcher(BREventHandler ignore,
                                      BREthereumListenerWalletEvent *event) {
    BREthereumLightNode node = event->node;
    
    int count = (int) array_count(node->listeners);
    for (int i = 0; i < count; i++) {
        if (NULL != node->listeners[i].walletEventHandler)
            node->listeners[i].walletEventHandler
            (node->listeners[i].context,
             node,
             event->wid,
             event->event,
             event->status,
             event->errorDescription);
    }
}

BREventType listenerWalletEventType = {
    "Listener Wallet Event",
    sizeof (BREthereumListenerWalletEvent),
    (BREventDispatcher) lightNodeListenerWalletEventDispatcher
};

extern void
lightNodeListenerAnnounceWalletEvent(BREthereumLightNode node,
                                     BREthereumWalletId wid,
                                     BREthereumWalletEvent event,
                                     BREthereumStatus status,
                                     const char *errorDescription) {
    BREthereumListenerWalletEvent message =
    LISTENER_WALLET_EVENT_INITIALIZER (node, wid, event, status, errorDescription);
    eventHandlerSignalEvent(node->handlerForListener, (BREvent*) &message);
}

//
// Block Event
//
typedef struct {
    BREvent base;
    BREthereumLightNode node;
    BREthereumBlockId bid;
    BREthereumBlockEvent event;
    BREthereumStatus status;
    const char *errorDescription;
} BREthereumListenerBlockEvent;

#define LISTENER_BLOCK_EVENT_INITIALIZER(node, bid, event, status, desc)  \
    { { NULL, &listenerBlockEventType }, (node), (bid), (event), (status), (desc) }

static void
lightNodeListenerBlockEventDispatcher(BREventHandler ignore,
                                      BREthereumListenerBlockEvent *event) {
    BREthereumLightNode node = event->node;

    int count = (int) array_count(node->listeners);
    for (int i = 0; i < count; i++) {
        if (NULL != node->listeners[i].blockEventHandler)
            node->listeners[i].blockEventHandler
            (node->listeners[i].context,
             node,
             event->bid,
             event->event,
             event->status,
             event->errorDescription);
    }
}

BREventType listenerBlockEventType = {
    "Listener Block Event",
    sizeof (BREthereumListenerBlockEvent),
    (BREventDispatcher) lightNodeListenerBlockEventDispatcher
};

extern void
lightNodeListenerAnnounceBlockEvent(BREthereumLightNode node,
                                    BREthereumBlockId bid,
                                    BREthereumBlockEvent event,
                                    BREthereumStatus status,
                                    const char *errorDescription) {
    BREthereumListenerBlockEvent message =
    LISTENER_BLOCK_EVENT_INITIALIZER (node, bid, event, status, errorDescription);
    eventHandlerSignalEvent(node->handlerForListener, (BREvent*) &message);
}

//
// Transaction Event
//
typedef struct {
    struct BREventRecord base;
    BREthereumLightNode node;
    BREthereumWalletId wid;
    BREthereumTransactionId tid;
    BREthereumTransactionEvent event;
    BREthereumStatus status;
    const char *errorDescription;
} BREthereumListenerTransactionEvent;

#define LISTENER_TRANSACTION_EVENT_INITIALIZER(node, wid, tid, event, status, desc)  \
    { { NULL, &listenerTransactionEventType }, (node), (wid), (tid), (event), (status), (desc) }

static void
lightNodeListenerTransactionEventDispatcher(BREventHandler ignore,
                                            BREthereumListenerTransactionEvent *event) {
    BREthereumLightNode node = event->node;

    int count = (int) array_count(node->listeners);
    for (int i = 0; i < count; i++) {
        if (NULL != node->listeners[i].transactionEventHandler)
            node->listeners[i].transactionEventHandler
            (node->listeners[i].context,
             node,
             event->wid,
             event->tid,
             event->event,
             event->status,
             event->errorDescription);
    }
}

BREventType listenerTransactionEventType = {
    "Listener Transaction Event",
    sizeof (BREthereumListenerTransactionEvent),
    (BREventDispatcher) lightNodeListenerTransactionEventDispatcher
};

extern void
lightNodeListenerAnnounceTransactionEvent(BREthereumLightNode node,
                                          BREthereumWalletId wid,
                                          BREthereumTransactionId tid,
                                          BREthereumTransactionEvent event,
                                          BREthereumStatus status,
                                          const char *errorDescription) {
    BREthereumListenerTransactionEvent message =
    LISTENER_TRANSACTION_EVENT_INITIALIZER (node, wid, tid, event, status, errorDescription);
    eventHandlerSignalEvent(node->handlerForListener, (BREvent*) &message);
}

const BREventType *listenerEventTypes[] = {
    &listenerWalletEventType,
    &listenerBlockEventType,
    &listenerTransactionEventType
};
const unsigned int listenerEventTypesCount = 3;
