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
public class BRCoreMasterPubKey extends BRCoreJniReference {

    /**
     * A serialization of `this` - it will be machine dependent as the serialization is simply
     * the instances raw bytes.
     *
     * @return
     */
    public native byte[] serialize ();

    /**
     * The MasterPubKey's public key as byte[].
     *
     * @return
     */
    public native byte[] getPubKey ();

    /**
     * The MasterPubKey's public key as BRCoreKey
     *
     * @return
     */
    public BRCoreKey getPubKeyAsCoreKey () {
        return new BRCoreKey (createPubKey ());
    }


    /**
     * Constructor from `bytes`.  If `isPaperKey` is true, then `bytes` represents that
     * 12-word 'paper key' string; otherwise, `bytes` is the serialization returned by
     * serialize()
     *
     * @param bytes either 'paper key' (as byte[]) or 'pub key'
     * @param isPaperKey true is 'paper key'
     */
    public BRCoreMasterPubKey (byte[] bytes, boolean isPaperKey) {
        this (isPaperKey
                ? createJniCoreMasterPubKeyFromPhrase(bytes)
                : createJniCoreMasterPubKeyFromSerialization(bytes));
    }

    private BRCoreMasterPubKey (long jniReferenceAddress) {
        super (jniReferenceAddress);
    }

    //
    // Native Support Methods
    //
    private native long createPubKey ();

    private static native long createJniCoreMasterPubKeyFromPhrase (byte[] phrase);

    private static native long createJniCoreMasterPubKeyFromSerialization(byte[] pubKey);

    /**
     *
     * @param seed
     * @param index
     * @param uri
     * @return
     */
    public static native byte[] bip32BitIDKey(byte[] seed, int index, String uri);

    /**
     * Validate `phrase` using `words`.  Essentially, every word in `phrase` must be found in
     * `words`
     *
     * @param words
     * @param phrase
     * @return
     */
    public static native boolean validateRecoveryPhrase(String[] words, String phrase);

    /**
     * Generate a 'paper key' given a random seed and array of words[2048].  This method will
     * fail, with assert(false), is seed is not of size 16 and words are not of size 2048.
     *
     * @param seed  The result of new SecureRandom().generateSeed(16)
     * @param words Valid words.
     * @return
     */
    public static native byte[] generatePaperKey (byte[] seed, String[] words);
}
