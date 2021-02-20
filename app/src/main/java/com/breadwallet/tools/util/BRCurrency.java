package com.breadwallet.tools.util;

import android.content.Context;

import com.breadwallet.tools.manager.BRSharedPrefs;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

import static com.breadwallet.tools.util.BRConstants.CURRENT_UNIT_PHOTONS;

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

public class BRCurrency {
    public static final String TAG = BRCurrency.class.getName();


    // amount is in currency or BTC (bits, mBTC or BTC)
    public static String getFormattedCurrencyString(Context app, String isoCurrencyCode, BigDecimal amount) {
        // This formats currency values as the user expects to read them (default locale).
        DecimalFormat  currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());
        // This specifies the actual currency that the value is in, and provide
        // s the currency symbol.
        DecimalFormatSymbols decimalFormatSymbols;
        Currency currency;
        String symbol;
        decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
        if (Objects.equals(isoCurrencyCode, "LTC")) {
            symbol = BRExchange.getBitcoinSymbol(app);
        } else {
            try {
                currency = Currency.getInstance(isoCurrencyCode);
            } catch (IllegalArgumentException e) {
                currency = Currency.getInstance(Locale.getDefault());
            }
            symbol = currency.getSymbol();
        }
        decimalFormatSymbols.setCurrencySymbol(symbol);
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setMaximumFractionDigits(BRSharedPrefs.getCurrencyUnit(app) == BRConstants.CURRENT_UNIT_LITECOINS ? 8 : 2);
        currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setNegativePrefix(decimalFormatSymbols.getCurrencySymbol() + "-");
        currencyFormat.setNegativeSuffix("");
        return currencyFormat.format(amount.doubleValue());
    }

    public static String getSymbolByIso(Context app, String iso) {
        String symbol;
        if (Objects.equals(iso, "LTC")) {
            String currencySymbolString = BRConstants.bitcoinLowercase;
            if (app != null) {
                int unit = BRSharedPrefs.getCurrencyUnit(app);
                switch (unit) {
                    case CURRENT_UNIT_PHOTONS:
                        currencySymbolString = BRConstants.bitcoinLowercase;
                        break;
                    case BRConstants.CURRENT_UNIT_LITES:
                        currencySymbolString = "m" + BRConstants.bitcoinUppercase;
                        break;
                    case BRConstants.CURRENT_UNIT_LITECOINS:
                        currencySymbolString = BRConstants.bitcoinUppercase;
                        break;
                }
            }
            symbol = currencySymbolString;
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
        if (Objects.equals(iso, "LTC")) {
            if (app != null) {
                int unit = BRSharedPrefs.getCurrencyUnit(app);
                switch (unit) {
                    case CURRENT_UNIT_PHOTONS:
                        return "Bits";
                    case BRConstants.CURRENT_UNIT_LITES:
                        return "MBits";
                    case BRConstants.CURRENT_UNIT_LITECOINS:
                        return "LTC";
                }
            }
        }
        return iso;
    }

    public static int getMaxDecimalPlaces(String iso) {
        if (Utils.isNullOrEmpty(iso)) return 8;

        if (iso.equalsIgnoreCase("LTC")) {
            return 8;
        } else {
            Currency currency = Currency.getInstance(iso);
            return currency.getDefaultFractionDigits();
        }

    }


}
