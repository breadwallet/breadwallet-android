/**
 * BreadWallet
 * <p/>
 * Created by Ahsan Butt on <ahsan.butt@breadwallet.com> 2/12/19.
 * Copyright (c) 2019 breadwallet LLC
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

package com.breadwallet.model;

import java.math.BigDecimal;

/**
 * Wallet is a simple container class for the data associated with a given wallet, including
 * its sync status.
 */
public class Wallet {
    private String mName;
    private String mCurrencyCode;
    private BigDecimal mExchangeRate = BigDecimal.ZERO;
    private BigDecimal mFiatBalance = BigDecimal.ZERO;
    private BigDecimal mCryptoBalance = BigDecimal.ZERO;
    private boolean mIsSyncing;
    private double mSyncProgress;

    /**
     * Constructor takes both name and currency code, which both identify a wallet.
     *
     * @param name         The name of wallet (e.g., "Ethereum" etc.)
     * @param currencyCode The currency code of the wallet (e.g., "ETH" etc.)
     */
    public Wallet(String name, String currencyCode) {
        mName = name;
        mCurrencyCode = currencyCode;
    }

    /**
     * Sets the fiat exchange rate.
     *
     * @param exchangeRate The value to which the exchange rate should be set.
     */
    public void setExchangeRate(BigDecimal exchangeRate) {
        mExchangeRate = exchangeRate;
    }

    /**
     * Sets the fiat balance (e.g., the balance in USD).
     *
     * @param fiatBalance The value to which the fiat balance should be set.
     */
    public void setFiatBalance(BigDecimal fiatBalance) {
        mFiatBalance = fiatBalance;
    }

    /**
     * Sets the crypto balance (i.e., the balance in its crypto currency).
     *
     * @param cryptoBalance The value to which the crypto balance should be set.
     */
    public void setCryptoBalance(BigDecimal cryptoBalance) {
        mCryptoBalance = cryptoBalance;
    }

    /**
     * Sets whether the wallet is sync'ing.
     *
     * @param isSyncing The value indicating whether the wallet is sync'ing.
     */
    public void setIsSyncing(boolean isSyncing) {
        mIsSyncing = isSyncing;
    }

    /**
     * Sets the progress of the sync process.
     *
     * @param syncProgress The progress between 0 and 1, indicating the percentage complete.
     */
    public void setSyncProgress(double syncProgress) {
        mSyncProgress = syncProgress;
    }

    /**
     * Returns the wallet's name (e.g., "Ethereum").
     *
     * @return The wallet name.
     */
    public String getName() {
        return mName;
    }

    /**
     * Returns the currency code (e.g., "ETC").
     *
     * @return The currency code.
     */
    public String getCurrencyCode() {
        return mCurrencyCode;
    }

    /**
     * Returns the exchange rate.
     *
     * @return The exchange rate.
     */
    public BigDecimal getExchangeRate() {
        return mExchangeRate;
    }

    /**
     * Returns the fiat balance (e.g., the balance in USD)
     *
     * @return The fiat balance.
     */
    public BigDecimal getFiatBalance() {
        return mFiatBalance;
    }

    /**
     * Returns the crypto balance (i.e., the balance in its crypto currency)
     *
     * @return The crypto balance.
     */
    public BigDecimal getCryptoBalance() {
        return mCryptoBalance;
    }

    /**
     * Returns whether the wallet is currently sync'ing.
     *
     * @return The value indicating whether the wallet is sync'ing.
     */
    public boolean isSyncing() {
        return mIsSyncing;
    }

    /**
     * Returns the progress of the wallet's sync'ing.
     *
     * @return The progress between 0 and 1, indicating the percentage complete.
     */
    public double getSyncProgress() {
        return mSyncProgress;
    }
}
