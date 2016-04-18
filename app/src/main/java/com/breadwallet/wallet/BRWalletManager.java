package com.breadwallet.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroShowPhraseActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.RequestQRActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.MainFragmentQR;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.WordsReader;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.sqlite.SQLiteManager;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.SecureRandom;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 9/22/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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
    public static final String ASKED_TO_WRITE_PHRASE = "phraseWrittenDown";
    private static BRWalletManager instance;
    private static Activity ctx;

    private BRWalletManager() {
    }

    public static BRWalletManager getInstance(Activity context) {
        ctx = context;
        if (instance == null) {
            instance = new BRWalletManager();
        }
        return instance;
    }

    public boolean generateRandomSeed() {
        SecureRandom sr = new SecureRandom();

        String[] words = new String[0];
        List<String> list;
        try {
            list = WordsReader.getWordList(ctx);
            words = list.toArray(new String[list.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] keyBytes = sr.generateSeed(16);
        if (words.length < 2000)
            throw new IllegalArgumentException("the list is wrong, size: " + words.length);
        if(keyBytes.length == 0) throw new NullPointerException("failed to create the seed");
        byte[] phrase = encodeSeed(keyBytes, words);
        if (phrase == null || phrase.length == 0)
            throw new NullPointerException("failed to encodeSeed");
        String strPhrase = null;
        try {
            strPhrase = new String(phrase, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
            throw new NullPointerException("failed to create the phrase");
        }
        boolean success = KeyStoreManager.putKeyStorePhrase(strPhrase, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        boolean success2 = false;
        if (success)
            success2 = KeyStoreManager.putKeyStoreCanary(BRConstants.CANARY_STRING, ctx, BRConstants.PUT_CANARY_REQUEST_CODE);
        IntroShowPhraseActivity.phrase = strPhrase;
        KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), ctx);
        byte[] pubKey = BRWalletManager.getInstance(ctx).getMasterPubKey(strPhrase);
        KeyStoreManager.putMasterPublicKey(pubKey, ctx);

        phrase = null;
        Log.e(TAG, "setKeyStoreString was successful: " + success);
        return success && success2;

    }

//    public boolean setKeyStoreString(String strPhrase, String key,
//                                     boolean authenticated, Context ctx) {
//        return KeyStoreManager.setKeyStoreString(strPhrase, ctx);
//    }

    /**
     * given a private key, queries chain.com for unspent outputs and calls the completion block with
     * a signed transaction that will sweep the balance into wallet (doesn't publish the tx)
     */
    public boolean sweepPrivateKey() {
        return KeyStoreManager.resetWalletKeyStore();
    }

    /**
     * true if keychain is available and we know that no wallet exists on it
     */
    public boolean noWallet(Activity ctx) {
        byte[] pubkey = KeyStoreManager.getMasterPublicKey(ctx);
//        Log.e(TAG, "in the noWallet, pubkey.length(): " + pubkey.length);
//        Log.e(TAG, "in the noWallet, pubkey: " + pubkey);
        return pubkey == null || pubkey.length == 0;

    }

    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Activity ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(Activity.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public static void refreshAddress() {
        Log.e(TAG, "refreshAddress: " + ctx);
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            MainFragmentQR mainFragmentQR = CustomPagerAdapter.adapter == null ? null : CustomPagerAdapter.adapter.mainFragmentQR;
            String tmpAddr = getReceiveAddress();
            if (tmpAddr == null || tmpAddr.isEmpty()) return;
            SharedPreferences.Editor editor = ctx.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).edit();
            editor.putString(MainActivity.PREFS_NAME, tmpAddr);
            editor.apply();
            if (mainFragmentQR == null) return;
            mainFragmentQR.refreshAddress(tmpAddr);
        } else {
            throw new NullPointerException("Cannot be null");
        }
    }

    public void wipeWallet(Activity activity) {
        sweepPrivateKey();
        BRPeerManager.saveStuffRunning = false;
        BRPeerManager.getInstance(activity).peerManagerFreeEverything();
        walletFreeEverything();
        SQLiteManager sqLiteManager = SQLiteManager.getInstance(activity);
        sqLiteManager.deleteTransactions();
        sqLiteManager.deleteBlocks();
        sqLiteManager.deletePeers();
        SharedPreferences.Editor editor = activity.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE).edit();
        editor.clear();
        editor.apply();
    }

    private static void showWritePhraseDialog() {

        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    SharedPreferences prefs = ctx.getSharedPreferences(MainActivity.PREFS_NAME, Context.MODE_PRIVATE);
                    boolean phraseWroteDown = prefs.getBoolean(ASKED_TO_WRITE_PHRASE, false);
                    if (phraseWroteDown) return;
                    AlertDialog alert;
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    builder.setTitle(ctx.getString(R.string.you_received_bitcoin));

                    builder.setMessage(ctx.getString(R.string.write_down_phrase));
                    builder.setPositiveButton(ctx.getString(R.string.show_phrase),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                    Intent intent;
                                    intent = new Intent(ctx, IntroShowPhraseActivity.class);
                                    ctx.startActivity(intent);
                                }
                            });
                    builder.setNegativeButton(ctx.getString(R.string.do_it_later),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    alert = builder.create();
                    alert.show();
                }
            });

        }

    }

    /**
     * Wallet callbacks
     */

    public static void onBalanceChanged(final long balance) {

        Log.e(TAG, "in the BRWalletManager - onBalanceChanged:  " + balance);
        if (ctx == null) ctx = MainActivity.app;
        ctx.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                CurrencyManager.getInstance(ctx).setBalance(balance);
                refreshAddress();
                FragmentSettingsAll.refreshTransactions(ctx);
            }
        });

    }

    public static void onTxAdded(byte[] tx, int blockHeight, long timestamp, final long amount, String hash) {
        Log.e(TAG, "in the BRWalletManager - onTxAdded: " + tx.length + " " + blockHeight + " " + timestamp + " " + amount);
        final RequestQRActivity requestApp = RequestQRActivity.requestApp;
        if (requestApp != null && !requestApp.activityIsInBackground) {
            requestApp.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    requestApp.close.performClick();
                }
            });
        }
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null && !MiddleViewAdapter.getSyncing()) {

            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CurrencyManager m = CurrencyManager.getInstance(ctx);
//                    if (amount > 0) {
//                        showWritePhraseDialog();
                    double absAmount = amount > 0 ? amount : amount * -1;
                    CurrencyManager cm = CurrencyManager.getInstance(ctx);
                    String strToShow = String.format(ctx.getString(amount > 0 ? R.string.received : R.string.sent),
                            cm.getFormattedCurrencyString("BTC", String.valueOf(cm.getBitsFromSatoshi(absAmount))) + " (" +
                                    m.getExchangeForAmount(m.getRateFromPrefs(), m.getISOFromPrefs(), String.valueOf(m.getBitsFromSatoshi(absAmount))) + ")");
                    ((BreadWalletApp) ctx.getApplicationContext()).showCustomToast((Activity) ctx, strToShow,
                            BreadWalletApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, 1);
//                    } else {
//                        ((BreadWalletApp) ctx.getApplicationContext()).showCustomToast((Activity) ctx,
//                                String.format(ctx.getString(R.string.sent), m.bitcoinLowercase + m.getBitsFromSatoshi(amount)),
//                                BreadWalletApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, 1);
//                    }
                }
            });

        }

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);
        sqLiteManager.insertTransaction(tx, blockHeight, timestamp, hash);
    }

    public static void onTxUpdated(String hash, int blockHeight) {
        Log.e(TAG, "in the BRWalletManager - onTxUpdated");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            SQLiteManager.getInstance(ctx).updateTxByHash(hash, blockHeight);
        }
    }

    public static void onTxDeleted(String hash) {
        Log.e(TAG, "in the BRWalletManager - onTxDeleted");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            SQLiteManager.getInstance(ctx).deleteTxByHash(hash);
        }
    }

    public boolean validatePhrase(Activity activity, String phrase) {
        String[] words = new String[0];
        List<String> list;
        try {
            list = WordsReader.getWordList(activity);
            words = list.toArray(new String[list.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (words.length != 2048)
            throw new IllegalArgumentException("words.length is not 2048");
        return validateRecoveryPhrase(words, phrase);
    }

    private native byte[] encodeSeed(byte[] seed, String[] wordList);

    public native void createWallet(int transactionCount, byte[] pubkey);

    public native void putTransaction(byte[] transaction, long blockHeight, long timeStamp);

    public native void createTxArrayWithCount(int count);

    public native void setCallbacks(byte[] wallet);

    public native void setPeerManagerCallbacks(byte[] peerManager);

    public native byte[] getMasterPubKey(String normalizedString);

    public native void testWalletCallbacks();

    public native void testTransactionAdding(long amount);

    public static native String getReceiveAddress();

    public native TransactionListItem[] getTransactions();

    public native boolean pay(String addressHolder, long amountHolder, String strSeed);

    public native boolean validateAddress(String address);

    public native boolean addressContainedInWallet(String address);

    public native boolean addressIsUsed(String address);

    public native int feeForTransaction(String addressHolder, long amountHolder);

    public native double getMinOutputAmount();

    public native long getMaxOutputAmount();

    public native boolean isCreated();

    public native boolean transactionIsVerified(String txHash);

    public native boolean tryTransaction(String addressHolder, long amountHolder);

    public native long localAmount(long amount, double price);

    public native long bitcoinAmount(long localAmount, double price);

    public native void walletFreeEverything();

    private native boolean validateRecoveryPhrase(String[] words, String phrase);

}
