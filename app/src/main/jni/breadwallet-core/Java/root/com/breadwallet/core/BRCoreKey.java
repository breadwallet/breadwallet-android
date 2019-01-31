/*
 * BreadWallet
 *
 * Created by Ed Gamble <ed@breadwallet.com> on 1/30/18.
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

public class BRCoreKey extends BRCoreJniReference {

    //
    // PrivateKey-Based Constructors
    //

    public BRCoreKey (String privateKey) {
        this (createJniCoreKey());

        if (null == privateKey || 0 == privateKey.length())
            throw new NullPointerException("key is empty");

        if (!setPrivKey(privateKey)) {
            throw new IllegalArgumentException("Failed to set PrivKey");
        }
    }

    public BRCoreKey (byte[] privateKey) {
        this (BRCoreKey.encodeASCII(privateKey));
    }

    //
    // Secret-Based Constructors
    //

    public BRCoreKey (byte[] secret, boolean compressed) {
        this (createJniCoreKey());

        if (!setSecret(secret, compressed)) {
            throw new IllegalArgumentException("Failed to set Secret");
        }
    }

    //
    // Seed (BIP32) Based Constructors
    //

    public BRCoreKey (byte[] seed, long chain, long index) {
        this (createCoreKeyForBIP32(seed, chain, index));
    }

    //
    // Internal, JNI-based Constructor
    //

    protected BRCoreKey (long jniReferenceAddress) {
        super (jniReferenceAddress);
    }

    // TEST ONLY - Invalid Key
    public BRCoreKey () {
        this (createJniCoreKey());
    }

    /**
     * Get the byte[] representation of the 256 bit (UInt256) secret
     *
     * @return
     */
    public native byte[] getSecret ();

    /**
     * Get the byte[] representation of the 65 byte public key.
     * @return
     */
    public native byte[] getPubKey ();

    /**
     * Returns true if compressed.
     *
     * @return
     */
    public native int getCompressed ();

    /**
     *
     * @return
     */
    public native String getPrivKey ();

    //
    //
    //
    public static native byte[] getSeedFromPhrase (byte[] phrase);

    public static native byte[] getAuthPrivKeyForAPI (byte[] seed);

    public static native String getAuthPublicKeyForAPI (byte[] privKey);

    public static native String decryptBip38Key (String privKey, String pass);

    //
    //
    //
    private static native long createJniCoreKey ();

    private static native long createCoreKeyForBIP32 (byte[] seed, long chain, long index);
    //
    //
    //
    public native boolean setPrivKey(String privKey);

    private native boolean setSecret(byte[] secret, boolean compressed);

    public native byte[] compactSign(byte[] data);

    public native byte[] encryptNative(byte[] data, byte[] nonce);

    public native byte[] decryptNative(byte[] data, byte[] nonce);

    //
    //
    //
    public native String address();

    //
    //
    //
    public static native boolean isValidBitcoinPrivateKey(String key);

    public static native boolean isValidBitcoinBIP38Key(String key);

    public static String encodeHex(byte[] in) {
        final StringBuilder builder = new StringBuilder();
        for (byte b : in) {
            builder.append(String.format("%02x", b));
        }
        return builder.toString();
    }

    public static byte[] decodeHex(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    public static String encodeASCII (byte[] in) {
        return new String (in);
    }

    public static byte[] decodeASCII (String s) {
        return s.getBytes();
    }

    /* Returns 'messageDigest (UInt256) */
    public static native byte[] encodeSHA256 (String message);

    /* Returns 'signature' */
    public native byte[] sign (byte[] messageDigest);

    public native boolean verify (byte[] messageDigest, byte[] signature);
}
