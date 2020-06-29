package com.breadwallet.tools.util;

import android.content.Context;
import android.util.Log;

import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;

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
    private static final String KRONE = "DKK";
    private static final String POUND = "GBP";
    private static final String EURO = "EUR";

    /**
     * @param app                       - the Context
     * @param iso                       - the iso for the currency we want to format the amount for
     * @param amount                    - the smallest denomination currency (e.g. dollars or satoshis)
     * @param maxDecimalPlacesForCrypto - max decimal places to use or -1 for wallet's default
     * @return - the formatted amount e.g. $535.50 or b5000
     */
    @Deprecated
    public static String getFormattedAmount(Context app, String iso, BigDecimal amount, int maxDecimalPlacesForCrypto) {
        // TODO: Once callers have been migrated to getFormattedCryptoAmount or getFormattedFiatAmount, delete this
        if (amount == null) {
            amount = BigDecimal.ZERO;
        }
        if (Utils.isNullOrEmpty(iso)) throw new RuntimeException("need iso for formatting!");
        DecimalFormat currencyFormat;
        // This formats currency values as the user expects to read them (default locale).
        currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());
        // This specifies the actual currency that the value is in, and provide
        // s the currency symbol.
        DecimalFormatSymbols decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setRoundingMode(BRConstants.ROUNDING_MODE);
        /* TODO getCryptoForSmallestCrypto
        amount = wallet.getCryptoForSmallestCrypto(app, amount);
        decimalFormatSymbols.setCurrencySymbol("");
        currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setMaximumFractionDigits(maxDecimalPlacesForCrypto == -1 ? wallet.getMaxDecimalPlaces(app) : maxDecimalPlacesForCrypto);
        currencyFormat.setMinimumFractionDigits(0);
        return String.format("%s %s", currencyFormat.format(amount), iso.toUpperCase());
         */
        try {
            Currency currency = Currency.getInstance(iso);
            String symbol = currency.getSymbol();
            decimalFormatSymbols.setCurrencySymbol(symbol);
            currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
            currencyFormat.setNegativePrefix("-" + symbol);
            currencyFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());
            currencyFormat.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Currency not found for " + iso, e);
            BRReportsManager.reportBug(new IllegalArgumentException("Illegal currency code: " + iso));
        }
        return currencyFormat.format(amount);
    }

    /**
     * Returns a formatted fiat amount using the locale-specific format
     *
     * @param currencyCode the fiat currency code
     * @param amount the amount to format
     * @return the formatted string
     */
    public static String getFormattedFiatAmount(String currencyCode, BigDecimal amount) {
        // This formats currency values as the user expects to read them (default locale)
        DecimalFormat currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance(Locale.getDefault());
        DecimalFormatSymbols decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setRoundingMode(BRConstants.ROUNDING_MODE);
        try {
            Currency currency = Currency.getInstance(currencyCode);
            String symbol = currency.getSymbol();
            decimalFormatSymbols.setCurrencySymbol(symbol);
            currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
            currencyFormat.setNegativePrefix("-" + symbol);
            currencyFormat.setMaximumFractionDigits(currency.getDefaultFractionDigits());
            currencyFormat.setMinimumFractionDigits(currency.getDefaultFractionDigits());
        } catch (IllegalArgumentException e) {
            Log.e(TAG, "Illegal Currency code: " + currencyCode);
            BRReportsManager.reportBug(new IllegalArgumentException("illegal iso: " + currencyCode));
        }
        return currencyFormat.format(amount);
    }

    public static boolean isBuyNotificationNeeded() {
        String fiatCurrencyCode = BRSharedPrefs.getPreferredFiatIso();
        return KRONE.equalsIgnoreCase(fiatCurrencyCode) || POUND.equalsIgnoreCase(fiatCurrencyCode) || EURO.equalsIgnoreCase(fiatCurrencyCode);
    }
}
