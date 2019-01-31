//
//  BREthereumamount
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

#include <string.h>
#include <assert.h>
#include "BREthereumAmount.h"

#define DEFAULT_ETHER_GAS_LIMIT    21000ull

//
// amount
//
extern BREthereumAmount
amountCreateEther (BREthereumEther ether) {
    BREthereumAmount amount;
    amount.type = AMOUNT_ETHER;
    amount.u.ether = ether;
    return amount;
}

extern BREthereumAmount
amountCreateToken (BREthereumTokenQuantity tokenQuantity) {
    BREthereumAmount amount;
    amount.type = AMOUNT_TOKEN;
    amount.u.tokenQuantity = tokenQuantity;
    return amount;
}

extern BREthereumAmountType
amountGetType (BREthereumAmount amount) {
    return amount.type;
}

extern BREthereumEther
amountGetEther (BREthereumAmount amount) {
    assert (amount.type == AMOUNT_ETHER);
    return amount.u.ether;
}

extern BREthereumTokenQuantity
amountGetTokenQuantity (BREthereumAmount amount) {
    assert (amount.type == AMOUNT_TOKEN);
    return amount.u.tokenQuantity;
}

extern BREthereumComparison
amountCompare (BREthereumAmount a1, BREthereumAmount a2, int *typeMismatch) {
    assert (NULL != typeMismatch);
    *typeMismatch = (a1.type != a2.type);
    if (*typeMismatch) return ETHEREUM_COMPARISON_GT;
    switch (a1.type) {
        case AMOUNT_ETHER:
            return etherCompare(a1.u.ether, a2.u.ether);
        case AMOUNT_TOKEN:
            return tokenQuantityCompare(a1.u.tokenQuantity, a2.u.tokenQuantity, typeMismatch);
    }
}

extern BREthereumGas
amountGetGasEstimate (BREthereumAmount amount) {
    switch (amount.type) {
        case AMOUNT_ETHER:
            return gasCreate (DEFAULT_ETHER_GAS_LIMIT);
        case AMOUNT_TOKEN:
            return tokenGetGasLimit (amount.u.tokenQuantity.token);
    }
}

extern BRRlpItem
amountRlpEncode(BREthereumAmount amount, BRRlpCoder coder) {
    switch (amount.type) {
        case AMOUNT_ETHER:
            return etherRlpEncode(amount.u.ether, coder);
            
        case AMOUNT_TOKEN:
            // We do not encode a 'number 0', we encode an empty string - it seems from ethereumio
            return rlpEncodeItemUInt64(coder, 0, 1);
    }
}

extern BREthereumAmount
amountRlpDecodeAsEther (BRRlpItem item, BRRlpCoder coder) {
    return amountCreateEther(etherRlpDecode(item, coder));
}

extern BREthereumAmount
amountRlpDecodeAsToken (BRRlpItem item, BRRlpCoder coder, BREthereumToken token) {
    return amountCreateToken(createTokenQuantity(token, rlpDecodeItemUInt256(coder, item, 1)));
}
 
//
// Parse
//
extern BREthereumAmount
amountCreateEtherString (const char *number, BREthereumEtherUnit unit, BRCoreParseStatus *status) {
    BREthereumAmount amount;
    amount.type = AMOUNT_ETHER;
    amount.u.ether = etherCreateString(number, unit, status);
    return amount;
}

extern BREthereumAmount
amountCreateTokenQuantityString (BREthereumToken token, const char *number, BREthereumTokenQuantityUnit unit, BRCoreParseStatus *status) {
    BREthereumAmount amount;
    amount.type = AMOUNT_TOKEN;
    amount.u.tokenQuantity = createTokenQuantityString(token, number, unit, status);
    return amount;
}

/*
 const Web3 = require("web3");
 const web3 = new Web3();
 web3.setProvider(new
 web3.providers.HttpProvider("https://ropsten.infura.io/XXXXXX"));
 var abi = [ {} ] // redacted on purpose
 const account1 = "0x9..."
 const account2 = "0x3..."
 var count = web3.eth.getTransactionCount(account1);
 var abiArray = abi;
 var contractAddress = "0x2...";
 var contract =  web3.eth.contract(abiArray).at(contractAddress);


 var data = contract.transfer.getData(account2, 10000, {from: account1});
 var gasPrice = web3.eth.gasPrice;
 var gasLimit = 90000;

 var rawTransaction = {
 "from": account1,
 "nonce": web3.toHex(count),
 "gasPrice": web3.toHex(gasPrice),
 "gasLimit": web3.toHex(gasLimit),
 "to": account2,
 "value": 0,
 "data": data,
 "chainId": 0x03
 };

 var privKey = new Buffer('XXXXXXXXXXXXXX', 'hex');
 var tx = new Tx(rawTransaction);

 tx.sign(privKey);
 var serializedTx = tx.serialize();

 web3.eth.sendRawTransaction('0x' + serializedTx.toString('hex'), function(err, hash) {
 if (!err)
 console.log(hash);
 else
 console.log(err);
 });

 */


/*
 // https://github.com/ethereum/wiki/wiki/Ethereum-Contract-ABI
 Given the contract:

 contract Foo {
 function bar(fixed[2] xy) {}
 function baz(uint32 x, bool y) returns (bool r) { r = x > 32 || y; }
 function sam(bytes name, bool z, uint[] data) {}
 }
 Thus for our Foo example if we wanted to call baz with the parameters 69 and true, we would pass 68 bytes total, which can be broken down into:

 0xcdcd77c0: the Method ID. This is derived as the first 4 bytes of the Keccak hash of the ASCII form of the signature baz(uint32,bool).
 0x0000000000000000000000000000000000000000000000000000000000000045: the first parameter, a uint32 value 69 padded to 32 bytes
 0x0000000000000000000000000000000000000000000000000000000000000001: the second parameter - boolean true, padded to 32 bytes
 In total:

 // commas added
 0xcdcd77c0,0000000000000000000000000000000000000000000000000000000000000045,0000000000000000000000000000000000000000000000000000000000000001
*/
