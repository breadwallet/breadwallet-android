package com.breadwallet.tools.util;

import android.content.Context;
import android.util.Log;

import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;

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

    public static String getFormattedAmount(Context app, String iso, BigDecimal amount) {
        //Use default (wallet's maxDecimal places)
        return getFormattedAmount(app, iso, amount, -1);
    }

    /**
     * @param app    - the Context
     * @param iso    - the iso for the currency we want to format the amount for
     * @param amount - the smallest denomination currency (e.g. dollars or satoshis)
     * @param maxDecimalPlacesForCrypto - max decimal places to use or -1 for wallet's default
     * @return - the formatted amount e.g. $535.50 or b5000
     */
    public static String getFormattedAmount(Context app, String iso, BigDecimal amount, int maxDecimalPlacesForCrypto) {
        if (amount == null) return "---"; //to be able to detect in a bug
        if (Utils.isNullOrEmpty(iso)) throw new RuntimeException("need iso for formatting!");
        DecimalFormat currencyFormat;
        // This formats currency values as the user expects to read them (default locale).
        currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());
        // This specifies the actual currency that the value is in, and provide
        // s the currency symbol.
        DecimalFormatSymbols decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
        BaseWalletManager wallet = WalletsMaster.getInstance(app).getWalletByIso(app, iso);
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setRoundingMode(BRConstants.ROUNDING_MODE);
        if (wallet != null) {
            amount = wallet.getCryptoForSmallestCrypto(app, amount);
            decimalFormatSymbols.setCurrencySymbol("");
            currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
            currencyFormat.setMaximumFractionDigits(8);
            currencyFormat.setMinimumFractionDigits(0);
            return String.format("%s %s", currencyFormat.format(amount), iso.toUpperCase());
        } else {
            try {
                Currency currency = Currency.getInstance(iso);
                String symbol = currency.getSymbol();
                decimalFormatSymbols.setCurrencySymbol(symbol);
                currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
                currencyFormat.setNegativePrefix("-" + symbol);
                currencyFormat.setMaximumFractionDigits(8);
                currencyFormat.setMinimumFractionDigits(0);
            } catch (IllegalArgumentException e) {
                e.printStackTrace();
                BRReportsManager.reportBug(new IllegalArgumentException("illegal iso: " + iso));
            }
            return currencyFormat.format(amount);
        }
    }

    public static String getSymbolByIso(Context app, String iso) {
        String symbol;
        BaseWalletManager wallet = WalletsMaster.getInstance(app).getWalletByIso(app, iso);
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

    public static int getMaxDecimalPlaces(Context app, String iso) {
        BaseWalletManager wallet = WalletsMaster.getInstance(app).getWalletByIso(app, iso);
        if (wallet == null) {
            Currency currency = Currency.getInstance(iso);
            return currency.getDefaultFractionDigits();
        } else {
            return wallet.getMaxDecimalPlaces(app);
        }

    }

}
