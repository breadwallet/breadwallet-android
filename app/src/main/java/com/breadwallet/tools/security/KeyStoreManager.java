package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.ByteReader;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.wallet.BRWalletManager;

import junit.framework.Assert;

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
import java.util.HashMap;
import java.util.Map;

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

    public static Map<String, AliasObject> aliasObjectMap;

    private static final String PHRASE_IV = "ivphrase";
    private static final String CANARY_IV = "ivcanary";
    private static final String PUB_KEY_IV = "ivpubkey";
    private static final String WALLET_CREATION_TIME_IV = "ivtime";
    private static final String PASS_CODE_IV = "ivpasscode";
    private static final String FAIL_COUNT_IV = "ivfailcount";
    private static final String SPENT_LIMIT_IV = "ivspendlimit";
    private static final String FAIL_TIMESTAMP_IV = "ivfailtimestamp";
    private static final String AUTH_KEY_IV = "ivauthkey";
    private static final String TOKEN_IV = "ivtoken";
    private static final String PASS_TIME_IV = "passtimetoken";

    public static final String PHRASE_ALIAS = "phrase";
    public static final String CANARY_ALIAS = "canary";
    public static final String PUB_KEY_ALIAS = "pubKey";
    public static final String WALLET_CREATION_TIME_ALIAS = "creationTime";
    public static final String PASS_CODE_ALIAS = "passCode";
    public static final String FAIL_COUNT_ALIAS = "failCount";
    public static final String SPEND_LIMIT_ALIAS = "spendlimit";
    public static final String FAIL_TIMESTAMP_ALIAS = "failTimeStamp";
    public static final String AUTH_KEY_ALIAS = "authKey";
    public static final String TOKEN_ALIAS = "token";
    public static final String PASS_TIME_ALIAS = "passTime";

    private static final String PHRASE_FILENAME = "my_phrase";
    private static final String CANARY_FILENAME = "my_canary";
    private static final String PUB_KEY_FILENAME = "my_pub_key";
    private static final String WALLET_CREATION_TIME_FILENAME = "my_creation_time";
    private static final String PASS_CODE_FILENAME = "my_pass_code";
    private static final String FAIL_COUNT_FILENAME = "my_fail_count";
    private static final String SPEND_LIMIT_FILENAME = "my_spend_limit";
    private static final String FAIL_TIMESTAMP_FILENAME = "my_fail_timestamp";
    private static final String AUTH_KEY_FILENAME = "my_auth_key";
    private static final String TOKEN_FILENAME = "my_token";
    private static final String PASS_TIME_FILENAME = "my_pass_time";

    public static final int AUTH_DURATION_SEC = 300;

    static {
        aliasObjectMap = new HashMap<>();
        aliasObjectMap.put(PHRASE_ALIAS, new AliasObject(PHRASE_ALIAS, PHRASE_FILENAME, PHRASE_IV));
        aliasObjectMap.put(CANARY_ALIAS, new AliasObject(CANARY_ALIAS, CANARY_FILENAME, CANARY_IV));
        aliasObjectMap.put(PUB_KEY_ALIAS, new AliasObject(PUB_KEY_ALIAS, PUB_KEY_FILENAME, PUB_KEY_IV));
        aliasObjectMap.put(WALLET_CREATION_TIME_ALIAS, new AliasObject(WALLET_CREATION_TIME_ALIAS, WALLET_CREATION_TIME_FILENAME, WALLET_CREATION_TIME_IV));
        aliasObjectMap.put(PASS_CODE_ALIAS, new AliasObject(PASS_CODE_ALIAS, PASS_CODE_FILENAME, PASS_CODE_IV));
        aliasObjectMap.put(FAIL_COUNT_ALIAS, new AliasObject(FAIL_COUNT_ALIAS, FAIL_COUNT_FILENAME, FAIL_COUNT_IV));
        aliasObjectMap.put(FAIL_COUNT_ALIAS, new AliasObject(FAIL_COUNT_ALIAS, FAIL_COUNT_FILENAME, FAIL_COUNT_IV));
        aliasObjectMap.put(SPEND_LIMIT_ALIAS, new AliasObject(SPEND_LIMIT_ALIAS, SPEND_LIMIT_FILENAME, SPENT_LIMIT_IV));
        aliasObjectMap.put(FAIL_TIMESTAMP_ALIAS, new AliasObject(FAIL_TIMESTAMP_ALIAS, FAIL_TIMESTAMP_FILENAME, FAIL_TIMESTAMP_IV));
        aliasObjectMap.put(AUTH_KEY_ALIAS, new AliasObject(AUTH_KEY_ALIAS, AUTH_KEY_FILENAME, AUTH_KEY_IV));
        aliasObjectMap.put(TOKEN_ALIAS, new AliasObject(TOKEN_ALIAS, TOKEN_FILENAME, TOKEN_IV));
        aliasObjectMap.put(PASS_TIME_ALIAS, new AliasObject(PASS_TIME_ALIAS, PASS_TIME_FILENAME, PASS_TIME_IV));

        Assert.assertEquals(aliasObjectMap.size(), 11);
        Assert.assertEquals(AUTH_DURATION_SEC, 300);
    }

    private static android.app.AlertDialog dialog;

    private static boolean _setData(Activity context, byte[] data, String alias, String alias_file, String alias_iv, int request_code, boolean auth_required) {
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

    private static byte[] _getData(final Activity context, String alias, String alias_file, String alias_iv, int request_code)
            throws BRKeystoreErrorException {

        if (alias.equals(alias_file) || alias.equals(alias_iv) || alias_file.equals(alias_iv))
            throw new RuntimeException("mistake in parameters!");
        Log.e(TAG, "_getData: " + alias);
        KeyStore keyStore;

        if (alias.equalsIgnoreCase(PHRASE_ALIAS)) {
            System.out.println("some");
        }
        String encryptedDataFilePath = getEncryptedDataFilePath(alias_file, context);
        byte[] result = new byte[0];
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey)
                    keyStore.getKey(alias, null);
            if (secretKey == null) {
                /** no such key, the key is just simply not there */
                boolean fileExists = new File(encryptedDataFilePath).exists();
                Log.e(TAG, "_getData: Phrase file exist: " + fileExists);
                if (!fileExists) return result; /** file also not there, fine then */
                boolean success = false;
                long startTime = System.currentTimeMillis();
                while (!success || System.currentTimeMillis() - startTime < 1000 * 2) { /** while secret is still null or it's been less then 2 seconds*/
                    Log.e(TAG, "_getData: the key is just simply not there");
                    secretKey = (SecretKey) keyStore.getKey(alias, null);
                    if (secretKey != null) {
                        success = true;
                    } else {
                        try {
                            Thread.sleep(50);
                        } catch (InterruptedException e) {
                            e.printStackTrace();
                        }
                    }
                }
                if (secretKey == null) {
                    /** no key but the phrase is there, meaning the keys are gone but the wallet is present */
                    showKeyStoreFailedToLoad(context);
                    throw new BRKeystoreErrorException("no key but the phrase is there");
                }
            }
            byte[] iv = readBytesFromFile(getEncryptedDataFilePath(alias_iv, context));
            Cipher outCipher;
            outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            return ByteReader.readBytesFromStream(cipherInputStream);
        } catch (InvalidKeyException e) {
            Log.e(TAG, "_getData: InvalidKeyException");
            if (e instanceof UserNotAuthenticatedException) {
                /**user not authenticated, ask the system for authentication*/
                Log.e(TAG, Log.getStackTraceString(e));
                showAuthenticationScreen(context, request_code);
                throw new BRKeystoreErrorException(e.getMessage());
            } else if (e instanceof KeyPermanentlyInvalidatedException) {
                showKeyStoreDialog("KeyStore Error", "Your Breadwallet encrypted data was recently invalidated because you disabled your Android lock screen. Please input your phrase to recover your Breadwallet now.", context.getString(R.string.ok), null,
                        new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        }, null, new DialogInterface.OnDismissListener() {
                            @Override
                            public void onDismiss(DialogInterface dialogInterface) {
                                if (context instanceof IntroActivity) {
                                    if (BRAnimator.checkTheMultipressingAvailability()) {
                                        ((IntroActivity) context).showRecoverWalletFragment();
                                    }
                                }
                            }
                        });
            } else {
                Log.e(TAG, "_getData: InvalidKeyException", e);
                showKeyStoreFailedToLoad(context);
                throw new BRKeystoreErrorException("Key store error");
            }
        } catch (IOException | CertificateException | KeyStoreException e) {
            /** keyStore.load(null) threw the Exception, meaning the keystore is unavailable */
            Log.e(TAG, "_getData: keyStore.load(null) threw the Exception, meaning the keystore is unavailable", e);
            if (e instanceof FileNotFoundException) {
                Log.e(TAG, "_getData: File not found exception", e);
                throw new RuntimeException("the key is present but the phrase on the disk no???");
            } else {
                showKeyStoreFailedToLoad(context);
                throw new BRKeystoreErrorException("Failed to load KeyStore");
            }

        } catch (UnrecoverableKeyException | InvalidAlgorithmParameterException | NoSuchAlgorithmException | NoSuchPaddingException e) {
            /** if for any other reason the keystore fails, crash! */
            Log.e(TAG, "getData: error: " + e.getClass().getSuperclass().getName());
            throw new RuntimeException(e.getMessage());
        }
        showKeyStoreFailedToLoad(context);
        throw new BRKeystoreErrorException("unknown");
    }

    private static String getEncryptedDataFilePath(String fileName, Context context) {
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        return filesDirectory + File.separator + fileName;
    }

    private static void showKeyStoreFailedToLoad(final Activity context) {
        showKeyStoreDialog("KeyStore Error", "Failed to load KeyStore. Please try again later or enter your phrase to recover your Breadwallet now.", "recover now", "try later",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        if (BRAnimator.checkTheMultipressingAvailability()) {
                            ((IntroActivity) context).showRecoverWalletFragment();
                        }
                    }
                }, new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        context.finish();
                    }
                },
                null);
    }

    public static boolean putKeyStorePhrase(byte[] strToStore, Activity context, int requestCode) {
        AliasObject obj = aliasObjectMap.get(PHRASE_ALIAS);
        return !(strToStore == null || strToStore.length == 0) && _setData(context, strToStore, obj.alias, obj.datafileName, obj.ivFileName, requestCode, true);
    }

    public static byte[] getKeyStorePhrase(final Activity context, int requestCode)
            throws BRKeystoreErrorException {
        AliasObject obj = aliasObjectMap.get(PHRASE_ALIAS);
        return _getData(context, obj.alias, obj.datafileName, obj.ivFileName, requestCode);
    }

    public static boolean putKeyStoreCanary(String strToStore, Activity context, int requestCode) {
        if (strToStore == null || strToStore.isEmpty()) return false;
        AliasObject obj = aliasObjectMap.get(CANARY_ALIAS);
        byte[] strBytes = new byte[0];
        try {
            strBytes = strToStore.getBytes("UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return strBytes.length != 0 && _setData(context, strBytes, obj.alias, obj.datafileName, obj.ivFileName, requestCode, true);
    }

    public static String getKeyStoreCanary(final Activity context, int requestCode)
            throws BRKeystoreErrorException {
        AliasObject obj = aliasObjectMap.get(CANARY_ALIAS);
        byte[] data = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, requestCode);
        String result = null;
        try {
            result = new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean putMasterPublicKey(byte[] masterPubKey, Activity context) {
        AliasObject obj = aliasObjectMap.get(PUB_KEY_ALIAS);
        return masterPubKey != null && masterPubKey.length != 0 && _setData(context, masterPubKey, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static byte[] getMasterPublicKey(final Activity context) {
        byte[] result = new byte[0];
        AliasObject obj = aliasObjectMap.get(PUB_KEY_ALIAS);
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean putAuthKey(byte[] authKey, Activity context) {
        AliasObject obj = aliasObjectMap.get(AUTH_KEY_ALIAS);
        return authKey != null && authKey.length != 0 && _setData(context, authKey, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static byte[] getAuthKey(final Activity context) {
        AliasObject obj = aliasObjectMap.get(AUTH_KEY_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean putToken(byte[] token, Activity context) {
        AliasObject obj = aliasObjectMap.get(TOKEN_ALIAS);
        return token != null && token.length != 0 && _setData(context, token, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static byte[] getToken(final Activity context) {
        AliasObject obj = aliasObjectMap.get(TOKEN_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean putWalletCreationTime(int creationTime, Activity context) {
        AliasObject obj = aliasObjectMap.get(WALLET_CREATION_TIME_ALIAS);
        byte[] bytesToStore = TypesConverter.intToBytes(creationTime);
        return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static int getWalletCreationTime(final Activity context) {
        AliasObject obj = aliasObjectMap.get(WALLET_CREATION_TIME_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
        return result.length > 0 ? TypesConverter.bytesToInt(result) : 0;
    }

    public static boolean putPassCode(String passcode, Activity context) {
        AliasObject obj = aliasObjectMap.get(PASS_CODE_ALIAS);
        byte[] bytesToStore = passcode.getBytes();
        return _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static String getPassCode(final Activity context) {
        AliasObject obj = aliasObjectMap.get(PASS_CODE_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
        String passCode = new String(result);
        try {
            int test = Integer.parseInt(passCode);
        } catch (Exception e) {
            Log.e(TAG, "getPassCode: " + e.getMessage());
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
        AliasObject obj = aliasObjectMap.get(FAIL_COUNT_ALIAS);
        if (failCount >= 3) {
            long time = SharedPreferencesManager.getSecureTime(context);
            putFailTimeStamp(time, context);
        }
        byte[] bytesToStore = TypesConverter.intToBytes(failCount);
        return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static int getFailCount(final Activity context) {
        AliasObject obj = aliasObjectMap.get(FAIL_COUNT_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }

        return result.length > 0 ? TypesConverter.bytesToInt(result) : 0;
    }

    public static boolean putSpendLimit(long spendLimit, Activity context) {
        AliasObject obj = aliasObjectMap.get(SPEND_LIMIT_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(spendLimit);
        return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static long getSpendLimit(final Activity context) {
        AliasObject obj = aliasObjectMap.get(SPEND_LIMIT_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }

        return result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static boolean putFailTimeStamp(long spendLimit, Activity context) {
        AliasObject obj = aliasObjectMap.get(FAIL_TIMESTAMP_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(spendLimit);
        return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static long getFailTimeStamp(final Activity context) {
        AliasObject obj = aliasObjectMap.get(FAIL_TIMESTAMP_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }

        return result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static boolean putLastPasscodeUsedTime(long time, Activity context) {
        AliasObject obj = aliasObjectMap.get(PASS_TIME_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(time);
        return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
    }

    public static long getLastPasscodeUsedTime(final Activity context) {
        AliasObject obj = aliasObjectMap.get(PASS_TIME_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
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

    public static boolean resetWalletKeyStore(Context context) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int count = 0;
            StringBuilder str = new StringBuilder();
            for (String a : aliasObjectMap.keySet()) {
                keyStore.deleteEntry(a);
                boolean b1 = new File(getEncryptedDataFilePath(aliasObjectMap.get(a).datafileName, context)).delete();
                if (!b1) str.append(" B1:" + a);
                boolean b2 = new File(getEncryptedDataFilePath(aliasObjectMap.get(a).ivFileName, context)).delete();
                if (!b2) str.append(" B2:" + a);
                count++;
            }
            Log.e(TAG, "resetWalletKeyStore: report: " + str.toString());
            Assert.assertEquals(count, 11);

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
        Log.e(TAG, "showAuthenticationScreen: " + requestCode);
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

    static void showKeyStoreDialog(final String title, final String message, final String posButton, final String negButton,
                                   final DialogInterface.OnClickListener posButtonListener,
                                   final DialogInterface.OnClickListener negButtonListener,
                                   final DialogInterface.OnDismissListener dismissListener) {
        Log.e(TAG, "showKeyStoreDialog");
        Activity app = MainActivity.app;
        if (app == null) app = IntroActivity.app;
        if (app == null) {
            Log.e(TAG, "showCustomDialog: FAILED, context is null");
            return;
        }
        final Activity finalApp = app;
        finalApp.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (dialog != null && dialog.isShowing()) {
                    System.out.println("some");
                    if (dialog.getOwnerActivity() != null && !dialog.getOwnerActivity().isDestroyed())
                        dialog.dismiss();
                    else
                        return;
                }
                dialog = new android.app.AlertDialog.Builder(finalApp).
                        setTitle(title)
                        .setMessage(message)
                        .setPositiveButton(posButton, posButtonListener)
                        .setNegativeButton(negButton, negButtonListener)
                        .setOnDismissListener(dismissListener)
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });
    }

    private static class AliasObject {
        String alias;
        String datafileName;
        String ivFileName;

        AliasObject(String alias, String datafileName, String ivFileName) {
            this.alias = alias;
            this.datafileName = datafileName;
            this.ivFileName = ivFileName;
        }

    }

}