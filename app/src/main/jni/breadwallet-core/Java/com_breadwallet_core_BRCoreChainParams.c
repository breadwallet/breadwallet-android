//  Created by Ed Gamble on 1/23/2018
//  Copyright (c) 2018 breadwallet LLC.
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
#include <malloc.h>
#include <string.h>
#include "BRCoreJni.h"
#include "BRChainParams.h"
#include "bcash/BRBCashParams.h"
#include "com_breadwallet_core_BRCoreChainParams.h"

/*
 * Class:     com_breadwallet_core_BRCoreChainParams
 * Method:    getJniMagicNumber
 * Signature: ()I
 */
JNIEXPORT jint JNICALL Java_com_breadwallet_core_BRCoreChainParams_getJniMagicNumber
        (JNIEnv *env, jobject thisObject)
{
    BRChainParams *params = (BRChainParams *) getJNIReference(env, thisObject);
    return params->magicNumber;
}

/*
 * Class:     com_breadwallet_core_BRCoreChainParams
 * Method:    createJniMainnetChainParams
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreChainParams_createJniMainnetChainParams
        (JNIEnv *env, jclass thisClass) {
    BRChainParams *result = (BRChainParams *) calloc (1, sizeof (BRChainParams));
    memcpy (result, &BRMainNetParams, sizeof (BRChainParams));
    return (jlong) result;
}

/*
 * Class:     com_breadwallet_core_BRCoreChainParams
 * Method:    createJniTestnetChainParams
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreChainParams_createJniTestnetChainParams
        (JNIEnv *env, jclass thisClass) {
    BRChainParams *result = (BRChainParams *) calloc (1, sizeof (BRChainParams));
    memcpy (result, &BRTestNetParams, sizeof (BRChainParams));
    return (jlong) result;
}

/*
 * Class:     com_breadwallet_core_BRCoreChainParams
 * Method:    createJniMainnetBcashChainParams
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreChainParams_createJniMainnetBcashChainParams
        (JNIEnv *env, jclass thisClass) {
    BRChainParams *result = (BRChainParams *) calloc (1, sizeof (BRChainParams));
    memcpy (result, &BRBCashParams, sizeof (BRChainParams));
    return (jlong) result;
}

/*
 * Class:     com_breadwallet_core_BRCoreChainParams
 * Method:    createJniTestnetBcashChainParams
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL Java_com_breadwallet_core_BRCoreChainParams_createJniTestnetBcashChainParams
        (JNIEnv *env, jclass thisClass) {
    BRChainParams *result = (BRChainParams *) calloc(1, sizeof(BRChainParams));
    memcpy(result, &BRBCashTestNetParams, sizeof(BRChainParams));
    return (jlong) result;
}