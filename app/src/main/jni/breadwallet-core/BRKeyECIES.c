//
//  BRKeyECIES.c
//
//  Created by Aaron Voisine on 4/30/18.
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

#include "BRKeyECIES.h"
#include "BRCrypto.h"
#include <stdlib.h>
#include <string.h>
#include <assert.h>

// ecies-aes128-sha256 as specified in SEC 1, 5.1: http://www.secg.org/SEC1-Ver-1.0.pdf
// NOTE: these are not implemented using constant time algorithms

static void _BRECDH(void *out32, const BRKey *privKey, BRKey *pubKey)
{
    uint8_t p[65];
    size_t pLen = BRKeyPubKey(pubKey, p, sizeof(p));
    
    if (pLen == 65) p[0] = (p[64] % 2) ? 0x03 : 0x02; // convert to compressed pubkey format
    BRSecp256k1PointMul((BRECPoint *)p, &privKey->secret); // calculate shared secret ec-point
    memcpy(out32, &p[1], 32); // unpack the x coordinate
    mem_clean(p, sizeof(p));
}

#define xt(x) (((x) << 1) ^ ((((x) >> 7) & 1)*0x1b))

static void _BRAES128CTR(void *out, const void *key16, const void *iv16, const void *data, size_t dataLen)
{
    static const uint8_t sbox[256] = {
        0x63, 0x7c, 0x77, 0x7b, 0xf2, 0x6b, 0x6f, 0xc5, 0x30, 0x01, 0x67, 0x2b, 0xfe, 0xd7, 0xab, 0x76,
        0xca, 0x82, 0xc9, 0x7d, 0xfa, 0x59, 0x47, 0xf0, 0xad, 0xd4, 0xa2, 0xaf, 0x9c, 0xa4, 0x72, 0xc0,
        0xb7, 0xfd, 0x93, 0x26, 0x36, 0x3f, 0xf7, 0xcc, 0x34, 0xa5, 0xe5, 0xf1, 0x71, 0xd8, 0x31, 0x15,
        0x04, 0xc7, 0x23, 0xc3, 0x18, 0x96, 0x05, 0x9a, 0x07, 0x12, 0x80, 0xe2, 0xeb, 0x27, 0xb2, 0x75,
        0x09, 0x83, 0x2c, 0x1a, 0x1b, 0x6e, 0x5a, 0xa0, 0x52, 0x3b, 0xd6, 0xb3, 0x29, 0xe3, 0x2f, 0x84,
        0x53, 0xd1, 0x00, 0xed, 0x20, 0xfc, 0xb1, 0x5b, 0x6a, 0xcb, 0xbe, 0x39, 0x4a, 0x4c, 0x58, 0xcf,
        0xd0, 0xef, 0xaa, 0xfb, 0x43, 0x4d, 0x33, 0x85, 0x45, 0xf9, 0x02, 0x7f, 0x50, 0x3c, 0x9f, 0xa8,
        0x51, 0xa3, 0x40, 0x8f, 0x92, 0x9d, 0x38, 0xf5, 0xbc, 0xb6, 0xda, 0x21, 0x10, 0xff, 0xf3, 0xd2,
        0xcd, 0x0c, 0x13, 0xec, 0x5f, 0x97, 0x44, 0x17, 0xc4, 0xa7, 0x7e, 0x3d, 0x64, 0x5d, 0x19, 0x73,
        0x60, 0x81, 0x4f, 0xdc, 0x22, 0x2a, 0x90, 0x88, 0x46, 0xee, 0xb8, 0x14, 0xde, 0x5e, 0x0b, 0xdb,
        0xe0, 0x32, 0x3a, 0x0a, 0x49, 0x06, 0x24, 0x5c, 0xc2, 0xd3, 0xac, 0x62, 0x91, 0x95, 0xe4, 0x79,
        0xe7, 0xc8, 0x37, 0x6d, 0x8d, 0xd5, 0x4e, 0xa9, 0x6c, 0x56, 0xf4, 0xea, 0x65, 0x7a, 0xae, 0x08,
        0xba, 0x78, 0x25, 0x2e, 0x1c, 0xa6, 0xb4, 0xc6, 0xe8, 0xdd, 0x74, 0x1f, 0x4b, 0xbd, 0x8b, 0x8a,
        0x70, 0x3e, 0xb5, 0x66, 0x48, 0x03, 0xf6, 0x0e, 0x61, 0x35, 0x57, 0xb9, 0x86, 0xc1, 0x1d, 0x9e,
        0xe1, 0xf8, 0x98, 0x11, 0x69, 0xd9, 0x8e, 0x94, 0x9b, 0x1e, 0x87, 0xe9, 0xce, 0x55, 0x28, 0xdf,
        0x8c, 0xa1, 0x89, 0x0d, 0xbf, 0xe6, 0x42, 0x68, 0x41, 0x99, 0x2d, 0x0f, 0xb0, 0x54, 0xbb, 0x16
    };
    
    uint8_t x[16], iv[16], k[176], a, b, c, d, e, r = 1;
    size_t off, i, j;
    
    memcpy(iv, iv16, 16);
    memcpy(k, key16, 16);
    
    for (i = 0; i <= 144; i += 16) { // expand key
        k[i + 16] = k[i] ^ sbox[k[i + 13]] ^ r, k[i + 17] = k[i + 1] ^ sbox[k[i + 14]];
        k[i + 18] = k[i + 2] ^ sbox[k[i + 15]], k[i + 19] = k[i + 3] ^ sbox[k[i + 12]], r = xt(r);
        for (j = i + 4; j < i + 16; j++) k[j + 16] = k[j] ^ k[j + 12];
    }

    for (off = 0; off < dataLen; off++) {
        if ((off % 16) == 0) { // generate xor compliment
            memcpy(x, iv, 16);

            for (i = 0; i < 10; i++) {
                for (j = 0; j < 16; j++) x[j] ^= k[i*16 + j]; // add round key
                
                for (j = 0; j < 16; j++) x[j] = sbox[x[j]]; // sub bytes
                
                // shift rows
                a = x[1], x[1] = x[5], x[5] = x[9], x[9] = x[13], x[13] = a, a = x[10], x[10] = x[2], x[2] = a;
                a = x[3], x[3] = x[15], x[15] = x[11], x[11] = x[7], x[7] = a, a = x[14], x[14] = x[6], x[6] = a;
                
                for (j = 0; i < 9 && j < 16; j += 4) { // mix columns
                    a = x[j], b = x[j + 1], c = x[j + 2], d = x[j + 3], e = a ^ b ^ c ^ d, x[j] ^= e ^ xt(a ^ b);
                    x[j + 1] ^= e ^ xt(b ^ c), x[j + 2] ^= e ^ xt(c ^ d), x[j + 3] ^= e ^ xt(d ^ a);
                }
            }
            
            for (j = 0; j < 16; j++) x[j] ^= k[i*16 + j]; // final add round key

            j = 16;
            do { iv[--j]++; } while (iv[j] == 0 && j > 0); // increment iv with overflow
        }
        
        ((uint8_t *)out)[off] = (((uint8_t *)data)[off] ^ x[off % 16]);
    }
    
    var_clean(&r, &a, &b, &c, &d, &e);
    mem_clean(k, sizeof(k));
    mem_clean(x, sizeof(x));
}

size_t BRKeyECIESAES128SHA256Encrypt(BRKey *pubKey, void *out, size_t outLen, BRKey *ephemKey,
                                     const void *data, size_t dataLen)
{
    uint8_t *encKey, macKey[32], shared[32], iv[16], K[32], V[32], buf[36] = { 0, 0, 0, 1 };
    size_t pkLen = ephemKey ? BRKeyPubKey(ephemKey, NULL, 0) : 0;
    
    assert(pkLen > 0);
    if (! out) return pkLen + sizeof(iv) + dataLen + 32;
    if (outLen < pkLen + sizeof(iv) + dataLen + 32) return 0;
    assert(pubKey != NULL && BRKeyPubKey(pubKey, NULL, 0) > 0);
    assert(data != NULL || dataLen == 0);

    // shared-secret = kdf(ecdh(ephemKey, pubKey))
    _BRECDH(&buf[4], ephemKey, pubKey);
    BRSHA256(shared, buf, sizeof(buf));
    mem_clean(buf, sizeof(buf));
    encKey = shared;
    BRSHA256(macKey, &shared[16], 16);
    
    // R = rG
    BRKeyPubKey(ephemKey, out, pkLen);

    // encrypt
    BRSHA256(buf, data, dataLen);
    BRHMACDRBG(iv, sizeof(iv), K, V, BRSHA256, 32, encKey, 16, buf, 32, NULL, 0);
    memcpy(&out[pkLen], iv, sizeof(iv));
    _BRAES128CTR(&out[pkLen + sizeof(iv)], encKey, iv, data, dataLen);
    mem_clean(shared, sizeof(shared));
    
    // tag with mac
    BRHMAC(&out[pkLen + sizeof(iv) + dataLen], BRSHA256, 32, macKey, 32, &out[pkLen], sizeof(iv) + dataLen);
    mem_clean(macKey, sizeof(macKey));
    return pkLen + sizeof(iv) + dataLen + 32;
}

size_t BRKeyECIESAES128SHA256Decrypt(BRKey *privKey, void *out, size_t outLen, const void *data, size_t dataLen)
{
    uint8_t *encKey, macKey[32], shared[32], mac[32], iv[16], buf[36] = { 0, 0, 0, 1 }, r = 0;
    size_t i, pkLen;
    BRKey pubKey;

    assert(data != NULL || dataLen == 0);
    pkLen = (dataLen > 0 && (((uint8_t *)data)[0] == 0x02 || ((uint8_t *)data)[0] == 0x03)) ? 33 : 65;
    if (dataLen < pkLen + sizeof(iv) + 32) return 0;
    if (BRKeySetPubKey(&pubKey, data, pkLen) == 0) return 0;
    if (! out) return dataLen - (pkLen + sizeof(iv) + 32);
    if (pkLen + sizeof(iv) + outLen + 32 < dataLen) return 0;
    assert(privKey != NULL && BRKeyPrivKey(privKey, NULL, 0) > 0);

    // shared-secret = kdf(ecdh(privKey, pubKey))
    _BRECDH(&buf[4], privKey, &pubKey);
    BRSHA256(shared, buf, sizeof(buf));
    mem_clean(buf, sizeof(buf));
    encKey = shared;
    BRSHA256(macKey, &shared[16], 16);
    
    // verify mac tag
    BRHMAC(mac, BRSHA256, 32, macKey, 32, &data[pkLen], dataLen - (pkLen + 32));
    mem_clean(macKey, sizeof(macKey));
    for (i = 0; i < 32; i++) r |= mac[i] ^ ((uint8_t *)data)[dataLen + i - 32]; // constant time compare
    mem_clean(mac, sizeof(mac));
    if (r != 0) return 0;
    
    // decrypt
    memcpy(iv, &data[pkLen], sizeof(iv));
    _BRAES128CTR(out, encKey, iv, &data[pkLen + sizeof(iv)], dataLen - (pkLen + sizeof(iv) + 32));
    mem_clean(shared, sizeof(shared));
    return dataLen - (pkLen + sizeof(iv) + 32);
}
