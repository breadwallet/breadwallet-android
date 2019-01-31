//
//  BREthereumNodeEventHandler.h
//  breadwallet-core Ethereum
//
//  Created by Lamont Samuels on 5/4/18.
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
#ifndef BR_Ethereum_Node_Event_Handler_h
#define BR_Ethereum_Node_Event_Handler_h

#include <inttypes.h>
#include "BREthereumNode.h"


#ifdef __cplusplus
extern "C" {
#endif

typedef struct BREthereumNodeEventHandlerContext* BREthereumNodeEventHandler;

//
// The type of events that can be enqueued
//
typedef enum {
    BRE_NODE_EVENT_DISCONNECT = 0,
    BRE_NODE_EVENT_FREE,
    BRE_NODE_EVENT_SUBMIT_TRANSACTION
}BREthereumNodeEventType;

typedef struct {
    BREthereumNodeEventType type;
    union {
        //Submit transaction data
        struct {
            uint8_t* transaction;
            size_t size;
        }submit_transaction;
    } u;
} BREthereumNodeEvent;

//
// Ethereum Node Event Handler management functions
//
/**
 * Creates a new event handler
 * @post: Must be released by a calling ethereumNodeManagerRelease(manager)
*/
extern BREthereumNodeEventHandler ethereumNodeEventHandlerCreate(void);

/**
 * Frees the memory assoicated with the given event handler.
 * @param handler - the even handler to release
 */
extern void ethereumNodeEventHandlerRelease(BREthereumNodeEventHandler handler);

/**
 * Enqueues an event into the event handler queue
 * @param handler - the event handler to release
 * @param event - the event to enqueue
 */
extern void ethereumNodeEventHandlerEnqueue(BREthereumNodeEventHandler handler, BREthereumNodeEvent event);
 
 /**
  * Dequeues an event from the even handler queue
  * @param handler - the event handler to release
  * @return the event removed from the queue
  */
extern void ethereumNodeEventHandlerDequeue(BREthereumNodeEventHandler handler, BREthereumNodeEvent* event);

/**
 * Determines whether an event is in the even queue
 * @param handler - the event handler to release
 * @param type - the event to check for inside the queue
 * @return ETHEREUM_BOOLEAN_TRUE, if the event is inside the queue. Otherwise, ETHEREUM_BOOLEAN_FALSE.
 */
extern BREthereumBoolean ethereumNodeEventHandlerHasEvent(BREthereumNodeEventHandler handler, BREthereumNodeEventType type);
 
/**
 * Determines the number of events inside the event queue 
 * @param handler - the event handler
 * @return the number of events in the queue
 */
extern size_t ethereumNodeEventHandlerSize(BREthereumNodeEventHandler handler);

#ifdef __cplusplus
}
#endif

#endif /* BR_Ethereum_Node_Event_Handler_h */
