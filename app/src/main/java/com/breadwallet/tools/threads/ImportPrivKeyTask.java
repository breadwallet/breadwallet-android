package com.breadwallet.tools.threads;

import android.app.Activity;
import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.tools.CurrencyManager;
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
 * Created by Mihail Gutan on 6/2/16.
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
        if (tmpAddrs.equals("")) {
            ((BreadWalletApp)app.getApplication()).showCustomDialog(app.getString(R.string.warning),
                    "this private key is already in your wallet", app.getString(R.string.ok));
            importPrivKeyEntity = null;
        }
        String url = UNSPENT_URL + tmpAddrs + "/utxo";
        importPrivKeyEntity = createTx(app, url);
        return null;
    }

    @Override
    protected void onPostExecute(String s) {
        if (app == null || importPrivKeyEntity == null) return;
        CurrencyManager cm = CurrencyManager.getInstance(app);
        String amountString = String.format("%s(%s)", cm.getFormattedCurrencyString("BTC",
                importPrivKeyEntity.getAmount()), cm.getExchangeForAmount(cm.getRateFromPrefs(), cm.getISOFromPrefs(), new BigDecimal(importPrivKeyEntity.getAmount())));
        String feeString = String.format("%s(%s)", cm.getFormattedCurrencyString("BTC",
                importPrivKeyEntity.getFee()), cm.getExchangeForAmount(cm.getRateFromPrefs(), cm.getISOFromPrefs(), new BigDecimal(importPrivKeyEntity.getFee())));
        String message = String.format("Send %s from this private key into your wallet? The bitcoin network will receive a fee of %s.", amountString, feeString);
        new android.app.AlertDialog.Builder(app)
                .setTitle("")
                .setMessage(message)
                .setPositiveButton(app.getString(R.string.send), new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        BRWalletManager.getInstance(app).confirmKeySweep(importPrivKeyEntity.getTx(), key);
                    }
                })
                .setIcon(android.R.drawable.ic_dialog_alert)
                .show();
        super.onPostExecute(s);
    }

    public static ImportPrivKeyEntity createTx(Activity activity, String url) {
        String jsonString = callURL(url);
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
                BRWalletManager.getInstance(activity).addInputToPrivKeyTx(txid.getBytes(), vout, scriptPubKey.getBytes(), amount);
            }

            result = BRWalletManager.getInstance(activity).getPrivKeyObject();

        } catch (JSONException e) {
            e.printStackTrace();
        }
        return result;
    }

    private static String callURL(String myURL) {
//        System.out.println("Requested URL:" + myURL);
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
