//
//  BREthereumTransactionReceipt.c
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

#include <string.h>
#include <assert.h>
#include "BRArray.h"
#include "BREthereumTransactionReceipt.h"

// The transaction receipt, R, is a tuple of four items comprising: ...
//
// However, there appears to be a change in interpretation for 'status code' and the
// order is not consistent with the 'Yellow Paper'
struct BREthereumTransactionReceiptRecord {
    // the cumulative gas used in the block containing the transaction receipt as of
    // immediately after the transaction has happened, Ru,
    uint64_t gasUsed;

    // the set of logs created through execution of the transaction, Rl
    BREthereumLog *logs;

    // the Bloom filter composed from information in those logs, Rb
    BREthereumBloomFilter bloomFilter;

    // and the status code of the transaction, Rz
    BRRlpData stateRoot;
};

//
// Bloom Filter Matches
//
extern BREthereumBoolean
transactionReceiptMatch (BREthereumTransactionReceipt receipt,
                         BREthereumBloomFilter filter) {
    return bloomFilterMatch(receipt->bloomFilter, filter);
}

extern BREthereumBoolean
transactionReceiptMatchAddress (BREthereumTransactionReceipt receipt,
                                BREthereumAddressRaw address) {
    return transactionReceiptMatch(receipt, logTopicGetBloomFilterAddress(address));
}

//
// Transaction Receipt Logs - RLP Encode/Decode
//
static BRRlpItem
transactionReceiptLogsRlpEncodeItem (BREthereumTransactionReceipt log,
                        BRRlpCoder coder) {
    size_t itemsCount = array_count(log->logs);
    BRRlpItem items[itemsCount];

    for (int i = 0; i < itemsCount; i++)
        items[i] = logRlpEncodeItem(log->logs[i], coder);

    return rlpEncodeListItems(coder, items, itemsCount);
}

static BREthereumLog *
transactionReceiptLogsRlpDecodeItem (BRRlpItem item,
                                     BRRlpCoder coder) {
    size_t itemsCount = 0;
    const BRRlpItem *items = rlpDecodeList(coder, item, &itemsCount);

    BREthereumLog *logs;
    array_new(logs, itemsCount);

    for (int i = 0; i < itemsCount; i++) {
        BREthereumLog log = logRlpDecodeItem(items[i], coder);
        array_add(logs, log);
    }

    return logs;
}

//
// Transaction Receipt - RLP Decode
//
static BREthereumTransactionReceipt
transactionReceiptRlpDecodeItem (BRRlpItem item,
                                 BRRlpCoder coder) {
    BREthereumTransactionReceipt receipt = calloc (1, sizeof(struct BREthereumTransactionReceiptRecord));
    memset (receipt, 0, sizeof(struct BREthereumTransactionReceiptRecord));

    size_t itemsCount = 0;
    const BRRlpItem *items = rlpDecodeList(coder, item, &itemsCount);
    assert (4 == itemsCount);

    receipt->stateRoot = rlpDecodeItemBytes(coder, items[0]);
    receipt->gasUsed = rlpDecodeItemUInt64(coder, items[1], 0);
    receipt->bloomFilter = bloomFilterRlpDecode(items[2], coder);
    receipt->logs = transactionReceiptLogsRlpDecodeItem(items[3], coder);

    return receipt;
}

extern BREthereumTransactionReceipt
transactionReceiptDecodeRLP (BRRlpData data) {
    BRRlpCoder coder = rlpCoderCreate();
    BRRlpItem item = rlpGetItem (coder, data);

    BREthereumTransactionReceipt receipt = transactionReceiptRlpDecodeItem(item, coder);

    rlpCoderRelease(coder);
    return receipt;
}

//
// Transaction Receipt - RLP Encode
//
static BRRlpItem
transactionReceiptRlpEncodeItem(BREthereumTransactionReceipt receipt,
                                BRRlpCoder coder) {
    BRRlpItem items[4];

    items[0] = rlpEncodeItemBytes(coder, receipt->stateRoot.bytes, receipt->stateRoot.bytesCount);
    items[1] = rlpEncodeItemUInt64(coder, receipt->gasUsed, 0);
    items[2] = bloomFilterRlpEncode(receipt->bloomFilter, coder);
    items[3] = transactionReceiptLogsRlpEncodeItem(receipt, coder);

    return rlpEncodeListItems(coder, items, 4);
}

extern BRRlpData
transactionReceiptEncodeRLP (BREthereumTransactionReceipt receipt) {
    BRRlpData result;

    BRRlpCoder coder = rlpCoderCreate();
    BRRlpItem encoding = transactionReceiptRlpEncodeItem(receipt, coder);

    rlpDataExtract(coder, encoding, &result.bytes, &result.bytesCount);
    rlpCoderRelease(coder);

    return result;
}
