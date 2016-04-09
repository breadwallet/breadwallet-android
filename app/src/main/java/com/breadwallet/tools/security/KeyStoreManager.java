package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.TypesConverter;

import org.apache.commons.io.IOUtils;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.ByteBuffer;
import java.nio.CharBuffer;
import java.nio.charset.Charset;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.Arrays;
import java.util.Enumeration;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.IvParameterSpec;

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

    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    private static final String PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    private static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    private static final String PHRASE_IV = "ivphrase";
    private static final String PUB_KEY_IV = "ivpubkey";
    private static final String TIME_IV = "ivtime";
    private static final String PASS_CODE_IV = "ivpasscode";

    public static final String PHRASE_ALIAS = "phrase";
    private static final String PUB_KEY_ALIAS = "pubKey";
    private static final String WALLET_CREATION_TIME_ALIAS = "creationTime";
    private static final String PASS_CODE_ALIAS = "passCode";

    private static final String PHRASE_FILENAME = "my_phrase";
    private static final String PUB_KEY_FILENAME = "my_pub_key";
    private static final String WALLET_CREATION_TIME_FILENAME = "my_creation_time";
    private static final String PASS_CODE_FILENAME = "my_pass_code";

    public static final int REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS = 1;

    public static boolean putKeyStorePhrase(String strToStore, Context context) {
        //TODO CHECK FOR FINGERPRINT SET!!! CANNOT WORK WITHOUT IT
        //TODO refactor the String to byte[] or char[]
        if (strToStore == null) return false;
        if (strToStore.length() == 0) return false;

        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int nBefore = keyStore.size();
            // Create the keys if necessary
            if (!keyStore.containsAlias(PHRASE_ALIAS)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                keyGenerator.init(new KeyGenParameterSpec.Builder(PHRASE_ALIAS,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(BLOCK_MODE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(false)
                        .setRandomizedEncryptionRequired(false)
                                // Require that the user has unlocked in the last 30 seconds
//                    .setUserAuthenticationValidityDurationSeconds(-1)
                        .setEncryptionPaddings(PADDING)
                        .build());
                SecretKey key = keyGenerator.generateKey();

            }

            int nAfter = keyStore.size();

            String filesDirectory = context.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + PHRASE_FILENAME;

            SecretKey secret = (SecretKey) keyStore.getKey(PHRASE_ALIAS, null);
            Cipher inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] iv = inCipher.getIV();
            String path = filesDirectory + File.separator + PHRASE_IV;
            boolean success = writeBytesToFile(path, iv);
            if (!success) throw new NullPointerException("FAILED TO WRITE BYTES TO FILE");
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedDataFilePath), inCipher);
            byte[] strBytes = strToStore.getBytes("UTF-8");
            cipherOutputStream.write(strBytes);
            try {
                cipherOutputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showAuthenticationScreen(context);
        } catch (IOException | CertificateException | NoSuchAlgorithmException | InvalidKeyException
                | NoSuchPaddingException | KeyStoreException | UnrecoverableKeyException | InvalidAlgorithmParameterException | NoSuchProviderException e) {
            e.printStackTrace();
        }
        return false;
    }


    public static String getKeyStorePhrase(final Context context) {

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
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey)
                    keyStore.getKey(PHRASE_ALIAS, null);
            if (secretKey == null) throw new NullPointerException("secretKey is null");
//            SecretKeyFactory factory = SecretKeyFactory.getInstance(secretKey.getAlgorithm(), ANDROID_KEY_STORE);
//            KeyInfo keyInfo;
//            try {
//                keyInfo = (KeyInfo) factory.getKeySpec(secretKey, KeyInfo.class);
//                Log.e(TAG, "keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware(): " + keyInfo.isUserAuthenticationRequirementEnforcedBySecureHardware());
//                Log.e(TAG, "keyInfo.isInsideSecureHardware(): " + keyInfo.isInsideSecureHardware());
//                Log.e(TAG, "keyInfo.isUserAuthenticationRequired(): " + keyInfo.isUserAuthenticationRequired());
//            } catch (InvalidKeySpecException e) {
//                Log.e(TAG, "keyInfo is not created, invalid SecretKey");
//            }
            String path = filesDirectory + File.separator + PHRASE_IV;
            byte[] iv = readBytesFromFile(path);
            Cipher outCipher;
            outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] roundTrippedBytes = new byte[256];
            int index = 0;
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                roundTrippedBytes[index] = (byte) nextByte;
                index++;
            }
            recoveredSecret = new String(roundTrippedBytes, 0, index, "UTF-8");
        } catch (Exception e) {
            e.printStackTrace();
        }


        return recoveredSecret;
    }

    public static boolean putMasterPublicKey(byte[] masterPubKey, Context context) {

        if (masterPubKey == null) return false;
        if (masterPubKey.length == 0) return false;
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int nBefore = keyStore.size();
            // Create the keys if necessary
            if (!keyStore.containsAlias(PUB_KEY_ALIAS)) {
                KeyGenerator generator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(PUB_KEY_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(256)
                        .setBlockModes(BLOCK_MODE)
                        .setUserAuthenticationRequired(false)
                        .setRandomizedEncryptionRequired(false)
                        .setEncryptionPaddings(PADDING)
                        .build();
                generator.init(spec);
                generator.generateKey();
            }
            int nAfter = keyStore.size();

//            // Encrypt the text
            String filesDirectory = context.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + PUB_KEY_FILENAME;
            SecretKey secret = (SecretKey) keyStore.getKey(PUB_KEY_ALIAS, null);
            Cipher inCipher;
            inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] iv = inCipher.getIV();
            String path = filesDirectory + File.separator + PUB_KEY_IV;
            boolean success = writeBytesToFile(path, iv);
            if (!success) throw new NullPointerException("FAILED TO WRITE BYTES TO FILE");
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedDataFilePath), inCipher);
            cipherOutputStream.write(masterPubKey);
            try {
                cipherOutputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }

            return true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return false;
    }

    public static byte[] getMasterPublicKey(final Context context) {
        KeyStore keyStore;
        byte[] recoveredSecret = null;
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + PUB_KEY_FILENAME;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey)
                    keyStore.getKey(PUB_KEY_ALIAS, null);
            if (secretKey == null) throw new RuntimeException("secretKey is null");

            String path = filesDirectory + File.separator + PUB_KEY_IV;
            byte[] iv = readBytesFromFile(path);

            Cipher outCipher;
            outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] roundTrippedBytes = new byte[256];
            int index = 0;
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                roundTrippedBytes[index] = (byte) nextByte;
                index++;
            }
            ByteBuffer bf = ByteBuffer.wrap(roundTrippedBytes, 0, index);
            recoveredSecret = trimBytes(bf.array());
            //TODO test the result recoveredSecret
        } catch (Exception e) {
            e.printStackTrace();
        }

        Log.e(TAG, "recovered pubKey: " + recoveredSecret);
        return recoveredSecret;
    }

    public static boolean putWalletCreationTime(int creationTime, Context context) {
        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int nBefore = keyStore.size();
            // Create the keys if necessary
            if (!keyStore.containsAlias(WALLET_CREATION_TIME_ALIAS)) {
//                Calendar notBefore = Calendar.getInstance();
//                Calendar notAfter = Calendar.getInstance();
//                notAfter.add(Calendar.YEAR, 99);
                KeyGenerator generator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(WALLET_CREATION_TIME_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(256)
                        .setBlockModes(BLOCK_MODE)
                        .setUserAuthenticationRequired(false)
                        .setRandomizedEncryptionRequired(false)
                        .setEncryptionPaddings(PADDING)
                        .build();
                generator.init(spec);
                generator.generateKey(); // needs to be here
            }
            int nAfter = keyStore.size();

//            // Encrypt the text
            String filesDirectory = context.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + WALLET_CREATION_TIME_FILENAME;
            SecretKey secret = (SecretKey) keyStore.getKey(WALLET_CREATION_TIME_ALIAS, null);
            Cipher inCipher;
            inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] iv = inCipher.getIV();
            String path = filesDirectory + File.separator + TIME_IV;
            boolean success = writeBytesToFile(path, iv);
            if (!success) throw new NullPointerException("FAILED TO WRITE BYTES TO FILE");
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedDataFilePath), inCipher);
            byte[] bytesToStore = TypesConverter.intToBytes(creationTime);
            cipherOutputStream.write(bytesToStore);
            cipherOutputStream.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            if (keyStore != null)
                try {
                    keyStore.deleteEntry(PASS_CODE_ALIAS);
                } catch (KeyStoreException e1) {
                    e1.printStackTrace();
                }
        }
        return false;

    }

    public static int getWalletCreationTime(final Context context) {
//        if (!((BreadWalletApp) context.getApplicationContext()).allowKeyStoreAccess) {
//            return null;
//        }
        int recoveredSecret = 0;
        KeyStore keyStore;
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + WALLET_CREATION_TIME_FILENAME;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey)
                    keyStore.getKey(WALLET_CREATION_TIME_ALIAS, null);
            if (secretKey == null) throw new RuntimeException("secretKey is null");
            String path = filesDirectory + File.separator + TIME_IV;
            byte[] iv = readBytesFromFile(path);
            Cipher outCipher;
            outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] roundTrippedBytes = new byte[4];
            int index = 0;
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                roundTrippedBytes[index] = (byte) nextByte;
                index++;
            }
            ByteBuffer bf = ByteBuffer.wrap(roundTrippedBytes, 0, index);
            recoveredSecret = TypesConverter.bytesToInt(trimBytes(bf.array()));
            Log.e(TAG, "recovered walletCreationTime: " + recoveredSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return recoveredSecret;
    }


    public static boolean putPassCode(int passcode, Context context) {

        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int nBefore = keyStore.size();
            // Create the keys if necessary
            if (!keyStore.containsAlias(PASS_CODE_ALIAS)) {
//                Calendar notBefore = Calendar.getInstance();
//                Calendar notAfter = Calendar.getInstance();
//                notAfter.add(Calendar.YEAR, 99);
                KeyGenerator generator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);
                KeyGenParameterSpec spec = new KeyGenParameterSpec.Builder(PASS_CODE_ALIAS, KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setKeySize(256)
                        .setBlockModes(BLOCK_MODE)
                        .setUserAuthenticationRequired(false)
                        .setRandomizedEncryptionRequired(false)
                        .setEncryptionPaddings(PADDING)
                        .build();
                generator.init(spec);
                generator.generateKey(); // needs to be here
            }
            int nAfter = keyStore.size();

//            // Encrypt the text
            String filesDirectory = context.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + PASS_CODE_FILENAME;
            SecretKey secret = (SecretKey) keyStore.getKey(PASS_CODE_ALIAS, null);
            Cipher inCipher;
            inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] iv = inCipher.getIV();
            String path = filesDirectory + File.separator + PASS_CODE_IV;
            boolean success = writeBytesToFile(path, iv);
            if (!success) throw new NullPointerException("FAILED TO WRITE BYTES TO FILE");
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedDataFilePath), inCipher);
            byte[] bytesToStore = TypesConverter.intToBytes(passcode);
            cipherOutputStream.write(bytesToStore);
            cipherOutputStream.close();
            return true;
        } catch (Exception e) {
            Log.e(TAG, Log.getStackTraceString(e));
            if (keyStore != null)
                try {
                    keyStore.deleteEntry(PASS_CODE_ALIAS);
                } catch (KeyStoreException e1) {
                    e1.printStackTrace();
                }
        }
        return false;
    }

    public static int getPassCode(final Context context) {
//        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
//        if (myKM.inKeyguardRestrictedInputMode()) {
//            Log.e(TAG, "THE SCREEN IS LOCKED!");
//            return null;
//        } else {
//            Log.e(TAG, "THE SCREEN IS UNLOCKED!");
//        }
        KeyStore keyStore;
        int recoveredSecret = 0;
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + PASS_CODE_FILENAME;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey)
                    keyStore.getKey(PASS_CODE_ALIAS, null);
            if (secretKey == null) throw new RuntimeException("secretKey is null");
            String path = filesDirectory + File.separator + PASS_CODE_IV;
            byte[] iv = readBytesFromFile(path);
            Cipher outCipher;
            outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] roundTrippedBytes = new byte[4];
            int index = 0;
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                roundTrippedBytes[index] = (byte) nextByte;
                index++;
            }
            ByteBuffer bf = ByteBuffer.wrap(roundTrippedBytes, 0, index);
            recoveredSecret = TypesConverter.bytesToInt(trimBytes(bf.array()));
            Log.e(TAG, "recovered passcode: " + recoveredSecret);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return recoveredSecret;
    }

    public static boolean deleteKeyStoreEntry() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(KeyStoreManager.PHRASE_ALIAS);
            keyStore.deleteEntry(KeyStoreManager.PUB_KEY_ALIAS);
            keyStore.deleteEntry(KeyStoreManager.WALLET_CREATION_TIME_ALIAS);
            keyStore.deleteEntry(KeyStoreManager.PASS_CODE_ALIAS);

        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (java.security.cert.CertificateException e) {
            e.printStackTrace();
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
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            return false;
        } catch (KeyStoreException e) {
            e.printStackTrace();
            return false;
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        } catch (java.security.cert.CertificateException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static void showAuthenticationScreen(Context context) {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);

        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(null, null);
        if (intent != null) {
            ((Activity) context).startActivityForResult(intent, REQUEST_CODE_CONFIRM_DEVICE_CREDENTIALS);
        }
    }

    public static byte[] readBytesFromFile(String path) {
        byte[] bytes = null;
        try {
            File file = new File(path);
            FileInputStream fin = new FileInputStream(file);
            bytes = IOUtils.toByteArray(fin);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    public static boolean writeBytesToFile(String path, byte[] data) {


        FileOutputStream fos = null;

        try {
            File file = new File(path);
            fos = new FileOutputStream(file);

            // Writes bytes from the specified byte array to this file output stream
            fos.write(data);
            return true;
        } catch (FileNotFoundException e) {
            System.out.println("File not found" + e);
        } catch (IOException ioe) {
            System.out.println("Exception while writing file " + ioe);
        } finally {
            // close the streams using close method
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (IOException ioe) {
                System.out.println("Error while closing stream: " + ioe);
            }

        }
        return false;
    }

    static byte[] trimBytes(byte[] bytes)
    {
        int i = bytes.length - 1;
        while (i >= 0 && bytes[i] == 0)
        {
            --i;
        }

        return Arrays.copyOf(bytes, i + 1);
    }

//    public static String getSeed() {
//        Log.e(TAG, "in getSeed in KeyStoreManager");
//        String denied = "none";
//        Context app = MainActivity.app;
//        if (app == null)
//            app = IntroActivity.app;
//        if (app == null) return denied;
//        String result = getKeyStoreString(app);
//
//        return result == null ? denied : result;
//    }


}
