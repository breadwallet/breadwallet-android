package com.breadwallet.tools.crypto;

import android.text.format.DateUtils;

import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.nio.ByteOrder;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/28/16.
 * Copyright (c) 2016 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class CryptoHelper {
    public static final String TAG = CryptoHelper.class.getName();
    private static final int NONCE_SIZE = 12;

    public static String base58ofSha256(byte[] toEncode) {
        byte[] sha256First = sha256(toEncode);
        return Base58.encode(sha256First);
    }

    public static byte[] doubleSha256(byte[] data) {
        byte[] sha256First = sha256(data);
        return sha256(sha256First);
    }

    public static byte[] sha256(byte[] data) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("SHA-256");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return digest.digest(data);
    }

    public static byte[] md5(byte[] data) {
        MessageDigest digest = null;
        try {
            digest = MessageDigest.getInstance("MD5");
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return null;
        }
        return digest.digest(data);
    }

    /**
     * generate a nonce using microseconds-since-epoch
     */
    public static byte[] generateRandomNonce() {
        byte[] nonce = new byte[NONCE_SIZE];
        ByteBuffer buffer = ByteBuffer.allocate(8);
        long t = System.nanoTime() / DateUtils.SECOND_IN_MILLIS;
        buffer.order(ByteOrder.LITTLE_ENDIAN);
        buffer.putLong(t);
        byte[] byteTime = buffer.array();
        System.arraycopy(byteTime, 0, nonce, 4, byteTime.length);
        return nonce;
    }

}
