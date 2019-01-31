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
public class BRCoreTransaction extends BRCoreJniReference {

    protected static final boolean JNI_COPIES_TRANSACTIONS = true;

    /**
     * Set to 'true' when this transaction is successfully registered with a wallet.
     * Once registered, the Wallet owns the JNI 'C memory' and thus on GC we won't
     * call BRTransactionFree (<jni reference>)
     */
    protected boolean isRegistered = false;

    public BRCoreTransaction (byte[] buffer) {
        this (createJniCoreTransactionSerialized (buffer));
        // ...
    }

    public BRCoreTransaction (byte[] buffer, long blockHeight, long timeStamp ) {
        this (createJniCoreTransaction (buffer, blockHeight, timeStamp));
        // ...
    }

    public BRCoreTransaction () {
        this (createJniCoreTransactionEmpty());
    }

    protected BRCoreTransaction (long jniReferenceAddress) {
        super (jniReferenceAddress);
    }

    @Override
    public void dispose () {
        if (!isRegistered)
            disposeNative ();
    }

    /**
     * Return the Transaction hash
     *
     * @return
     */
    public native byte[] getHash ();

    /**
     * The transaction's version
     *
     * @return the version is a long (from a uint32_t)
     */
    public native long getVersion ();

    /**
     *
     * @return
     */
    public native BRCoreTransactionInput[] getInputs ();

    public String[] getInputAddresses () {
        BRCoreTransactionInput[] inputs = getInputs();
        String[] addresses = new String [inputs.length];

        for (int i = 0; i < inputs.length; i++)
            addresses[i] = inputs[i].getAddress();

        return addresses;
    }

    /**
     *
     * @return
     */
    public native BRCoreTransactionOutput[] getOutputs ();

    public String[] getOutputAddresses () {
        BRCoreTransactionOutput[] outputs = getOutputs();
        String[] addresses = new String [outputs.length];

        for (int i = 0; i < outputs.length; i++)
            addresses[i] = outputs[i].getAddress();

        return addresses;
    }

    /**
     * The transaction's lockTime
     *
     * @return the lock time as a long (from a uint32_t)
     */
    public native long getLockTime ();

    public native void setLockTime (long lockTime);

    /**
     * The transaction's blockHeight.
     *
     * @return the blockHeight as a long (from a uint32_t).
     */
    public native long getBlockHeight ();

    /**
     * The transacdtion's timestamp.
     *
     * @return the timestamp as a long (from a uint32_t).
     */
    public native long getTimestamp ();

    public native void setTimestamp (long timestamp);

    // parse

    /**
     * Serialize the transaction into a byte array.
     *
     * @return the byte array
     */
    public native byte[] serialize ();

    /**
     *
     * @param input
     */
    public native void addInput (BRCoreTransactionInput input);

    /**
     *
     * @param output
     */
    public native void addOutput (BRCoreTransactionOutput output);

    /**
     * Shuffle the transaction's outputs.
     */
    public native void shuffleOutputs ();

    /**
     * The the transactions' size in bytes if signed, or the estimated size assuming
     * compact pubkey sigs

     * @return the size in bytes.
     */
    public native long getSize ();

    /**
     * The transaction's standard fee which is the minimum transaction fee needed for the
     * transaction to relay across the bitcoin network.
     * *
     * @return the fee (in Satoshis)?
     */
    public native long getStandardFee ();

    /**
     * Returns true if all the transaction's signatures exists.  This method does not verify
     * the signatures.
     *
     * @return true if all exist.
     */
    public native boolean isSigned ();


    public native void sign (BRCoreKey[] keys, int forkId);

    public void sign (BRCoreKey key, int forkId) {
        sign (new BRCoreKey[] { key }, forkId);
    }

    /**
     * Return true if this transaction satisfied the rules in:
     *      https://bitcoin.org/en/developer-guide#standard-transactions
     *
     * @return true if standard; false otherwise
     */
    public native boolean isStandard ();

    /*
         UInt256 txid = tempTx->txHash;
        UInt256 reversedHash = UInt256Reverse(txid);
        return (*env)->NewStringUTF(env, u256hex(reversedHash));
     */
    public native String getReverseHash ();

    public static native long getMinOutputAmount ();

    /**
     * Call BRTransactionFree()
     */
    public native void disposeNative ();

    protected static native void initializeNative ();

    static { initializeNative(); }

    /**
     *
     * @param buffer
     * @param blockHeight
     * @param timeStamp
     * @return
     */
    private static native long createJniCoreTransaction (byte[] buffer, long blockHeight, long timeStamp);

    private static native long createJniCoreTransactionSerialized (byte[] buffer);

    private static native long createJniCoreTransactionEmpty ();
}