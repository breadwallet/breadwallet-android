package com.breadwallet.tools.security;

import android.util.Log;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

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
    public static final String ROOT_CERTS_DIR = "/system/etc/security/cacerts";


    public static boolean validateCertificateChain() {
//        List<File> rootCerts = getRootCerts(ROOT_CERTS_DIR);
//        // Load CAs from an InputStream
//        // (could be from a resource or ByteArrayInputStream or ...)
//        CertificateFactory cf = CertificateFactory.getInstance("X.509");
//        // From https://www.washington.edu/itconnect/security/ca/load-der.crt
//        InputStream caInput = new BufferedInputStream(new FileInputStream("load-der.crt"));
//        Certificate ca;
//        try {
//            ca = cf.generateCertificate(caInput);
//            System.out.println("ca=" + ((X509Certificate) ca).getSubjectDN());
//        } finally {
//            caInput.close();
//        }
//
//        // Create a KeyStore containing our trusted CAs
//        String keyStoreType = KeyStore.getDefaultType();
//        KeyStore keyStore = KeyStore.getInstance(keyStoreType);
//        keyStore.load(null, null);
//        keyStore.setCertificateEntry("ca", ca);
//
//        // Create a TrustManager that trusts the CAs in our KeyStore
//        String tmfAlgorithm = TrustManagerFactory.getDefaultAlgorithm();
//        TrustManagerFactory tmf = TrustManagerFactory.getInstance(tmfAlgorithm);
//        tmf.init(keyStore);
//
//        // Create an SSLContext that uses our TrustManager
//        SSLContext context = SSLContext.getInstance("TLS");
//        context.init(null, tmf.getTrustManagers(), null);
//
//        // Tell the URLConnection to use a SocketFactory from our SSLContext
//        URL url = new URL("https://certs.cac.washington.edu/CAtest/");
//        HttpsURLConnection urlConnection =
//                (HttpsURLConnection) url.openConnection();
//        urlConnection.setSSLSocketFactory(context.getSocketFactory());
//        InputStream in = urlConnection.getInputStream();
//        copyInputStreamToOutputStream(in, System.out);
        return false;
    }

    private static List<File> getRootCerts(String dir) {

        File parentDir = new File(dir);
        ArrayList<File> inFiles = new ArrayList<>();
        File[] files = parentDir.listFiles();
        for (File file : files) {
            if (file.isDirectory()) {
                Log.e(TAG, "This one is a directory!");
            } else {
                Log.e(TAG, file.toString());
            }
        }
        return inFiles;
    }
//    try {
//                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//                             chain is of type X509Certificate[]
//                            CertPath cp = cf.generateCertPath(Arrays.asList(certs));
//
//                            CertPathValidator cpv = CertPathValidator.getInstance(CertPathValidator.getDefaultType());
//                            KeyStore ks = KeyStore.getInstance(KeyStore.getDefaultType());
//
//                            File fis = new File();
//                            printFiles(fis);
//
//
//                            ks.load(fis, null);
//                            BufferedInputStream bis = new BufferedInputStream(fis);
//
//                            while (bis.available() > 0) {
//                                Certificate cert = cf.generateCertificate(bis);
//                                Log.e(TAG, "CERT: " + cert.toString());
//                            }
//
//                            PKIXParameters params = new PKIXParameters(ks);
//                            CertPathValidatorResult cpvr = cpv.validate(cp, params);
//                        } catch (CertificateException e) {
//                            e.printStackTrace();
//                        } catch (KeyStoreException e) {
//                            e.printStackTrace();
//                        }
    // Load CAs from an InputStream
//                        Certificate ca;
//                        InputStream caInput = null;
//                        try {
//                            CertificateFactory cf = CertificateFactory.getInstance("X.509");
//                            byte[] certs = parsePaymentRequest(response.getBytes());
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
