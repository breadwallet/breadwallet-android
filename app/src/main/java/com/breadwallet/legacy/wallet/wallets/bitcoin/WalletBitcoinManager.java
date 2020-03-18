package com.breadwallet.legacy.wallet.wallets.bitcoin;

import android.content.Context;
import com.breadwallet.model.FeeOption;
import com.breadwallet.repository.FeeRepository;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

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
public final class WalletBitcoinManager extends BaseBitcoinWalletManager {

    private static final String TAG = WalletBitcoinManager.class.getName();

    private static final String CURRENCY_CODE = BITCOIN_CURRENCY_CODE;
    public static final String NAME = "Bitcoin";
    private static final String SCHEME = "bitcoin";
    private static final String COLOR = "#f29500";

    private static WalletBitcoinManager mInstance;

    public static synchronized WalletBitcoinManager getInstance(Context context) {
        return mInstance;
    }

    private WalletBitcoinManager(final Context context,
                                 double earliestPeerTime) {
        super(context, earliestPeerTime);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                String currencyCode = getCurrencyCode();
                FeeOption preferredFeeOption = FeeRepository.getInstance(context).getPreferredFeeOptionByCurrency(currencyCode);
                BigDecimal preferredFee = FeeRepository.getInstance(context).getFeeByCurrency(currencyCode, preferredFeeOption);

            }
        });
        updateSettings(context);
    }

    public void updateSettings(Context context) {
//        setSettingsConfig(new WalletSettingsConfiguration(context, getCurrencyCode(), SettingsUtil.getBitcoinSettings(context), getFingerprintLimits(context)));
    }

    @Override
    protected String getTag() {
        return TAG;
    }

    @Override
    protected String getColor() {
        return COLOR;
    }

    protected List<BigDecimal> getFingerprintLimits(Context app) {
        List<BigDecimal> result = new ArrayList<>();
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS).divide(new BigDecimal(1000), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS).divide(new BigDecimal(100), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS).divide(new BigDecimal(10), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS));
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS).multiply(new BigDecimal(10)));
        return result;
    }

    @Override
    public String getCurrencyCode() {
        return CURRENCY_CODE;
    }

    @Override
    public String getScheme() {
        return SCHEME;
    }

    @Override
    public String getName() {
        return NAME;
    }

    @Override
    public String decorateAddress(String addr) {
        return addr; // no need to decorate
    }

    @Override
    public String undecorateAddress(String addr) {
        return addr; //no need to undecorate
    }

    protected void syncStopped(Context context) {
    }

    @Override
    public void refreshAddress(Context context) {
    }
}
