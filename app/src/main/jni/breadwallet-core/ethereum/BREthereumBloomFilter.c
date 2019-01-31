//
//  BREthereumBloomFilter.c
//  BRCore
//
//  Created by Ed Gamble on 5/10/18.
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

#include <assert.h>
#include <string.h>
#include "BREthereumBloomFilter.h"

/* Forward Declarations */
static void
bloomFilterExtractLocation (unsigned int index, unsigned int *byteIndex, unsigned int *bitIndex);

static void
bloomFilterSetBit (BREthereumBloomFilter *filter, unsigned int index);

static void
bloomFilterClrBit (BREthereumBloomFilter *filter, unsigned int index);

static unsigned int
bloomFilterCreateIndex (uint8_t highByte, uint8_t lowByte);

//
// An Empty BloomFilter
//
static BREthereumBloomFilter empty;

extern BREthereumBloomFilter
bloomFilterCreateEmpty (void) {
    return empty;
}

/**
 * 'Designated Contructor' for BloomFilter
 */
extern BREthereumBloomFilter
bloomFilterCreateHash (const BREthereumHash hash) {
    BREthereumBloomFilter filter = empty;
    bloomFilterSetBit(&filter, bloomFilterCreateIndex(hash.bytes[0], hash.bytes[1]));
    bloomFilterSetBit(&filter, bloomFilterCreateIndex(hash.bytes[2], hash.bytes[3]));
    bloomFilterSetBit(&filter, bloomFilterCreateIndex(hash.bytes[4], hash.bytes[5]));
    return filter;
}

extern BREthereumBloomFilter
bloomFilterCreateData (const BRRlpData data) {
    return bloomFilterCreateHash(hashCreateFromData(data));
}

extern BREthereumBloomFilter
bloomFilterCreateAddress (const BREthereumAddressRaw address) {
    BRRlpData data;
    data.bytes = (uint8_t *)  address.bytes;
    data.bytesCount = sizeof (address.bytes);
    return bloomFilterCreateHash(hashCreateFromData(data));
}

extern BREthereumBloomFilter
bloomFilterOr (const BREthereumBloomFilter filter1, const BREthereumBloomFilter filter2) {
    BREthereumBloomFilter result = empty;
    for (int i = 0; i < ETHEREUM_BLOOM_FILTER_BYTES; i++)
        result.bytes[i] = filter1.bytes[i] | filter2.bytes[i];
    return result;
}

extern void
bloomFilterOrInPlace (BREthereumBloomFilter filter1, const BREthereumBloomFilter filter2) {
    for (int i = 0; i < ETHEREUM_BLOOM_FILTER_BYTES; i++)
        filter1.bytes[i] |= filter2.bytes[i];
}

extern BREthereumBoolean
bloomFilterEqual (const BREthereumBloomFilter filter1, const BREthereumBloomFilter filter2) {
    return (0 == memcmp (filter1.bytes, filter2.bytes, ETHEREUM_BLOOM_FILTER_BYTES)
            ? ETHEREUM_BOOLEAN_TRUE
            : ETHEREUM_BOOLEAN_FALSE);
}

extern BREthereumBoolean
bloomFilterMatch (const BREthereumBloomFilter filter, const BREthereumBloomFilter other) {
    return bloomFilterEqual(filter, bloomFilterOr(filter, other));
}

//
// RLP Encode / Decoe
//
extern BRRlpItem
bloomFilterRlpEncode(BREthereumBloomFilter filter, BRRlpCoder coder) {
    return rlpEncodeItemBytes(coder, filter.bytes, 256);
}

extern BREthereumBloomFilter
bloomFilterRlpDecode (BRRlpItem item, BRRlpCoder coder) {
    BREthereumBloomFilter filter;

    BRRlpData data = rlpDecodeItemBytes(coder, item);
    assert (256 == data.bytesCount);

    memcpy (filter.bytes, data.bytes, 256);
    return filter;
}

//
// As String
//
extern char *
bloomFilterAsString (BREthereumBloomFilter filter) {
    return encodeHexCreate(NULL, filter.bytes, sizeof (filter.bytes));
}

//
// Support
//

/**
 * Extract the byte and bit position of `index` within ETHEREUM_BLOOM_FILTER_BITS.  `index`
 * is [0, ETHEREUM_BLOOM_FILTER_BITS).
 *
 */
static void
bloomFilterExtractLocation (unsigned int index, unsigned int *byteIndex, unsigned int *bitIndex) {
    assert (index < ETHEREUM_BLOOM_FILTER_BITS);
    assert (NULL != byteIndex && NULL != bitIndex);

    *byteIndex = (ETHEREUM_BLOOM_FILTER_BITS - 1 - index ) / 8;
    *bitIndex  = index % 8;
}

static void
bloomFilterSetBit (BREthereumBloomFilter *filter, unsigned int index) {
    unsigned int byteIndex;
    unsigned int bitIndex;
    bloomFilterExtractLocation(index, &byteIndex, &bitIndex);
    filter->bytes[byteIndex] |= (1 << bitIndex);
}

static void
bloomFilterClrBit (BREthereumBloomFilter *filter, unsigned int index) {
    unsigned int byteIndex;
    unsigned int bitIndex;
    bloomFilterExtractLocation(index, &byteIndex, &bitIndex);
    filter->bytes[byteIndex] &= ~(1 << bitIndex);
}

#define ETHERUM_BLOOM_FILTER_BITS_MASK  (ETHEREUM_BLOOM_FILTER_BITS - 1)

static unsigned int
bloomFilterCreateIndex (uint8_t highByte, uint8_t lowByte) {
    return ((highByte << 8) | lowByte) & ETHERUM_BLOOM_FILTER_BITS_MASK;
}
