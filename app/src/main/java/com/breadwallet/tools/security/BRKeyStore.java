package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.security.keystore.KeyGenParameterSpec;
import android.security.keystore.KeyPermanentlyInvalidatedException;
import android.security.keystore.KeyProperties;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.v4.app.FragmentActivity;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextPaint;
import android.text.style.ClickableSpan;
import android.util.Base64;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.tools.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BytesUtil;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.configs.WalletSettingsConfiguration;
import com.platform.entities.WalletInfo;
import com.platform.tools.KVStoreManager;

import junit.framework.Assert;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.security.cert.CertificateException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
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

    private static final String KEY_STORE_PREFS_NAME = "keyStorePrefs";
    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";

    public static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";
    public static final String PADDING = KeyProperties.ENCRYPTION_PADDING_PKCS7;
    public static final String BLOCK_MODE = KeyProperties.BLOCK_MODE_CBC;

    public static final String NEW_CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    public static final String NEW_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE;
    public static final String NEW_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM;

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
    private static final String PASS_TIME_IV = "ivpasstimetoken";
    private static final String ETH_PUBKEY_IV = "ivethpubkey";

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
    public static final String ETH_PUBKEY_ALIAS = "ethpubkey";

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
    private static final String ETH_PUBKEY_FILENAME = "my_eth_pubkey";
    private static boolean bugMessageShowing;

    public static final int AUTH_DURATION_SEC = 300;
    private static final ReentrantLock lock = new ReentrantLock();

    static {
        aliasObjectMap = new HashMap<>();
        aliasObjectMap.put(PHRASE_ALIAS, new AliasObject(PHRASE_ALIAS, PHRASE_FILENAME, PHRASE_IV));
        aliasObjectMap.put(CANARY_ALIAS, new AliasObject(CANARY_ALIAS, CANARY_FILENAME, CANARY_IV));
        aliasObjectMap.put(PUB_KEY_ALIAS, new AliasObject(PUB_KEY_ALIAS, PUB_KEY_FILENAME, PUB_KEY_IV));
        aliasObjectMap.put(WALLET_CREATION_TIME_ALIAS, new AliasObject(WALLET_CREATION_TIME_ALIAS, WALLET_CREATION_TIME_FILENAME, WALLET_CREATION_TIME_IV));
        aliasObjectMap.put(PASS_CODE_ALIAS, new AliasObject(PASS_CODE_ALIAS, PASS_CODE_FILENAME, PASS_CODE_IV));
        aliasObjectMap.put(FAIL_COUNT_ALIAS, new AliasObject(FAIL_COUNT_ALIAS, FAIL_COUNT_FILENAME, FAIL_COUNT_IV));
        aliasObjectMap.put(SPEND_LIMIT_ALIAS, new AliasObject(SPEND_LIMIT_ALIAS, SPEND_LIMIT_FILENAME, SPENT_LIMIT_IV));
        aliasObjectMap.put(FAIL_TIMESTAMP_ALIAS, new AliasObject(FAIL_TIMESTAMP_ALIAS, FAIL_TIMESTAMP_FILENAME, FAIL_TIMESTAMP_IV));
        aliasObjectMap.put(AUTH_KEY_ALIAS, new AliasObject(AUTH_KEY_ALIAS, AUTH_KEY_FILENAME, AUTH_KEY_IV));
        aliasObjectMap.put(TOKEN_ALIAS, new AliasObject(TOKEN_ALIAS, TOKEN_FILENAME, TOKEN_IV));
        aliasObjectMap.put(PASS_TIME_ALIAS, new AliasObject(PASS_TIME_ALIAS, PASS_TIME_FILENAME, PASS_TIME_IV));
        aliasObjectMap.put(TOTAL_LIMIT_ALIAS, new AliasObject(TOTAL_LIMIT_ALIAS, TOTAL_LIMIT_FILENAME, TOTAL_LIMIT_IV));
        aliasObjectMap.put(ETH_PUBKEY_ALIAS, new AliasObject(ETH_PUBKEY_ALIAS, ETH_PUBKEY_FILENAME, ETH_PUBKEY_IV));

        Assert.assertEquals(aliasObjectMap.size(), 13);

    }


    private synchronized static boolean _setData(Context context, byte[] data, String alias, String alias_file, String alias_iv,
                                                 int request_code, boolean auth_required) throws UserNotAuthenticatedException {
//        Log.e(TAG, "_setData: " + alias);
        validateSet(data, alias, alias_file, alias_iv, auth_required);

        KeyStore keyStore = null;
        try {
            lock.lock();
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, null);
            Cipher inCipher = Cipher.getInstance(NEW_CIPHER_ALGORITHM);

            if (secretKey == null) {
                //create key if not present
                secretKey = createKeys(alias, auth_required);
                inCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            } else {
                //see if the key is old format, create a new one if it is
                try {
                    inCipher.init(Cipher.ENCRYPT_MODE, secretKey);
                } catch (InvalidKeyException ignored) {
                    if (ignored instanceof UserNotAuthenticatedException) {
                        throw ignored;
                    }
                    Log.e(TAG, "_setData: OLD KEY PRESENT: " + alias);
                    //create new key and reinitialize the cipher
                    secretKey = createKeys(alias, auth_required);
                    inCipher.init(Cipher.ENCRYPT_MODE, secretKey);
                }
            }

            //the key cannot still be null
            if (secretKey == null) {
                BRKeystoreErrorException ex = new BRKeystoreErrorException("secret is null on _setData: " + alias);
                BRReportsManager.reportBug(ex);
                return false;
            }


            byte[] iv = inCipher.getIV();
            if (iv == null) throw new NullPointerException("iv is null!");

            //store the iv
            storeEncryptedData(context, iv, alias_iv);
            byte[] encryptedData = inCipher.doFinal(data);
            //store the encrypted data
            storeEncryptedData(context, encryptedData, alias);
            return true;
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "_setData: showAuthenticationScreen: " + alias);
            showAuthenticationScreen(context, request_code, alias);
            throw e;
        } catch (InvalidKeyException ex) {
            if (ex instanceof KeyPermanentlyInvalidatedException) {
                showKeyInvalidated(context);
                throw new UserNotAuthenticatedException(); //just to make the flow stop
            }

            BRReportsManager.reportBug(ex);
            return false;
        } catch (Exception e) {
            BRReportsManager.reportBug(e);
            e.printStackTrace();
            return false;
        } finally {
            lock.unlock();
        }
    }

    private static SecretKey createKeys(String alias, boolean auth_required) throws InvalidAlgorithmParameterException, KeyStoreException, NoSuchProviderException, NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        // Set the alias of the entry in Android KeyStore where the key will appear
        // and the constrains (purposes) in the constructor of the Builder
        keyGenerator.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(NEW_BLOCK_MODE)
                .setUserAuthenticationRequired(auth_required)
                .setUserAuthenticationValidityDurationSeconds(AUTH_DURATION_SEC)
                .setRandomizedEncryptionRequired(false)
                .setEncryptionPaddings(NEW_PADDING)
                .build());
        return keyGenerator.generateKey();

    }

    private static byte[] _getData(final Context context, String alias, String alias_file, String alias_iv, int request_code)
            throws UserNotAuthenticatedException {
        validateGet(alias, alias_file, alias_iv);//validate entries
        KeyStore keyStore = null;
//        Log.e(TAG, "_getData: " + alias);
        try {
            lock.lock();
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, null);

            byte[] encryptedData = retrieveEncryptedData(context, alias);
            if (encryptedData != null) {
                //new format data is present, good
                byte[] iv = retrieveEncryptedData(context, alias_iv);
                if (iv == null) {
                    if (alias.equalsIgnoreCase(PHRASE_ALIAS))
                        throw new RuntimeException("iv is missing when data isn't: " + alias + " (Can't proceed, risking user's phrase! )"); //crash here!
                    else
                        BRReportsManager.reportBug(new NullPointerException("iv is missing when data isn't: " + alias));
                    return null;
                }
                Cipher outCipher;

                outCipher = Cipher.getInstance(NEW_CIPHER_ALGORITHM);
                outCipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(128, iv));
                try {
                    byte[] decryptedData = outCipher.doFinal(encryptedData);
                    if (decryptedData != null) {
                        return decryptedData;
                    }
                } catch (IllegalBlockSizeException | BadPaddingException e) {
                    e.printStackTrace();
                    throw new RuntimeException("failed to decrypt data: " + e.getMessage());
                }
            }
            //no new format data, get the old one and migrate it to the new format
            String encryptedDataFilePath = getFilePath(alias_file, context);

            if (secretKey == null) {
                /* no such key, the key is just simply not there */
                boolean fileExists = new File(encryptedDataFilePath).exists();
//                Log.e(TAG, "_getData: " + alias + " file exist: " + fileExists);
                if (!fileExists) {
                    return null;/* file also not there, fine then */
                }
                BRKeystoreErrorException ex = new BRKeystoreErrorException("file is present but the key is gone: " + alias);
                BRReportsManager.reportBug(ex);
                return null;
            }

            boolean ivExists = new File(getFilePath(alias_iv, context)).exists();
            boolean aliasExists = new File(getFilePath(alias_file, context)).exists();
            //cannot happen, they all should be present
            if (!ivExists || !aliasExists) {
                removeAliasAndDatas(keyStore, alias, context);
                //report it if one exists and not the other.
                if (ivExists != aliasExists) {
                    BRKeystoreErrorException ex = new BRKeystoreErrorException("alias or iv isn't on the disk: " + alias + ", aliasExists:" + aliasExists);
                    BRReportsManager.reportBug(ex);
                    return null;
                } else {
                    BRKeystoreErrorException ex = new BRKeystoreErrorException("!ivExists && !aliasExists: " + alias);
                    BRReportsManager.reportBug(ex);
                    return null;
                }
            }

            byte[] iv = readBytesFromFile(getFilePath(alias_iv, context));
            if (Utils.isNullOrEmpty(iv))
                throw new RuntimeException("iv is missing for " + alias);
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] result = BytesUtil.readBytesFromStream(cipherInputStream);
            if (result == null)
                throw new RuntimeException("Failed to read bytes from CipherInputStream for alias " + alias);

            //create the new format key
            SecretKey newKey = createKeys(alias, (alias.equals(PHRASE_ALIAS) || alias.equals(CANARY_ALIAS)));
            if (newKey == null)
                throw new RuntimeException("Failed to create new key for alias " + alias);
            Cipher inCipher = Cipher.getInstance(NEW_CIPHER_ALGORITHM);
            //init the cipher
            inCipher.init(Cipher.ENCRYPT_MODE, newKey);
            iv = inCipher.getIV();
            //store the new iv
            storeEncryptedData(context, iv, alias_iv);
            //encrypt the data
            encryptedData = inCipher.doFinal(result);
            //store the new data
            storeEncryptedData(context, encryptedData, alias);
            return result;

        } catch (InvalidKeyException e) {
            if (e instanceof UserNotAuthenticatedException) {
                /** user not authenticated, ask the system for authentication */
                Log.e(TAG, "_getData: showAuthenticationScreen: " + alias);
                showAuthenticationScreen(context, request_code, alias);
                throw (UserNotAuthenticatedException) e;
            } else {
                Log.e(TAG, "_getData: InvalidKeyException", e);
                BRReportsManager.reportBug(e);
                if (e instanceof KeyPermanentlyInvalidatedException)
                    showKeyInvalidated(context);
                throw new UserNotAuthenticatedException(); //just to not go any further
            }
        } catch (IOException | CertificateException | KeyStoreException e) {
            /** keyStore.load(null) threw the Exception, meaning the keystore is unavailable */
            Log.e(TAG, "_getData: keyStore.load(null) threw the Exception, meaning the keystore is unavailable", e);
            BRReportsManager.reportBug(e);
            if (e instanceof FileNotFoundException) {
                Log.e(TAG, "_getData: File not found exception", e);

                RuntimeException ex = new RuntimeException("the key is present but the phrase on the disk no");
                BRReportsManager.reportBug(ex);
                throw new RuntimeException(e.getMessage());
            } else {
                BRReportsManager.reportBug(e);
                throw new RuntimeException(e.getMessage());
            }

        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            /** if for any other reason the keystore fails, crash! */
            Log.e(TAG, "getData: error: " + e.getClass().getSuperclass().getName());
            BRReportsManager.reportBug(e);
            throw new RuntimeException(e.getMessage());
        } catch (BadPaddingException | IllegalBlockSizeException | NoSuchProviderException e) {
            e.printStackTrace();
            throw new RuntimeException(e.getMessage());
        } finally {
            lock.unlock();
        }
    }

    private static void validateGet(String alias, String alias_file, String alias_iv) throws IllegalArgumentException {
        AliasObject obj = aliasObjectMap.get(alias);
        if (obj != null && (!obj.alias.equals(alias) || !obj.datafileName.equals(alias_file) || !obj.ivFileName.equals(alias_iv))) {
            String err = alias + "|" + alias_file + "|" + alias_iv + ", obj: " + obj.alias + "|" + obj.datafileName + "|" + obj.ivFileName;
            throw new IllegalArgumentException("keystore insert inconsistency in names: " + err);
        }

    }

    private static void validateSet(byte[] data, String alias, String alias_file, String alias_iv, boolean auth_required) throws IllegalArgumentException {
        if (data == null) throw new IllegalArgumentException("keystore insert data is null");
        AliasObject obj = aliasObjectMap.get(alias);
        if (obj != null && (!obj.alias.equals(alias) || !obj.datafileName.equals(alias_file) || !obj.ivFileName.equals(alias_iv))) {
            String err = alias + "|" + alias_file + "|" + alias_iv + ", obj: " + obj.alias + "|" + obj.datafileName + "|" + obj.ivFileName;
            throw new IllegalArgumentException("keystore insert inconsistency in names: " + err);
        }

        if (auth_required)
            if (!alias.equals(PHRASE_ALIAS) && !alias.equals(CANARY_ALIAS))
                throw new IllegalArgumentException("keystore auth_required is true but alias is: " + alias);
    }

    public static void showKeyInvalidated(final Context app) {
        BRDialog.showCustomDialog(app, app.getString(R.string.Alert_keystore_title_android), app.getString(R.string.Alert_keystore_invalidated_android), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
            @Override
            public void onClick(BRDialogView brDialogView) {
                brDialogView.dismissWithAnimation();
            }
        }, null, new DialogInterface.OnDismissListener() {
            @Override
            public void onDismiss(DialogInterface dialog) {
                WalletsMaster.getInstance(app).wipeKeyStore(app);
                WalletsMaster.getInstance(app).wipeWalletButKeystore(app);
                dialog.dismiss();
            }
        }, 0);
    }

    public static String getFilePath(String fileName, Context context) {
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        return filesDirectory + File.separator + fileName;
    }

    public static boolean putPhrase(byte[] strToStore, Context context, int requestCode) throws UserNotAuthenticatedException {
        if (PostAuth.mAuthLoopBugHappened) {
            showLoopBugMessage(context);
            throw new UserNotAuthenticatedException();
        }
        AliasObject obj = aliasObjectMap.get(PHRASE_ALIAS);
        return !(strToStore == null || strToStore.length == 0) && _setData(context, strToStore, obj.alias, obj.datafileName, obj.ivFileName, requestCode, true);
    }

    public static byte[] getPhrase(final Context context, int requestCode) throws UserNotAuthenticatedException {
        if (PostAuth.mAuthLoopBugHappened) {
            showLoopBugMessage(context);
            throw new UserNotAuthenticatedException();
        }
        AliasObject obj = aliasObjectMap.get(PHRASE_ALIAS);
        return _getData(context, obj.alias, obj.datafileName, obj.ivFileName, requestCode);
    }

    public static boolean putCanary(String strToStore, Context context, int requestCode) throws UserNotAuthenticatedException {
        if (PostAuth.mAuthLoopBugHappened) {
            showLoopBugMessage(context);
            throw new UserNotAuthenticatedException();
        }
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
        if (PostAuth.mAuthLoopBugHappened) {
            showLoopBugMessage(context);
            throw new UserNotAuthenticatedException();
        }
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

    public static boolean putEthPublicKey(byte[] masterPubKey, Context context) {
        AliasObject obj = aliasObjectMap.get(ETH_PUBKEY_ALIAS);
        try {
            return masterPubKey != null && masterPubKey.length != 0 && _setData(context, masterPubKey, obj.alias, obj.datafileName, obj.ivFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getEthPublicKey(final Context context) {
        AliasObject obj = aliasObjectMap.get(ETH_PUBKEY_ALIAS);
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

    /**
     * DO NOT USE DIRECTLY, use AuthManager.setPinCode instead.
     *
     * @param pinCode the new pin code
     * @param context the context
     * @return true if succeeded
     */
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
        long start = System.currentTimeMillis();
        AliasObject obj = aliasObjectMap.get(FAIL_COUNT_ALIAS);
        byte[] result = null;
        try {
            result = _getData(context, obj.alias, obj.datafileName, obj.ivFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return result != null && result.length > 0 ? TypesConverter.bytesToInt(result) : 0;
    }

    public static boolean putSpendLimit(Context context, BigDecimal spendLimit, String iso) {
        AliasObject obj = aliasObjectMap.get(SPEND_LIMIT_ALIAS);
        byte[] bytesToStore = spendLimit.toPlainString().getBytes();
        try {
            return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias + iso, obj.datafileName + iso, obj.ivFileName + iso, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static BigDecimal getSpendLimit(final Context context, String iso) {
        AliasObject obj = aliasObjectMap.get(SPEND_LIMIT_ALIAS);
        byte[] result = null;
        try {
            result = _getData(context, obj.alias + iso, obj.datafileName + iso, obj.ivFileName + iso, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        BaseWalletManager wm = WalletsMaster.getInstance(context).getWalletByIso(context, iso);
        WalletSettingsConfiguration configs = wm.getSettingsConfiguration();
        return (result != null && result.length > 0) ?
                new BigDecimal(new String(result)) :
                (configs.getFingerprintLimits().size() != 0 ? configs.getFingerprintLimits().get(1) : BigDecimal.ZERO);
    }

    public static boolean putTotalLimit(Context context, BigDecimal totalLimit, String iso) {
        AliasObject obj = aliasObjectMap.get(TOTAL_LIMIT_ALIAS);
        byte[] bytesToStore = totalLimit.toPlainString().getBytes();
        try {
            return bytesToStore.length != 0 && _setData(context, bytesToStore, obj.alias + iso, obj.datafileName + iso, obj.ivFileName + iso, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static BigDecimal getTotalLimit(final Context context, String iso) {
        AliasObject obj = aliasObjectMap.get(TOTAL_LIMIT_ALIAS);
        byte[] result = new byte[0];
        try {
            result = _getData(context, obj.alias + iso, obj.datafileName + iso, obj.ivFileName + iso, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return (result != null && result.length > 0) ? new BigDecimal(new String(result)) : BigDecimal.ZERO;
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

    public synchronized static boolean resetWalletKeyStore(Context context) {
        KeyStore keyStore;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            int count = 0;
            if (keyStore.aliases() != null) {
                while (keyStore.aliases().hasMoreElements()) {
                    String alias = keyStore.aliases().nextElement();
                    removeAliasAndDatas(keyStore, alias, context);
                    destroyEncryptedData(context, alias);
                    count++;
                }
            } else {
                BRReportsManager.reportBug(new NullPointerException("keyStore.aliases() is null"));
                return false;
            }
            Log.e(TAG, "resetWalletKeyStore: removed:" + count);
//            Assert.assertEquals(count, 11);

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

    public synchronized static void removeAliasAndDatas(KeyStore keyStore, String alias, Context context) {
        if (Utils.isNullOrEmpty(alias)) return;
        try {
            keyStore.deleteEntry(alias);
            AliasObject iv = aliasObjectMap.get(alias);
            if (iv == null) return;

            BRKeyStore.destroyEncryptedData(context, alias);
            BRKeyStore.destroyEncryptedData(context, iv.ivFileName);

        } catch (KeyStoreException e) {
            e.printStackTrace();
        }

    }

    public static void storeEncryptedData(Context ctx, byte[] data, String name) {
        SharedPreferences pref = ctx.getSharedPreferences(KEY_STORE_PREFS_NAME, Context.MODE_PRIVATE);
        String base64 = Base64.encodeToString(data, Base64.DEFAULT);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(name, base64);
        edit.apply();

    }

    public static void destroyEncryptedData(Context ctx, String name) {
        SharedPreferences pref = ctx.getSharedPreferences(KEY_STORE_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.remove(name);
        edit.apply();

    }

    public static byte[] retrieveEncryptedData(Context ctx, String name) {
        SharedPreferences pref = ctx.getSharedPreferences(KEY_STORE_PREFS_NAME, Context.MODE_PRIVATE);
        String base64 = pref.getString(name, null);
        if (base64 == null) return null;
        return Base64.decode(base64, Base64.DEFAULT);
    }

    public synchronized static void showAuthenticationScreen(Context context, int requestCode, String alias) {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        if (!alias.equalsIgnoreCase(PHRASE_ALIAS) && !alias.equalsIgnoreCase(CANARY_ALIAS)) {
            BRReportsManager.reportBug(new IllegalArgumentException("requesting auth for: " + alias), true);
        }
//        Log.e(TAG, "showAuthenticationScreen: " + alias);
        if (context instanceof Activity) {
            Activity app = (Activity) context;
            KeyguardManager mKeyguardManager = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE);
            if (mKeyguardManager == null) {
                NullPointerException ex = new NullPointerException("KeyguardManager is null in showAuthenticationScreen");
                BRReportsManager.reportBug(ex, true);
                return;
            }
            String message = context.getString(R.string.UnlockScreen_touchIdPrompt_android);
            if (Utils.isEmulatorOrDebug(app)) {
                message = alias;
            }
            Intent intent = mKeyguardManager.createConfirmDeviceCredentialIntent(context.getString(R.string.UnlockScreen_touchIdTitle_android), message);

            if (Utils.isEmulatorOrDebug(context))
                intent = mKeyguardManager.createConfirmDeviceCredentialIntent(alias, context.getString(R.string.UnlockScreen_touchIdPrompt_android));
            if (intent != null) {
                app.startActivityForResult(intent, requestCode);
            } else {
                Log.e(TAG, "showAuthenticationScreen: failed to create intent for auth");
                BRReportsManager.reportBug(new RuntimeException("showAuthenticationScreen: failed to create intent for auth"));
                app.finish();
            }
        } else {
            BRReportsManager.reportBug(new RuntimeException("showAuthenticationScreen: context is not activity!"));
            Log.e(TAG, "showAuthenticationScreen: context is not activity!");
        }
    }

    public static byte[] readBytesFromFile(String path) {
        byte[] bytes = null;
        FileInputStream fin = null;
        try {
            File file = new File(path);
            fin = new FileInputStream(file);
            bytes = BytesUtil.readBytesFromStream(fin);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return bytes;
    }

    //USE ONLY FOR TESTING
    public synchronized static boolean _setOldData(Context context, byte[] data, String alias, String alias_file, String alias_iv,
                                                   int request_code, boolean auth_required) throws UserNotAuthenticatedException {
//        Log.e(TAG, "_setData: " + alias);
        try {
            validateSet(data, alias, alias_file, alias_iv, auth_required);
        } catch (Exception e) {
            Log.e(TAG, "_setData: ", e);
        }


        KeyStore keyStore = null;
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
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
                        .setRandomizedEncryptionRequired(true)
                        .setEncryptionPaddings(PADDING)
                        .build());
                SecretKey key = keyGenerator.generateKey();

            }

            String encryptedDataFilePath = getFilePath(alias_file, context);

            SecretKey secret = (SecretKey) keyStore.getKey(alias, null);
            if (secret == null) {
                Log.e(TAG, "_setOldData: " + "secret is null on _setData: " + alias);
                return false;
            }
            Cipher inCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            inCipher.init(Cipher.ENCRYPT_MODE, secret);
            byte[] iv = inCipher.getIV();
            String path = getFilePath(alias_iv, context);
            boolean success = writeBytesToFile(path, iv);
            if (!success) {
                Log.e(TAG, "_setOldData: " + "failed to writeBytesToFile: " + alias);
                BRDialog.showCustomDialog(context, context.getString(R.string.Alert_keystore_title_android), "Failed to save the iv file for: " + alias, "close", null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, null, 0);
                keyStore.deleteEntry(alias);
                return false;
            }
            CipherOutputStream cipherOutputStream = null;
            try {
                cipherOutputStream = new CipherOutputStream(
                        new FileOutputStream(encryptedDataFilePath), inCipher);
                cipherOutputStream.write(data);

            } catch (Exception ex) {
                ex.printStackTrace();
            } finally {
                if (cipherOutputStream != null) cipherOutputStream.close();
            }
            return true;
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "_setOldData: showAuthenticationScreen: " + alias);
            showAuthenticationScreen(context, request_code, alias);
            throw e;
        } catch (Exception e) {
            Log.e(TAG, "_setOldData: ", e);
            return false;
        }
    }

    //USE ONLY FOR TESTING
    public synchronized static byte[] _getOldData(final Context context, String alias, String alias_file, String alias_iv, int request_code)
            throws UserNotAuthenticatedException {
//        Log.e(TAG, "_getData: " + alias);

        try {
            validateGet(alias, alias_file, alias_iv);
        } catch (Exception e) {
            Log.e(TAG, "_getOldData: ", e);
        }

        KeyStore keyStore = null;

        String encryptedDataFilePath = getFilePath(alias_file, context);
//        byte[] result = new byte[0];
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
                Log.e(TAG, "_getOldData: " + "file is present but the key is gone: " + alias);
                return null;
            }

            boolean ivExists = new File(getFilePath(alias_iv, context)).exists();
            boolean aliasExists = new File(getFilePath(alias_file, context)).exists();
            if (!ivExists || !aliasExists) {
                removeAliasAndDatas(keyStore, alias, context);
                //report it if one exists and not the other.
                if (ivExists != aliasExists) {
                    Log.e(TAG, "_getOldData: " + "alias or iv isn't on the disk: " + alias + ", aliasExists:" + aliasExists);
                    return null;
                } else {
                    Log.e(TAG, "_getOldData: " + "!ivExists && !aliasExists: " + alias);
                    return null;
                }
            }

            byte[] iv = readBytesFromFile(getFilePath(alias_iv, context));
            if (Utils.isNullOrEmpty(iv))
                throw new NullPointerException("iv is missing for " + alias);
            Cipher outCipher;
            outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            return BytesUtil.readBytesFromStream(cipherInputStream);
        } catch (InvalidKeyException e) {
            if (e instanceof UserNotAuthenticatedException) {
                /** user not authenticated, ask the system for authentication */
                Log.e(TAG, "_getOldData: showAuthenticationScreen: " + alias);
                showAuthenticationScreen(context, request_code, alias);
                throw (UserNotAuthenticatedException) e;
            } else {
                Log.e(TAG, "_getOldData: InvalidKeyException", e);
                return null;
            }
        } catch (IOException | CertificateException | KeyStoreException e) {
            /** keyStore.load(null) threw the Exception, meaning the keystore is unavailable */
            Log.e(TAG, "_getOldData: keyStore.load(null) threw the Exception, meaning the keystore is unavailable", e);
            return null;

        } catch (UnrecoverableKeyException | NoSuchAlgorithmException | NoSuchPaddingException | InvalidAlgorithmParameterException e) {
            /** if for any other reason the keystore fails, crash! */
            Log.e(TAG, "getData: error: " + e.getClass().getSuperclass().getName());
            return null;
        }
    }

    private static void showLoopBugMessage(final Context app) {
        if (bugMessageShowing) return;
        bugMessageShowing = true;
        Log.e(TAG, "showLoopBugMessage: ");
        String mess = app.getString(R.string.ErrorMessages_loopingLockScreen_android);

        SpannableString ss = new SpannableString(mess.replace("[", "").replace("]", ""));
        ClickableSpan clickableSpan = new ClickableSpan() {
            @Override
            public void onClick(View textView) {
                Log.e(TAG, "onClick: clicked on span!");
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        BRDialog.hideDialog();
                        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
                        UiUtils.showSupportFragment((FragmentActivity) app, BRConstants.FAQ_LOOP_BUG, wm);
                    }
                });

            }

            @Override
            public void updateDrawState(TextPaint ds) {
                super.updateDrawState(ds);
                ds.setUnderlineText(false);
            }
        };
        ss.setSpan(clickableSpan, mess.indexOf("[") - 1, mess.indexOf("]") - 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title), ss, app.getString(R.string.AccessibilityLabels_close), null,
                new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        if (app instanceof Activity) ((Activity) app).finish();
                    }
                }, null, new DialogInterface.OnDismissListener() {
                    @Override
                    public void onDismiss(DialogInterface dialog) {
                        bugMessageShowing = false;
                    }
                }, 0);

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
