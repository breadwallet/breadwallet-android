package com.breadwallet.tools.security;

import android.os.AsyncTask;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.exceptions.CertificateChainNotFound;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.KeyStoreException;
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

    public static void processRequest(String address) {

        //check if it has an BIP72 request URI
        String addressToProcess = address;
        int length = addressToProcess.length();
        int indx;
        for (indx = 0; indx < length; indx++) {
            if (addressToProcess.charAt(indx) == 'r') {
                if (indx < length - 1)
                    if (addressToProcess.charAt(indx + 1) == '=' &&
                            (addressToProcess.charAt(indx - 1) == '&' /** backwards-compatible */
                                    || addressToProcess.charAt(indx - 1) == '?' /** Non-backwards-compatible */)) {
                        int uriStartIndex = indx + 2;
                        processRequestURI(addressToProcess.substring(uriStartIndex));
                        return;
                    }
            }
        }
        int startIndex = 0;
        int endIndex = 0;
        for (indx = 0; indx < length; indx++) {
            if (addressToProcess.charAt(indx) == ':') {
                if (startIndex == 0) startIndex = indx + 1;
            }
            if (addressToProcess.charAt(indx) == '?') {
                if (endIndex == 0) endIndex = indx + 1;
            }
        }
        if (endIndex == 0) endIndex = length - 1;
        finalAddress = addressToProcess.substring(startIndex, endIndex);
    }

    private static void processRequestURI(String url) {
        synchronized (new Object()) {
            String theURL = null;
            try {
                theURL = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }
            new RequestTask().execute(theURL);
        }
    }

    private static boolean checkTheCleanAddress(String str) {
        /** use the C implementation to check it */
//        if (str == null) return false;
//        int length = str.length();
//        String tmp;
//        if (length < 34) {
//            return false;
//        } else {
//            tmp = str.substring(length - 34);
//            int tmpLength = tmp.length();
//            for (int i = 0; i < tmpLength; i++) {
//                if (tmp.charAt(i) < 48) {
//                    Log.e(TAG, "Bad address, char: " + tmp.charAt(i));
//                    return "";
//                } else {
//                    if (tmp.charAt(i) > 57 && tmp.charAt(i) < 65) {
//                        Log.e(TAG, "Bad address, char: " + tmp.charAt(i));
//                        return "";
//                    }
//                    if (tmp.charAt(i) > 90 && tmp.charAt(i) < 61) {
//                        Log.e(TAG, "Bad address, char: " + tmp.charAt(i));
//                        return "";
//                    }
//                    if (tmp.charAt(i) > 122) {
//                        Log.e(TAG, "Bad address, char: " + tmp.charAt(i));
//                        return "";
//                    }
//                }
//            }
//        }
        return true;
    }


//    public CertPath buildCertPath(CertPathParameters params, String algorithm) {
//        CertPathBuilder cpb = null;
//        try {
//            cpb = CertPathBuilder.getInstance(algorithm);
//        } catch (NoSuchAlgorithmException nsae) {
//            System.err.println(nsae);
//        }
//        // build certification path using specified parameters ("params")
//        try {
//            CertPathBuilderResult cpbResult = cpb.build(params);
//            CertPath cp = cpbResult.getCertPath();
//            System.out.println("build passed, path contents: " + cp);
//            return cp;
//        } catch (InvalidAlgorithmParameterException iape) {
//            System.err.println("build failed: " + iape);
//        } catch (CertPathBuilderException cpbe) {
//            System.err.println("build failed: " + cpbe);
//        }
//        return null;
//    }

    static class RequestTask extends AsyncTask<String, String, String> {
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(String... uri) {
            InputStream in;
            try {
                Log.e(TAG, "the uri: " + uri[0]);
                URL url = new URL(uri[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept", "application/bitcoin-paymentrequest");
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
                PaymentRequestEntity paymentRequest = parsePaymentRequest(serializedBytes);
                Log.e(TAG, "Signature: " + paymentRequest.signature.length + ", pkiType: "
                        + paymentRequest.pkiType + ", pkiData: " + paymentRequest.pkiData.length);

                List<X509Certificate> certList = X509CertificateValidator.getCertificateFromBytes(serializedBytes);
                boolean success = X509CertificateValidator.certificateValidation(certList, paymentRequest);
                Log.e(TAG, "The certificate is valid: " + success);
            } catch (IOException e) {
                if (e instanceof java.net.UnknownHostException) {
                    MainActivity.app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            MainActivity app = MainActivity.app;
                            if (app != null) {
                                ((BreadWalletApp) app.getApplication()).
                                        showCustomToast(app, "No Internet Connection",
                                                MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG);
                            }
                        }
                    });
                }
                e.printStackTrace();
            } catch (KeyStoreException e) {
                e.printStackTrace();
            } catch (CertificateChainNotFound ex) {
                ex.printStackTrace();
                Log.e(TAG, "No certificates!", ex);

            } finally {
                if (urlConnection != null) urlConnection.disconnect();
            }
            return null;
        }

        @Override
        protected void onPostExecute(String result) {
            super.onPostExecute(result);
            //Do anything with response..

        }
    }

    public static native PaymentRequestEntity parsePaymentRequest(byte[] req);

    public static native byte[] getCertificatesFromPaymentRequest(byte[] req, int index);

}
