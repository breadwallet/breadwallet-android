//
//  core.h
//
//  Created by Mihail Gutan on 9/24/2015.
//  Copyright (c) 2015 breadwallet LLC
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
#include "jni.h"

#ifndef BREADWALLET_CORE_H
#define BREADWALLET_CORE_H

#ifdef __cplusplus
extern "C" {
#endif

JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_tools_security_BitcoinUrlHandler_parsePaymentRequest(JNIEnv *env, jobject obj, jbyteArray payment);

JNIEXPORT jbyteArray JNICALL
Java_com_breadwallet_tools_security_BitcoinUrlHandler_getCertificatesFromPaymentRequest(JNIEnv *env, jobject obj,
                                                                                     jbyteArray payment, jint index);

JNIEXPORT jstring JNICALL
Java_com_breadwallet_tools_security_BitcoinUrlHandler_parsePaymentACK(JNIEnv *env, jobject obj, jbyteArray paymentACK);

#ifdef __cplusplus
}
#endif

#endif //BREADWALLET_CORE_H
