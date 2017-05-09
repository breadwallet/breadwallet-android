package com.breadwallet.tools.threads;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.wallet.BRWalletManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.math.BigDecimal;
import java.net.URL;
import java.net.URLConnection;
import java.nio.charset.Charset;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/2/16.
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

public class ImportPrivKeyTask extends AsyncTask<String, String, String> {
    public static final String TAG = ImportPrivKeyTask.class.getName();
    public static final String UNSPENT_URL = "https://api.breadwallet.com/q/addr/";
    private Activity app;
    private String key;
    private ImportPrivKeyEntity importPrivKeyEntity;

    public ImportPrivKeyTask(Activity activity) {
        app = activity;
    }

    @Override
    protected String doInBackground(String... params) {
        if (params.length == 0) return null;
        key = params[0];
        if (key == null || key.isEmpty() || app == null) return null;
        String tmpAddrs = BRWalletManager.getInstance(app).getAddressFromPrivKey(key);
        String url = UNSPENT_URL + tmpAddrs + "/utxo";
        importPrivKeyEntity = createTx(app, url);
        if (importPrivKeyEntity == null) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    ((BreadWalletApp) app.getApplication()).showCustomDialog(app.getString(R.string.warning),
                            app.getString(R.string.priv_key_empty), app.getString(R.string.ok));
                }
            });
        }
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        if (importPrivKeyEntity == null) {
            return;
        }
        String sentBits = BRStringFormatter.getFormattedCurrencyString("BTC", importPrivKeyEntity.getAmount());
        String sentExchange = BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(app),
                SharedPreferencesManager.getIso(app), new BigDecimal(importPrivKeyEntity.getAmount()), app);
        String feeBits = BRStringFormatter.getFormattedCurrencyString("BTC", importPrivKeyEntity.getFee());
        String feeExchange = BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(app),
                SharedPreferencesManager.getIso(app), new BigDecimal(importPrivKeyEntity.getFee()), app);
        if (app == null || importPrivKeyEntity == null) return;
        String message = String.format(app.getString(R.string.send_money_from_privkey_message), sentBits, sentExchange, feeBits, feeExchange);
        new AlertDialog.Builder(app)
                .setTitle("")
                .setMessage(message)
                .setPositiveButton(String.format("%s (%s)", sentBits, sentExchange), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                boolean result = BRWalletManager.getInstance(app).confirmKeySweep(importPrivKeyEntity.getTx(), key);
                                if (!result) {
                                    ((BreadWalletApp) app.getApplication()).showCustomDialog(app.getString(R.string.warning),
                                            app.getString(R.string.could_not_sweep_the_balance), app.getString(R.string.ok));
                                }
                            }
                        }).start();

                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        super.onPostExecute(s);
    }

    public static ImportPrivKeyEntity createTx(Activity activity, String url) {
        if (url == null || url.isEmpty()) return null;
        String jsonString = callURL(url);
        if (jsonString == null || jsonString.isEmpty()) return null;
        ImportPrivKeyEntity result = null;
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(jsonString);
            int length = jsonArray.length();
            if (length > 0)
                BRWalletManager.getInstance(activity).createInputArray();

            for (int i = 0; i < length; i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String txid = obj.getString("txid");
                int vout = obj.getInt("vout");
                String scriptPubKey = obj.getString("scriptPubKey");
                long amount = obj.getLong("amount");
                byte[] txidBytes = hexStringToByteArray(txid);
                byte[] scriptPubKeyBytes = hexStringToByteArray(scriptPubKey);
                BRWalletManager.getInstance(activity).addInputToPrivKeyTx(txidBytes, vout, scriptPubKeyBytes, amount);
            }

            result = BRWalletManager.getInstance(activity).getPrivKeyObject();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len / 2];
        for (int i = 0; i < len; i += 2) {
            data[i / 2] = (byte) ((Character.digit(s.charAt(i), 16) << 4)
                    + Character.digit(s.charAt(i + 1), 16));
        }
        return data;
    }

    private static String callURL(String myURL) {
//        System.out.println("Requested URL_EA:" + myURL);
        StringBuilder sb = new StringBuilder();
        URLConnection urlConn = null;
        InputStreamReader in = null;
        try {
            URL url = new URL(myURL);
            urlConn = url.openConnection();
            if (urlConn != null)
                urlConn.setReadTimeout(60 * 1000);
            if (urlConn != null && urlConn.getInputStream() != null) {
                in = new InputStreamReader(urlConn.getInputStream(),
                        Charset.defaultCharset());
                BufferedReader bufferedReader = new BufferedReader(in);

                int cp;
                while ((cp = bufferedReader.read()) != -1) {
                    sb.append((char) cp);
                }
                bufferedReader.close();
            }
            assert in != null;
            in.close();
        } catch (Exception e) {
            return null;
        }

        return sb.toString();
    }
}
