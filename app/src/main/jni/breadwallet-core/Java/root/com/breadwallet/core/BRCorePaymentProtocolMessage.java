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

public class BRCorePaymentProtocolMessage extends BRCoreJniReference {
    public BRCorePaymentProtocolMessage (byte[] data) {
        this (createPaymentProtocolMessage (data));
    }

    public BRCorePaymentProtocolMessage (MessageType type, byte[] message,
                                         long statusCode, String statusMsg,
                                         byte[] identifier) {
        this (createPaymentProtocolMessageFull(type.value, message, statusCode, statusMsg, identifier));
    }

    protected BRCorePaymentProtocolMessage (long jniReferenceAddress) {
        super (jniReferenceAddress);
    }

    public MessageType getMessageType () {
        return MessageType.fromValue(getMessageTypeValue ());
    }

    private native int getMessageTypeValue ();

    public native byte[] getMessage ();

    public native long getStatusCode ();

    public native String getStatusMessage ();

    public native byte[] getIdentifier ();

    private static native long createPaymentProtocolMessage (byte[] data);

    private static native long createPaymentProtocolMessageFull (int type, byte[] message,
                                                                 long statusCode, String statusMsg,
                                                                 byte[] identifier);

    public native byte[] serialize ();

    public native void disposeNative ();

    //
    //
    //
    public enum MessageType {
        Unknown(0),
        InvoiceRequest(1),
        Request(2),
        Payment(3),
        ACK(4);

        protected int value;

        MessageType(int value) {
            this.value = value;
        }

        public static MessageType fromValue (int value) {
            for (MessageType type : MessageType.values())
                if (type.value == value)
                    return type;
            return Unknown;
        }
    }

}
