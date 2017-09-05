package com.platform.entities;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 6/22/17.
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
public class TxMetaData {

    /**
     * Key: “txn-<txHash>”
     * <p>
     * {
     * “classVersion”: 5, //used for versioning the schema
     * “bh”: 47583, //blockheight
     * “er”: 2800.1, //exchange rate
     * “erc”: “USD”, //exchange currency
     * “fr”: 300, //fee rate
     * “s”: fd, //size
     * “c”: 123475859 //created
     * “dId”: ”<UUID>” //DeviceId - This is a UUID that gets generated and then persisted so it can get sent with every tx
     * “comment”: “Vodka for Mihail”
     * }
     */

    public String deviceId;
    public String comment;
    public String exchangeCurrency;
    public int classVersion;
    public int blockHeight;
    public double exchangeRate;
    public long fee;
    public int txSize;
    public int creationTime;

}
