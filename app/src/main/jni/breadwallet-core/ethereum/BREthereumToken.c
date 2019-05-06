//
//  BREthereumToken
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
#include "BRArray.h"
#include "BREthereumToken.h"
#include "BREthereum.h"

// For tokenBRD define defaults for Gas Limit and Price.  These are arguably never up to date
// and thus should be changed in programmatically using walletSetDefaultGas{Price,Limit}().
#define TOKEN_BRD_DEFAULT_GAS_LIMIT  92000
#define TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64  500000000 // 0.5 GWEI

//
// Token
//

struct BREthereumTokenRecord {
    /**
     * An Ethereum '0x' address for the token's contract.
     */
    char *address;
    
    /**
     * The (exchange) symbol - "BRD"
     */
    char *symbol;
    
    /**
     * The name - "Bread Token"
     */
    char *name;
    
    /**
     * The description - "The Bread Token ..."
     */
    char *description;
    
    /**
     * The maximum decimals (typically 0 to 18).
     */
    unsigned int decimals;
    
    /**
     * Color Transition
     */
    char *colorLeft;
    char *colorRight;
    
    /**
     * The (default) Gas Limit for exchanges of this token.
     */
    BREthereumGas gasLimit;           // TODO: Feels modifiable
    
    /**
     * The (default) Gas Price for exchanges of this token.
     */
    BREthereumGasPrice gasPrice;      // TODO: Feels modifiable
    
    /**
     * True(1) if allocated statically
     */
    int staticallyAllocated;
};

extern const char *
tokenGetAddress(BREthereumToken token) {
    return token->address;
}

extern const char *
tokenGetSymbol(BREthereumToken token) {
    return token->symbol;
}

extern const char *
tokenGetName(BREthereumToken token) {
    return token->name;
}

extern const char *
tokenGetDescription(BREthereumToken token) {
    return token->description;
}

extern int
tokenGetDecimals(BREthereumToken token) {
    return token->decimals;
}

extern BREthereumGas
tokenGetGasLimit(BREthereumToken token) {
    return token->gasLimit;
}


extern BREthereumGasPrice
tokenGetGasPrice(BREthereumToken token) {
    return token->gasPrice;
}

extern const char *
tokenGetColorLeft(BREthereumToken token) {
    return token->colorLeft;
}

extern const char *
tokenGetColorRight(BREthereumToken token) {
    return token->colorRight;
}

extern BREthereumContract
tokenGetContract(BREthereumToken token) {
    return contractERC20;
}

//
// Token Quantity
//
extern BREthereumTokenQuantity
createTokenQuantity(BREthereumToken token,
                    UInt256 valueAsInteger) {
    assert (NULL != token);
    
    BREthereumTokenQuantity quantity;
    quantity.token = token;
    quantity.valueAsInteger = valueAsInteger;
    return quantity;
}

extern BREthereumTokenQuantity
createTokenQuantityString(BREthereumToken token,
                          const char *number,
                          BREthereumTokenQuantityUnit unit,
                          BRCoreParseStatus *status) {
    UInt256 valueAsInteger;
    
    if ((TOKEN_QUANTITY_TYPE_DECIMAL == unit && CORE_PARSE_OK != parseIsDecimal(number))
        || (TOKEN_QUANTITY_TYPE_INTEGER == unit && CORE_PARSE_OK != parseIsInteger(number))) {
        *status = CORE_PARSE_STRANGE_DIGITS;
        valueAsInteger = UINT256_ZERO;
    } else {
        valueAsInteger = (TOKEN_QUANTITY_TYPE_DECIMAL == unit
                          ? createUInt256ParseDecimal(number, token->decimals, status)
                          : createUInt256Parse(number, 10, status));
    }
    
    return createTokenQuantity(token, (CORE_PARSE_OK != *status ? UINT256_ZERO : valueAsInteger));
}

extern const BREthereumToken
tokenQuantityGetToken(BREthereumTokenQuantity quantity) {
    return quantity.token;
}

extern char *
tokenQuantityGetValueString(const BREthereumTokenQuantity quantity,
                            BREthereumTokenQuantityUnit unit) {
    return TOKEN_QUANTITY_TYPE_INTEGER == unit
           ? coerceString(quantity.valueAsInteger, 10)
           : coerceStringDecimal(quantity.valueAsInteger, quantity.token->decimals);
}

extern BREthereumComparison
tokenQuantityCompare(BREthereumTokenQuantity q1, BREthereumTokenQuantity q2, int *typeMismatch) {
    *typeMismatch = (q1.token != q2.token);
    if (*typeMismatch) return ETHEREUM_COMPARISON_GT;
    switch (compareUInt256(q1.valueAsInteger, q2.valueAsInteger)) {
        case -1:
            return ETHEREUM_COMPARISON_LT;
        case 0:
            return ETHEREUM_COMPARISON_EQ;
        case +1:
            return ETHEREUM_COMPARISON_GT;
        default:
            return ETHEREUM_COMPARISON_GT;
    }
}

/*

{"code":"SNGLS","colors":["FAFAFA","FAFAFA"],"name":"Singular DTV","decimal":"0","address":"0xaeC2E87E0A235266D9C5ADc9DEb4b2E29b54D009"}

var tokensInJSONToC = function (tokens) {
return "static struct BREthereumTokenRecord tokens[] = \n{" +
       tokens.map (function (token) {
    return `
    {
        "${token.address}",
        "${token.code}",
        "${token.name}",
        "",
        ${token.decimal},
        "${token.colors[0]}",
        "${token.colors[1]}",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    }`})
.join (",\n") + "\n};"
}

var result = tokensInJSONToC (tokens);
console.log (result)
*/

//
//
//
static struct BREthereumTokenRecord tokens[] = {
    {
        // BRD first... so we can find it.
        "0x558ec3152e2eb2174905cd19aea4e34a23de9ad6",
        "BRD",
        "BRD Token",
        "",
        18,
        "#ff5193",
        "#f9a43a",
        {TOKEN_BRD_DEFAULT_GAS_LIMIT},
        {{{.u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
#if defined (BITCOIN_DEBUG)
    {
        // (Optional) TST next... so we can find it too.
#if defined (BITCOIN_TESTNET) && 1 == BITCOIN_TESTNET
        "0x722dd3f80bac40c951b51bdd28dd19d435762180", // testnet,
#else
        "0x3efd578b271d034a69499e4a2d933c631d44b9ad", // mainnet
#endif
        "TST",
        "Test Standard Token",
        "TeST Standard Token (TST) for TeSTing (TST)",
        18,
        "#bfbfbf",
        "#bfbfbf",
        {TOKEN_BRD_DEFAULT_GAS_LIMIT},
        {{{.u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
#endif // defined (BITCOIN_DEBUG)
    {
        "0xAf30D2a7E90d7DC361c8C4585e9BB7D2F6f15bc7",
        "1ST",
        "FirstBlood",
        "",
        18,
        "#f15a22",
        "#f15a22",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xb98d4c97425d9908e66e53a6fdf673acca0be986",
        "ABT",
        "ArcBlock",
        "",
        18,
        "#3effff",
        "#3effff",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xD0D6D6C5Fe4a677D343cC433536BB717bAe167dD",
        "ADT",
        "adToken",
        "",
        9,
        "#0071bc",
        "#0071bc",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x4470BB87d77b963A013DB939BE332f927f2b992e",
        "ADX",
        "AdEx",
        "",
        4,
        "#1b75bc",
        "#1b75bc",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x5ca9a71b1d01849c0a95490cc00559717fcf0d1d",
        "AE",
        "Aeternity",
        "",
        18,
        "#de3f6b",
        "#de3f6b",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x8eB24319393716668D768dCEC29356ae9CfFe285",
        "AGI",
        "SingularityNET",
        "",
        8,
        "#543ce9",
        "#543ce9",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x4CEdA7906a5Ed2179785Cd3A40A69ee8bc99C466",
        "AION",
        "Aion",
        "",
        8,
        "#00bfec",
        "#00bfec",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x4dc3643dbc642b72c158e7f3d2ff232df61cb6ce",
        "AMB",
        "Amber",
        "",
        18,
        "#5ca7bc",
        "#5ca7bc",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x960b236A07cf122663c4303350609A66A7B288C0",
        "ANT",
        "Aragon",
        "",
        18,
        "#2cd3e1",
        "#2cd3e1",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x622dFfCc4e83C64ba959530A5a5580687a57581b",
        "AUTO",
        "CUBE",
        "",
        18,
        "#fab431",
        "#fab431",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x74004a7227615fb52b82d17ffabfa376907d8a4d",
        "AVM",
        "AVM Ecosystem",
        "",
        18,
        "#364aad",
        "#364aad",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x0D8775F648430679A709E98d2b0Cb6250d2887EF",
        "BAT",
        "Basic Attention",
        "",
        18,
        "#ff5000",
        "#ff5000",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xb3104b4b9da82025e8b9f8fb28b3553ce2f67069",
        "BIX",
        "BIX Token",
        "",
        18,
        "#6499ea",
        "#6499ea",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x5732046a883704404f284ce41ffadd5b007fd668",
        "BLZ",
        "Bluzelle",
        "",
        18,
        "#18578c",
        "#18578c",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xb8c77482e45f1f44de1745f52c74426c631bdd52",
        "BNB",
        "Binance Coin",
        "",
        18,
        "#f3ba2f",
        "#f3ba2f",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x1F573D6Fb3F13d689FF844B4cE37794d79a7FF1C",
        "BNT",
        "Bancor",
        "",
        18,
        "#000d2b",
        "#000d2b",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xcb97e65f07da24d46bcdd078ebebd7c6e6e3d750",
        "BTM",
        "Bytom",
        "",
        8,
        "#504c4c",
        "#504c4c",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x12FEF5e57bF45873Cd9B62E9DBd7BFb99e32D73e",
        "CFI",
        "Cofound.it",
        "",
        18,
        "#0099e5",
        "#0099e5",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xf85fEea2FdD81d51177F6b8F35F0e6734Ce45F5F",
        "CMT",
        "CyberMiles",
        "",
        18,
        "#c1a05c",
        "#c1a05c",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xd4c435f5b09f855c3317c8524cb1f586e42795fa",
        "CND",
        "Cindicator",
        "",
        18,
        "#383939",
        "#383939",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x8713d26637cf49e1b6b4a7ce57106aabc9325343",
        "CNN",
        "CNN Token",
        "",
        18,
        "#6fa7f6",
        "#6fa7f6",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x80a7e048f37a50500351c204cb407766fa3bae7f",
        "CRPT",
        "Crypterium",
        "",
        18,
        "#1a223e",
        "#1a223e",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x46b9ad944d1059450da1163511069c718f699d31",
        "CS",
        "Credits",
        "",
        6,
        "#262626",
        "#262626",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x41e5560054824ea6b0732e656e3ad64e20e94e45",
        "CVC",
        "Civic",
        "",
        8,
        "#3ab03e",
        "#3ab03e",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x3597bfd533a99c9aa083587b074434e61eb0a258",
        "DENT",
        "DENT",
        "",
        8,
        "#666666",
        "#666666",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xE0B7927c4aF23765Cb51314A0E0521A9645F0E2A",
        "DGD",
        "DigixDaAO",
        "",
        9,
        "#f4d029",
        "#f4d029",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x0abdace70d3790235af448c88547603b945604ea",
        "DNT",
        "district0x",
        "",
        18,
        "#2c398f",
        "#2c398f",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x419c4db4b9e25d6db2ad9691ccb832c8d9fda05e",
        "DRGN",
        "Dragon",
        "",
        18,
        "#c91111",
        "#c91111",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x08711D3B02C8758F2FB3ab4e80228418a7F8e39c",
        "EDG",
        "Edgeless",
        "",
        0,
        "#2b1544",
        "#2b1544",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xced4e93198734ddaff8492d525bd258d49eb388e",
        "EDO",
        "Eidoo",
        "",
        18,
        "#242424",
        "#242424",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xbf2179859fc6d5bee9bf9158632dc51678a4100e",
        "ELF",
        "Aelf",
        "",
        18,
        "#2b5ebb",
        "#2b5ebb",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xf0ee6b27b759c9893ce4f094b49ad28fd15a23e4",
        "ENG",
        "Enigma",
        "",
        8,
        "#2f2f2f",
        "#2f2f2f",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xF629cBd94d3791C9250152BD8dfBDF380E2a3B9c",
        "ENJ",
        "EnjinCoin",
        "",
        18,
        "#624dbf",
        "#624dbf",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    {
        "0x5af2be193a6abca9c8817001f45744777db30756",
        "ETHOS",
        "Ethos",
        "",
        8,
        "#00ffba",
        "#00ffba",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xd0352a019e9ab9d757776f532377aaebd36fd541",
        "FSN",
        "Fusion",
        "",
        18,
        "#1d9ad7",
        "#1d9ad7",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x419D0d8BdD9aF5e606Ae2232ed285Aff190E711b",
        "FUN",
        "FunFair",
        "",
        8,
        "#ed1968",
        "#ed1968",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xb4cebdb66b3a5183fd4764a25cad1fc109535016",
        "GD",
        "Zilla GD",
        "",
        18,
        "#3C3C3B",
        "#3C3C3B",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x6810e776880C02933D47DB1b9fc05908e5386b96",
        "GNO",
        "Gnosis",
        "",
        18,
        "#00a6c4",
        "#00a6c4",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xa74476443119A942dE498590Fe1f2454d7D4aC0d",
        "GNT",
        "Golem",
        "",
        18,
        "#001d57",
        "#001d57",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x6ec8a24cabdc339a06a172f8223ea557055adaa5",
        "GNX",
        "Genaro Network",
        "",
        9,
        "#030a34",
        "#030a34",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xc5bbae50781be1669306b9e001eff57a2957b09d",
        "GTO",
        "Gifto",
        "",
        5,
        "#7f27ff",
        "#7f27ff",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xf7B098298f7C69Fc14610bf71d5e02c60792894C",
        "GUP",
        "Matchpool",
        "",
        3,
        "#37dcd8",
        "#37dcd8",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x103c3A209da59d3E7C4A89307e66521e081CFDF0",
        "GVT",
        "Genesis Vision",
        "",
        18,
        "#16b9ad",
        "#16b9ad",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x38c6a68304cdefb9bec48bbfaaba5c5b47818bb2",
        "HPB",
        "HPBCoin",
        "",
        18,
        "#1591ca",
        "#1591ca",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xebbdf302c940c6bfd49c6b165f457fdb324649bc",
        "HYDRO",
        "Hydro",
        "",
        18,
        "#2e72ed",
        "#2e72ed",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x888666CA69E0f178DED6D75b5726Cee99A87D698",
        "ICN",
        "ICONOMI",
        "",
        18,
        "#4c6f8c",
        "#4c6f8c",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xb5a5f22694352c15b00323844ad545abb2b11028",
        "ICX",
        "ICON",
        "",
        18,
        "#1fc5c9",
        "#1fc5c9",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xfa1a856cfa3409cfa145fa4e20eb270df3eb21ab",
        "IOST",
        "IOStoken",
        "",
        18,
        "#1c1c1c",
        "#1c1c1c",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x5e6b6d9abad9093fdc861ea1600eba1b355cd940",
        "ITC",
        "IOT Chain",
        "",
        18,
        "#102044",
        "#102044",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x818Fc6C2Ec5986bc6E2CBf00939d90556aB12ce5",
        "KIN",
        "Kin",
        "",
        18,
        "#2359cf",
        "#2359cf",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xdd974D5C2e2928deA5F71b9825b8b646686BD200",
        "KNC",
        "Kyber Network",
        "",
        18,
        "#188c92",
        "#188c92",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x80fB784B7eD66730e8b1DBd9820aFD29931aab03",
        "LEND",
        "EthLend",
        "",
        18,
        "#0fa9c9",
        "#0fa9c9",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x514910771af9ca656af840dff83e8264ecf986ca",
        "LINK",
        "ChainLink",
        "",
        18,
        "#01a6fb",
        "#01a6fb",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xa4e8c3ec456107ea67d3075bf9e3df3a75823db0",
        "LOOM",
        "Loom",
        "",
        18,
        "#028fac",
        "#028fac",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xEF68e7C694F40c8202821eDF525dE3782458639f",
        "LRC",
        "Loopring",
        "",
        18,
        "#2ab6f6",
        "#2ab6f6",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xfa05A73FfE78ef8f1a739473e462c54bae6567D9",
        "LUN",
        "Lunyr",
        "",
        18,
        "#f55749",
        "#f55749",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xe25bcec5d3801ce3a794079bf94adf1b8ccd802d",
        "MAN",
        "MATRIX AI Network",
        "",
        18,
        "#63f8ed",
        "#63f8ed",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x0F5D2fB29fb7d3CFeE444a200298f468908cC942",
        "MANA",
        "Decentraland",
        "",
        18,
        "#bfb5af",
        "#bfb5af",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xB63B606Ac810a52cCa15e44bB630fd42D8d1d83d",
        "MCO",
        "Monaco",
        "",
        8,
        "#103f68",
        "#103f68",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xF433089366899D83a9f26A773D59ec7eCF30355e",
        "MTL",
        "Metal",
        "",
        8,
        "#1e1f25",
        "#1e1f25",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x809826cceab68c387726af962713b64cb5cb3cca",
        "nCash",
        "Nucleus Vision",
        "",
        18,
        "#36a9cf",
        "#36a9cf",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xb62132e35a6c13ee1ee0f84dc5d40bad8d815206",
        "NEXO",
        "Nexo",
        "",
        18,
        "#4aa2ea",
        "#4aa2ea",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x1776e1F26f98b1A5dF9cD347953a26dd3Cb46671",
        "NMR",
        "Numeraire",
        "",
        18,
        "#000000",
        "#000000",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xa15c7ebe1f07caf6bff097d8a589fb8ac49ae5b3",
        "NPXS",
        "Pundi X Token",
        "",
        18,
        "#eecf52",
        "#eecf52",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xb91318f35bdb262e9423bc7c7c2a3a93dd93c92c",
        "NULS",
        "Nuls",
        "",
        18,
        "#82bd39",
        "#82bd39",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xd26114cd6EE289AccF82350c8d8487fedB8A0C07",
        "OMG",
        "OmiseGO",
        "",
        18,
        "#1a53f0",
        "#1a53f0",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x2C4e8f2D746113d0696cE89B35F0d8bF88E0AEcA",
        "OST",
        "OST",
        "",
        18,
        "#bbdfd0",
        "#bbdfd0",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xfeDAE5642668f8636A11987Ff386bfd215F942EE",
        "PAL",
        "PolicyPal Network",
        "",
        18,
        "#54b9a1",
        "#54b9a1",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xB97048628DB6B661D4C2aA833e95Dbe1A905B280",
        "PAY",
        "TenX",
        "",
        18,
        "#302c2c",
        "#302c2c",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x0e0989b1f9b8a38983c2ba8053269ca62ec9b195",
        "POE",
        "Po.et",
        "",
        8,
        "#dcd6cc",
        "#dcd6cc",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x9992eC3cF6A55b00978cdDF2b27BC6882d88D1eC",
        "POLY",
        "Polymath",
        "",
        18,
        "#4c5a95",
        "#4c5a95",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x595832f8fc6bf59c85c527fec3740a1b7a361269",
        "POWR",
        "Power Ledger",
        "",
        6,
        "#05bca9",
        "#05bca9",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xd4fa1460F537bb9085d22C7bcCB5DD450Ef28e3a",
        "PPT",
        "Populous",
        "",
        8,
        "#152743",
        "#152743",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x1844b21593262668b7248d0f57a220caaba46ab9",
        "PRL",
        "Oyster Pearl",
        "",
        18,
        "#0984fb",
        "#0984fb",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x8Ae4BF2C33a8e667de34B54938B0ccD03Eb8CC06",
        "PTOY",
        "Patientory",
        "",
        8,
        "#42b34e",
        "#42b34e",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x618e75ac90b12c6049ba3b27f5d5f8651b0037f6",
        "QASH",
        "QASH",
        "",
        6,
        "#1347e8",
        "#1347e8",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x697beac28B09E122C4332D163985e8a73121b97F",
        "QRL",
        "QRL",
        "",
        8,
        "#252525",
        "#252525",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x99ea4dB9EE77ACD40B119BD1dC4E33e1C070b80d",
        "QSP",
        "Quantstamp",
        "",
        18,
        "#454545",
        "#454545",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x48f775efbe4f5ece6e0df2f7b5932df56823b990",
        "R",
        "Revain",
        "",
        0,
        "#771a4e",
        "#771a4e",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xf970b8e36e23f7fc3fd752eea86f8be8d83375a6",
        "RCN",
        "Ripio Credit",
        "",
        18,
        "#3555f9",
        "#3555f9",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x255aa6df07540cb5d3d297f0d0d4d84cb52bc8e6",
        "RDN",
        "Raiden Network",
        "",
        18,
        "#2a2a2a",
        "#2a2a2a",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xE94327D07Fc17907b4DB788E5aDf2ed424adDff6",
        "REP",
        "Augur",
        "",
        18,
        "#602a52",
        "#602a52",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x8f8221aFbB33998d8584A2B05749bA73c37a938a",
        "REQ",
        "Request",
        "",
        18,
        "#6cfccd",
        "#6cfccd",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x607F4C5BB672230e8672085532f7e901544a7375",
        "RLC",
        "iExec RLC",
        "",
        9,
        "#ffd800",
        "#ffd800",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xf278c1ca969095ffddded020290cf8b5c424ace2",
        "RUFF",
        "RUFF",
        "",
        18,
        "#01c8ca",
        "#01c8ca",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x4156D3342D5c385a87D264F90653733592000581",
        "SALT",
        "SALT",
        "",
        8,
        "#1beef4",
        "#1beef4",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x7C5A0CE9267ED19B22F8cae653F198e3E8daf098",
        "SAN",
        "SAN",
        "",
        18,
        "#2b77b3",
        "#2b77b3",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xa44e5137293e855b1b7bc7e2c6f8cd796ffcb037",
        "SENT",
        "SENTinel",
        "",
        8,
        "#071c5b",
        "#071c5b",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xF4134146AF2d511Dd5EA8cDB1C4AC88C57D60404",
        "SNC",
        "SunContract",
        "",
        18,
        "#1096d4",
        "#1096d4",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xaeC2E87E0A235266D9C5ADc9DEb4b2E29b54D009",
        "SNGLS",
        "Singular DTV",
        "",
        0,
        "#b30d23",
        "#b30d23",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x983F6d60db79ea8cA4eB9968C6aFf8cfA04B3c63",
        "SNM",
        "SONM",
        "",
        18,
        "#0b1c26",
        "#0b1c26",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x744d70FDBE2Ba4CF95131626614a1763DF805B9E",
        "SNT",
        "Status",
        "",
        18,
        "#5b6dee",
        "#5b6dee",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x68d57c9a1C35f63E2c83eE8e49A64e9d70528D25",
        "SRN",
        "SIRIN",
        "",
        18,
        "#1c1c1c",
        "#1c1c1c",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xB64ef51C888972c908CFacf59B47C1AfBC0Ab8aC",
        "STORJ",
        "Storj",
        "",
        8,
        "#2683ff",
        "#2683ff",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xD0a4b8946Cb52f0661273bfbC6fD0E0C75Fc6433",
        "STORM",
        "Storm",
        "",
        18,
        "#080d98",
        "#080d98",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x5c3a228510d246b78a3765c20221cbf3082b44a4",
        "STQ",
        "Storiqa",
        "",
        18,
        "#58b4f1",
        "#58b4f1",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x006BeA43Baa3f7A6f765F14f10A1a1b08334EF45",
        "STX",
        "Stox",
        "",
        18,
        "#7022eb",
        "#7022eb",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x12480E24eb5bec1a9D4369CaB6a80caD3c0A377A",
        "SUB",
        "Substratum",
        "",
        2,
        "#e53431",
        "#e53431",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x3883f5e181fccaf8410fa61e12b59bad963fb645",
        "THETA",
        "Theta Token",
        "",
        18,
        "#2ab8e6",
        "#2ab8e6",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x6531f133e6DeeBe7F2dcE5A0441aA7ef330B4e53",
        "TIME",
        "Chronobank",
        "",
        8,
        "#5db3ed",
        "#5db3ed",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },

    {
            "0xdAC17F958D2ee523a2206206994597C13D831ec7",
            "USDT",
            "Tether USD",
            "",
            6,
            "#c59a47",
            "#c59a47",
            { TOKEN_BRD_DEFAULT_GAS_LIMIT },
            { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
            1
    },

    {
        "0x92e52a1a235d9a103d970901066ce910aacefd37",
        "UCASH",
        "UCASH",
        "",
        8,
        "#c59a47",
        "#c59a47",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x340d2bde5eb28c1eed91b2f790723e3b160613b7",
        "VEE",
        "BLOCKv",
        "",
        18,
        "#000000",
        "#000000",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xD850942eF8811f2A866692A623011bDE52a462C1",
        "VEN",
        "VeChain",
        "",
        18,
        "#15bdff",
        "#15bdff",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x2C974B2d0BA1716E644c1FC59982a89DDD2fF724",
        "VIB",
        "Viberate",
        "",
        18,
        "#ff1f43",
        "#ff1f43",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x39Bb259F66E1C59d5ABEF88375979b4D20D98022",
        "WAX",
        "WAX",
        "",
        8,
        "#f89022",
        "#f89022",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x667088b212ce3d06a1b553a7221E1fD19000d9aF",
        "WINGS",
        "Wings",
        "",
        18,
        "#0dc9f7",
        "#0dc9f7",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x4CF488387F035FF08c371515562CBa712f9015d4",
        "WPR",
        "WePower",
        "",
        18,
        "#dbcc49",
        "#dbcc49",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x72adadb447784dd7ab1f472467750fc485e4cb2d",
        "WRC",
        "Worldcore",
        "",
        6,
        "#1f4251",
        "#1f4251",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xb7cb1c96db6b22b0d3d9536e0108d062bd488f74",
        "WTC",
        "Waltonchain",
        "",
        18,
        "#8200ff",
        "#8200ff",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x39689fE671C01fcE173395f6BC45D4C332026666",
        "XJP",
        "Digital JPY",
        "",
        0,
        "#f15a22",
        "#f15a22",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x05f4a42e251f2d52b8ed15E9FEdAacFcEF1FAD27",
        "ZIL",
        "Zilliqa",
        "",
        12,
        "#49c1bf",
        "#49c1bf",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xfd8971d5e8e1740ce2d0a84095fca4de729d0c16",
        "ZLA",
        "Zilla",
        "",
        18,
        "#5e6ab2",
        "#5e6ab2",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0xE41d2489571d322189246DaFA5ebDe1F4699F498",
        "ZRX",
        "0x",
        "",
        18,
        "#302c2c",
        "#302c2c",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    
    {
        "0x85e076361cc813a908ff672f9bad1541474402b2",
        "TEL",
        "Telcoin",
        "",
        2,
        "#59C6FA",
        "#59C6FA",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },

    {
        "0xbf3f09e4eba5f7805e5fac0ee09fd6ee8eebe4cb",
        "BGX",
        "BIT GAME EXCHANGE",
        "",
        18,
        "#ff0000",
        "#ff0000",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    {
        "0x2bba3cf6de6058cc1b4457ce00deb359e2703d7f",
        "HSC",
        "Hash Coin",
        "",
        18,
        "#0000ff",
        "#0000ff",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    },
    {
            "0x8f7571fdbfc3ce3ab4d697afb4f9950e9574f676",
            "FIlmC",
            "WeFilmchain",
            "",
            18,
            "#0000ff",
            "#0000ff",
            { TOKEN_BRD_DEFAULT_GAS_LIMIT },
            { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
            1
    },
    {
            "0xa8cac329f783edac931815c5466e283d48c9d7f7",
            "FISH",
            "FishChain Token",
            "",
            4,
            "#0000ff",
            "#0000ff",
            { TOKEN_BRD_DEFAULT_GAS_LIMIT },
            { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
            1
    },
    {
        "0x2bba3cf6de6058cc1b4457ce00deb359e2703d7d",
        "IOEX",
        "Coming soon",
        "",
        18,
        "#0000ff",
        "#0000ff",
        { TOKEN_BRD_DEFAULT_GAS_LIMIT },
        { { { .u64 = {TOKEN_BRD_DEFAULT_GAS_PRICE_IN_WEI_UINT64, 0, 0, 0}}}},
        1
    }
};

const BREthereumToken tokenBRD = &tokens[0];

#if defined (BITCOIN_DEBUG)
const BREthereumToken tokenTST = &tokens[1];
#endif

extern BREthereumToken
tokenLookup(const char *address) {
    if (NULL != address && '\0' != address[0]) {
        int count = tokenCount();
        for (int i = 0; i < count; i++)
            if (0 == strcasecmp(address, tokens[i].address))
                return &tokens[i];
    }
    return NULL;
}

extern int
tokenCount() {
    return sizeof(tokens) / sizeof(struct BREthereumTokenRecord);
}

extern BREthereumToken
tokenGet(int index) {
    return (0 <= index && index < tokenCount()
            ? &tokens[index]
            : NULL);
}
