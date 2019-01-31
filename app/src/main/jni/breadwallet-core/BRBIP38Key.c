//
//  BRBIP38Key.c
//
//  Created by Aaron Voisine on 9/7/15.
//  Copyright (c) 2015 breadwallet LLC
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

#include "BRBIP38Key.h"
#include "BRAddress.h"
#include "BRCrypto.h"
#include "BRBase58.h"
#include "BRInt.h"
#include <stdlib.h>
#include <string.h>
#include <assert.h>

#define BIP38_NOEC_PREFIX      0x0142
#define BIP38_EC_PREFIX        0x0143
#define BIP38_NOEC_FLAG        (0x80 | 0x40)
#define BIP38_COMPRESSED_FLAG  0x20
#define BIP38_LOTSEQUENCE_FLAG 0x04
#define BIP38_INVALID_FLAG     (0x10 | 0x08 | 0x02 | 0x01)
#define BIP38_SCRYPT_N         16384
#define BIP38_SCRYPT_R         8
#define BIP38_SCRYPT_P         8
#define BIP38_SCRYPT_EC_N      1024
#define BIP38_SCRYPT_EC_R      1
#define BIP38_SCRYPT_EC_P      1

// BIP38 is a method for encrypting private keys with a passphrase
// https://github.com/bitcoin/bips/blob/master/bip-0038.mediawiki

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

static const uint8_t sboxi[256] = {
    0x52, 0x09, 0x6a, 0xd5, 0x30, 0x36, 0xa5, 0x38, 0xbf, 0x40, 0xa3, 0x9e, 0x81, 0xf3, 0xd7, 0xfb,
    0x7c, 0xe3, 0x39, 0x82, 0x9b, 0x2f, 0xff, 0x87, 0x34, 0x8e, 0x43, 0x44, 0xc4, 0xde, 0xe9, 0xcb,
    0x54, 0x7b, 0x94, 0x32, 0xa6, 0xc2, 0x23, 0x3d, 0xee, 0x4c, 0x95, 0x0b, 0x42, 0xfa, 0xc3, 0x4e,
    0x08, 0x2e, 0xa1, 0x66, 0x28, 0xd9, 0x24, 0xb2, 0x76, 0x5b, 0xa2, 0x49, 0x6d, 0x8b, 0xd1, 0x25,
    0x72, 0xf8, 0xf6, 0x64, 0x86, 0x68, 0x98, 0x16, 0xd4, 0xa4, 0x5c, 0xcc, 0x5d, 0x65, 0xb6, 0x92,
    0x6c, 0x70, 0x48, 0x50, 0xfd, 0xed, 0xb9, 0xda, 0x5e, 0x15, 0x46, 0x57, 0xa7, 0x8d, 0x9d, 0x84,
    0x90, 0xd8, 0xab, 0x00, 0x8c, 0xbc, 0xd3, 0x0a, 0xf7, 0xe4, 0x58, 0x05, 0xb8, 0xb3, 0x45, 0x06,
    0xd0, 0x2c, 0x1e, 0x8f, 0xca, 0x3f, 0x0f, 0x02, 0xc1, 0xaf, 0xbd, 0x03, 0x01, 0x13, 0x8a, 0x6b,
    0x3a, 0x91, 0x11, 0x41, 0x4f, 0x67, 0xdc, 0xea, 0x97, 0xf2, 0xcf, 0xce, 0xf0, 0xb4, 0xe6, 0x73,
    0x96, 0xac, 0x74, 0x22, 0xe7, 0xad, 0x35, 0x85, 0xe2, 0xf9, 0x37, 0xe8, 0x1c, 0x75, 0xdf, 0x6e,
    0x47, 0xf1, 0x1a, 0x71, 0x1d, 0x29, 0xc5, 0x89, 0x6f, 0xb7, 0x62, 0x0e, 0xaa, 0x18, 0xbe, 0x1b,
    0xfc, 0x56, 0x3e, 0x4b, 0xc6, 0xd2, 0x79, 0x20, 0x9a, 0xdb, 0xc0, 0xfe, 0x78, 0xcd, 0x5a, 0xf4,
    0x1f, 0xdd, 0xa8, 0x33, 0x88, 0x07, 0xc7, 0x31, 0xb1, 0x12, 0x10, 0x59, 0x27, 0x80, 0xec, 0x5f,
    0x60, 0x51, 0x7f, 0xa9, 0x19, 0xb5, 0x4a, 0x0d, 0x2d, 0xe5, 0x7a, 0x9f, 0x93, 0xc9, 0x9c, 0xef,
    0xa0, 0xe0, 0x3b, 0x4d, 0xae, 0x2a, 0xf5, 0xb0, 0xc8, 0xeb, 0xbb, 0x3c, 0x83, 0x53, 0x99, 0x61,
    0x17, 0x2b, 0x04, 0x7e, 0xba, 0x77, 0xd6, 0x26, 0xe1, 0x69, 0x14, 0x63, 0x55, 0x21, 0x0c, 0x7d
};

#define xt(x) (((x) << 1) ^ ((((x) >> 7) & 1)*0x1b))

static void _BRAES256ECBEncrypt(const void *key32, void *buf16)
{
    size_t i, j;
    uint32_t key[32/4], buf[16/4];
    uint8_t *x = (uint8_t *)buf, *k = (uint8_t *)key, r = 1, a, b, c, d, e;
    
    memcpy(key, key32, sizeof(key));
    memcpy(buf, buf16, sizeof(buf));
    
    for (i = 0; i < 14; i++) {
        for (j = 0; j < 4; j++) buf[j] ^= key[j + (i & 1)*4]; // add round key
        
        for (j = 0; j < 16; j++) x[j] = sbox[x[j]]; // sub bytes
        
        // shift rows
        a = x[1], x[1] = x[5], x[5] = x[9], x[9] = x[13], x[13] = a, a = x[10], x[10] = x[2], x[2] = a;
        a = x[3], x[3] = x[15], x[15] = x[11], x[11] = x[7], x[7] = a, a = x[14], x[14] = x[6], x[6] = a;
        
        for (j = 0; i < 13 && j < 16; j += 4) { // mix columns
            a = x[j], b = x[j + 1], c = x[j + 2], d = x[j + 3], e = a ^ b ^ c ^ d;
            x[j] ^= e ^ xt(a ^ b), x[j + 1] ^= e ^ xt(b ^ c), x[j + 2] ^= e ^ xt(c ^ d), x[j + 3] ^= e ^ xt(d ^ a);
        }
        
        if ((i % 2) != 0) { // expand key
            k[0] ^= sbox[k[29]] ^ r, k[1] ^= sbox[k[30]], k[2] ^= sbox[k[31]], k[3] ^= sbox[k[28]], r = xt(r);
            for (j = 4; j < 16; j++) k[j] ^= k[j - 4];
            k[16] ^= sbox[k[12]], k[17] ^= sbox[k[13]], k[18] ^= sbox[k[14]], k[19] ^= sbox[k[15]];
            for (j = 20; j < 32; j++) k[j] ^= k[j - 4];
        }
    }
    
    var_clean(&r, &a, &b, &c, &d, &e);
    for (i = 0; i < 4; i++) buf[i] ^= key[i]; // final add round key
    mem_clean(key, sizeof(key));
    memcpy(buf16, buf, sizeof(buf));
    mem_clean(buf, sizeof(buf));
}

static void _BRAES256ECBDecrypt(const void *key32, void *buf16)
{
    size_t i, j;
    uint32_t key[32/4], buf[16/4];
    uint8_t *x = (uint8_t *)buf, *k = (uint8_t *)key, r = 1, a, b, c, d, e, f, g, h;
    
    memcpy(key, key32, sizeof(key));
    memcpy(buf, buf16, sizeof(buf));
    
    for (i = 0; i < 7; i++) { // expand key
        k[0] ^= sbox[k[29]] ^ r, k[1] ^= sbox[k[30]], k[2] ^= sbox[k[31]], k[3] ^= sbox[k[28]], r = xt(r);
        for (j = 4; j < 16; j++) k[j] ^= k[j - 4];
        k[16] ^= sbox[k[12]], k[17] ^= sbox[k[13]], k[18] ^= sbox[k[14]], k[19] ^= sbox[k[15]];
        for (j = 20; j < 32; j++) k[j] ^= k[j - 4];
    }
    
    for (i = 0; i < 14; i++) {
        for (j = 0; j < 4; j++) buf[j] ^= key[j + (i & 1)*4]; // add round key
        
        for (j = 0; i > 0 && j < 16; j += 4) { // unmix columns
            a = x[j], b = x[j + 1], c = x[j + 2], d = x[j + 3], e = a ^ b ^ c ^ d;
            h = xt(e), f = e ^ xt(xt(h ^ a ^ c)), g = e ^ xt(xt(h ^ b ^ d));
            x[j] ^= f ^ xt(a ^ b), x[j + 1] ^= g ^ xt(b ^ c), x[j + 2] ^= f ^ xt(c ^ d), x[j + 3] ^= g ^ xt(d ^ a);
        }
        
        // unshift rows
        a = x[1], x[1] = x[13], x[13] = x[9], x[9] = x[5], x[5] = a, a = x[2], x[2] = x[10], x[10] = a;
        a = x[3], x[3] = x[7], x[7] = x[11], x[11] = x[15], x[15] = a, a = x[6], x[6] = x[14], x[14] = a;
        
        for (j = 0; j < 16; j++) x[j] = sboxi[x[j]]; // unsub bytes
        
        if ((i % 2) == 0) { // unexpand key
            for (j = 28; j > 16; j--) k[j + 3] ^= k[j - 1];
            k[16] ^= sbox[k[12]], k[17] ^= sbox[k[13]], k[18] ^= sbox[k[14]], k[19] ^= sbox[k[15]];
            for (j = 12; j > 0; j--) k[j + 3] ^= k[j - 1];
            r = (r >> 1) ^ ((r & 1)*0x8d);
            k[0] ^= sbox[k[29]] ^ r, k[1] ^= sbox[k[30]], k[2] ^= sbox[k[31]], k[3] ^= sbox[k[28]];
        }
    }
    
    var_clean(&r, &a, &b, &c, &d, &e, &f, &g, &h);
    for (i = 0; i < 4; i++) buf[i] ^= key[i]; // final add round key
    mem_clean(key, sizeof(key));
    memcpy(buf16, buf, sizeof(buf));
    mem_clean(buf, sizeof(buf));
}

static UInt256 _BRBIP38DerivePassfactor(uint8_t flag, const uint8_t *entropy, const char *passphrase)
{
    size_t len = strlen(passphrase);
    UInt256 prefactor, passfactor;
    
    BRScrypt(&prefactor, sizeof(prefactor), passphrase, len, entropy, (flag & BIP38_LOTSEQUENCE_FLAG) ? 4 : 8,
             BIP38_SCRYPT_N, BIP38_SCRYPT_R, BIP38_SCRYPT_P);
    
    if (flag & BIP38_LOTSEQUENCE_FLAG) { // passfactor = SHA256(SHA256(prefactor + entropy))
        uint8_t d[sizeof(prefactor) + sizeof(uint64_t)];

        memcpy(d, &prefactor, sizeof(prefactor));
        memcpy(&d[sizeof(prefactor)], entropy, sizeof(uint64_t));
        BRSHA256_2(&passfactor, d, sizeof(d));
        mem_clean(d, sizeof(d));
    }
    else passfactor = prefactor;
    
    var_clean(&len);
    var_clean(&prefactor);
    return passfactor;
}

static UInt512 _BRBIP38DeriveKey(BRECPoint passpoint, const uint8_t *addresshash, const uint8_t *entropy)
{
    UInt512 dk;
    uint8_t salt[sizeof(uint32_t) + sizeof(uint64_t)];
    
    memcpy(salt, addresshash, sizeof(uint32_t));
    memcpy(&salt[sizeof(uint32_t)], entropy, sizeof(uint64_t)); // salt = addresshash + entropy
    BRScrypt(&dk, sizeof(dk), &passpoint, sizeof(passpoint), salt, sizeof(salt), BIP38_SCRYPT_EC_N, BIP38_SCRYPT_EC_R,
             BIP38_SCRYPT_EC_P);
    mem_clean(salt, sizeof(salt));
    return dk;
}

int BRBIP38KeyIsValid(const char *bip38Key)
{
    uint8_t data[39];
    
    assert(bip38Key != NULL);
    
    if (BRBase58CheckDecode(data, sizeof(data), bip38Key) != 39) return 0; // invalid length
    
    uint16_t prefix = UInt16GetBE(data);
    uint8_t flag = data[2];
    
    if (prefix == BIP38_NOEC_PREFIX) { // non EC multiplied key
        return ((flag & BIP38_NOEC_FLAG) == BIP38_NOEC_FLAG && (flag & BIP38_LOTSEQUENCE_FLAG) == 0 &&
                (flag & BIP38_INVALID_FLAG) == 0);
    }
    else if (prefix == BIP38_EC_PREFIX) { // EC multiplied key
        return ((flag & BIP38_NOEC_FLAG) == 0 && (flag & BIP38_INVALID_FLAG) == 0);
    }
    else return 0; // invalid prefix
}

// decrypts a BIP38 key using the given passphrase and returns false if passphrase is incorrect
// passphrase must be unicode NFC normalized: http://www.unicode.org/reports/tr15/#Norm_Forms
int BRKeySetBIP38Key(BRKey *key, const char *bip38Key, const char *passphrase)
{
    int r = 1;
    uint8_t data[39];
    
    assert(key != NULL);
    assert(bip38Key != NULL);
    assert(passphrase != NULL);
    
    if (BRBase58CheckDecode(data, sizeof(data), bip38Key) != 39) return 0; // invalid length
    
    uint16_t prefix = UInt16GetBE(data);
    uint8_t flag = data[2];
    const uint8_t *addresshash = &data[3];
    size_t pwLen = strlen(passphrase);
    UInt512 derived;
    UInt256 secret, derived1, derived2, hash;
    BRAddress address = BR_ADDRESS_NONE;

    if (prefix == BIP38_NOEC_PREFIX) { // non EC multiplied key
        // data = prefix + flag + addresshash + encrypted1 + encrypted2
        UInt128 encrypted1 = UInt128Get(&data[7]), encrypted2 = UInt128Get(&data[23]);

        BRScrypt(&derived, sizeof(derived), passphrase, pwLen, addresshash, sizeof(uint32_t),
                 BIP38_SCRYPT_N, BIP38_SCRYPT_R, BIP38_SCRYPT_P);
        derived1 = *(UInt256 *)&derived, derived2 = *(UInt256 *)&derived.u8[sizeof(UInt256)];
        var_clean(&derived);
        
        _BRAES256ECBDecrypt(&derived2, &encrypted1);
        secret.u64[0] = encrypted1.u64[0] ^ derived1.u64[0];
        secret.u64[1] = encrypted1.u64[1] ^ derived1.u64[1];
        
        _BRAES256ECBDecrypt(&derived2, &encrypted2);
        secret.u64[2] = encrypted2.u64[0] ^ derived1.u64[2];
        secret.u64[3] = encrypted2.u64[1] ^ derived1.u64[3];
        var_clean(&derived1, &derived2);
        var_clean(&encrypted1, &encrypted2);
    }
    else if (prefix == BIP38_EC_PREFIX) { // EC multipled key
        // data = prefix + flag + addresshash + entropy + encrypted1[0...7] + encrypted2
        const uint8_t *entropy = &data[7];
        UInt128 encrypted1 = UINT128_ZERO, encrypted2 = UInt128Get(&data[23]);
        UInt256 passfactor = _BRBIP38DerivePassfactor(flag, entropy, passphrase), factorb;
        BRECPoint passpoint;
        uint64_t seedb[3];
        
        BRSecp256k1PointGen(&passpoint, &passfactor); // passpoint = G*passfactor
        derived = _BRBIP38DeriveKey(passpoint, addresshash, entropy);
        var_clean(&passpoint);
        derived1 = *(UInt256 *)&derived, derived2 = *(UInt256 *)&derived.u8[sizeof(UInt256)];
        var_clean(&derived);
        memcpy(&encrypted1, &data[15], sizeof(uint64_t));

        // encrypted2 = (encrypted1[8...15] + seedb[16...23]) xor derived1[16...31]
        _BRAES256ECBDecrypt(&derived2, &encrypted2);
        encrypted1.u64[1] = encrypted2.u64[0] ^ derived1.u64[2];
        seedb[2] = encrypted2.u64[1] ^ derived1.u64[3];

        // encrypted1 = seedb[0...15] xor derived1[0...15]
        _BRAES256ECBDecrypt(&derived2, &encrypted1);
        seedb[0] = encrypted1.u64[0] ^ derived1.u64[0];
        seedb[1] = encrypted1.u64[1] ^ derived1.u64[1];
        var_clean(&derived1, &derived2);
        var_clean(&encrypted1, &encrypted2);
        
        BRSHA256_2(&factorb, seedb, sizeof(seedb)); // factorb = SHA256(SHA256(seedb))
        mem_clean(seedb, sizeof(seedb));
        secret = passfactor;
        BRSecp256k1ModMul(&secret, &factorb); // secret = passfactor*factorb mod N
        var_clean(&passfactor, &factorb);
    }
    
    BRKeySetSecret(key, &secret, flag & BIP38_COMPRESSED_FLAG);
    var_clean(&secret);
    BRKeyAddress(key, address.s, sizeof(address));
    BRSHA256_2(&hash, address.s, strlen(address.s));
    if (! address.s[0] || memcmp(&hash, addresshash, sizeof(uint32_t)) != 0) r = 0;
    return r;
}

// generates an "intermediate code" for an EC multiply mode key
// salt should be 64bits of random data
// passphrase must be unicode NFC normalized
// returns number of bytes written to code including NULL terminator, or total codeLen needed if code is NULL
size_t BRKeyBIP38ItermediateCode(char *code, size_t codeLen, uint64_t salt, const char *passphrase)
{
    // TODO: XXX implement
    return 0;
}

// generates an "intermediate code" for an EC multiply mode key with a lot and sequence number
// lot must be less than 1048576, sequence must be less than 4096, and salt should be 32bits of random data
// passphrase must be unicode NFC normalized
// returns number of bytes written to code including NULL terminator, or total codeLen needed if code is NULL
size_t BRKeyBIP38ItermediateCodeLS(char *code, size_t codeLen, uint32_t lot, uint16_t sequence, uint32_t salt,
                                   const char *passphrase)
{
    // TODO: XXX implement
    return 0;
}

// generates a BIP38 key from an "intermediate code" and 24 bytes of cryptographically random data (seedb)
// compressed indicates if compressed pubKey format should be used for the bitcoin address
void BRKeySetBIP38ItermediateCode(BRKey *key, const char *code, const uint8_t *seedb, int compressed)
{
    // TODO: XXX implement
}

// encrypts key with passphrase
// passphrase must be unicode NFC normalized
// returns number of bytes written to bip38Key including NULL terminator or total bip38KeyLen needed if bip38Key is NULL
size_t BRKeyBIP38Key(BRKey *key, char *bip38Key, size_t bip38KeyLen, const char *passphrase)
{
    uint16_t prefix = BIP38_NOEC_PREFIX;
    uint8_t buf[39], flag = BIP38_NOEC_FLAG;
    uint32_t salt;
    size_t off = 0;
    BRAddress address;
    UInt512 derived;
    UInt256 hash, derived1, derived2;
    UInt128 encrypted1, encrypted2;
    
    if (! bip38Key) return 43*138/100 + 2; // 43bytes*log(256)/log(58), rounded up, plus NULL terminator

    assert(key != NULL && BRKeyPrivKey(key, NULL, 0) > 0);
    assert(passphrase != NULL);
   
    if (key->compressed) flag |= BIP38_COMPRESSED_FLAG;
    BRKeyAddress(key, address.s, sizeof(address));
    BRSHA256_2(&hash, address.s, strlen(address.s));
    salt = hash.u32[0];

    BRScrypt(&derived, sizeof(derived), passphrase, strlen(passphrase), &salt, sizeof(salt),
             BIP38_SCRYPT_N, BIP38_SCRYPT_R, BIP38_SCRYPT_P);
    derived1 = *(UInt256 *)&derived, derived2 = *(UInt256 *)&derived.u8[sizeof(UInt256)];
    var_clean(&derived);
    
    // enctryped1 = AES256Encrypt(privkey[0...15] xor derived1[0...15], derived2)
    encrypted1.u64[0] = key->secret.u64[0] ^ derived1.u64[0];
    encrypted1.u64[1] = key->secret.u64[1] ^ derived1.u64[1];
    _BRAES256ECBEncrypt(&derived2, &encrypted1);

    // encrypted2 = AES256Encrypt(privkey[16...31] xor derived1[16...31], derived2)
    encrypted2.u64[0] = key->secret.u64[2] ^ derived1.u64[2];
    encrypted2.u64[1] = key->secret.u64[3] ^ derived1.u64[3];
    _BRAES256ECBEncrypt(&derived2, &encrypted2);
    
    UInt16SetBE(&buf[off], prefix);
    off += sizeof(prefix);
    buf[off] = flag;
    off += sizeof(flag);
    UInt32SetBE(&buf[off], UInt32GetBE(&salt));
    off += sizeof(salt);
    UInt128Set(&buf[off], encrypted1);
    off += sizeof(encrypted1);
    UInt128Set(&buf[off], encrypted2);
    off += sizeof(encrypted2);
    return BRBase58CheckEncode(bip38Key, bip38KeyLen, buf, off);
}
