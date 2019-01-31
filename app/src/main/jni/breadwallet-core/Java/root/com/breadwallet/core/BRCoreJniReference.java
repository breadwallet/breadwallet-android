/*
 * BreadWallet
 *
 * Created by Ed Gamble <ed@breadwallet.com> on 1/22/18.
 * Copyright (c) 2018 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.core;

/**
 *
 */
public abstract class BRCoreJniReference {

    protected static boolean SHOW_FINALIZE = false;
    /**
     * C Pointer (as a Java long) to the underlying Breadwallet Core entity allocated from the
     * C heap memory.  The referenced Core entity is used to implement native functions that
     * call Core functions (and thus expect a Core entity).
     *
     * The address must be determined in a subclass specific way and thus must be provided in the
     * subclasses constructor.
     */
    protected long jniReferenceAddress;

    protected BRCoreJniReference (long jniReferenceAddress)
    {
        this.jniReferenceAddress = jniReferenceAddress;
    }

    //
    //
    //
    protected void finalize () throws Throwable {
        if (SHOW_FINALIZE) System.err.println("Finalize: " + toString());
        dispose ();
    }

    public void dispose () {
        disposeNative ();
    }

    public native void disposeNative ();

    public String toString() {
        return getClass().getName() + "@" + Integer.toHexString(hashCode()) + " JNI=" + Long.toHexString(jniReferenceAddress);
    }
}
