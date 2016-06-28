package com.breadwallet.tools;

import android.app.Activity;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.adapter.AmountAdapter;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.text.DecimalFormat;
import java.text.DecimalFormatSymbols;
import java.text.NumberFormat;
import java.util.Currency;
import java.util.Locale;
import java.util.Objects;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 6/28/16.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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
public class BRStringFormatter {
    public static final String TAG = BRStringFormatter.class.getName();


    public static String getMiddleTextExchangeString(double rate, String iso, Activity ctx) {
//        Log.e(TAG, "result of the exchange rate calculation: " + result);
        if (rate == 0) rate = 1;
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return null;
        long result = BRWalletManager.getInstance(ctx).bitcoinAmount(100, new BigDecimal(String.valueOf(rate)).multiply(new BigDecimal("100")).doubleValue());
        return getFormattedCurrencyString(iso, 100) + " = " +
                getFormattedCurrencyString("BTC", result);
    }

    public static String getBitsAndExchangeString(double rate, String iso, BigDecimal target, Activity ctx) {
        Log.e(TAG, "target: " + target);
//        Log.e(TAG, "result of the exchange rate calculation: " + result);
        if (rate == 0) rate = 1;
        long exchange = BRWalletManager.getInstance(ctx).localAmount(target.longValue(),
                new BigDecimal(String.valueOf(rate)).multiply(new BigDecimal("100")).doubleValue());
        return getFormattedCurrencyString("BTC", target.longValue()) + " = " +
                getFormattedCurrencyString(iso, exchange);
    }

    public static String getExchangeForAmount(double rate, String iso, BigDecimal target, Activity ctx) {
        if (rate == 0) rate = 1;
        long exchange = BRWalletManager.getInstance(ctx).localAmount(target.longValue(),
                new BigDecimal(String.valueOf(rate)).multiply(new BigDecimal("100")).doubleValue());
        if (ctx != null) {
//            long exchangeFromCore = BRWalletManager.getInstance(ctx).localAmount(new Double(target).longValue(),rate);
            Log.e(TAG, "exchange: " + exchange);
//            Log.e(TAG,"exchangeFromCore: " + exchangeFromCore);
        }
        return getFormattedCurrencyString(iso, exchange);
    }

    public static String getCurrentBalanceText(Activity ctx) {
        CurrencyManager cm = CurrencyManager.getInstance(ctx);
        String iso = SharedPreferencesManager.getIso(ctx);
        double rate = SharedPreferencesManager.getRate(ctx);
        long exchange = BRWalletManager.getInstance(ctx).localAmount(cm.getBALANCE(), new BigDecimal(String.valueOf(rate)).multiply(new BigDecimal("100")).doubleValue());
        Log.e(TAG, "getCurrentBalanceText, exchange: " + exchange);

        return getFormattedCurrencyString("BTC", cm.getBALANCE()) + " (" +
                getFormattedCurrencyString(iso, exchange) + ")";
    }

    public static String getFormattedCurrencyString(String isoCurrencyCode, long amount) {
        DecimalFormat currencyFormat;

        // This formats currency values as the user expects to read them (default locale).
        currencyFormat = (DecimalFormat) DecimalFormat.getCurrencyInstance();
        // This specifies the actual currency that the value is in, and provide
        // s the currency symbol.
        DecimalFormatSymbols decimalFormatSymbols;
        Currency currency;
        String symbol = null;
        if (Objects.equals(isoCurrencyCode, "BTC")) {
            decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
            symbol = CurrencyManager.bitcoinLowercase;
            decimalFormatSymbols.setCurrencySymbol(symbol);
        } else {
            try {
                currency = Currency.getInstance(isoCurrencyCode);
//                Log.e(TAG, "Currency.getInstance succeeded: " + currency.getSymbol());
            } catch (IllegalArgumentException e) {
//                Log.e(TAG, "Currency.getInstance did not succeed, going with the default", e);
                currency = Currency.getInstance(Locale.getDefault());
            }
            decimalFormatSymbols = currencyFormat.getDecimalFormatSymbols();
            symbol = currency.getSymbol();
            decimalFormatSymbols.setCurrencySymbol(symbol);
        }
        currencyFormat.setDecimalSeparatorAlwaysShown(CurrencyManager.separatorNeedsToBeShown);
        currencyFormat.setMaximumFractionDigits(2);
        currencyFormat.setMinimumFractionDigits(AmountAdapter.digitsInserted);
        currencyFormat.setGroupingUsed(true);
        currencyFormat.setDecimalFormatSymbols(decimalFormatSymbols);
        currencyFormat.setNegativePrefix(decimalFormatSymbols.getCurrencySymbol() + "-");
// or "-"+symbol if that's what you need
        currencyFormat.setNegativeSuffix("");
//        Log.e(TAG, "Returning the formatted string with separatorVisibility: " +
// currencyFormat.isDecimalSeparatorAlwaysShown());
        return currencyFormat.format(new BigDecimal(String.valueOf(amount)).divide(new BigDecimal("100")).doubleValue());
    }

    public static String getFormattedCurrencyStringForLocale(Locale locale, String isoCurrencyCode, double amount) {
        // This formats currency values as the user expects to read them (default locale).
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);

        // This specifies the actual currency that the value is in, and provides the currency symbol.
        Currency currency = Currency.getInstance(isoCurrencyCode);

        // Note we don't supply a locale to this method - uses default locale to format the currency symbol.
        String symbol = currency.getSymbol(locale);

        // We then tell our formatter to use this symbol.
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(symbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        return currencyFormat.format(amount);
    }

    public static String getFormattedCurrencyStringFixed(Locale locale, String isoCurrencyCode, double amount) {
        // This formats currency values as the user expects to read them in the supplied locale.
        NumberFormat currencyFormat = NumberFormat.getCurrencyInstance(locale);

        // This specifies the actual currency that the value is in, and provides
        // the currency symbol that is used
        Currency currency = Currency.getInstance(isoCurrencyCode);

        // Our fix is to use the US locale as default for the symbol, unless the currency is USD
        // and the locale is NOT the US, in which case we know it should be US$.
        String symbol;
        if (isoCurrencyCode.equalsIgnoreCase("usd") && !locale.equals(Locale.US)) {
            symbol = "US$";// currency.getSymbol(Locale.UK);
        } else {
            symbol = currency.getSymbol(Locale.US); // US locale has the best symbol formatting table.
        }

        // We tell our formatter to use this symbol
        DecimalFormatSymbols decimalFormatSymbols = ((java.text.DecimalFormat) currencyFormat).getDecimalFormatSymbols();
        decimalFormatSymbols.setCurrencySymbol(symbol);
        ((java.text.DecimalFormat) currencyFormat).setDecimalFormatSymbols(decimalFormatSymbols);

        return currencyFormat.format(amount);
    }
}
