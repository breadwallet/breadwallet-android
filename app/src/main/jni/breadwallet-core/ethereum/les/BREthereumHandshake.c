//
//  BREthereumNode.h
//  breadwallet-core Ethereum
//
//  Created by Lamont Samuels on 4/18/18.
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
#include <errno.h>
#include <string.h>
#include <netdb.h>
#include <sys/socket.h>
#include <sys/types.h>
#include <unistd.h>
#include "BRInt.h"
#include "BREthereumBase.h"
#include "BRKey.h"
#include "BRCrypto.h"
#include "BREthereumHandshake.h"
#include "BREthereumNode.h"
#include "BREthereumLESBase.h"
#include "BRRlpCoder.h"
#include "BRArray.h"

#ifndef MSG_NOSIGNAL   // linux based systems have a MSG_NOSIGNAL send flag, useful for supressing SIGPIPE signals
#define MSG_NOSIGNAL 0 // set to 0 if undefined (BSD has the SO_NOSIGPIPE sockopt, and windows has no signals at all)
#endif

#define SIG_SIZE_BYTES      65
#define PUBLIC_SIZE_BYTES   64
#define HEPUBLIC_BYTES      64
#define NONCE_BYTES         64

static const ssize_t authBufLen = SIG_SIZE_BYTES + HEPUBLIC_BYTES + PUBLIC_SIZE_BYTES + NONCE_BYTES + 1;
static const ssize_t ackBufLen = PUBLIC_SIZE_BYTES + NONCE_BYTES + 1;

typedef struct {

    //A weak reference to the remote peer information
    BREthereumPeer* peer;
    
    //A weak reference to the frame coder between a node and its peer
    BREthereumFrameCoder ioCoder;
    
    //The header information for a peer
    BREthereumLESHeader peerHeader;
    
    //The next state of the handshake
    BREthereumHandshakeStatus nextState;
    
    //A weak reference to the BREthereumNode's keypair
    BRKey* key;
    
    //A local nonce for the handshake
    UInt256 nonce;
    
   // Local Ephemeral ECDH key
    BRKey ecdhe;
    
    //The plain auth buffer
    uint8_t authBuf[authBufLen];
    
    //The cipher auth buffer
    uint8_t authBufCipher[authBufLen];
    
    //The plain ack buffer
    uint8_t ackBuf[ackBufLen];
    
    //The cipher ack buffer
    uint8_t ackBufCipher[ackBufLen];
    
    //Represents whether remote peer or node initiated the handshake
    BREthereumBoolean didOriginate;
    
    //Represents the bytes of the status message to send. This is already RLP encoded
    uint8_t * statusBytes;
    
    //The length of the status message
    size_t statusBytesLen;
    
    // The public key for the remote peer
    UInt512 remotePubKey;
    
    // The nonce for the remote peer
    UInt256 remoteNonce;
    
    // The ephemeral public key of the remote peer
    UInt512 remoteEphemeralKey;
    
}BREthereumHandshakeContext;

//
// Private functions
//
int _readBuffer(BREthereumHandshakeContext* handshakeCtx, uint8_t * buf, size_t bufSize, const char * type){

    BREthereumPeer * peerCtx = handshakeCtx->peer;
    ssize_t n = 0, len = 0;
    int socket, error = 0;

    bre_peer_log(peerCtx, "handshake reading: %s", type);

    socket = peerCtx->socket;

    if (socket < 0) error = ENOTCONN;

    while (socket >= 0 && ! error && len < bufSize) {
        n = read(socket, &buf[len], bufSize - len);
        if (n > 0) len += n;
        if (n == 0) error = ECONNRESET;
        if (n < 0 && errno != EWOULDBLOCK) error = errno;
        
        socket = peerCtx->socket;
    }
    
    if (error) {
        bre_peer_log(peerCtx, "%s", strerror(error));
    }
    return error;
}
int _sendBuffer(BREthereumHandshakeContext* handshakeCtx, uint8_t * buf, size_t bufSize, char* type){

    BREthereumPeer * peerCtx = handshakeCtx->peer;
    ssize_t n = 0;
    int socket, error = 0;

    bre_peer_log(peerCtx, "handshake sending: %s", type);

    size_t offset = 0;
    socket = peerCtx->socket;

    if (socket < 0) error = ENOTCONN;

    while (socket >= 0 && !error && offset <  bufSize) {
        n = send(socket, &buf[offset], bufSize - offset, MSG_NOSIGNAL);
        if (n >= 0) offset += n;
        if (n < 0 && errno != EWOULDBLOCK) error = errno;
        socket = peerCtx->socket;
    }

    if (error) {
        bre_peer_log(peerCtx, "%s", strerror(error));
    }
    return error;
}
int _writeAuth(BREthereumHandshakeContext * ctx){

    BREthereumPeer* peer = ctx->peer;
    
    bre_peer_log(peer, "sending auth");

    // authInitiator -> E(remote-pubk, S(ecdhe-random, ecdh-shared-secret^nonce) || H(ecdhe-random-pubk) || pubk || nonce || 0x0)
    uint8_t * authBuf = ctx->authBuf;
    uint8_t * authBufCipher = ctx->authBufCipher;

    uint8_t* signature = &authBuf[0];
    uint8_t* hPubKey = &authBuf[SIG_SIZE_BYTES];
    uint8_t* pubKey = &authBuf[SIG_SIZE_BYTES + HEPUBLIC_BYTES];
    uint8_t* nonce =  &authBuf[SIG_SIZE_BYTES + HEPUBLIC_BYTES + NONCE_BYTES];
    
    //ephemeral-shared-secret = ecdh.agree(ephemeral-privkey, remote-ephemeral-pubk)
    UInt256 ephemeralSharedSecret;
    
    etheruemECDHAgree(ctx->key, &ctx->peer->remoteId, &ephemeralSharedSecret);
    
    //ecdh-shared-secret^nonce
    UInt256 xorStaticNonce;
    ethereumXORBytes(ephemeralSharedSecret.u8, ctx->nonce.u8, xorStaticNonce.u8, sizeof(ctx->nonce.u8));
    
    // S(ecdhe-random, ecdh-shared-secret^nonce)
    BRKeySign(&ctx->ecdhe, signature, SIG_SIZE_BYTES, xorStaticNonce);
    // || H(ecdhe-random-pubk) ||
    BRKeccak256(hPubKey, ctx->ecdhe.pubKey, 32);
    memset(&hPubKey[32], 0, 32);
    // || pubK ||
    memcpy(pubKey, ctx->key->pubKey, sizeof(ctx->key->pubKey));
    // || nonce ||
    memcpy(nonce, ctx->nonce.u8, sizeof(ctx->nonce.u8));
    // || 0x0   ||
    authBuf[authBufLen - 1] = 0x0;

    //E(remote-pubk, S(ecdhe-random, ecdh-shared-secret^nonce) || H(ecdhe-random-pubk) || pubk || nonce || 0x0)
    ethereumEncryptECIES(&ctx->peer->remoteId, authBuf, authBufCipher, authBufLen);
    
    return _sendBuffer(ctx, authBufCipher, authBufLen, "writeAuth");

}
void _writeAck(BREthereumHandshakeContext * ctx) {

    BREthereumPeer* peer = ctx->peer;
    
    bre_peer_log(peer, "sending ack");

    // ack -> E( epubk || nonce || 0x0)
    uint8_t* ackBuf = ctx->ackBuf;
    uint8_t* ackBufCipher = ctx->ackBufCipher;

    uint8_t* pubKey = &ackBuf[0];
    uint8_t* nonce =  &ackBuf[PUBLIC_SIZE_BYTES];
    
    // || epubK ||
    memcpy(pubKey, ctx->ecdhe.pubKey, sizeof(ctx->ecdhe.pubKey));
    // || nonce ||
    memcpy(nonce, ctx->nonce.u8, sizeof(ctx->nonce.u8));
    // || 0x0   ||
    ackBuf[ackBufLen- 1] = 0x0;

    //E( epubk || nonce || 0x0)
    ethereumEncryptECIES(&ctx->peer->remoteId, ackBuf, ackBufCipher, ackBufLen);
    
    _sendBuffer(ctx, ackBufCipher, ackBufLen, "writeAuck");
}
int _readAuth(BREthereumHandshakeContext * ctx) {

    BREthereumPeer* peer = ctx->peer;
    
    bre_peer_log(peer, "receiving auth");
    
    int ec = _readBuffer(ctx, ctx->authBufCipher, authBufLen, "auth");
    
    if (ec) {
        return ec;
    }
    else if (ethereumDecryptECIES(&ctx->key->secret, ctx->authBufCipher, ctx->authBuf, authBufLen))
    {
        //copy remote nonce
        memcpy(ctx->remoteNonce.u8, &ctx->authBuf[SIG_SIZE_BYTES + HEPUBLIC_BYTES + PUBLIC_SIZE_BYTES], sizeof(ctx->remoteNonce.u8));
        
        //copy remote public key
        memcpy(ctx->remotePubKey.u8, &ctx->authBuf[SIG_SIZE_BYTES + HEPUBLIC_BYTES], sizeof(ctx->remotePubKey.u8));

        UInt256 sharedSecret;
        
        etheruemECDHAgree(ctx->key, ctx->remotePubKey.u8, &sharedSecret);
        
        UInt256 xOrSharedSecret;
        ethereumXORBytes(sharedSecret.u8, ctx->remoteNonce.u8, xOrSharedSecret.u8, sizeof(xOrSharedSecret.u8));
        
        BRKey key;
        BRKeyRecoverPubKey(&key, xOrSharedSecret, ctx->authBuf, SIG_SIZE_BYTES);
        
        // The ephemeral public key of the remote peer
        memcpy(ctx->remoteEphemeralKey.u8, key.pubKey, sizeof(ctx->remoteEphemeralKey.u8));
        
    }
    return ec;
}
int _readAck(BREthereumHandshakeContext * ctx) {

    BREthereumPeer* peer = ctx->peer;
    
    bre_peer_log(peer, "receiving ack");
    
    int ec = _readBuffer(ctx, ctx->ackBufCipher, ackBufLen, "ack");
    
    if (ec) {
        return ec;
    }
    else if (ethereumDecryptECIES(&ctx->key->secret, ctx->ackBufCipher, ctx->ackBuf, ackBufLen))
    {
        //copy remote nonce key
        memcpy(ctx->remoteNonce.u8, &ctx->authBuf[HEPUBLIC_BYTES], sizeof(ctx->remoteNonce.u8));
        
        //copy ephemeral public key of the remote peer
        memcpy(ctx->remoteEphemeralKey.u8, &ctx->authBuf, sizeof(ctx->remoteEphemeralKey.u8));
    }
    return ec;
}
int _writeStatus(BREthereumHandshakeContext * ctx){
    
    BREthereumPeer* peer = ctx->peer;
    
    bre_peer_log(peer, "sending status message with capabilities handshake");
    
    uint8_t* statusBuffer;
    size_t statusBufferSize;
    
    ethereumFrameCoderWrite(ctx->ioCoder, 0x00, ctx->statusBytes, ctx->statusBytesLen, &statusBuffer, &statusBufferSize);
    
    _sendBuffer(ctx,statusBuffer, statusBufferSize, "write status");
    
    free(statusBuffer);
    
    return 0;
}
void _decodeStatus (BREthereumLESHeader* header,
                   BRRlpCoder coder,
                   BRRlpItem item ) {
    
    size_t itemsCount = 0;
    const BRRlpItem *items = rlpDecodeList(coder, item, &itemsCount);
    char* key = rlpDecodeItemString(coder, items[0]);
    
    if(strcmp(key, "protocolVersion") == 0) {
        header->protocolVersion = rlpDecodeItemUInt64(coder, items[1], 0);
    }else if (strcmp(key, "networkID") == 0) {
        header->chainId = rlpDecodeItemUInt64(coder, items[1], 0);
    }else if (strcmp(key, "headTd") == 0) {
        header->headerTd = rlpDecodeItemUInt64(coder, items[1], 0);
    }else if (strcmp(key, "headHash") == 0) {
        BRRlpData hashData = rlpDecodeItemBytes(coder, items[1]);
        memcpy(header->headHash, hashData.bytes, hashData.bytesCount);
        rlpDataRelease(hashData);
    }else if (strcmp(key, "headNum") == 0) {
        header->headerTd = rlpDecodeItemUInt64(coder, items[1], 0);
    }else if (strcmp(key, "genesisHash") == 0) {
        BRRlpData hashData = rlpDecodeItemBytes(coder, items[1]);
        memcpy(header->genesisHash, hashData.bytes, hashData.bytesCount);
        rlpDataRelease(hashData);
    }else if (strcmp(key, "serveHeaders") == 0) {
        header->serveHeaders = malloc(sizeof(BREthereumBoolean));
        *(header->serveHeaders) = ETHEREUM_BOOLEAN_TRUE;
    }else if (strcmp(key, "serveHeaders") == 0) {
        header->serveHeaders = malloc(sizeof(BREthereumBoolean));
        *(header->serveHeaders) = ETHEREUM_BOOLEAN_TRUE;
    }else if (strcmp(key, "serveChainSince") == 0) {
        header->serveChainSince = malloc(sizeof(uint64_t));
        *(header->serveChainSince) = rlpDecodeItemUInt64(coder, items[1], 0);
    }else if (strcmp(key, "serveStateSince") == 0) {
        header->serveStateSince = malloc(sizeof(uint64_t));
        *(header->serveStateSince) = rlpDecodeItemUInt64(coder, items[1], 0);
    }else if (strcmp(key, "txRelay") == 0) {
        header->txRelay = malloc(sizeof(BREthereumBoolean));
        *(header->txRelay) = ETHEREUM_BOOLEAN_TRUE;
    }
    //TODO: Add client side flow control model
    
}
int _readStatus(BREthereumHandshakeContext * ctx) {

    BREthereumPeer* peer = ctx->peer;

    bre_peer_log(peer, "reading status message with capabilities handshake");
    
    uint8_t header[32];
    
    if(_readBuffer(ctx, header, 32, "reading in header")){
        return BRE_HANDSHAKE_ERROR;
    }
    
    // authenticate and decrypt header
    if(ETHEREUM_BOOLEAN_IS_FALSE(ethereumFrameCoderDecryptHeader(ctx->ioCoder, header, 32)))
    {
        return BRE_HANDSHAKE_ERROR;
    }
    bre_peer_log(peer, "reacieved header message");

    //Get frame size
    uint32_t frameSize = (uint32_t)(header[2]) | (uint32_t)(header[1])<<8 | (uint32_t)(header[0])<<16;
    
    if(frameSize > 1024){
        bre_peer_log(peer, "status message is too large");
        return BRE_HANDSHAKE_ERROR;
    }
    
    uint32_t fullFrameSize = frameSize + ((16 - (frameSize % 16)) % 16) + 16;
    
    uint8_t* body;
    
    array_new(body, fullFrameSize);
    
    if(_readBuffer(ctx, body, fullFrameSize, "reading in frame body (packet type, packet-data)")) {
        return BRE_HANDSHAKE_ERROR;
    }
    
    // authenticate and decrypt frame
    if(ETHEREUM_BOOLEAN_IS_FALSE(ethereumFrameCoderDecryptFrame(ctx->ioCoder, body, fullFrameSize)))
    {
        return BRE_HANDSHAKE_ERROR;
    }
    
    BRRlpCoder coder = rlpCoderCreate();
    BRRlpData frameData = {1, body};
    BRRlpItem item = rlpGetItem (coder, frameData);
    
    BRRlpData packetType = rlpDecodeItemBytes(coder, item);
    
    if(packetType.bytes[0] != 0x00){
        bre_peer_log(peer, "invalid packet type. Expected: Status packet");
        return BRE_HANDSHAKE_ERROR;
    }
    frameData.bytesCount = frameSize - 1;
    frameData.bytes = &body[1];
    
    //Get the status information
    item = rlpGetItem (coder, frameData);
    
    size_t itemsCount = 0;
    const BRRlpItem *items = rlpDecodeList(coder, item, &itemsCount);
    
    uint64_t packetTypeMsg = rlpDecodeItemUInt64(coder, items[0], 0);

    if(packetTypeMsg != 0x00){
        bre_peer_log(peer, "Packet type in message is incorrect");
        return BRE_HANDSHAKE_ERROR;
    }
    
    for(unsigned int i = 1; i < itemsCount; ++i){
       _decodeStatus(&ctx->peerHeader, coder, items[i]);
    }

    rlpCoderRelease(coder);
    
    return 0;
}

//
// Public functions
//
BREthereumHandShake ethereumHandshakeCreate(BREthereumPeer* peer,
                                            BRKey* nodeKey,
                                            BREthereumBoolean didOriginate,
                                            uint8_t* statusMessage,
                                            size_t statusMessageLen,
                                            BREthereumFrameCoder coder) {

    BREthereumHandshakeContext * ctx =  ( BREthereumHandshakeContext *) calloc (1, sizeof(*ctx));
    ctx->peer = peer;
    ctx->ioCoder = coder;
    ctx->nextState = BRE_HANDSHAKE_NEW;
    ctx->key = nodeKey;
    ctx->didOriginate = didOriginate;
    ctx->statusBytes = (uint8_t *)calloc(statusMessageLen, sizeof(uint8_t));
    memcpy(ctx->statusBytes, statusMessage, statusMessageLen);
    ctx->statusBytesLen = statusMessageLen;
    ctx->nonce = ethereumGetNonce();
    return (BREthereumHandShake)ctx;
}
BREthereumHandshakeStatus ethereumHandshakeTransition(BREthereumHandShake handshake){

    BREthereumHandshakeContext* ctx = (BREthereumHandshakeContext *)handshake;
    
    if (ctx->nextState == BRE_HANDSHAKE_NEW)
    {
        ctx->nextState = BRE_HANDSHAKE_ACKAUTH;
        if (ETHEREUM_BOOLEAN_IS_TRUE(ctx->didOriginate))
        {
            _writeAuth(ctx);
        }
        else
        {
            _readAuth(ctx);
        }
    }
    else if (ctx->nextState == BRE_HANDSHAKE_ACKAUTH)
    {
        ctx->nextState = BRE_HANDSHAKE_WRITESTATUS;
        if (ETHEREUM_BOOLEAN_IS_TRUE(ctx->didOriginate))
        {
            _readAck(ctx);
        }
        else
        {
            _writeAck(ctx);
        }
    }
    else if (ctx->nextState == BRE_HANDSHAKE_WRITESTATUS)
    {
        ctx->nextState = BRE_HANDSHAKE_READSTATUS;
       _writeStatus(ctx);
       
    }
    else if (ctx->nextState == BRE_HANDSHAKE_READSTATUS)
    {
        // Authenticate and decrypt initial hello frame with initial RLPXFrameCoder
        _readStatus (ctx);
        ctx->nextState = BRE_HANDSHAKE_FINISHED;
    }
    
    return ctx->nextState;
}
void ethereumHandshakeFree(BREthereumHandShake handshake) {

    BREthereumHandshakeContext * ctx =  (BREthereumHandshakeContext *) handshake;
    free(ctx->statusBytes);
    free(ctx);
}
