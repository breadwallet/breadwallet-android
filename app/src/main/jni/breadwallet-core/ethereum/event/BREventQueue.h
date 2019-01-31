//
//  BREventQueue.h
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
//

#ifndef BR_Event_Queue_H
#define BR_Event_Queue_H

#include "BREvent.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct BREventQueueRecord *BREventQueue;

/**
 * Create an Event Queue with `size` as the maximum event size and with the
 * optional `lock`.
 */
extern BREventQueue
eventQueueCreate (size_t size, pthread_mutex_t *lock);

extern void
eventQueueDestroy (BREventQueue queue);

extern void
eventQueueEnqueue (BREventQueue queue,
                   const BREvent *event);

extern BREventStatus
eventQueueDequeue (BREventQueue queue,
                   BREvent *event);

extern int
eventQueueHasPending (BREventQueue queue);

#ifdef __cplusplus
}
#endif

#endif /* BR_Event_Queue_H */
