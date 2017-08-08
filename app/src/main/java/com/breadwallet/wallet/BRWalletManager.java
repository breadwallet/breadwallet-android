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
import android.security.keystore.UserNotAuthenticatedException;
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
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.PhraseFlowActivity;
import com.breadwallet.presenter.activities.RequestQRActivity;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.MainFragmentQR;
import com.breadwallet.tools.threads.PaymentProtocolPostPaymentTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.BRNotificationManager;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.util.WordsReader;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.tools.threads.ImportPrivKeyTask;
import com.google.firebase.crash.FirebaseCrash;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import junit.framework.Assert;

import java.io.File;
import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import static com.breadwallet.presenter.activities.MainActivity.app;

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

public class BRWalletManager {
    private static final String TAG = BRWalletManager.class.getName();

    private static BRWalletManager instance;
    private static Activity ctx;
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;

    private static int messageId = 0;
    private boolean walletCreated;

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
        boolean success = false;
        byte[] keyBytes = new byte[0];
        byte[] strBytes = new byte[0];
        byte[] authKey = new byte[0];
        try {
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
            keyBytes = sr.generateSeed(16);
            if (words.length != 2048) {
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
            boolean b;
            try {
                b = KeyStoreManager.putPhrase(strPhrase, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
            } catch (UserNotAuthenticatedException e) {
                return false;
            }
            if (!b) return false;

            KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), ctx);
            strBytes = TypesConverter.getNullTerminatedPhrase(strPhrase);
            byte[] pubKey = BRWalletManager.getInstance(ctx).getMasterPubKey(strBytes);
            if (Utils.isNullOrEmpty(pubKey))
                throw new RuntimeException("pubkey is malformed: " + Arrays.toString(pubKey));
            KeyStoreManager.putMasterPublicKey(pubKey, ctx);

            authKey = getAuthPrivKeyForAPI(keyBytes);
            if (authKey == null || authKey.length == 0) {
                RuntimeException ex = new IllegalArgumentException("authKey is invalid");
                FirebaseCrash.report(ex);
                throw ex;
            }
            KeyStoreManager.putAuthKey(authKey, ctx);

            success = true;
        } finally {
            if (!success) {
                KeyStoreManager.resetWalletKeyStore(ctx);
                SharedPreferencesManager.clearAllPrefs(ctx);
            }
            Arrays.fill(keyBytes, (byte) 0);
            Arrays.fill(strBytes, (byte) 0);
            if (authKey != null)
                Arrays.fill(authKey, (byte) 0);
        }

        return true;
    }

    public boolean wipeKeyStore(Context context) {
        Log.e(TAG, "wipeKeyStore");
        return KeyStoreManager.resetWalletKeyStore(context);
    }

    /**
     * true if keystore is available and we know that no wallet exists on it
     */
    public boolean noWallet(Activity ctx) {
        if (ctx == null) throw new NullPointerException("noWallet ctx is null");
        if (isKeyStoreCorrupt(ctx)) return true;
        patchIfNeeded(ctx);
        byte[] pubkey = KeyStoreManager.getMasterPublicKey(ctx);

        if (pubkey == null || pubkey.length == 0) {
            byte[] phrase;
            try {
                phrase = KeyStoreManager.getPhrase(ctx, 0);
                if (phrase == null || phrase.length == 0) {
                    return true;
                }
            } catch (UserNotAuthenticatedException e) {
                e.printStackTrace();
                return false;
            }

        }
        return false;
    }

    private void patchIfNeeded(Activity ctx) {
        Log.d(TAG, "patchIfNeeded: ");
        byte[] phrase;
        try {
            phrase = KeyStoreManager.getPhrase(ctx, 0);
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
            return;
        }
        byte[] strBytes = TypesConverter.getNullTerminatedPhrase(phrase);

        if (Utils.isNullOrEmpty(KeyStoreManager.getMasterPublicKey(ctx))) {
            Log.e(TAG, "patchIfNeeded: missing pubKey");
            KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), ctx);
            byte[] pubKey = BRWalletManager.getInstance(ctx).getMasterPubKey(strBytes);
            if (Utils.isNullOrEmpty(pubKey))
                throw new RuntimeException("pubkey is malformed: " + Arrays.toString(pubKey));
            KeyStoreManager.putMasterPublicKey(pubKey, ctx);
            SQLiteManager.getInstance(ctx).deleteBlocks();
            SQLiteManager.getInstance(ctx).deleteTransactions();
            SQLiteManager.getInstance(ctx).deletePeers();
        }

        if (Utils.isNullOrEmpty(KeyStoreManager.getAuthKey(ctx))) {
            Log.e(TAG, "patchIfNeeded: missing authKey");
            byte[] seed = getSeedFromPhrase(strBytes);
            byte[] authKey = getAuthPrivKeyForAPI(seed);
            if (authKey == null || authKey.length == 0) {
                RuntimeException ex = new IllegalArgumentException("authKey is invalid");
                FirebaseCrash.report(ex);
                throw ex;
            }
            KeyStoreManager.putAuthKey(authKey, ctx);
        }
    }

    private boolean isKeyStoreCorrupt(Activity ctx) {
        try {
            List<String> aliases = new ArrayList<>();
            aliases.add(KeyStoreManager.PHRASE_ALIAS);
            for (String alias : aliases) {
                KeyStoreManager.AliasObject obj = KeyStoreManager.aliasObjectMap.get(alias);
                boolean fileExists = new File(KeyStoreManager.getEncryptedDataFilePath(obj.datafileName, ctx)).exists();
                boolean ivExists = new File(KeyStoreManager.getEncryptedDataFilePath(obj.ivFileName, ctx)).exists();
                if (!fileExists || !ivExists) {
                    Log.e(TAG, "isKeyStoreCorrupt: KS corrupt for: " + alias + ", fileExists: " + fileExists + ", ivExists: " + ivExists);
                    return true;
                }
            }
        } catch (Exception ex) {
            Log.e(TAG, "isKeyStoreCorrupt: KS corrupt with exception: " + ex.getMessage());
            return true;
        }
        return false;
    }

    public boolean noWalletForPlatform(Activity ctx) {
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

    //BLOCKS
    public static void refreshAddress() {
        if (ctx == null) ctx = app;
        if (ctx != null) {
            new Thread(new Runnable() {
                @Override
                public void run() {
                    final MainFragmentQR mainFragmentQR = CustomPagerAdapter.adapter == null ? null : CustomPagerAdapter.adapter.mainFragmentQR;
                    final String tmpAddr = getReceiveAddress();
                    Log.e(TAG, "run: got address: " + tmpAddr);
                    if (tmpAddr == null || tmpAddr.isEmpty()) return;
                    SharedPreferencesManager.putReceiveAddress(ctx, tmpAddr);
                    if (mainFragmentQR == null) return;
                    ctx.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            mainFragmentQR.refreshAddress(tmpAddr);
                        }
                    });

                }
            }).start();
        } else {
            RuntimeException ex = new NullPointerException("Cannot be null");
            FirebaseCrash.report(ex);
            throw ex;
        }
    }

    public void wipeWalletButKeystore(final Activity activity) {
        Log.e(TAG, "wipeWalletButKeystore");
        new Thread(new Runnable() {
            @Override
            public void run() {
                BRPeerManager.getInstance(activity).peerManagerFreeEverything();
                walletFreeEverything();
            }
        }).start();

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(activity);
        sqLiteManager.deleteTransactions();
        sqLiteManager.deleteBlocks();
        sqLiteManager.deletePeers();
        SharedPreferencesManager.clearAllPrefs(activity);
    }

    public boolean confirmSweep(final Activity activity, final String privKey) {
        if (activity == null) return false;
        if (isValidBitcoinBIP38Key(privKey)) {
            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//                    builder.setTitle("password protected key");

                    final View input = activity.getLayoutInflater().inflate(R.layout.view_bip38password_dialog, null);
                    // Specify the type of input expected; this, for example, sets the input as a password, and will mask the text
                    builder.setView(input);

                    final EditText editText = (EditText) input.findViewById(R.id.bip38password_edittext);

                    (new Handler()).postDelayed(new Runnable() {
                        public void run() {
                            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_DOWN, 0, 0, 0));
                            editText.dispatchTouchEvent(MotionEvent.obtain(SystemClock.uptimeMillis(), SystemClock.uptimeMillis(), MotionEvent.ACTION_UP, 0, 0, 0));

                        }
                    }, 100);

                    // Set up the buttons
                    builder.setPositiveButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
                            if (!((BreadWalletApp) activity.getApplication()).hasInternetAccess()) {
                                ctx.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((BreadWalletApp) activity.getApplication()).showCustomDialog(activity.getString(R.string.warning),
                                                activity.getString(R.string.not_connected), activity.getString(R.string.ok));
                                    }
                                });

                                return;
                            }
                            if (ctx != null)
                                ctx.runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        ((BreadWalletApp) ctx.getApplication()).showCustomToast(ctx,
                                                activity.getString(R.string.checking_privkey_balance), MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 1);
                                    }
                                });
                            if (editText == null) return;

                            String pass = editText.getText().toString();
                            String decryptedKey = decryptBip38Key(privKey, pass);

                            if (decryptedKey.equals("")) {
                                SpringAnimator.showAnimation(input);
                                confirmSweep(activity, privKey);
                            } else {
                                confirmSweep(activity, decryptedKey);
                            }

                        }
                    });
                    builder.setNegativeButton(activity.getString(R.string.cancel), new DialogInterface.OnClickListener() {
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
            Log.d(TAG, "isValidBitcoinPrivateKey true");
            new ImportPrivKeyTask(activity).execute(privKey);
            return true;
        } else {
//            Log.e(TAG, "confirmSweep: !isValidBitcoinPrivateKey && !isValidBitcoinBIP38Key");
            return false;
        }
    }

    public static void showWritePhraseDialog(final boolean firstTime) {

        if (ctx == null) ctx = app;
        if (ctx != null) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean phraseWroteDown = SharedPreferencesManager.getPhraseWroteDown(ctx);
                    if (phraseWroteDown) return;
                    long now = System.currentTimeMillis() / 1000;
                    long lastMessageShow = SharedPreferencesManager.getPhraseWarningTime(ctx);
                    if (lastMessageShow == 0 || (!firstTime && lastMessageShow > (now - 36 * 60 * 60)))
                        return;//36 * 60 * 60//
                    if (CurrencyManager.getInstance(ctx).getBALANCE() > SharedPreferencesManager.getLimit(ctx)) {
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
                                            BRWalletManager.getInstance(ctx).animateSavePhraseFlow();
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

    public static void publishCallback(final String message, int error) {
        PaymentProtocolPostPaymentTask.waiting = false;
        if (error != 0) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    if (!PaymentProtocolPostPaymentTask.waiting && !PaymentProtocolPostPaymentTask.sent) {

                        if (PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.MESSAGE) != null) {
                            ((BreadWalletApp) ctx.getApplication()).
                                    showCustomDialog(PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.TITLE),
                                            PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.MESSAGE), ctx.getString(R.string.ok));
                            PaymentProtocolPostPaymentTask.pendingErrorMessages = null;
                        } else {
                            ((BreadWalletApp) ctx.getApplication()).showCustomToast(ctx, message,
                                    MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 1);
                        }
                    }
                }
            });
        } else {
            PaymentProtocolPostPaymentTask.sent = true;
        }
    }

    public static void onBalanceChanged(final long balance) {

//        Log.d(TAG, "onBalanceChanged:  " + balance);
        if (ctx == null) ctx = app;
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
//        Log.d(TAG, "onTxAdded: " + String.format("tx.length: %d, blockHeight: %d, timestamp: %d, amount: %d, hash: %s", tx.length, blockHeight, timestamp, amount, hash));
        final RequestQRActivity requestApp = RequestQRActivity.requestApp;
        if (requestApp != null && !requestApp.activityIsInBackground) {
            requestApp.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    requestApp.close.performClick();
                }
            });
        }
        if (ctx == null) ctx = app;
        if (ctx != null) {

            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    long absAmount = (amount > 0 ? amount : amount * -1);
                    String strToShow = amount > 0 ?
                            (String.format(ctx.getString(R.string.received_amount),
                                    BRStringFormatter.getFormattedCurrencyString("BTC", absAmount),
                                    BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx),
                                            SharedPreferencesManager.getIso(ctx), new BigDecimal(absAmount), ctx))) :
                            ctx.getString(R.string.sent_exclaimed);

                    showSentReceivedToast(strToShow);
                }

            });

        }

        if (getInstance(ctx).getTxCount() <= 1) {
            SharedPreferencesManager.putPhraseWarningTime(ctx, System.currentTimeMillis() / 1000);
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            showWritePhraseDialog(true);
                        }
                    }, 2000);
                }
            });

        }

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);
        sqLiteManager.insertTransaction(tx, blockHeight, timestamp, hash);
    }

    private static void showSentReceivedToast(final String message) {
        messageId++;
        if (ctx == null) ctx = app;
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
                                    AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                                    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                        final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.coinflip);
                                        mp.start();

                                    }
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
        Log.d(TAG, "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
        if (ctx == null) ctx = app;
        if (ctx != null) {
            SQLiteManager.getInstance(ctx).updateTxByHash(hash, blockHeight, timeStamp);
        }
    }

    public static void onTxDeleted(String hash, int notifyUser, final int recommendRescan) {
        Log.e(TAG, "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d", hash, notifyUser, recommendRescan));

        if (ctx == null) ctx = app;
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
                                            if (BRAnimator.checkTheMultipressingAvailability()) {
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
        if (words.length != 2048) {
            RuntimeException ex = new IllegalArgumentException("words.length is not 2048");
            FirebaseCrash.report(ex);
            throw ex;
        }
        return validateRecoveryPhrase(cleanWordList, phrase);
    }

    public void confirmPay(final PaymentRequestEntity request) {
        if (((BreadWalletApp) ctx.getApplication()).hasInternetAccess()) {
            if (ctx == null) ctx = app;
            if (ctx == null) return;

            new Thread(new Runnable() {
                @Override
                public void run() {
                    boolean certified = false;
                    if (request.cn != null && request.cn.length() != 0) {
                        certified = true;
                    }
                    StringBuilder allAddresses = new StringBuilder();
                    for (String s : request.addresses) {
                        allAddresses.append(s + ", ");
                    }
                    allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
                    String certification = "";
                    if (certified) {
                        certification = "certified: " + request.cn + "\n";
                        allAddresses = new StringBuilder();
                    }

                    //DecimalFormat decimalFormat = new DecimalFormat("0.00");
                    String iso = SharedPreferencesManager.getIso(ctx);

                    float rate = SharedPreferencesManager.getRate(ctx);
                    BRWalletManager m = getInstance(ctx);
                    long feeForTx = m.feeForTransaction(request.addresses[0], request.amount);
                    if (feeForTx == 0) {
                        long maxAmountDouble = m.getMaxOutputAmount();
                        if (maxAmountDouble == -1) {
                            RuntimeException ex = new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL");
                            FirebaseCrash.report(ex);
                            return;
//                            throw ex;
                        }
                        if (maxAmountDouble == 0) {
                            ctx.runOnUiThread(new Runnable() {
                                @Override
                                public void run() {
                                    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                                    builder.setMessage("")
                                            .setTitle(R.string.insufficient_funds_for_fee)
                                            .setCancelable(false)
                                            .setNegativeButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
                                                @Override
                                                public void onClick(DialogInterface dialog, int which) {
                                                    dialog.cancel();
                                                }
                                            });
                                    AlertDialog alert = builder.create();
                                    alert.show();
                                }
                            });
                            return;
                        }
                        feeForTx = m.feeForTransaction(request.addresses[0], maxAmountDouble);
                        feeForTx += (CurrencyManager.getInstance(ctx).getBALANCE() - request.amount) % 100;
                    }
                    final long total = request.amount + feeForTx;
                    final String message = certification + allAddresses.toString() + "\n\n" + "amount: " + BRStringFormatter.getFormattedCurrencyString("BTC", request.amount)
                            + " (" + BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(request.amount), ctx) + ")" + "\nnetwork fee: +" + BRStringFormatter.getFormattedCurrencyString("BTC", feeForTx)
                            + " (" + BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(feeForTx), ctx) + ")" + "\ntotal: " + BRStringFormatter.getFormattedCurrencyString("BTC", total)
                            + " (" + BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(total), ctx) + ")";

                    double minOutput;
                    if (request.isAmountRequested) {
                        minOutput = BRWalletManager.getInstance(ctx).getMinOutputAmountRequested();
                    } else {
                        minOutput = BRWalletManager.getInstance(ctx).getMinOutputAmount();
                    }
                    if (request.amount < minOutput) {
                        final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.bitcoin_payment_cant_be_less),
                                BRConstants.bitcoinLowercase + new BigDecimal(minOutput).divide(new BigDecimal("100")));
                        ctx.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                new android.app.AlertDialog.Builder(ctx)
                                        .setTitle(ctx.getString(R.string.payment_failed))
                                        .setMessage(bitcoinMinMessage)
                                        .setPositiveButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.dismiss();
                                            }
                                        })
                                        .setIcon(android.R.drawable.ic_dialog_alert)
                                        .show();
                            }
                        });
                        return;
                    }

                    ctx.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) ctx.getApplicationContext()).promptForAuthentication(ctx,
                                    BRConstants.AUTH_FOR_PAY, request, message, "", null, false);
                        }
                    });
                }
            }).start();


        } else {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    builder.setMessage(R.string.not_connected)
                            .setCancelable(false)
                            .setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
                                public void onClick(DialogInterface dialog, int id) {
                                    dialog.cancel();
                                }
                            });
                    AlertDialog alert = builder.create();
                    alert.show();
                }
            });

        }

    }

    public void pay(final String addressHolder, final BigDecimal bigDecimalAmount, final String cn, final boolean isAmountRequested) {
        Log.e(TAG, "pay: " + String.format("addressHolder: %s, bigDecimalAmount: %s, cn: %s, isAmountRequested: %b", addressHolder, bigDecimalAmount == null ? null : bigDecimalAmount.toPlainString(), cn, isAmountRequested));
        if (addressHolder == null || bigDecimalAmount == null) return;
        if (addressHolder.length() < 20) return;
        if (!SharedPreferencesManager.getAllowSpend(app)) {
            showSpendNotAllowed(app);
            return;
        }

        new Thread(new Runnable() {
            @Override
            public void run() {
                int unit = BRConstants.CURRENT_UNIT_BITS;
                Activity context = app;
                String divideBy = "100";
                if (context != null)
                    unit = SharedPreferencesManager.getCurrencyUnit(context);
                if (unit == BRConstants.CURRENT_UNIT_MBITS) divideBy = "100000";
                if (unit == BRConstants.CURRENT_UNIT_BITCOINS) divideBy = "100000000";
//        final long amountAsLong = bigDecimal.longValue();
                if (bigDecimalAmount.longValue() < 0) return;
                final CurrencyManager cm = CurrencyManager.getInstance(ctx);
                final long minAmount = getMinOutputAmountRequested();
                if (bigDecimalAmount.longValue() < minAmount) {
                    Log.e(TAG, "pay: FAIL: bitcoin payment is less then the minimum.");
                    final String finalDivideBy = divideBy;
                    ctx.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.bitcoin_payment_cant_be_less),
                                    BRConstants.bitcoinLowercase + new BigDecimal(minAmount).divide(new BigDecimal(finalDivideBy)));
                            builder.setMessage(bitcoinMinMessage)
                                    .setTitle(R.string.could_not_make_payment)
                                    .setCancelable(false)
                                    .setNegativeButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });
                    return;
                }

                final BRWalletManager m = BRWalletManager.getInstance(ctx);
                byte[] tmpTx = m.tryTransaction(addressHolder, bigDecimalAmount.longValue());
                long feeForTx = m.feeForTransaction(addressHolder, bigDecimalAmount.longValue());

                if (tmpTx == null && bigDecimalAmount.longValue() <= cm.getBALANCE() && bigDecimalAmount.longValue() > 0) {
                    final long maxAmountDouble = m.getMaxOutputAmount();
                    if (maxAmountDouble == -1) {
                        RuntimeException ex = new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL");
                        FirebaseCrash.report(ex);
                        throw ex;
                    }
                    if (maxAmountDouble < getMinOutputAmount()) {
                        Log.e(TAG, "pay: FAIL: insufficient funds for fee.");
                        ctx.runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                                builder.setMessage("")
                                        .setTitle(R.string.insufficient_funds_for_fee)
                                        .setCancelable(false)
                                        .setNegativeButton(ctx.getString(R.string.ok), new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialog, int which) {
                                                dialog.cancel();
                                            }
                                        });
                                AlertDialog alert = builder.create();
                                alert.show();
                            }
                        });

                        return;
                    }

                    final long amountToReduce = bigDecimalAmount.longValue() - maxAmountDouble;
                    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                    final String reduceBits = BRStringFormatter.getFormattedCurrencyString("BTC", amountToReduce);
                    final String reduceFee = BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx), SharedPreferencesManager.getIso(ctx), new BigDecimal(amountToReduce), ctx);
                    final String reduceBitsMinus = BRStringFormatter.getFormattedCurrencyString("BTC", -amountToReduce);
                    final String reduceFeeMinus = BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx), SharedPreferencesManager.getIso(ctx), new BigDecimal(-amountToReduce), ctx);

                    ctx.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            builder.setMessage(String.format(ctx.getString(R.string.reduce_payment_amount_by), reduceBits, reduceFee))
                                    .setTitle(R.string.insufficient_funds_for_fee)
                                    .setCancelable(false)
                                    .setNegativeButton(ctx.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                                        @Override
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    })
                                    .setPositiveButton(String.format("%s (%s)", reduceBitsMinus, reduceFeeMinus), new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            new Thread(new Runnable() {
                                                @Override
                                                public void run() {
                                                    final byte[] tmpTx2 = m.tryTransaction(addressHolder, bigDecimalAmount.longValue() - amountToReduce);
                                                    ctx.runOnUiThread(new Runnable() {
                                                        @Override
                                                        public void run() {
                                                            if (tmpTx2 != null) {
                                                                PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx2);
                                                                confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue() - amountToReduce, cn, tmpTx2, isAmountRequested));
                                                            } else {
                                                                Log.e(TAG, "tmpTxObject2 is null!");

                                                                ((BreadWalletApp) ctx.getApplication()).showCustomToast(ctx, ctx.getString(R.string.insufficient_funds),
                                                                        MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
                                                            }
                                                        }
                                                    });

                                                }
                                            }).start();

                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                            alert.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                        }
                    });

                    return;
                } else if (tmpTx == null && bigDecimalAmount.longValue() >= cm.getBALANCE() && bigDecimalAmount.longValue() > 0) {

                    FragmentScanResult.address = addressHolder;
                    if (!BreadWalletApp.unlocked) {
                        Log.e(TAG, "pay: FAIL: insufficient funds, but let the user auth first then tell");
                        //let it fail but the after the auth let the user know there is not enough money
                        confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue(), cn, null, isAmountRequested));
                        return;
                    } else {
                        Log.e(TAG, "pay: FAIL: offer To Change The Amount.");
                        BRWalletManager.getInstance(ctx).offerToChangeTheAmount(ctx, ctx.getString(R.string.insufficient_funds));
                        return;
                    }

                }
                PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx);
                Log.d(TAG, "pay: feeForTx: " + feeForTx + ", amountAsDouble: " + bigDecimalAmount.longValue() +
                        ", CurrencyManager.getInstance(this).getBALANCE(): " + cm.getBALANCE());
                if ((feeForTx != 0 && bigDecimalAmount.longValue() + feeForTx < cm.getBALANCE()) || (isAmountRequested && !BreadWalletApp.unlocked)) {
                    Log.d(TAG, "pay: SUCCESS: going to confirmPay");
                    confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue(), cn, tmpTx, isAmountRequested));
                } else {
                    Log.d(TAG, "pay: FAIL: insufficient funds");
                    ctx.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                            builder.setMessage(ctx.getString(R.string.insufficient_funds))
                                    .setCancelable(false)
                                    .setPositiveButton("ok", new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int id) {
                                            dialog.cancel();
                                        }
                                    });
                            AlertDialog alert = builder.create();
                            alert.show();
                        }
                    });

                }
            }
        }).start();


    }

    public void askForPasscode() {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return;
        final String pass = KeyStoreManager.getPassCode(ctx);
        if (pass == null || pass.length() != 4) {
            ((BreadWalletApp) ctx.getApplication()).promptForAuthentication(ctx, BRConstants.AUTH_FOR_GENERAL, null, null, null, null, true);
        }

    }

    public void setUpTheWallet(final Activity ctx) {
        Log.d(TAG, "setUpTheWallet...");
        synchronized (this) {
            if (walletCreated) return; //return if the wallet was already created
        }
        Assert.assertNotNull(ctx);
        BRWalletManager m = BRWalletManager.getInstance(ctx);
        final BRPeerManager pm = BRPeerManager.getInstance(ctx);

        SQLiteManager sqLiteManager = SQLiteManager.getInstance(ctx);

        List<BRTransactionEntity> transactions = sqLiteManager.getTransactions();
        int transactionsCount = transactions.size();
        if (transactionsCount > 0) {
            m.createTxArrayWithCount(transactionsCount);
            for (BRTransactionEntity entity : transactions) {
                m.putTransaction(entity.getBuff(), entity.getBlockheight(), entity.getTimestamp());
            }
        }

        byte[] pubkeyEncoded = KeyStoreManager.getMasterPublicKey(ctx);
        if (pubkeyEncoded == null || pubkeyEncoded.length == 0)
            throw new RuntimeException("pubkey is malformed: " + Arrays.toString(pubkeyEncoded));

        //Save the first address for future check
        m.createWallet(transactionsCount, pubkeyEncoded);
        String firstAddress = BRWalletManager.getFirstAddress(pubkeyEncoded);
        SharedPreferencesManager.putFirstAddress(ctx, firstAddress);
        long fee = SharedPreferencesManager.getFeePerKb(ctx);
        if (fee == 0) fee = BRConstants.DEFAULT_FEE_PER_KB;
        BRWalletManager.getInstance(ctx).setFeePerKb(fee);

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
//        Log.d(TAG, "blocksCount before connecting: " + blocksCount);
//        Log.d(TAG, "peersCount before connecting: " + peersCount);

        int walletTimeString = KeyStoreManager.getWalletCreationTime(ctx);
        Log.e(TAG, "setUpTheWallet: walletTimeString: " + walletTimeString);
        pm.create(walletTimeString, blocksCount, peersCount);


        BRPeerManager.getInstance(ctx).updateFixedPeer();
        pm.connect();
        setWalletCreated(true);
        if (SharedPreferencesManager.getStartHeight(ctx) == 0)
            new Thread(new Runnable() {
                @Override
                public void run() {
                    SharedPreferencesManager.putStartHeight(ctx, BRPeerManager.getCurrentBlockHeight());
                }
            }).start();

    }

    public void generateQR(String bitcoinURL, ImageView qrcode) {
        if (qrcode == null || bitcoinURL == null || bitcoinURL.isEmpty()) return;
        if (ctx == null) ctx = app;
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
            bitmap = encodeAsBitmap(bitcoinURL, smallerDimension);
        } catch (WriterException e) {
            e.printStackTrace();
        }
        qrcode.setPadding(1, 1, 1, 1);
        qrcode.setBackgroundResource(R.color.gray);
        qrcode.setImageBitmap(bitmap);

    }

    private Bitmap encodeAsBitmap(String content, int dimension) throws WriterException {

        if (content == null) {
            return null;
        }
        Map<EncodeHintType, Object> hints = null;
        String encoding = guessAppropriateEncoding(content);
        hints = new EnumMap<>(EncodeHintType.class);
        if (encoding != null) {
            hints.put(EncodeHintType.CHARACTER_SET, encoding);
        }
        hints.put(EncodeHintType.MARGIN, 1);
        BitMatrix result;
        try {
            result = new MultiFormatWriter().encode(content, BarcodeFormat.QR_CODE, dimension, dimension, hints);
        } catch (IllegalArgumentException iae) {
            // Unsupported format
            return null;
        }
        int width = result.getWidth();
        int height = result.getHeight();
        int[] pixels = new int[width * height];
        for (int y = 0; y < height; y++) {
            int offset = y * width;
            for (int x = 0; x < width; x++) {
                pixels[offset + x] = result.get(x, y) ? BLACK : WHITE;
            }
        }

        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        bitmap.setPixels(pixels, 0, width, 0, 0, width, height);
        return bitmap;
    }

    private static String guessAppropriateEncoding(CharSequence contents) {
        // Very crude at the moment
        for (int i = 0; i < contents.length(); i++) {
            if (contents.charAt(i) > 0xFF) {
                return "UTF-8";
            }
        }
        return null;
    }

    public void offerToChangeTheAmount(final Activity app, final String title) {

        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new AlertDialog.Builder(app)
                        .setTitle(title)
                        .setMessage(R.string.change_payment_amount)
                        .setPositiveButton(R.string.change, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {

                                BRAnimator.animateScanResultFragment();
                            }
                        }).setNegativeButton(app.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.dismiss();
                    }
                })
                        .setIcon(android.R.drawable.ic_dialog_alert)
                        .show();
            }
        });

    }

    public void animateSavePhraseFlow() {
        PhraseFlowActivity.screenParametersPoint = IntroActivity.screenParametersPoint;
        if (PhraseFlowActivity.screenParametersPoint == null ||
                PhraseFlowActivity.screenParametersPoint.y == 0 ||
                PhraseFlowActivity.screenParametersPoint.x == 0)
            PhraseFlowActivity.screenParametersPoint = MainActivity.screenParametersPoint;
        Intent intent;
        intent = new Intent(ctx, PhraseFlowActivity.class);
        ctx.startActivity(intent);
        if (!ctx.isDestroyed()) {
            ctx.finish();
        }
    }

    private static void showSpendNotAllowed(final MainActivity app) {
        Log.d(TAG, "showSpendNotAllowed");
        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                AlertDialog.Builder builder = new AlertDialog.Builder(app);
                builder.setTitle(R.string.syncing_in_progress)
                        .setMessage(R.string.wait_for_sync_to_finish)
                        .setNegativeButton(app.getString(R.string.ok), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
            }
        });
    }

    public boolean isWalletCreated() {
        return walletCreated;
    }

    public void setWalletCreated(boolean walletCreated) {
        Log.e(TAG, "setWalletCreated to " + walletCreated);
        this.walletCreated = walletCreated;
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

//    public native boolean isCreated();

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

    public static native byte[] sweepBCash(byte[] pubKey, String address, byte[] phrase);

    public static native long getBCashBalance(byte[] pubKey);

}