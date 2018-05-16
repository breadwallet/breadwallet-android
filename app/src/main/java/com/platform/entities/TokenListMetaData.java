package com.platform.entities;


import android.util.Log;

import com.breadwallet.presenter.entities.TokenItem;

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

    public static final int CLASS_VERSION = 2;
    public List<TokenInfo> enabledCurrencies;
    public List<TokenInfo> hiddenCurrencies;

    public TokenListMetaData(List<TokenInfo> enabledCurrencies, List<TokenInfo> hiddenCurrencies) {
        this.enabledCurrencies = enabledCurrencies;
        this.hiddenCurrencies = hiddenCurrencies;

        if (this.enabledCurrencies == null) {
            this.enabledCurrencies = new ArrayList<>();
            this.enabledCurrencies.add(new TokenInfo("BTC", false, null));
            this.enabledCurrencies.add(new TokenInfo("BCH", false, null));
            this.enabledCurrencies.add(new TokenInfo("ETH", false, null));
        }
        if (this.hiddenCurrencies == null) this.hiddenCurrencies = new ArrayList<>();
    }

    public synchronized boolean isCurrencyHidden(String symbol) {
        if (hiddenCurrencies == null || hiddenCurrencies.size() == 0) return false;
        for (TokenInfo info : hiddenCurrencies) {
            if (info.symbol.equalsIgnoreCase(symbol)) return true;
        }
        return false;

    }


    public synchronized void showCurrency(String symbol) {
        if (hiddenCurrencies == null) return;
        for (int i = 0; i < hiddenCurrencies.size(); i++) {

            TokenInfo info = hiddenCurrencies.get(i);

            if (info.symbol.equalsIgnoreCase(symbol)) {
                hiddenCurrencies.remove(info);
            }
        }


    }

    public boolean isCurrencyEnabled(String symbol) {
        if (enabledCurrencies == null || enabledCurrencies.size() == 0) return false;
        for (TokenInfo info : enabledCurrencies)
            if (info.symbol.equalsIgnoreCase(symbol)) return true;
        return false;
    }

    public synchronized void disableCurrency(String symbol) {
        if (enabledCurrencies == null || enabledCurrencies.size() == 0) return;
        for (int i = 0; i < enabledCurrencies.size(); i++) {
            TokenInfo info = enabledCurrencies.get(i);

            if (info.symbol.equalsIgnoreCase(symbol))
                enabledCurrencies.remove(info);
        }
    }

    public static class TokenInfo {
        public String symbol;
        public boolean erc20;
        public String contractAddress;

        public TokenInfo(String symbol, boolean erc20, String contractAddress) {
            this.symbol = symbol;
            this.erc20 = erc20;
            this.contractAddress = contractAddress;
        }

        @Override
        public String toString() {
            return erc20 ? symbol + ":" + contractAddress : symbol;
        }
    }


}
