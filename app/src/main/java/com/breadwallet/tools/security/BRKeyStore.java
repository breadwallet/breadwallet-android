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
package com.breadwallet.tools.security;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Build;
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
import com.platform.entities.WalletInfoData;
import com.platform.tools.KVStoreManager;

import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.GeneralSecurityException;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.Key;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.UnrecoverableKeyException;
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReentrantLock;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.KeyGenerator;
import javax.crypto.SecretKey;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;

/**
 * This class is responsible for storing sensitive data into the KeyStore.
 */

// TODO Remove logic from setters and getters.
public final class BRKeyStore {
    private BRKeyStore() {
    }

    private static final String TAG = BRKeyStore.class.getName();

    private static final String KEY_STORE_PREFS_NAME = "keyStorePrefs";
    private static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    private static final String MANUFACTURER_GOOGLE = "Google";

    // Old encryption parameters
    private static final String CIPHER_ALGORITHM = "AES/CBC/PKCS7Padding";

    // New encryption parameters
    // Recommended way to encrypt in android.
    private static final String NEW_CIPHER_ALGORITHM = "AES/GCM/NoPadding";
    private static final String NEW_PADDING = KeyProperties.ENCRYPTION_PADDING_NONE;
    private static final String NEW_BLOCK_MODE = KeyProperties.BLOCK_MODE_GCM;

    private static final String START_SPANNABLE_SYMBOL = "[";
    private static final String END_SPANNABLE_SYMBOL = "]";

    // Iv names
    private static final String PHRASE_IV = "ivphrase";
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
    public static final int AUTH_DURATION_SEC = 300;
    private static final int GMC_TAG_LENGTH = 128;

    private static boolean bugMessageShowing;

    public static final Map<String, AliasObject> ALIAS_OBJECT_MAP;

    private static final ReentrantLock LOCK = new ReentrantLock();

    public enum ValidityStatus {
        VALID,
        INVALID_WIPE,
        INVALID_UNINSTALL;
    }

    // Storing all the Keystore data into a map.
    // TODO Wrong/old implementation, needs refactoring.
    // TODO Create generic functions and remove duplicate code.
    static {
        ALIAS_OBJECT_MAP = new HashMap<>();
        ALIAS_OBJECT_MAP.put(PHRASE_ALIAS, new AliasObject(PHRASE_ALIAS, PHRASE_FILENAME, PHRASE_IV));
        ALIAS_OBJECT_MAP.put(PUB_KEY_ALIAS, new AliasObject(PUB_KEY_ALIAS, PUB_KEY_FILENAME, PUB_KEY_IV));
        ALIAS_OBJECT_MAP.put(WALLET_CREATION_TIME_ALIAS, new AliasObject(WALLET_CREATION_TIME_ALIAS, WALLET_CREATION_TIME_FILENAME, WALLET_CREATION_TIME_IV));
        ALIAS_OBJECT_MAP.put(PASS_CODE_ALIAS, new AliasObject(PASS_CODE_ALIAS, PASS_CODE_FILENAME, PASS_CODE_IV));
        ALIAS_OBJECT_MAP.put(FAIL_COUNT_ALIAS, new AliasObject(FAIL_COUNT_ALIAS, FAIL_COUNT_FILENAME, FAIL_COUNT_IV));
        ALIAS_OBJECT_MAP.put(SPEND_LIMIT_ALIAS, new AliasObject(SPEND_LIMIT_ALIAS, SPEND_LIMIT_FILENAME, SPENT_LIMIT_IV));
        ALIAS_OBJECT_MAP.put(FAIL_TIMESTAMP_ALIAS, new AliasObject(FAIL_TIMESTAMP_ALIAS, FAIL_TIMESTAMP_FILENAME, FAIL_TIMESTAMP_IV));
        ALIAS_OBJECT_MAP.put(AUTH_KEY_ALIAS, new AliasObject(AUTH_KEY_ALIAS, AUTH_KEY_FILENAME, AUTH_KEY_IV));
        ALIAS_OBJECT_MAP.put(TOKEN_ALIAS, new AliasObject(TOKEN_ALIAS, TOKEN_FILENAME, TOKEN_IV));
        ALIAS_OBJECT_MAP.put(PASS_TIME_ALIAS, new AliasObject(PASS_TIME_ALIAS, PASS_TIME_FILENAME, PASS_TIME_IV));
        ALIAS_OBJECT_MAP.put(TOTAL_LIMIT_ALIAS, new AliasObject(TOTAL_LIMIT_ALIAS, TOTAL_LIMIT_FILENAME, TOTAL_LIMIT_IV));
        ALIAS_OBJECT_MAP.put(ETH_PUBKEY_ALIAS, new AliasObject(ETH_PUBKEY_ALIAS, ETH_PUBKEY_FILENAME, ETH_PUBKEY_IV));
    }

    /**
     * Returns a value based on if Android key store is valid. We test if the paper key encryption key can no longer be
     * used because it has been permanently invalidated which indicates if the Android key store is invalidated. If the
     * Android key store has been invalidated, our entire app data should be wiped and the app should be
     * re-initialized.  We cannot recover from this otherwise.
     *
     * We found on Google devices a wipe is sufficient to recover except on Android 8.1 which requires an uninstall.
     * On non-Google devices a wipe is required on Android 6-7.1 and an uninstall is required on Android 8+.
     *
     * See {@link KeyPermanentlyInvalidatedException} for further details.
     *
     * @return A {@link ValidityStatus} based on the current status of the Android key store.
     */
    public static ValidityStatus getValidityStatus() {
        try {
            // Attempt to retrieve the key that protects the paper key and initialize an encryption cipher.
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            Key key = keyStore.getKey(PHRASE_ALIAS, null);

            // If there is no key, then it has not been initialized yet. The key store is still considered valid.
            if (key != null) {
                Cipher cipher = Cipher.getInstance(NEW_CIPHER_ALGORITHM);
                cipher.init(Cipher.ENCRYPT_MODE, key);
            }
        } catch (KeyPermanentlyInvalidatedException | UnrecoverableKeyException e) {
            // If KeyPermanentlyInvalidatedException
            //  -> with no cause happens, then the password was disabled. See DROID-1019.
            // If UnrecoverableKeyException
            //  -> with cause "Key blob corrupted" happens then the password was disabled & re-enabled. See DROID-1207.
            //  -> with cause "Key blob corrupted" happens then after DROID-1019 the app was opened again while password is on.
            //  -> with cause "Key not found" happens then after DROID-1019 the app was opened again while password is off.
            //  -> with cause "System error" (KeyStoreException) after app wipe on devices that need uninstall to recover.
            // Note: These exceptions would happen before a UserNotAuthenticatedException, so we don't need to handle that.

            boolean isGoogleDevice = MANUFACTURER_GOOGLE.equals(Build.MANUFACTURER);
            if ((isGoogleDevice && android.os.Build.VERSION.SDK_INT == android.os.Build.VERSION_CODES.O_MR1)
                    || (!isGoogleDevice && android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O)) {
                Log.e(TAG, "The key store has been invalidated. Uninstall required. Manufacturer: "
                        + Build.MANUFACTURER + "OS Version: " + Build.VERSION.RELEASE, e);
                return ValidityStatus.INVALID_UNINSTALL;
            } else {
                Log.e(TAG, "The key store has been invalidated. Wipe required. Manufacturer: "
                        + Build.MANUFACTURER + "OS Version: " + Build.VERSION.RELEASE, e);
                return ValidityStatus.INVALID_WIPE;
            }
        } catch (GeneralSecurityException | IOException e) {
            // We can safely ignore these exceptions, because we are only concerned with
            // KeyPermanentlyInvalidatedException here.
            Log.e(TAG, "Error while checking if key store is still valid. Ignoring. ", e);
        }

        return ValidityStatus.VALID;
    }

    /**
     * Sets the data into the device's keystore.
     *
     * @param context      The context.
     * @param data         The byte[] to be stored into the keystore.
     * @param alias        The mAlias name (key).
     * @param aliasFile    NOT USED anymore, was used in the old implementation.
     * @param aliasIv      The iv mAlias (key) to be used to store the iv to SharedPreferences.
     * @param requestCode  The request code to be used for createConfirmDeviceCredentialIntent for
     *                     keys that need user authentication
     * @param authRequired Authentication needed if true.
     * @return true if setData was successful.
     * @throws UserNotAuthenticatedException If the wallet was not authenticated in the last AUTH_DURATION_SEC seconds.
     */
    private static synchronized boolean setData(Context context, byte[] data, String alias, String aliasFile, String aliasIv,
                                                int requestCode, boolean authRequired) throws UserNotAuthenticatedException {
        // Validates if the parameters combination parameters are valid (deprecated)
        validateSet(data, alias, aliasFile, aliasIv, authRequired);

        KeyStore keyStore;
        try {
            LOCK.lock();
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            // Load the keystore.
            keyStore.load(null);
            // Get the existing key for mAlias.
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, null);
            // Get the cipher algorithm.
            Cipher inCipher = Cipher.getInstance(NEW_CIPHER_ALGORITHM);

            if (secretKey == null) {
                // Create key if not present.
                secretKey = createKeys(alias, authRequired);
                // Initialize the cipher.
                inCipher.init(Cipher.ENCRYPT_MODE, secretKey);
            } else {
                // See if the key is old format, create a new one if it is.
                // Since we used from Old encryption parameters -> New parameters. See top of the class.
                try {
                    inCipher.init(Cipher.ENCRYPT_MODE, secretKey);
                } catch (InvalidKeyException e) {
                    if (e instanceof UserNotAuthenticatedException) {
                        throw e;
                    }
                    Log.e(TAG, "setData: OLD KEY PRESENT: " + alias, e);
                    // Old key format is present.
                    // Create new key and reinitialize the cipher.
                    secretKey = createKeys(alias, authRequired);
                    inCipher.init(Cipher.ENCRYPT_MODE, secretKey);
                }
            }

            // At this point the key cannot still be null.
            if (secretKey == null) {
                BRKeystoreErrorException ex = new BRKeystoreErrorException("secret is null on setData: " + alias);
                BRReportsManager.reportBug(ex);
                return false;
            }

            byte[] iv = inCipher.getIV();
            if (iv == null) {
                throw new NullPointerException("iv is null!");
            }

            // Store the iv in SharedPreferences to use for decryption.
            storeEncryptedData(context, iv, aliasIv);
            // Encrypt data.
            byte[] encryptedData = inCipher.doFinal(data);
            // Store the encrypted data in SharedPreferences.
            storeEncryptedData(context, encryptedData, alias);
            return true;
        } catch (UserNotAuthenticatedException e) {
            // The user needs to authenticate before proceeding.
            Log.e(TAG, "setData: showAuthenticationScreen: " + alias);
            showAuthenticationScreen(context, requestCode, alias);
            throw e;
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "setData: Error setting: " + alias, e);
            BRReportsManager.reportBug(e);
            return false;
        } finally {
            LOCK.unlock();
        }
    }

    /**
     * Creates a new key for encrypting the specified alias.
     * @param alias        The mAlias to be used (key name).
     * @param authRequired Needs user authentication to retrieve if true.
     * @return The newly created key.
     * @throws InvalidAlgorithmParameterException Invalid algorithm exception.
     * @throws NoSuchProviderException            Padding exception.
     * @throws NoSuchAlgorithmException           Algorithm exception.
     */
    private static SecretKey createKeys(String alias, boolean authRequired)
            throws InvalidAlgorithmParameterException, NoSuchProviderException, NoSuchAlgorithmException {
        KeyGenerator keyGenerator = KeyGenerator.getInstance(KeyProperties.KEY_ALGORITHM_AES, ANDROID_KEY_STORE);

        keyGenerator.init(new KeyGenParameterSpec.Builder(alias,
                KeyProperties.PURPOSE_ENCRYPT | KeyProperties.PURPOSE_DECRYPT)
                .setBlockModes(NEW_BLOCK_MODE)
                .setUserAuthenticationRequired(authRequired)
                .setUserAuthenticationValidityDurationSeconds(AUTH_DURATION_SEC)
                .setRandomizedEncryptionRequired(false)
                .setEncryptionPaddings(NEW_PADDING)
                .build());
        return keyGenerator.generateKey();

    }

    /**
     * Gets the data from the device's keystore.
     *
     * @param context     The context.
     * @param alias       The mAlias name (key).
     * @param aliasFile   NOT USED anymore, was used in the old implementation.
     * @param aliasIv     The iv mAlias (key) to be used to store the iv to SharedPreferences.
     * @param requestCode The request code to be used for createConfirmDeviceCredentialIntent for
     *                    keys that need user authentication
     * @return true if setData was successful.
     * @throws UserNotAuthenticatedException If the wallet was not authenticated in the last AUTH_DURATION_SEC seconds.
     */
    private static byte[] getData(final Context context, String alias, String aliasFile, String aliasIv, int requestCode)
            throws UserNotAuthenticatedException {
        // validate entries
        validateGet(alias, aliasFile, aliasIv);
        KeyStore keyStore;
        try {
            LOCK.lock();
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            SecretKey secretKey = (SecretKey) keyStore.getKey(alias, null);

            byte[] encryptedData = retrieveEncryptedData(context, alias);
            if (encryptedData != null) {
                // New format data is present, the old one was stored on file.
                // Get the iv we stored when created the key.
                byte[] iv = retrieveEncryptedData(context, aliasIv);
                if (iv == null) {
                    if (alias.equalsIgnoreCase(PHRASE_ALIAS)) {
                        throw new RuntimeException("iv is missing when data isn't: "
                                + alias + " (Can't proceed, risking user's phrase! )"); //crash here!
                    } else {
                        BRReportsManager.reportBug(new NullPointerException("iv is missing when data isn't: " + alias));
                    }
                    return null;
                }
                Cipher outCipher;

                outCipher = Cipher.getInstance(NEW_CIPHER_ALGORITHM);
                outCipher.init(Cipher.DECRYPT_MODE, secretKey, new GCMParameterSpec(GMC_TAG_LENGTH, iv));
                try {
                    byte[] decryptedData = outCipher.doFinal(encryptedData);
                    if (decryptedData != null) {
                        return decryptedData;
                    }
                } catch (IllegalBlockSizeException | BadPaddingException e) {
                    Log.e(TAG, "Failed to decrypt the following data: " + alias, e);
                    BRReportsManager.reportBug(e);
                    return null;
                }
            }
            // No new format data, get the old one and migrate it to the new format.
            String encryptedDataFilePath = getFilePath(aliasFile, context);

            if (secretKey == null) {
                // No such key.
                boolean fileExists = new File(encryptedDataFilePath).exists();
                if (!fileExists) {
                    // File also not there, fine then.
                    return null;
                }
                // If there's a key but no file with encrypted data, that is wrong.
                BRKeystoreErrorException ex = new BRKeystoreErrorException("file is present but the key is gone: " + alias);
                BRReportsManager.reportBug(ex);
                return null;
            }

            boolean ivExists = new File(getFilePath(aliasIv, context)).exists();
            boolean aliasExists = new File(getFilePath(aliasFile, context)).exists();
            // Cannot happen, either both or neither.
            if (!ivExists || !aliasExists) {
                removeAliasAndDatas(keyStore, alias, context);
                // Report it if one exists and not the other.
                if (ivExists != aliasExists) {
                    BRKeystoreErrorException ex = new BRKeystoreErrorException("mAlias or iv isn't on the disk: "
                            + alias + ", aliasExists:" + aliasExists);
                    BRReportsManager.reportBug(ex);
                } else {
                    BRKeystoreErrorException ex = new BRKeystoreErrorException("!ivExists && !aliasExists: " + alias);
                    BRReportsManager.reportBug(ex);
                }
                return null;
            }

            // Get the iv.
            byte[] iv = readBytesFromFile(getFilePath(aliasIv, context));
            if (Utils.isNullOrEmpty(iv)) {
                throw new RuntimeException("iv is missing for " + alias);
            }
            Cipher outCipher = Cipher.getInstance(CIPHER_ALGORITHM);
            outCipher.init(Cipher.DECRYPT_MODE, secretKey, new IvParameterSpec(iv));
            CipherInputStream cipherInputStream = new CipherInputStream(new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] result = BytesUtil.readBytesFromStream(cipherInputStream);
            if (result == null) {
                throw new RuntimeException("Failed to read bytes from CipherInputStream for mAlias " + alias);
            }

            // Create the new format key.
            SecretKey newKey = createKeys(alias, (alias.equals(PHRASE_ALIAS)));
            if (newKey == null) {
                throw new RuntimeException("Failed to create new key for mAlias " + alias);
            }
            Cipher inCipher = Cipher.getInstance(NEW_CIPHER_ALGORITHM);
            // Initialize the cipher.
            inCipher.init(Cipher.ENCRYPT_MODE, newKey);
            iv = inCipher.getIV();
            // Store the new iv.
            storeEncryptedData(context, iv, aliasIv);
            // Encrypt the data.
            encryptedData = inCipher.doFinal(result);
            // Store the new data.
            storeEncryptedData(context, encryptedData, alias);
            return result;
        } catch (UserNotAuthenticatedException e) {
            // User not authenticated, ask the system for authentication.
            Log.e(TAG, "getData: showAuthenticationScreen: " + alias);
            showAuthenticationScreen(context, requestCode, alias);
            throw e;
        } catch (GeneralSecurityException | IOException e) {
            Log.e(TAG, "getData: Error retrieving: " + alias, e);
            BRReportsManager.reportBug(e);
            throw new IllegalStateException(e);
        } finally {
            LOCK.unlock();
        }
    }

    // TODO: Investigate if this validation is necessary, else remove.
    private static void validateGet(String alias, String aliasFile, String aliasIv) throws IllegalArgumentException {
        AliasObject obj = ALIAS_OBJECT_MAP.get(alias);
        if (obj != null && (!obj.mAlias.equals(alias) || !obj.mDatafileName.equals(aliasFile) || !obj.mIvFileName.equals(aliasIv))) {
            String err = alias + "|" + aliasFile + "|" + aliasIv + ", obj: " + obj.mAlias + "|" + obj.mDatafileName + "|" + obj.mIvFileName;
            throw new IllegalArgumentException("keystore insert inconsistency in names: " + err);
        }

    }

    // TODO: Investigate if this validation is necessary, else remove.
    private static void validateSet(byte[] data, String alias, String aliasFile, String aliasIv, boolean authRequired)
            throws IllegalArgumentException {
        if (data == null) {
            throw new IllegalArgumentException("keystore insert data is null");
        }
        AliasObject obj = ALIAS_OBJECT_MAP.get(alias);
        if (obj != null && (!obj.mAlias.equals(alias) || !obj.mDatafileName.equals(aliasFile) || !obj.mIvFileName.equals(aliasIv))) {
            String err = alias + "|" + aliasFile + "|" + aliasIv + ", obj: " + obj.mAlias + "|" + obj.mDatafileName + "|" + obj.mIvFileName;
            throw new IllegalArgumentException("keystore insert inconsistency in names: " + err);
        }

        if (authRequired) {
            if (!alias.equals(PHRASE_ALIAS)) {
                throw new IllegalArgumentException("keystore auth_required is true but mAlias is: " + alias);
            }
        }
    }

    /**
     * Get the full path of a file by name.
     *
     * @param fileName The name of the file.
     * @param context  The context.
     * @return The full path.
     */
    // TODO: This should be in a public util class.
    private static String getFilePath(String fileName, Context context) {
        String filesDirectory = context.getFilesDir().getAbsolutePath();
        return filesDirectory + File.separator + fileName;
    }

    public static boolean putPhrase(byte[] strToStore, Context context, int requestCode) throws UserNotAuthenticatedException {
        if (PostAuth.mAuthLoopBugHappened) {
            showLoopBugMessage(context);
            throw new UserNotAuthenticatedException();
        }
        AliasObject obj = ALIAS_OBJECT_MAP.get(PHRASE_ALIAS);
        return !(strToStore == null || strToStore.length == 0) && setData(context, strToStore, obj.mAlias, obj.mDatafileName, obj.mIvFileName, requestCode, true);
    }

    public static byte[] getPhrase(final Context context, int requestCode) throws UserNotAuthenticatedException {
        if (PostAuth.mAuthLoopBugHappened) {
            showLoopBugMessage(context);
            throw new UserNotAuthenticatedException();
        }
        AliasObject obj = ALIAS_OBJECT_MAP.get(PHRASE_ALIAS);
        return getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, requestCode);
    }

    public static boolean putMasterPublicKey(byte[] masterPubKey, Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(PUB_KEY_ALIAS);
        try {
            return masterPubKey != null && masterPubKey.length != 0
                    && setData(context, masterPubKey, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getMasterPublicKey(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(PUB_KEY_ALIAS);
        try {
            return getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean putEthPublicKey(byte[] masterPubKey, Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(ETH_PUBKEY_ALIAS);
        try {
            return masterPubKey != null && masterPubKey.length != 0 && setData(context, masterPubKey, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getEthPublicKey(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(ETH_PUBKEY_ALIAS);
        try {
            return getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean putAuthKey(byte[] authKey, Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(AUTH_KEY_ALIAS);
        try {
            return authKey != null && authKey.length != 0 && setData(context, authKey, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getAuthKey(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(AUTH_KEY_ALIAS);
        try {
            return getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean putToken(byte[] token, Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(TOKEN_ALIAS);
        try {
            return token != null && token.length != 0 && setData(context, token, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static byte[] getToken(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(TOKEN_ALIAS);
        try {
            return getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    public static boolean putWalletCreationTime(int creationTime, Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(WALLET_CREATION_TIME_ALIAS);
        byte[] bytesToStore = TypesConverter.intToBytes(creationTime);
        try {
            return bytesToStore.length != 0 && setData(context, bytesToStore, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getWalletCreationTime(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(WALLET_CREATION_TIME_ALIAS);
        byte[] result = null;
        try {
            result = getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        if (Utils.isNullOrEmpty(result)) {
            //if none, try getting from KVStore
            WalletInfoData info = KVStoreManager.getWalletInfo(context);
            if (info != null) {
                int creationDate = info.creationDate;
                putWalletCreationTime(creationDate, context);
                return creationDate;
            } else {
                return 0;
            }
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
        AliasObject obj = ALIAS_OBJECT_MAP.get(PASS_CODE_ALIAS);
        byte[] bytesToStore = pinCode.getBytes();
        try {
            return setData(context, bytesToStore, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static String getPinCode(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(PASS_CODE_ALIAS);
        byte[] result = null;
        try {
            result = getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
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
        AliasObject obj = ALIAS_OBJECT_MAP.get(FAIL_COUNT_ALIAS);
        if (failCount >= 3) {
            long time = BRSharedPrefs.getSecureTime(context);
            putFailTimeStamp(time, context);
        }
        byte[] bytesToStore = TypesConverter.intToBytes(failCount);
        try {
            return bytesToStore.length != 0 && setData(context, bytesToStore, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static int getFailCount(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(FAIL_COUNT_ALIAS);
        byte[] result = null;
        try {
            result = getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return result != null && result.length > 0 ? TypesConverter.bytesToInt(result) : 0;
    }

    public static boolean putSpendLimit(Context context, BigDecimal spendLimit, String iso) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(SPEND_LIMIT_ALIAS);
        byte[] bytesToStore = spendLimit.toPlainString().getBytes();
        try {
            return bytesToStore.length != 0 && setData(context, bytesToStore, obj.mAlias + iso, obj.mDatafileName + iso, obj.mIvFileName + iso, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static BigDecimal getSpendLimit(final Context context, String iso) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(SPEND_LIMIT_ALIAS);
        byte[] result = null;
        try {
            result = getData(context, obj.mAlias + iso, obj.mDatafileName + iso, obj.mIvFileName + iso, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        BaseWalletManager wm = WalletsMaster.getInstance().getWalletByIso(context, iso);
        WalletSettingsConfiguration configs = wm.getSettingsConfiguration();
        return (result != null && result.length > 0)
                ? new BigDecimal(new String(result))
                : (configs.getFingerprintLimits().size() != 0 ? configs.getFingerprintLimits().get(1) : BigDecimal.ZERO);
    }

    public static boolean putTotalLimit(Context context, BigDecimal totalLimit, String iso) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(TOTAL_LIMIT_ALIAS);
        byte[] bytesToStore = totalLimit.toPlainString().getBytes();
        try {
            return bytesToStore.length != 0 && setData(context, bytesToStore, obj.mAlias + iso, obj.mDatafileName + iso, obj.mIvFileName + iso, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static BigDecimal getTotalLimit(final Context context, String iso) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(TOTAL_LIMIT_ALIAS);
        byte[] result = new byte[0];
        try {
            result = getData(context, obj.mAlias + iso, obj.mDatafileName + iso, obj.mIvFileName + iso, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return (result != null && result.length > 0) ? new BigDecimal(new String(result)) : BigDecimal.ZERO;
    }

    public static boolean putFailTimeStamp(long spendLimit, Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(FAIL_TIMESTAMP_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(spendLimit);
        try {
            return bytesToStore.length != 0 && setData(context, bytesToStore, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static long getFailTimeStamp(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(FAIL_TIMESTAMP_ALIAS);
        byte[] result = null;
        try {
            result = getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return result != null && result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static boolean putLastPinUsedTime(long time, Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(PASS_TIME_ALIAS);
        byte[] bytesToStore = TypesConverter.long2byteArray(time);
        try {
            return bytesToStore.length != 0 && setData(context, bytesToStore, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0, false);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return false;
    }

    public static long getLastPinUsedTime(final Context context) {
        AliasObject obj = ALIAS_OBJECT_MAP.get(PASS_TIME_ALIAS);
        byte[] result = null;
        try {
            result = getData(context, obj.mAlias, obj.mDatafileName, obj.mIvFileName, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return result != null && result.length > 0 ? TypesConverter.byteArray2long(result) : 0;
    }

    public static synchronized boolean resetWalletKeyStore(Context context) {
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

        } catch (NoSuchAlgorithmException | KeyStoreException | IOException e) {
            e.printStackTrace();
            return false;
        } catch (java.security.cert.CertificateException e) {
            e.printStackTrace();
        }
        return true;
    }

    private static synchronized void removeAliasAndDatas(KeyStore keyStore, String alias, Context context) {
        if (!Utils.isNullOrEmpty(alias)) {
            try {
                keyStore.deleteEntry(alias);
                AliasObject iv = ALIAS_OBJECT_MAP.get(alias);
                if (iv != null) {
                    BRKeyStore.destroyEncryptedData(context, alias);
                    BRKeyStore.destroyEncryptedData(context, iv.mIvFileName);
                }

            } catch (KeyStoreException e) {
                e.printStackTrace();
            }
        }

    }

    public static void storeEncryptedData(Context ctx, byte[] data, String name) {
        SharedPreferences pref = ctx.getSharedPreferences(KEY_STORE_PREFS_NAME, Context.MODE_PRIVATE);
        String base64 = Base64.encodeToString(data, Base64.DEFAULT);
        SharedPreferences.Editor edit = pref.edit();
        edit.putString(name, base64);
        edit.apply();
    }

    private static void destroyEncryptedData(Context ctx, String name) {
        SharedPreferences pref = ctx.getSharedPreferences(KEY_STORE_PREFS_NAME, Context.MODE_PRIVATE);
        SharedPreferences.Editor edit = pref.edit();
        edit.remove(name);
        edit.apply();
    }

    public static byte[] retrieveEncryptedData(Context ctx, String name) {
        SharedPreferences pref = ctx.getSharedPreferences(KEY_STORE_PREFS_NAME, Context.MODE_PRIVATE);
        String base64 = pref.getString(name, null);
        return base64 == null ? null : Base64.decode(base64, Base64.DEFAULT);
    }

    private static synchronized void showAuthenticationScreen(Context context, int requestCode, String alias) {
        // Create the Confirm Credentials screen. You can customize the title and description. Or
        // we will provide a generic one for you if you leave it null
        if (!alias.equalsIgnoreCase(PHRASE_ALIAS)) {
            BRReportsManager.reportBug(new IllegalArgumentException("requesting auth for: " + alias), true);
        }
        if (context instanceof Activity) {
            Activity app = (Activity) context;
            KeyguardManager keyguardManager = (KeyguardManager) app.getSystemService(Context.KEYGUARD_SERVICE);
            Intent intent = keyguardManager.createConfirmDeviceCredentialIntent(
                    context.getString(R.string.UnlockScreen_touchIdTitle_android),
                    context.getString(R.string.UnlockScreen_touchIdPrompt_android));
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

    /**
     * Read data from file at path.
     *
     * @param path The path.
     * @return The data.
     */
    private static byte[] readBytesFromFile(String path) {
        byte[] bytes = null;
        FileInputStream fin = null;
        try {
            File file = new File(path);
            fin = new FileInputStream(file);
            bytes = BytesUtil.readBytesFromStream(fin);
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (fin != null) {
                    fin.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        return bytes;
    }

    /**
     * Show a dialog to the user explaining the loop bug.
     *
     * @param context The context
     */
    private static void showLoopBugMessage(final Context context) {
        if (!bugMessageShowing) {
            bugMessageShowing = true;
            Log.e(TAG, "showLoopBugMessage: ");
            String mess = context.getString(R.string.ErrorMessages_loopingLockScreen_android);

            int startIndex = mess.indexOf(START_SPANNABLE_SYMBOL) - 1;
            int endIndex = mess.indexOf(END_SPANNABLE_SYMBOL) - 1;
            SpannableString spannableMessage = new SpannableString(mess.replace(START_SPANNABLE_SYMBOL, "")
                    .replace(END_SPANNABLE_SYMBOL, ""));
            ClickableSpan clickableSpan = new ClickableSpan() {
                @Override
                public void onClick(View textView) {
                    Log.e(TAG, "onClick: clicked on span!");
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            BRDialog.hideDialog();
                            BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(context);
                            UiUtils.showSupportFragment((FragmentActivity) context, BRConstants.FAQ_LOOP_BUG, wm);
                        }
                    });
                }

                @Override
                public void updateDrawState(TextPaint ds) {
                    super.updateDrawState(ds);
                    ds.setUnderlineText(false);
                }
            };
            spannableMessage.setSpan(clickableSpan, startIndex, endIndex, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
            BRDialog.showCustomDialog(context, context.getString(R.string.JailbreakWarnings_title),
                    spannableMessage, context.getString(R.string.AccessibilityLabels_close), null,
                    new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            if (context instanceof Activity) {
                                ((Activity) context).finish();
                            }
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            bugMessageShowing = false;
                        }
                    }, 0);
        }

    }

    public static class AliasObject {
        private String mAlias;
        private String mDatafileName;
        private String mIvFileName;

        AliasObject(String alias, String datafileName, String ivFileName) {
            this.mAlias = alias;
            this.mDatafileName = datafileName;
            this.mIvFileName = ivFileName;
        }

        public String getIvFileName() {
            return mIvFileName;
        }

    }
}
