package com.breadwallet.tools.threads;

import android.app.Activity;
import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.tools.security.BitcoinUrlHandler;
import com.breadwallet.tools.util.BytesUtil;

import java.io.DataOutputStream;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.HashMap;
import java.util.Map;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 5/9/16.
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

public class PaymentProtocolPostPaymentTask extends AsyncTask<String, String, String> {
    public static final String TAG = PaymentProtocolPostPaymentTask.class.getName();

    public static final String TITLE = "title";
    public static final String MESSAGE = "message";

    public HttpURLConnection urlConnection;
    public PaymentRequestWrapper paymentRequest = null;
    public static String message;

    public static boolean waiting = true;
    public static boolean sent = false;
    public static Map<String, String> pendingErrorMessages = new HashMap<>();

    public PaymentProtocolPostPaymentTask(PaymentRequestWrapper paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    @Override
    protected String doInBackground(String... uri) {
        InputStream in;
        try {
            waiting = true;
            sent = false;
            URL url = new URL(paymentRequest.paymentURL);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Content-Type", "application/litecoin-payment");
            urlConnection.addRequestProperty("Accept", "application/litecoin-paymentack");
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
            byte[] serializedBytes = BytesUtil.readBytesFromStream(in);
            if (serializedBytes == null || serializedBytes.length == 0) {
                Log.e(TAG, "serializedBytes are null!!!");
                return null;
            }

            message = BitcoinUrlHandler.parsePaymentACK(serializedBytes);
//            PostAuth.getInstance().setTmpPaymentRequest(paymentRequest);
//            PostAuth.getInstance().onPaymentProtocolRequest(app,false);
        } catch (Exception e) {
            Context context = BreadApp.getInstance();
            if (e instanceof java.net.UnknownHostException) {
                pendingErrorMessages.put(TITLE, context.getString(R.string.Alert_error));
                pendingErrorMessages.put(MESSAGE, context.getString(R.string.Send_remoteRequestError));
            }
//            else if (e instanceof FileNotFoundException) {
//                if (app != null) {
//                    pendingErrorMessages.put(TITLE, app.getString(R.string.JailbreakWarnings_title));
//                    pendingErrorMessages.put(MESSAGE, app.getString(R.string.bad_payment_request));
//                }
//            } else if (e instanceof SocketTimeoutException) {
//                if (app != null) {
//                    pendingErrorMessages.put(TITLE, app.getString(R.string.JailbreakWarnings_title));
//                    pendingErrorMessages.put(MESSAGE, app.getString(R.string.connection_timed_out));
//                }
//            } else {
//                if (app != null) {
//                    pendingErrorMessages.put(TITLE, app.getString(R.string.JailbreakWarnings_title));
//                    pendingErrorMessages.put(MESSAGE, app.getString(R.string.could_not_transmit_payment));
////                    if (!((BreadApp) app.getApplication()).hasInternetAccess())
////                        BreadDialog.
////                                showCustomDialog(app,app.getString(R.string.could_not_make_payment), app.getString(R.string.not_connected_network), app.getString(R.string.ok));
//
//                }
//
//            }
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
            waiting = false;
        }
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        handleMessage();

    }

    public static void handleMessage() {
        Activity app = BreadApp.getCurrentActivity();
        if (app != null && message != null) {
            if (!message.isEmpty()) {
                BRToast.
                        showCustomToast(app, message, BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, R.drawable.toast_layout_black);
            } else {
                if (!waiting && !sent && pendingErrorMessages.get(MESSAGE) != null) {
//                    BreadDialog.
//                            showCustomDialog(app,pendingErrorMessages.get(TITLE), pendingErrorMessages.get(MESSAGE), app.getString(R.string.ok));
                    pendingErrorMessages = null;
                }
            }
        }
    }

}
