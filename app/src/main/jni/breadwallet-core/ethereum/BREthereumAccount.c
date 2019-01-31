//
//  BBREthereumAddress.c
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 2/21/2018.
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
#include <string.h>
#include <assert.h>
#include <ctype.h>
#include <BRKey.h>
#include "BRBIP32Sequence.h"
#include "BRBIP39Mnemonic.h"
#include "BRCrypto.h"
#include "BRBase58.h"
#include "BRBIP39WordsEn.h"

#include "BRUtil.h"
#include "BREthereumAccount.h"

// BIP39 test vectors
// https://github.com/trezor/python-mnemonic/blob/master/vectors.json

// Ethereum
// https://mytokenwallet.com/bip39.html

#define PRIMARY_ADDRESS_BIP44_INDEX 0

/* Forward Declarations */
static BREthereumAddress
accountCreateAddress (BREthereumAccount account, UInt512 seed, uint32_t index);

//
// Locale-Based BIP-39 Word List
//
static const char **sharedWordList;

#define WORD_LIST_LENGTH 2048

extern int
installSharedWordList (const char *wordList[], int wordListLength) {
    if (BIP39_WORDLIST_COUNT != wordListLength)
        return 0;
    
    sharedWordList = wordList;
    
    return 1;
}

//
// Address
//

/**
 * Two address types - explicitly provided or derived from BIP44
 */
typedef enum {
    ADDRESS_PROVIDED,   // target,
    ADDRESS_DERIVED,    // from BIP44
} BREthereumAddressType;

/**
 * An EthereumAddress is as '0x'-prefixed, hex-encoded string with an overall lenght of 42
 * characters.  Addresses can be explicitly provided - such as with a 'send to' addresses; or can
 * be derived using BIP44 scheme - such as with internal addresses.
 */
struct BREthereumAddressRecord {
    
    /**
     * The 'official' ethereum address string for (the external representation of) this
     * BREthereum address.
     *
     * THIS IS NOT A SIMPLE STRING; this is a hex encoded (with encodeHex) string prefixed with
     * "0x".  Generally, when using this string, for example when RLP encoding, one needs to
     * convert back to the byte array (use rlpEncodeItemHexString())
     */
    char string[43];    // '0x' + <40 chars> + '\0'
    
    /**
     * Identify the type of this address record - created with a provided string or
     * with a provided publicKey.
     */
    BREthereumAddressType type;
    
    /**
     * The public key.  This started out as a BIP44 264 bits (65 bytes) array with a value of
     * 0x04 at byte 0; we strip off that first byte and are left with 64.  Go figure.
     */
    uint8_t publicKey [64];  // BIP44: 'Master Public Key 'M' (264 bits) - 8
    
    /**
     * The BIP-44 Index used for this key.
     */
    uint32_t index;
    
    /**
     * The NEXT nonce value
     */
    uint64_t nonce;
};

static struct BREthereumAddressRecord emptyAddressRecord;

extern BREthereumAddress
createAddress (const char *string) {
    if (ETHEREUM_BOOLEAN_IS_FALSE(validateAddressString(string))) return NULL;
    
    BREthereumAddress address = malloc (sizeof (struct BREthereumAddressRecord));
    
    address->type = ADDRESS_PROVIDED;
    address->nonce = 0;
    strncpy (address->string, string, 42);
    address->string[42] = '\0';
    
    return address;
}

extern BREthereumBoolean
validateAddressString(const char *string) {
    return 42 == strlen(string)
    && '0' == string[0]
    && 'x' == string[1]
    && encodeHexValidate (&string[2])
    ? ETHEREUM_BOOLEAN_TRUE
    : ETHEREUM_BOOLEAN_FALSE;
}

extern void
addressFree (BREthereumAddress address) {
    free (address);
}

extern uint64_t
addressGetNonce(BREthereumAddress address) {
    return address->nonce;
}

private_extern void
addressSetNonce(BREthereumAddress address,
                uint64_t nonce,
                BREthereumBoolean force) {
    if (ETHEREUM_BOOLEAN_IS_TRUE(force) || nonce > address->nonce)
        address->nonce = nonce;
}

private_extern uint64_t
addressGetThenIncrementNonce(BREthereumAddress address) {
    return address->nonce++;
}

/**
 * Create an address given a 65 byte publicKey (derived from a BIP-44 public key).
 *
 * Details: publicKey[0] must be '0x04';
 *
 * @param publicKey
 * @return
 */
static BREthereumAddress
createAddressDerived (const uint8_t *publicKey, uint32_t index) {
    BREthereumAddress address = malloc (sizeof (struct BREthereumAddressRecord));
    
    address->type = ADDRESS_DERIVED;  // painfully
    address->nonce = 0;
    address->index = index;
    
    // Seriously???
    //
    // https://kobl.one/blog/create-full-ethereum-keypair-and-address/#derive-the-ethereum-address-from-the-public-key
    //
    // "The public key is what we need in order to derive its Ethereum address. Every EC public key
    // begins with the 0x04 prefix before giving the location of the two point on the curve. You
    // should remove this leading 0x04 byte in order to hash it correctly. ...
    
    assert (publicKey[0] == 0x04);
    
    // Strip off byte 0
    memcpy(address->publicKey, &publicKey[1], sizeof (address->publicKey));
    
    // We interrupt your regularly scheduled programming...
    
    // "Use any method you like to get it in the form of an hexadecimal string
    // "The <pub file> now contains the hexadecimal value of the public key without the 0x04 prefix.
    
    // "An Ethereum address is made of 20 bytes (40 hex characters long), it is commonly
    // represented by adding the 0x prefix. In order to derive it, one should take the keccak-256
    // hash of the hexadecimal form of a public key, then keep only the last 20 bytes (aka get
    // rid of the first 12 bytes).
    //
    // "Simply pass the file containing the public key in hexadecimal format to the keccak-256sum
    // command. Do not forget to use the ‘-x’ option in order to interpret it as hexadecimal and
    // not a simple string.
    //
    // WTF is a 'simple string'.  Seriously??
    
    // Back to our regularly scheduled programming...
    //
    // We'll assume our BRKeccak256 takes an array of bytes (sure, the argument is void*); NOT
    // a hexadecimal format of a 0x04 stripped public key...
    
    // Fill in string
    address->string[0] = '0';
    address->string[1] = 'x';
    
    // Hash the public key (64 bytes, 0x04 prefix axed) and then hex encode the last 20 values
    uint8_t hash[32];
    BRKeccak256(hash, address->publicKey, sizeof (address->publicKey));
    // Offset '2' into address->string and account for the '\0' terminator.
    encodeHex(&address->string[2], 40 + 1, &hash[12], 20);
    
    // And now the 'checksum after thought'
    
    // https://ethereum.stackexchange.com/a/19048/33128
    //
    // Ethereum wallet addresses are in hex [0-9A-F]*. While the address itself is case-insensitive
    // (A is the same as a to the network), the case sensitivity is used as a (optional) checksum.
    // It was built as an after-thought to an addressing scheme that lacked basic checksum
    // validation.  https://github.com/ethereum/EIPs/issues/55#issuecomment-187159063
    //
    // The checksum works like so:
    //
    // 1) lowercase address and remove 0x prefix
    // 2) sha3 hash result from #1
    // 3) change nth letter of address according to the nth letter of the hash:
    //      0,1,2,3,4,5,6,7 → Lowercase
    //      8, 9, a, b, c, d, e, f → Uppercase
    //
    // So, you sha3 hash the address, and look at each Nth character of the sha result. If it's 7
    // or below, the Nth character in the address is lowercase. If it is 8 or above, that character
    // is uppercase.

    // We'll skip it - unless somebody requests it.

    // PaperKey: boring head harsh green empty clip fatal typical found crane dinner timber
    //  Address: 0xa9de3dbd7d561e67527bc1ecb025c59d53b9f7ef
    //   Result: 0xa9de3dbD7d561e67527bC1Ecb025c59D53b9F7Ef
    //
    //        > web3.toChecksumAddress("0xa9de3dbd7d561e67527bc1ecb025c59d53b9f7ef")
    //          "0xa9de3dbD7d561e67527bC1Ecb025c59D53b9F7Ef"
    //
    //        > web3.sha3("a9de3dbd7d561e67527bc1ecb025c59d53b9f7ef")
    //          "0x6540e229f74514b83dd4a29553c029ad7b31c882df256a8c5222802c1b9b78d9"

    // We'll checksum address->string but while avoiding the '0x' prefix
    char *checksumAddr = &address->string[2];
    size_t checksumAddrLen = strlen(checksumAddr);
    assert (checksumAddrLen < 2 * sizeof(hash));

    // Ethereum 'SHA3' is actually Keccak256
    BRKeccak256(hash, checksumAddr, checksumAddrLen);

    for (int i = 0; i < checksumAddrLen; i++) {
        // We should hex-encode the hash and then look character by character.  Instead
        // we'll extract 4 bits as the upper or lower nibble and compare to 8.  This is the
        // same extracting that encodeHex performs, ultimately.
        int value = 0x0f & (hash[i / 2] >> ((0 == i % 2) ? 4 : 0));
        checksumAddr[i] = (value < 8
                           ? (char) tolower(checksumAddr[i])
                           : (char) toupper(checksumAddr[i]));
    }
    return address;
}

extern BREthereumBoolean
addressHasString (BREthereumAddress address,
                  const char *string) {
    return (0 == strcasecmp(string, address->string)
            ? ETHEREUM_BOOLEAN_TRUE
            : ETHEREUM_BOOLEAN_FALSE);
}

extern BREthereumBoolean
addressEqual (BREthereumAddress a1, BREthereumAddress a2) {
    return (0 == strcmp (a1->string, a2->string)
            ? ETHEREUM_BOOLEAN_TRUE
            : ETHEREUM_BOOLEAN_FALSE);
}

extern char *
addressAsString (BREthereumAddress address) {
    return strndup (address->string, 43);
}

extern BRKey // 65 bytes
addressGetPublicKey (BREthereumAddress address) {
    BRKey result;
    BRKeyClean(&result);

    result.pubKey[0] = 0x04;
    memcpy (&result.pubKey[1], address->publicKey, sizeof (address->publicKey));

    return result;
}

#if defined (DEBUG)
extern const char *
addressPublicKeyAsString (BREthereumAddress address, int compressed) {
    // The byte array at address->publicKey has the '04' 'uncompressed' prefix removed.  Thus
    // the value in publicKey is uncompressed and 64 bytes.  As a string, this result will have
    // an 0x0<n> prefix where 'n' is in { 4: uncompressed, 2: compressed even, 3: compressed odd }.
    
    // Default, uncompressed
    char *prefix = "0x04";
    size_t sourceLen = sizeof (address->publicKey);           // 64 bytes: { x y }
    
    if (compressed) {
        sourceLen /= 2;  // use 'x'; skip 'y'
        prefix = (0 == address->publicKey[63] % 2 ? "0x02" : "0x03");
    }
    
    char *result = malloc (4 + 2 * sourceLen + 1);
    strcpy (result, prefix);  // encode properly...
    encodeHex(&result[4], 2 * sourceLen + 1, address->publicKey, sourceLen);
    
    return result;
}
#endif

extern BRRlpItem
addressRlpEncode (BREthereumAddress address, BRRlpCoder coder) {
    return rlpEncodeItemHexString(coder, address->string);
}

extern BREthereumAddress
addressRlpDecode (BRRlpItem item, BRRlpCoder coder) {
    return createAddress(rlpDecodeItemHexString (coder, item, "0x"));
}

//
// Address Raw
//
extern BREthereumAddressRaw
addressRawCreate (const char *address) {
    BREthereumAddressRaw raw;
    decodeHex(raw.bytes, sizeof(raw.bytes), address, strlen(address));
    return raw;
}

extern BREthereumAddressRaw
addressRawRlpDecode (BRRlpItem item, BRRlpCoder coder) {
    BREthereumAddressRaw address;

    BRRlpData data = rlpDecodeItemBytes(coder, item);
    assert (20 == data.bytesCount);

    memcpy (address.bytes, data.bytes, 20);
    return address;
}

extern BRRlpItem
addressRawRlpEncode(BREthereumAddressRaw address,
                    BRRlpCoder coder) {
    return rlpEncodeItemBytes(coder, address.bytes, 20);
}

//
// Account
//
struct BREthereumAccountRecord {
    
    BRMasterPubKey masterPubKey;
    
    /**
     * The primary address for this account - aka address[0].
     */
    BREthereumAddress primaryAddress;
};

static BREthereumAccount
createAccountWithBIP32Seed (UInt512 seed) {
    BREthereumAccount account = (BREthereumAccount) calloc (1, sizeof (struct BREthereumAccountRecord));

    // Assign the key; create the primary address.
    account->masterPubKey = BRBIP32MasterPubKey(&seed, sizeof(seed));
    account->primaryAddress = accountCreateAddress(account, seed, PRIMARY_ADDRESS_BIP44_INDEX);

    return account;

}

extern BREthereumAccount
createAccountWithPublicKey (const BRKey key) { // 65 bytes, 0x04-prefixed, uncompressed public key
    BREthereumAccount account = (BREthereumAccount) calloc (1, sizeof (struct BREthereumAccountRecord));

    // Assign the key; create the primary address.
    account->masterPubKey = BR_MASTER_PUBKEY_NONE;
    account->primaryAddress = createAddressDerived (key.pubKey, PRIMARY_ADDRESS_BIP44_INDEX);

    return account;
}

extern BREthereumAccount
createAccountDetailed(const char *paperKey, const char *wordList[], const int wordListLength) {

    // Validate arguments
    if (NULL == paperKey || NULL == wordList || BIP39_WORDLIST_COUNT != wordListLength)
        return NULL;

    // Validate paperKey
    if (0 == BRBIP39Decode(NULL, 0, wordList, paperKey))
        return NULL;

    // Generate the 512bit private key using a BIP39 paperKey
    return createAccountWithBIP32Seed(deriveSeedFromPaperKey(paperKey));
}

extern BREthereumAccount
createAccount(const char *paperKey) {
    if (NULL == sharedWordList)
        installSharedWordList(BRBIP39WordsEn, BIP39_WORDLIST_COUNT);
    
    return createAccountDetailed(paperKey, sharedWordList, BIP39_WORDLIST_COUNT);
}

extern void
accountFree (BREthereumAccount account) {
    addressFree(account->primaryAddress);
    free (account);
}

extern BREthereumAddress
accountGetPrimaryAddress (BREthereumAccount account) {
    return account->primaryAddress;
}

extern BRKey
accountGetPrimaryAddressPublicKey (BREthereumAccount account) {
    return addressGetPublicKey(account->primaryAddress);
}

extern BRKey
accountGetPrimaryAddressPrivateKey (BREthereumAccount account,
                                    const char *paperKey) {
 return derivePrivateKeyFromSeed(deriveSeedFromPaperKey(paperKey),
                                 account->primaryAddress->index);
}

extern BREthereumBoolean
accountHasAddress (BREthereumAccount account,
                   BREthereumAddress address) {
    return addressEqual(account->primaryAddress, address);
}

static BREthereumAddress
accountCreateAddress (BREthereumAccount account, UInt512 seed, uint32_t index) {
    BRKey privateKey = derivePrivateKeyFromSeed (seed, index);
    
    // Seriously???
    //
    // https://kobl.one/blog/create-full-ethereum-keypair-and-address/#derive-the-ethereum-address-from-the-public-key
    //
    // "The private key must be 32 bytes and not begin with 0x00 and the public one must be
    // uncompressed and 64 bytes long or 65 with the constant 0x04 prefix. More on that in the
    // next section. ...
    
    uint8_t publicKey[65];
    size_t pubKeyLength = BRKeyPubKey(&privateKey, NULL, 0);
    assert (pubKeyLength == 65);
    
    // "The public key is what we need in order to derive its Ethereum address. Every EC public key
    // begins with the 0x04 prefix before giving the location of the two point on the curve. You
    // should remove this leading 0x04 byte in order to hash it correctly. ...
    
    BRKeyPubKey(&privateKey, publicKey, 65);
    assert (publicKey[0] = 0x04);
    
    return createAddressDerived(publicKey, index);
}

//
// Signature
//
extern BREthereumBoolean
signatureEqual (BREthereumSignature s1, BREthereumSignature s2) {
    return (0 == memcmp(&s1, &s2, sizeof (BREthereumSignature))
            ? ETHEREUM_BOOLEAN_TRUE
            : ETHEREUM_BOOLEAN_FALSE);
}

extern BREthereumAddress
signatureExtractAddress (const BREthereumSignature signature,
                         const uint8_t *bytes,
                         size_t bytesCount,
                         int *success) {
    assert (NULL != success);

    if (SIGNATURE_TYPE_RECOVERABLE != signature.type) {
        *success = 0;
        return &emptyAddressRecord;
    }

    UInt256 digest;
    BRKeccak256 (&digest, bytes, bytesCount);

    BRKey key;
    *success = BRKeyRecoverPubKey(&key, digest,
                                  &signature.sig.recoverable,
                                  sizeof (signature.sig.recoverable));

    if (0 == *success)
        return &emptyAddressRecord;

    return createAddressDerived (key.pubKey, 0);
}

extern BREthereumSignature
accountSignBytesWithPrivateKey(BREthereumAccount account,
                 BREthereumAddress address,
                 BREthereumSignatureType type,
                 uint8_t *bytes,
                 size_t bytesCount,
                 BRKey privateKeyUncompressed) {
    BREthereumSignature signature;
    
    // Save the type.
    signature.type = type;
    
    // Hash with the required Keccak-256
    UInt256 messageDigest;
    BRKeccak256(&messageDigest, bytes, bytesCount);
    
    switch (type) {
        case SIGNATURE_TYPE_FOO:
            break;
            
        case SIGNATURE_TYPE_RECOVERABLE: {
            // Determine the signature length
            size_t signatureLen = BRKeyCompactSign(&privateKeyUncompressed,
                                                   NULL, 0,
                                                   messageDigest);
            
            // Fill the signature
            uint8_t signatureBytes[signatureLen];
            signatureLen = BRKeyCompactSign(&privateKeyUncompressed,
                                            signatureBytes, signatureLen,
                                            messageDigest);
            assert (65 == signatureLen);
            
            // The actual 'signature' is one byte added to secp256k1_ecdsa_recoverable_signature
            // and secp256k1_ecdsa_recoverable_signature is 64 bytes as {r[32], s32]}
            
            // Extract V, R, and S
            signature.sig.recoverable.v = signatureBytes[0];
            memcpy(signature.sig.recoverable.r, &signatureBytes[ 1], 32);
            memcpy(signature.sig.recoverable.s, &signatureBytes[33], 32);
            
            // TODO: Confirm signature
            // assigns pubKey recovered from compactSig to key and returns true on success
            // int BRKeyRecoverPubKey(BRKey *key, UInt256 md, const void *compactSig, size_t sigLen)
            
            break;
        }
    }
    
    return signature;
}

extern BREthereumSignature
accountSignBytes(BREthereumAccount account,
                 BREthereumAddress address,
                 BREthereumSignatureType type,
                 uint8_t *bytes,
                 size_t bytesCount,
                 const char *paperKey) {
    UInt512 seed = deriveSeedFromPaperKey(paperKey);
    return accountSignBytesWithPrivateKey(account,
                             address,
                             type,
                             bytes,
                             bytesCount,
                             derivePrivateKeyFromSeed(seed, address->index));
}

//
// Support
//

extern UInt512
deriveSeedFromPaperKey (const char *paperKey) {
    // Generate the 512bit private key using a BIP39 paperKey
    UInt512 seed = UINT512_ZERO;
    BRBIP39DeriveKey(seed.u8, paperKey, NULL); // no passphrase
    return seed;
}

extern BRKey
derivePrivateKeyFromSeed (UInt512 seed, uint32_t index) {
    BRKey privateKey;
    
    // The BIP32 privateKey for m/44'/60'/0'/0/index
    BRBIP32PrivKeyPath(&privateKey, &seed, sizeof(UInt512), 5,
                       44 | BIP32_HARD,          // purpose  : BIP-44
                       60 | BIP32_HARD,          // coin_type: Ethereum
                       0 | BIP32_HARD,          // account  : <n/a>
                       0,                        // change   : not change
                       index);                   // index    :
    
    privateKey.compressed = 0;
    
    return privateKey;
}
