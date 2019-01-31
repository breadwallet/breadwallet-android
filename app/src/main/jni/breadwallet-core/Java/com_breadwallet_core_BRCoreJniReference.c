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
#include <assert.h>
#include <malloc.h>
#include "BRCoreJni.h"

#define JNI_REFERENCE_ADDRESS_FIELD_NAME "jniReferenceAddress"
#define JNI_REFERENCE_ADDRESS_FIELD_TYPE "J" // long

static jfieldID getJNIReferenceField (
        JNIEnv *env,
        jobject thisObject)
{
    jclass thisClass = (*env)->GetObjectClass (env, thisObject);
    jfieldID thisFieldId = (*env)->GetFieldID(env, thisClass,
                              JNI_REFERENCE_ADDRESS_FIELD_NAME,
                              JNI_REFERENCE_ADDRESS_FIELD_TYPE);
    (*env)->DeleteLocalRef (env, thisClass);
    return thisFieldId;
}

static jlong getJNIReferenceAddress (
        JNIEnv *env,
        jobject thisObject)
{
    jfieldID coreBRKeyAddressField = getJNIReferenceField(env, thisObject);
    assert (NULL != coreBRKeyAddressField);

    return (*env)->GetLongField (env, thisObject, coreBRKeyAddressField);
}

extern void *getJNIReference (
        JNIEnv *env,
        jobject thisObject)
{
    return (void *) getJNIReferenceAddress(env, thisObject);
}

/*
 * Class:     com_breadwallet_core_BRCoreJniReference
 * Method:    disposeNative
 * Signature: ()V
 */
JNIEXPORT void JNICALL Java_com_breadwallet_core_BRCoreJniReference_disposeNative
        (JNIEnv *env, jobject thisObject) {
    void *reference = getJNIReference(env, thisObject);

    // Not always free(); could be BRPeerManagerFree()
    if (NULL != reference) free (reference);
}
