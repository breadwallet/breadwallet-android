package com.platform.entities;


import java.util.ArrayList;
import java.util.List;

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
public class TokenListMetaData {
    /**
     * WalletInfo:
     * <p>
     * Key: “wallet-info”
     * <p>
     * {
     * “classVersion”: 2, //used for versioning the schema
     * "enabledCurrencies": ["btc":"eth": "erc20:0xsd98fjetc"] enabled currencies
     * "hiddenCurrencies": "bch"] hidden currencies
     * }
     */

    public int classVersion;
    public List<TokenItem> enabledCurrencies;
    public List<TokenItem> hiddenCurrencies;

    public TokenListMetaData(int classVersion, List<TokenItem> enabledCurrencies, List<TokenItem> hiddenCurrencies) {
        this.classVersion = classVersion;
        this.enabledCurrencies = enabledCurrencies;
        this.hiddenCurrencies = hiddenCurrencies;

        if (this.enabledCurrencies == null) {
            this.enabledCurrencies = new ArrayList<>();
            this.enabledCurrencies.add(new TokenItem("BTC", false, null));
            this.enabledCurrencies.add(new TokenItem("BCH", false, null));
            this.enabledCurrencies.add(new TokenItem("ETH", false, null));
        }
        if (this.hiddenCurrencies == null) this.hiddenCurrencies = new ArrayList<>();
    }


    public static class TokenItem {
        public String name;
        public boolean erc20;
        public String contractAddress;

        public TokenItem(String name, boolean erc20, String contractAddress) {
            this.name = name;
            this.erc20 = erc20;
            this.contractAddress = contractAddress;
        }

        @Override
        public String toString() {
            return erc20 ? name + ":" + contractAddress : name;
        }
    }

}
