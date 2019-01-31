//
//  BREthereumNode.c
//  breadwallet-core Ethereum
//
//  Created by Lamont Samuels on 3/10/2018.
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
#include <stdlib.h>
#include <unistd.h>
#include <fcntl.h>
#include <errno.h>
#include <netdb.h>
#include <sys/socket.h>
#include <sys/time.h>
#include <netinet/in.h>

#include <string.h>
#include "BREthereumNode.h"
#include "BREthereumHandshake.h"
#include "BREthereumBase.h"
#include "BRKey.h"


#define PTHREAD_STACK_SIZE  (512 * 1024)
#define CONNECTION_TIME 3.0
#define DEFAULT_LES_PORT 30303

/**
 * BREthereumNodeContext - holds information about the client les node
 */
typedef struct {
    
    //The peer information for this node
    BREthereumPeer peer;
    
    //The public identifier for a node
    UInt512 id;
    
    //The Key for for a node
    BRKey key; 
    
    //The current connection status of a node
    BREthereumNodeStatus status;
    
    //The handshake context
    BREthereumHandShake handshake;
    
    //The thread representing this node
    pthread_t thread;
    
    //Lock to handle shared resources in the node
    pthread_mutex_t lock;
    
    //Represents whether this ndoe should start the handshake or wait for auth
    BREthereumBoolean shouldOriginate;
    
}BREthereumNodeContext;

/**** Private functions ****/
static BREthereumBoolean _isAddressIPv4(UInt128 address);
static int _openEtheruemPeerSocket(BREthereumNode node, int domain, double timeout, int *error);
static void _updateStatus(BREthereumNodeContext * node, BREthereumNodeStatus status);
static void *_nodeThreadRunFunc(void *arg);


/**
 * Note: This function is a direct copy of Aaron's _BRPeerOpenSocket function with a few modifications to
 * work for the Ethereum Core.
 * TODO: May want to make this more modular to work for both etheruem and bitcoin
 */
static int _openEtheruemPeerSocket(BREthereumNode node, int domain, double timeout, int *error)
{
    BREthereumNodeContext * cxt = (BREthereumNodeContext *)node;
    BREthereumPeer * peerCtx = &cxt->peer;
    
    struct sockaddr_storage addr;
    struct timeval tv;
    fd_set fds;
    socklen_t addrLen, optLen;
    int count, arg = 0, err = 0, on = 1, r = 1;

    peerCtx->socket = socket(domain, SOCK_STREAM, 0);
    
    if (peerCtx->socket < 0) {
        err = errno;
        r = 0;
    }
    else {
        tv.tv_sec = 1; // one second timeout for send/receive, so thread doesn't block for too long
        tv.tv_usec = 0;
        setsockopt(peerCtx->socket, SOL_SOCKET, SO_RCVTIMEO, &tv, sizeof(tv));
        setsockopt(peerCtx->socket, SOL_SOCKET, SO_SNDTIMEO, &tv, sizeof(tv));
        setsockopt(peerCtx->socket, SOL_SOCKET, SO_KEEPALIVE, &on, sizeof(on));
#ifdef SO_NOSIGPIPE // BSD based systems have a SO_NOSIGPIPE socket option to supress SIGPIPE signals
        setsockopt(peerCtx->socket, SOL_SOCKET, SO_NOSIGPIPE, &on, sizeof(on));
#endif
        arg = fcntl(peerCtx->socket, F_GETFL, NULL);
        if (arg < 0 || fcntl(peerCtx->socket, F_SETFL, arg | O_NONBLOCK) < 0) r = 0; // temporarily set socket non-blocking
        if (! r) err = errno;
    }

    if (r) {
        memset(&addr, 0, sizeof(addr));
        
        if (domain == PF_INET6) {
            ((struct sockaddr_in6 *)&addr)->sin6_family = AF_INET6;
            ((struct sockaddr_in6 *)&addr)->sin6_addr = *(struct in6_addr *)&peerCtx->address;
            ((struct sockaddr_in6 *)&addr)->sin6_port = htons(peerCtx->port);
            addrLen = sizeof(struct sockaddr_in6);
        }
        else {
            ((struct sockaddr_in *)&addr)->sin_family = AF_INET;
            ((struct sockaddr_in *)&addr)->sin_addr = *(struct in_addr *)&peerCtx->address.u32[3];
            ((struct sockaddr_in *)&addr)->sin_port = htons(peerCtx->port);
            addrLen = sizeof(struct sockaddr_in);
        }
        
        if (connect(peerCtx->socket, (struct sockaddr *)&addr, addrLen) < 0) err = errno;
        
        if (err == EINPROGRESS) {
            err = 0;
            optLen = sizeof(err);
            tv.tv_sec = timeout;
            tv.tv_usec = (long)(timeout*1000000) % 1000000;
            FD_ZERO(&fds);
            FD_SET(peerCtx->socket, &fds);
            count = select(peerCtx->socket + 1, NULL, &fds, NULL, &tv);

            if (count <= 0 || getsockopt(peerCtx->socket, SOL_SOCKET, SO_ERROR, &err, &optLen) < 0 || err) {
                if (count == 0) err = ETIMEDOUT;
                if (count < 0 || ! err) err = errno;
                r = 0;
            }
        }
        else if (err && domain == PF_INET6 && ETHEREUM_BOOLEAN_IS_TRUE(_isAddressIPv4(peerCtx->address))) {
            return _openEtheruemPeerSocket(node, PF_INET, timeout, error); // fallback to IPv4
        }
        else if (err) r = 0;

        if (r) {
            bre_peer_log(peerCtx, "ethereum socket connected");
        }
        fcntl(peerCtx->socket, F_SETFL, arg); // restore socket non-blocking status
    }

    if (! r && err) {
         bre_peer_log(peerCtx, "ethereum connect error: %s", strerror(err));
    }
    if (error && err) *error = err;
    return r;
}
/**
 * This is the theard run functions for an ethereum function. This function is called
 * when a node needs to begin connecting to a remote peer and start sending messages to the
 * remote node.
 */
static void *_nodeThreadRunFunc(void *arg) {

    BREthereumNodeContext * ctx = (BREthereumNodeContext *)arg;
    
    while(ctx->status != BRE_NODE_DISCONNECTED)
    {
        switch (ctx->status) {
            case BRE_NODE_DISCONNECTED:
            {
                continue;
            }
            break;
            case BRE_NODE_CONNECTING:
            {
             //   ctx->handshake = ethereumHandshakeCreate(&ctx->peer, &ctx->key, ctx->shouldOriginate);
                ctx->status = BRE_NODE_PERFORMING_HANDSHAKE;
            }
            break;
            case BRE_NODE_PERFORMING_HANDSHAKE:
            {
                if(ethereumHandshakeTransition(ctx->handshake) == BRE_HANDSHAKE_FINISHED) {
                    ctx->status = BRE_NODE_CONNECTED;
                    ethereumHandshakeFree(ctx->handshake);
                }
            }
            break;
            case BRE_NODE_CONNECTED:
            {
                //TODO: Read back messages from the remote peer
            }
            break;
            default:
            break;
        }
    }
    return NULL;
}
static BREthereumBoolean _isAddressIPv4(UInt128 address)
{
    return (address.u64[0] == 0 && address.u16[4] == 0 && address.u16[5] == 0xffff) ? ETHEREUM_BOOLEAN_TRUE : ETHEREUM_BOOLEAN_FALSE;
}
/*** Public functions ***/
BREthereumNode ethereumNodeCreate(BREthereumPeer peer, BREthereumBoolean originate) {

    BREthereumNodeContext * node = calloc (1, sizeof(*node));
    node->status = BRE_NODE_DISCONNECTED;
    node->peer.address = peer.address;
    node->peer.port = peer.port;
    node->peer.socket = peer.socket;
    node->peer.remoteId = peer.remoteId;
    node->handshake = NULL;
    node->shouldOriginate = originate;
    strncpy(node->peer.host, peer.host, sizeof(peer.host));
    node->id = UINT512_ZERO;
    
    {
        pthread_mutexattr_t attr;
        pthread_mutexattr_init(&attr);
        pthread_mutexattr_settype(&attr, PTHREAD_MUTEX_RECURSIVE);

        pthread_mutex_init(&node->lock, &attr);
        pthread_mutexattr_destroy(&attr);
    }
    
    return (BREthereumNode)node;

}
void ethereumNodeConnect(BREthereumNode node) {

    BREthereumNodeContext *ctx = (BREthereumNodeContext *)node;
    int error = 0;
    pthread_attr_t attr;
    
    if(_openEtheruemPeerSocket(node, PF_INET6, CONNECTION_TIME, &error)) {

        if (ctx->status == BRE_NODE_DISCONNECTED) {
            ctx->status = BRE_NODE_CONNECTING;
            
            if (pthread_attr_init(&attr) != 0) {
                error = ENOMEM;
                bre_peer_log(&ctx->peer, "error creating thread");
                ctx->status = BRE_NODE_DISCONNECTED;
            }
            else if (pthread_attr_setdetachstate(&attr, PTHREAD_CREATE_DETACHED) != 0 ||
                     pthread_attr_setstacksize(&attr, PTHREAD_STACK_SIZE) != 0 ||
                     pthread_create(&ctx->thread, &attr, _nodeThreadRunFunc, node) != 0) {
                error = EAGAIN;
                bre_peer_log(&ctx->peer, "error creating thread");
                pthread_attr_destroy(&attr);
                ctx->status = BRE_NODE_DISCONNECTED;
            }
        }
    }
}
BREthereumNodeStatus ethereumNodeStatus(BREthereumNode node){

    BREthereumNodeContext *ctx = (BREthereumNodeContext *)node;
    return ctx->status;
}
void ethereumNodeDisconnect(BREthereumNode node) {
    
    BREthereumNodeContext *ctx = (BREthereumNodeContext *)node;
    ctx->status = BRE_NODE_DISCONNECTED;
    int socket = ctx->peer.socket;

    if (socket >= 0) {
        ctx->peer.socket = -1;
        if (shutdown(socket, SHUT_RDWR) < 0){
            bre_peer_log(&ctx->peer, "%s", strerror(errno));
        }
        close(socket);
    }
}
void ethereumNodeFree(BREthereumNode node) {

    BREthereumNodeContext *ctx = (BREthereumNodeContext *)node;
    free(ctx);
}
const char * ethereumPeerGetHost(BREthereumPeer* peer){
    
    if( peer->host[0] == '\0') {
        if (ETHEREUM_BOOLEAN_IS_TRUE(_isAddressIPv4(peer->address))) {
            inet_ntop(AF_INET, &peer->address.u32[3], peer->host, sizeof(peer->host));
        }else {
            inet_ntop(AF_INET6, &peer->address, peer->host, sizeof(peer->host));
        }
    }
    return peer->host;
}
