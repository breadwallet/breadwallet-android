//
//  BREthereumAccountState.c
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

#include <stdlib.h>
#include <assert.h>
#include "BREthereumAccountState.h"

// The account state, σ[a], comprises the following four fields:
struct BREthereumAccountStateRecord {
    // A scalar value equal to the number of trans- actions sent from this address or, in the
    // case of accounts with associated code, the number of contract-creations made by this
    // account. For ac- count of address a in state σ, this would be for- mally denoted σ[a]n.
    uint64_t nonce;

    // A scalar value equal to the number of Wei owned by this address. Formally denoted σ[a]b.
    BREthereumEther balance;

    // A 256-bit hash of the root node of a Merkle Patricia tree that encodes the storage contents
    // of the account (a mapping between 256-bit integer values), encoded into the trie as a
    // mapping from the Keccak 256-bit hash of the 256-bit integer keys to the RLP-encoded 256-bit
    // integer values. The hash is formally denoted σ[a]s.
    BREthereumHash storageRoot;

    // The hash of the EVM code of this account this is the code that gets executed should this
    // address receive a message call; it is immutable and thus, unlike all other fields, cannot
    // be changed after construction. All such code fragments are contained in the state database
    // under their corresponding hashes for later retrieval. This hash is formally denoted σ[a]c,
    // and thus the code may be denoted as b, given that KEC(b) = σ[a]c.
    BREthereumHash codeHash;
};

extern uint64_t
accountStateGetNonce (BREthereumAccountState state) {
    return state->nonce;
}

extern BREthereumEther
accountStateGetBalance (BREthereumAccountState state) {
    return state->balance;
}

extern BREthereumHash
accountStateGetStorageRoot (BREthereumAccountState state) {
    return state->storageRoot;
}

extern BREthereumHash
accountStateGetCodeHash (BREthereumAccountState state) {
    return state->codeHash;
}

extern BREthereumAccountState
accountStateCreate (uint64_t nonce,
                    BREthereumEther balance,
                    BREthereumHash storageRoot,
                    BREthereumHash codeHash) {

    BREthereumAccountState state = (BREthereumAccountState) calloc (1, sizeof (struct BREthereumAccountStateRecord));

    state->nonce = nonce;
    state->balance = balance;
    state->storageRoot = storageRoot;
    state->codeHash = codeHash;

    return state;
}

extern BRRlpItem
accountStateRlpEncodeItem(BREthereumAccountState state, BRRlpCoder coder) {
    BRRlpItem items[4];

    items[0] = rlpEncodeItemUInt64(coder, state->nonce, 0);
    items[1] = etherRlpEncode(state->balance, coder);
    items[2] = hashRlpEncode(state->storageRoot, coder);
    items[3] = hashRlpEncode(state->codeHash, coder);

    return rlpEncodeListItems(coder, items, 4);
}

extern BREthereumAccountState
accountStateRlpDecodeItem (BRRlpItem item, BRRlpCoder coder) {
    BREthereumAccountState state = (BREthereumAccountState) calloc (1, sizeof (struct BREthereumAccountStateRecord));

    size_t itemsCount = 0;
    const BRRlpItem *items = rlpDecodeList(coder, item, &itemsCount);
    assert (4 == itemsCount);

    state->nonce = rlpDecodeItemUInt64(coder, items[0], 0);
    state->balance = etherRlpDecode(items[1], coder);
    state->storageRoot = hashRlpDecode(items[2], coder);
    state->codeHash = hashRlpDecode(items[3], coder);

    return state;
}

