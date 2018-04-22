package com.breadwallet.wallet.configs;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/25/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class WalletUiConfiguration {
    public String mStartColor;
    public String mEndColor;

    public boolean showRequestAnAmount;

    public WalletUiConfiguration(String startColor, String endColor, boolean showRequestAnAmount) {
        this.mStartColor = startColor.contains("#") ? startColor : "#" + startColor;
        this.mEndColor = endColor != null ? (endColor.contains("#") ? endColor : "#" + endColor) : this.mStartColor;
        this.showRequestAnAmount = showRequestAnAmount;
        //temp
        if(mStartColor.equalsIgnoreCase("#fafafa")) mStartColor = "#6AAC6D";
        if(mEndColor.equalsIgnoreCase("#fafafa")) mEndColor = "#549957";
    }

}
