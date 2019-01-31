//
//  BREthereumFrameCoder.h
//  breadwallet-core Ethereum
//
//  Created by Lamont Samuels on 4/26/18.
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
//#include "aes.h"
//#include "sha3.h"
#include "BRCrypto.h"
#include "BRKey.h"
#include "BREthereumFrameCoder.h"
#include "BREthereumLESBase.h"
#include "BRRlpCoder.h"
#include "BRArray.h"

#define UINT256_SIZE 32

/**
 *
 * The context for a frame coder
 */
typedef struct {

    //Encryption for frame
   // struct aes256_ctx frameEncrypt;
    
    //Decryption for frame
  //  struct aes256_ctx frameDecrypt;
    
    //Encryption for Mac
  //  struct aes256_ctx macEncrypt;
    
    // Ingress ciphertext
   // struct sha3_256_ctx ingressMac;
    
    // Egress ciphertext
  //  struct sha3_256_ctx egressMac;
    
}BREthereumFrameCoderContext;

//
// Private Functions
//
void _egressDigest(BREthereumFrameCoderContext* ctx, UInt128 * digest)
{

  //  struct sha3_256_ctx curEgressMacH;
 //   memcpy(&curEgressMacH, &ctx->egressMac, sizeof(struct sha3_256_ctx));
  //  sha3_256_digest(&curEgressMacH, sizeof(digest->u8), digest->u8);
}
void _ingressDigest(BREthereumFrameCoderContext* ctx, UInt128 * digest)
{
//    struct sha3_256_ctx curIngressMacH;
//    memcpy(&curIngressMacH, &ctx->ingressMac, sizeof(struct sha3_256_ctx));
//    sha3_256_digest(&curIngressMacH, sizeof(digest->u8), digest->u8);
}
void _updateMac(BREthereumFrameCoderContext* ctx, struct sha3_256_ctx* mac, uint8_t* sData, size_t sDataSize) {

    //Peform check for sData size is h1238 _seed.size() && _seed.size() != h128::size)
//    struct sha3_256_ctx prevDigest;
//    memcpy(&prevDigest, mac, sizeof(struct sha3_256_ctx));
    UInt128 encDigest;
    
  //  sha3_256_digest(&prevDigest, 16, encDigest.u8);
    
    UInt128 pDigest;
    
    memcpy(&pDigest.u8, &encDigest.u8, 16);
    
//    aes256_encrypt(&ctx->macEncrypt, 16, encDigest.u8, encDigest.u8);

    UInt128 xOrDigest;
    
    if (sDataSize){
        ethereumXORBytes(encDigest.u8, sData, xOrDigest.u8, 128);
    }
    else{
        ethereumXORBytes(encDigest.u8, pDigest.u8, xOrDigest.u8, 128);
    }

//    sha3_256_update(mac, sizeof(encDigest.u8), encDigest.u8);
    
}
void _writeFrame(BREthereumFrameCoderContext* ctx, BRRlpData * headerData, uint8_t* payload, size_t payloadSize, uint8_t** oBytes, size_t * oBytesSize)
{
    // TODO: SECURITY check header values && header <= 16 bytes
    size_t uint256_size = 32;
    uint8_t headerMac[uint256_size];
    memcpy(headerMac, headerData->bytes, headerData->bytesCount);
    
   // aes256_encrypt(&ctx->frameEncrypt, 16, headerMac, headerMac);
    
  //  _updateMac(ctx, &ctx->egressMac, headerMac, 16);
    UInt128 egressDigest;
    
    _egressDigest(ctx, &egressDigest);
    
    
    memcpy(&headerMac[16], egressDigest.u8, sizeof(egressDigest.u8));
    
    uint32_t padding = (16 - (payloadSize % 16)) % 16;
    

    size_t oBytesPtrSize = 32 + payloadSize + padding + 16;
    uint8_t* oBytesPtr = (uint8_t *)calloc(oBytesPtrSize, sizeof(uint8_t));
    
    memcpy(oBytesPtr, headerMac, sizeof(headerMac));
    
    //aes256_encrypt(&ctx->frameEncrypt, payloadSize, &oBytesPtr[32], payload);

    if (padding) {
   //     aes256_encrypt(&ctx->frameEncrypt, padding, &oBytesPtr[32 + payloadSize], &oBytesPtr[32 + payloadSize]);
    }
    
    
   // sha3_256_update(&ctx->egressMac, payloadSize + padding, &oBytesPtr[32]);
  //  _updateMac(ctx, &ctx->egressMac, NULL, 0);
    
    UInt128 egressDigestFrame;
    _egressDigest(ctx, &egressDigestFrame);
    memcpy(&oBytesPtr[32 + payloadSize + padding], egressDigestFrame.u8, sizeof(egressDigestFrame.u8));
    
    *oBytes = oBytesPtr;
    *oBytesSize = oBytesPtrSize;
}
//
// Public Functions
//
BREthereumFrameCoder ethereumFrameCoderCreate(void) {
    
    BREthereumFrameCoderContext * ctx = (BREthereumFrameCoderContext*) calloc (1, sizeof(*ctx));
    return (BREthereumFrameCoder)ctx;
}
BREthereumBoolean ethereumFrameCoderInit(BREthereumFrameCoder fCoder,
                            UInt512* remoteEphemeral,
                            UInt256* remoteNonce,
                            BRKey* ecdheLocal,
                            UInt256* localNonce,
                            uint8_t* aukCipher,
                            size_t aukCipherLen,
                            uint8_t* authCiper,
                            size_t authCipherLen,
                            BREthereumBoolean didOriginate) {
    
    BREthereumFrameCoderContext * ctx = (BREthereumFrameCoderContext*) fCoder;
    uint8_t keyMaterial[64];
    size_t uint256_size = 32;
    
    // shared-secret = sha3(ecdhe-shared-secret || sha3(nonce || initiator-nonce))
    UInt256 ephemeralShared;
    
    if(etheruemECDHAgree(ecdheLocal, remoteEphemeral, &ephemeralShared)){
        return ETHEREUM_BOOLEAN_FALSE;
    }
    
    memcpy(keyMaterial, &ephemeralShared, uint256_size);

    UInt512 nonceMaterial;
    
    UInt256* lNonce = ETHEREUM_BOOLEAN_IS_TRUE(didOriginate) ? remoteNonce : localNonce;
    memcpy(nonceMaterial.u8, lNonce->u8, uint256_size);


    UInt256* rNonce = ETHEREUM_BOOLEAN_IS_TRUE(didOriginate)  ? localNonce : remoteNonce;
    memcpy(&nonceMaterial.u8[sizeof(lNonce->u8)], rNonce->u8, uint256_size);

    
    // sha3(nonce || initiator-nonce)
    BRKeccak256(&keyMaterial[sizeof(lNonce->u8)], &nonceMaterial.u8,uint256_size);
    
    
    // shared-secret = sha3(ecdhe-shared-secret || sha3(nonce || initiator-nonce))
    BRKeccak256(&keyMaterial[uint256_size], keyMaterial, uint256_size);


    // aes-secret = sha3(ecdhe-shared-secret || shared-secret)
    BRKeccak256(&keyMaterial[uint256_size], &keyMaterial[uint256_size], uint256_size);
    
 //  aes256_set_encrypt_key(&ctx->frameEncrypt, &keyMaterial[uint256_size]);
 //   aes256_set_encrypt_key(&ctx->frameDecrypt, &keyMaterial[uint256_size]);
    

    // mac-secret = sha3(ecdhe-shared-secret || aes-secret)
     BRKeccak256(&keyMaterial[uint256_size], &keyMaterial[uint256_size], uint256_size);
 //   aes256_set_encrypt_key(&ctx->macEncrypt, &keyMaterial[uint256_size]);


    // Initiator egress-mac: sha3(mac-secret^recipient-nonce || auth-sent-init)
    //           ingress-mac: sha3(mac-secret^initiator-nonce || auth-recvd-ack)
    // Recipient egress-mac: sha3(mac-secret^initiator-nonce || auth-sent-ack)
    //           ingress-mac: sha3(mac-secret^recipient-nonce || auth-recvd-init)
    UInt256 xORMacRemoteNonce;
    ethereumXORBytes(&keyMaterial[uint256_size], remoteNonce->u8, xORMacRemoteNonce.u8, uint256_size);
    memcpy(keyMaterial, xORMacRemoteNonce.u8, uint256_size);
    uint8_t* egressCipher, *ingressCipher;
    size_t egressCipherLen, ingressCipherLen;
    
    if(didOriginate)
    {
        egressCipher = aukCipher;
        egressCipherLen = aukCipherLen;
        ingressCipher = aukCipher;
        ingressCipherLen = aukCipherLen;
    }
    else
    {
        egressCipher = authCiper;
        egressCipherLen = authCipherLen;
        ingressCipher = authCiper;
        ingressCipherLen = authCipherLen;
    }
    
    uint8_t* gressBytes;
    size_t egressBytesLen = uint256_size + egressCipherLen;
    
    
    array_new(gressBytes, egressBytesLen);
    array_add_array(gressBytes, keyMaterial, uint256_size);
    array_insert_array(gressBytes, uint256_size, egressCipher, egressCipherLen);
 //   sha3_256_init(&ctx->egressMac);
    
 //   sha3_256_update(&ctx->egressMac, egressBytesLen, gressBytes);
    
    // recover mac-secret by re-xoring remoteNonce
    UInt256 xOrMacSecret;
    ethereumXORBytes(xORMacRemoteNonce.u8, localNonce->u8, xOrMacSecret.u8, uint256_size);
    size_t ingressBytesLen = uint256_size + egressCipherLen;

    array_set_capacity(gressBytes, ingressBytesLen);
    array_insert_array(gressBytes, 0, xOrMacSecret.u8, uint256_size);
    array_insert_array(gressBytes, uint256_size, ingressCipher, ingressBytesLen);
//    sha3_256_init(&ctx->ingressMac);
//    sha3_256_update(&ctx->ingressMac, ingressBytesLen, gressBytes);

    array_free(gressBytes);
    
    return ETHEREUM_BOOLEAN_TRUE; 
}
void ethereumFrameCoderFree(BREthereumFrameCoder coder) {

    BREthereumFrameCoderContext* ctx = (BREthereumFrameCoderContext*)coder;
    free(ctx);
}
void ethereumFrameCoderWrite(BREthereumFrameCoder fCoder, uint8_t msgId,  uint8_t* payload, size_t payloadSize, uint8_t** oBytes, size_t * oBytesSize) {

    BREthereumFrameCoderContext* ctx = (BREthereumFrameCoderContext*) fCoder;

    BRRlpCoder coder = rlpCoderCreate();

    BRRlpItem headerItem;
    
    uint32_t frameSize =  (uint32_t)(sizeof(msgId) + payloadSize);
    
    uint8_t header[4] = {(uint8_t)((frameSize >> 16) & 0xff), (uint8_t)((frameSize >> 8) & 0xff), (uint8_t)(frameSize & 0xff), msgId};
    
    headerItem = rlpEncodeItemBytes(coder, header, sizeof(header));
    
    BRRlpData headerData;

    rlpDataExtract(coder, headerItem, &headerData.bytes, &headerData.bytesCount);

    _writeFrame(ctx, &headerData, payload, payloadSize, oBytes, oBytesSize);
    
    rlpCoderRelease(coder);
}
BREthereumBoolean ethereumFrameCoderDecryptHeader(BREthereumFrameCoder fCoder, uint8_t * oBytes, size_t outSize) {

    BREthereumFrameCoderContext* ctx = (BREthereumFrameCoderContext*) fCoder;

    if(outSize != UINT256_SIZE) {
        return ETHEREUM_BOOLEAN_FALSE;
    }
    
//    _updateMac(ctx, &ctx->ingressMac, oBytes, 16);
    
    UInt128 expected;
    _ingressDigest(ctx,&expected);
    
    if(memcmp(&oBytes[outSize], expected.u8, 16)) {
        return ETHEREUM_BOOLEAN_FALSE;
    }

//    aes256_encrypt(&ctx->frameEncrypt, 16, oBytes, oBytes);
    
    return ETHEREUM_BOOLEAN_TRUE;
}
BREthereumBoolean ethereumFrameCoderDecryptFrame(BREthereumFrameCoder fCoder, uint8_t * oBytes, size_t outSize) {

    BREthereumFrameCoderContext* ctx = (BREthereumFrameCoderContext*) fCoder;
    size_t cipherLen = outSize - 16;
    uint8_t cipher[cipherLen];
 //   sha3_256_update(&ctx->ingressMac, cipherLen, cipher);
 //   _updateMac(ctx, &ctx->ingressMac, NULL, 0);

    UInt128 expected;
    _ingressDigest(ctx,&expected);
    
    if(memcmp(&oBytes[cipherLen], expected.u8, 16)) {
        return ETHEREUM_BOOLEAN_FALSE;
    }
    
//    aes256_encrypt(&ctx->frameEncrypt, outSize, oBytes, oBytes);
    
    return ETHEREUM_BOOLEAN_TRUE;
}
