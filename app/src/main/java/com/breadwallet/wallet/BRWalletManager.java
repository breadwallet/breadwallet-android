package com.breadwallet.wallet;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.security.KeyPairGeneratorSpec;
import android.util.Log;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.nio.ByteBuffer;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.KeyStore;
import java.security.KeyStoreException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.UnrecoverableEntryException;
import java.security.cert.CertificateException;
import java.security.interfaces.RSAPrivateKey;
import java.security.interfaces.RSAPublicKey;
import java.text.NumberFormat;
import java.util.Calendar;
import java.util.List;

import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.NoSuchPaddingException;
import javax.security.auth.x500.X500Principal;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/22/15.
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

public class BRWalletManager {
    public static final String TAG = BRWalletManager.class.getName();
    private static BRWalletManager instance;

    public static final long SATOSHIS = 100000000;
    public static final long MAX_MONEY = 21000000 * SATOSHIS;
    public static final long DEFAULT_FEE_PER_KB = 4096 * 1000 / 225; // fee required by eligius pool, which supports child-pays-for-parent
    public static final long MAX_FEE_PER_KB = 100100 * 1000 / 225; // slightly higher than a 1000bit fee on a typical 225byte transaction
    public static final String UNSPENT_URL_1 = "https://api.chain.com/v2/"; // + a string
    public static final String UNSPENT_URL_2 = "/addresses/"; // + a string
    public static final String UNSPENT_URL_3 = "/unspents?api-key-id=eed0d7697a880144bb854676f88d123f";
    public static final String TICKER_URL = "https://bitpay.com/rates";
    public static final String FEE_PER_KB_URL = "https://api.breadwallet.com/v1/fee-per-kb";
    public static final int SEED_ENTROPY_LENGTH = 128 / 8;
    public static final String SEC_ATTR_SERVICE = "org.voisine.breadwallet";
    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";
    public static final String ALIAS = "phrase";

    ByteBuffer masterPublicKey; // master public key used to generate wallet addresses
    char[] seedPhrase;          // requesting seedPhrase will trigger authentication
    long seedCreationTime;      // interval since reference date, 00:00:00 01/01/01 GMT
    long secureTime;            // last known time from an ssl server connection
    long spendingLimit;         // amount that can be spent using touch id without pin entry
    boolean passcodeEnabled;    // true if device passcode is enabled
    boolean didAuthenticate;    // true if the user authenticated after this was last set to false
    NumberFormat format;        // bitcoin currency formatter
    NumberFormat localFormat;   // local currency formatter
    String localCurrencyCode;   // local currency ISO code
    double localCurrencyPrice;  // exchange rate in local currency units per bitcoin
    List<String> currencyCodes; // list of supported local currency codes
    List<String> currencyNames; // names for local currency codes

    private BRWalletManager() {
        /**
         * initialize the class
         */
    }

    public static synchronized BRWalletManager getInstance() {
        if (instance == null) {
            instance = new BRWalletManager();
        }
        return instance;
    }

    public BRWallet wallet() {
        return null;
    }

    public static boolean setKeychainData(ByteBuffer buffer, String key, boolean authenticated) {

        return true;
    }

    public ByteBuffer getKeychainData(String key) {
        return null;
    }

    public static boolean setKeychainInt(long i, String key, boolean authenticated) {
        return false;
    }

    public long getKeychainInt(String key) {
        return 0;
    }

    public boolean setKeyStoreString(String strPhrase, String key,
                                     boolean authenticated, Context ctx) {
        try {
            KeyStore keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            int nBefore = keyStore.size();

            // Create the keys if necessary
            if (!keyStore.containsAlias(ALIAS)) {

                Calendar notBefore = Calendar.getInstance();
                Calendar notAfter = Calendar.getInstance();
                notAfter.add(Calendar.YEAR, 1);

                KeyPairGeneratorSpec spec = new KeyPairGeneratorSpec.Builder(ctx)
                        .setAlias(ALIAS)
                        .setKeyType("RSA")
                        .setKeySize(2048)
                        .setSubject(new X500Principal("CN=test"))
                        .setSerialNumber(BigInteger.ONE)
                        .setStartDate(notBefore.getTime())
                        .setEndDate(notAfter.getTime())
                        .build();
                KeyPairGenerator generator = KeyPairGenerator.getInstance("RSA", ANDROID_KEY_STORE);
                generator.initialize(spec);

                KeyPair keyPair = generator.generateKeyPair();
            }
            int nAfter = keyStore.size();
            Log.v(TAG, "Before = " + nBefore + " After = " + nAfter);

            // Retrieve the keys
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry) keyStore.getEntry(ALIAS, null);
            RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();
            RSAPublicKey publicKey = (RSAPublicKey) privateKeyEntry.getCertificate().getPublicKey();

            Log.v(TAG, "private key = " + privateKey.toString());
            Log.v(TAG, "public key = " + publicKey.toString());

            // Encrypt the text
            String dataDirectory = ctx.getApplicationInfo().dataDir;
            String filesDirectory = ctx.getFilesDir().getAbsolutePath();
            String encryptedDataFilePath = filesDirectory + File.separator + "my_phrase";

            Log.v(TAG, "strPhrase = " + strPhrase);
            Log.v(TAG, "dataDirectory = " + dataDirectory);
            Log.v(TAG, "filesDirectory = " + filesDirectory);
            Log.v(TAG, "encryptedDataFilePath = " + encryptedDataFilePath);

            Cipher inCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            inCipher.init(Cipher.ENCRYPT_MODE, publicKey);

            CipherOutputStream cipherOutputStream =
                    new CipherOutputStream(
                            new FileOutputStream(encryptedDataFilePath), inCipher);
            cipherOutputStream.write(strPhrase.getBytes("UTF-8"));
            cipherOutputStream.close();

        } catch (NoSuchAlgorithmException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (NoSuchProviderException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (InvalidAlgorithmParameterException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (KeyStoreException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (CertificateException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (IOException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (UnrecoverableEntryException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (NoSuchPaddingException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (InvalidKeyException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        } catch (UnsupportedOperationException e) {
            Log.e(TAG, Log.getStackTraceString(e));
        }
        return true;
    }

    public String getKeyStoreString(String key, Context ctx) {
        KeyStore keyStore;
        String recoveredSecret = "";
        String filesDirectory = ctx.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + "my_phrase";
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);

            // Retrieve the keys
            KeyStore.PrivateKeyEntry privateKeyEntry = (KeyStore.PrivateKeyEntry)
                    keyStore.getEntry(ALIAS, null);
            RSAPrivateKey privateKey = (RSAPrivateKey) privateKeyEntry.getPrivateKey();

            Cipher outCipher = Cipher.getInstance("RSA/ECB/PKCS1Padding", "AndroidOpenSSL");
            outCipher.init(Cipher.DECRYPT_MODE, privateKey);

            CipherInputStream cipherInputStream = new CipherInputStream(
                    new FileInputStream(encryptedDataFilePath), outCipher);
            byte[] roundTrippedBytes = new byte[1000]; // TODO: dynamically resize as we get more data
            int index = 0;
            int nextByte;
            while ((nextByte = cipherInputStream.read()) != -1) {
                roundTrippedBytes[index] = (byte) nextByte;
                index++;
            }
            recoveredSecret = new String(roundTrippedBytes, 0, index, "UTF-8");
            Log.e(TAG, "round tripped string = " + recoveredSecret);
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (UnrecoverableEntryException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } catch (NoSuchProviderException e) {
            e.printStackTrace();
        } catch (NoSuchPaddingException e) {
            e.printStackTrace();
        } catch (InvalidKeyException e) {
            e.printStackTrace();
        } catch (NullPointerException e) {
            e.printStackTrace();
        }

        Log.e(TAG, "recovered: " + recoveredSecret);
        return recoveredSecret;
    }

    /**
     * generates a random seed, saves to keychain and returns the associated seedPhrase
     */
    public String generateRandomSeed(Context ctx) {
        final SecureRandom sr = new SecureRandom();
        final byte[] keyBytes = new byte[128];
        sr.nextBytes(keyBytes);
        Log.e(TAG, keyBytes.toString());
//        byte[] wordListBytes = new byte[0];
//        try {
//            wordListBytes = WordsReader.getWordListBytes(IntroActivity.app);
//        } catch (IOException e) {
//            e.printStackTrace();
//        }
//        String phrase = encodePhrase(keyBytes, wordListBytes); //not working yet
        String phrase = "short apple trunk riot coyote innocent zebra venture ill lava shop test";
        boolean success = setKeyStoreString(phrase, null, true, ctx);
        Log.e(TAG, "setKeyStoreString was successful: " + success);
        return phrase;
    }

    private native String encodePhrase(byte[] seed, byte[] wordList);

    /**
     * authenticates user and returns seed
     */
    public ByteBuffer seedWithPrompt(String authPrompt, long amount) {
        return null;
    }

    /**
     * authenticates user and returns seedPhrase
     */
    public String seedPhraseWithPrompt(String authPrompt) {
        return null;
    }

    /**
     * prompts the user to set or change wallet pin and returns true if the pin was successfully set
     */
    public boolean setPin() {
        return false;
    }

    /**
     * queries chain.com and calls the completion block with unspent outputs for the given address
     */
    public void utxosForAddress(String address) {
        //?????????????????????????????
    }

    /**
     * given a private key, queries chain.com for unspent outputs and calls the completion block with
     * a signed transaction that will sweep the balance into wallet (doesn't publish the tx)
     */
    public void sweepPrivateKey(String privKey, boolean fee, Context ctx) {
        KeyStore keyStore;
        String filesDirectory = ctx.getFilesDir().getAbsolutePath();
        String encryptedDataFilePath = filesDirectory + File.separator + "my_phrase";
        try {
            keyStore = KeyStore.getInstance(ANDROID_KEY_STORE);
            keyStore.load(null);
            keyStore.deleteEntry(ALIAS);

        } catch (CertificateException e) {
            e.printStackTrace();
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
        } catch (KeyStoreException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public long amountForString(String string) {
        return 0;
    }

    public String stringForAmount(long amount) {
        return null;
    }

    public long amountForLocalCurrencyString(String string) {
        return 0;
    }

    public String localCurrencyStringForAmount(long amount) {
        return null;
    }

    /**
     * true if keychain is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        String phrase = getKeyStoreString(null, ctx);

        return phrase.length() == 0;
    }

    /**
     * master public key used to generate wallet addresses
     */
    public ByteBuffer masterPublicKey() {
        return null;
    }

    /**
     * requesting seedPhrase will trigger authentication
     */
    public String seedPhrase() {
        return null;
    }

    public void setSeedPhrase(char[] seedPhrase) {

    }

    /**
     * interval since refrence date, 00:00:00 01/01/01 GMT
     */
    public long seedCreationTime() {
        return 0;
    }

    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Context ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(Activity.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public void updateFeePerKb() {

    }

}
