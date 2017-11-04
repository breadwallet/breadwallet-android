package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.Intent;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BytesUtil;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;
import com.platform.entities.WalletInfo;
import com.platform.tools.KVStoreManager;

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

public class BRKeyStore {
    private static final String TAG = BRKeyStore.class.getName();

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
    private static final String TOTAL_LIMIT_IV = "ivtotallimit";
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
    public static final String TOTAL_LIMIT_ALIAS = "totallimit";
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
    private static final String TOTAL_LIMIT_FILENAME = "my_total_limit";
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
        aliasObjectMap.put(TOTAL_LIMIT_ALIAS, new AliasObject(TOTAL_LIMIT_ALIAS, TOTAL_LIMIT_FILENAME, TOTAL_LIMIT_IV));

        Assert.assertEquals(aliasObjectMap.size(), 12);
//        Assert.assertEquals(AUTH_DURATION_SEC, 300);
    }

    private synchronized static boolean _setData(Context context, byte[] data, String alias, String alias_file, String alias_iv,
                                                 int request_code, boolean auth_required) throws UserNotAuthenticatedException {
//        Log.e(TAG, "_setData: " + alias);
        if (alias.equals(alias_file) || alias.equals(alias_iv) || alias_file.equals(alias_iv)) {
            RuntimeException ex = new IllegalArgumentException("_setData:mistake in parameters");
            BRErrorPipe.parseKeyStoreError(context, ex, alias, true);
            throw ex;
        }

        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
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

            String encryptedDataFilePath = getEncryptedDataFilePath(alias_file, context);

            SecretKey secret = (SecretKey) keyStore.getKey(alias, null);
            if (secret == null) {
                BRKeystoreErrorException ex = new BRKeystoreErrorException("secret is null on _setData: " + alias);
                BRErrorPipe.parseKeyStoreError(context, ex, alias, true);
                return false;
            }
            Cipher inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
//            try {
            inCipher.init(Cipher.ENCRYPT_MODE, secret);
//            } catch (InvalidKeyException ex) {
//                /** store this data with the new algorithm */
//                Log.e(TAG, "_setData: Old algorithm, clearing...");
//                boolean deleteIv = new File(getEncryptedDataFilePath(alias_iv, context)).delete();
//                boolean deleteData = new File(getEncryptedDataFilePath(alias_file, context)).delete();
//                if (deleteIv && deleteData) {
//                    keyStore.deleteEntry(alias);
//                    boolean needsAuth = alias.equalsIgnoreCase(CANARY_ALIAS) || alias.equalsIgnoreCase(PHRASE_ALIAS);
//                    return _setData(context, data, alias, alias_file, alias_iv, request_code, needsAuth);
//                } else {
//                    Log.e(TAG, "_setData: deleteIv:" + deleteIv + ", deleteData: " + deleteData);
//                }
//            }
            byte[] iv = inCipher.getIV();
            String path = getEncryptedDataFilePath(alias_iv, context);
            boolean success = writeBytesToFile(path, iv);
            if (!success) {
                RuntimeException ex = new NullPointerException("failed to writeBytesToFile: " + alias);
                BRErrorPipe.parseKeyStoreError(context, ex, alias, true);
                throw ex;
            }
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
            Log.d(TAG, "setData: User not Authenticated, requesting..." + alias + ", err(" + e.getMessage() + ")");
            showAuthenticationScreen(context, request_code);
            throw e;
        } catch (Exception e) {
            BRErrorPipe.parseKeyStoreError(context, e, alias, true);
            e.printStackTrace();
            return false;
        }
    }

    private synchronized static byte[] _getData(final Context context, String alias, String alias_file, String alias_iv, int request_code)
            throws UserNotAuthenticatedException {
//        Log.e(TAG, "_getData: " + alias);

        if (alias.equals(alias_file) || alias.equals(alias_iv) || alias_file.equals(alias_iv)) {
            RuntimeException ex = new IllegalArgumentException("_getData:mistake in parameters!");
            BRErrorPipe.parseKeyStoreError(context, ex, alias, true);
            return null;
        }
        KeyStore keyStore;

        String encryptedDataFilePath = getEncryptedDataFilePath(alias_file, context);
        byte[] result = new byte[0];
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, null);
            if (secretKey == null) {
                /* no such key, the key is just simply not there */
                boolean fileExists = new File(encryptedDataFilePath).exists();
//                Log.e(TAG, "_getData: " + alias + " file exist: " + fileExists);
                if (!fileExists) {
                    return null;/* file also not there, fine then */
                }
                BRKeystoreErrorException ex = new BRKeystoreErrorException("file is present but the key is gone: " + alias);
                BRErrorPipe.parseKeyStoreError(context, ex, alias, true);
                return null;
            }

            boolean ivExists = new File(getEncryptedDataFilePath(alias_iv, context)).exists();
            boolean aliasExists = new File(getEncryptedDataFilePath(alias_file, context)).exists();
            if (!ivExists || !aliasExists) {
                removeAliasAndFiles(alias, context);
                //report it if one exists and not the other.
                if (ivExists != aliasExists) {
                    BRKeystoreErrorException ex = new BRKeystoreErrorException("alias or iv isn't on the disk: " + alias + ", aliasExists:" + aliasExists);
                    BRErrorPipe.parseKeyStoreError(context, ex, alias, true);
                    return null;
                } else {
                    BRKeystoreErrorException ex = new BRKeystoreErrorException("!ivExists && !aliasExists");
                    BRErrorPipe.parseKeyStoreError(context, ex, alias, true);
                    return null;
                }
            }

            byte[] iv = readBytesFromFile(getEncryptedDataFilePath(alias_iv, context));
            Cipher outCipher;
//            boolean isOldAlgorithm = false;
//            try {
            outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
//            } catch (InvalidAlgorithmParameterException ignored) {
//                /** means the keys are created with the old algorithm */
//                Log.e(TAG, "_getData: found old keys");
//                outCipher = Cipher.getInstance("AES/CBC/PKCS7Padding");
//                try {
//                    outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
//                    isOldAlgorithm = true;
//                    Log.e(TAG, "_getData: recovered old keys");
//                } catch (InvalidAlgorithmParameterException e) {
//                    e.printStackTrace();
//                    BRErrorPipe.parseKeyStoreError(context, e, alias, true);
//                    return null;
//                }
//            }
            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] data = BytesUtil.readBytesFromStream(cipherInputStream);

//            if (isOldAlgorithm) {
//                /** store this data with the new algorithm */
//
//                boolean deleteIv = new File(getEncryptedDataFilePath(alias_iv, context)).delete();
//                boolean deleteData = new File(getEncryptedDataFilePath(alias_file, context)).delete();
//                if (deleteIv && deleteData) {
//                    keyStore.deleteEntry(alias);
//                    boolean needsAuth = alias.equalsIgnoreCase(CANARY_ALIAS) || alias.equalsIgnoreCase(PHRASE_ALIAS);
//                    _setData(context, data, alias, alias_file, alias_iv, request_code, needsAuth);
//                } else {
//                    Log.e(TAG, "_getData: deleteIv:" + deleteIv + ", deleteData: " + deleteData);
//                }
//            }

            return data;
        } catch (InvalidKeyException e) {
            if (e instanceof UserNotAuthenticatedException) {
                /** user not authenticated, ask the system for authentication */
                showAuthenticationScreen(context, request_code);
                throw (UserNotAuthenticatedException) e;
            } else {
                Log.e(TAG, "_getData: InvalidKeyException", e);
                BRErrorPipe.parseKeyStoreError(context, e, alias, true);
                return null;
            }
        } catch (IOException | CertificateException | KeyStoreException e) {
            /** keyStore.load(null) threw the Exception, meaning the keystore is unavailable */
            Log.e(TAG, "_getData: keyStore.load(null) threw the Exception, meaning the keystore is unavailable", e);
            FirebaseCrash.report(e);
            if (e instanceof FileNotFoundException) {
                Log.e(TAG, "_getData: File not found exception", e);

                RuntimeException ex = new RuntimeException("the key is present but the phrase on the disk no");
                BRErrorPipe.parseKeyStoreError(context, ex, alias, true);
                return null;
            } else {
                BRErrorPipe.parseKeyStoreError(context, e, alias, true);
                return null;
            }

        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            /** if for any other reason the keystore fails, crash! */
            Log.e(TAG, "getData: error: " + e.getClass().getSuperclass().getName());
            BRErrorPipe.parseKeyStoreError(context, e, alias, true);
            return null;
        }
    }

    public static String getEncryptedDataFilePath(String fileName, Context context) {
        String filesDirectory = BreadApp.getInstance().getFilesDir().getAbsolutePath();
        return filesDirectory + File.separator + fileName;
    }

    public static boolean putPhrase(byte[] strToStore, Context context, int requestCode) throws UserNotAuthenticatedException {
        AliasObject obj = aliasObjectMap.get(PHRASE_ALIAS);
        return !(strToStore == null || strToStore.length == 0) && _setData(context, strToStore, obj.alias, obj.datafileName, obj.ivFileName, requestCode, true);
    }

    public static byte[] getPhrase(final Context context, int requestCode) throws UserNotAuthenticatedException {
        AliasObject obj = aliasObjectMap.get(PHRASE_ALIAS);
        return _getData(context, obj.alias, obj.datafileName, obj.ivFileName, requestCode);
    }

    public static boolean putCanary(String strToStore, Context context, int requestCode) throws UserNotAuthenticatedException {
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

    public static String getCanary(final Context context, int requestCode) throws UserNotAuthenticatedException {
        AliasObject obj = aliasObjectMap.get(CANARY_ALIAS);
        byte[] data;
        data = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, requestCode);
        String result = null;
        try {
            result = data == null ? null : new String(data, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }
        return result;
    }

    public static boolean putMasterPublicKey(byte[] masterPubKey, Context context) {
        AliasObject obj = aliasObjectMap.get(PUB_KEY_ALIAS);
        try {
            return masterPubKey != null && masterPubKey.length != 0 && _setData(context, masterPubKey, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getMasterPublicKey(final Context context) {
        AliasObject obj = aliasObjectMap.get(PUB_KEY_ALIAS);
        try {
            return _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean putAuthKey(byte[] authKey, Context context) {
        AliasObject obj = aliasObjectMap.get(AUTH_KEY_ALIAS);
        try {
            return authKey != null && authKey.length != 0 && _setData(context, authKey, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getAuthKey(final Context context) {
        AliasObject obj = aliasObjectMap.get(AUTH_KEY_ALIAS);
        try {
            return _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean putToken(byte[] token, Context context) {
        AliasObject obj = aliasObjectMap.get(TOKEN_ALIAS);
        try {
            return token != null && token.length != 0 && _setData(context, token, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getToken(final Context context) {
        AliasObject obj = aliasObjectMap.get(TOKEN_ALIAS);
        try {
            return _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean putWalletCreationTime(int creationTime, Context context) {
        AliasObject obj = aliasObjectMap.get(WALLET_CREATION_TIME_ALIAS);
        byte[] bytesToStore = TypesConverter.intToBytes(creationTime);
        try {
            return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getWalletCreationTime(final Context context) {
        AliasObject obj = aliasObjectMap.get(WALLET_CREATION_TIME_ALIAS);
        byte[] result = null;
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        if (Utils.isNullOrEmpty(result)) {
            //if none, try getting from KVStore
            WalletInfo info = KVStoreManager.getInstance().getWalletInfo(context);
            if (info != null) {
                int creationDate = info.creationDate;
                putWalletCreationTime(creationDate, context);
                return creationDate;
            } else
                return 0;
        } else {
            return TypesConverter.bytesToInt(result);
        }
    }

    public static boolean putPinCode(String pinCode, Context context) {
        AliasObject obj = aliasObjectMap.get(PASS_CODE_ALIAS);
        byte[] bytesToStore = pinCode.getBytes();
        try {
            return _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getPinCode(final Context context) {
        AliasObject obj = aliasObjectMap.get(PASS_CODE_ALIAS);
        byte[] result = null;
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        String pinCode = result == null ? "" : new String(result);
        try {
            Integer.parseInt(pinCode);
        } catch (Exception e) {
            Log.e(TAG, "getPinCode: WARNING passcode isn't a number: " + pinCode);
            pinCode = "";
            putPinCode(pinCode, context);
            putFailCount(0, context);
            putFailTimeStamp(0, context);
            return pinCode;
        }
        if (pinCode.length() != 6 && pinCode.length() != 4) {
            pinCode = "";
            putPinCode(pinCode, context);
            putFailCount(0, context);
            putFailTimeStamp(0, context);
        }
        return pinCode;
    }

    public static boolean putFailCount(int failCount, Context context) {
        AliasObject obj = aliasObjectMap.get(FAIL_COUNT_ALIAS);
        if (failCount >= 3) {
            long time = BRSharedPrefs.getSecureTime(context);
            putFailTimeStamp(time, context);
        }
        byte[] bytesToStore = TypesConverter.intToBytes(failCount);
        try {
            return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getFailCount(final Context context) {
        AliasObject obj = aliasObjectMap.get(FAIL_COUNT_ALIAS);
        byte[] result = null;
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        return result != null && result.length > 0 ? TypesConverter.bytesToInt(result) : 0;
    }

    public static boolean putSpendLimit(long spendLimit, Context context) {
        AliasObject obj = aliasObjectMap.get(SPEND_LIMIT_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(spendLimit);
        try {
            return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static long getSpendLimit(final Context context) {
        AliasObject obj = aliasObjectMap.get(SPEND_LIMIT_ALIAS);
        byte[] result = null;
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        return result != null && result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static boolean putFailTimeStamp(long spendLimit, Context context) {
        AliasObject obj = aliasObjectMap.get(FAIL_TIMESTAMP_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(spendLimit);
        try {
            return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static long getFailTimeStamp(final Context context) {
        AliasObject obj = aliasObjectMap.get(FAIL_TIMESTAMP_ALIAS);
        byte[] result = null;
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }

        return result != null && result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static boolean putLastPinUsedTime(long time, Context context) {
        AliasObject obj = aliasObjectMap.get(PASS_TIME_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(time);
        try {
            return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    // WARNING use AuthManager to get the limit
    public static boolean putTotalLimit(long totalLimit, Context context) {
        AliasObject obj = aliasObjectMap.get(TOTAL_LIMIT_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(totalLimit);
        try {
            return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }


    // WARNING use AuthManager to set the limit
    public static long getTotalLimit(final Context context) {
        AliasObject obj = aliasObjectMap.get(TOTAL_LIMIT_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return (result != null && result.length > 0) ? TypesConverter.byteArray2long(result) : 0;
    }

    public static long getLastPinUsedTime(final Context context) {
        AliasObject obj = aliasObjectMap.get(PASS_TIME_ALIAS);
        byte[] result = null;
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return result != null && result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static boolean phraseIsValid(String insertedPhrase, Context activity) {
        String normalizedPhrase = Normalizer.normalize(insertedPhrase.trim(), Normalizer.Form.NFKD);
        if (!BRWalletManager.getInstance().validatePhrase(activity, normalizedPhrase))
            return false;
        BRWalletManager m = BRWalletManager.getInstance();
        byte[] rawPhrase = normalizedPhrase.getBytes();
        byte[] bytePhrase = TypesConverter.getNullTerminatedPhrase(rawPhrase);
        byte[] pubKey = m.getMasterPubKey(bytePhrase);
        byte[] pubKeyFromKeyStore = new byte[0];
        try {
            pubKeyFromKeyStore = BRKeyStore.getMasterPublicKey(activity);
        } catch (Exception e) {
            e.printStackTrace();
            BRErrorPipe.parseKeyStoreError(activity, e, "", true);
        }
        Arrays.fill(bytePhrase, (byte) 0);
        return Arrays.equals(pubKey, pubKeyFromKeyStore);
    }

    public static boolean resetWalletKeyStore(Context context) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int count = 0;
            while (keyStore.aliases().hasMoreElements()) {
                removeAliasAndFiles(keyStore.aliases().nextElement(), context);
                count++;
            }
//            Assert.assertEquals(count, 11);
            assert (count == 11);

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

    public static void removeAliasAndFiles(String alias, Context context) {
        KeyStore keyStore;
        try {
            boolean b1 = new File(getEncryptedDataFilePath(aliasObjectMap.get(alias).datafileName, context)).delete();
            boolean b2 = new File(getEncryptedDataFilePath(aliasObjectMap.get(alias).ivFileName, context)).delete();
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(alias);
        } catch (KeyStoreException | CertificateException | NoSuchAlgorithmException | IOException e) {
            e.printStackTrace();
        }


    }

    public static void showAuthenticationScreen(Context context, int requestCode) {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        if (context instanceof Activity) {
            Activity app = (Activity) context;
            KeyguardManager mKeyguardManager = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE);
            Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent("Authentication required", context.getString(R.string.Auth_body_Android));
//        Assert.assertTrue(intent != null);
            if (intent != null) {
                app.startActivityForResult(intent, requestCode);
            } else {
                Log.e(TAG, "showAuthenticationScreen: failed to create intent for auth");
                FirebaseCrash.report(new RuntimeException("showAuthenticationScreen: failed to create intent for auth"));
                app.finish();
            }
        } else {
            Log.e(TAG, "showAuthenticationScreen: context is not activity!");
        }
    }

    public static byte[] readBytesFromFile(String path) {
        byte[] bytes = null;
        try {
            File file = new File(path);
            FileInputStream fin = new FileInputStream(file);
            bytes = BytesUtil.readBytesFromStream(fin);
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


    public static class AliasObject {
        public String alias;
        public String datafileName;
        public String ivFileName;

        AliasObject(String alias, String datafileName, String ivFileName) {
            this.alias = alias;
            this.datafileName = datafileName;
            this.ivFileName = ivFileName;
        }

    }
}
