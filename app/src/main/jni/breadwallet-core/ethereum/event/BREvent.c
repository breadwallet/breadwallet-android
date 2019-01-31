//
//  BREvent.c
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

#include <errno.h>
#include <pthread.h>
#include "BREvent.h"
#include "BREventQueue.h"

#if defined (__ANDROID__)
static int
pthread_cond_timedwait_relative_np(pthread_cond_t *cond,
                                   pthread_mutex_t *lock,
                                   const struct timespec *time) {
    struct timeval  now;
    struct timespec timeout;

    gettimeofday(&now, NULL);

    timeout.tv_sec = now.tv_sec + time->tv_sec;
    timeout.tv_nsec = 1000 * now.tv_usec + time->tv_nsec;

    return pthread_cond_timedwait(cond, lock, &timeout);
}
#endif // defined (__ANDROID__)

/* Forward Declarations */
static void *
eventHandlerThread (BREventHandler handler);

//
// Event Handler Thread Status
//
typedef enum  {
    EVENT_HANDLER_THREAD_STATUS_STARTING,
    EVENT_HANDLER_THREAD_STATUS_RUNNING,
    EVENT_HANDLER_THREAD_STATUS_STOPPING,
    EVENT_HANDLER_THREAD_STATUS_STOPPED
} BREventHandlerThreadStatus;

//
// Event Handler
//
struct BREventHandlerRecord {
    // Types
    size_t typesCount;
    const BREventType **types;

    // Queue
    size_t eventSize;
    BREventQueue queue;
    BREvent *scratch;

    // (Optional) Timeout
    struct timespec timeout;
    BREventDispatcher timeoutDispatcher;

    // Thread
    pthread_t thread;
    pthread_cond_t cond;
    pthread_mutex_t lock;

    BREventHandlerThreadStatus status;
};

extern BREventHandler
eventHandlerCreate (const BREventType *types[], unsigned int typesCount) {
    BREventHandler handler = calloc (1, sizeof (struct BREventHandlerRecord));

    handler->status = EVENT_HANDLER_THREAD_STATUS_STOPPED;
    handler->typesCount = typesCount;
    handler->types = types;
    handler->eventSize = 0;

    // Update `eventSize` with the largest sized event
    for (int i = 0; i < handler->typesCount; i++) {
        const BREventType *type = handler->types[i];

        if (handler->eventSize < type->eventSize)
            handler->eventSize = type->eventSize;
    }

    // Create the PTHREAD CONDition variable
    {
        pthread_condattr_t attr;
        pthread_condattr_init(&attr);
        pthread_cond_init(&handler->cond, &attr);
        pthread_condattr_destroy(&attr);
    }

    // Create the PTHREAD LOCK variable
    {
        // The cacheLock is a normal, non-recursive lock
        pthread_mutexattr_t attr;
        pthread_mutexattr_init(&attr);
        pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_NORMAL);
        pthread_mutex_init(&handler->lock, &attr);
        pthread_mutexattr_destroy(&attr);
    }

    handler->scratch = (BREvent*) calloc (1, handler->eventSize);
    handler->queue = eventQueueCreate(handler->eventSize, &handler->lock);

    return handler;
}

extern void
eventHandlerSetTimeoutDispatcher (BREventHandler handler,
                                  unsigned int timeInMilliseconds,
                                  BREventDispatcher dispatcher) {
    pthread_mutex_lock(&handler->lock);
    handler->timeout.tv_sec = timeInMilliseconds / 1000;
    handler->timeout.tv_nsec = 1000000 * (timeInMilliseconds % 1000);
    handler->timeoutDispatcher = dispatcher;
    pthread_mutex_unlock(&handler->lock);


    pthread_cond_signal(&handler->cond);
}

#define PTHREAD_STACK_SIZE (512 * 1024)
#define PTHREAD_SLEEP_SECONDS (15)

typedef void* (*ThreadRoutine) (void*);

static void *
eventHandlerThread (BREventHandler handler) {
//    node->state = LIGHT_NODE_CONNECTED;

    pthread_mutex_lock(&handler->lock);
    handler->status = EVENT_HANDLER_THREAD_STATUS_RUNNING;

    while (EVENT_HANDLER_THREAD_STATUS_RUNNING == handler->status) {
        // If there is an event pending...
        if (eventQueueHasPending(handler->queue)) {
            // ... then handle it
            switch (eventQueueDequeue(handler->queue, handler->scratch)) {
                case EVENT_STATUS_SUCCESS: {
                    BREventType *type = handler->scratch->type;
                    type->eventDispatcher (handler, handler->scratch);
                    break;
                }

                case EVENT_STATUS_NOT_STARTED:
                case EVENT_STATUS_UNKNOWN_TYPE:
                case EVENT_STATUS_NULL_EVENT:
                    // impossible?
                    break;

                case EVENT_STATUS_NONE_PENDING:
                    break;
            }
        }
        // ... otherwise wait for an event ...
        else if (NULL == handler->timeoutDispatcher)
            pthread_cond_wait(&handler->cond, &handler->lock);
        // ... or for a timeout.
        else if (ETIMEDOUT == pthread_cond_timedwait_relative_np(&handler->cond,
                                                                 &handler->lock,
                                                                 &handler->timeout))
            handler->timeoutDispatcher (handler, NULL);
    }

    handler->status = EVENT_HANDLER_THREAD_STATUS_STOPPED;
    pthread_detach(handler->thread);
    return NULL;
}

extern void
eventHandlerDestroy (BREventHandler handler) {
    pthread_kill(handler->thread, 0);
    pthread_cond_destroy(&handler->cond);
    pthread_mutex_destroy(&handler->lock);

    eventQueueDestroy(handler->queue);
    free (handler->scratch);
    free (handler);
}

//
// Start / Stop
//
extern void
eventHandlerStart (BREventHandler handler) {
    switch (handler->status) {
        case EVENT_HANDLER_THREAD_STATUS_RUNNING:
        case EVENT_HANDLER_THREAD_STATUS_STARTING:
            break;

        case EVENT_HANDLER_THREAD_STATUS_STOPPED:
        case EVENT_HANDLER_THREAD_STATUS_STOPPING: {
            handler->status = EVENT_HANDLER_THREAD_STATUS_STARTING;

            // if (0 != pthread_attr_t (...) && 0 != pthread_attr_...() && ...
            pthread_attr_t attr;
            pthread_attr_init(&attr);
            pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED);
            pthread_attr_setstacksize(&attr, PTHREAD_STACK_SIZE);

            pthread_create(&handler->thread, &attr, (ThreadRoutine) eventHandlerThread, handler);
            pthread_attr_destroy(&attr);
            break;
        }
    }
}

extern void
eventHandlerStop (BREventHandler handler) {
    switch (handler->status) {
        case EVENT_HANDLER_THREAD_STATUS_RUNNING:
        case EVENT_HANDLER_THREAD_STATUS_STARTING:
            handler->status = EVENT_HANDLER_THREAD_STATUS_STOPPING;
            break;

        case EVENT_HANDLER_THREAD_STATUS_STOPPED:
        case EVENT_HANDLER_THREAD_STATUS_STOPPING:
            break;
    }
}

extern BREventStatus
eventHandlerSignalEvent (BREventHandler handler,
                         BREvent *event) {
    eventQueueEnqueue(handler->queue, event);
    pthread_cond_signal(&handler->cond);
    return EVENT_STATUS_SUCCESS;
}
