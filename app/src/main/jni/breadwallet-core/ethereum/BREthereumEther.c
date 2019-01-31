//
//  BBREthereumEther.c
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
#include <stdio.h>
#include <string.h>
#include <assert.h>
#include "BREthereumEther.h"

#if LITTLE_ENDIAN != BYTE_ORDER
#error "Must be a `LITTLE ENDIAN` cpu architecture"
#endif

//    > (define ether (lambda (x) (values (quotient x max64) (remainder x max64)))
//    > (ether #e1e21) -> 54, 3875820019684212736
//    > (ether #e1e24) -> 54210, 2003764205206896640
//    > (ether #e1e27) -> 54210108, 11515845246265065472
//    > (ether #e1e30) -> 54210108624, 5076944270305263616

static UInt256 etherUnitScaleFactor [NUMBER_OF_ETHER_UNITS] = {   /* LITTLE ENDIAN    */
    { .u64 = {                     1,            0, 0, 0 } }, /* wei       - 1    */
    { .u64 = {                  1000,            0, 0, 0 } }, /* kwei      - 1e3  */
    { .u64 = {               1000000,            0, 0, 0 } }, /* mwei      - 1e6  */
    { .u64 = {            1000000000,            0, 0, 0 } }, /* gwei      - 1e9  */
    { .u64 = {         1000000000000,            0, 0, 0 } }, /* szabo     - 1e12 */
    { .u64 = {      1000000000000000,            0, 0, 0 } }, /* finney    - 1e15 */
    { .u64 = {   1000000000000000000,            0, 0, 0 } }, /* ether     - 1e18 */
    { .u64 = {   3875820019684212736u,          54, 0, 0 } }, /* kether    - 1e21 */
    { .u64 = {   2003764205206896640u,       54210, 0, 0 } }, /* mether    - 1e24 */
    { .u64 = {  11515845246265065472u,    54210108, 0, 0 } }, /* gether    - 1e27 */
    { .u64 = {   5076944270305263616u, 54210108624, 0, 0 } }  /* tether    - 1e30 */
};

extern BREthereumEther
etherCreate(const UInt256 value) {
    BREthereumEther ether;
    ether.valueInWEI = value;
    return ether;
}

extern BREthereumEther
etherCreateUnit(const UInt256 value, BREthereumEtherUnit unit, int *overflow) {
    assert (NULL != overflow);
    
    BREthereumEther ether;
    switch (unit) {
        case WEI:
            ether.valueInWEI = value;
            *overflow = 0;
            break;
        default: {
            ether.valueInWEI = mulUInt256_Overflow(value, etherUnitScaleFactor[unit], overflow);
            break;
        }
    }
    return ether;
}

extern BREthereumEther
etherCreateNumber (uint64_t number, BREthereumEtherUnit unit) {
    int overflow;
    UInt256 value = { .u64 = { number, 0, 0, 0 } };
    BREthereumEther ether = etherCreateUnit(value, unit, &overflow);
    assert (0 == overflow);
    return ether;
}

extern BREthereumEther
etherCreateZero(void) {
    return etherCreate(UINT256_ZERO);
}

extern BREthereumEther
etherCreateString(const char *number, BREthereumEtherUnit unit, BRCoreParseStatus *status) {
    int decimals = 3 * unit;
    
    UInt256 value = createUInt256ParseDecimal(number, decimals, status);
    return etherCreate(value);
}


extern UInt256 // Can't be done: 1 WEI in ETHER... not representable as UInt256
etherGetValue(const BREthereumEther ether,
              BREthereumEtherUnit unit) {
    switch (unit) {
        case WEI:
            return ether.valueInWEI;
        default:
            // TODO: CRITICAL
            return UINT256_ZERO; /* divideUInt256 (ether.valueInWEI, etherUnitScaleFactor[unit]); */
    }
}

extern char * // Perhaps can be done. 1 WEI -> 1e-18 Ether
etherGetValueString(const BREthereumEther ether, BREthereumEtherUnit unit) {
    return coerceStringDecimal(ether.valueInWEI, 3 * unit);
}

extern BRRlpItem
etherRlpEncode (const BREthereumEther ether, BRRlpCoder coder) {
    return rlpEncodeItemUInt256(coder, ether.valueInWEI, 1);
}

extern BREthereumEther
etherRlpDecode (BRRlpItem item, BRRlpCoder coder) {
    return etherCreate(rlpDecodeItemUInt256(coder, item, 1));
}

extern BREthereumEther
etherAdd (BREthereumEther e1, BREthereumEther e2, int *overflow) {
    BREthereumEther result;
    result.valueInWEI = addUInt256_Overflow(e1.valueInWEI, e2.valueInWEI, overflow);
    return result;
}

extern BREthereumEther
etherSub (BREthereumEther e1, BREthereumEther e2, int *negative) {
    BREthereumEther result;
    result.valueInWEI = subUInt256_Negative(e1.valueInWEI, e2.valueInWEI, negative);
    return result;
    
}

//
// Comparisons
//
extern BREthereumBoolean
etherIsEQ (BREthereumEther e1, BREthereumEther e2) {
    return eqUInt256 (e1.valueInWEI, e2.valueInWEI) ? ETHEREUM_BOOLEAN_TRUE : ETHEREUM_BOOLEAN_FALSE;
}

extern BREthereumBoolean
etherIsGT (BREthereumEther e1, BREthereumEther e2) {
    return gtUInt256(e1.valueInWEI, e2.valueInWEI) ? ETHEREUM_BOOLEAN_TRUE : ETHEREUM_BOOLEAN_FALSE;
}

extern BREthereumBoolean
etherIsGE (BREthereumEther e1, BREthereumEther e2) {
    return geUInt256(e1.valueInWEI, e2.valueInWEI) ? ETHEREUM_BOOLEAN_TRUE : ETHEREUM_BOOLEAN_FALSE;
}

extern BREthereumBoolean
etherIsLT (BREthereumEther e1, BREthereumEther e2) {
    return ltUInt256(e1.valueInWEI, e2.valueInWEI) ? ETHEREUM_BOOLEAN_TRUE : ETHEREUM_BOOLEAN_FALSE;
}

extern BREthereumBoolean
etherIsLE (BREthereumEther e1, BREthereumEther e2) {
    return leUInt256(e1.valueInWEI, e2.valueInWEI) ? ETHEREUM_BOOLEAN_TRUE : ETHEREUM_BOOLEAN_FALSE;
}

extern BREthereumBoolean
etherIsZero (BREthereumEther e) {
    return UInt256IsZero (e.valueInWEI) ? ETHEREUM_BOOLEAN_TRUE : ETHEREUM_BOOLEAN_FALSE;
}

extern BREthereumComparison
etherCompare (BREthereumEther e1, BREthereumEther e2) {
    switch (compareUInt256(e1.valueInWEI, e2.valueInWEI))
    {
        case -1: return ETHEREUM_COMPARISON_LT;
        case  0: return ETHEREUM_COMPARISON_EQ;
        case +1: return ETHEREUM_COMPARISON_GT;
        default: assert (0);
    }
}
