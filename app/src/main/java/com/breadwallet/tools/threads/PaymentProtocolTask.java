package com.breadwallet.tools.threads;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.exceptions.CertificateChainNotFound;
import com.breadwallet.presenter.fragments.FragmentCurrency;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.tools.security.X509CertificateValidator;
import com.breadwallet.wallet.BRWalletManager;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 5/9/16.
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
public class PaymentProtocolTask extends AsyncTask<String, String, String> {
    public static final String TAG = PaymentProtocolTask.class.getName();

    HttpURLConnection urlConnection;
    String certName = null;
    PaymentRequestWrapper paymentRequest = null;

    @Override
    protected String doInBackground(String... uri) {
        InputStream in;
        final MainActivity app = MainActivity.app;
        try {
            Log.e(TAG, "the uri: " + uri[0]);
            URL url = new URL(uri[0]);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/bitcoin-paymentrequest");
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            urlConnection.setUseCaches(false);
            in = urlConnection.getInputStream();

            String phrase = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE);
            if (phrase == null || phrase.isEmpty()) {
                if (urlConnection != null) urlConnection.disconnect();
                PostAuthenticationProcessor.getInstance().setUri(uri[0]);
                return null;
            }

            if (in == null) {
                Log.e(TAG, "The inputStream is null!");
                return null;
            }
            byte[] serializedBytes = IOUtils.toByteArray(in);
            if (serializedBytes == null || serializedBytes.length == 0) {
                Log.e(TAG, "serializedBytes are null!!!");
                return null;
            }

            paymentRequest = RequestHandler.parsePaymentRequest(serializedBytes, phrase);

            if (paymentRequest == null || paymentRequest.error == PaymentRequestWrapper.INVALID_REQUEST_ERROR) {
                Log.e(TAG, "paymentRequest is null!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "Invalid request", "ok");
                        }
                    });
                }
                paymentRequest = null;
                return null;
            } else if (paymentRequest.error == PaymentRequestWrapper.INSUFFICIENT_FUNDS_ERROR) {
                Log.e(TAG, "insufficient amount!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "Insufficient amount to satisfy the request", "ok");
                        }
                    });
                }
                paymentRequest = null;
                return null;
            } else if (paymentRequest.error == PaymentRequestWrapper.SIGNING_FAILED_ERROR) {
                Log.e(TAG, "failed to sign tx!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "Failed to sign transaction", "ok");
                        }
                    });
                }
                paymentRequest = null;
                return null;
            } else if (paymentRequest.error == PaymentRequestWrapper.REQUEST_TOO_LONG_ERROR) {
                Log.e(TAG, "failed to sign tx!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "PaymentRequest message is too large", "ok");
                        }
                    });
                }
                paymentRequest = null;
                return null;
            } else if (paymentRequest.error == PaymentRequestWrapper.AMOUNTS_ERROR) {
                Log.e(TAG, "failed to sign tx!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "Something went wrong", "ok");
                        }
                    });
                }
                paymentRequest = null;
                return null;
            }

            //Logging
            StringBuilder allAddresses = new StringBuilder();
            for (String s : paymentRequest.addresses) {
                allAddresses.append(s).append(", ");
                if (!BRWalletManager.validateAddress(s)) {
                    if (app != null)
                        ((BreadWalletApp) app.getApplication()).
                                showCustomDialog(app.getString(R.string.attention),
                                        String.format(app.getString(R.string.invalid_address_with_holder), s),
                                        app.getString(R.string.close));
                }
            }
            allAddresses.delete(allAddresses.length() - 2, allAddresses.length());

//                CustomLogger.logThis("Signature", String.valueOf(paymentRequest.signature.length),
//                        "pkiType", paymentRequest.pkiType, "pkiData", String.valueOf(paymentRequest.pkiData.length));
//                CustomLogger.logThis("network", paymentRequest.network, "time", String.valueOf(paymentRequest.time),
//                        "expires", String.valueOf(paymentRequest.expires), "memo", paymentRequest.memo,
//                        "paymentURL", paymentRequest.paymentURL, "merchantDataSize",
//                        String.valueOf(paymentRequest.merchantData.length), "addresses", allAddresses.toString(),
//                        "amount", String.valueOf(paymentRequest.amount));
            //end logging
            if (paymentRequest.expires != 0 && paymentRequest.time > paymentRequest.expires) {
                Log.e(TAG, "Request is expired");
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.attention), "Expired request",
                                    app.getString(R.string.close));
                return null;
            }
            List<X509Certificate> certList = X509CertificateValidator.getCertificateFromBytes(serializedBytes);
            certName = X509CertificateValidator.certificateValidation(certList, paymentRequest);

        } catch (Exception e) {
            if (e instanceof java.net.UnknownHostException) {
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.attention), app.getString(R.string.unknown_host), app.getString(R.string.close));
            } else if (e instanceof FileNotFoundException) {
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.warning), app.getString(R.string.invalid_payment_request), app.getString(R.string.close));
            } else if (e instanceof SocketTimeoutException) {
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.warning), app.getString(R.string.connection_timed_out), app.getString(R.string.close));
            } else if (e instanceof CertificateChainNotFound) {
                Log.e(TAG, "No certificates!", e);
            } else {
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.warning), app.getString(R.string.something_went_wrong), app.getString(R.string.close));
            }
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        String cn = extractCNFromCertName(certName);
        final MainActivity app = MainActivity.app;
        if (app == null) return;
        if (paymentRequest == null || paymentRequest.addresses == null || paymentRequest.addresses.length == 0 || paymentRequest.amount == 0) {
            return;
        }

        boolean certified = false;
        if (cn != null && cn.length() != 0) {
            certified = true;
        }
        StringBuilder allAddresses = new StringBuilder();
        for (String s : paymentRequest.addresses) {
            allAddresses.append(s + ", ");
        }
        allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
        String certification = "";
        if (certified) {
            certification = "certified: " + cn + "\n";
            allAddresses = new StringBuilder();
        }

        //DecimalFormat decimalFormat = new DecimalFormat("0.00");
        SharedPreferences settings;
        settings = app.getSharedPreferences(MainActivity.PREFS_NAME, 0);
        String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        float rate = settings.getFloat(FragmentCurrency.RATE, 1.0f);
        CurrencyManager cm = CurrencyManager.getInstance(app);

        final long total = paymentRequest.amount + paymentRequest.fee;

        final PaymentRequestEntity request = new PaymentRequestEntity(paymentRequest.addresses, paymentRequest.amount, cn, paymentRequest.serializedTx);
        final String message = certification + allAddresses.toString() + "\n\n" + "amount: " + cm.getFormattedCurrencyString("BTC", paymentRequest.amount)
                + " (" + cm.getExchangeForAmount(rate, iso, new BigDecimal(paymentRequest.amount)) + ")" + "\nnetwork fee: +" + cm.getFormattedCurrencyString("BTC", paymentRequest.fee)
                + " (" + cm.getExchangeForAmount(rate, iso, new BigDecimal(paymentRequest.fee)) + ")" + "\ntotal: " + cm.getFormattedCurrencyString("BTC", total)
                + " (" + cm.getExchangeForAmount(rate, iso, new BigDecimal(total)) + ")";

        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new android.app.AlertDialog.Builder(app)
                        .setTitle(app.getString(R.string.payment_info))
                        .setMessage(message)
                        .setPositiveButton(app.getString(R.string.send), new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                ((BreadWalletApp) app.getApplicationContext()).promptForAuthentication(app, BRConstants.AUTH_FOR_PAY, request);
                            }
                        })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
//            Log.e(TAG, "paymentRequest.amount: " + paymentRequest.amount);
//            PaymentRequestEntity requestEntity = new PaymentRequestEntity(paymentRequest.addresses,
//                    paymentRequest.amount, cn);
//            if (app != null) {
//                app.pay(requestEntity.addresses[0], new BigDecimal(requestEntity.amount), requestEntity.cn);
//            }
    }

    private String extractCNFromCertName(String str) {
        if (str == null || str.length() < 4) return null;
        String cn = "CN=";
        int index = -1;
        int endIndex = -1;
        for (int i = 0; i < str.length() - 3; i++) {
            if (str.substring(i, i + 3).equalsIgnoreCase(cn)) {
                index = i + 3;
            }
            if (index != -1) {
                if (str.charAt(i) == ',') {
                    endIndex = i;
                    break;
                }

            }
        }
        String cleanCN = str.substring(index, endIndex);
//            Log.e(TAG, "cleanCN: " + cleanCN);
        return (index != -1 && endIndex != -1) ? cleanCN : null;
    }

}
