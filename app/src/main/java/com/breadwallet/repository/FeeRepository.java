/**
 * BreadWallet
 * <p/>
 * Created by Ahsan Butt on <ahsan.butt@breadwallet.com> 5/22/19.
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
package com.breadwallet.repository;

import android.content.Context;

import com.breadwallet.model.FeeOption;
import com.breadwallet.tools.manager.BRSharedPrefs;

import java.math.BigDecimal;

/**
 * Repository for Fee data, such as a given currency's priority fee rate per kb. Contains methods for reading and writing fee data,
 * and abstracts away the underlying data source.
 */
public class FeeRepository {
    private static final String TAG = FeeRepository.class.getName();
    private static FeeRepository mInstance;

    private Context mContext;

    synchronized public static FeeRepository getInstance(Context context) {
        if (mInstance == null) {
            mInstance = new FeeRepository(context);
        }

        return mInstance;
    }

    private FeeRepository(Context context) {
        mContext = context;
    }

    /**
     * Retrieves the fee for a given currency and fee option.
     * @param currencyCode the currency for which the fee is returned
     * @param feeOption the fee option (e.g., Priority, Regular, Economy)
     * @return the fee
     */
    public BigDecimal getFeeByCurrency(String currencyCode, FeeOption feeOption) {
        return BRSharedPrefs.getFeeRate(mContext, currencyCode, feeOption);
    }

    /**
     * Persists the fee for a given currency and fee option, and records the given timestamp.
     * @param currencyCode the currency for which the fee is being persisted
     * @param fee the fee
     * @param feeOption the fee option (e.g., Priority, Regular, Economy)
     * @param timestampInMillis the timestamp (in millis) for this fee
     */
    public void putFeeForCurrency(String currencyCode, BigDecimal fee, FeeOption feeOption, long timestampInMillis) {
        BRSharedPrefs.putFeeRate(mContext, currencyCode, fee, feeOption);
        BRSharedPrefs.putFeeTime(mContext, currencyCode, timestampInMillis);
    }

    /**
     * Retrieves the preferred fee option for a given currency.
     * @param currencyCode the currency for which the preferred fee option is being retrieved
     * @return the fee option preferred for this currency
     */
    public FeeOption getPreferredFeeOptionByCurrency(String currencyCode) {
        return FeeOption.valueOf(BRSharedPrefs.getPreferredFeeOption(mContext, currencyCode));
    }

    /**
     * Persists the preferred fee option for a given currency.
     * @param currencyCode the currency for which the preferred fee option is being retrieved
     * @param feeOption the preferred fee option
     */
    public void putPreferredFeeOptionForCurrency(String currencyCode, FeeOption feeOption) {
        BRSharedPrefs.putPreferredFeeOption(mContext, currencyCode, feeOption);
    }
}
