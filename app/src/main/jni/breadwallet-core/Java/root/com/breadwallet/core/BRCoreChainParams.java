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
public class BRCoreChainParams extends BRCoreJniReference {

    private BRCoreChainParams (long jniReferenceAddress) {
        super (jniReferenceAddress);
    }

    //
    public native int getJniMagicNumber ();

    //
    //
    //

    public static BRCoreChainParams mainnetChainParams =
            new BRCoreChainParams(createJniMainnetChainParams());

    private static native long createJniMainnetChainParams ();

    public static BRCoreChainParams testnetChainParams =
            new BRCoreChainParams(createJniTestnetChainParams());

    private static native long createJniTestnetChainParams ();

    public static BRCoreChainParams mainnetBcashChainParams =
            new BRCoreChainParams(createJniMainnetBcashChainParams());

    private static native long createJniMainnetBcashChainParams();

    public static BRCoreChainParams testnetBcashChainParams =
            new BRCoreChainParams(createJniTestnetBcashChainParams());

    private static native long createJniTestnetBcashChainParams();
}
