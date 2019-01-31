//
//  com_breadwallet_core_ethereum_BREthereumNetwork
//  breadwallet-core Ethereum
//
//  Created by Ed Gamble on 3/20/18.
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

#include "com_breadwallet_core_ethereum_BREthereumNetwork.h"
#include "BREthereumNetwork.h"

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumNetwork
 * Method:    jniGetMainnet
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumNetwork_jniGetMainnet
        (JNIEnv *env, jclass thisClass) {
    return (jlong) ethereumMainnet;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumNetwork
 * Method:    jniGetTestnet
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumNetwork_jniGetTestnet
        (JNIEnv *env, jclass thisClass) {
    return (jlong) ethereumTestnet;
}

/*
 * Class:     com_breadwallet_core_ethereum_BREthereumNetwork
 * Method:    jniGetRinkeby
 * Signature: ()J
 */
JNIEXPORT jlong JNICALL
Java_com_breadwallet_core_ethereum_BREthereumNetwork_jniGetRinkeby
        (JNIEnv *env, jclass thisClass) {
    return (jlong) ethereumRinkeby;
}

