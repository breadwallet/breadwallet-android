//
//  BBREthereumWallet.c
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
#include "BREthereumPrivate.h"
#include "BRArray.h"

#define DEFAULT_ETHER_GAS_PRICE_NUMBER   500000000 // 0.5 GWEI
#define DEFAULT_ETHER_GAS_PRICE_UNIT     WEI

#define DEFAULT_TRANSACTION_CAPACITY     20

#define TRANSACTION_NONCE_IS_NOT_ASSIGNED   UINT64_MAX

/* Forward Declarations */
static BREthereumGasPrice
walletCreateDefaultGasPrice (BREthereumWallet wallet);

static BREthereumGas
walletCreateDefaultGasLimit (BREthereumWallet wallet);

static void
walletInsertTransactionSorted (BREthereumWallet wallet,
                               BREthereumTransaction transaction);

static int // -1 if not found
walletLookupTransactionIndex (BREthereumWallet wallet,
                              BREthereumTransaction transaction);

/**
 *
 */
struct BREthereumWalletRecord {
    
    /**
     * The wallet's account.  The account is used to sign transactions.
     */
    BREthereumAccount account;
    
    /**
     * The wallet's primary address - perhaps the sole address.  Must be an address
     * from the wallet's account.
     */
    BREthereumAddress address;      // Primary Address
    
    /**
     * The wallet's network.
     */
    BREthereumNetwork network;
    
    /**
     * The wallet' default gasPrice. gasPrice is the maximum price of gas you are willing to pay
     * for a transaction of this wallet's holding type.  This default value can be 'overridden'
     * when creating a specific transaction.
     *
     * The gasPrice determines how 'interested' a miner is in 'blocking' a transaction.  Thus,
     * the gasPrice determines how quickly your transaction will be added to the block chain.
     */
    BREthereumGasPrice defaultGasPrice;
    
    /**
     * The wallet's default gasLimit. gasLimit is the maximum gas you are willing to pay for
     * a transaction of this wallet's holding type.  This default value can be 'overridden'
     * when creating a specific transaction.
     *
     * The gasLimit prevents your transaction's computation from 'spinning out of control' and
     * thus consuming unexpectedly large amounts of Ether.
     */
    BREthereumGas defaultGasLimit;
    
    /**
     * The wallet's balance, either ETHER or a TOKEN.
     */
    BREthereumAmount balance;
    
    /**
     * An optional ERC20 token specification.  Will be NULL (and unused) for holding ETHER.
     */
    BREthereumToken token; // optional
    
    //
    // Transactions - these are sorted from oldest [index 0] to newest.  As transactions are added
    // we'll maintain the ordering using an 'insertion sort' - while starting at the end and
    // working backwards.
    //
    BREthereumTransaction *transactions;
    
    // Listeners -
    //   on all transaction state changes.
    //   on balance changes? (implied by transactions)
};

//
// Wallet Creation
//
static BREthereumWallet
walletCreateDetailed (BREthereumAccount account,
                      BREthereumAddress address,
                      BREthereumNetwork network,
                      BREthereumAmountType type,
                      BREthereumToken optionalToken) {
    
    assert (NULL != account);
    assert (NULL != address);
    assert (AMOUNT_TOKEN != type || NULL != optionalToken);
    
    BREthereumWallet wallet = calloc(1, sizeof(struct BREthereumWalletRecord));
    
    wallet->account = account;
    wallet->address = address;
    wallet->network = network;
    
    wallet->token = optionalToken;
    wallet->balance = (AMOUNT_ETHER == type
                       ? amountCreateEther(etherCreate(UINT256_ZERO))
                       : amountCreateToken(createTokenQuantity (wallet->token, UINT256_ZERO)));
    
    wallet->defaultGasLimit = AMOUNT_ETHER == type
    ? walletCreateDefaultGasLimit(wallet)
    : tokenGetGasLimit (optionalToken);
    
    wallet->defaultGasPrice = AMOUNT_ETHER == type
    ? walletCreateDefaultGasPrice(wallet)
    : tokenGetGasPrice (optionalToken);
    
    array_new(wallet->transactions, DEFAULT_TRANSACTION_CAPACITY);
    // nonce = eth.getTransactionCount(<account>)
    return wallet;
}

extern BREthereumWallet
walletCreate(BREthereumAccount account,
             BREthereumNetwork network)
{
    return walletCreateWithAddress
    (account,
     accountGetPrimaryAddress(account),
     network);
}

extern BREthereumWallet
walletCreateWithAddress(BREthereumAccount account,
                        BREthereumAddress address,
                        BREthereumNetwork network) {
    return walletCreateDetailed
    (account,
     address,
     network,
     AMOUNT_ETHER,
     NULL);
}

extern BREthereumWallet
walletCreateHoldingToken(BREthereumAccount account,
                         BREthereumNetwork network,
                         BREthereumToken token) {
    return walletCreateDetailed
    (account,
     accountGetPrimaryAddress(account),
     network,
     AMOUNT_TOKEN,
     token);
}

//
// Transaction
//
extern BREthereumEther
walletEstimateTransactionFee (BREthereumWallet wallet,
                              BREthereumAmount amount,
                              int *overflow) {
    return walletEstimateTransactionFeeDetailed(wallet,
                                                amount,
                                                wallet->defaultGasPrice,
                                                amountGetGasEstimate(amount),
                                                overflow);
}

/**
 * Estimate the transaction fee (in Ether) for transferring amount.
 */
extern BREthereumEther
walletEstimateTransactionFeeDetailed (BREthereumWallet wallet,
                                      BREthereumAmount amount,
                                      BREthereumGasPrice price,
                                      BREthereumGas gas,
                                      int *overflow) {
    return etherCreate(mulUInt256_Overflow(price.etherPerGas.valueInWEI,
                                           createUInt256(gas.amountOfGas),
                                           overflow));
}

//
// Transaction Creation
//
extern BREthereumTransaction
walletCreateTransaction(BREthereumWallet wallet,
                        BREthereumAddress recvAddress,
                        BREthereumAmount amount) {
    
    return walletCreateTransactionDetailed
    (wallet,
     recvAddress,
     amount,
     walletGetDefaultGasPrice(wallet),
     walletGetDefaultGasLimit(wallet),
     TRANSACTION_NONCE_IS_NOT_ASSIGNED);
}

extern BREthereumTransaction
walletCreateTransactionDetailed(BREthereumWallet wallet,
                                BREthereumAddress recvAddress,
                                BREthereumAmount amount,
                                BREthereumGasPrice gasPrice,
                                BREthereumGas gasLimit,
                                uint64_t nonce) {
    assert (walletGetAmountType(wallet) == amountGetType(amount));
    assert (AMOUNT_ETHER == amountGetType(amount)
            || (wallet->token == tokenQuantityGetToken (amountGetTokenQuantity(amount))));
    
    BREthereumTransaction transaction = transactionCreate(wallet->address,
                                                          recvAddress,
                                                          amount,
                                                          gasPrice,
                                                          gasLimit,
                                                          nonce);
    walletHandleTransaction(wallet, transaction);
    return transaction;
}

private_extern void
walletHandleTransaction(BREthereumWallet wallet,
                        BREthereumTransaction transaction) {
    walletInsertTransactionSorted(wallet, transaction);
}

private_extern void
walletUnhandleTransaction (BREthereumWallet wallet,
                           BREthereumTransaction transaction) {
    int index = walletLookupTransactionIndex(wallet, transaction);
    assert (-1 != index);
    array_rm(wallet->transactions, index);
}

private_extern int
walletHasTransaction (BREthereumWallet wallet,
                      BREthereumTransaction transaction) {
    return -1 != walletLookupTransactionIndex(wallet, transaction);
}
//
// Transaction Signing and Encoding
//

/**
 * Sign the transaction.
 *
 * @param wallet
 * @param transaction
 * @param paperKey
 */
extern void
walletSignTransaction(BREthereumWallet wallet,
                      BREthereumTransaction transaction,
                      const char *paperKey) {

    if (TRANSACTION_NONCE_IS_NOT_ASSIGNED == transactionGetNonce(transaction))
        transactionSetNonce (transaction, addressGetThenIncrementNonce(wallet->address));

    // TODO: This is overkill...
    //assert (transactionGetNonce(transaction) + 1 == addressGetNonce(wallet->address));
    
    // RLP Encode the UNSIGNED transaction
    BRRlpData transactionUnsignedRLP = transactionEncodeRLP
    (transaction, wallet->network, TRANSACTION_RLP_UNSIGNED);
    
    // Sign the RLP Encoded bytes.
    BREthereumSignature signature = accountSignBytes
    (wallet->account,
     wallet->address,
     SIGNATURE_TYPE_RECOVERABLE,
     transactionUnsignedRLP.bytes,
     transactionUnsignedRLP.bytesCount,
     paperKey);
    
    // Attach the signature
    transactionSign(transaction, wallet->account, signature);
}

// For now.
extern void
walletSignTransactionWithPrivateKey(BREthereumWallet wallet,
                                    BREthereumTransaction transaction,
                                    BRKey privateKey) {

    if (TRANSACTION_NONCE_IS_NOT_ASSIGNED == transactionGetNonce(transaction))
        transactionSetNonce (transaction, addressGetThenIncrementNonce(wallet->address));

    // TODO: This is overkill...
    //assert (transactionGetNonce(transaction) + 1 == addressGetNonce(wallet->address));

    // RLP Encode the UNSIGNED transaction
    BRRlpData transactionUnsignedRLP = transactionEncodeRLP
    (transaction, wallet->network, TRANSACTION_RLP_UNSIGNED);

    // Sign the RLP Encoded bytes.
    BREthereumSignature signature = accountSignBytesWithPrivateKey
    (wallet->account,
     wallet->address,
     SIGNATURE_TYPE_RECOVERABLE,
     transactionUnsignedRLP.bytes,
     transactionUnsignedRLP.bytesCount,
     privateKey);

    // Attach the signature
    transactionSign(transaction, wallet->account, signature);

}

extern BRRlpData
walletGetRawTransaction(BREthereumWallet wallet,
                        BREthereumTransaction transaction) {
    return transactionEncodeRLP (transaction,
                                 wallet->network,
                                 (ETHEREUM_BOOLEAN_TRUE == transactionIsSigned(transaction)
                                  ? TRANSACTION_RLP_SIGNED
                                  : TRANSACTION_RLP_UNSIGNED));
}

extern char *
walletGetRawTransactionHexEncoded (BREthereumWallet wallet,
                                   BREthereumTransaction transaction,
                                   const char *prefix) {
    BRRlpData data = walletGetRawTransaction (wallet, transaction);
    char *result;
    
    if (NULL == prefix) prefix = "";
    
    if (0 == data.bytesCount)
        result = (char *) prefix;
    else {
        size_t resultLen = strlen(prefix) + 2 * data.bytesCount + 1;
        result = malloc (resultLen);
        strcpy (result, prefix);
        encodeHex(&result[strlen(prefix)], 2 * data.bytesCount + 1, data.bytes, data.bytesCount);
    }
    
    rlpDataRelease(data);
    return result;
}

//
// Wallet 'Field' Accessors
//

extern BREthereumAddress
walletGetAddress (BREthereumWallet wallet) {
    return wallet->address;
}

extern BREthereumAmountType
walletGetAmountType (BREthereumWallet wallet) {
    return wallet->balance.type;
}

extern BREthereumToken
walletGetToken (BREthereumWallet wallet) {
    return wallet->token;
}

// Balance

extern BREthereumAmount
walletGetBalance (BREthereumWallet wallet) {
    return wallet->balance;
}

private_extern void
walletSetBalance (BREthereumWallet wallet,
                  BREthereumAmount balance) {
    wallet->balance = balance;
}

// Gas Limit

extern BREthereumGas
walletGetDefaultGasLimit(BREthereumWallet wallet) {
    return wallet->defaultGasLimit;
}

extern void
walletSetDefaultGasLimit(BREthereumWallet wallet, BREthereumGas gasLimit) {
    wallet->defaultGasLimit = gasLimit;
}

static BREthereumGas
walletCreateDefaultGasLimit (BREthereumWallet wallet) {
    return amountGetGasEstimate(wallet->balance);
}

// Gas Price

extern BREthereumGasPrice
walletGetDefaultGasPrice(BREthereumWallet wallet) {
    return wallet->defaultGasPrice;
}

extern void
walletSetDefaultGasPrice(BREthereumWallet wallet, BREthereumGasPrice gasPrice) {
    wallet->defaultGasPrice = gasPrice;
}

static BREthereumGasPrice
walletCreateDefaultGasPrice (BREthereumWallet wallet) {
    switch (amountGetType(wallet->balance)) {
        case AMOUNT_ETHER:
            return gasPriceCreate(etherCreateNumber
                                  (DEFAULT_ETHER_GAS_PRICE_NUMBER,
                                   DEFAULT_ETHER_GAS_PRICE_UNIT));
        case AMOUNT_TOKEN:
            return tokenGetGasPrice (wallet->token);
    }
}

//
// Transaction 'Observation'
//

extern int
transactionPredicateAny (void *ignore,
                         BREthereumTransaction transaction,
                         unsigned int index) {
    return 1;
}

extern int
transactionPredicateStatus (BREthereumTransactionStatus status,
                            BREthereumTransaction transaction,
                            unsigned int index) {
    return status == transactionGetStatus(transaction);
}

extern void
walletWalkTransactions (BREthereumWallet wallet,
                        void *context,
                        BREthereumTransactionPredicate predicate,
                        BREthereumTransactionWalker walker) {
    for (int i = 0; i < array_count(wallet->transactions); i++)
        if (predicate (context, wallet->transactions[i], i))
            walker (context, wallet->transactions[i], i);
}

extern BREthereumTransaction
walletGetTransactionByHash (BREthereumWallet wallet,
                            BREthereumHash hash) {
    if (ETHEREUM_BOOLEAN_IS_TRUE (hashExists(hash)))
        for (int i = 0; i < array_count(wallet->transactions); i++) {
            BREthereumHash transactionHash = transactionGetHash(wallet->transactions[i]);
            if (ETHEREUM_BOOLEAN_IS_TRUE(hashEqual(hash, transactionHash)))
                return wallet->transactions[i];
        }
    return NULL;
}

extern BREthereumTransaction
walletGetTransactionByNonce (BREthereumWallet wallet,
                             BREthereumAddress sourceAddress,
                             uint64_t nonce) {
    for (int i = 0; i < array_count(wallet->transactions); i++)
        if (nonce == transactionGetNonce (wallet->transactions[i])
            && ETHEREUM_BOOLEAN_IS_TRUE(addressEqual(sourceAddress, transactionGetSourceAddress(wallet->transactions[i]))))
            return wallet->transactions [i];
    return NULL;
}

extern BREthereumTransaction
walletGetTransactionByIndex(BREthereumWallet wallet,
                            uint64_t index) {
    return (index < array_count(wallet->transactions)
            ? wallet->transactions[index]
            : NULL);
}

static int // -1 if not found
walletLookupTransactionIndex (BREthereumWallet wallet,
                              BREthereumTransaction transaction) {
    for (int i = 0; i < array_count(wallet->transactions); i++)
        if (transaction == wallet->transactions[i])
            return i;
    return -1;
}

static void
walletInsertTransactionSorted (BREthereumWallet wallet,
                               BREthereumTransaction transaction) {
    size_t index = array_count(wallet->transactions);  // if empty (unsigned int) index == 0
    for (; index > 0; index--)
        // quit if transaction is not-less-than the next in wallet
        if (ETHEREUM_COMPARISON_LT != transactionCompare(transaction, wallet->transactions[index - 1]))
            break;
    array_insert(wallet->transactions, index, transaction);
}

static void
walletUpdateTransactionSorted (BREthereumWallet wallet,
                               BREthereumTransaction transaction) {
    // transaction might have moved - move it if needed - but for now, remove then insert.
    int index = walletLookupTransactionIndex(wallet, transaction);
    assert (-1 != index);
    array_rm(wallet->transactions, index);
    walletInsertTransactionSorted(wallet, transaction);
}

extern unsigned long
walletGetTransactionCount (BREthereumWallet wallet) {
    return array_count(wallet->transactions);
}

//
// Transaction State
//
private_extern void
walletTransactionSubmitted (BREthereumWallet wallet,
                            BREthereumTransaction transaction,
                            const BREthereumHash hash) {
    transactionAnnounceSubmitted (transaction, hash);
    // balance updated?
}

private_extern void
walletTransactionBlocked(BREthereumWallet wallet, BREthereumTransaction transaction,
                         BREthereumGas gasUsed, uint64_t
                         blockNumber, uint64_t
                         blockTimestamp,
                         uint64_t blockTransactionIndex) {
    transactionAnnounceBlocked(transaction, gasUsed,
                               blockNumber,
                               blockTimestamp,
                               blockTransactionIndex);
    walletUpdateTransactionSorted(wallet, transaction);
}

private_extern void
walletTransactionDropped (BREthereumWallet wallet,
                          BREthereumTransaction transaction) {
    transactionAnnounceDropped (transaction, 0);
}

/*
 * https://medium.com/blockchain-musings/how-to-create-raw-transactions-in-ethereum-part-1-1df91abdba7c
 *
 *
 
 // Private key
 const keythereum = require('keythereum');
 const address = '0x9e378d2365b7657ebb0f72ae402bc08812022211';
 const datadir = '/home/administrator/ethereum/data';
 const password = 'password';
 let   privKey; // a 'buffer'
 
 keythereum.importFromFile(address, datadir,
 function (keyObject) {
 keythereum.recover(password, keyObject,
 function (privateKey) {
 console.log(privateKey.toString('hex'));
 privKey = privateKey
 });
 });
 //05a20149c1c76ae9da8457435bf0224a4f81801da1d8204cb81608abe8c112ca
 
 const ethTx = require('ethereumjs-tx');
 
 const txParams = {
 nonce: '0x6', // Replace by nonce for your account on geth node
 gasPrice: '0x09184e72a000',
 gasLimit: '0x30000',
 to: '0xfa3caabc8eefec2b5e2895e5afbf79379e7268a7',
 value: '0x00'
 };
 
 // Transaction is created
 const tx = new ethTx(txParams);
 const privKey = Buffer.from('05a20149c1c76ae9da8457435bf0224a4f81801da1d8204cb81608abe8c112ca', 'hex');
 
 // Transaction is signed
 tx.sign(privKey);
 
 const serializedTx = tx.serialize();
 const rawTx = '0x' + serializedTx.toString('hex');
 console.log(rawTx)
 
 eth.sendRawTransaction(raxTX)
 
 
 */


/*
 *
 * https://ethereum.stackexchange.com/questions/16472/signing-a-raw-transaction-in-go
 
 signer := types.NewEIP155Signer(nil)
 tx := types.NewTransaction(nonce, to, amount, gas, gasPrice, data)
 signature, _ := crypto.Sign(tx.SigHash(signer).Bytes(), privkey)
 signed_tx, _ := tx.WithSignature(signer, signature)
 
 */

/*
 *
 
 web3.eth.accounts.create();
 > {
 address: "0xb8CE9ab6943e0eCED004cDe8e3bBed6568B2Fa01",
 privateKey: "0x348ce564d427a3311b6536bbcff9390d69395b06ed6c486954e971d960fe8709",
 walletSignTransaction: function(tx){...},
 sign: function(data){...},
 encrypt: function(password){...}
 }
 
 */
