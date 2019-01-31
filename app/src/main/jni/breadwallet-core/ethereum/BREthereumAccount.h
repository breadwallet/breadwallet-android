//
//  BBREthereumAddress.h
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

#ifndef BR_Ethereum_Account_H
#define BR_Ethereum_Account_H

#ifdef __cplusplus
extern "C" {
#endif

#include "BRKey.h"
#include "BRInt.h"
#include "rlp/BRRlpCoder.h"
#include "BREthereumEther.h"

//
// Address
//

/**
 *
 */
typedef struct BREthereumAddressRecord *BREthereumAddress;

/**
 * Create an address from the external representation of an address.  The provided address *must*
 * include a prefix of "Ox" and pass the validateAddressString() function; otherwise NULL is
 * returned.
 *
 * @param string
 * @return
 */
extern BREthereumAddress
createAddress (const char *string);

/**
 * Validate `string` as an Ethereum address.  The validation is minimal - based solely on the
 * `string` content.  Said another way, the Ethereum Network is not used for validation.
 *
 * At a minimum `string` must start with "0x", have a total of 42 characters and by a 'hex' string
 * (as if a result of encodeHex(); containing characters [0-9,a-f])
 *
 * @param string
 * @return
 */
extern BREthereumBoolean
validateAddressString(const char *string);

extern uint64_t
addressGetNonce(BREthereumAddress address);

extern void
addressFree (BREthereumAddress address);

extern BREthereumBoolean
addressHasString (BREthereumAddress address,
                  const char *string);

extern BREthereumBoolean
addressEqual (BREthereumAddress a1, BREthereumAddress a2);

/**
 * Returns a string representation of the address, newly allocated.  YOU OWN THIS.
 */
extern char *
addressAsString (BREthereumAddress address);

extern BRKey
addressGetPublicKey (BREthereumAddress address);

#if defined (DEBUG)
extern const char *
addressPublicKeyAsString (BREthereumAddress address, int compressed);
#endif

extern BRRlpItem
addressRlpEncode (BREthereumAddress address, BRRlpCoder coder);

extern BREthereumAddress
addressRlpDecode (BRRlpItem item, BRRlpCoder coder);

//
// Address Raw (QUASI-INTERNAL - currently used for Block/Log Encoding/Decoding
//
typedef struct {
    uint8_t bytes[20];
} BREthereumAddressRaw;

#define EMPTY_ADDRESS_INIT   { \
    0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0,   0, 0, 0, 0 \
}

extern BREthereumAddressRaw
addressRawCreate (const char *address);

extern BREthereumAddressRaw
addressRawRlpDecode (BRRlpItem item,
                     BRRlpCoder coder);

extern BRRlpItem
addressRawRlpEncode(BREthereumAddressRaw address,
                    BRRlpCoder coder);

//
// Account
//

/**
 * The Bread App will have a single EthereumAccount for both Ether and all ERC20 tokens.  This
 * account is conceptually identical to the App's 'private key' derived from the User's 'paper
 * key'.  An EthereumAccount uses BIP32 (probably not BIP44) to generate addresses; and thus
 * the provided 'private key' must be suitable for BIP32.  [The 'private key` argument is likely
 * a BRMasterPubKey thingy]
 *
 * An EthereumAccount can generate an essentially arbitrary number of EthereumAddress-es.  However,
 * in Ethereum addresses are not a factor in privacy; therefore, we'll use one EthereumAddress per
 * EthereumWallet - all transactions for that wallet will use the same address.
 *
 */
typedef struct BREthereumAccountRecord *BREthereumAccount;

/**
 * Create a new account using paperKey and the sharedWordList (see installSharedWordList).
 *
 * @param paperKey
 * @return
 */
extern BREthereumAccount
createAccount(const char *paperKey);

/**
 * Create a new account using the 65 bytes, 0x04-prefixed, uncompressed public key (as returned
 * by addressGetPublicKey())
 */
extern BREthereumAccount
createAccountWithPublicKey (const BRKey publicKey);

extern void
accountFree (BREthereumAccount account);

/**
 * Create a new account using paperKey and the provided wordList
 *
 * @param paperKey
 * @param wordList
 * @param wordListLength
 * @return
 */
extern BREthereumAccount
createAccountDetailed(const char *paperKey, const char *wordList[], const int wordListLength);

/**
 * The account's primary address (aka 'address[0]').
 *
 * TODO: Copy or not
 *
 * @param account
 * @return
 */
extern BREthereumAddress
accountGetPrimaryAddress (BREthereumAccount account);

/**
 * the public key for the account's primary address
 */
extern BRKey
accountGetPrimaryAddressPublicKey (BREthereumAccount account);

/**
 * the privateKey for the account's primary address
 */
extern BRKey
accountGetPrimaryAddressPrivateKey (BREthereumAccount account,
                                    const char *paperKey);

extern BREthereumBoolean
accountHasAddress (BREthereumAccount account,
                   BREthereumAddress address);
    
//
// Signature
//

typedef enum {
    SIGNATURE_TYPE_FOO,
    SIGNATURE_TYPE_RECOVERABLE
} BREthereumSignatureType;

typedef struct {
    BREthereumSignatureType type;
    union {
        struct {
            int ignore;
        } foo;

        struct {
            uint8_t v;
            uint8_t r[32];
            uint8_t s[32];
        } recoverable;
    } sig;
} BREthereumSignature;

extern BREthereumBoolean
signatureEqual (BREthereumSignature s1, BREthereumSignature s2);

extern BREthereumAddress
signatureExtractAddress (const BREthereumSignature signature,
                         const uint8_t *bytes,
                         size_t bytesCount,
                         int *success);

/**
 * Sign an arbitrary array of bytes with the account's private key using the signature algorithm
 * specified by `type`.
 *
 * @param account
 * @param type
 * @param bytes
 * @param bytesCount
 * @return
 */
extern BREthereumSignature
accountSignBytesWithPrivateKey(BREthereumAccount account,
                 BREthereumAddress address,
                 BREthereumSignatureType type,
                 uint8_t *bytes,
                 size_t bytesCount,
                 BRKey privateKey);

extern BREthereumSignature
accountSignBytes(BREthereumAccount account,
                 BREthereumAddress address,
                 BREthereumSignatureType type,
                 uint8_t *bytes,
                 size_t bytesCount,
                 const char *paperKey);

//
// Support (quasi-private)
//
extern UInt512
deriveSeedFromPaperKey (const char *paperKey);

extern BRKey
derivePrivateKeyFromSeed (UInt512 seed, uint32_t index);

#ifdef __cplusplus
}
#endif

#endif /* BR_Ethereum_Account_H */
