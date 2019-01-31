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

#include <jni.h>
#include <BRTransaction.h>
#include <assert.h>
#include "BRCoreJni.h"

static JavaVM *jvm = NULL;

extern
JNIEnv *getEnv() {
    JNIEnv *env;

    if (NULL == jvm) return NULL;

    int status = (*jvm)->GetEnv(jvm, (void **) &env, JNI_VERSION_1_6);

    if (status != JNI_OK)
      status = (*jvm)->AttachCurrentThread(jvm, (JNIEnv **) &env, NULL);

    return status == JNI_OK ? env : NULL;
}

extern
void releaseEnv () {
    if (NULL != jvm)
        (*jvm)->DetachCurrentThread (jvm);
}

JNIEXPORT jint JNICALL
JNI_OnLoad (JavaVM *theJvm, void *reserved) {
    JNIEnv *env = 0;

    if ((*theJvm)->GetEnv(theJvm, (void **)&env, JNI_VERSION_1_6)) {
        return JNI_ERR;
    }

    jvm = theJvm;

    return JNI_VERSION_1_6;
}

//
// Support
//
extern void
transactionInputCopy(BRTxInput *target,
                     const BRTxInput *source) {
    assert (target != NULL);
    assert (source != NULL);
    *target = *source;

    target->script = NULL;
    BRTxInputSetScript(target, source->script, source->scriptLen);

    target->signature = NULL;
    BRTxInputSetSignature(target, source->signature, source->sigLen);
}

extern void
transactionOutputCopy (BRTxOutput *target,
                       const BRTxOutput *source) {
    assert (target != NULL);
    assert (source != NULL);
    *target = *source;

    target->script = NULL;
    BRTxOutputSetScript(target, source->script, source->scriptLen);
}


