//
//  BBREthereumTransaction.c
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

#include <stdlib.h>
#include <string.h>
#include <assert.h>

#include "BREthereumTransaction.h"
#include "BREthereumAmount.h"
#include "BREthereumAccount.h"
#include "BREthereumPrivate.h"

#define MAX(a,b) ((a) > (b) ? (a) : (b))

// Forward Declarations
static void
provideData (BREthereumTransaction transaction);

static void
provideGasEstimate (BREthereumTransaction transaction);

//
// Transaction State
//
typedef struct {
    BREthereumTransactionStatus status;
    union {
        struct {
            int foo;
        } created;

        struct {
            int foo;
        } _signed;

        struct {
            BREthereumGas gasUsed;
            uint64_t number;
            uint64_t timestamp;
            uint64_t transactionIndex;
        } blocked;

        struct {
            int foo;
        } dropped;

        struct {
            int foo;
        } submitted;
    } u;
} BREthereumTransactionState;

static void
transactionStateCreated (BREthereumTransactionState *state /* ... */) {
    state->status = TRANSACTION_CREATED;
}

static void
transactionStateSigned (BREthereumTransactionState *state /* ... */) {
    state->status = TRANSACTION_SIGNED;
}

static void
transactionStateSubmitted (BREthereumTransactionState *state /* ... */) {
    state->status = TRANSACTION_SUBMITTED;
}

static void
transactionStateBlocked(BREthereumTransactionState *state,
                        BREthereumGas gasUsed,
                        uint64_t blockNumber,
                        uint64_t blockTimestamp,
                         uint64_t blockTransactionIndex) {

    // Ensure blockConfirmations is the maximum seen.

    state->status = TRANSACTION_BLOCKED;
    state->u.blocked.gasUsed = gasUsed;
    state->u.blocked.number = blockNumber;
    state->u.blocked.timestamp = blockTimestamp;
    state->u.blocked.transactionIndex = blockTransactionIndex;
}

static void
transactionStateDropped (BREthereumTransactionState *state /* ... */) {
    state->status = TRANSACTION_DROPPED;
}


/**
 * An Ethereum Transaction ...
 *
 */
struct BREthereumTransactionRecord {

    //
    //
    //
    BREthereumAddress sourceAddress;

    //
    //
    //
    BREthereumAddress targetAddress;

    /**
     * The amount transferred from sourceAddress to targetAddress.  Note that this is not
     * necessarily the 'amount' RLP encoded in the 'raw transaction'.  Specifically if the `amount`
     * is for TOKEN, then the RLP encoded amount is 0 and the RLP encoded data for the ERC20
     * transfer function encodes the amount.
     */
    BREthereumAmount amount;
    BREthereumGasPrice gasPrice;
    BREthereumGas gasLimit;
    uint64_t nonce;
    BREthereumChainId chainId;   // EIP-135 - chainId - "Since EIP-155 use chainId for v"

    /**
     *
     */
    char *data;

    /**
     * The transaction's hash.   This will be 'empty' until the transaction is submitted.
     */
    BREthereumHash hash;

    /**
     * The estimated amount of Gas needed to process this transaction.
     */
    BREthereumGas gasEstimate;

    /**
     * The signature, if signed (signer is not NULL).  This is a 'VRS' signature.
     */
    BREthereumSignature signature;

    //
    // State
    //
    BREthereumTransactionState state;
};

extern BREthereumTransaction
transactionCreate(BREthereumAddress sourceAddress,
                  BREthereumAddress targetAddress,
                  BREthereumAmount amount,
                  BREthereumGasPrice gasPrice,
                  BREthereumGas gasLimit,
                  uint64_t nonce) {
    BREthereumTransaction transaction = calloc (1, sizeof (struct BREthereumTransactionRecord));

    transactionStateCreated(&transaction->state);
    transaction->sourceAddress = sourceAddress;
    transaction->targetAddress = targetAddress;
    transaction->amount = amount;
    transaction->gasPrice = gasPrice;
    transaction->gasLimit = gasLimit;
    transaction->nonce = nonce;
    transaction->chainId = 0;
    transaction->hash = hashCreateEmpty();

    provideData(transaction);
    provideGasEstimate(transaction);

    return transaction;
}

extern BREthereumAddress
transactionGetSourceAddress(BREthereumTransaction transaction) {
    return transaction->sourceAddress;
}

extern BREthereumAddress
transactionGetTargetAddress(BREthereumTransaction transaction) {
    return transaction->targetAddress;
}

extern BREthereumAmount
transactionGetAmount(BREthereumTransaction transaction) {
    return transaction->amount;
}

extern BREthereumEther
transactionGetFee (BREthereumTransaction transaction, int *overflow) {
    return etherCreate
    (mulUInt256_Overflow(transaction->gasPrice.etherPerGas.valueInWEI,
                         createUInt256 (ETHEREUM_BOOLEAN_IS_TRUE(transactionIsConfirmed(transaction))
                                        ? transaction->state.u.blocked.gasUsed.amountOfGas
                                        : transaction->gasEstimate.amountOfGas),
                         overflow));
}

extern BREthereumEther
transactionGetFeeLimit (BREthereumTransaction transaction, int *overflow) {
    return etherCreate
    (mulUInt256_Overflow(transaction->gasPrice.etherPerGas.valueInWEI,
                         createUInt256 (transaction->gasLimit.amountOfGas),
                         overflow));
}

extern BREthereumGasPrice
transactionGetGasPrice (BREthereumTransaction transaction) {
    return transaction->gasPrice;
}

extern void
transactionSetGasPrice (BREthereumTransaction transaction,
                        BREthereumGasPrice gasPrice) {
    transaction->gasPrice = gasPrice;
}

extern BREthereumGas
transactionGetGasLimit (BREthereumTransaction transaction) {
    return transaction->gasLimit;
}

extern void
transactionSetGasLimit (BREthereumTransaction transaction,
                        BREthereumGas gasLimit) {
    transaction->gasLimit = gasLimit;
}

extern BREthereumGas
transactionGetGasEstimate (BREthereumTransaction transaction) {
    return transaction->gasEstimate;
}

extern void
transactionSetGasEstimate (BREthereumTransaction transaction,
                           BREthereumGas gasEstimate) {
    transaction->gasEstimate = gasEstimate;
}

static void
provideGasEstimate (BREthereumTransaction transaction) {
    transactionSetGasEstimate(transaction, amountGetGasEstimate(transaction->amount));
}

extern uint64_t
transactionGetNonce (BREthereumTransaction transaction) {
    return transaction->nonce;
}

private_extern void
transactionSetNonce (BREthereumTransaction transaction,
                     uint64_t nonce) {
    transaction->nonce = nonce;
}

extern BREthereumToken
transactionGetToken (BREthereumTransaction transaction) {
    return (AMOUNT_ETHER == amountGetType(transaction->amount)
            ? NULL
            : tokenQuantityGetToken(amountGetTokenQuantity(transaction->amount)));
}

//
// Data
//
extern const char *
transactionGetData (BREthereumTransaction transaction) {
    return transaction->data;
}

static void
provideData (BREthereumTransaction transaction) {
    if (NULL == transaction->data) {
        switch (amountGetType (transaction->amount)) {
            case AMOUNT_ETHER:
                transaction->data = "";
                break;
            case AMOUNT_TOKEN: {
                UInt256 value = amountGetTokenQuantity(transaction->amount).valueAsInteger;
                const char *address = addressAsString(transaction->targetAddress);

                // Data is a HEX ENCODED string
                transaction->data = (char *) contractEncode
                (contractERC20, functionERC20Transfer,
                 // Address
                 (uint8_t *) &address[2], strlen(address) - 2,
                 // Amount
                 (uint8_t *) &value, sizeof (UInt256),
                 NULL);

                free ((char *) address);
            }
        }
    }
}

//
// Sign
//
extern void
transactionSign(BREthereumTransaction transaction,
                BREthereumAccount account,
                BREthereumSignature signature) {
    transactionStateSigned(&transaction->state);
    transaction->signature = signature;

    // The signature algorithm does not account for EIP-155 and thus the chainID.  We are signing
    // transactions according to EIP-155.  Thus v = CHAIN_ID * 2 + 35 or v = CHAIN_ID * 2 + 36
    // whereas the non-EIP-155 value of v is { 27, 28 }
    assert (SIGNATURE_TYPE_RECOVERABLE == signature.type);
    assert (27 == signature.sig.recoverable.v || 28 == signature.sig.recoverable.v);
}

extern BREthereumBoolean
transactionIsSigned (BREthereumTransaction transaction) {
    switch (transaction->signature.type) {
        case SIGNATURE_TYPE_FOO:
            return AS_ETHEREUM_BOOLEAN (transaction->signature.sig.foo.ignore != 0);
        case SIGNATURE_TYPE_RECOVERABLE:
            return AS_ETHEREUM_BOOLEAN (transaction->signature.sig.recoverable.v != 0);
    }
}

extern const BREthereumHash
transactionGetHash (BREthereumTransaction transaction) {
    return transaction->hash;
}

extern BREthereumSignature
transactionGetSignature (BREthereumTransaction transaction) {
    return transaction->signature;
}

static BREthereumAddress emptyAddress;

extern BREthereumAddress
transactionExtractAddress (BREthereumTransaction transaction,
                           BREthereumNetwork network)
{
    if (ETHEREUM_BOOLEAN_IS_FALSE (transactionIsSigned(transaction))) return emptyAddress;

    int success = 1;
    BRRlpData unsignedRLPData = transactionEncodeRLP(transaction, network, TRANSACTION_RLP_UNSIGNED);

    return signatureExtractAddress(transaction->signature,
                                   unsignedRLPData.bytes,
                                   unsignedRLPData.bytesCount,
                                   &success);
}

//
// RLP Encode / Decode
//
static BRRlpItem
transactionEncodeDataForHolding (BREthereumTransaction transaction,
                                 BREthereumAmount holding,
                                 BRRlpCoder coder) {
    return (NULL == transaction->data || 0 == strlen(transaction->data)
            ? rlpEncodeItemString(coder, "")
            : rlpEncodeItemHexString(coder, transaction->data));
}

static BRRlpItem
transactionEncodeAddressForHolding (BREthereumTransaction transaction,
                                    BREthereumAmount holding,
                                    BRRlpCoder coder) {
    switch (amountGetType(holding)) {
        case AMOUNT_ETHER:
            return addressRlpEncode(transaction->targetAddress, coder);
        case AMOUNT_TOKEN: {
            BREthereumToken token = tokenQuantityGetToken (amountGetTokenQuantity(holding));
            BREthereumAddress contractAddress = createAddress(tokenGetAddress(token));
            BRRlpItem result = addressRlpEncode(contractAddress, coder);
            addressFree(contractAddress);
            return result;
        }
    }
}

static BRRlpItem
transactionEncodeNonce (BREthereumTransaction transaction,
                        uint64_t nonce,
                        BRRlpCoder coder) {
    return rlpEncodeItemUInt64(coder, nonce, 1);
}

static uint64_t
transactionDecodeNonce (BRRlpItem item,
                        BRRlpCoder coder) {
    return rlpDecodeItemUInt64(coder, item, 1);
}

//
// Tranaction RLP Encode
//
extern BRRlpItem
transactionRlpEncodeItem(BREthereumTransaction transaction,
                         BREthereumNetwork network,
                         BREthereumTransactionRLPType type,
                         BRRlpCoder coder) {
    BRRlpItem items[10];
    size_t itemsCount = 0;

    items[0] = transactionEncodeNonce(transaction, transaction->nonce, coder);
    items[1] = gasPriceRlpEncode(transaction->gasPrice, coder);
    items[2] = gasRlpEncode(transaction->gasLimit, coder);
    items[3] = transactionEncodeAddressForHolding(transaction, transaction->amount, coder);
    items[4] = amountRlpEncode(transaction->amount, coder);
    items[5] = transactionEncodeDataForHolding(transaction, transaction->amount, coder);
    itemsCount = 6;

    // EIP-155:
    // If block.number >= FORK_BLKNUM and v = CHAIN_ID * 2 + 35 or v = CHAIN_ID * 2 + 36, then when
    // computing the hash of a transaction for purposes of signing or recovering, instead of hashing
    // only the first six elements (i.e. nonce, gasprice, startgas, to, value, data), hash nine
    // elements, with v replaced by CHAIN_ID, r = 0 and s = 0. The currently existing signature
    // scheme using v = 27 and v = 28 remains valid and continues to operate under the same rules
    // as it does now.

    transaction->chainId = networkGetChainId(network);

    switch (type) {
        case TRANSACTION_RLP_UNSIGNED:
            // For EIP-155, encode { v, r, s } with v as the chainId and both r and s as empty.
            items[6] = rlpEncodeItemUInt64(coder, transaction->chainId, 1);
            items[7] = rlpEncodeItemString(coder, "");
            items[8] = rlpEncodeItemString(coder, "");
            itemsCount += 3;
            break;

        case TRANSACTION_RLP_SIGNED:
            // For EIP-155, encode v with the chainID.
            items[6] = rlpEncodeItemUInt64(coder, transaction->signature.sig.recoverable.v + 8 +
                                           2 * transaction->chainId, 1);

            items[7] = rlpEncodeItemBytes (coder,
                                           transaction->signature.sig.recoverable.r,
                                           sizeof (transaction->signature.sig.recoverable.r));

            items[8] = rlpEncodeItemBytes (coder,
                                           transaction->signature.sig.recoverable.s,
                                           sizeof (transaction->signature.sig.recoverable.s));
            itemsCount += 3;
            break;
    }

    return rlpEncodeListItems(coder, items, itemsCount);
}



extern BRRlpData
transactionEncodeRLP (BREthereumTransaction transaction,
                      BREthereumNetwork network,
                      BREthereumTransactionRLPType type) {
    BRRlpData result;

    BRRlpCoder coder = rlpCoderCreate();
    BRRlpItem encoding = transactionRlpEncodeItem(transaction, network, type, coder);

    rlpDataExtract(coder, encoding, &result.bytes, &result.bytesCount);
    rlpCoderRelease(coder);

    return result;
}

//
// Tranaction RLP Decode
//
extern BREthereumTransaction
transactionRlpDecodeItem (BRRlpItem item,
                          BREthereumNetwork network,
                          BREthereumTransactionRLPType type,
                          BRRlpCoder coder) {

    BREthereumTransaction transaction = calloc (1, sizeof(struct BREthereumTransactionRecord));

    size_t itemsCount = 0;
    const BRRlpItem *items = rlpDecodeList(coder, item, &itemsCount);
    assert (9 == itemsCount);

    // Encoded as:
    //    items[0] = transactionEncodeNonce(transaction, transaction->nonce, coder);
    //    items[1] = gasPriceRlpEncode(transaction->gasPrice, coder);
    //    items[2] = gasRlpEncode(transaction->gasLimit, coder);
    //    items[3] = transactionEncodeAddressForHolding(transaction, transaction->amount, coder);
    //    items[4] = amountRlpEncode(transaction->amount, coder);
    //    items[5] = transactionEncodeDataForHolding(transaction, transaction->amount, coder);

    transaction->nonce = transactionDecodeNonce(items[0], coder);
    transaction->gasPrice = gasPriceRlpDecode(items[1], coder);
    transaction->gasLimit = gasRlpDecode(items[2], coder);

    char *strData = rlpDecodeItemHexString (coder, items[5], "0x");
    assert (NULL != strData);
    if ('\0' == strData[0] || 0 == strcmp (strData, "0x")) {
        // This is a ETHER transfer
        transaction->targetAddress = addressRlpDecode(items[3], coder);
        transaction->amount = amountRlpDecodeAsEther(items[4], coder);
        transaction->data = strData;
    }
    else {
        // This is a TOKEN transfer.

        BREthereumAddress contractAddr = addressRlpDecode(items[3], coder);
        BREthereumToken token = tokenLookup(addressAsString (contractAddr));

        // Confirm `strData` encodes functionERC20Transfer
        BREthereumContractFunction function = contractLookupFunctionForEncoding(contractERC20, strData);
        if (NULL == token || function != functionERC20Transfer) {
            free (transaction);
            return NULL;
        }

        BRCoreParseStatus status = CORE_PARSE_OK;
        UInt256 amount = functionERC20TransferDecodeAmount (function, strData, &status);
        char *recvAddr = functionERC20TransferDecodeAddress(function, strData);

        if (CORE_PARSE_OK != status) {
            free (transaction);
            return NULL;
        }

        transaction->amount = amountCreateToken(createTokenQuantity(token, amount));
        transaction->targetAddress = createAddress(recvAddr);

        free (recvAddr);
    }

    transaction->chainId = networkGetChainId(network);

    uint64_t eipChainId = rlpDecodeItemUInt64(coder, items[6], 1);

    if (eipChainId == transaction->chainId) {
        // TRANSACTION_RLP_UNSIGNED
        transaction->signature.type = SIGNATURE_TYPE_RECOVERABLE;
    }
    else {
        // TRANSACTION_RLP_SIGNED
        transaction->signature.type = SIGNATURE_TYPE_RECOVERABLE;

        // If we are RLP decoding a transactino prior to EIP-xxx, then the eipChainId will
        // not be encoded with the chainId.  In that case, just use the eipChainId
        transaction->signature.sig.recoverable.v = (eipChainId > 30
                                                    ? eipChainId - 8 - 2 * transaction->chainId
                                                    : eipChainId);

        BRRlpData rData = rlpDecodeItemBytes (coder, items[7]);
        assert (32 == rData.bytesCount);
        memcpy (transaction->signature.sig.recoverable.r, rData.bytes, rData.bytesCount);

        BRRlpData sData = rlpDecodeItemBytes (coder, items[8]);
        assert (32 == sData.bytesCount);
        memcpy (transaction->signature.sig.recoverable.s, sData.bytes, sData.bytesCount);

        // :fingers-crossed:
        transaction->sourceAddress = transactionExtractAddress(transaction, network);
    }

    return transaction;
}


extern BREthereumTransaction
transactionDecodeRLP (BREthereumNetwork network,
                      BREthereumTransactionRLPType type,
                      BRRlpData data) {
    BRRlpCoder coder = rlpCoderCreate();
    BRRlpItem item = rlpGetItem (coder, data);

    BREthereumTransaction transaction = transactionRlpDecodeItem(item, network, type, coder);

    rlpCoderRelease(coder);
    return transaction;
}

//
// Private
//
private_extern BREthereumEther
transactionGetEffectiveAmountInEther (BREthereumTransaction transaction) {
    switch (amountGetType(transaction->amount)) {
        case AMOUNT_TOKEN:
            return etherCreate(UINT256_ZERO);
        case AMOUNT_ETHER:
            return transaction->amount.u.ether;
    }
}

extern BREthereumTransactionStatus
transactionGetStatus (BREthereumTransaction transaction) {
    return transaction->state.status;
}

extern BREthereumBoolean
transactionIsConfirmed (BREthereumTransaction transaction) {
    return (transaction->state.status == TRANSACTION_BLOCKED
            ? ETHEREUM_BOOLEAN_TRUE
            : ETHEREUM_BOOLEAN_FALSE);
}

extern BREthereumBoolean
transactionIsSubmitted (BREthereumTransaction transaction) {
    switch (transaction->state.status) {
        case TRANSACTION_CREATED:
            return ETHEREUM_BOOLEAN_FALSE;
        case TRANSACTION_SIGNED:
        case TRANSACTION_SUBMITTED:
        case TRANSACTION_BLOCKED:
        case TRANSACTION_DROPPED:
            return ETHEREUM_BOOLEAN_TRUE;
    }
}

extern void
transactionAnnounceBlocked(BREthereumTransaction transaction,
                           BREthereumGas gasUsed,
                           uint64_t blockNumber,
                           uint64_t blockTimestamp,
                           uint64_t blockTransactionIndex) {
    transactionStateBlocked(&transaction->state, gasUsed,
                            blockNumber,
                            blockTimestamp,
                            blockTransactionIndex);
}

extern void
transactionAnnounceDropped (BREthereumTransaction transaction,
                            int foo) {
    transactionStateDropped(&transaction->state);
}

extern void
transactionAnnounceSubmitted (BREthereumTransaction transaction,
                              BREthereumHash hash) {
    transactionStateSubmitted(&transaction->state);
    transaction->hash = hashCopy(hash);
}

static int
transactionHasStatus(BREthereumTransaction transaction,
                     BREthereumTransactionStatus status) {
    return status == transaction->state.status;
}

extern int
transactionExtractBlocked(BREthereumTransaction transaction, BREthereumGas *gas,
                          uint64_t *blockNumber,
                          uint64_t *blockTimestamp,
                          uint64_t *blockTransactionIndex) {
    if (!transactionHasStatus(transaction, TRANSACTION_BLOCKED))
        return 0;

    if (NULL != gas) *gas = transaction->state.u.blocked.gasUsed;
    if (NULL != blockNumber) *blockNumber = transaction->state.u.blocked.number;
    if (NULL != blockTimestamp) *blockTimestamp = transaction->state.u.blocked.timestamp;
    if (NULL != blockTransactionIndex) *blockTransactionIndex = transaction->state.u.blocked.transactionIndex;

    return 1;
}

/**
 * Compare two transactions based on their block, or if not blocked, their nonce.
 * @param t1
 * @param t2
 * @return
 */
extern BREthereumComparison
transactionCompare(BREthereumTransaction t1,
                   BREthereumTransaction t2) {
    int t1Blocked = transactionHasStatus(t1, TRANSACTION_BLOCKED);
    int t2Blocked = transactionHasStatus(t2, TRANSACTION_BLOCKED);

    if (t1Blocked && t2Blocked)
        return (t1->state.u.blocked.number < t2->state.u.blocked.number
                ? ETHEREUM_COMPARISON_LT
                : (t1->state.u.blocked.number > t2->state.u.blocked.number
                   ? ETHEREUM_COMPARISON_GT
                   : (t1->state.u.blocked.transactionIndex < t2->state.u.blocked.transactionIndex
                      ? ETHEREUM_COMPARISON_LT
                      : (t1->state.u.blocked.transactionIndex > t2->state.u.blocked.transactionIndex
                         ? ETHEREUM_COMPARISON_GT
                         : ETHEREUM_COMPARISON_EQ))));

    else if (!t1Blocked && t2Blocked)
        return ETHEREUM_COMPARISON_GT;

    else if (t1Blocked && !t2Blocked)
        return ETHEREUM_COMPARISON_LT;
    
    else
        return (t1->nonce < t2->nonce
                ? ETHEREUM_COMPARISON_LT
                : (t1->nonce > t2->nonce
                   ? ETHEREUM_COMPARISON_GT
                   : ETHEREUM_COMPARISON_EQ));
}


//
// Transaction Result
//
struct BREthereumTransactionResult {
    BREthereumTransaction transaction;
    BREthereumGas gas;
    // block hash
    // block number
    // transaction index
};

/*
     https://github.com/ethereum/pyethereum/blob/develop/ethereum/transactions.py#L22
     https://github.com/ethereum/pyrlp/blob/develop/rlp/sedes/lists.py#L135

     A transaction is stored as:
    [nonce, gasprice, startgas, to, value, data, v, r, s]
    nonce is the number of transactions already sent by that account, encoded
    in binary form (eg.  0 -> '', 7 -> '\x07', 1000 -> '\x03\xd8').
    (v,r,s) is the raw Electrum-style signature of the transaction without the
    signature made with the private key corresponding to the sending account,
    with 0 <= v <= 3. From an Electrum-style signature (65 bytes) it is
    possible to extract the public key, and thereby the address, directly.
    A valid transaction is one where:
    (i) the signature is well-formed (ie. 0 <= v <= 3, 0 <= r < P, 0 <= s < N,
        0 <= r < P - N if v >= 2), and
    (ii) the sending account has enough funds to pay the fee and the value.
    """

    fields = [
        ('nonce', big_endian_int),
        ('gasprice', big_endian_int),
        ('startgas', big_endian_int),
        ('to', utils.address),
        ('value', big_endian_int),
        ('data', binary),
        ('v', big_endian_int),
        ('r', big_endian_int),
        ('s', big_endian_int),
    ]

 */


/*
 $ curl -X POST -H  "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_getTransactionByHash","params":["0x3104b0ee2aba4197f4da656d6144e5978c0b7bcb08890ed7bd6228bc9dbe745e"],"id":1}' http://localhost:8545
    {"jsonrpc":"2.0","id":1,
    "result":{"blockHash":"0xbf197f8ce876514b8922af10824efba5b4ce3fc7ab9ef97443ef9c56bd0cae32",
        "blockNumber":"0x1b930a",
        "from":"0x888197521cfe05ff89960c50012252008819b2cb",
        "gas":"0x1d8a8",
        "gasPrice":"0x4a817c800",
        "hash":"0x3104b0ee2aba4197f4da656d6144e5978c0b7bcb08890ed7bd6228bc9dbe745e",
        "input":"0x",
        "nonce":"0x0",
        "to":"0xf8e60edd24bc15f32bb4260ec2cea7c54cced121",
        "transactionIndex":"0x3",
        "value":"0xde0b6b3a7640000",
        "v":"0x2b",
        "r":"0xa571650cb08199d808b6646f634a8f7431cfd103a243654263faf2518e3efd40",
        "s":"0x4d2774147ccb90d1e7ad9358eb895c5f5d24db26b9d3e880bcee4fa06e5b3e1b"}}
 */

/*
 Signing

 https://bitcoin.stackexchange.com/questions/38351/ecdsa-v-r-s-what-is-v

 > msgSha = web3.sha3('Now it the time')
"0x8b3942af68acfd875239181babe9ce093c420ca78d15b178fb63cf839dcf0971"

 > personal.unlockAccount(eth.accounts[<n>], "password", 3600)

 $ curl -X POST -H  "Content-Type: application/json" --data '{"jsonrpc":"2.0","method":"eth_sign","
    params":["0xf8e60edd24bc15f32bb4260ec2cea7c54cced121", "0x8b3942af68acfd875239181babe9ce093c420ca78d15b178fb63cf839dcf0971"],
    "id":1}'
    http://localhost:8545

 {"jsonrpc":"2.0","id":1,
    "result":"0xe79ba93e981e8ee50b8d07b0be7ae4526bc4d9bf7dcffe88dff62c502b2a126d7f772e2374869b41b0f5c0061d6d828348c96a7021f0c3227e73431d8ebbf1331b"}
 */

/*
 * r, s, v

  signature = signature.substr(2); //remove 0x
  const r = '0x' + signature.slice(0, 64)
  const s = '0x' + signature.slice(64, 128)
  const v = '0x' + signature.slice(128, 130)
  const v_decimal = web3.toDecimal(v)

 > web3.eth.sign ("0xf8e60edd24bc15f32bb4260ec2cea7c54cced121", "0x8b3942af68acfd875239181babe9ce093c420ca78d15b178fb63cf839dcf0971")
"0xe79ba93e981e8ee50b8d07b0be7ae4526bc4d9bf7dcffe88dff62c502b2a126d7f772e2374869b41b0f5c0061d6d828348c96a7021f0c3227e73431d8ebbf1331b"
 */

/*
 > msg = 'Try again'
 > msgSha = web3.sha3(msg)
 > sig = web3.eth.sign ("0xf8e60edd24bc15f32bb4260ec2cea7c54cced121", msgSha) // account, sha-ed msg
 > personal.ecRecover (msgSha, sig)
 "0xf8e60edd24bc15f32bb4260ec2cea7c54cced121"
 > eth.accounts[1]
 "0xf8e60edd24bc15f32bb4260ec2cea7c54cced121"
 */


/*

 ===== RLP Encode - =====

 All this method use some form of rlp.encode(<transaction>, ...)
   sigHash = utils.sha3 (rlp.encode (self, UnsignedTransaction)

     @property
    def sender(self):
        if not self._sender:
            # Determine sender
            if self.r == 0 and self.s == 0:
                self._sender = null_address
            else:
                if self.v in (27, 28):
                    vee = self.v
                    sighash = utils.sha3(rlp.encode(self, UnsignedTransaction))
                elif self.v >= 37:
                    vee = self.v - self.network_id * 2 - 8
                    assert vee in (27, 28)
                    rlpdata = rlp.encode(rlp.infer_sedes(self).serialize(self)[
                                         :-3] + [self.network_id, '', ''])
                    sighash = utils.sha3(rlpdata)
                else:
                    raise InvalidTransaction("Invalid V value")
                if self.r >= secpk1n or self.s >= secpk1n or self.r == 0 or self.s == 0:
                    raise InvalidTransaction("Invalid signature values!")
                pub = ecrecover_to_pub(sighash, vee, self.r, self.s)
                if pub == b'\x00' * 64:
                    raise InvalidTransaction(
                        "Invalid signature (zero privkey cannot sign)")
                self._sender = utils.sha3(pub)[-20:]
        return self._sender

    @property
    def network_id(self):
        if self.r == 0 and self.s == 0:
            return self.v
        elif self.v in (27, 28):
            return None
        else:
            return ((self.v - 1) // 2) - 17

    @sender.setter
    def sender(self, value):
        self._sender = value

    def sign(self, key, network_id=None):
        """Sign this transaction with a private key.
        A potentially already existing signature would be overridden.
        """
        if network_id is None:
            rawhash = utils.sha3(rlp.encode(self, UnsignedTransaction))
        else:
            assert 1 <= network_id < 2**63 - 18
            rlpdata = rlp.encode(rlp.infer_sedes(self).serialize(self)[
                                 :-3] + [network_id, b'', b''])
            rawhash = utils.sha3(rlpdata)

        key = normalize_key(key)

        self.v, self.r, self.s = ecsign(rawhash, key)
        if network_id is not None:
            self.v += 8 + network_id * 2

        self._sender = utils.privtoaddr(key)
        return self

    @property
    def hash(self):
        return utils.sha3(rlp.encode(self))

    def to_dict(self):
        d = {}
        for name, _ in self.__class__.fields:
            d[name] = getattr(self, name)
            if name in ('to', 'data'):
                d[name] = '0x' + encode_hex(d[name])
        d['sender'] = '0x' + encode_hex(self.sender)
        d['hash'] = '0x' + encode_hex(self.hash)
        return d


 */


/*

 ##
    # Sign this transaction with a private key.
    #
    # A potentially already existing signature would be override.
    #
    def sign(key)
      raise InvalidTransaction, "Zero privkey cannot sign" if [0, '', Constant::PRIVKEY_ZERO, Constant::PRIVKEY_ZERO_HEX].include?(key)

      rawhash = Utils.keccak256 signing_data(:sign)
      key = PrivateKey.new(key).encode(:bin)

      vrs = Secp256k1.recoverable_sign rawhash, key
      self.v = encode_v(vrs[0])
      self.r = vrs[1]
      self.s = vrs[2]

      self.sender = PrivateKey.new(key).to_address

      self
    end


     def signing_data(mode)
      case mode
      when :sign
        if v == 0 # use encoding rules before EIP155
          RLP.encode(self, sedes: UnsignedTransaction)
        else
          raise InvalidTransaction, "invalid signature"
        end
      when :verify
        if v == V_ZERO || v == V_ONE # encoded v before EIP155
          RLP.encode(self, sedes: UnsignedTransaction)
        end
      else
        raise InvalidTransaction, "invalid signature"
      end
    end
  end


 def encode(obj, sedes=None, infer_serializer=True, cache=False):
    """Encode a Python object in RLP format.
    By default, the object is serialized in a suitable way first (using :func:`rlp.infer_sedes`)
    and then encoded. Serialization can be explicitly suppressed by setting `infer_serializer` to
    ``False`` and not passing an alternative as `sedes`.
    If `obj` has an attribute :attr:`_cached_rlp` (as, notably, :class:`rlp.Serializable`) and its
    value is not `None`, this value is returned bypassing serialization and encoding, unless
    `sedes` is given (as the cache is assumed to refer to the standard serialization which can be
    replaced by specifying `sedes`).
    If `obj` is a :class:`rlp.Serializable` and `cache` is true, the result of the encoding will be
    stored in :attr:`_cached_rlp` if it is empty and :meth:`rlp.Serializable.make_immutable` will
    be invoked on `obj`.
    :param sedes: an object implementing a function ``serialize(obj)`` which will be used to
                  serialize ``obj`` before encoding, or ``None`` to use the infered one (if any)
    :param infer_serializer: if ``True`` an appropriate serializer will be selected using
                             :func:`rlp.infer_sedes` to serialize `obj` before encoding
    :param cache: cache the return value in `obj._cached_rlp` if possible and make `obj` immutable
                  (default `False`)
    :returns: the RLP encoded item
    :raises: :exc:`rlp.EncodingError` in the rather unlikely case that the item is too big to
             encode (will not happen)
    :raises: :exc:`rlp.SerializationError` if the serialization fails
    """


 https://github.com/ethereum/pyrlp/blob/develop/rlp/sedes/lists.py
 *
 */



/*
     public byte[] getEncoded() {

        if (rlpEncoded != null) return rlpEncoded;

        // parse null as 0 for nonce
        byte[] nonce = null;
        if (this.nonce == null || this.nonce.length == 1 && this.nonce[0] == 0) {
            nonce = RLP.encodeElement(null);
        } else {
            nonce = RLP.encodeElement(this.nonce);
        }
        byte[] gasPrice = RLP.encodeElement(this.gasPrice);
        byte[] gasLimit = RLP.encodeElement(this.gasLimit);
        byte[] receiveAddress = RLP.encodeElement(this.receiveAddress);
        byte[] value = RLP.encodeElement(this.value);
        byte[] data = RLP.encodeElement(this.data);

        byte[] v, r, s;

        if (signature != null) {
            int encodeV;
            if (chainId == null) {
                encodeV = signature.v;
            } else {
                encodeV = signature.v - LOWER_REAL_V;
                encodeV += chainId * 2 + CHAIN_ID_INC;
            }
            v = RLP.encodeInt(encodeV);
            r = RLP.encodeElement(BigIntegers.asUnsignedByteArray(signature.r));
            s = RLP.encodeElement(BigIntegers.asUnsignedByteArray(signature.s));
        } else {
            // Since EIP-155 use chainId for v
            v = chainId == null ? RLP.encodeElement(EMPTY_BYTE_ARRAY) : RLP.encodeInt(chainId);
            r = RLP.encodeElement(EMPTY_BYTE_ARRAY);
            s = RLP.encodeElement(EMPTY_BYTE_ARRAY);
        }

        this.rlpEncoded = RLP.encodeList(nonce, gasPrice, gasLimit,
                receiveAddress, value, data, v, r, s);

        this.hash = this.getHash();

        return rlpEncoded;
    }


     public synchronized void rlpParse() {
        if (parsed) return;
        try {
            RLPList decodedTxList = RLP.decode2(rlpEncoded);
            RLPList transaction = (RLPList) decodedTxList.get(0);

            // Basic verification
            if (transaction.size() > 9 ) throw new RuntimeException("Too many RLP elements");
            for (RLPElement rlpElement : transaction) {
                if (!(rlpElement instanceof RLPItem))
                    throw new RuntimeException("Transaction RLP elements shouldn't be lists");
            }

            this.nonce = transaction.get(0).getRLPData();
            this.gasPrice = transaction.get(1).getRLPData();
            this.gasLimit = transaction.get(2).getRLPData();
            this.receiveAddress = transaction.get(3).getRLPData();
            this.value = transaction.get(4).getRLPData();
            this.data = transaction.get(5).getRLPData();
            // only parse signature in case tx is signed
            if (transaction.get(6).getRLPData() != null) {
                byte[] vData =  transaction.get(6).getRLPData();
                BigInteger v = ByteUtil.bytesToBigInteger(vData);
                this.chainId = extractChainIdFromV(v);
                byte[] r = transaction.get(7).getRLPData();
                byte[] s = transaction.get(8).getRLPData();
                this.signature = ECDSASignature.fromComponents(r, s, getRealV(v));
            } else {
                logger.debug("RLP encoded tx is not signed!");
            }
            this.parsed = true;
            this.hash = getHash();
        } catch (Exception e) {
            throw new RuntimeException("Error on parsing RLP", e);
        }
    }

 */
