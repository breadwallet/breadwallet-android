package com.breadwallet.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.util.Log;
import android.view.Display;
import android.view.MotionEvent;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.activities.PinActivity;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.threads.PaymentProtocolPostPaymentTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.BRNotificationManager;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.WordsReader;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.tools.threads.ImportPrivKeyTask;
import com.google.firebase.crash.FirebaseCrash;
import com.google.zxing.WriterException;

import junit.framework.Assert;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;
import java.util.Observable;

import static com.breadwallet.presenter.activities.BreadActivity.app;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 12/10/15.
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

public class BRWalletManager extends Observable {
    private static final String TAG = BRWalletManager.class.getName();

    private static BRWalletManager instance;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private long balance = 0;

    public void setBalance(long balance) {
        this.balance = balance;
        setChanged();
        notifyObservers();
        refreshAddress();
//        FragmentSettingsAll.refreshTransactions(ctx);
        //todo add transactions as an observer
    }

    public long getBalance() {
        return balance;
    }

    private static int messageId = 0;

    private BRWalletManager() {
    }

    public static BRWalletManager getInstance() {
        if (instance == null) {
            instance = new BRWalletManager();
        }
        return instance;
    }

    public boolean generateRandomSeed(Context ctx) {
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
        if (words.length < 2000) {
            RuntimeException ex = new IllegalArgumentException("the list is wrong, size: " + words.length);
            FirebaseCrash.report(ex);
            throw ex;
        }
        if (keyBytes.length == 0) throw new NullPointerException("failed to create the seed");
        byte[] strPhrase = encodeSeed(keyBytes, words);
        if (strPhrase == null || strPhrase.length == 0) {
            RuntimeException ex = new NullPointerException("failed to encodeSeed");
            FirebaseCrash.report(ex);
            throw ex;
        }
        boolean success = false;
        try {
            success = KeyStoreManager.putKeyStorePhrase(strPhrase, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
        }
        if (!success) return false;
        byte[] authKey = getAuthPrivKeyForAPI(keyBytes);
        if (authKey == null || authKey.length == 0) {
            RuntimeException ex = new IllegalArgumentException("authKey is invalid");
            FirebaseCrash.report(ex);
            throw ex;
        }
        KeyStoreManager.putAuthKey(authKey, ctx);
        KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), ctx);
        byte[] strBytes = TypesConverter.getNullTerminatedPhrase(strPhrase);
        byte[] pubKey = BRWalletManager.getInstance().getMasterPubKey(strBytes);
        KeyStoreManager.putMasterPublicKey(pubKey, ctx);

        return true;

    }

    public boolean wipeKeyStore(Context context) {
        Log.e(TAG, "wipeKeyStore");
        return KeyStoreManager.resetWalletKeyStore(context);
    }

    /**
     * true if keychain is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        byte[] pubkey = KeyStoreManager.getMasterPublicKey(ctx);

        if (pubkey == null || pubkey.length == 0) {
            byte[] phrase;
            try {
                phrase = KeyStoreManager.getKeyStorePhrase(ctx, 0);
                if (phrase == null || phrase.length == 0) {
                    return true;
                }
            } catch (BRKeystoreErrorException e) {
                e.printStackTrace();
                return false;
            }

        }
        return false;
    }

    public boolean noWalletForPlatform(Context ctx) {
        byte[] pubkey = KeyStoreManager.getMasterPublicKey(ctx);
        return pubkey == null || pubkey.length == 0;
    }

    /**
     * true if device passcode is enabled
     */
    public boolean isPasscodeEnabled(Context ctx) {
        KeyguardManager keyguardManager = (KeyguardManager) ctx.getSystemService(Activity.KEYGUARD_SERVICE);
        return keyguardManager.isKeyguardSecure();
    }

    public static void refreshAddress() {
        //todo finish
//        if (ctx != null) {
//            MainFragmentQR mainFragmentQR = CustomPagerAdapter.adapter == null ? null : CustomPagerAdapter.adapter.mainFragmentQR;
//            String tmpAddr = getReceiveAddress();
//            if (tmpAddr == null || tmpAddr.isEmpty()) return;
//            SharedPreferencesManager.putReceiveAddress(ctx, tmpAddr);
//            if (mainFragmentQR == null) return;
//            mainFragmentQR.refreshAddress(tmpAddr);
//        } else {
//            RuntimeException ex = new NullPointerException("Cannot be null");
//            FirebaseCrash.report(ex);
//            throw ex;
//        }
    }

    public void wipeWalletButKeystore(final Context ctx) {
        Log.e(TAG, "wipeWalletButKeystore");
        new Thread(new Runnable() {
            @Override
            public void run() {
                BRPeerManager.getInstance().peerManagerFreeEverything();
                walletFreeEverything();
            }
        }).start();

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);
        sqLiteManager.deleteTransactions();
        sqLiteManager.deleteBlocks();
        sqLiteManager.deletePeers();
        SharedPreferencesManager.clearAllPrefs(ctx);
    }

    public boolean confirmSweep(final Context ctx, final String privKey) {
//        if (ctx == null) return false;
//        if (isValidBitcoinBIP38Key(privKey)) {
//            Log.d(TAG, "isValidBitcoinBIP38Key true");
//            ((Activity) ctx).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//
//                    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
////                    builder.setmTitle("password protected key");
//
//                    final View input = ((Activity) ctx).getLayoutInflater().inflate(R.layout.view_bip38password_dialog, null);
//                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
//                    builder.setView(input);
//
//                    final EditText editText = (EditText) input.findViewById(R.id.bip38password_edittext);
//
//                    (new Handler()).postDelayed(new Runnable() {
//                        public void run() {
//                            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
//                            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));
//
//                        }
//                    }, 100);
//
//                    // Set up the buttons
//                    builder.setPositiveButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            if (!((BreadWalletApp) ((Activity) ctx).getApplication()).hasInternetAccess()) {
//                                ((Activity) ctx).runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
////                                        BreadDialog.showCustomDialog(ctx, ctx.getString(R.string.warning),
////                                                ctx.getString(R.string.not_connected), ctx.getString(R.string.ok));
//                                    }
//                                });
//
//                                return;
//                            }
//                            if (ctx != null)
//                                ((Activity) ctx).runOnUiThread(new Runnable() {
//                                    @Override
//                                    public void run() {
//                                        ((BreadWalletApp) ((Activity) ctx).getApplication()).showCustomToast(ctx,
//                                                ctx.getString(R.string.checking_privkey_balance), BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 1);
//                                    }
//                                });
//                            if (editText == null) return;
//
//                            String pass = editText.getText().toString();
//                            String decryptedKey = decryptBip38Key(privKey, pass);
//
//                            if (decryptedKey.equals("")) {
//                                SpringAnimator.showAnimation(input);
//                                confirmSweep(ctx, privKey);
//                            } else {
//                                confirmSweep(ctx, decryptedKey);
//                            }
//
//                        }
//                    });
//                    builder.setNegativeButton(ctx.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.cancel();
//                        }
//                    });
//
//                    builder.show();
//                }
//            });
//            return true;
//        } else if (isValidBitcoinPrivateKey(privKey)) {
//            Log.d(TAG, "isValidBitcoinPrivateKey true");
//            new ImportPrivKeyTask(((Activity) ctx)).execute(privKey);
//            return true;
//        } else {
//            Log.e(TAG, "confirmSweep: !isValidBitcoinPrivateKey && !isValidBitcoinBIP38Key");
//            return false;
//        }
        return false;
    }

    public static void showWritePhraseDialog(final Context ctx, final boolean firstTime) {

        if (ctx != null) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean phraseWroteDown = SharedPreferencesManager.getPhraseWroteDown(ctx);
                    if (phraseWroteDown) return;
                    long now = System.currentTimeMillis() / 1000;
                    long lastMessageShow = SharedPreferencesManager.getPhraseWarningTime(ctx);
                    if (lastMessageShow == 0 || (!firstTime && lastMessageShow > (now - 36 * 60 * 60)))
                        return;//36 * 60 * 60//
                    if (BRWalletManager.getInstance().getBalance() > SharedPreferencesManager.getLimit(ctx)) {
//                        getInstance(ctx).animateSavePhraseFlow();
                        return;
                    }
                    SharedPreferencesManager.putPhraseWarningTime(ctx, System.currentTimeMillis() / 1000);
                    AlertDialog alert;
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    builder.setTitle(ctx.getString(R.string.you_received_bitcoin));
                    builder.setMessage(String.format(ctx.getString(R.string.write_down_phrase),
                            ctx.getString(R.string.write_down_phrase_holder1)));
                    builder.setPositiveButton(ctx.getString(R.string.show_phrase),
                            new DialogInterface.OnClickListener() {
                                public void onClick(final DialogInterface dialog, int which) {
                                    new Thread(new Runnable() {
                                        @Override
                                        public void run() {
                                            dialog.dismiss();
//                                            BRWalletManager.getInstance().animateSavePhraseFlow();
                                        }
                                    }).start();
                                }
                            });
                    builder.setNegativeButton(ctx.getString(R.string.ok),
                            new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int which) {
                                    dialog.dismiss();
                                }
                            });
                    builder.setCancelable(false);
                    alert = builder.create();
                    alert.show();
                }
            });

        }

    }

    /**
     * Wallet callbacks
     */

    public static void publishCallback(Context ctx, final String message, int error) {
        PaymentProtocolPostPaymentTask.waiting = false;
        if (error != 0) {
            if (!PaymentProtocolPostPaymentTask.waiting && !PaymentProtocolPostPaymentTask.sent) {
                if (PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.MESSAGE) != null) {
//                    BreadDialog.showCustomDialog(ctx, PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.TITLE),
//                            PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.MESSAGE), ctx.getString(R.string.ok));
                    PaymentProtocolPostPaymentTask.pendingErrorMessages = null;
                } else {
                    ((BreadWalletApp) ((Activity) ctx).getApplication()).showCustomToast(ctx, message,
                            BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 1);
                }
            }
        } else {
            PaymentProtocolPostPaymentTask.sent = true;
        }
    }

    public static void onBalanceChanged(final long balance) {
        Log.d(TAG, "onBalanceChanged:  " + balance);
        BRWalletManager.getInstance().setBalance(balance);

    }

    public static void onTxAdded(byte[] tx, int blockHeight, long timestamp, final long amount, String hash) {
        Log.d(TAG, "onTxAdded: " + String.format("tx.length: %d, blockHeight: %d, timestamp: %d, amount: %d, hash: %s", tx.length, blockHeight, timestamp, amount, hash));
//        final RequestQRActivity requestApp = RequestQRActivity.requestApp;
//        if (requestApp != null && !requestApp.activityIsInBackground) {
//            requestApp.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    requestApp.close.performClick();
//                }
//            });
//        }
        final BreadActivity ctx = app;
        if (ctx != null) {

//            ctx.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    long absAmount = (amount > 0 ? amount : amount * -1);
//                    String strToShow = amount > 0 ?
//                            (String.format(ctx.getString(R.string.received_amount),
//                                    BRStringFormatter.getFormattedCurrencyString(app, "BTC", absAmount),
//                                    BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx),
//                                            SharedPreferencesManager.getIso(ctx), new BigDecimal(absAmount), ctx))) :
//                            ctx.getString(R.string.sent_exclaimed);
//
//                    showSentReceivedToast(ctx, strToShow);
//                }
//
//            });

        }

        if (getInstance().getTxCount() <= 1) {
            SharedPreferencesManager.putPhraseWarningTime(ctx, System.currentTimeMillis() / 1000);
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showWritePhraseDialog(ctx, true);
                        }
                    }, 2000);
                }
            });

        }

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);
        sqLiteManager.insertTransaction(tx, blockHeight, timestamp, hash);
    }

    private static void showSentReceivedToast(final Context ctx, final String message) {
        messageId++;
        if (ctx != null) {
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    final int temp = messageId;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (temp == messageId) {
                                if (!((BreadWalletApp) ((Activity) ctx).getApplication()).isToastShown()) {
                                    ((BreadWalletApp) ctx.getApplicationContext()).showCustomToast(ctx, message,
                                            BreadWalletApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, 1);
                                    AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                                    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                        final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.coinflip);
                                        mp.start();

                                    }
                                    messageId = 0;
                                    if (BreadActivity.appInBackground)
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
        Log.d(TAG, "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
        BreadActivity ctx = app;
        if (ctx != null) {
            SQLiteManager.getInstance(ctx).updateTxByHash(hash, blockHeight, timeStamp);
        }
    }

    public static void onTxDeleted(String hash, int notifyUser, final int recommendRescan) {
        Log.e(TAG, "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d", hash, notifyUser, recommendRescan));
        final BreadActivity ctx = app;
        if (ctx != null) {
            SQLiteManager.getInstance(ctx).deleteTxByHash(hash);
            if (notifyUser == 1) {
                ctx.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog alert;
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setTitle(R.string.transaction_rejected);

                        builder.setMessage(recommendRescan == 1 ? ctx.getString(R.string.wallet_out_of_sync_message) : "");
                        if (recommendRescan == 1)
                            builder.setPositiveButton(R.string.rescan,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
//                                            if (BRAnimator.checkTheMultipressingAvailability()) {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    BRPeerManager.getInstance().rescan();
                                                }
                                            }).start();
//                                            }
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

    public boolean validatePhrase(Context ctx, String phrase) {
        String[] words = new String[0];
        List<String> list;

        String[] cleanWordList = null;
        try {
            boolean isLocal = true;
            String languageCode = ctx.getString(R.string.lang);
            list = WordsReader.getWordList(ctx, languageCode);

            String[] phraseWords = phrase.split(" ");
            if (!list.contains(phraseWords[0])) {
                isLocal = false;
            }
            if (!isLocal) {
                String lang = WordsReader.getLang(ctx, phraseWords[0]);
                if (lang != null) {
                    list = WordsReader.getWordList(ctx, lang);
                }

            }
            words = list.toArray(new String[list.size()]);
            cleanWordList = WordsReader.cleanWordList(words);
            if (cleanWordList == null) return false;
        } catch (IOException e) {
            e.printStackTrace();
        }
        if (words.length != 2048) {
            RuntimeException ex = new IllegalArgumentException("words.length is not 2048");
            FirebaseCrash.report(ex);
            throw ex;
        }
        return validateRecoveryPhrase(cleanWordList, phrase);
    }

    public void confirmPay(final Context ctx, final PaymentRequestEntity request) {
//        if (((BreadWalletApp) ((Activity) ctx).getApplication()).hasInternetAccess()) {
//
//            if (ctx == null) return;
//            boolean certified = false;
//            if (request.cn != null && request.cn.length() != 0) {
//                certified = true;
//            }
//            StringBuilder allAddresses = new StringBuilder();
//            for (String s : request.addresses) {
//                allAddresses.append(s + ", ");
//            }
//            allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
//            String certification = "";
//            if (certified) {
//                certification = "certified: " + request.cn + "\n";
//                allAddresses = new StringBuilder();
//            }
//
//            //DecimalFormat decimalFormat = new DecimalFormat("0.00");
//            String iso = SharedPreferencesManager.getIso(ctx);
//
//            float rate = SharedPreferencesManager.getRate(ctx);
//            BRWalletManager m = getInstance();
//            long feeForTx = m.feeForTransaction(request.addresses[0], request.amount);
//            if (feeForTx == 0) {
//                long maxAmountDouble = m.getMaxOutputAmount();
//                if (maxAmountDouble == -1) {
//                    RuntimeException ex = new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL");
//                    FirebaseCrash.report(ex);
//                    throw ex;
//                }
//                if (maxAmountDouble == 0) {
//                    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                    builder.setmMessage("")
//                            .setmTitle(R.string.insufficient_funds_for_fee)
//                            .setCancelable(false)
//                            .setNegativeButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
//                                @Override
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.cancel();
//                                }
//                            });
//                    AlertDialog alert = builder.create();
//                    alert.show();
//                    return;
//                }
//                feeForTx = m.feeForTransaction(request.addresses[0], maxAmountDouble);
//                feeForTx += (getBalance() - request.amount) % 100;
//            }
//            final long total = request.amount + feeForTx;
//            final String message = certification + allAddresses.toString() + "\n\n" + "amount: " + BRStringFormatter.getFormattedCurrencyString(ctx, "BTC", request.amount)
//                    + " (" + BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(request.amount), ctx) + ")" + "\nnetwork fee: +" + BRStringFormatter.getFormattedCurrencyString(ctx,"BTC", feeForTx)
//                    + " (" + BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(feeForTx), ctx) + ")" + "\ntotal: " + BRStringFormatter.getFormattedCurrencyString(ctx,"BTC", total)
//                    + " (" + BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(total), ctx) + ")";
//
//            double minOutput;
//            if (request.isAmountRequested) {
//                minOutput = BRWalletManager.getInstance().getMinOutputAmountRequested();
//            } else {
//                minOutput = BRWalletManager.getInstance().getMinOutputAmount();
//            }
//            if (request.amount < minOutput) {
//                final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.bitcoin_payment_cant_be_less),
//                        BRConstants.bitcoinLowercase + new BigDecimal(minOutput).divide(new BigDecimal("100")));
//                ((Activity) ctx).runOnUiThread(new Runnable() {
//                    @Override
//                    public void run() {
//                        new android.app.AlertDialog.Builder(ctx)
//                                .setmTitle(ctx.getString(R.string.payment_failed))
//                                .setmMessage(bitcoinMinMessage)
//                                .setPositiveButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
//                                    public void onClick(DialogInterface dialog, int which) {
//                                        dialog.dismiss();
//                                    }
//                                })
//                                .setIcon(android.R.drawable.ic_dialog_alert)
//                                .show();
//                    }
//                });
//                return;
//            }
//
//            ((Activity) ctx).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    ((BreadWalletApp) ctx.getApplicationContext()).authPrompt(ctx,
//                            BRConstants.AUTH_FOR_PAY, request, message, "", null, false);
//                }
//            });
//        } else {
//            ((Activity) ctx).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                    builder.setmMessage(R.string.not_connected)
//                            .setCancelable(false)
//                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int id) {
//                                    dialog.cancel();
//                                }
//                            });
//                    AlertDialog alert = builder.create();
//                    alert.show();
//                }
//            });
//
//        }

    }

    public void pay(final Context ctx, final String addressHolder, final BigDecimal bigDecimalAmount, final String cn, final boolean isAmountRequested) {
//        Log.e(TAG, "pay: " + String.format("addressHolder: %s, bigDecimalAmount: %s, cn: %s, isAmountRequested: %b", addressHolder, bigDecimalAmount == null ? null : bigDecimalAmount.toPlainString(), cn, isAmountRequested));
//        if (addressHolder == null || bigDecimalAmount == null) return;
//        if (addressHolder.length() < 20) return;
//        if (!SharedPreferencesManager.getAllowSpend(app)) {
//            showSpendNotAllowed(app);
//            return;
//        }
//
//        int unit = BRConstants.CURRENT_UNIT_BITS;
//        String divideBy = "100";
//        if (ctx != null)
//            unit = SharedPreferencesManager.getCurrencyUnit(ctx);
//        if (unit == BRConstants.CURRENT_UNIT_MBITS) divideBy = "100000";
//        if (unit == BRConstants.CURRENT_UNIT_BITCOINS) divideBy = "100000000";
////        final long amountAsLong = bigDecimal.longValue();
//        if (bigDecimalAmount.longValue() < 0) return;
//        final CurrencyManager cm = CurrencyManager.getInstance(ctx);
//        long minAmount = getMinOutputAmountRequested();
//        if (bigDecimalAmount.longValue() < minAmount) {
//            Log.e(TAG, "pay: FAIL: bitcoin payment is less then the minimum.");
//            final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.bitcoin_payment_cant_be_less),
//                    BRConstants.bitcoinLowercase + new BigDecimal(minAmount).divide(new BigDecimal(divideBy)));
//            builder.setmMessage(bitcoinMinMessage)
//                    .setmTitle(R.string.could_not_make_payment)
//                    .setCancelable(false)
//                    .setNegativeButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.cancel();
//                        }
//                    });
//            AlertDialog alert = builder.create();
//            alert.show();
//            return;
//        }
//
//        final BRWalletManager m = BRWalletManager.getInstance();
//        byte[] tmpTx = m.tryTransaction(addressHolder, bigDecimalAmount.longValue());
//        long feeForTx = m.feeForTransaction(addressHolder, bigDecimalAmount.longValue());
//
//        if (tmpTx == null && bigDecimalAmount.longValue() <= getBalance() && bigDecimalAmount.longValue() > 0) {
//            final long maxAmountDouble = m.getMaxOutputAmount();
//            if (maxAmountDouble == -1) {
//                RuntimeException ex = new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL");
//                FirebaseCrash.report(ex);
//                throw ex;
//            }
//            if (maxAmountDouble < getMinOutputAmount()) {
//                Log.e(TAG, "pay: FAIL: insufficient funds for fee.");
//                final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                builder.setmMessage("")
//                        .setmTitle(R.string.insufficient_funds_for_fee)
//                        .setCancelable(false)
//                        .setNegativeButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//                AlertDialog alert = builder.create();
//                alert.show();
//
//                return;
//            }
//
//            final long amountToReduce = bigDecimalAmount.longValue() - maxAmountDouble;
//            final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//            String reduceBits = BRStringFormatter.getFormattedCurrencyString(ctx,"BTC", amountToReduce);
//            String reduceFee = BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx), SharedPreferencesManager.getIso(ctx), new BigDecimal(amountToReduce), ctx);
//            String reduceBitsMinus = BRStringFormatter.getFormattedCurrencyString(ctx,"BTC", -amountToReduce);
//            String reduceFeeMinus = BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx), SharedPreferencesManager.getIso(ctx), new BigDecimal(-amountToReduce), ctx);
//
//            builder.setmMessage(String.format(ctx.getString(R.string.reduce_payment_amount_by), reduceBits, reduceFee))
//                    .setmTitle(R.string.insufficient_funds_for_fee)
//                    .setCancelable(false)
//                    .setNegativeButton(ctx.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                        @Override
//                        public void onClick(DialogInterface dialog, int which) {
//                            dialog.cancel();
//                        }
//                    })
//                    .setPositiveButton(String.format("%s (%s)", reduceBitsMinus, reduceFeeMinus), new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            byte[] tmpTx2 = m.tryTransaction(addressHolder, bigDecimalAmount.longValue() - amountToReduce);
//                            if (tmpTx2 != null) {
////                                PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx2);
//                                confirmPay(ctx, new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue() - amountToReduce, cn, tmpTx2, isAmountRequested));
//                            } else {
//                                Log.e(TAG, "tmpTxObject2 is null!");
//                                ((BreadWalletApp) ((Activity) ctx).getApplication()).showCustomToast(ctx, ctx.getString(R.string.insufficient_funds),
//                                        BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
//                            }
//                        }
//                    });
//            AlertDialog alert = builder.create();
//            alert.show();
//            alert.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
//            return;
//        } else if (tmpTx == null && bigDecimalAmount.longValue() >= getBalance() && bigDecimalAmount.longValue() > 0) {
//
////            FragmentScanResult.address = addressHolder;
//            if (!BreadWalletApp.unlocked) {
//                Log.e(TAG, "pay: FAIL: insufficient funds, but let the user auth first then tell");
//                //let it fail but the after the auth let the user know there is not enough money
//                confirmPay(ctx, new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue(), cn, null, isAmountRequested));
//                return;
//            } else {
//                Log.e(TAG, "pay: FAIL: offer To Change The Amount.");
//                BRWalletManager.getInstance().offerToChangeTheAmount(ctx, ctx.getString(R.string.insufficient_funds));
//                return;
//            }
//
//        }
//        PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx);
//        Log.d(TAG, "pay: feeForTx: " + feeForTx + ", amountAsDouble: " + bigDecimalAmount.longValue() +
//                ", CurrencyManager.getInstance(this).getBalance(): " + getBalance());
//        if ((feeForTx != 0 && bigDecimalAmount.longValue() + feeForTx < getBalance()) || (isAmountRequested && !BreadWalletApp.unlocked)) {
//            Log.d(TAG, "pay: SUCCESS: going to confirmPay");
//            confirmPay(ctx, new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue(), cn, tmpTx, isAmountRequested));
//        } else {
//            Log.d(TAG, "pay: FAIL: insufficient funds");
//            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//            builder.setmMessage(ctx.getString(R.string.insufficient_funds))
//                    .setCancelable(false)
//                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
//                        public void onClick(DialogInterface dialog, int id) {
//                            dialog.cancel();
//                        }
//                    });
//            AlertDialog alert = builder.create();
//            alert.show();
//        }

    }

//    public void askForPasscode(Context ctx) {
//        if (ctx == null) return;
//        final String pass = KeyStoreManager.getPinCode(ctx);
//        if (pass == null || pass.length() != 4) {
//            ((BreadWalletApp) ((Activity) ctx).getApplication()).authPrompt(ctx, BRConstants.AUTH_FOR_GENERAL, null, null, null, null, true);
//        }
//
//    }

    public void setUpTheWallet(final Context ctx) {
        Log.d(TAG, "setUpTheWallet...");
        Assert.assertNotNull(ctx);
        if (ctx == null) return;
        BRWalletManager m = BRWalletManager.getInstance();
        final BRPeerManager pm = BRPeerManager.getInstance();

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);

        if (!m.isCreated()) {
            List<BRTransactionEntity> transactions = sqLiteManager.getTransactions();
            int transactionsCount = transactions.size();
            if (transactionsCount > 0) {
                m.createTxArrayWithCount(transactionsCount);
                for (BRTransactionEntity entity : transactions) {
                    m.putTransaction(entity.getBuff(), entity.getBlockheight(), entity.getTimestamp());
                }
            }

            byte[] pubkeyEncoded = KeyStoreManager.getMasterPublicKey(ctx);

            //Save the first address for future check
            m.createWallet(transactionsCount, pubkeyEncoded);
            String firstAddress = BRWalletManager.getFirstAddress(pubkeyEncoded);
            SharedPreferencesManager.putFirstAddress(ctx, firstAddress);
            long fee = SharedPreferencesManager.getFeePerKb(ctx);
            if (fee == 0) fee = BRConstants.DEFAULT_FEE_PER_KB;
            BRWalletManager.getInstance().setFeePerKb(fee);
        }

        if (!pm.isCreated()) {
            List<BRMerkleBlockEntity> blocks = sqLiteManager.getBlocks();
            List<BRPeerEntity> peers = sqLiteManager.getPeers();
            final int blocksCount = blocks.size();
            final int peersCount = peers.size();
            if (blocksCount > 0) {
                pm.createBlockArrayWithCount(blocksCount);
                for (BRMerkleBlockEntity entity : blocks) {
                    pm.putBlock(entity.getBuff(), entity.getBlockHeight());
                }
            }
            if (peersCount > 0) {
                pm.createPeerArrayWithCount(peersCount);
                for (BRPeerEntity entity : peers) {
                    pm.putPeer(entity.getAddress(), entity.getPort(), entity.getTimeStamp());
                }
            }
            Log.d(TAG, "blocksCount before connecting: " + blocksCount);
            Log.d(TAG, "peersCount before connecting: " + peersCount);

            int walletTimeString = KeyStoreManager.getWalletCreationTime(ctx);
            Log.e(TAG, "setUpTheWallet: walletTimeString: " + walletTimeString);
            pm.create(walletTimeString, blocksCount, peersCount);

        }
        pm.connect();
        if (SharedPreferencesManager.getStartHeight(ctx) == 0)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SharedPreferencesManager.putStartHeight(ctx, BRPeerManager.getCurrentBlockHeight());
                }
            }).start();
    }

    public void generateQR(Context ctx, String bitcoinURL, ImageView qrcode) {
        if (qrcode == null || bitcoinURL == null || bitcoinURL.isEmpty()) return;
        WindowManager manager = (WindowManager) ctx.getSystemService(Activity.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = (int) (smallerDimension * 0.7f);
        Bitmap bitmap = null;
        try {
            bitmap = QRUtils.encodeAsBitmap(bitcoinURL, smallerDimension);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        qrcode.setPadding(1, 1, 1, 1);
        qrcode.setBackgroundResource(R.color.gray);
        qrcode.setImageBitmap(bitmap);

    }

    public void offerToChangeTheAmount(Context app, String title) {
//
//        new AlertDialog.Builder(app)
//                .setmTitle(title)
//                .setmMessage(R.string.change_payment_amount)
//                .setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//
//                        BRAnimator.animateScanResultFragment();
//                    }
//                }).setNegativeButton(app.getString(R.string.cancel), new DialogInterface.OnClickListener() {
//            @Override
//            public void onClick(DialogInterface dialog, int which) {
//                dialog.dismiss();
//            }
//        })
//                .setIcon(android.R.drawable.ic_dialog_alert)
//                .show();
    }

//    public void animateSavePhraseFlow() {
//        PhraseFlowActivity.screenParametersPoint = IntroActivity.screenParametersPoint;
//        if (PhraseFlowActivity.screenParametersPoint == null ||
//                PhraseFlowActivity.screenParametersPoint.y == 0 ||
//                PhraseFlowActivity.screenParametersPoint.x == 0)
//            PhraseFlowActivity.screenParametersPoint = MainActivity.screenParametersPoint;
//        Intent intent;
//        intent = new Intent(ctx, PhraseFlowActivity.class);
//        ctx.startActivity(intent);
//        if (!ctx.isDestroyed()) {
//            ctx.finish();
//        }
//    }

    private static void showSpendNotAllowed(final Context app) {
//        Log.d(TAG, "showSpendNotAllowed");
//        ((Activity) ctx).runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                AlertDialog.Builder builder = new AlertDialog.Builder(app);
//                builder.setmTitle(R.string.syncing_in_progress)
//                        .setmMessage(R.string.wait_for_sync_to_finish)
//                        .setNegativeButton(app.getString(R.string.ok), new DialogInterface.OnClickListener() {
//                            @Override
//                            public void onClick(DialogInterface dialog, int which) {
//                                dialog.cancel();
//                            }
//                        });
//                AlertDialog alert = builder.create();
//                alert.show();
//            }
//        });
    }


    public void startBreadActivity(Activity from) {
        Log.e(TAG, "startBreadActivity: from: " + from);
        Intent intent;
        intent = new Intent(from, PinActivity.class);
        from.startActivity(intent);
        if (!from.isDestroyed()) {
            from.finish();
        }
    }

    private native byte[] encodeSeed(byte[] seed, String[] wordList);

    public native void createWallet(int transactionCount, byte[] pubkey);

    public native void putTransaction(byte[] transaction, long blockHeight, long timeStamp);

    public native void createTxArrayWithCount(int count);

    public native byte[] getMasterPubKey(byte[] normalizedString);

    public static native String getReceiveAddress();

    public native TransactionListItem[] getTransactions();

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

    public native boolean publishSerializedTransaction(byte[] serializedTransaction, byte[] phrase);

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

    public native long getMinOutputAmountRequested();

    public static native byte[] getAuthPrivKeyForAPI(byte[] seed);

    public static native String getAuthPublicKeyForAPI(byte[] privKey);

    public static native byte[] getSeedFromPhrase(byte[] phrase);

    public static native boolean isTestNet();

}