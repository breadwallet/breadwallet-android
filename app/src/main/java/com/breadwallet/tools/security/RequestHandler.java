package com.breadwallet.tools.security;

import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestCWrapper;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.presenter.exceptions.CertificateChainNotFound;
import com.breadwallet.presenter.exceptions.PaymentRequestExpiredException;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.tools.CustomLogger;
import com.breadwallet.tools.animation.FragmentAnimator;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 10/19/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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
public class RequestHandler {
    public static final String TAG = RequestHandler.class.getName();
    public static String finalAddress;
    public static final String REQ_ADDRESS = "addresses";
    public static final String REQ_PAYMENT_REQUEST = "request";
    public static final String REQ_PAYMENT_URL = "url";
    public static final Object lockObject = new Object();

    public static void processRequest(String address) {
        try {
            MainActivity app = MainActivity.app;
            RequestObject requestObject = getRequestFromString(address);
            if (requestObject == null) {
                if (app != null) {
                    ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "invalid address", "close");
                }
                return;
            }
            if (requestObject.r != null) {
                tryAndProcessRequestURL(requestObject);
            } else if (requestObject.address != null) {
                tryAndProcessBitcoinURL(requestObject);
            } else {
                if (app != null) {
                    ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "invalid payment request", "close");
                }
            }
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    public static RequestObject getRequestFromString(String str)
            throws InvalidAlgorithmParameterException {
        RequestObject obj = new RequestObject();
        if (str.startsWith("bitcoin:")) {
            String[] parts = str.split("\\?", 2);
            String address = parts[0].substring(8);
            Log.e(TAG, "Address: " + address);
            obj.address = address;
            if (parts.length == 1) return obj;
            String[] params = parts[1].split("&");
            for (String s : params) {
                String[] keyValue = s.split("=");
                if (keyValue.length != 2)
                    throw new InvalidAlgorithmParameterException();
                if (keyValue[0].equals("amount")) {
                    obj.amount = keyValue[1];
                } else if (keyValue[0].equals("label")) {
                    obj.label = keyValue[1];
                } else if (keyValue[0].equals("message")) {
                    obj.message = keyValue[1].replace("%20", " ");
                } else if (keyValue[0].startsWith("req")) {
                    obj.req = keyValue[1];
                } else if (keyValue[0].startsWith("r")) {
                    obj.r = keyValue[1];
                }
            }
        }
        return obj;
    }

    private static void tryAndProcessRequestURL(RequestObject requestObject) {
        String theURL = null;
        String url = requestObject.r;
        synchronized (lockObject) {
            try {
                theURL = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            new RequestTask().execute(theURL);
        }

    }

    private static boolean tryAndProcessBitcoinURL(RequestObject requestObject) {
        /** use the C implementation to check it */
        final String str = requestObject.address;
        if (str == null) return false;
        int length = str.length();
        if (length < 26 || length > 35) {
            return false;
        }
        MainActivity.app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                FragmentScanResult.address = str;
                FragmentAnimator.animateScanResultFragment();
            }
        });
        return true;
    }

    static class RequestTask extends AsyncTask<String, String, String> {
        HttpURLConnection urlConnection;
        String certName = null;
        PaymentRequestCWrapper paymentRequest = null;

        @Override
        protected String doInBackground(String... uri) {
            InputStream in;
            MainActivity app = MainActivity.app;
            try {
                Log.e(TAG, "the uri: " + uri[0]);
                URL url = new URL(uri[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept", "application/bitcoin-paymentrequest");
                urlConnection.setConnectTimeout(5000);
                urlConnection.setReadTimeout(10000);
                urlConnection.setUseCaches(false);
                in = urlConnection.getInputStream();
                if (in == null) {
                    Log.e(TAG, "The inputStream is null!");
                    return null;
                }
                byte[] serializedBytes = IOUtils.toByteArray(in);
                if (serializedBytes == null || serializedBytes.length == 0) {
                    throw new NullPointerException("bytes are null!");
                }
                paymentRequest = parsePaymentRequest(serializedBytes);
                //Logging
                StringBuilder allAddresses = new StringBuilder();
                for (String s : paymentRequest.addresses) {
                    allAddresses.append(s + ", ");
                    if(!validateAddress(s)){
                        if (app != null)
                            ((BreadWalletApp) app.getApplication()).
                                    showCustomDialog("Attention", "invalid address\n" + s, "close");
                    }
                }
                allAddresses.delete(allAddresses.length() - 2, allAddresses.length());

                CustomLogger.LogThis("Signature", String.valueOf(paymentRequest.signature.length),
                        "pkiType", paymentRequest.pkiType, "pkiData", String.valueOf(paymentRequest.pkiData.length));
                CustomLogger.LogThis("network", paymentRequest.network, "time", String.valueOf(paymentRequest.time),
                        "expires", String.valueOf(paymentRequest.expires), "memo", paymentRequest.memo,
                        "paymentURL", paymentRequest.paymentURL, "merchantDataSize",
                        String.valueOf(paymentRequest.merchantData.length), "addresses", allAddresses.toString(),
                        "amount", String.valueOf(paymentRequest.amount));
                //end logging
                if (paymentRequest.time > paymentRequest.expires)
                    throw new PaymentRequestExpiredException("The request is expired!");
                List<X509Certificate> certList = X509CertificateValidator.getCertificateFromBytes(serializedBytes);
                certName = X509CertificateValidator.certificateValidation(certList, paymentRequest);

            } catch (Exception e) {

                if (e instanceof java.net.UnknownHostException) {
                    if (app != null)
                        ((BreadWalletApp) app.getApplication()).
                                showCustomDialog("Attention", "unknown host", "close");
                } else if (e instanceof FileNotFoundException) {
                    if (app != null)
                        ((BreadWalletApp) app.getApplication()).
                                showCustomDialog("Warning", "invalid payment request", "close");
                } else if (e instanceof SocketTimeoutException) {
                    if (app != null)
                        ((BreadWalletApp) app.getApplication()).
                                showCustomDialog("Warning", "connection timed out", "close");
                } else if (e instanceof CertificateChainNotFound) {
                    Log.e(TAG, "No certificates!", e);
                } else if (e instanceof PaymentRequestExpiredException) {
                    if (app != null)
                        ((BreadWalletApp) app.getApplication()).
                                showCustomDialog("Warning", "payment request expired", "close");
                } else {
                    if (app != null)
                        ((BreadWalletApp) app.getApplication()).
                                showCustomDialog("Warning", "something went wrong", "close");
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
            Log.e(TAG, "paymentRequest.amount: " + paymentRequest.amount);
            if (paymentRequest == null) return;
            PaymentRequestEntity requestEntity = new PaymentRequestEntity(paymentRequest.addresses,
                    paymentRequest.amount, 13, cn);
            MainActivity app = MainActivity.app;
            if (app != null) {
                app.confirmPay(requestEntity);
            }

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
            Log.e(TAG, "cleanCN: " + cleanCN);
            return (index != -1 && endIndex != -1) ? cleanCN : null;
        }

    }

    public static native PaymentRequestCWrapper parsePaymentRequest(byte[] req);

    public static native byte[] getCertificatesFromPaymentRequest(byte[] req, int index);

    public static native boolean validateAddress(String address);

}
