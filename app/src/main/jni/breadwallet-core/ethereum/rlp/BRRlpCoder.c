//
//  rlp
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 2/25/18.
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
#include <stdarg.h>
#include <memory.h>
#include <assert.h>
#include "BRRlpCoder.h"
#include "BRUtil.h"

static int
rlpDecodeItemIsEmptyString (BRRlpCoder coder, BRRlpItem item);

/**
 * An RLP Encoding is comprised of two types: an ITEM and a LIST (of ITEM).
 *
 */
typedef enum {
    CODER_ITEM,
    CODER_LIST
} BRRlpItemType;

/**
 * An RLP Context holds encoding results for each of the encoding types, either ITEM or LIST.
 * The ITEM type holds the bytes directly; the LIST type holds a list/array of ITEMS.
 *
 * The upcoming RLP Coder is going to hold multiple Contexts.  The public interface for RLP Item
 * holds an 'indexer' which is the index to a Context in the Coder.
 */
typedef struct {
    BRRlpCoder coder;  // validation
    BRRlpItemType type;
    
    // The encoding
    size_t bytesCount;
    uint8_t *bytes;
    
    // If CODER_LIST, then the component items.
    size_t itemsCount;
    BRRlpItem *items;
    
} BRRlpContext;

static BRRlpContext contextEmpty = { NULL, CODER_ITEM, 0, NULL, 0, NULL };

//static int
//contextIsEmpty (BRRlpContext context) {
//  return NULL == context.coder;
//}

static void
contextRelease (BRRlpContext context) {
    if (NULL != context.bytes) free (context.bytes);
    if (NULL != context.items) free (context.items);
}

static int
contextIsValid (BRRlpContext context) {
    return NULL != context.coder;
}

static BRRlpContext
createContextItem (BRRlpCoder coder, uint8_t *bytes, size_t bytesCount, int takeBytes) {
    // assert (bytesCount > 0);
    BRRlpContext context = contextEmpty;
    
    context.coder = coder;
    context.type = CODER_ITEM;
    context.bytesCount = bytesCount;
    if (takeBytes)
        context.bytes = bytes;
    else {
        uint8_t *myBytes = malloc (bytesCount);
        memcpy (myBytes, bytes, bytesCount);
        context.bytes = myBytes;
    }
    return context;
}

static BRRlpContext
createContextList (BRRlpCoder coder, uint8_t *bytes, size_t bytesCount, int takeBytes, BRRlpItem *items, size_t itemsCount) {
    BRRlpContext context = createContextItem(coder, bytes, bytesCount, takeBytes);
    context.type = CODER_LIST;
    context.itemsCount = itemsCount;
    
    context.items = calloc (itemsCount, sizeof (BRRlpItem));
    for (int i = 0; i < itemsCount; i++)
        context.items[i] = items[i];
    
    return context;
}

/**
 * Return a new BRRlpContext by appending the two provided contexts.  Both provided contexts
 * must be for CODER_ITEM (othewise an 'assert' is raised); the appending is performed by simply
 * concatenating the two context's byte arrays.
 *
 * If release is TRUE, then both the provided contexts are released; thereby freeing their memory.
 *
 */
static BRRlpContext
createContextItemAppend (BRRlpCoder coder, BRRlpContext context1, BRRlpContext context2, int release) {
    assert (CODER_ITEM == context1.type && CODER_ITEM == context2.type);
    assert (coder == context1.coder     && coder == context2.coder);
    
    BRRlpContext context = contextEmpty;
    
    context.coder = coder;
    context.type = CODER_ITEM;
    
    context.bytesCount = context1.bytesCount + context2.bytesCount;
    context.bytes = malloc (context.bytesCount);
    memcpy (&context.bytes[0], context1.bytes, context1.bytesCount);
    memcpy (&context.bytes[context1.bytesCount], context2.bytes, context2.bytesCount);
    
    if (release) {
        contextRelease(context1);
        contextRelease(context2);
    }
    
    return context;
}

/**
 * And RLP Coder holds Contexts; any held Context can be encoded into an array of bytes (uint8_t)
 * using coderContextFillData() or the public funtion rlpGetData().
 */
struct BRRlpCoderRecord {
    BRRlpContext *contexts;
    size_t contextsCount;
    size_t contextsAllocated;
};

#define CODER_DEFAULT_CONTEXTS 10

extern BRRlpCoder
rlpCoderCreate (void) {
    BRRlpCoder coder = (BRRlpCoder) malloc (sizeof (struct BRRlpCoderRecord));
    
    coder->contextsCount = 0;
    coder->contextsAllocated = CODER_DEFAULT_CONTEXTS;
    coder->contexts = (BRRlpContext *) calloc (CODER_DEFAULT_CONTEXTS, sizeof (BRRlpContext));
    
    return coder;
}

static void
coderRelease (BRRlpCoder coder) {
    for (int i = 0; i < coder->contextsCount; i++) {
        contextRelease(coder->contexts[i]);
    }
    free (coder->contexts);
    free (coder);
}

static int
coderIsValidItem (BRRlpCoder coder, BRRlpItem item) {
    return item.indexer < coder->contextsCount && item.identifier == coder;
}

/**
 * Return the RLP Context corresponding to the provided RLP Item; if `item` is invalid, then
 * an empty context is returned.
 */
static BRRlpContext
coderLookupContext (BRRlpCoder coder, BRRlpItem item) {
    return (coderIsValidItem(coder, item)
            ? coder->contexts[item.indexer]
            : contextEmpty);
}

/**
 * Add `context` to `coder` and return the corresponding RLP Item.  Extends coder's context
 * array if required.
 */
static BRRlpItem
coderAddContext (BRRlpCoder coder, BRRlpContext context) {
    if (coder->contextsCount + 1 >= coder->contextsAllocated) {
        coder->contextsAllocated += CODER_DEFAULT_CONTEXTS;
        coder->contexts = (BRRlpContext *) realloc (coder->contexts, coder->contextsAllocated * sizeof (BRRlpContext));
        return coderAddContext(coder, context);
    }
    else {
        BRRlpItem item;
        item.identifier = coder;
        item.indexer = coder->contextsCount;
        coder->contexts[item.indexer] = context;
        coder->contextsCount += 1;
        return item;
    }
}

// The largest number supported for encoding is a UInt256 - which is representable as 32 bytes.
#define CODER_NUMBER_BYTES_LIMIT    (256/8)

/**
 * Return the index of the first non-zero byte; if all bytes are zero, bytesCount is returned
 */
static int
findNonZeroIndex (uint8_t *bytes, size_t bytesCount) {
    for (int i = 0; i < bytesCount; i++)
        if (bytes[i] != 0) return i;
    return (int) bytesCount;
}

/**
 * Fill `target` with `source` converted to BIG_ENDIAN.
 *
 * Note: target and source must not overlap.
 */
static void
swapBytesIfLittleEndian (uint8_t *target, uint8_t *source, size_t count) {
    assert (target != source);  // common overlap case, but wholely insufficient.
    for (int i = 0; i < count; i++) {
#if BYTE_ORDER == LITTLE_ENDIAN
        target[i] = source[count - 1 - i];
#else
        target[i] = source[i]
#endif
    }
}

/**
 * Fill `length` bytes formatted as big-endian into `target` from `source`.  Set `targetIndex`
 * as the `target` index of the first non-zero value; set `targetCount` is the number of bytes
 * after `targetIndex`.
 */
static void
convertToBigEndianAndNormalize (uint8_t *target, uint8_t *source, size_t length, size_t *targetIndex, size_t *targetCount) {
    assert (length <= CODER_NUMBER_BYTES_LIMIT);
    
    swapBytesIfLittleEndian (target, source, length);
    
    *targetIndex = findNonZeroIndex(target, length);
    *targetCount = length - *targetIndex;
    
    if (0 == *targetCount) {
        *targetCount = 1;
        *targetIndex = 0;
    }
}

/**
 * Fill `targetCount` bytes into `target` using the big-endian formatted `bytesCount` at `bytes`.
 */
static void
convertFromBigEndian (uint8_t *target, size_t targetCount, uint8_t *bytes, size_t bytesCount) {
    // Bytes represents a number in big-endian              : 04 00
    // Fill out the number with prefix zeros                : 00 00 00 00 00 00 04 00
    // Copy the bytes into target, swap if little endian    : 00 04 00 00 00 00 00 00
    uint8_t value[targetCount];
    memset (value, 0, targetCount);
    memcpy (&value[targetCount - bytesCount], bytes, bytesCount);

    swapBytesIfLittleEndian(target, value, targetCount);
}

#define RLP_PREFIX_BYTES  (0x80)
#define RLP_PREFIX_LIST   (0xc0)
#define RLP_PREFIX_LENGTH_LIMIT  (55)

static BRRlpContext
coderEncodeLength (BRRlpCoder coder, uint64_t length, uint8_t baseline) {
    // If the length is small, simply encode a single byte as (baseline + length)
    if (length <= RLP_PREFIX_LENGTH_LIMIT) {
        uint8_t encoding = baseline + length;
        return createContextItem (coder, &encoding, 1, 0);
    }
    // Otherwise, encode the length as bytes.
    else {
        size_t lengthSize = sizeof (uint64_t);
        
        uint8_t bytes [lengthSize]; // big_endian representation of the bytes in 'length'
        size_t bytesIndex;          // Index of the first non-zero byte
        size_t bytesCount;          // The number of bytes to encode (beyond index)
        
        convertToBigEndianAndNormalize (bytes, (uint8_t *) &length, lengthSize, &bytesIndex, &bytesCount);
        
        // The encoding - a header byte with the bytesCount and then the big_endian bytes themselves.
        uint8_t encoding [1 + bytesCount];
        encoding[0] = baseline + RLP_PREFIX_LENGTH_LIMIT + bytesCount;
        memcpy (&encoding[1], &bytes[bytesIndex], bytesCount);
        return createContextItem(coder, encoding, 1 + bytesCount, 0);
    }
}

static uint64_t
coderDecodeLength (BRRlpCoder coder, uint8_t *bytes, uint8_t baseline, uint8_t *offset) {
    uint8_t prefix = bytes[0];

    *offset = 0;
    if (prefix < baseline) return 1;

    else if ((prefix - baseline) <= RLP_PREFIX_LENGTH_LIMIT) {
        *offset = 1; // just prefix
        return (prefix - baseline);
    }
    else {
        // Number of bytes encoding the length
        size_t lengthByteCount = (prefix - baseline) - RLP_PREFIX_LENGTH_LIMIT;
        *offset = 1 + lengthByteCount; // prefix + length bytes

        // Result
        uint64_t length = 0;
        size_t lengthSize = sizeof (uint64_t);
        assert (lengthByteCount <= lengthSize);

        convertFromBigEndian((uint8_t*)&length, lengthSize, &bytes[1], lengthByteCount);
//        // A big-endian byte array.
//        uint8_t bytesValue [lengthSize];
//        memset (bytesValue, 0, lengthSize);
//        memcpy (&bytesValue[8 - lengthByteCount], &bytes[1], lengthByteCount);
//
//        coderSwapBytesIfLittleEndian((uint8_t*)&length, bytesValue, lengthSize);
        return length;
    }
}

static BRRlpContext
coderEncodeBytes(BRRlpCoder coder, uint8_t *bytes, size_t bytesCount) {
    // Encode a single byte directly
    if (1 == bytesCount && bytes[0] < RLP_PREFIX_BYTES) {
        return createContextItem(coder, bytes, 1, 0);
    }
    
    // otherwise, encode the length and then the bytes themselves
    else {
        return createContextItemAppend(coder,
                                       coderEncodeLength(coder, bytesCount, RLP_PREFIX_BYTES),
                                       createContextItem(coder, bytes, bytesCount, 0),
                                       1);
    }
}

//
// Number
//
static BRRlpContext
coderEncodeNumber (BRRlpCoder coder, uint8_t *source, size_t sourceCount) {
    // Encode a number by converting the number to a big_endian representation and then simply
    // encoding those bytes.
    uint8_t bytes [sourceCount]; // big_endian representation of the bytes in 'length'
    size_t bytesIndex;           // Index of the first non-zero byte
    size_t bytesCount;           // The number of bytes to encode
    
    convertToBigEndianAndNormalize (bytes, source, sourceCount, &bytesIndex, &bytesCount);
    
    return coderEncodeBytes(coder, &bytes[bytesIndex], bytesCount);
}

static void
coderDecodeNumber (BRRlpCoder coder, uint8_t *target, size_t targetCount, uint8_t *bytes, size_t bytesCount) {
    uint8_t offset = 0;
    uint64_t length = coderDecodeLength(coder, bytes, RLP_PREFIX_BYTES, &offset);

    convertFromBigEndian(target, targetCount, &bytes[offset], length);
}

//
// UInt64
//
static BRRlpContext
coderEncodeUInt64 (BRRlpCoder coder, uint64_t value) {
    return coderEncodeNumber(coder, (uint8_t *) &value, sizeof(value));
}

static uint64_t
coderDecodeUInt64 (BRRlpCoder coder, BRRlpContext context) {
    assert (contextIsValid(context));
    uint64_t value = 0;
    coderDecodeNumber (coder, (uint8_t*)&value, sizeof(uint64_t), context.bytes, context.bytesCount);
    return value;
}

//
// UInt256
//
static BRRlpContext
coderEncodeUInt256 (BRRlpCoder coder, UInt256 value) {
    return coderEncodeNumber(coder, (uint8_t *) &value, sizeof(value));
}

static UInt256
coderDecodeUInt256 (BRRlpCoder coder, BRRlpContext context) {
    assert (contextIsValid(context));
    UInt256 value = UINT256_ZERO;
    coderDecodeNumber (coder, (uint8_t*)&value, sizeof (UInt256), context.bytes, context.bytesCount);
    return value;
}

//
// List
//
static BRRlpContext
coderEncodeList (BRRlpCoder coder, BRRlpItem *items, size_t itemsCount) {
    // Validate the items
    for (int i = 0; i < itemsCount; i++) {
        assert (coderIsValidItem(coder, items[i]));
    }
    
    // Eventually fill these with concatenated item encodings.
    size_t bytesCount = 0;
    uint8_t *bytes = NULL;
    
    for (int i = 0; i < itemsCount; i++)
        bytesCount += coderLookupContext(coder, items[i]).bytesCount;
    
    bytes = malloc (bytesCount);
    
    {
        size_t bytesIndex = 0;
        for (int i = 0; i < itemsCount; i++) {
            BRRlpContext itemContext = coderLookupContext(coder, items[i]);
            memcpy (&bytes[bytesIndex], itemContext.bytes, itemContext.bytesCount);
            bytesIndex += itemContext.bytesCount;
        }
    }
    
    BRRlpContext encodedBytesContext = (0 == bytesCount
                                        ? coderEncodeLength(coder, bytesCount, RLP_PREFIX_LIST)
                                        : createContextItemAppend(coder,
                                                                  coderEncodeLength(coder, bytesCount, RLP_PREFIX_LIST),
                                                                  createContextItem(coder, bytes, bytesCount, 1),
                                                                  1));
    
    return createContextList(coder,
                             encodedBytesContext.bytes,
                             encodedBytesContext.bytesCount,
                             1,
                             items,
                             itemsCount);
}


//
// Public Interface
//
extern void
rlpCoderRelease (BRRlpCoder coder) {
    coderRelease (coder);
}

//
// UInt64
//
extern BRRlpItem
rlpEncodeItemUInt64(BRRlpCoder coder, uint64_t value, int zeroAsEmptyString) {
    return (1 == zeroAsEmptyString && 0 == value
            ? rlpEncodeItemString(coder, "")
            : coderAddContext(coder, coderEncodeUInt64(coder, value)));
}

extern uint64_t
rlpDecodeItemUInt64(BRRlpCoder coder, BRRlpItem item, int zeroAsEmptyString) {
    return (1 == zeroAsEmptyString &&  rlpDecodeItemIsEmptyString (coder, item)
            ? 0
            : coderDecodeUInt64(coder, coderLookupContext(coder, item)));
}

//
// UInt256
//
extern BRRlpItem
rlpEncodeItemUInt256(BRRlpCoder coder, UInt256 value, int zeroAsEmptyString) {
    return (1 == zeroAsEmptyString && 0 == compareUInt256 (value, UINT256_ZERO)
            ? rlpEncodeItemString(coder, "")
            : coderAddContext(coder, coderEncodeUInt256(coder, value)));
}

extern UInt256
rlpDecodeItemUInt256(BRRlpCoder coder, BRRlpItem item, int zeroAsEmptyString) {
    return (1 == zeroAsEmptyString &&  rlpDecodeItemIsEmptyString (coder, item)
            ? UINT256_ZERO
            : coderDecodeUInt256(coder, coderLookupContext(coder, item)));
}

//
// Bytes
//
extern BRRlpItem
rlpEncodeItemBytes(BRRlpCoder coder, uint8_t *bytes, size_t bytesCount) {
    return coderAddContext(coder, coderEncodeBytes(coder, bytes, bytesCount));
}

extern BRRlpData
rlpDecodeItemBytes (BRRlpCoder coder, BRRlpItem item) {
    assert (coderIsValidItem(coder, item));
    BRRlpContext context = coderLookupContext(coder, item);

    uint8_t offset = 0;
    uint64_t length = coderDecodeLength(coder, context.bytes, RLP_PREFIX_BYTES, &offset);

    BRRlpData result;
    result.bytesCount = length;
    result.bytes = malloc (length);
    memcpy (result.bytes, &context.bytes[offset], length);

    return result;
}

//
// String
//
extern BRRlpItem
rlpEncodeItemString (BRRlpCoder coder, char *string) {
    if (NULL == string) string = "";
    return rlpEncodeItemBytes(coder, (uint8_t *) string, strlen (string));
}

extern char *
rlpDecodeItemString (BRRlpCoder coder, BRRlpItem item) {
    assert (coderIsValidItem(coder, item));
    BRRlpContext context = coderLookupContext(coder, item);

    uint8_t offset = 0;
    uint64_t length = coderDecodeLength(coder, context.bytes, RLP_PREFIX_BYTES, &offset);

    char *result = malloc (length + 1);
    memcpy (result, &context.bytes[offset], length);
    result[length] = '\0';

    return result;
}

extern int
rlpDecodeItemIsString (BRRlpCoder coder, BRRlpItem item) {
    assert (coderIsValidItem(coder, item));
    BRRlpContext context = coderLookupContext(coder, item);
    return (CODER_ITEM == context.type
            && 0 != context.bytesCount
            && RLP_PREFIX_BYTES <= context.bytes[0]
            && context.bytes[0] <  RLP_PREFIX_LIST);
}

static int
rlpDecodeItemIsEmptyString (BRRlpCoder coder, BRRlpItem item) {
    assert (coderIsValidItem(coder, item));
    BRRlpContext context = coderLookupContext(coder, item);
    return (CODER_ITEM == context.type
            && 1 == context.bytesCount
            && RLP_PREFIX_BYTES <= context.bytes[0]);
}

//
// Hex String
//
extern BRRlpItem
rlpEncodeItemHexString (BRRlpCoder coder, char *string) {
    if (NULL == string)
        return rlpEncodeItemString(coder, string);
    
    // Strip off "0x" if it exists
    if (0 == strncmp (string, "0x", 2))
        string = &string[2];
    
    if (0 == strlen(string))
        return rlpEncodeItemString(coder, string);
    
    // Decode Hex into (new) BYTES; then RLP encode those bytes.
    size_t bytesCount = 0;
    uint8_t *bytes = decodeHexCreate(&bytesCount, string, strlen(string));
    BRRlpItem item = rlpEncodeItemBytes(coder, bytes, bytesCount);
    free (bytes);
    
    return item;
}

extern char *
rlpDecodeItemHexString (BRRlpCoder coder, BRRlpItem item, const char *prefix) {
    BRRlpData data = rlpDecodeItemBytes(coder, item);
    if (NULL == prefix) prefix = "";

    char *result = malloc (strlen(prefix) + 2 * data.bytesCount + 1);
    strcpy (result, prefix);
    encodeHex(&result[strlen(prefix)], 2 * data.bytesCount + 1, data.bytes, data.bytesCount);

    rlpDataRelease (data);
    return result;
}

//
// List
//
extern BRRlpItem
rlpEncodeList1 (BRRlpCoder coder, BRRlpItem item) {
    assert (coderIsValidItem(coder, item));
    BRRlpItem items[1];
    
    items[0] = item;
    
    return coderAddContext(coder, coderEncodeList(coder, items, 1));
}

extern BRRlpItem
rlpEncodeList2 (BRRlpCoder coder, BRRlpItem item1, BRRlpItem item2) {
    assert (coderIsValidItem(coder, item1));
    assert (coderIsValidItem(coder, item1));
    
    BRRlpItem items[2];
    
    items[0] = item1;
    items[1] = item2;
    
    return coderAddContext(coder, coderEncodeList(coder, items, 2));
}

extern BRRlpItem
rlpEncodeList (BRRlpCoder coder, size_t count, ...) {
    BRRlpItem items[count];
    
    va_list args;
    va_start (args, count);
    for (int i = 0; i < count; i++)
        items[i] = va_arg (args, BRRlpItem);
    va_end(args);
    
    return coderAddContext(coder, coderEncodeList(coder, items, count));
}

extern BRRlpItem
rlpEncodeListItems (BRRlpCoder coder, BRRlpItem *items, size_t itemsCount) {
    return coderAddContext(coder, coderEncodeList(coder, items, itemsCount));
}

extern const BRRlpItem *
rlpDecodeList (BRRlpCoder coder, BRRlpItem item, size_t *itemsCount) {
    assert (coderIsValidItem(coder, item));
    BRRlpContext context = coderLookupContext(coder, item);

    switch (context.type) {
        case CODER_ITEM:
            *itemsCount = 0;
            return NULL;
        case CODER_LIST:
            *itemsCount = context.itemsCount;
            return context.items;
    }
}

//
// Data
//
extern BRRlpData
createRlpDataEmpty (void) {
    BRRlpData data;
    data.bytesCount = 0;
    data.bytes = NULL;
    return data;
}

extern void
rlpDataRelease (BRRlpData data) {
    if (NULL != data.bytes && '\0' != data.bytes[0]) free (data.bytes);
    data.bytesCount = 0;
    data.bytes = NULL;
}

extern void
rlpDataExtract (BRRlpCoder coder, BRRlpItem item, uint8_t **bytes, size_t *bytesCount) {
    assert (coderIsValidItem(coder, item));
    assert (NULL != bytes && NULL != bytesCount);
    
    BRRlpContext context = coderLookupContext(coder, item);
    *bytesCount = context.bytesCount;
    *bytes = malloc (*bytesCount);
    memcpy (*bytes, context.bytes, context.bytesCount);
}

/**
 * Return `data` with `bytes` and bytesCount derived from the bytes[0] and associated length.
 */
static BRRlpData
rlpGetItem_FillData (BRRlpCoder coder, uint8_t *bytes) {
    BRRlpData data;
    data.bytes = bytes;
    data.bytesCount = 1;

    uint8_t prefix = bytes[0];
    if (prefix >= RLP_PREFIX_BYTES) {
        uint8_t offset;
        data.bytesCount = coderDecodeLength(coder, bytes,
                                            (prefix < RLP_PREFIX_LIST ? RLP_PREFIX_BYTES : RLP_PREFIX_LIST),
                                            &offset);
        data.bytesCount += offset;
    }
    return data;
}

#define DEFAULT_ITEM_INCREMENT 20

/**
 * Convet the bytes in `data` into an `item`.  If `data` represents a RLP list, then `item` will
 * represent a list.
 */
extern BRRlpItem
rlpGetItem (BRRlpCoder coder, BRRlpData data) {
    assert (0 != data.bytesCount);

    uint8_t prefix = data.bytes[0];

    // If not a list, then we are done; just return an `item` with `data`
    if (prefix < RLP_PREFIX_LIST) {
        return coderAddContext(coder, createContextItem(coder, data.bytes, data.bytesCount, 0));
    }

    // If a list, then we'll consume `data` with sub-items.
    else {
        // We can have an arbitrary number of sub-times.  Assume we have DEFAULT_ITEM_INCREMENT
        // but be willing to increase the number if needed.
        BRRlpItem itemsArray[DEFAULT_ITEM_INCREMENT];
        uint64_t itemsIndex = 0;
        uint64_t itemsCount = DEFAULT_ITEM_INCREMENT;

        // We'll use this to accumulate subitems.
        BRRlpItem *items = itemsArray;

        // The upper limit on bytes to consume.
        uint8_t *bytesLimit = data.bytes + data.bytesCount;
        uint8_t *bytes = data.bytes;

        // Start of `data` encodes a list with a number of bytes.  We'll start extracting
        // sub-items after the list's length.
        uint8_t bytesOffset = 0;
        uint64_t bytesCount = coderDecodeLength(coder, data.bytes, RLP_PREFIX_LIST, &bytesOffset);
        assert (data.bytesCount == bytesCount + bytesOffset);

        // Start of the first sub-item
        bytes += bytesOffset;
        
        while (bytes < bytesLimit) {
            // Get the `data` for this sub-item and then recurse
            BRRlpData d = rlpGetItem_FillData(coder, bytes);
            items[itemsIndex++] = rlpGetItem (coder, d);

            // Move to the next sub-item
            bytes += d.bytesCount;

            // Extend `items` is we've used the allocated number.
            if (itemsIndex == itemsCount) {
                itemsCount += DEFAULT_ITEM_INCREMENT;
                if (items == itemsArray) {
                    // Move 'off' the stack allocated array.
                    items = malloc(itemsCount * sizeof(BRRlpItem));
                    memcpy (items, itemsArray, itemsIndex * sizeof(BRRlpItem));
                }
                else
                    items = realloc(items, itemsCount * sizeof (BRRlpItem));
            }
        }

        if (items != itemsArray) free(items);
        return coderAddContext(coder, createContextList(coder, data.bytes, data.bytesCount, 0, items, itemsIndex));
    }
}

/*
def rlp_decode(input):
  if len(input) == 0:
    return
  output = ''
  (offset, dataLen, type) = decode_length(input)
  if type is str:
    output = instantiate_str(substr(input, offset, dataLen))
  elif type is list:
    output = instantiate_list(substr(input, offset, dataLen))

  output + rlp_decode(substr(input, offset + dataLen))
  return output

def decode_length(input):
  length = len(input)
  if length == 0:
    raise Exception("input is null")
  prefix = ord(input[0])
  if prefix <= 0x7f:
    return (0, 1, str)
  elif prefix <= 0xb7 and length > prefix - 0x80:
    strLen = prefix - 0x80
    return (1, strLen, str)
  elif prefix <= 0xbf and length > prefix - 0xb7 and length > prefix - 0xb7 + to_integer(substr(input, 1, prefix - 0xb7)):
    lenOfStrLen = prefix - 0xb7
    strLen = to_integer(substr(input, 1, lenOfStrLen))
    return (1 + lenOfStrLen, strLen, str)
  elif prefix <= 0xf7 and length > prefix - 0xc0:
    listLen = prefix - 0xc0;
    return (1, listLen, list)
  elif prefix <= 0xff and length > prefix - 0xf7 and length > prefix - 0xf7 + to_integer(substr(input, 1, prefix - 0xf7)):
    lenOfListLen = prefix - 0xf7
    listLen = to_integer(substr(input, 1, lenOfListLen))
    return (1 + lenOfListLen, listLen, list)
  else:
    raise Exception("input don't conform RLP encoding form")

def to_integer(b)
  length = len(b)
  if length == 0:
    raise Exception("input is null")
  elif length == 1:
    return ord(b[0])
  else:
    return ord(substr(b, -1)) + to_integer(substr(b, 0, -1)) * 256
 */
