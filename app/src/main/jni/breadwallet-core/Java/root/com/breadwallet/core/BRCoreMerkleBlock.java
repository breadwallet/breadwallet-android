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

import java.util.Arrays;

/**
 *
 */
public class BRCoreMerkleBlock extends BRCoreJniReference {

    public BRCoreMerkleBlock(byte[] block, int blockHeight) {
        this (createJniCoreMerkleBlock (block, blockHeight));
    }

    protected BRCoreMerkleBlock (long jniReferenceAddress) {
        super (jniReferenceAddress);
    }

    // Test
    public BRCoreMerkleBlock () {
        this (createJniCoreMerkleBlockEmpty());
    }

    private static native long createJniCoreMerkleBlock (byte[] block, int blockHeight);

    // Test
    private static native long createJniCoreMerkleBlockEmpty ();

    public native byte[] getBlockHash ();

    public native long getVersion ();

    public native byte[] getPrevBlockHash ();

    public native byte[] getRootBlockHash ();

    public native long getTimestamp ();

    public native long getTarget ();

    public native long getNonce ();

    public native long getTransactionCount ();

    // hashes

    // flags

    public native long getHeight ();

    /**
     * Serialize the transaction into a byte array.
     *
     * @return the byte array
     */
    public native byte[] serialize ();

    public native boolean isValid (long currentTime);

    /**
     * True if the given hash is known to be included in the block.  [The 'blockHash' is not
     * 'included in the block']
     *
     * @param hash
     * @return
     */
    public native boolean containsTransactionHash (byte[] hash);

    // verify difficulty

    // To call BRMerkleBlockFree()
    public native void disposeNative ();
}
