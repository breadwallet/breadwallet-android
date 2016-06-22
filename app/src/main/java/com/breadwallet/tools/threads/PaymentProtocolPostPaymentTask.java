package com.breadwallet.tools.threads;

import android.content.DialogInterface;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.exceptions.CertificateChainNotFound;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.ByteReader;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.SharedPreferencesManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.security.RequestHandler;
import com.breadwallet.tools.security.X509CertificateValidator;
import com.breadwallet.wallet.BRWalletManager;

import java.io.BufferedWriter;
import java.io.DataOutputStream;
import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
public class PaymentProtocolPostPaymentTask extends AsyncTask<String, String, String> {
    public static final String TAG = PaymentProtocolPostPaymentTask.class.getName();

    HttpURLConnection urlConnection;
    PaymentRequestWrapper paymentRequest = null;
    String message;

    public PaymentProtocolPostPaymentTask(PaymentRequestWrapper paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    @Override
    protected String doInBackground(String... uri) {
        InputStream in;
        final MainActivity app = MainActivity.app;
        try {
            Log.e(TAG, "the uri: " + paymentRequest.paymentURL);
            URL url = new URL(paymentRequest.paymentURL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/bitcoin-payment");
            urlConnection.addRequestProperty("Accept", "application/bitcoin-paymentack");
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            urlConnection.setRequestMethod("POST");
            urlConnection.setUseCaches(false);
            urlConnection.setDoInput(true);
            urlConnection.setDoOutput(true);

            DataOutputStream dStream = new DataOutputStream(urlConnection.getOutputStream());
            dStream.write(paymentRequest.payment);

            in = urlConnection.getInputStream();

            if (in == null) {
                Log.e(TAG, "The inputStream is null!");
                return null;
            }
            byte[] serializedBytes = ByteReader.readBytesFromStream(in);
            if (serializedBytes == null || serializedBytes.length == 0) {
                Log.e(TAG, "serializedBytes are null!!!");
                return null;
            }

            message = RequestHandler.parsePaymentACK(serializedBytes);
            BRWalletManager.getInstance(app).publishSignedSerializedTransaction(paymentRequest.serializedTx);

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
        final MainActivity app = MainActivity.app;

        if (app != null && message != null) {
            if(!message.isEmpty())
            ((BreadWalletApp) app.getApplication()).
                    showCustomToast(app, message, MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 1);
        }
    }

}
