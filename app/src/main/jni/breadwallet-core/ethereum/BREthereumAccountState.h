//
//  BREthereumAccountState.h
//  BRCore
//
//  Created by Ed Gamble on 5/15/18.
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

#ifndef BR_Ethereum_AccountState_H
#define BR_Ethereum_AccountState_H

#include "BREthereumEther.h"
#include "BREthereumBase.h"

#ifdef __cplusplus
extern "C" {
#endif

typedef struct BREthereumAccountStateRecord *BREthereumAccountState;

extern uint64_t
accountStateGetNonce (BREthereumAccountState state);

extern BREthereumEther
accountStateGetBalance (BREthereumAccountState state);

extern BREthereumHash
accountStateGetStorageRoot (BREthereumAccountState state);

extern BREthereumHash
accountStateGetCodeHash (BREthereumAccountState state);

extern BRRlpItem
accountStateRlpEncodeItem (BREthereumAccountState state, BRRlpCoder coder);

extern BREthereumAccountState
accountStateRlpDecodeItem (BRRlpItem item, BRRlpCoder coder);

#ifdef __cplusplus
}
#endif

#endif /* BR_Ethereum_AccountState_H */
