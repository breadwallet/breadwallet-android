package com.breadwallet.wallet.wallets;

import android.content.Context;

import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.interfaces.BaseWallet;
import com.breadwallet.wallet.wallets.configs.WalletUiConfiguration;

import java.math.BigDecimal;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/22/18.
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
public class WalletBitcoinCash implements BaseWallet {

    public final long MAX_BTC = 21000000;

    private static WalletBitcoinCash instance;
    private WalletUiConfiguration uiConfig;

    public static WalletBitcoinCash getInstance() {
        if (instance == null) instance = new WalletBitcoinCash();
        return instance;
    }

    private WalletBitcoinCash() {
        uiConfig = new WalletUiConfiguration("#478559", true, true, false);
    }

    @Override
    public boolean sendTransaction(Context app, PaymentItem item) {
        //todo implement
        return false;
    }

    @Override
    public boolean generateWallet(Context app) {
        //No need, we generate one private key for all wallets
        return false;
    }

    @Override
    public boolean initWallet(Context app) {
        //todo implement
        return false;
    }

    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.bitcoinLowercase;
        if (app != null) {
            int unit = BRSharedPrefs.getBitcoinUnit(app);
            switch (unit) {
                case BRConstants.CURRENT_UNIT_BITS:
                    currencySymbolString = BRConstants.bitcoinLowercase;
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + BRConstants.bitcoinUppercase;
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString = BRConstants.bitcoinUppercase;
                    break;
            }
        }
        return currencySymbolString;
    }

    @Override
    public String getIso(Context app) {
        if (app == null) return null;
        int unit = BRSharedPrefs.getBitcoinUnit(app);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return "Bits";
            case BRConstants.CURRENT_UNIT_MBITS:
                return "MBits";
            default:
                return "BCH";
        }
    }

    @Override
    public String getName(Context app) {
        return "BitcoinCash";
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        int unit = BRSharedPrefs.getBitcoinUnit(app);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return 2;
            case BRConstants.CURRENT_UNIT_MBITS:
                return 5;
            default:
                return 8;
        }
    }

    @Override
    public long getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, "BCH");
    }

    @Override
    public void setCashedBalance(Context app, long balance) {
        BRSharedPrefs.putCachedBalance(app, "BCH", balance);
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        //return max bitcoin
        return new BigDecimal(MAX_BTC);
    }

    @Override
    public boolean tryUri(Context app, String uriStr) {
        return false;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return uiConfig;
    }
}
