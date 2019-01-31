//
//  BREthereumLog.h
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

#ifndef BR_Ethereum_Log_h
#define BR_Ethereum_Log_h

#include "BREthereumBase.h"
#include "BREthereumAccount.h"
#include "BREthereumBloomFilter.h"

#ifdef __cplusplus
extern "C" {
#endif

    //
    // Log Topic
    //
typedef struct {
    uint8_t bytes[32];
} BREthereumLogTopic;

extern BREthereumBloomFilter
logTopicGetBloomFilter (BREthereumLogTopic topic);

extern BREthereumBloomFilter
logTopicGetBloomFilterAddress (BREthereumAddressRaw address);

    //
    // Log
    //
typedef struct BREthereumLogRecord *BREthereumLog;

extern BREthereumAddressRaw
logGetAddress (BREthereumLog log);

extern size_t
logGetTopicsCount (BREthereumLog log);

extern  BREthereumLogTopic
logGetTopic (BREthereumLog log, size_t index);

extern BRRlpData
logGetData (BREthereumLog log);
    
extern BREthereumLog
logRlpDecodeItem (BRRlpItem item,
                  BRRlpCoder coder);
/**
 * [QUASI-INTERNAL - used by BREthereumBlock]
 */
extern BRRlpItem
logRlpEncodeItem(BREthereumLog log,
                 BRRlpCoder coder);

extern BRRlpData
logEncodeRLP (BREthereumLog log);

extern BREthereumLog
logDecodeRLP (BRRlpData data);

#ifdef __cplusplus
}
#endif

#endif /* BR_Ethereum_Log_h */
