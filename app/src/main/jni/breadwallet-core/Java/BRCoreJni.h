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

#ifndef COREJNI_BRCOREJVM_H
#define COREJNI_BRCOREJVM_H

#include <jni.h>
#include <BRTransaction.h>
#include "com_breadwallet_core_BRCoreJniReference.h"

/**
 *
 * @return
 */
extern JNIEnv *
getEnv();

extern void
releaseEnv ();

/**
 *
 * @param env
 * @param thisObject
 * @return
 */
extern void *
getJNIReference (
        JNIEnv *env,
        jobject thisObject);

//
// Support
//
extern void
transactionInputCopy(BRTxInput *target,
                     const BRTxInput *source);

extern void
transactionOutputCopy (BRTxOutput *target,
                       const BRTxOutput *source);

#endif //COREJNI_BRCOREJVM_H
