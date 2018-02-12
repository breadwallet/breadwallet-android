package com.breadwallet.tools.util;

import android.content.Context;

import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.interfaces.BaseWallet;
import com.breadwallet.wallet.wallets.WalletBitcoin;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/28/16.
 * Copyright (c) 2016 breadwallet LLC
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

public class CurrencyUtils {
    public static final String TAG = CurrencyUtils.class.getName();


    // amount is in currency or BTC (bits, mBTC or BTC)
    public static String getFormattedCurrencyString(Context app, String isoCurrencyCode, BigDecimal amount) {
//        Log.e(TAG, "amount: " + amount);
        DecimalFormat currencyFormat;

        // This formats currency values as the user expects to read them (default locale).
        currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());
        // This specifies the actual currency that the value is in, and provide
        // s the currency symbol.
        DecimalFormatSymbols decimalFormatSymbols;
        Currency currency;
        String symbol = null;
        decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
//        int decimalPoints = 0;
        if (Objects.equals(isoCurrencyCode, "BTC")) {
            symbol = WalletBitcoin.getInstance().getSymbol(app);
        } else {
            try {
                currency = Currency.getInstance(isoCurrencyCode);
            } catch (IllegalArgumentException e) {
                currency = Currency.getInstance(Locale.getDefault());
            }
            symbol = currency.getSymbol();
//            decimalPoints = currency.getDefaultFractionDigits();
        }
        decimalFormatSymbols.setCurrencySymbol(symbol);
//        currencyFormat.setMaximumFractionDigits(decimalPoints);
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setMaximumFractionDigits(BRSharedPrefs.getCurrencyUnit(app) == BRConstants.CURRENT_UNIT_BITCOINS ? 8 : 2);
        currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setNegativePrefix(decimalFormatSymbols.getCurrencySymbol() + "-");
        currencyFormat.setNegativeSuffix("");
        return currencyFormat.format(amount.doubleValue());
    }

    public static String getSymbolByIso(Context app, String iso) {
        String symbol;
        BaseWallet wallet = WalletsMaster.getInstance().getWalletByIso(iso);
        if (wallet != null) {
            symbol = wallet.getSymbol(app);
        } else {
            Currency currency;
            try {
                currency = Currency.getInstance(iso);
            } catch (IllegalArgumentException e) {
                currency = Currency.getInstance(Locale.getDefault());
            }
            symbol = currency.getSymbol();
        }
        return Utils.isNullOrEmpty(symbol) ? iso : symbol;
    }

    //for now only use for BTC and Bits
    public static String getCurrencyName(Context app, String iso) {
        BaseWallet wallet = WalletsMaster.getInstance().getWalletByIso(iso);
        if (wallet == null) return iso;
        return wallet.getName(app);
    }

    public static int getMaxDecimalPlaces(Context app, String iso) {
        BaseWallet wallet = WalletsMaster.getInstance().getWalletByIso(iso);
        if (wallet == null) {
            Currency currency = Currency.getInstance(iso);
            return currency.getDefaultFractionDigits();
        } else {
            return wallet.getMaxDecimalPlaces(app);
        }

    }

}
