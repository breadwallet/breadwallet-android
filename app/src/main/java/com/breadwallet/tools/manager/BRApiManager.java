package com.breadwallet.tools.manager;

import android.content.Context;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.request.Outputs;
import com.breadwallet.wallet.wallets.ela.response.transaction.TransactionRes;
import com.breadwallet.wallet.wallets.ela.response.transaction.UTXOInputs;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.Timer;
import java.util.TimerTask;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
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

    public static final String HEADER_WALLET_ID = "X-Wallet-Id";
    public static final String HEADER_IS_INTERNAL = "X-Is-Internal";
    public static final String HEADER_TESTFLIGHT = "X-Testflight";
    public static final String HEADER_TESTNET = "X-Bitcoin-Testnet";
    public static final String HEADER_ACCEPT_LANGUAGE = "Accept-Language";

    private BRApiManager() {
        handler = new Handler();
    }

    public static BRApiManager getInstance() {

        if (instance == null) {
            instance = new BRApiManager();
        }
        return instance;
    }

    @WorkerThread
    private void updateRates(Context context, BaseWalletManager walletManager) {
        if (UiUtils.isMainThread()) {
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
                        tmp.rate = Float.valueOf(tmpObj.getString("rate"));
                        tmp.iso = walletManager.getIso();
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
        if (set.size() > 0) RatesDataSource.getInstance(context).putCurrencies(context, set);

    }


    private void initializeTimerTask(final Context context) {
        timerTask = new TimerTask() {
            public void run() {
                //use a handler to run a toast that shows the current timestamp
                handler.post(new Runnable() {
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

        if (BreadApp.isAppInBackground(context)) {
            Log.e(TAG, "doInBackground: Stopping timer, no activity on.");
            stopTimerTask();
            return;
        }
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                updateErc20Rates(context);
            }
        });

        List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(context).getAllWallets(context));

        for (final BaseWalletManager w : list) {
            //only update stuff for non erc20 for now, API endpoint BUG
            if (w.getIso().equalsIgnoreCase("BTC") || w.getIso().equalsIgnoreCase("BCH")
                    || w.getIso().equalsIgnoreCase("ETH")) {
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
                        updateRates(context, w);

                    }
                });
            }
        }

    }

    @WorkerThread
    private synchronized void updateErc20Rates(Context context) {
        //get all erc20 rates.
        String url = "https://api.coinmarketcap.com/v1/ticker/?limit=1000&convert=BTC";
        String result = urlGET(context, url);
        try {
            if (Utils.isNullOrEmpty(result)) {
                Log.e(TAG, "updateErc20Rates: Failed to fetch");
                return;
            }
            JSONArray arr = new JSONArray(result);
            if (arr.length() == 0) {
                Log.e(TAG, "updateErc20Rates: empty json");
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
                String rate = json.getString("price_btc");
                String iso = json.getString("symbol");

                CurrencyEntity ent = new CurrencyEntity(code, name, Float.valueOf(rate), iso);
                tmp.add(ent);

            }
            RatesDataSource.getInstance(context).putCurrencies(context, tmp);
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

    @WorkerThread
    public static JSONArray fetchRates(Context app, BaseWalletManager walletManager) {
        String url = "https://" + BreadApp.HOST + "/rates?currency=" + walletManager.getIso();
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

    @WorkerThread
    public static JSONArray backupFetchRates(Context app, BaseWalletManager walletManager) {
        if (!walletManager.getIso().equalsIgnoreCase(WalletBitcoinManager.getInstance(app).getIso())) {
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

    @WorkerThread
    public static String getElaBalance(Context app, String address){
//        String address = WalletElaManager.getInstance(app).getAddress();
        String url = "http://fun-api-test.elastos.org/"+"/api/1/balance/"+address;
        String result = urlGET(app, url);
        String balance = null;
        try {
            JSONObject object = new JSONObject(result);
            balance = object.getString("result");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return balance;
    }


    public static String createElaTx(final String inputAddress, final String outputsAddress, final int amount, String memo){
        String transactionJson = null;
        try {
            String url = "http://fun-api-test.elastos.org/"+"/api/1/createTx";

            CreateTx tx = new CreateTx();
            tx.inputs.add(inputAddress);

            Outputs outputs = new Outputs();
            outputs.addr = outputsAddress;
            outputs.amt = amount;

            tx.outputs.add(outputs);

            String json = new Gson().toJson(tx);
//                    String result = urlPost(url, json);

            String result = "{\n" +
                    "\t\"result\": {\n" +
                    "\t\t\"Transactions\": [{\n" +
                    "\t\t\t\"UTXOInputs\": [{\n" +
                    "\t\t\t\t\"address\": \"EbunxcqXie6UExs5SXDbFZxr788iGGvAs9\",\n" +
                    "\t\t\t\t\"txid\": \"1d88dad1c8bfc2d58b37d3d022e38f18d2a80148468e448ece3a3c230ced9bcc\",\n" +
                    "\t\t\t\t\"index\": 0\n" +
                    "\t\t\t}],\n" +
                    "\t\t\t\"Fee\": 100.0,\n" +
                    "\t\t\t\"Outputs\": [{\n" +
                    "\t\t\t\t\"amount\": 10000,\n" +
                    "\t\t\t\t\"address\": \"ETyWPmg5aNais2iNrt2zGVjxJ3EVbuzVYo\"\n" +
                    "\t\t\t}, {\n" +
                    "\t\t\t\t\"amount\": 99989900,\n" +
                    "\t\t\t\t\"address\": \"EbunxcqXie6UExs5SXDbFZxr788iGGvAs9\"\n" +
                    "\t\t\t}]\n" +
                    "\t\t}]\n" +
                    "\t},\n" +
                    "\t\"status\": 200\n" +
                    "}";
            JSONObject jsonObject = new JSONObject(result);
            String tranactions = jsonObject.getString("result");
            TransactionRes res = new Gson().fromJson(tranactions, TransactionRes.class);
            List<UTXOInputs> inputs = res.Transactions.get(0).UTXOInputs;
            for(int i=0; i<inputs.size(); i++){
                UTXOInputs utxoInputs = inputs.get(i);
                utxoInputs.privateKey  = "FABB669B7D2FF2BEBBED1C3F1C9A9519C48993D1FC9D89DCB4C7CA14BDB8C99F";
            }

            transactionJson =new Gson().toJson(res);

            sendElaRawTx(transactionJson);

            Log.i("xidaokun", transactionJson);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return transactionJson;
    }


    public static void sendElaRawTx(final String transaction){
        try {
            String url = "http://fun-api-test.elastos.org/"+"/api/1/sendRawTx";
            String rawTransaction = Utility.generateRawTransaction(transaction);
            String json = "{"+"\"data\"" + ":" + "\"" + rawTransaction + "\"" +"}";
            String result = urlPost(url, json);

            Log.i("xidaokun", "result:"+result);
        } catch (IOException e) {
            e.printStackTrace();
        }

    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public static String urlPost(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = APIClient.elaClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    @WorkerThread
    public static String urlGET(Context app, String myURL) {
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
        String bodyText = null;
        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, false);

        try {
            bodyText = resp.getBodyText();
            String strDate = resp.getHeaders().get("date");
            if (strDate == null) {
                Log.e(TAG, "urlGET: strDate is null!");
                return bodyText;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(app, timeStamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return bodyText;
    }

}
