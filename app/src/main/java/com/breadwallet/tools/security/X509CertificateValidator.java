package com.breadwallet.tools.security;

import android.util.Log;

import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.exceptions.CertificateChainNotFound;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PublicKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Enumeration;
import java.util.List;

import javax.net.ssl.TrustManager;
import javax.net.ssl.TrustManagerFactory;
import javax.net.ssl.X509TrustManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 11/11/15.
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
public class X509CertificateValidator {
    public static final String TAG = X509CertificateValidator.class.getName();
    public static final String PKI_X509_SHA256 = "x509+sha256";
    public static final String PKI_X509_SHA1 = "x509+sha1";
    public static final String PKI_NONE = "none";
    public static final String ROOT_CERTS_DIR = "/system/etc/security/cacerts";


    public static String certificateValidation(List<X509Certificate> certList,
                                                PaymentRequestEntity paymentRequest)
            throws KeyStoreException, CertificateChainNotFound {
        if (certList.size() == 0) {
            throw new CertificateChainNotFound("no certificates supplied");
        }
        String result = null;
        try {
            Log.e(TAG, "The size of certList is: " + certList.size());
//            // parse each certificate from the chain ...
//            ArrayList<X509Certificate> certs = getRootCerts();
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

//            // duplicate the payment-request but with an empty signature
//            // then check the again serialized format of it
//            PaymentRequest checkPaymentRequest = new PaymentRequest.Builder(paymentRequest)
//                    .signature(ByteString.EMPTY)
//                    .build();
//
//            // serialize the payment request (now with an empty signature field) and check if the signature verifies
//            signature.update(checkPaymentRequest.toByteArray());
            TrustManagerFactory tmf = TrustManagerFactory.getInstance("X509");
            tmf.init((KeyStore) null);
            X509Certificate[] certListArray = new X509Certificate[certList.size()];
            for (int i = 0; i < certList.size(); i++) {
                certListArray[i] = certList.get(i);
            }
            TrustManager[] tms = tmf.getTrustManagers();
            for (TrustManager m : tms) {
                X509TrustManager xtm = (X509TrustManager) m;
                Log.d(TAG, "checking chain with " + xtm + ", Alg: " + certListArray[0].getSigAlgName());
                xtm.checkClientTrusted(certListArray, certListArray[0].getSigAlgName());
            }
            PublicKey publicKey = certListArray[0].getPublicKey();
            Signature signature = Signature.getInstance(certList.get(0).getSigAlgName());
            signature.initVerify(publicKey);
            signature.update(paymentRequest.signature);
            signature.initVerify(publicKey);
            result = certList.get(0).getSubjectX500Principal().getName();
            Log.e(TAG,"result cn getName(): " + result);

        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (SignatureException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static ArrayList<X509Certificate> getRootCerts() {
        ArrayList<X509Certificate> certificates = new ArrayList<>();
        try {
            KeyStore ks = KeyStore.getInstance("AndroidCAStore");
            ks.load(null, null);
            Enumeration aliases = ks.aliases();
            while (aliases.hasMoreElements()) {
                String alias = (String) aliases.nextElement();
                X509Certificate cert = (X509Certificate)
                        ks.getCertificate(alias);
                certificates.add(cert);
                Log.d(TAG, "Subject DN: " +
                        cert.getSubjectDN().getName());
                Log.d(TAG, "Issuer DN: " +
                        cert.getIssuerDN().getName());
            }
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return certificates;
    }

    public static List<X509Certificate> getCertificateFromBytes(byte[] rawCerts) {
        Log.e(TAG, "This is the rawCerts.length supplied for certificates: " + rawCerts.length);
        List<X509Certificate> theList = new ArrayList<>();
        byte[] result;
        int i = 0;
        try {
            CertificateFactory certFact = CertificateFactory.getInstance("X.509");
            while (true) {
                result = RequestHandler.getCertificatesFromPaymentRequest(rawCerts, i++);
                Log.e(TAG, "The result certificate #" + i + " : " + result.length);
                if (result.length > 0) {
                    X509Certificate certForValidation = (X509Certificate)
                            certFact.generateCertificate(new ByteArrayInputStream(result));
                    theList.add(certForValidation);
                    Log.e(TAG, "THIS IS THE CERTIFICATE NAME: " + certForValidation.getIssuerDN().toString());
                } else {
                    break;
                }
            }
        } catch (CertificateException e) {
            e.printStackTrace();
        }
        return theList;

    }

}
