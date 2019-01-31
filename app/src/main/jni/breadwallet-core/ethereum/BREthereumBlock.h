//
//  BBREthereumBlock.h
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 3/23/2018.
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

#ifndef BR_Ethereum_Block_H
#define BR_Ethereum_Block_H

#include "BREthereumBase.h"
#include "BREthereumTransaction.h"
#include "BREthereumBloomFilter.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct BREthereumBlockHeaderRecord *BREthereumBlockHeader;
typedef struct BREthereumBlockRecord *BREthereumBlock;

//
// Block Header
//
extern BREthereumBlockHeader
blockHeaderDecodeRLP (BRRlpData data);

extern BRRlpData
blockHeaderEncodeRLP (BREthereumBlockHeader header,
                      BREthereumBoolean withNonce);

extern BREthereumHash
blockHeaderGetParentHash (BREthereumBlockHeader header);

// ...

extern uint64_t
blockHeaderGetNonce (BREthereumBlockHeader header);

extern BREthereumBoolean
blockHeaderMatch (BREthereumBlockHeader header,
            BREthereumBloomFilter filter);

extern BREthereumBoolean
blockHeaderMatchAddress (BREthereumBlockHeader header,
                   BREthereumAddressRaw address);

//
// Block
//
extern BREthereumBlock
createBlockMinimal(BREthereumHash hash,
            uint64_t number,
            uint64_t timestamp);

extern BREthereumBlock
createBlock (BREthereumBlockHeader header,
             BREthereumBlockHeader ommers[], size_t ommersCount,
             BREthereumTransaction transactions[], size_t transactionCount);

extern BREthereumBlockHeader
blockGetHeader (BREthereumBlock block);

extern unsigned long
blockGetTransactionsCount (BREthereumBlock block);

extern BREthereumTransaction
blockGetTransaction (BREthereumBlock block, unsigned int index);

extern unsigned long
blockGetOmmersCount (BREthereumBlock block);

extern BREthereumBlockHeader
blockGetOmmer (BREthereumBlock block, unsigned int index);

extern BREthereumHash
blockGetHash (BREthereumBlock block);

extern uint64_t
blockGetNumber (BREthereumBlock block);

extern uint64_t
blockGetConfirmations (BREthereumBlock block);

extern uint64_t
blockGetTimestamp (BREthereumBlock block);

extern BRRlpData
blockEncodeRLP (BREthereumBlock block,
                BREthereumNetwork network);

extern BREthereumBlock
blockDecodeRLP (BRRlpData data,
                BREthereumNetwork network);

//
// Genesis Blocks
//
extern const BREthereumBlockHeader ethereumMainnetBlockHeader;
extern const BREthereumBlockHeader ethereumTestnetBlockHeader;
extern const BREthereumBlockHeader ethereumRinkebyBlockHeader;

extern const BREthereumBlockHeader
networkGetGenesisBlockHeader (BREthereumNetwork network);
    
#ifdef __cplusplus
}
#endif

#endif /* BR_Ethereum_Block_H */
