//
//  BREthereumLog.c
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

#include "BRArray.h"
#include "BREthereumAccount.h"
#include "BREthereumBase.h"
#include "BREthereumLog.h"

static BREthereumLogTopic empty;

//
// Log Topic
//
static BREthereumLogTopic
logTopicCreateAddress (BREthereumAddressRaw raw) {
    BREthereumLogTopic topic = empty;
    unsigned int addressBytes = sizeof (raw.bytes);
    unsigned int topicBytes = sizeof (topic.bytes);
    assert (topicBytes >= addressBytes);

    memcpy (&topic.bytes[topicBytes - addressBytes], raw.bytes, addressBytes);
    return topic;
}

extern BREthereumBloomFilter
logTopicGetBloomFilter (BREthereumLogTopic topic) {
    BRRlpData data;
    data.bytes = topic.bytes;
    data.bytesCount = sizeof (topic.bytes);
    return bloomFilterCreateData(data);
}

extern BREthereumBloomFilter
logTopicGetBloomFilterAddress (BREthereumAddressRaw address) {
    return logTopicGetBloomFilter (logTopicCreateAddress(address));
}

//
// Support
//
static BREthereumLogTopic
logTopicRlpDecodeItem (BRRlpItem item,
                       BRRlpCoder coder) {
    BREthereumLogTopic topic;

    BRRlpData data = rlpDecodeItemBytes(coder, item);
    assert (32 == data.bytesCount);

    memcpy (topic.bytes, data.bytes, 32);
    return topic;
}

static BRRlpItem
logTopicRlpEncodeItem(BREthereumLogTopic topic,
                      BRRlpCoder coder) {
    return rlpEncodeItemBytes(coder, topic.bytes, 32);
}

static BREthereumLogTopic emptyTopic;

//
// Ethereum Log
//
// A log entry, O, is:
struct BREthereumLogRecord {
    // a tuple of the logger’s address, Oa;
    BREthereumAddressRaw address;

    // a series of 32-byte log topics, Ot;
    BREthereumLogTopic *topics;

    // and some number of bytes of data, Od
    uint8_t *data;
    uint8_t dataCount;
};

extern BREthereumAddressRaw
logGetAddress (BREthereumLog log) {
    return log->address;
}

extern size_t
logGetTopicsCount (BREthereumLog log) {
    return array_count(log->topics);
}

extern  BREthereumLogTopic
logGetTopic (BREthereumLog log, size_t index) {
    return (index < array_count(log->topics)
            ? log->topics[index]
            : emptyTopic);
}

extern BRRlpData
logGetData (BREthereumLog log) {
    BRRlpData data;

    data.bytesCount = log->dataCount;
    data.bytes = malloc (data.bytesCount);
    memcpy (data.bytes, log->data, data.bytesCount);

    return data;
}

//
// Log Topics - RLP Encode/Decode
//
static BRRlpItem
logTopicsRlpEncodeItem (BREthereumLog log,
                        BRRlpCoder coder) {
    size_t itemsCount = array_count(log->topics);
    BRRlpItem items[itemsCount];

    for (int i = 0; i < itemsCount; i++)
        items[i] = logTopicRlpEncodeItem(log->topics[i], coder);

    return rlpEncodeListItems(coder, items, itemsCount);
}

static BREthereumLogTopic *
logTopicsRlpDecodeItem (BRRlpItem item,
                        BRRlpCoder coder) {
    size_t itemsCount = 0;
    const BRRlpItem *items = rlpDecodeList(coder, item, &itemsCount);

    BREthereumLogTopic *topics;
    array_new(topics, itemsCount);

    for (int i = 0; i < itemsCount; i++) {
        BREthereumLogTopic topic = logTopicRlpDecodeItem(items[i], coder);
        array_add(topics, topic);
    }

    return topics;
}

//
// Log - RLP Decode
//
extern BREthereumLog
logRlpDecodeItem (BRRlpItem item,
                  BRRlpCoder coder) {
    BREthereumLog log = (BREthereumLog) calloc (1, sizeof (struct BREthereumLogRecord));

    size_t itemsCount = 0;
    const BRRlpItem *items = rlpDecodeList(coder, item, &itemsCount);
    assert (3 == itemsCount);

    log->address = addressRawRlpDecode(items[0], coder);
    log->topics = logTopicsRlpDecodeItem (items[1], coder);

    BRRlpData data = rlpDecodeItemBytes(coder, items[2]);
    log->data = data.bytes;
    log->dataCount = data.bytesCount;

    return log;
}

extern BREthereumLog
logDecodeRLP (BRRlpData data) {
    BRRlpCoder coder = rlpCoderCreate();
    BRRlpItem item = rlpGetItem (coder, data);

    BREthereumLog log = logRlpDecodeItem(item, coder);

    rlpCoderRelease(coder);
    return log;
}

//
// Log - RLP Encode
//
extern BRRlpItem
logRlpEncodeItem(BREthereumLog log,
                 BRRlpCoder coder) {

    BRRlpItem items[3];

    items[0] = addressRawRlpEncode(log->address, coder);
    items[1] = logTopicsRlpEncodeItem(log, coder);
    items[2] = rlpEncodeItemBytes(coder, log->data, log->dataCount);

    return rlpEncodeListItems(coder, items, 3);
}

extern BRRlpData
logEncodeRLP (BREthereumLog log) {
    BRRlpData result;

    BRRlpCoder coder = rlpCoderCreate();
    BRRlpItem encoding = logRlpEncodeItem(log, coder);

    rlpDataExtract(coder, encoding, &result.bytes, &result.bytesCount);
    rlpCoderRelease(coder);

    return result;
}


