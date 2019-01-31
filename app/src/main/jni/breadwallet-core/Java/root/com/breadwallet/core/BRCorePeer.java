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
public class BRCorePeer extends BRCoreJniReference {

    /**
     *
     */
    public enum ConnectStatus {
        Disconnected(0),
        Connecting(1),
        Connected(2),
        Unknown(-2);

        private int value;

        public int getValue () {
            return value;
        }

        ConnectStatus (int value) {
            this.value = value;
        }

        public static ConnectStatus fromValue (int value) {
            // Just a linear search - easy, quick-enough.
            for (ConnectStatus status : ConnectStatus.values())
                if (status.value == value)
                    return status;
            return Unknown;
        }
    }


//    public String getName () {
//        return getAddress() + ":" + getPort();
//    }

    public native byte[] getAddress ();

    public native int getPort ();

    public native long getTimestamp ();

    //
    // These all require a PeerContext
    //
    private native void setEarliestKeyTime (long earliestKeyTime);

    private native void setCurrentBlockHeight (long currentBlockHeight);

    private ConnectStatus getConnectStatus () {
        return ConnectStatus.fromValue(getConnectStatusValue());
    }

    private native int getConnectStatusValue ();

    private native void connect ();

    private native void disconnect ();

    private native void scheduleDisconnect (double secoonds);

    private native void setNeedsFilterUpdate (boolean needsFilterUpdate);

    private native String getHost ();

    private native long getVersion ();

    private native String getUserAgent ();

    private native long getLastBlock ();

    private native long getFeePerKb ();

    private native double getPingTime ();

    // send message

    // send filter

    // send mempool

    // send getheaders

    // ...

    // send ping


    //
    //
    //
    public BRCorePeer (byte[] peerAddress,
                       int peerPort,
                       long timestamp) {
        this (createJniCorePeerNatural(peerAddress, peerPort, timestamp));
    }

    public BRCorePeer(byte[] peerAddress,
                      byte[] peerPort,
                      byte[] timeStamp) {
        this(createJniCorePeer(peerAddress, peerPort, timeStamp));
    }

    public BRCorePeer (int magicNumber) {
        this (createJniCorePeerMagic(magicNumber));
    }

    protected BRCorePeer(long jniReferenceAddress) {
        super(jniReferenceAddress);
    }

    private static native long createJniCorePeerNatural (byte[] peerAddress,
                                                            int peerPort,
                                                            long timeStamp);

    private static native long createJniCorePeer (byte[] peerAddress,
                                                  byte[] peerPort,
                                                  byte[] timeStamp);

    private static native long createJniCorePeerMagic(long magicNumber);
}
