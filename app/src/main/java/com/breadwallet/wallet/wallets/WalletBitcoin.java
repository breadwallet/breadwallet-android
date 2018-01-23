package com.breadwallet.wallet.wallets;

import android.content.Context;

import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.ExchangeUtils;
import com.breadwallet.wallet.interfaces.BaseWallet;

import java.math.BigDecimal;

import static com.breadwallet.tools.util.BRConstants.CURRENT_UNIT_BITS;

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
public class WalletBitcoin implements BaseWallet {

    public static final long MAX_BTC = 21000000;

    private static WalletBitcoin instance;

    public static WalletBitcoin getInstance() {
        if (instance == null) instance = new WalletBitcoin();
        return instance;
    }

    private WalletBitcoin() {
    }


    @Override
    public boolean sendTransaction(Context app, PaymentItem item) {
        return false;
    }

    @Override
    public boolean generateWallet(Context app) {
        return false;
    }

    @Override
    public boolean initWallet(Context app) {
        return false;
    }

    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.bitcoinLowercase;
        if (app != null) {
            int unit = BRSharedPrefs.getCurrencyUnit(app);
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
    public String getName(Context app) {
        if (app == null) return null;
        int unit = BRSharedPrefs.getCurrencyUnit(app);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return "Bits";
            case BRConstants.CURRENT_UNIT_MBITS:
                return "MBits";
            default:
                return "BTC";
        }

    }


    @Override
    public int getMaxDecimalPlaces(Context app) {
        int unit = BRSharedPrefs.getCurrencyUnit(app);
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
    public BigDecimal maxAmount(Context app) {
        //return max bitcoin
        return new BigDecimal(MAX_BTC);
    }

}
