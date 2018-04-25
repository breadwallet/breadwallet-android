package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.Nullable;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Currency;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.Request;
import okhttp3.Response;

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

public class BRApiManager {
    private static final String TAG = BRApiManager.class.getName();

    private static BRApiManager instance;
    private Timer timer;

    private TimerTask timerTask;

    private Handler handler;

    private BRApiManager() {
        handler = new Handler();
    }

    public static BRApiManager getInstance() {

        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    private Set<CurrencyEntity> getCurrencies(Activity context, BaseWalletManager walletManager) {
        if (ActivityUTILS.isMainThread()) {
            throw new NetworkOnMainThreadException();
        }
        Set<CurrencyEntity> set = new LinkedHashSet<>();
        try {
            JSONArray arr = fetchRates(context, walletManager);
            if (arr != null) {
                int length = arr.length();
                for (int i = 0; i < length; i++) {
                    CurrencyEntity tmp = new CurrencyEntity();
                    try {
                        JSONObject tmpObj = (JSONObject) arr.get(i);
                        tmp.name = tmpObj.getString("name");
                        tmp.code = tmpObj.getString("code");
                        tmp.rate = (float) tmpObj.getDouble("rate");
                        tmp.iso = walletManager.getIso(context);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }
                    set.add(tmp);
                }

            } else {
                Log.e(TAG, "getCurrencies: failed to get currencies, response string: " + arr);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return new LinkedHashSet<>(set);
    }


    private void initializeTimerTask(final Context context) {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
                    public void run() {
                        updateData(context);
                    }
                });
            }
        };
    }

    private void updateData(final Context context) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (BreadApp.isAppInBackground(context)) {
                    Log.e(TAG, "doInBackground: Stopping timer, no activity on.");
                    stopTimerTask();
                    return;
                }
                List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(context).getAllWallets(context));

                for (final BaseWalletManager w : list) {
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            w.updateFee(context);
                        }
                    });
                    BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            //get each wallet's rates
                            Set<CurrencyEntity> tmp = getCurrencies((Activity) context, w);
                            CurrencyDataSource.getInstance(context).putCurrencies(context, tmp);
                        }
                    });
                }
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        updateErc20Rates(context);
                    }
                });
            }
        });
    }

    private void updateErc20Rates(Context context) {
        //get all erc20 rates.
        String url = "https://api.coinmarketcap.com/v1/ticker/?limit=1000&convert=BTC";
        String result = urlGET(context, url);
        try {
            JSONArray arr = new JSONArray(result);
            if (arr.length() == 0) {
                BRReportsManager.reportBug(new NullPointerException("Failed to retrieve erc20 rates: " + url));
                return;
            }
            String object = null;
            Set<CurrencyEntity> tmp = new LinkedHashSet<>();
            for (int i = 0; i < arr.length(); i++) {

                Object obj = arr.get(i);
                if (!(obj instanceof JSONObject)) {
                    object = obj.getClass().getSimpleName();
                    continue;
                }
                JSONObject json = (JSONObject) obj;
                String code = "BTC";
                String name = json.getString("name");
                double rate = json.getDouble("price_btc");
                String iso = json.getString("symbol");

                CurrencyEntity ent = new CurrencyEntity(code, name, (float) rate, iso);
                tmp.add(ent);

            }
            CurrencyDataSource.getInstance(context).putCurrencies(context, tmp);
            if (object != null)
                BRReportsManager.reportBug(new IllegalArgumentException("JSONArray returns a wrong object: " + object));
        } catch (JSONException e) {
            BRReportsManager.reportBug(e);
            e.printStackTrace();
        }

    }

    public void startTimer(Context context) {
        //set a new Timer
        if (timer != null) return;
        timer = new Timer();
        Log.e(TAG, "startTimer: started...");
        //initialize the TimerTask's job
        initializeTimerTask(context);

        timer.schedule(timerTask, 1000, 60000);
    }

    public void stopTimerTask() {
        //stop the timer, if it's not already null
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    public static JSONArray fetchRates(Activity app, BaseWalletManager walletManager) {
        String url = "https://" + BreadApp.HOST + "/rates?currency=" + walletManager.getIso(app);
        String jsonString = urlGET(app, url);
        JSONArray jsonArray = null;
        if (jsonString == null) {
            Log.e(TAG, "fetchRates: failed, response is null");
            return null;
        }
        try {
            JSONObject obj = new JSONObject(jsonString);
            jsonArray = obj.getJSONArray("body");

        } catch (JSONException ignored) {
        }
        return jsonArray == null ? backupFetchRates(app, walletManager) : jsonArray;
    }

    public static JSONArray backupFetchRates(Activity app, BaseWalletManager walletManager) {
        if (!walletManager.getIso(app).equalsIgnoreCase(WalletBitcoinManager.getInstance(app).getIso(app))) {
            //todo add backup for BCH
            return null;
        }
        String jsonString = urlGET(app, "https://bitpay.com/rates");

        JSONArray jsonArray = null;
        if (jsonString == null) return null;
        try {
            JSONObject obj = new JSONObject(jsonString);

            jsonArray = obj.getJSONArray("data");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return jsonArray;
    }

    public static String urlGET(Context app, String myURL) {
//        System.out.println("Requested URL_EA:" + myURL);
        if (ActivityUTILS.isMainThread()) {
            Log.e(TAG, "urlGET: network on main thread");
            throw new RuntimeException("network on main thread");
        }
        Map<String, String> headers = BreadApp.getBreadHeaders();

        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .get();
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        Request request = builder.build();
        String response = null;
        Response resp = APIClient.getInstance(app).sendRequest(request, false, 0);

        try {
            if (resp == null) {
                Log.e(TAG, "urlGET: " + myURL + ", resp is null");
                return null;
            }
            response = resp.body().string();
            String strDate = resp.header("date");
            if (strDate == null) {
                Log.e(TAG, "urlGET: strDate is null!");
                return response;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException | IOException e) {
            e.printStackTrace();
        } finally {
            if (resp != null) resp.close();

        }
        return response;
    }

}
