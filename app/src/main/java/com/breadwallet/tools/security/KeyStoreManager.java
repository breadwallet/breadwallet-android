package com.breadwallet.tools.security;

import android.annotation.TargetApi;
import android.app.KeyguardManager;
import android.content.Context;
import android.os.Build;
import android.security.KeyPairGeneratorSpec;
import android.security.keystore.KeyProperties;
import android.util.Log;

import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPublicKey;
import java.util.Calendar;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.security.auth.x500.X500Principal;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/29/15.
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

public class KeyStoreManager {
    private static final String TAG = KeyStoreManager.class.getName();

    private static final String ENCRYPTION_PADDING = "RSA/ECB/PKCS1Padding";
    private static final String CIPHER_PROVIDER = "AndroidOpenSSL";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String BREADWALLET_X500 = "CN=Breadwallet";

    public static final String PHRASE_ALIAS = "phrase";
    private static final String PUB_KEY_ALIAS = "pubKey";
    private static final String WALLET_CREATION_TIME_ALIAS = "creationTime";

    private static final String PHRASE_FILENAME = "my_phrase";
    private static final String PUB_KEY_FILENAME = "my_pub_key";
    private static final String WALLET_CREATION_TIME_FILENAME = "my_creation_time";

    @TargetApi(Build.VERSION_CODES.M)
    @SuppressWarnings("deprecation") // for api < 23
    public static boolean setKeyStoreString(String strToStore, Context context) {

        if (strToStore == null) return false;
        if (strToStore.length() == 0) return false;
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int nBefore = keyStore.size();

            // Create the keys if necessary
            if (!keyStore.containsAlias(PHRASE_ALIAS)) {
                Calendar notBefore = Calendar.getInstance();
                Calendar notAfter = Calendar.getInstance();
                notAfter.add(Calendar.YEAR, 99);
                KeyPairGenerator generator = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(PHRASE_ALIAS)
                        .setKeySize(2048)
                        .setSubject(new X500Principal(BREADWALLET_X500))
                        .setSerialNumber(BigInteger.ONE)
                        .setEncryptionRequired().setStartDate(notBefore.getTime())
                        .setEndDate(notAfter.getTime())
                        .build();
                generator.initialize(spec);
                KeyPair keyPair = generator.generateKeyPair(); // needs to be here
                Log.v(TAG, "The keypair" + keyPair.toString());
            }
            int nAfter = keyStore.size();
            Log.v(TAG, "Before = " + nBefore + " After = " + nAfter);

            RSAPublicKey publicKey;
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(PHRASE_ALIAS, null);
            publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            // Encrypt the text
            String filesDirectory = context.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + PHRASE_FILENAME;
//            Log.v(TAG, "strPhrase = " + strToStore);
//            Log.v(TAG, "dataDirectory = " + dataDirectory);
//            Log.v(TAG, "filesDirectory = " + filesDirectory);
//            Log.v(TAG, "encryptedDataFilePath = " + encryptedDataFilePath);
            Cipher inCipher;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                inCipher = Cipher.getInstance(ENCRYPTION_PADDING, CIPHER_PROVIDER);
            } else {
                inCipher = Cipher.getInstance(ENCRYPTION_PADDING);
            }
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedDataFilePath), inCipher);
            byte[] bytesToStore = strToStore.getBytes("UTF-8");
            Log.e(TAG, "bytesToStore(phrase): length " + bytesToStore.length);
            cipherOutputStream.write(bytesToStore);
            cipherOutputStream.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return false;
    }

    public static String getKeyStoreString(final Context context) {
        Log.e(TAG, "after while and allowKeyStoreAccess: " +
                ((BreadWalletApp) context.getApplicationContext()).allowKeyStoreAccess);
//        if (!((BreadWalletApp) context.getApplicationContext()).allowKeyStoreAccess) {
//            return null;
//        }
        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (myKM.inKeyguardRestrictedInputMode()) {
            Log.e(TAG, "THE SCREEN IS LOCKED!");
            return null;
        } else {
            Log.e(TAG, "THE SCREEN IS UNLOCKED!");
        }
        KeyStore keyStore;
        String recoveredSecret = "";
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + PHRASE_FILENAME;
        PrivateKey privateKey;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(PHRASE_ALIAS, null);
            privateKey = privateKeyEntry.getPrivateKey();
            if (privateKey == null) throw new RuntimeException("private key is null");

            Cipher outCipher;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                outCipher = Cipher.getInstance(ENCRYPTION_PADDING, CIPHER_PROVIDER);
            } else {
                outCipher = Cipher.getInstance(ENCRYPTION_PADDING);
            }
            outCipher.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] roundTrippedBytes = new byte[1000]; //TODO: dynamically resize as we get more data
            int index = 0;
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                roundTrippedBytes[index] = (byte) nextByte;
                index++;
            }
            recoveredSecret = new String(roundTrippedBytes, 0, index, "UTF-8");
            Log.e(TAG, "round tripped string = " + recoveredSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e(TAG, "recovered: " + recoveredSecret);
        return recoveredSecret;
    }

    public static boolean putMasterPublicKey(byte[] masterPubKey, Context context) {

        Log.e(TAG, "putMasterPublicKey length: " + masterPubKey.length);
        if (masterPubKey.length == 0) return false;
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int nBefore = keyStore.size();

            // Create the keys if necessary
            if (!keyStore.containsAlias(PUB_KEY_ALIAS)) {
                Calendar notBefore = Calendar.getInstance();
                Calendar notAfter = Calendar.getInstance();
                notAfter.add(Calendar.YEAR, 99);
                KeyPairGenerator generator = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
                @SuppressWarnings("deprecation") KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(PUB_KEY_ALIAS)
                        .setKeySize(2048)
                        .setSubject(new X500Principal(BREADWALLET_X500))
                        .setSerialNumber(BigInteger.valueOf(2))
                        .setEncryptionRequired().setStartDate(notBefore.getTime())
                        .setEndDate(notAfter.getTime())
                        .build();
                generator.initialize(spec);
                KeyPair keyPair = generator.generateKeyPair(); // needs to be here
                Log.v(TAG, "The keypair" + keyPair.toString());
            }
            int nAfter = keyStore.size();
            Log.v(TAG, "Before = " + nBefore + " After = " + nAfter);

            RSAPublicKey publicKey;
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(PUB_KEY_ALIAS, null);
            publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            // Encrypt the text
            String filesDirectory = context.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + PUB_KEY_FILENAME;
//            Log.v(TAG, "strPhrase = " + strToStore);
//            Log.v(TAG, "dataDirectory = " + dataDirectory);
//            Log.v(TAG, "filesDirectory = " + filesDirectory);
//            Log.v(TAG, "encryptedDataFilePath = " + encryptedDataFilePath);
            Cipher inCipher;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                inCipher = Cipher.getInstance(ENCRYPTION_PADDING, CIPHER_PROVIDER);
            } else {
                inCipher = Cipher.getInstance(ENCRYPTION_PADDING);
            }
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedDataFilePath), inCipher);
            cipherOutputStream.write(masterPubKey);
            cipherOutputStream.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return false;
    }

    public static byte[] getMasterPublicKey(final Context context) {
//        if (!((BreadWalletApp) context.getApplicationContext()).allowKeyStoreAccess) {
//            return null;
//        }
        byte[] result = new byte[0];
        KeyStore keyStore;
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + PUB_KEY_FILENAME;
        PrivateKey privateKey;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(PUB_KEY_ALIAS, null);
            privateKey = privateKeyEntry.getPrivateKey();
            if (privateKey == null) throw new RuntimeException("private key is null");

            Cipher outCipher;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                outCipher = Cipher.getInstance(ENCRYPTION_PADDING, CIPHER_PROVIDER);
            } else {
                outCipher = Cipher.getInstance(ENCRYPTION_PADDING);
            }
            outCipher.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            result = IOUtils.toByteArray(cipherInputStream);


            //TODO USE BELOW IF NEEDED TO GET RID OF THE COMMONS IO LIB
//            ByteArrayOutputStream buffer = new ByteArrayOutputStream();
//
//            int nRead;
//            byte[] data = new byte[16384];
//
//            while ((nRead = is.read(data, 0, data.length)) != -1) {
//                buffer.write(data, 0, nRead);
//            }
//
//            buffer.flush();
//
//            return buffer.toByteArray();

//            Log.e(TAG, "round tripped bytes: " + roundTrippedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
    }

    public static boolean putWalletCreationTime(long creationTime, Context context) {

//        Log.e(TAG, "putWalletCreationTime: " + creationTime);
        if (creationTime == 0) return false;
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int nBefore = keyStore.size();

            // Create the keys if necessary
            if (!keyStore.containsAlias(WALLET_CREATION_TIME_ALIAS)) {
                Calendar notBefore = Calendar.getInstance();
                Calendar notAfter = Calendar.getInstance();
                notAfter.add(Calendar.YEAR, 99);
                KeyPairGenerator generator = KeyPairGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_RSA, ANDROID_KEY_STORE);
                @SuppressWarnings("deprecation") KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(context)
                        .setAlias(WALLET_CREATION_TIME_ALIAS)
                        .setKeySize(2048)
                        .setSubject(new X500Principal(BREADWALLET_X500))
                        .setSerialNumber(BigInteger.valueOf(3))
                        .setEncryptionRequired().setStartDate(notBefore.getTime())
                        .setEndDate(notAfter.getTime())
                        .build();
                generator.initialize(spec);
                KeyPair keyPair = generator.generateKeyPair(); // needs to be here
//                Log.v(TAG, "The keypair" + keyPair.toString());
            }
            int nAfter = keyStore.size();
//            Log.v(TAG, "Before = " + nBefore + " After = " + nAfter);

            RSAPublicKey publicKey;
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(WALLET_CREATION_TIME_ALIAS, null);
            publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            // Encrypt the text
            String filesDirectory = context.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + WALLET_CREATION_TIME_FILENAME;
//            Log.v(TAG, "strPhrase = " + strToStore);
//            Log.v(TAG, "dataDirectory = " + dataDirectory);
//            Log.v(TAG, "filesDirectory = " + filesDirectory);
//            Log.v(TAG, "encryptedDataFilePath = " + encryptedDataFilePath);
            Cipher inCipher;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                inCipher = Cipher.getInstance(ENCRYPTION_PADDING, CIPHER_PROVIDER);
            } else {
                inCipher = Cipher.getInstance(ENCRYPTION_PADDING);
            }
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedDataFilePath), inCipher);
            cipherOutputStream.write(longToBytes(creationTime));
            cipherOutputStream.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return false;
    }

    public static long getWalletCreationTime(final Context context) {
//        if (!((BreadWalletApp) context.getApplicationContext()).allowKeyStoreAccess) {
//            return null;
//        }
        byte[] result = new byte[0];
        KeyStore keyStore;
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + WALLET_CREATION_TIME_FILENAME;
        PrivateKey privateKey;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(WALLET_CREATION_TIME_ALIAS, null);
            privateKey = privateKeyEntry.getPrivateKey();
            if (privateKey == null) throw new RuntimeException("private key is null");

            Cipher outCipher;
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
                outCipher = Cipher.getInstance(ENCRYPTION_PADDING, CIPHER_PROVIDER);
            } else {
                outCipher = Cipher.getInstance(ENCRYPTION_PADDING);
            }
            outCipher.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] roundTrippedBytes = new byte[64]; //TODO: dynamically resize as we get more data
            int index = 0;
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                roundTrippedBytes[index] = (byte) nextByte;
                index++;
            }
            result = roundTrippedBytes;
//            Log.e(TAG, "round tripped bytes: " + roundTrippedBytes);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return bytesToLong(result);
    }

    public static boolean deleteKeyStoreEntry() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(KeyStoreManager.PHRASE_ALIAS);
        } catch (CertificateException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static boolean deleteAllKeyStoreEntries() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            Enumeration<String> aliases = keyStore.aliases();
            while (aliases.hasMoreElements()) {
                String alias = aliases.nextElement();
                Log.e(TAG, "Deleting alias: " + alias);
                keyStore.deleteEntry(alias);
            }
        } catch (CertificateException e) {
            e.printStackTrace();
            return false;
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }
        return true;
    }

    public static String getSeed() {
        String denied = "none";
        Log.e(TAG, "in getSeed in KeyStoreManager");
        Context app = MainActivity.app;
        if (app == null)
            app = IntroActivity.app;
        if (app == null) return denied;
        String result = getKeyStoreString(app);

        return result == null ? denied : result;
    }

    private static byte[] longToBytes(long x) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
        buffer.putLong(x);
        return buffer.array();
    }

    private static long bytesToLong(byte[] bytes) {
        ByteBuffer buffer = ByteBuffer.allocate(Long.SIZE);
        buffer.put(bytes);
        buffer.flip();
        return buffer.getLong();
    }

}
