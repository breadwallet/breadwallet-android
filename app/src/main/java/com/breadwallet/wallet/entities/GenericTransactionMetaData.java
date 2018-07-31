package com.breadwallet.wallet.entities;

import com.breadwallet.core.ethereum.BREthereumAmount;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/30/18.
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
public class GenericTransactionMetaData {
    String targetAddress;
    String amount;
    BREthereumAmount.Unit amountUnit;
    long gasPrice;
    BREthereumAmount.Unit gasPriceUnit;
    long gasLimit;
    String data;

    public GenericTransactionMetaData(String targetAddress, String amount, BREthereumAmount.Unit amountUnit,
                                      long gasPrice, BREthereumAmount.Unit gasPriceUnit, long gasLimit, String data) {
        this.targetAddress = targetAddress;
        this.amount = amount;
        this.amountUnit = amountUnit;
        this.gasPrice = gasPrice;
        this.gasPriceUnit = gasPriceUnit;
        this.gasLimit = gasLimit;
        this.data = data;
    }

    public String getTargetAddress() {
        return targetAddress;
    }

    public String getAmount() {
        return amount;
    }

    public BREthereumAmount.Unit getAmountUnit() {
        return amountUnit;
    }

    public long getGasPrice() {
        return gasPrice;
    }

    public BREthereumAmount.Unit getGasPriceUnit() {
        return gasPriceUnit;
    }

    public long getGasLimit() {
        return gasLimit;
    }

    public String getData() {
        return data;
    }
}