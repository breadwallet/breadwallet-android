package com.breadwallet.legacy.wallet.wallets.bitcoin;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BuildConfig;
import com.breadwallet.legacy.presenter.entities.CurrencyEntity;
import com.breadwallet.model.FeeOption;
import com.breadwallet.repository.FeeRepository;
import com.breadwallet.repository.RatesRepository;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;

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
public final class WalletBchManager extends BaseBitcoinWalletManager {

    private static final String TAG = WalletBchManager.class.getName();
    private static final String CURRENCY_CODE = BITCASH_CURRENCY_CODE;
    public static final String NAME = "Bitcoin Cash";
    private static final String SCHEME = BuildConfig.BITCOIN_TESTNET ? "bchtest" : "bitcoincash";
    private static final String COLOR = "#478559";
    private static final long MAINNET_FORK_TIME = 1501568580; // Tuesday, August 1, 2017 6:23:00 AM GMT in seconds since Epoch
    private static final long TESTNET_FORK_TIME = 1501597117; // Tuesday, August 1, 2017 2:18:37 PM GMT in seconds since Epoch

    private static WalletBchManager mInstance;

    public static synchronized WalletBchManager getInstance(Context context) {
        if (mInstance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(context);
            if (Utils.isNullOrEmpty(rawPubKey)) {
                Log.e(TAG, "getInstance: rawPubKey is null");
                return null;
            }
        }
        return mInstance;
    }

    private WalletBchManager(final Context context, double earliestPeerTime) {
        super(context, earliestPeerTime);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                String currencyCode = getCurrencyCode();
                FeeOption preferredFeeOption = FeeRepository.getInstance(context).getPreferredFeeOptionByCurrency(currencyCode);
                BigDecimal preferredFee = FeeRepository.getInstance(context).getFeeByCurrency(currencyCode, preferredFeeOption);

            }
        });
        // setSettingsConfig(new WalletSettingsConfiguration(context, getCurrencyCode(), SettingsUtil.getBitcoinCashSettings(context), getFingerprintLimits(context)));
//          BRPeerManager.getInstance().updateFixedPeer(ctx);//todo reimplement the fixed peer
    }

    protected String getTag() {
        return TAG;
    }

    @Override
    protected String getColor() {
        return COLOR;
    }

    protected List<BigDecimal> getFingerprintLimits(Context app) {
        List<BigDecimal> result = new ArrayList<>();
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS).divide(new BigDecimal(100), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS).divide(new BigDecimal(10), getMaxDecimalPlaces(app), BRConstants.ROUNDING_MODE));
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS));
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS).multiply(new BigDecimal(10)));
        result.add(new BigDecimal(ONE_BITCOIN_IN_SATOSHIS).multiply(new BigDecimal(100)));
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
        return null;

    }

    @Override
    public void refreshAddress(Context context) {
    }

    @Override
    public String undecorateAddress(String addr) {
        return null;
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context context, BigDecimal amount, CurrencyEntity currencyEntity) {
        if (amount == null || amount.equals(BigDecimal.ZERO)) {
            return amount;
        }
        if (currencyEntity != null) {
            //passed in a custom CurrencyEntity
            //get crypto amount
            //multiply by fiat rate
            return getCryptoForSmallestCrypto(context, amount).multiply(new BigDecimal(currencyEntity.rate));
        }
        String currencyCode = BRSharedPrefs.getPreferredFiatIso(context);
        BigDecimal cryptoAmount = getCryptoForSmallestCrypto(context, amount);
        BigDecimal fiatData = RatesRepository.getInstance(context).getFiatForCrypto(cryptoAmount, getCurrencyCode(), currencyCode);
        if (fiatData == null) {
            return null;
        }
        return fiatData;
    }

    protected void syncStopped(Context context) {
        BRSharedPrefs.putBchPreForkSynced(context, true);
    }
}
