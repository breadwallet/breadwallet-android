package com.breadwallet.tools;

import android.util.Log;

import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.StringRequest;
import com.breadwallet.presenter.activities.MainActivity;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertPathValidatorResult;

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
public class AddressReader {
    public static final String TAG = AddressReader.class.getName();

    public static String getTheAddress(String address) {
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
                        return null;
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
        return addressToProcess.substring(startIndex, endIndex);
    }

    private static void processRequestURI(String url) {

        String URL;
        try {
            URL = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            return;
        }
        Log.e(TAG, "URL = " + URL);
        // Request a string response from the provided URL.
        StringRequest stringRequest = new StringRequest(Request.Method.GET, URL,
                new Response.Listener<String>() {
                    @Override
                    public void onResponse(String response) {
//                        String utfResponse = null;
//                        try {
//                            utfResponse = URLDecoder.decode(response, "UTF-16");
//                        } catch (UnsupportedEncodingException e) {
//                            e.printStackTrace();
//                        }

                        Log.e(TAG, "Response is: " + response.toString());
                        // Load CAs from an InputStream
//                        Certificate ca;
//                        InputStream caInput = null;
//                        try {
//                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//                            byte[] certs = getCertificatesFromPaymentRequest(response.getBytes());
//                            caInput = new ByteArrayInputStream(certs);
//                            ca = cf.generateCertificate(caInput);
//                            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
//                            Certificate[] chain = null; //TODO create an array of certs from the byte[] certs
//                            // Create a KeyStore containing our trusted CAs
//                            String keyStoreType = KeyStore.getDefaultType();
//                            KeyStore keyStore = KeyStore.getInstance(keyStoreType);
//                            keyStore.load(null, null);
//                            CertPathValidator cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
//                            keyStore.setCertificateEntry("ca", ca);
//                            PKIXParameters params = new PKIXParameters(keyStore);
//                            CertPath cp = cf.generateCertPath(Arrays.asList(chain));
//                            CertPathValidatorResult cpvr = cpv.validate(cp, params);
//                            // Create a TrustManager that trusts the CAs in our KeyStore
//                            String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
//                            TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
//                            tmf.init(keyStore);
//                        } catch (CertificateException e) {
//                            e.printStackTrace();
//                        } catch (NoSuchAlgorithmException e) {
//                            e.printStackTrace();
//                        } catch (KeyStoreException e) {
//                            e.printStackTrace();
//                        } catch (IOException e) {
//                            e.printStackTrace();
//                        } catch (InvalidAlgorithmParameterException e) {
//                            e.printStackTrace();
//                        } catch (CertPathValidatorException e) {
//                            e.printStackTrace();
//                        } finally {
//                            try {
//                                caInput.close();
//                            } catch (IOException e) {
//                                e.printStackTrace();
//                            }
//                        }

                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        Log.e(TAG, "Response is: That didn't work!", error);
                    }
                });
        // Add the request to the RequestQueue.
        MainActivity.queue.add(stringRequest);
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

    public CertPathValidatorResult certificateValidation(
            CertPath cp, CertPathParameters params, String algorithm) {
        CertPathValidator cpv = null;
        try {
            cpv = CertPathValidator.getInstance(algorithm);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println(nsae);
            System.exit(1);
        }
        // validate certification path ("cp") with specified parameters ("params")
        try {
            CertPathValidatorResult cpvResult = cpv.validate(cp, params);
            return cpvResult;
        } catch (InvalidAlgorithmParameterException iape) {
            System.err.println("validation failed: " + iape);
            System.exit(1);
        } catch (CertPathValidatorException cpve) {
            System.err.println("validation failed: " + cpve);
            System.err.println("index of certificate that caused exception: "
                    + cpve.getIndex());
            System.exit(1);
        }
        return null;
    }

    public CertPath buildCertPath(CertPathParameters params, String algorithm) {
        CertPathBuilder cpb = null;
        try {
            cpb = CertPathBuilder.getInstance(algorithm);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println(nsae);
            System.exit(1);
        }
        // build certification path using specified parameters ("params")
        try {
            CertPathBuilderResult cpbResult = cpb.build(params);
            CertPath cp = cpbResult.getCertPath();
            System.out.println("build passed, path contents: " + cp);
            return cp;
        } catch (InvalidAlgorithmParameterException iape) {
            System.err.println("build failed: " + iape);
            System.exit(1);
        } catch (CertPathBuilderException cpbe) {
            System.err.println("build failed: " + cpbe);
            System.exit(1);
        }
        return null;
    }


    private static native byte[] getCertificatesFromPaymentRequest(byte[] req);
}
