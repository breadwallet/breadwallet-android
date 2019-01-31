//
//  BREthereumHash.c
//  BRCore
//
//  Created by Ed Gamble on 5/9/18.
//  Copyright © 2018 breadwallet LLC. All rights reserved.
//

#include <stdio.h>
#include <string.h>
#include <assert.h>
#include "BRCrypto.h"
#include "util/BRUtil.h"
#include "BREthereumBase.h"


/**
 * Create a Hash by converting from a hex-encoded string of a hash.  The string must
 * begin with '0x'.
 */
extern BREthereumHash
hashCreate (const char *string) {
    if (NULL == string || '\0' == string[0] || 0 == strcmp (string, "0x")) return hashCreateEmpty();

    assert (0 == strncmp (string, "0x", 2)
            && (2 + 2 * ETHEREUM_HASH_BYTES) == strlen (string));

    BREthereumHash hash;
    decodeHex(hash.bytes, ETHEREUM_HASH_BYTES, &string[2], 2 * ETHEREUM_HASH_BYTES);
    return hash;
}

/**
 * Create an empty (all zeros) Hash
 */
extern BREthereumHash
hashCreateEmpty (void) {
    BREthereumHash hash;
    memset (hash.bytes, 0, sizeof (BREthereumHash));
    return hash;
}

/**
 * Creata a Hash by computing it from a arbitrary data set
 */
extern BREthereumHash
hashCreateFromData (BRRlpData data) {
    BREthereumHash hash;
    BRKeccak256(hash.bytes, data.bytes, data.bytesCount);
    return hash;
}

/**
 * Return the hex-encoded string
 */
extern char *
hashAsString (BREthereumHash hash) {
    char result [2 + 2 * ETHEREUM_HASH_BYTES + 1];
    result[0] = '0';
    result[1] = 'x';
    encodeHex(&result[2], 2 * ETHEREUM_HASH_BYTES + 1, hash.bytes, ETHEREUM_HASH_BYTES);
    return strdup (result);
}

extern BREthereumBoolean
hashExists (BREthereumHash hash) {
    for (int i = 0; i < ETHEREUM_HASH_BYTES; i++)
        if (0 != hash.bytes[i]) return ETHEREUM_BOOLEAN_TRUE;
    return ETHEREUM_BOOLEAN_FALSE;
}

extern BREthereumHash
hashCopy(BREthereumHash hash) {
    return hash;
}

extern BREthereumComparison
hashCompare(BREthereumHash hash1, BREthereumHash hash2) {
    for (int i = 0; i < ETHEREUM_HASH_BYTES; i++) {
        if (hash1.bytes[i] > hash2.bytes[i]) return ETHEREUM_COMPARISON_GT;
        else if (hash1.bytes[i] < hash2.bytes[i]) return ETHEREUM_COMPARISON_LT;
    }
    return ETHEREUM_COMPARISON_EQ;
}

extern BREthereumBoolean
hashEqual (BREthereumHash hash1, BREthereumHash hash2) {
    return AS_ETHEREUM_BOOLEAN (0 == memcmp (hash1.bytes, hash2.bytes, ETHEREUM_HASH_BYTES));
}

extern BRRlpItem
hashRlpEncode(BREthereumHash hash, BRRlpCoder coder) {
    return rlpEncodeItemBytes(coder, hash.bytes, ETHEREUM_HASH_BYTES);
}

extern BREthereumHash
hashRlpDecode (BRRlpItem item, BRRlpCoder coder) {
    BREthereumHash hash;

    BRRlpData data = rlpDecodeItemBytes(coder, item);
    assert (ETHEREUM_HASH_BYTES == data.bytesCount);

    memcpy (hash.bytes, data.bytes, ETHEREUM_HASH_BYTES);
    return hash;
}
