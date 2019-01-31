//
//  BREthereumLESBase.c
//  breadwallet-core Ethereum
//
//  Created by Lamont Samuels on 4/24/18.
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
#include <inttypes.h>
#include <stdlib.h>
#include <stdarg.h>
#include <assert.h>
#include "BRKey.h"
#include "secp256k1.h"
#include "secp256k1_ecdh.h"
#include "secp256k1/src/hash.h"
#include "BREthereumLESBase.h"
#include "BREthereumBase.h"
#include "BRCrypto.h"


#define PRI_KEY_BYTES_SIZE 32
#define PUB_KEY_BYTES_SIZE 65

static secp256k1_context *_ctx = NULL;
static pthread_once_t _ctx_once = PTHREAD_ONCE_INIT;

static void _ctx_init()
{
    _ctx = secp256k1_context_create(SECP256K1_CONTEXT_SIGN | SECP256K1_CONTEXT_VERIFY);
}

/**
 * Determines a uniform number between the given ranges
 * @cite: https://stackoverflow.com/questions/11641629/generating-a-uniform-distribution-of-integers-in-c
 * TODO: We need to use a crypto sercure rand function instead of rand()
 */
uint8_t _randomSecure(int rangeLow, int rangeHigh) {
    int range = rangeHigh - rangeLow + 1;
    int secureMax = RAND_MAX - RAND_MAX % range;
    int x;
    do x = rand(); while (x >= secureMax);
    return rangeLow + x / (secureMax / range);
}

UInt256 ethereumGetNonce(void){

    return UINT256_ZERO; 
}
BREthereumBoolean ethereumGenRandomPriKey(BRKey ** key) {
    
    assert(key != NULL);
    
    uint8_t data [PRI_KEY_BYTES_SIZE];
    
    for(int i = 0; i < PRI_KEY_BYTES_SIZE; ++i)
    {
        data[i] = _randomSecure(0, 255);
    }
    
    BRKey * newKey = (BRKey *)calloc(1, sizeof(BRKey));
    assert(newKey != NULL);

    pthread_once(&_ctx_once, _ctx_init);
    
    // Set the Private Key
    memcpy(newKey->secret.u8, data, PRI_KEY_BYTES_SIZE);
    newKey->compressed = 0;
    
    if(!secp256k1_ec_seckey_verify(_ctx, newKey->secret.u8)) {
        return ETHEREUM_BOOLEAN_FALSE;
    }
    
    // Set the Public Key
    secp256k1_pubkey pk;

    if (!secp256k1_ec_pubkey_create(_ctx, &pk, newKey->secret.u8))
    {
        return ETHEREUM_BOOLEAN_FALSE;
    }
    
    uint8_t sPubKey [PUB_KEY_BYTES_SIZE];
    
    size_t sPubkeySize = PUB_KEY_BYTES_SIZE;

    
    secp256k1_ec_pubkey_serialize(
            _ctx, sPubKey, &sPubkeySize,
            &pk, SECP256K1_EC_UNCOMPRESSED
    );
    
    if(sPubkeySize != PUB_KEY_BYTES_SIZE || sPubKey[0]  != 0x04){
        return ETHEREUM_BOOLEAN_FALSE;
    }
    
    memcpy(newKey->pubKey, &sPubKey[1], PUB_KEY_BYTES_SIZE);
    
    *key = newKey;
    
    return ETHEREUM_BOOLEAN_TRUE;
}
void _kdf(UInt256* priKey, uint8_t* bytes, size_t len, uint8_t * dst) {
    /**
     * KDF implementation
     * @cite: modification from the cpp-ethereum project defined in /devcrypto/Common.cpp (bytes ecies::kdf)
  
    */
}
void ethereumXORBytes(uint8_t * op1, uint8_t* op2, uint8_t* result, size_t len) {
    for (unsigned int i = 0; i < len;  ++i) {
        result[i] = op1[i] ^ op2[i];
    }

}

BREthereumBoolean etheruemECDHAgree(BRKey* key, UInt512* pubKey, UInt256* outSecret) {
   /* secp256k1_context* ctx = _ctx;
    secp256k1_pubkey rawPubKey;
    
    unsigned char compressedPoint[33];
    unsigned char serialPubKey[65];
    serialPubKey[0] = 0x04;
    
    memcpy(&serialPubKey[1], pubKey->u8, sizeof(pubKey->u8));
    if (!secp256k1_ec_pubkey_parse(ctx, &rawPubKey, serialPubKey, sizeof(serialPubKey)))
        return ETHEREUM_BOOLEAN_FALSE;  // Invalid public key.

    if (!secp256k1_ecdh(ctx, compressedPoint, &rawPubKey, (unsigned char*)key->secret.u8))
        return ETHEREUM_BOOLEAN_FALSE;  // Invalid secret key.
    
    memcpy(outSecret->u8, &compressedPoint[1], sizeof(outSecret->u8));
    secp256k1_context_destroy(ctx);
    
    return ETHEREUM_BOOLEAN_TRUE;
    */
    return ETHEREUM_BOOLEAN_TRUE;
}
BREthereumBoolean ethereumEncryptECIES(UInt512* pubKey, uint8_t * plain, uint8_t * cipher, size_t len){
   
    /**
     *  Algorithm is
     * Encrypt data with ECIES method to the given public key
    1) generate r = random value
    2) generate shared-secret = kdf( ecdhAgree(r, P) )
    3) generate R = rG [same op as generating a public key]
    4) 0x04 || R || AsymmetricEncrypt(shared-secret, plaintext) || tag
    **/
    


    memcpy(cipher, plain, len);
    
    return ETHEREUM_BOOLEAN_TRUE; 
    
}
BREthereumBoolean ethereumDecryptECIES(UInt256* priKey, uint8_t * plain, uint8_t * cipher, size_t len)
{
    //TODO: Implement decrypt ECIES
    
    /** Decrypt data with ECIES method using the given private key
    1) generate shared-secret = kdf( ecdhAgree(myPrivKey, msg[1:65]) )
    2) verify tag
    3) decrypt
    ecdhAgree(r, recipientPublic) == ecdhAgree(recipientPrivate, R)
    [where R = r*G, and recipientPublic = recipientPrivate*G]
    **/
    memcpy(cipher, plain, len);
    return ETHEREUM_BOOLEAN_TRUE;
}
