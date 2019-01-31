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
public class BRCoreAddress extends BRCoreJniReference {
    public static BRCoreAddress createAddress (String address) {
        return null == address || address.isEmpty()
                ? null
                : new BRCoreAddress (address);
    }

    public BRCoreAddress (String address) {
        this (createCoreAddress (address));
    }

    protected BRCoreAddress (long jniReferenceAddress) {
        super (jniReferenceAddress);
    }

    protected static native long createCoreAddress (String address);

    protected static native long createCoreAddressFromScriptPubKey (byte[] script);

    public static BRCoreAddress fromScriptPubKey (byte[] script) {
        return new BRCoreAddress (createCoreAddressFromScriptPubKey (script));
    }

    protected static native long createCoreAddressFromScriptSignature (byte[] script);

    public static BRCoreAddress fromScriptSignature (byte[] script) {
        return new BRCoreAddress (createCoreAddressFromScriptSignature (script));
    }

    public native String stringify ();

    public native boolean isValid ();

    public native byte[] getPubKeyScript();

    /**
     * Decode a bitcash address into a bitcoin address.
     *
     * @param bcashAddr the bitcash address
     * @return the bitcoin address or NULL if unable to decode
     */
    public static native String bcashDecodeBitcoin (String bcashAddr);

    /**
     * Encode a bitcash address from a bitcoin address.
     *
     * @param bitcoinAddr the bitcoin address
     * @return a bitcash address or NULL if unable to encode
     */
    public static native String bcashEncodeBitcoin (String bitcoinAddr);
}
