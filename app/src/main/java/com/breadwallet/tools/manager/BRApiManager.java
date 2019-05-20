package com.breadwallet.tools.manager;

import android.arch.lifecycle.Lifecycle;
import android.content.Context;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.WorkerThread;
import android.text.format.DateUtils;
import android.util.Log;

import com.breadwallet.app.ApplicationLifecycleObserver;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.breadwallet.wallet.wallets.ethereum.WalletTokenManager;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public final class BRApiManager implements ApplicationLifecycleObserver.ApplicationLifecycleListener {
    private static final String TAG = BRApiManager.class.getName();
    private static final String BIT_PAY_URL = "https://bitpay.com/rates";
    private static final String DATE_FORMAT = "EEE, dd MMM yyyy HH:mm:ss z";
    private static final String CURRENCY_QUERY_STRING = "/rates?currency=";
    private static final String CURRENCIES_PATH = "/currencies";
    private static final String NAME = "name";
    private static final String CODE = "code";
    private static final String RATE = "rate";
    private static final String PRICE_BTC = "price_btc";
    private static final String SYMBOL = "symbol";
    private static final String TOKEN_RATES_URL_PREFIX = "https://min-api.cryptocompare.com/data/pricemulti?fsyms=";
    private static final String TOKEN_RATES_URL_SUFFIX = "&tsyms=BTC";
    private static final int FSYMS_CHAR_LIMIT = 300;
    private static final String CONTRACT_INITIAL_VALUE = "contract_initial_value";

    private static BRApiManager mInstance;
    private Timer mTimer;
    private TimerTask mTimerTask;
    private Handler mHandler;

    private BRApiManager() {
        mHandler = new Handler();
    }

    public static BRApiManager getInstance() {
        if (mInstance == null) {
            mInstance = new BRApiManager();
        }
        return mInstance;
    }

    @WorkerThread
    private void updateFiatRates(Context context) {
        if (UiUtils.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = fetchFiatRates(context);
            if (arr != null) {
                int length = arr.length();
                for (int i = 0; i < length; i++) {
                    CurrencyEntity currencyEntity = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        currencyEntity.name = tmpObj.getString(NAME);
                        currencyEntity.code = tmpObj.getString(CODE);
                        currencyEntity.rate = Float.valueOf(tmpObj.getString(RATE));
                        currencyEntity.iso = WalletBitcoinManager.BITCOIN_CURRENCY_CODE;
                    } catch (JSONException e) {
                        Log.e(TAG, "updateFiatRates: ", e);
                    }
                    set.add(currencyEntity);
                }
            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            Log.e(TAG, "updateFiatRates: ", e);
        }
        if (set.size() > 0) {
            RatesDataSource.getInstance(context).putCurrencies(context, set);
        }

    }

    private void initializeTimerTask(final Context context) {
        ApplicationLifecycleObserver.addApplicationLifecycleListener(this);
        mTimerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                mHandler.post(new Runnable() {
                    public void run() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                updateData(context);
                            }
                        });
                    }
                });
            }
        };
    }

    @WorkerThread
    private void updateData(final Context context) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                //Update Crypto Rates
                List<String> codeList = WalletsMaster.getInstance().getAllCurrencyCodesPossible(context);
                updateCryptoRates(context, codeList);
                //Update new tokens rate (e.g. CCC)
                fetchNewTokensData(context);
            }
        });
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                //Update BTC/Fiat rates
                updateFiatRates(context);
            }
        });
        List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance().getAllWallets(context));
        for (final BaseWalletManager walletManager : list) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    walletManager.updateFee(context);
                }
            });
        }

    }

    @WorkerThread
    private synchronized void updateCryptoRates(Context context, List<String> currencyCodeList) {
        List<String> currencyCodeListChunks = new ArrayList<>();
        StringBuilder chunkStringBuilder = new StringBuilder();
        for (String currencyCode : currencyCodeList) {
            //check if there's enough space before appending the param.
            //code length + 1 (comma)
            if (chunkStringBuilder.length() + currencyCode.length() + 1 > FSYMS_CHAR_LIMIT) {
                //One chunk is full, add it and create new builder.
                currencyCodeListChunks.add(chunkStringBuilder.toString());
                chunkStringBuilder = new StringBuilder();
            }
            chunkStringBuilder.append(currencyCode);
            chunkStringBuilder.append(',');
        }
        currencyCodeListChunks.add(chunkStringBuilder.toString());
        for (String currencyCodeChunk : currencyCodeListChunks) {
            fetchCryptoRates(context, currencyCodeChunk);
        }
    }

    /**
     * Gets the rates from cryptocompare for the provided codeList
     *
     * @param context       The Context
     * @param codeListChunk The comma separated code list.
     */
    private synchronized void fetchCryptoRates(Context context, String codeListChunk) {
        String url = TOKEN_RATES_URL_PREFIX + codeListChunk + TOKEN_RATES_URL_SUFFIX;
        String result = urlGET(context, url);
        try {
            if (Utils.isNullOrEmpty(result)) {
                Log.e(TAG, "fetchCryptoRates: Failed to fetch");
                return;
            }

            JSONObject ratesJsonObject = new JSONObject(result);
            if (ratesJsonObject.length() == 0) {
                Log.e(TAG, "fetchCryptoRates: empty json");
                return;
            }
            Iterator<String> keys = ratesJsonObject.keys();
            Set<CurrencyEntity> ratesList = new LinkedHashSet<>();
            while (keys.hasNext()) {
                String currencyCode = keys.next();
                JSONObject jsonObject = ratesJsonObject.getJSONObject(currencyCode);
                String code = WalletBitcoinManager.BITCOIN_CURRENCY_CODE;
                String rate = jsonObject.getString(code);
                CurrencyEntity currencyEntity = new CurrencyEntity(code, "", Float.valueOf(rate), currencyCode);
                ratesList.add(currencyEntity);
            }
            RatesDataSource.getInstance(context).putCurrencies(context, ratesList);
        } catch (JSONException e) {
            BRReportsManager.reportBug(e);
            Log.e(TAG, "fetchCryptoRates: ", e);
        }
    }

    public synchronized void startTimer(Context context) {
        //set a new Timer
        if (mTimer != null) {
            return;
        }
        mTimer = new Timer();
        Log.d(TAG, "startTimer: started...");
        //initialize the TimerTask's job
        initializeTimerTask(context);

        mTimer.schedule(mTimerTask, DateUtils.SECOND_IN_MILLIS, DateUtils.MINUTE_IN_MILLIS);
    }

    private synchronized void stopTimerTask() {
        //stop the timer, if it's not already null
        if (mTimer != null) {
            mTimer.cancel();
            mTimer = null;
        }
    }

    @WorkerThread
    private static JSONArray fetchFiatRates(Context app) {
        //Fetch the BTC-Fiat rates
        String url = APIClient.getBaseURL() + CURRENCY_QUERY_STRING + WalletBitcoinManager.BITCOIN_CURRENCY_CODE;
        String jsonString = urlGET(app, url);
        JSONArray jsonArray = null;
        if (jsonString == null) {
            Log.e(TAG, "fetchFiatRates: failed, response is null");
            return null;
        }
        try {
            JSONObject obj = new JSONObject(jsonString);
            jsonArray = obj.getJSONArray(BRConstants.BODY);
        } catch (JSONException ex) {
            Log.e(TAG, "fetchFiatRates: ", ex);
        }
        return jsonArray == null ? backupFetchRates(app) : jsonArray;
    }

    /**
     * Fetches data from /currencies api meant for new icos and tokens with no public rates yet.
     *
     * @param context Context
     */
    @WorkerThread
    private static void fetchNewTokensData(Context context) {
        String url = APIClient.getBaseURL() + CURRENCIES_PATH;
        String tokenDataJsonString = urlGET(context, url);
        if (tokenDataJsonString == null || tokenDataJsonString.length() == 0) {
            Log.e(TAG, "fetchFiatRates: failed, response is null");
            return;
        }
        try {
            List<CurrencyEntity> currencyEntities = new ArrayList<>();
            JSONArray tokenDataArray = new JSONArray(tokenDataJsonString);
            for (int i = 0; i < tokenDataArray.length(); i++) {
                JSONObject tokenDataJsonObject =  tokenDataArray.getJSONObject(i);
                if (tokenDataJsonObject.has(CONTRACT_INITIAL_VALUE)) {
                    String priceInEth = tokenDataJsonObject.getString(CONTRACT_INITIAL_VALUE)
                            .replace(WalletEthManager.ETH_CURRENCY_CODE, "").trim();
                    String name = tokenDataJsonObject.getString(BRConstants.NAME);
                    String code = tokenDataJsonObject.getString(BRConstants.CODE);
                    CurrencyEntity ethCurrencyEntity =
                            new CurrencyEntity(WalletEthManager.ETH_CURRENCY_CODE, name, Float.valueOf(priceInEth), code);
                    CurrencyEntity currencyEntity = convertEthRateToBtc(context, ethCurrencyEntity);
                    currencyEntities.add(currencyEntity);
                }
            }
            RatesDataSource.getInstance(context).putCurrencies(context, currencyEntities);
        } catch (JSONException ex) {
            Log.e(TAG, "fetchNewTokensData: ", ex);
        }
    }

    private static CurrencyEntity convertEthRateToBtc(Context context, CurrencyEntity currencyEntity) {
        if (currencyEntity == null) {
            return null;
        }
        CurrencyEntity ethBtcExchangeRate = RatesDataSource.getInstance(context)
                .getCurrencyByCode(context, WalletEthManager.ETH_CURRENCY_CODE, WalletBitcoinManager.BITCOIN_CURRENCY_CODE);
        if (ethBtcExchangeRate == null) {
            Log.e(TAG, "computeCccRates: ethBtcExchangeRate is null");
            return null;
        }
        float newRate = new BigDecimal(currencyEntity.rate).multiply(new BigDecimal(ethBtcExchangeRate.rate)).floatValue();
        return new CurrencyEntity(WalletBitcoinManager.BITCOIN_CURRENCY_CODE, currencyEntity.name, newRate, currencyEntity.iso);
    }


    /**
     * uses https://bitpay.com/rates to fetch the rates as a backup in case our api is down.
     * @param context
     * @return JSONArray with rates data.
     */
    @WorkerThread
    private static JSONArray backupFetchRates(Context context) {
        String ratesJsonString = urlGET(context, BIT_PAY_URL);
        JSONArray ratesJsonArray = null;
        if (ratesJsonString != null) {
            try {
                JSONObject ratesJsonObject = new JSONObject(ratesJsonString);
                ratesJsonArray = ratesJsonObject.getJSONArray(BRConstants.DATA);
            } catch (JSONException e) {
                Log.e(TAG, "backupFetchRates: ", e);
            }
            return ratesJsonArray;
        }
        return null;
    }

    @WorkerThread
    public static String urlGET(Context app, String myURL) {
        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)
                .header(BRConstants.HEADER_ACCEPT, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)
                .get();

        Request request = builder.build();
        String bodyText = null;
        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, false);

        try {
            bodyText = resp.getBodyText();
            String strDate = resp.getHeaders().get(BRConstants.DATE);
            if (strDate == null) {
                Log.e(TAG, "urlGET: strDate is null!");
                return bodyText;
            }
            SimpleDateFormat formatter = new SimpleDateFormat(DATE_FORMAT, Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException e) {
            Log.e(TAG, "urlGET: ", e);
        }
        return bodyText;
    }

    @Override
    public void onLifeCycle(Lifecycle.Event event) {
        switch (event) {
            case ON_STOP:
                stopTimerTask();
                ApplicationLifecycleObserver.removeApplicationLifecycleListener(this);
                break;
        }

    }
}
