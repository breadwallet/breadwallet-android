package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.tools.util.ByteReader;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.wallet.BRWalletManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.text.Normalizer;
import java.util.Arrays;

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
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/29/15.
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

public class KeyStoreManager {
    private static final String TAG = KeyStoreManager.class.getName();

    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    public static final String PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    public static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;
    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    public static final String NO_AUTH = "noauth";

    public static final String PHRASE_IV = "ivphrase";
    public static final String CANARY_IV = "ivcanary";
    public static final String PUB_KEY_IV = "ivpubkey";
    public static final String TIME_IV = "ivtime";
    public static final String PASS_CODE_IV = "ivpasscode";
    public static final String FAIL_COUNT_IV = "ivfailcount";
    public static final String SPENT_LIMIT_IV = "ivspendlimit";
    public static final String FAIL_TIMESTAMP_IV = "ivfailtimestamp";
    public static final String AUTH_KEY_IV = "ivauthkey";

    public static final String PHRASE_ALIAS = "phrase";
    public static final String CANARY_ALIAS = "canary";
    public static final String PUB_KEY_ALIAS = "pubKey";
    public static final String WALLET_CREATION_TIME_ALIAS = "creationTime";
    public static final String PASS_CODE_ALIAS = "passCode";
    public static final String FAIL_COUNT_ALIAS = "failCount";
    public static final String SPEND_LIMIT_ALIAS = "spendlimit";
    public static final String FAIL_TIMESTAMP_ALIAS = "failTimeStamp";
    public static final String AUTH_KEY_ALIAS = "authKey";

    public static final String PHRASE_FILENAME = "my_phrase";
    public static final String CANARY_FILENAME = "my_canary";
    public static final String PUB_KEY_FILENAME = "my_pub_key";
    public static final String WALLET_CREATION_TIME_FILENAME = "my_creation_time";
    public static final String PASS_CODE_FILENAME = "my_pass_code";
    public static final String FAIL_COUNT_FILENAME = "my_fail_count";
    public static final String SPEND_LIMIT_FILENAME = "my_spend_limit";
    public static final String FAIL_TIMESTAMP_FILENAME = "my_fail_timestamp";
    public static final String AUTH_KEY_FILENAME = "my_auth_key";

    public static final int AUTH_DURATION_SEC = 300;

    private static boolean setData(Activity context, byte[] data, String alias, String alias_file, String alias_iv, int request_code, boolean auth_required) {
        if (alias.equals(alias_file) || alias.equals(alias_iv) || alias_file.equals(alias_iv))
            throw new IllegalArgumentException("mistake in parameters!");
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int nBefore = keyStore.size();
            // Create the keys if necessary
            if (!keyStore.containsAlias(alias)) {
                KeyGenerator keyGenerator = KeyGenerator.getInstance(
                        KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

                // Set the alias of the entry in Android KeyStore where the key will appear
                // and the constrains (purposes) in the constructor of the Builder
                keyGenerator.init(new KeyGenParameterSpec.Builder(alias,
                        KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                        .setBlockModes(BLOCK_MODE)
                        .setKeySize(256)
                        .setUserAuthenticationRequired(auth_required)
                        .setUserAuthenticationValidityDurationSeconds(AUTH_DURATION_SEC)
                        .setRandomizedEncryptionRequired(false)
                        .setEncryptionPaddings(PADDING)
                        .build());
                SecretKey key = keyGenerator.generateKey();

            }

            int nAfter = keyStore.size();

            String filesDirectory = context.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + alias_file;

            SecretKey secret = (SecretKey) keyStore.getKey(alias, null);
            Cipher inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] iv = inCipher.getIV();
            String path = filesDirectory + File.separator + alias_iv;
            boolean success = writeBytesToFile(path, iv);
            if (!success) throw new NullPointerException("FAILED TO WRITE BYTES TO FILE");
            CipherOutputStream cipherOutputStream = new CipherOutputStream(
                    new FileOutputStream(encryptedDataFilePath), inCipher);
            cipherOutputStream.write(data);
            try {
                cipherOutputStream.close();
            } catch (Exception ex) {
                ex.printStackTrace();
            }
            return true;
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showAuthenticationScreen(context, request_code);
        } catch (CertificateException | NoSuchAlgorithmException | InvalidKeyException | NullPointerException
                | NoSuchPaddingException | KeyStoreException | UnrecoverableKeyException |
                InvalidAlgorithmParameterException | NoSuchProviderException | IOException e) {
            e.printStackTrace();
        }
        return false;
    }

    private static byte[] getData(Activity context, String alias, String alias_file, String alias_iv, int request_code) {
        if (alias.equals(alias_file) || alias.equals(alias_iv) || alias_file.equals(alias_iv))
            throw new IllegalArgumentException("mistake in parameters!");
        KeyStore keyStore;
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + alias_file;
        byte[] result = new byte[0];
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey)
                    keyStore.getKey(alias, null);
            if (secretKey == null) throw new NullPointerException("secretKey is null");
            String path = filesDirectory + File.separator + alias_iv;
            byte[] iv = readBytesFromFile(path);
            Cipher outCipher;
            outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            result = ByteReader.readBytesFromStream(cipherInputStream);
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, Log.getStackTraceString(e));
            showAuthenticationScreen(context, request_code);
            if (alias.equalsIgnoreCase(CANARY_ALIAS) || alias.equalsIgnoreCase(PHRASE_ALIAS))
                return NO_AUTH.getBytes();
        } catch (IOException | CertificateException | NoSuchAlgorithmException | UnrecoverableKeyException | NullPointerException |
                InvalidAlgorithmParameterException | NoSuchPaddingException | KeyStoreException | InvalidKeyException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean putKeyStorePhrase(byte[] strToStore, Activity context, int requestCode) {
        return !(strToStore == null || strToStore.length == 0) && setData(context, strToStore, PHRASE_ALIAS, PHRASE_FILENAME, PHRASE_IV, requestCode, true);

    }

    public static byte[] getKeyStorePhrase(final Activity context, int requestCode) {

        KeyguardManager myKM = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        if (myKM.inKeyguardRestrictedInputMode())
            return new byte[0];

        return getData(context, PHRASE_ALIAS, PHRASE_FILENAME, PHRASE_IV, requestCode);
    }

    public static boolean putKeyStoreCanary(String strToStore, Activity context, int requestCode) {
        if (strToStore == null || strToStore.isEmpty()) return false;

        byte[] strBytes = new byte[0];
        try {
            strBytes = strToStore.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return strBytes.length != 0 && setData(context, strBytes, CANARY_ALIAS, CANARY_FILENAME, CANARY_IV, requestCode, true);
    }

    public static String getKeyStoreCanary(final Activity context, int requestCode) {
        byte[] data = getData(context, CANARY_ALIAS, CANARY_FILENAME, CANARY_IV, requestCode);
        String result = null;
        try {
            result = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean putMasterPublicKey(byte[] masterPubKey, Activity context) {
        return masterPubKey != null && masterPubKey.length != 0 && setData(context, masterPubKey, PUB_KEY_ALIAS, PUB_KEY_FILENAME, PUB_KEY_IV, 0, false);
    }

    public static byte[] getMasterPublicKey(final Activity context) {
        return getData(context, PUB_KEY_ALIAS, PUB_KEY_FILENAME, PUB_KEY_IV, 0);
    }
    public static boolean putAuthKey(byte[] authKey, Activity context) {
        return authKey != null && authKey.length != 0 && setData(context, authKey, AUTH_KEY_ALIAS, AUTH_KEY_FILENAME, AUTH_KEY_IV, 0, false);
    }

    public static byte[] getAuthKey(final Activity context) {
        return getData(context, AUTH_KEY_ALIAS, AUTH_KEY_FILENAME, AUTH_KEY_IV, 0);
    }

    public static boolean putWalletCreationTime(int creationTime, Activity context) {
        byte[] bytesToStore = TypesConverter.intToBytes(creationTime);
        return bytesToStore.length != 0 && setData(context, bytesToStore, WALLET_CREATION_TIME_ALIAS, WALLET_CREATION_TIME_FILENAME, TIME_IV, 0, false);
    }

    public static int getWalletCreationTime(final Activity context) {
        byte[] result = getData(context, WALLET_CREATION_TIME_ALIAS, WALLET_CREATION_TIME_FILENAME, TIME_IV, 0);
        return result.length > 0 ? TypesConverter.bytesToInt(result) : 0;
    }

    public static boolean putPassCode(String passcode, Activity context) {
        byte[] bytesToStore = passcode.getBytes();
        return setData(context, bytesToStore, PASS_CODE_ALIAS, PASS_CODE_FILENAME, PASS_CODE_IV, 0, false);
    }

    public static String getPassCode(final Activity context) {
        byte[] result = getData(context, PASS_CODE_ALIAS, PASS_CODE_FILENAME, PASS_CODE_IV, 0);
        String passCode = new String(result);
        try {
            int test = Integer.parseInt(passCode);
        } catch (Exception e) {
            e.printStackTrace();
            passCode = "";
            putPassCode(passCode, context);
            KeyStoreManager.putFailCount(0, context);
            KeyStoreManager.putFailTimeStamp(0, context);
            return passCode;
        }
        if (passCode.length() != 4) {
            passCode = "";
            putPassCode(passCode, context);
        }
        return passCode;
    }

    public static boolean putFailCount(int failCount, Activity context) {
        if (failCount >= 3) {
            long time = SharedPreferencesManager.getSecureTime(context);
            putFailTimeStamp(time, context);
        }
        byte[] bytesToStore = TypesConverter.intToBytes(failCount);
        return bytesToStore.length != 0 && setData(context, bytesToStore, FAIL_COUNT_ALIAS, FAIL_COUNT_FILENAME, FAIL_COUNT_IV, 0, false);
    }

    public static int getFailCount(final Activity context) {
        byte[] result = getData(context, FAIL_COUNT_ALIAS, FAIL_COUNT_FILENAME, FAIL_COUNT_IV, 0);
//        Log.e(TAG, "getFailCount: " + (result.length > 0 ? TypesConverter.bytesToInt(result) : 0));
        return result.length > 0 ? TypesConverter.bytesToInt(result) : 0;
    }

    public static boolean putSpendLimit(long spendLimit, Activity context) {

        byte[] bytesToStore = TypesConverter.long2byteArray(spendLimit);
        return bytesToStore.length != 0 && setData(context, bytesToStore, SPEND_LIMIT_ALIAS, SPEND_LIMIT_FILENAME, SPENT_LIMIT_IV, 0, false);
    }

    public static long getSpendLimit(final Activity context) {
        byte[] result = getData(context, SPEND_LIMIT_ALIAS, SPEND_LIMIT_FILENAME, SPENT_LIMIT_IV, 0);
        return result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static boolean putFailTimeStamp(long spendLimit, Activity context) {

        byte[] bytesToStore = TypesConverter.long2byteArray(spendLimit);
        return bytesToStore.length != 0 && setData(context, bytesToStore, FAIL_TIMESTAMP_ALIAS, FAIL_TIMESTAMP_FILENAME, FAIL_TIMESTAMP_IV, 0, false);
    }

    public static long getFailTimeStamp(final Activity context) {
        byte[] result = getData(context, FAIL_TIMESTAMP_ALIAS, FAIL_TIMESTAMP_FILENAME, FAIL_TIMESTAMP_IV, 0);
        return result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static boolean phraseIsValid(String insertedPhrase, Activity activity) {
        String normalizedPhrase = Normalizer.normalize(insertedPhrase.trim(), Normalizer.Form.NFKD);
        if (!BRWalletManager.getInstance(activity).validatePhrase(activity, normalizedPhrase))
            return false;
        BRWalletManager m = BRWalletManager.getInstance(activity);
        byte[] rawPhrase = normalizedPhrase.getBytes();
        byte[] bytePhrase = TypesConverter.getNullTerminatedPhrase(rawPhrase);
        byte[] pubKey = m.getMasterPubKey(bytePhrase);
        byte[] pubKeyFromKeyStore = KeyStoreManager.getMasterPublicKey(activity);
        Arrays.fill(bytePhrase, (byte) 0);
        return Arrays.equals(pubKey, pubKeyFromKeyStore);
    }

    public static boolean resetWalletKeyStore() {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(PHRASE_ALIAS);
            keyStore.deleteEntry(CANARY_ALIAS);
            keyStore.deleteEntry(PUB_KEY_ALIAS);
            keyStore.deleteEntry(WALLET_CREATION_TIME_ALIAS);
            keyStore.deleteEntry(PASS_CODE_ALIAS);
            keyStore.deleteEntry(FAIL_COUNT_ALIAS);
            keyStore.deleteEntry(FAIL_TIMESTAMP_ALIAS);
            keyStore.deleteEntry(AUTH_KEY_ALIAS);
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

    public static void showAuthenticationScreen(Activity context, int requestCode) {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        KeyguardManager mKeyguardManager = (KeyguardManager) context.getSystemService(Context.KEYGUARD_SERVICE);
        Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(context.getString(R.string.auth_required), context.getString(R.string.auth_message));
        if (intent != null) {
            context.startActivityForResult(intent, requestCode);
        } else {
            throw new NullPointerException("no passcode is set");
        }
    }

    public static byte[] readBytesFromFile(String path) {
        byte[] bytes = null;
        try {
            File file = new File(path);
            FileInputStream fin = new FileInputStream(file);
            bytes = ByteReader.readBytesFromStream(fin);
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

}