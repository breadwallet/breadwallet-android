package com.breadwallet.tools.security;

import android.os.AsyncTask;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.presenter.exceptions.CertificateChainNotFound;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.wallet.BRWalletManager;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URI;
import java.net.URL;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.cert.X509Certificate;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 10/19/15.
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
public class RequestHandler {
    private static final String TAG = RequestHandler.class.getName();
    private static final Object lockObject = new Object();

    public static synchronized void processRequest(MainActivity app, String address) {

        try {
            RequestObject requestObject = getRequestFromString(address);
            if (requestObject == null) {
                if (app != null) {
                    ((BreadWalletApp) app.getApplication()).showCustomDialog(app.getString(R.string.warning),
                            app.getString(R.string.invalid_address), app.getString(R.string.close));
                }
                return;
            }
            if (requestObject.r != null) {
                //TODO fix that later
//                ----------------------------------------------------------------------------------
                ((BreadWalletApp) app.getApplication()).showCustomToast(app, "Payment protocol not available in beta",
                        MainActivity.screenParametersPoint.y / 2 + 300, Toast.LENGTH_LONG, 0);
                if(requestObject.address == null || requestObject.address.isEmpty()) return;
                tryAndProcessBitcoinURL(requestObject, app);
                //----------------------------------------------------------------------------------
//                tryAndProcessRequestURL(requestObject);
                //----------------------------------------------------------------------------------
            } else if (requestObject.address != null) {
                tryAndProcessBitcoinURL(requestObject, app);
            } else {
                if (app != null) {
                    ((BreadWalletApp) app.getApplication()).showCustomDialog(app.getString(R.string.warning),
                            app.getString(R.string.invalid_payment_request), app.getString(R.string.close));
                }
            }
        } catch (InvalidAlgorithmParameterException e) {
            e.printStackTrace();
        }
    }

    public static RequestObject getRequestFromString(String str)
            throws InvalidAlgorithmParameterException {
        if (str == null || str.isEmpty()) return null;
        RequestObject obj = new RequestObject();

        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");
        URI uri = URI.create(tmp);

        if (uri.getScheme() == null || !uri.getScheme().equals("bitcoin")) {
            tmp = "bitcoin://".concat(tmp);
        } else {
            tmp = tmp.replace("bitcoin:", "bitcoin://");
        }
        uri = URI.create(tmp);
//        String[] parts = tmp.split("\\?", 2);
        String host = uri.getHost();
        if (host != null) {
            String addrs = host.trim();
            if (BRWalletManager.validateAddress(addrs)) {
                obj.address = addrs;
            }
        }
        String query = uri.getQuery();
        if (query == null) return obj;
        String[] params = query.split("&");
        for (String s : params) {
            String[] keyValue = s.split("=", 2);
            if (keyValue.length != 2)
                continue;
            if (keyValue[0].trim().equals("amount")) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(keyValue[1]);
                    obj.amount = bigDecimal.multiply(new BigDecimal("100000000")).toString();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                Log.e(TAG, "amount: " + obj.amount);
            } else if (keyValue[0].trim().equals("label")) {
                obj.label = keyValue[1];
                Log.e(TAG, "label: " + obj.label);
            } else if (keyValue[0].trim().equals("message")) {
                obj.message = keyValue[1];
                Log.e(TAG, "message: " + obj.message);
            } else if (keyValue[0].trim().startsWith("req")) {
                obj.req = keyValue[1];
                Log.e(TAG, "req: " + obj.req);
            } else if (keyValue[0].trim().startsWith("r")) {
                obj.r = keyValue[1];
                Log.e(TAG, "r: " + obj.r);
            }
        }
        Log.e(TAG, "obj.address: " + obj.address);
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

    private static boolean tryAndProcessBitcoinURL(RequestObject requestObject, MainActivity app) {
        /** use the C implementation to check it */
        final String str = requestObject.address;
        if (str == null) return false;
        final String[] addresses = new String[1];
        addresses[0] = str;
//        CustomLogger.LogThis("amount", requestObject.amount, "address", requestObject.address);
        if (requestObject.amount != null) {
            BigDecimal bigDecimal = new BigDecimal(requestObject.amount);
            long amount = bigDecimal.longValue();
            if (amount == 0) MainActivity.app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FragmentScanResult.address = str;
                    //TODO find a better way
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            FragmentAnimator.animateScanResultFragment();
                        }
                    }, 1000);

                }
            });
            String strAmount = String.valueOf(amount);
            if (app != null) {
                app.pay(addresses[0], new BigDecimal(strAmount), null);
            }
        } else {
            MainActivity.app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    FragmentScanResult.address = str;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            FragmentAnimator.animateScanResultFragment();
                        }
                    }, 1000);
                }
            });
        }
        return true;
    }

    static class RequestTask extends AsyncTask<String, String, String> {
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
                if (phrase == null) {
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

                paymentRequest = parsePaymentRequest(serializedBytes, phrase);

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

//                CustomLogger.LogThis("Signature", String.valueOf(paymentRequest.signature.length),
//                        "pkiType", paymentRequest.pkiType, "pkiData", String.valueOf(paymentRequest.pkiData.length));
//                CustomLogger.LogThis("network", paymentRequest.network, "time", String.valueOf(paymentRequest.time),
//                        "expires", String.valueOf(paymentRequest.expires), "memo", paymentRequest.memo,
//                        "paymentURL", paymentRequest.paymentURL, "merchantDataSize",
//                        String.valueOf(paymentRequest.merchantData.length), "addresses", allAddresses.toString(),
//                        "amount", String.valueOf(paymentRequest.amount));
                //end logging
                if (paymentRequest.time > paymentRequest.expires) {
                    Log.e(TAG, "Request is expired");
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
            if (paymentRequest == null) return;
//            Log.e(TAG, "paymentRequest.amount: " + paymentRequest.amount);
            PaymentRequestEntity requestEntity = new PaymentRequestEntity(paymentRequest.addresses,
                    paymentRequest.amount, cn);
            MainActivity app = MainActivity.app;
            if (app != null) {
                app.pay(requestEntity.addresses[0], new BigDecimal(requestEntity.amount), requestEntity.cn);
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
//            Log.e(TAG, "cleanCN: " + cleanCN);
            return (index != -1 && endIndex != -1) ? cleanCN : null;
        }

    }

    public static native PaymentRequestWrapper parsePaymentRequest(byte[] req, String phrase);

    public static native byte[] getCertificatesFromPaymentRequest(byte[] req, int index);

}
