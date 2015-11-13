package com.breadwallet.tools.security;

import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.UnsupportedEncodingException;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLDecoder;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertPath;
import java.security.cert.CertPathBuilder;
import java.security.cert.CertPathBuilderException;
import java.security.cert.CertPathBuilderResult;
import java.security.cert.CertPathParameters;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.PKIXCertPathValidatorResult;
import java.security.cert.PKIXParameters;
import java.security.cert.X509Certificate;
import java.util.ArrayList;

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

    public static void proccessRequest(String address) {

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
        String theURL = null;
        try {
            theURL = URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        new RequestTask().execute(theURL);
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

    public static native PaymentRequestEntity parsePaymentRequest(byte[] req);

    public static native byte[] getCertificatesFromPaymentRequest(byte[] req, int index);

//    public static boolean certificateValidation(byte[] rawCerts) {
//        if (rawCerts == null) {
//            throw new NullPointerException("no certificates supplied");
//        }
//        try {
//            String keystoreType;
//            keystoreType = "JKS";
//            KeyStore keyStore;
//            File file = new File(MainActivity.class.getResource("/cacerts").getFile());
//            InputStream is = new FileInputStream(file);
//            try {
//                keyStore = KeyStore.getInstance(keystoreType);
//                keyStore.load(is, null);
//            } catch (IOException x) {
//                throw new KeyStoreException(x);
//            } catch (GeneralSecurityException x) {
//                throw new KeyStoreException(x);
//            } finally {
//                try {
//                    is.close();
//                } catch (IOException x) {
//                    // Ignored.
//                }
//            }
//            if (keyStore == null) throw new NullPointerException("keystore is null!");
//            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
//
//            // parse each certificate from the chain ...
//            ArrayList<X509Certificate> certs = new ArrayList<>();
//
//            X509Certificate cert = (X509Certificate) certFact.generateCertificate(new ByteArrayInputStream(rawCerts));
//            certs.add(cert);
//
//            // ... and generate the certification path from it.
//            CertPath certPath = certFact.generateCertPath(certs);
//
//            // Retrieves the most-trusted CAs from keystore.
//            PKIXParameters params = new PKIXParameters(keyStore);
//            // Revocation not supported in the current version.
//            params.setRevocationEnabled(false);
//
//            // Now verify the certificate chain is correct and trusted. This let's us get an identity linked pubkey.
//            CertPathValidator validator = CertPathValidator.getInstance("PKIX");
//            PKIXCertPathValidatorResult result = (PKIXCertPathValidatorResult) validator.validate(certPath, params);
//            PublicKey publicKey = result.getPublicKey();
//
//            // OK, we got an identity, now check it was used to sign this message.
//            Signature signature = Signature.getInstance(getPkiSignatureAlgorithm(paymentRequest));
//            // Note that we don't use signature.initVerify(certs.get(0)) here despite it being the most obvious
//            // way to set it up, because we don't care about the constraints specified on the certificates: any
//            // cert that links a key to a domain name or other identity will do for us.
//            signature.initVerify(publicKey);
//
//            // duplicate the payment-request but with an empty signature
//            // then check the again serialized format of it
//            PaymentRequest checkPaymentRequest = new PaymentRequest.Builder(paymentRequest)
//                    .signature(ByteString.EMPTY)
//                    .build();
//
//            // serialize the payment request (now with an empty signature field) and check if the signature verifies
//            signature.update(checkPaymentRequest.toByteArray());
//
//            boolean isValid = signature.verify(paymentRequest.signature.toByteArray());
//
//            if (!isValid) {
//                throw new PaymentRequestException("signature does not match");
//            }
//
//
//            // Signature verifies, get the names from the identity we just verified for presentation to the user.
//            final X509Certificate cert = certs.get(0);
//            //return new PkiVerificationData(displayName, publicKey, result.getTrustAnchor());
//            String displayName = X509Utils.getDisplayNameFromCertificate(cert, true);
//            return new PkiVerificationData(displayName, publicKey, result.getTrustAnchor());
//
//
//        } catch (CertificateException e) {
//            throw new PaymentRequestException("invalid certificate", e);
//        } catch (InvalidKeyException e) {
//            throw new PaymentRequestException("keystore not ready", e);
//        } catch (NoSuchAlgorithmException e) {
//            throw new RuntimeException(e);
//        } catch (InvalidAlgorithmParameterException e) {
//            throw new PaymentRequestException("invalid certificate", e);
//        } catch (KeyStoreException e) {
//            throw new RuntimeException(e);
//        } catch (CertPathValidatorException e) {
//            throw new PaymentRequestException("invalid certificate", e);
//        } catch (SignatureException e) {
//            throw new PaymentRequestException("invalid certificate", e);
//        } catch (FileNotFoundException e) {
//            e.printStackTrace();
//        } catch (PaymentRequestException paymentRequestException) {
//            paymentRequestException.printStackTrace();
//        }
//    }

    public CertPath buildCertPath(CertPathParameters params, String algorithm) {
        CertPathBuilder cpb = null;
        try {
            cpb = CertPathBuilder.getInstance(algorithm);
        } catch (NoSuchAlgorithmException nsae) {
            System.err.println(nsae);
        }
        // build certification path using specified parameters ("params")
        try {
            CertPathBuilderResult cpbResult = cpb.build(params);
            CertPath cp = cpbResult.getCertPath();
            System.out.println("build passed, path contents: " + cp);
            return cp;
        } catch (InvalidAlgorithmParameterException iape) {
            System.err.println("build failed: " + iape);
        } catch (CertPathBuilderException cpbe) {
            System.err.println("build failed: " + cpbe);
        }
        return null;
    }

    static class RequestTask extends AsyncTask<String, String, String> {
        HttpURLConnection urlConnection;

        @Override
        protected String doInBackground(String... uri) {
            InputStream in = null;
            try {
                Log.e(TAG, "the uri: " + uri[0]);
                URL url = new URL(uri[0]);
                urlConnection = (HttpURLConnection) url.openConnection();
                urlConnection.setRequestProperty("Accept", "application/bitcoin-paymentrequest");
                urlConnection.setUseCaches(false);
                in = urlConnection.getInputStream();
                if (in == null)
                    return null;

                byte[] serializedBytes = getBytes(in);
                PaymentRequestEntity paymentRequest = parsePaymentRequest(serializedBytes);
                Log.e(TAG,"Signature: " + paymentRequest.signature + ", pkiType: " + paymentRequest.pkiType);
//                byte[] result = getCertificatesFromPaymentRequest(serializedBytes, 0);
//                certificateValidation(result);
//                Log.e(TAG, "YAYYAYAYYAYYAY: " + result.toString());
            } catch (IOException e) {
                e.printStackTrace();
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

    public static byte[] getBytes(InputStream is) throws IOException {

        int len;
        int size = 1024;
        byte[] buf;

        if (is instanceof ByteArrayInputStream) {
            size = is.available();
            buf = new byte[size];
            len = is.read(buf, 0, size);
        } else {
            ByteArrayOutputStream bos = new ByteArrayOutputStream();
            buf = new byte[size];
            while ((len = is.read(buf, 0, size)) != -1)
                bos.write(buf, 0, len);
            buf = bos.toByteArray();
        }
        return buf;
    }

}
