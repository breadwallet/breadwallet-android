package com.breadwallet.wallet;

import android.app.Activity;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.MainFragmentQR;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.WordsReader;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.sqlite.SQLiteManager;

import java.io.IOException;
import java.nio.ByteBuffer;
import java.security.SecureRandom;
import java.util.List;

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
    private static final String TAG = BRWalletManager.class.getName();
    private static BRWalletManager instance;
    private static Context ctx;


//    public static final long SATOSHIS = 100000000;
//    public static final long MAX_MONEY = 21000000 * SATOSHIS;
//    public static final long DEFAULT_FEE_PER_KB = 4096 * 1000 / 225; // fee required by eligius pool, which supports child-pays-for-parent
//    public static final long MAX_FEE_PER_KB = 100100 * 1000 / 225; // slightly higher than a 1000bit fee on a typical 225byte transaction
//    public static final String UNSPENT_URL_1 = "https://api.chain.com/v2/"; // + a string
//    public static final String UNSPENT_URL_2 = "/addresses/"; // + a string
//    public static final String UNSPENT_URL_3 = "/unspents?api-key-id=eed0d7697a880144bb854676f88d123f";
//    public static final String TICKER_URL = "https://bitpay.com/rates";
//    public static final String FEE_PER_KB_URL = "https://api.breadwallet.com/v1/fee-per-kb";
//    public static final int SEED_ENTROPY_LENGTH = 128 / 8;
//    public static final String SEC_ATTR_SERVICE = "org.voisine.breadwallet";
//    public static final String ANDROID_KEY_STORE = "AndroidKeyStore";

//    ByteBuffer masterPublicKey; // master public key used to generate wallet addresses
//    byte[] wallet;
//    char[] seedPhrase;          // requesting seedPhrase will trigger authentication
//    long seedCreationTime;      // interval since reference date, 00:00:00 01/01/01 GMT
//    long secureTime;            // last known time from an ssl server connection
//    long spendingLimit;         // amount that can be spent using touch id without pin entry
//    boolean passcodeEnabled;    // true if device passcode is enabled
//    boolean didAuthenticate;    // true if the user authenticated after this was last set to false
//    NumberFormat format;        // bitcoin currency formatter
//    NumberFormat localFormat;   // local currency formatter
//    String localCurrencyCode;   // local currency ISO code
//    double localCurrencyPrice;  // exchange rate in local currency units per bitcoin
//    List<String> currencyCodes; // list of supported local currency codes
//    List<String> currencyNames; // names for local currency codes

    private BRWalletManager() {
    }

    public static BRWalletManager getInstance(Context context) {
        ctx = context;
        if (instance == null) {
            instance = new BRWalletManager();
        }
        return instance;
    }

    public String generateRandomSeed() {

        final SecureRandom sr = new SecureRandom();
        final byte[] keyBytes = new byte[16];
        sr.nextBytes(keyBytes);
        String[] words = new String[0];
        List<String> list;
        try {
            list = WordsReader.getWordList(ctx);
            words = list.toArray(new String[list.size()]);
//            CustomLogger.LogThis(words);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (words.length < 2000)
            throw new IllegalArgumentException("the list is wrong, size: " + words.length);
        byte[] phrase = encodeSeed(keyBytes, words);
        String strPhrase = new String(phrase);
//        String phrase = "short apple trunk riot coyote innocent zebra venture ill lava shop test";
        boolean success = KeyStoreManager.setKeyStoreString(strPhrase, ctx);
        Log.e(TAG, "setKeyStoreString was successful: " + success);
        return success ? strPhrase : null;
    }

    public boolean setKeyStoreString(String strPhrase, String key,
                                     boolean authenticated, Context ctx) {
        return KeyStoreManager.setKeyStoreString(strPhrase, ctx);
    }


    /**
     * given a private key, queries chain.com for unspent outputs and calls the completion block with
     * a signed transaction that will sweep the balance into wallet (doesn't publish the tx)
     */
    public boolean sweepPrivateKey() {
        return KeyStoreManager.deleteKeyStoreEntry();
    }

    /**
     * true if keychain is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        String pubkey = KeyStoreManager.getMasterPublicKey(ctx);
        Log.e(TAG, "in the noWallet, pubkey.length(): " + pubkey.length());
//        Log.e(TAG, "in the noWallet, pubkey: " + pubkey);
        return pubkey.length() == 0;

    }


    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Context ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(Activity.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public static void refreshAddress() {
        Log.e(TAG, "refreshAddress: " + ctx);
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            MainFragmentQR mainFragmentQR = CustomPagerAdapter.adapter == null? null : CustomPagerAdapter.adapter.mainFragmentQR;
            String tmpAddr = getReceiveAddress();
            SharedPreferences.Editor editor = ctx.getSharedPreferences(MainFragmentQR.RECEIVE_ADDRESS_PREFS, Context.MODE_PRIVATE).edit();
            editor.putString(MainFragmentQR.RECEIVE_ADDRESS, tmpAddr);
            editor.apply();
            if (mainFragmentQR == null) return;
            mainFragmentQR.refreshAddress(tmpAddr);
        } else {
            throw new NullPointerException("Cannot be null");
        }
    }

    /**
     * Wallet callbacks
     */

    public static void onBalanceChanged(final long balance) {

        Log.e(TAG, "in the BRWalletManager - onBalanceChanged:  " + balance);
        if (ctx == null) ctx = MainActivity.app;
        ((Activity)ctx).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CurrencyManager.getInstance(ctx).setBalance(balance);
                //TODO check this when Aaron fixes the bug
                refreshAddress();
                FragmentSettingsAll.refreshTransactions(ctx);
            }
        });

    }

    public static void onTxAdded(byte[] tx, int blockHeight, long timestamp, final long amount) {
//        Log.e(TAG, "amount on the txAdded:" + amount);
        Log.e(TAG, "in the BRWalletManager - onTxAdded: " + tx.length + " " + blockHeight + " " + timestamp);
//        for (byte b : tx) {
//            System.out.println(Integer.toBinaryString(b & 255 | 256).substring(1));
//        }
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            ((Activity)ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CurrencyManager m = CurrencyManager.getInstance(ctx);
                    if (amount > 0) {
                        ((BreadWalletApp) ctx.getApplicationContext()).showCustomToast((Activity) ctx,
                                String.format(ctx.getString(R.string.received), m.getBitsFromSatoshi(amount) + m.bitcoinLowercase),
                                BreadWalletApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, 1);
                    } else {
                        ((BreadWalletApp) ctx.getApplicationContext()).showCustomToast((Activity) ctx,
                                String.format(ctx.getString(R.string.sent), m.getBitsFromSatoshi(amount * -1) + m.bitcoinLowercase),
                                BreadWalletApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, 1);
                    }
                }
            });

        }
        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);
        sqLiteManager.insertTransaction(tx, blockHeight, timestamp);
    }

    public static void onTxUpdated(int blockHeight) {
        //TODO ask Aaron how to update tx
        Log.e(TAG, "in the BRWalletManager - onTxUpdated");
    }

    public static void onTxDeleted() {
        //TODO ask Aaron how to delete tx
//        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);
//        List<BRTransactionEntity> txList = sqLiteManager.getTransactions();


        Log.e(TAG, "in the BRWalletManager - onTxDeleted");
    }

    private native byte[] encodeSeed(byte[] seed, String[] wordList);

    //    public native void createWallet(ByteBuffer transactions[], int transactionCount);
    public native void createWallet(int transactionCount, String pubkey, int r);

    public native void putTransaction(byte[] transaction);

    public native void createTxArrayWithCount(int count);

    public native void setCallbacks(byte[] wallet);

    public native void setPeerManagerCallbacks(byte[] peerManager);

    public native String getMasterPubKey(String normalizedString);

    public native void testWalletCallbacks();

    public native void testTransactionAdding(long amount);

    public static native String getReceiveAddress();

    public native TransactionListItem[] getTransactions();

    public native void pay(String addressHolder, long amountHolder);

    public native void rescan();

    public native boolean validateAddress(String address);

    public native boolean addressContainedInWallet(String address);

    public native long getMinOutputAmount();

}
