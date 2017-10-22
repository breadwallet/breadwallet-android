package com.breadwallet.tools.threads;

import android.app.Activity;
import android.os.AsyncTask;

import com.breadwallet.BreadApp;
import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
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
    public static String UNSPENT_URL;
    private Activity app;
    private String key;
    private ImportPrivKeyEntity importPrivKeyEntity;

    public ImportPrivKeyTask(Activity activity) {
        app = activity;
        UNSPENT_URL = BuildConfig.BITCOIN_TESTNET ? "https://testnet.litecore.io/api/addrs/" : "https://insight.litecore.io/api/addrs/";
    }

    @Override
    protected String doInBackground(String... params) {
        if (params.length == 0) return null;
        key = params[0];
        if (key == null || key.isEmpty() || app == null) return null;
        String tmpAddrs = BRWalletManager.getInstance().getAddressFromPrivKey(key);
        String url = UNSPENT_URL + tmpAddrs + "/utxo";
        importPrivKeyEntity = createTx(url);
        if (importPrivKeyEntity == null) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                            app.getString(R.string.Import_Error_empty), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);
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

        String iso = BRSharedPrefs.getIso(app);
        String sentBits = BRCurrency.getFormattedCurrencyString(app, "LTC", BRExchange.getAmountFromSatoshis(app, "LTC", new BigDecimal(importPrivKeyEntity.getAmount())));

        String sentExchange = BRCurrency.getFormattedCurrencyString(app, iso, BRExchange.getAmountFromSatoshis(app, iso, new BigDecimal(importPrivKeyEntity.getAmount())));
        String feeBits = BRCurrency.getFormattedCurrencyString(app, "LTC", BRExchange.getAmountFromSatoshis(app, "LTC", new BigDecimal(importPrivKeyEntity.getFee())));
        String feeExchange = BRCurrency.getFormattedCurrencyString(app, iso, BRExchange.getAmountFromSatoshis(app, iso, new BigDecimal(importPrivKeyEntity.getFee())));

        if (app == null || importPrivKeyEntity == null) return;
        String message = String.format(app.getString(R.string.Import_confirm), sentBits, sentExchange, feeBits, feeExchange);
        String posButton = String.format("%s (%s)", sentBits, sentExchange);
        BRDialog.showCustomDialog(app, "", message, posButton, app.getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        boolean result = BRWalletManager.getInstance().confirmKeySweep(importPrivKeyEntity.getTx(), key);
                        if (!result) {
                            app.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                                            app.getString(R.string.Import_Error_notValid), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                                @Override
                                                public void onClick(BRDialogView brDialogView) {
                                                    brDialogView.dismissWithAnimation();
                                                }
                                            }, null, null, 0);
                                }
                            });

                        }
                    }
                }).start();

                brDialogView.dismissWithAnimation();

            }
        }, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismissWithAnimation();
            }
        }, null, 0);
        super.onPostExecute(s);
    }

    public static ImportPrivKeyEntity createTx(String url) {
        if (url == null || url.isEmpty()) return null;
        String jsonString = callURL(url);
        if (jsonString == null || jsonString.isEmpty()) return null;
        ImportPrivKeyEntity result = null;
        JSONArray jsonArray = null;
        try {
            jsonArray = new JSONArray(jsonString);
            int length = jsonArray.length();
            if (length > 0)
                BRWalletManager.getInstance().createInputArray();

            for (int i = 0; i < length; i++) {
                JSONObject obj = jsonArray.getJSONObject(i);
                String txid = obj.getString("txid");
                int vout = obj.getInt("vout");
                String scriptPubKey = obj.getString("scriptPubKey");
                long amount = obj.getLong("satoshis");
                byte[] txidBytes = hexStringToByteArray(txid);
                byte[] scriptPubKeyBytes = hexStringToByteArray(scriptPubKey);
                BRWalletManager.getInstance().addInputToPrivKeyTx(txidBytes, vout, scriptPubKeyBytes, amount);
            }

            result = BRWalletManager.getInstance().getPrivKeyObject();

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
