package com.platform.tools;

import android.util.Log;

import com.jniwrappers.BRBase58;
import com.jniwrappers.BRKey;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Arrays;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/25/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class BRBitId {
    public static final String TAG = BRBitId.class.getName();
    public static final String BITCOIN_SIGNED_MESSAGE_HEADER = "Bitcoin Signed Message:\n";

    public static String signMessage(String message, byte[] key) {
        byte[] signingData = formatMessageForBitcoinSigning(message);
        byte[] signature = new BRKey(key).compactSign(signingData);
        return BRBase58.getInstance().base58Encode(signature);
    }

    public static byte[] formatMessageForBitcoinSigning(String message) {
        Log.e(TAG, "formatMessageForBitcoinSigning: ");
        byte[] headerBytes = null;
        byte[] messageBytes = null;
        Log.e(TAG, "head: " + BITCOIN_SIGNED_MESSAGE_HEADER);
        Log.e(TAG, "message: " + message);

        try {
            headerBytes = BITCOIN_SIGNED_MESSAGE_HEADER.getBytes("UTF-8");
            messageBytes = message.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        assert (headerBytes != null);
        assert (messageBytes != null);
        if (headerBytes == null || messageBytes == null) return new byte[0];

        int cap = 1 + headerBytes.length + 4 + messageBytes.length;
        Log.e(TAG, "cap: " + cap);
        Log.e(TAG, "messageBytes: " + Arrays.toString(messageBytes));
        Log.e(TAG, "headerBytes: " + Arrays.toString(headerBytes));
        ByteBuffer dataBuffer = ByteBuffer.allocate(cap).order(java.nio.ByteOrder.LITTLE_ENDIAN);
        dataBuffer.put((byte) headerBytes.length);          //put header count
        dataBuffer.put(headerBytes);                        //put the header
        dataBuffer.putInt(messageBytes.length);             //put message count
        dataBuffer.put(messageBytes);                       //put the message
        byte[] result = dataBuffer.array();
        Log.e(TAG, "result.length: " + result.length);
        Log.e(TAG, "result: " + Arrays.toString(result));
        return result;
    }
}
