//
//  BREthereumToken
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 3/15/18.
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

#ifndef BR_Ethereum_Token_H
#define BR_Ethereum_Token_H

#include "BREthereumEther.h"
#include "BREthereumGas.h"
#include "BREthereumContract.h"

#ifdef __cplusplus
extern "C" {
#endif

/**
 * An Ethereum ERC20 Token
 */
typedef struct BREthereumTokenRecord *BREthereumToken;

/**
 * Return the token address as a '0x'-prefixed string.  DO NOT FREE THIS.
 */
extern const char *
tokenGetAddress (BREthereumToken token);

extern const char *
tokenGetSymbol (BREthereumToken token);

extern const char *
tokenGetName (BREthereumToken token);

extern const char *
tokenGetDescription(BREthereumToken token);

extern int
tokenGetDecimals (BREthereumToken token);

extern BREthereumGas
tokenGetGasLimit (BREthereumToken token);

extern BREthereumGasPrice
tokenGetGasPrice (BREthereumToken token);

extern const char *
tokenGetColorLeft (BREthereumToken token);

extern const char *
tokenGetColorRight (BREthereumToken token);

extern BREthereumContract
tokenGetContract (BREthereumToken token);

extern const BREthereumToken tokenBRD;

#if defined (BITCOIN_DEBUG)
extern const BREthereumToken tokenTST;
#endif

extern BREthereumToken
tokenLookup (const char *address);

extern int
tokenCount (void);

extern BREthereumToken
tokenGet (int index);

//
// Token Quantity
//

/**
 * A BREthereumTokenQuantityUnit defines the (external) representation of a token quantity
 */
typedef enum {
  TOKEN_QUANTITY_TYPE_DECIMAL,
  TOKEN_QUANTITY_TYPE_INTEGER
} BREthereumTokenQuantityUnit;

/**
 * A BREthereumTokenQuantity defines a token amount.
 *
 */
typedef struct {
  BREthereumToken token;
  UInt256 valueAsInteger;
} BREthereumTokenQuantity;

extern BREthereumTokenQuantity
createTokenQuantity (BREthereumToken token,
                     UInt256 valueAsInteger);

extern BREthereumTokenQuantity
createTokenQuantityString (BREthereumToken token,
                           const char *number,
                           BREthereumTokenQuantityUnit unit,
                           BRCoreParseStatus *status);

extern const BREthereumToken
tokenQuantityGetToken (BREthereumTokenQuantity quantity);

/**
 * A newly allocated string; you own it.
 *
 * @param quantity
 * @param unit
 * @return
 */
extern char *
tokenQuantityGetValueString(const BREthereumTokenQuantity quantity,
                            BREthereumTokenQuantityUnit unit);

extern BREthereumComparison
tokenQuantityCompare (BREthereumTokenQuantity q1, BREthereumTokenQuantity q2, int *typeMismatch);

#ifdef __cplusplus
}
#endif

#endif //BR_Ethereum_Token_H

