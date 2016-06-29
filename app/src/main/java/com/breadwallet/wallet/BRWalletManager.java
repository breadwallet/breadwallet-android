package com.breadwallet.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.FragmentTransaction;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.SharedPreferences;
import android.media.MediaPlayer;
import android.os.Handler;
import android.text.InputType;
import android.util.Log;
import android.widget.EditText;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroShowPhraseActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.RequestQRActivity;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.presenter.entities.TmpTxObject;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.fragments.FragmentRecoveryPhrase;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.MainFragmentQR;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.BRNotificationManager;
import com.breadwallet.tools.BRStringFormatter;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.SharedPreferencesManager;
import com.breadwallet.tools.WordsReader;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.tools.threads.ImportPrivKeyTask;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
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
    public static final String PHRASE_WRITTEN = "phraseWritten";
    private static BRWalletManager instance;
    private static Activity ctx;
    public static final long TX_FEE_PER_KB = 5000;
    public static final long DEFAULT_FEE_PER_KB = (TX_FEE_PER_KB * 1000 + 190) / 191;
    public static final long MAX_FEE_PER_KB = (100100 * 1000 + 190) / 191;
    private static int messageId = 0;

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
            String languageCode = ctx.getString(R.string.lang);
            list = WordsReader.getWordList(ctx, languageCode);
            words = list.toArray(new String[list.size()]);
        } catch (IOException e) {
            e.printStackTrace();
        }
        byte[] keyBytes = sr.generateSeed(16);
        if (words.length < 2000)
            throw new IllegalArgumentException("the list is wrong, size: " + words.length);
        if (keyBytes.length == 0) throw new NullPointerException("failed to create the seed");
        String strPhrase = encodeSeed(keyBytes, words);
        if (strPhrase == null || strPhrase.length() == 0)
            throw new NullPointerException("failed to encodeSeed");
        boolean success = KeyStoreManager.putKeyStorePhrase(strPhrase, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        boolean success2 = false;
        if (!success) return false;
        success2 = KeyStoreManager.putKeyStoreCanary(BRConstants.CANARY_STRING, ctx, 0);
        IntroShowPhraseActivity.phrase = strPhrase;
        KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), ctx);
        byte[] pubKey = BRWalletManager.getInstance(ctx).getMasterPubKey(strPhrase);
        KeyStoreManager.putMasterPublicKey(pubKey, ctx);
        return success2;

    }

    public boolean wipeKeyStore() {
        return KeyStoreManager.resetWalletKeyStore();
    }

    /**
     * true if keychain is available and we know that no wallet exists on it
     */
    public boolean noWallet(Activity ctx) {
        byte[] pubkey = KeyStoreManager.getMasterPublicKey(ctx);
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
            SharedPreferencesManager.putReceiveAddress(ctx, tmpAddr);
            if (mainFragmentQR == null) return;
            mainFragmentQR.refreshAddress(tmpAddr);
        } else {
            throw new NullPointerException("Cannot be null");
        }
    }

    public void wipeWalletButKeystore(Activity activity) {
        BRPeerManager.saveStuffRunning = false;
        BRPeerManager.getInstance(activity).peerManagerFreeEverything();
        walletFreeEverything();
        SQLiteManager sqLiteManager = SQLiteManager.getInstance(activity);
        sqLiteManager.deleteTransactions();
        sqLiteManager.deleteBlocks();
        sqLiteManager.deletePeers();
        SharedPreferencesManager.clearAllPrefs(activity);
    }

    public boolean confirmSweep(final Activity activity, final String privKey) {

        if (activity == null) return false;

        if (isValidBitcoinBIP38Key(privKey)) {
            Log.e(TAG, "isValidBitcoinBIP38Key true");
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    if (!CurrencyManager.getInstance(activity).isNetworkAvailable(activity)) {
                        ((BreadWalletApp) activity.getApplication()).showCustomDialog(activity.getString(R.string.warning),
                                "not connected", activity.getString(R.string.ok));
                        return;
                    }

                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
                    builder.setTitle("password protected key");

                    // Set up the input
                    final EditText input = new EditText(activity);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    input.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                    builder.setView(input);

                    // Set up the buttons
                    builder.setPositiveButton("OK", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            String pass = input.getText().toString();
                            String decryptedKey = decryptBip38Key(privKey, pass);
                            Log.e(TAG, "decryptedKey: " + decryptedKey);
                            if (decryptedKey.equals("")) {
                                SpringAnimator.showAnimation(input);
                                confirmSweep(activity, privKey);
                            } else {
                                confirmSweep(activity, decryptedKey);
                            }
                        }
                    });
                    builder.setNegativeButton("Cancel", new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    });

                    builder.show();
                }
            });
            return true;
        } else if (isValidBitcoinPrivateKey(privKey)) {
            Log.e(TAG, "isValidBitcoinPrivateKey true");
            new ImportPrivKeyTask(activity).execute(privKey);
            return true;
        } else {
            return false;
        }
    }

    private static void showWritePhraseDialog() {

        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean phraseWroteDown = SharedPreferencesManager.getPhraseWroteDown(ctx);
                    if (phraseWroteDown) return;
                    AlertDialog alert;
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    builder.setTitle(ctx.getString(R.string.you_received_bitcoin));

                    builder.setMessage(ctx.getString(R.string.write_down_phrase));
//                    builder.setPositiveButton(ctx.getString(R.string.show_phrase),
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.dismiss();
//                                    MainActivity app = MainActivity.app;
//                                    if (app != null)
//                                        FragmentAnimator.animateSlideToLeft(app, new FragmentRecoveryPhrase(), null);
//                                }
//                            });
                    builder.setNegativeButton(ctx.getString(R.string.ok),
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
        if (ctx != null) {

            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    CurrencyManager m = CurrencyManager.getInstance(ctx);
                    long absAmount = amount > 0 ? amount : amount * -1;
                    String strToShow = String.format(ctx.getString(amount > 0 ? R.string.received : R.string.sent),
                            BRStringFormatter.getFormattedCurrencyString("BTC", absAmount) + " (" +
                                    BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx),
                                            SharedPreferencesManager.getIso(ctx), new BigDecimal(absAmount), ctx) + ")");
                    showSentReceivedToast(strToShow);
                }

            });

        }

        if (getInstance(ctx).getTxCount() <= 1) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showWritePhraseDialog();
                        }
                    }, 4000);
                }
            });

        }

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);
        sqLiteManager.insertTransaction(tx, blockHeight, timestamp, hash);
    }

    private static void showSentReceivedToast(final String message) {
        messageId++;
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int temp = messageId;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (temp == messageId) {
                                if (!((BreadWalletApp) ctx.getApplication()).isToastShown()) {
                                    ((BreadWalletApp) ctx.getApplicationContext()).showCustomToast(ctx, message,
                                            BreadWalletApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, 1);
                                    final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.coinflip);
                                    mp.start();
                                    messageId = 0;
                                    if (MainActivity.appInBackground)
                                        BRNotificationManager.sendNotification(ctx, R.drawable.notification_icon, ctx.getString(R.string.app_name), message, 1);
                                }
                            }
                        }
                    }, 1000);

                }
            });

        }
    }

    public static void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        Log.e(TAG, "in the BRWalletManager - onTxUpdated");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            SQLiteManager.getInstance(ctx).updateTxByHash(hash, blockHeight, timeStamp);
        }
    }

    public static void onTxDeleted(String hash, int notifyUser, final int recommendRescan) {
        Log.e(TAG, "in the BRWalletManager - onTxDeleted");
        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            SQLiteManager.getInstance(ctx).deleteTxByHash(hash);
            if (notifyUser == 1) {
                ctx.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog alert;
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setTitle("transaction rejected");

                        builder.setMessage(recommendRescan == 1 ? "Your wallet may be out of sync.\nThis can often be fixed by rescanning the blockchain." : "");
                        if (recommendRescan == 1)
                            builder.setPositiveButton("rescan",
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            if (FragmentAnimator.checkTheMultipressingAvailability()) {
                                                new Thread(new Runnable() {
                                                    @Override
                                                    public void run() {
                                                        BRPeerManager.getInstance(ctx).rescan();
                                                    }
                                                }).start();
                                            }
                                        }
                                    });
                        builder.setNegativeButton(ctx.getString(R.string.cancel),
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
    }

    public boolean validatePhrase(Activity activity, String phrase) {
        String[] words = new String[0];
        List<String> list;

        String[] cleanWordList = null;
        try {
            boolean isLocal = true;
            String languageCode = ctx.getString(R.string.lang);
            list = WordsReader.getWordList(activity, languageCode);

            String[] phraseWords = phrase.split(" ");
            if (!list.contains(phraseWords[0])) {
                isLocal = false;
            }
            if (!isLocal) {
                String lang = WordsReader.getLang(activity, phraseWords[0]);
                if (lang != null) {
                    list = WordsReader.getWordList(activity, lang);
                }

            }
            words = list.toArray(new String[list.size()]);
            cleanWordList = WordsReader.cleanWordList(words);
            if (cleanWordList == null) return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (words.length != 2048)
            throw new IllegalArgumentException("words.length is not 2048");
        return validateRecoveryPhrase(cleanWordList, phrase);
    }

    private native String encodeSeed(byte[] seed, String[] wordList);

    public native void createWallet(int transactionCount, byte[] pubkey);

    public native void putTransaction(byte[] transaction, long blockHeight, long timeStamp);

    public native void createTxArrayWithCount(int count);

    public native byte[] getMasterPubKey(String normalizedString);

    public static native String getReceiveAddress();

    public native TransactionListItem[] getTransactions();

    public native boolean pay(String addressHolder, long amountHolder, String strSeed);

    public static native boolean validateAddress(String address);

    public native boolean addressContainedInWallet(String address);

    public native boolean addressIsUsed(String address);

    public native int feeForTransaction(String addressHolder, long amountHolder);

    public native long getMinOutputAmount();

    public native long getMaxOutputAmount();

    public native boolean isCreated();

    public native boolean transactionIsVerified(String txHash);

    public native byte[] tryTransaction(String addressHolder, long amountHolder);

    public native long localAmount(long amount, double price);

    public native long bitcoinAmount(long localAmount, double price);

    public native void walletFreeEverything();

    private native boolean validateRecoveryPhrase(String[] words, String phrase);

    public native static String getFirstAddress(byte[] mpk);

    public native boolean publishSerializedTransaction(byte[] serializedTransaction, String phrase);

    public native boolean publishSignedSerializedTransaction(byte[] serializedTransaction);

    public native long getTotalSent();

    public native long setFeePerKb(long fee);

    public native boolean isValidBitcoinPrivateKey(String key);

    public native boolean isValidBitcoinBIP38Key(String key);

    public native String getAddressFromPrivKey(String key);

    public native void createInputArray();

    public native void addInputToPrivKeyTx(byte[] hash, int vout, byte[] script, long amount);

    public native boolean confirmKeySweep(byte[] tx, String key);

    public native ImportPrivKeyEntity getPrivKeyObject();

    public native String decryptBip38Key(String privKey, String pass);

    public native String reverseTxHash(String txHash);

    public native int getTxCount();

    public native double getMinOutputAmountRequested();
}
