//
//  BBRUtilHex.c
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 3/10/2018.
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
#include <ctype.h>
#include <string.h>
#include "BRInt.h"
#include "BRUtilHex.h"

//
//
//

extern void
decodeHex (uint8_t *target, size_t targetLen, const char *source, size_t sourceLen) {
    //
    assert (0 == sourceLen % 2);
    assert (2 * targetLen == sourceLen);
    
    for (int i = 0; i < targetLen; i++) {
        target[i] = (uint8_t) ((_hexu(source[2*i]) << 4) | _hexu(source[(2*i)+1]));
    }
}

extern size_t
decodeHexLength (size_t stringLen) {
    assert (0 == stringLen % 2);
    return stringLen/2;
}

extern uint8_t *
decodeHexCreate (size_t *targetLen, const char *source, size_t sourceLen) {
    size_t length = decodeHexLength(sourceLen);
    if (NULL != targetLen) *targetLen = length;
    uint8_t *target = malloc (length);
    decodeHex (target, length, source, sourceLen);
    return target;
}

extern void
encodeHex (char *target, size_t targetLen, const uint8_t *source, size_t sourceLen) {
    assert (targetLen == 2 * sourceLen  + 1);
    
    for (int i = 0; i < sourceLen; i++) {
        target[2*i] = (uint8_t) _hexc (source[i] >> 4);
        target[2*i + 1] = (uint8_t) _hexc (source[i]);
    }
    target[2*sourceLen] = '\0';
}

extern size_t
encodeHexLength(size_t byteArrayLen) {
    return 2 * byteArrayLen + 1;
}

extern char *
encodeHexCreate (size_t *targetLen, const uint8_t *source, size_t sourceLen) {
    size_t length = encodeHexLength(sourceLen);
    if (NULL != targetLen) *targetLen = length;
    char *target = malloc (length);
    encodeHex(target, length, source, sourceLen);
    return target;
}

extern int
encodeHexValidate (const char *number) {
    // Number contains only hex digits, has an even number and has at least two.
    if (NULL == number || '\0' == *number || 0 != strlen(number) % 2) return 0;

    while (*number)
        if (!isxdigit (*number++)) return 0;
    return 1;
}
