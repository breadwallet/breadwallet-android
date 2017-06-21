package com.breadwallet.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.Context;
import android.content.DialogInterface;
import android.graphics.Bitmap;
import android.graphics.Point;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.qrcode.QRUtils;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.threads.ImportPrivKeyTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.BRNotificationManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.tools.util.Bip39Reader;
import com.breadwallet.tools.security.KeyStoreManager;
import com.google.firebase.crash.FirebaseCrash;

import junit.framework.Assert;

import java.io.IOException;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.List;

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
    private static final int WHITE = 0xFFFFFFFF;
    private static final int BLACK = 0xFF000000;
    public List<OnBalanceChanged> balanceListeners;


    public void setBalance(Context context, long balance) {
        if (context == null) {
            Log.e(TAG, "setBalance: FAILED TO SET THE BALANCE");
            return;
        }
        SharedPreferencesManager.putCatchedBalance(context, balance);

        refreshAddress(context);
        for (OnBalanceChanged listener : balanceListeners) {
            if (listener != null) listener.onBalanceChanged(balance);
        }
    }

    public long getBalance(Context context) {
        return SharedPreferencesManager.getCatchedBalance(context);
    }

    private static int messageId = 0;

    private BRWalletManager() {
        balanceListeners = new ArrayList<>();
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
            String languageCode = ctx.getString(R.string.lang_Android);
            list = Bip39Reader.getWordList(ctx, languageCode);
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
     * true if keystore is available and we know that no wallet exists on it
     */
    public boolean noWallet(Context ctx) {
        byte[] pubkey = KeyStoreManager.getMasterPublicKey(ctx);

        if (pubkey == null || pubkey.length == 0) {
            byte[] phrase;
            try {
                phrase = KeyStoreManager.getKeyStorePhrase(ctx, 0);
                //if not authenticated, an error will be thrown and returned false, so no worry about mistakenly removing the wallet
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

    public boolean isNetworkAvailable(Context ctx) {
        if (ctx == null) return false;
        ConnectivityManager cm = (ConnectivityManager) ctx.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();

    }

    //BLOCKS
    public static boolean refreshAddress(Context ctx) {
        String address = getReceiveAddress();
        if (Utils.isNullOrEmpty(address)) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
            return false;
        }
        SharedPreferencesManager.putReceiveAddress(ctx, address);
        return true;

    }

    public boolean isPaperKeyWritten(Context context) {
        return SharedPreferencesManager.getPhraseWroteDown(context);
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

        TransactionDataSource.getInstance(ctx).deleteAllTransactions();
        MerkleBlockDataSource.getInstance(ctx).deleteAllBlocks();
        PeerDataSource.getInstance(ctx).deleteAllPeers();
        SharedPreferencesManager.clearAllPrefs(ctx);
    }

    public boolean confirmSweep(final Context ctx, final String privKey) {
        if (ctx == null) return false;
        if (isValidBitcoinBIP38Key(privKey)) {
            Log.d(TAG, "isValidBitcoinBIP38Key true");
            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {

                    final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                    builder.setTitle("password protected key");

                    final View input = ((Activity) ctx).getLayoutInflater().inflate(R.layout.view_bip38password_dialog, null);
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
                    builder.setPositiveButton(ctx.getString(R.string.Button_ok), new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialog, int which) {
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
                            if (ctx != null)
                                ((Activity) ctx).runOnUiThread(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRToast.showCustomToast(ctx, ctx.getString(R.string.BRWalletManager_checkingPrivKeyBalance_Android), 500, Toast.LENGTH_LONG, R.drawable.toast_layout_blue);
                                    }
                                });
                            if (editText == null) {
                                Log.e(TAG, "onClick: edit text is null!");
                                return;
                            }

                            final String pass = editText.getText().toString();
                            Log.e(TAG, "onClick: before");
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
                                    String decryptedKey = decryptBip38Key(privKey, pass);
                                    Log.e(TAG, "onClick: after");

                                    if (decryptedKey.equals("")) {
                                        SpringAnimator.springView(input);
                                        confirmSweep(ctx, privKey);
                                    } else {
                                        confirmSweep(ctx, decryptedKey);
                                    }
                                }
                            }).start();


                        }
                    });
                    builder.setNegativeButton(ctx.getString(R.string.Button_cancel), new DialogInterface.OnClickListener() {
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
            new ImportPrivKeyTask(((Activity) ctx)).execute(privKey);
            return true;
        } else {
            Log.e(TAG, "confirmSweep: !isValidBitcoinPrivateKey && !isValidBitcoinBIP38Key");
            return false;
        }
    }

//    public static void showWritePhraseDialog(final Context ctx, final boolean firstTime) {
//
//        if (ctx != null) {
//            ((Activity) ctx).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    boolean phraseWroteDown = SharedPreferencesManager.getPhraseWroteDown(ctx);
//                    if (phraseWroteDown) return;
//                    long now = System.currentTimeMillis() / 1000;
//                    long lastMessageShow = SharedPreferencesManager.getPhraseWarningTime(ctx);
//                    if (lastMessageShow == 0 || (!firstTime && lastMessageShow > (now - 36 * 60 * 60)))
//                        return;//36 * 60 * 60//
//                    if (BRWalletManager.getInstance().getBalance(ctx) > SharedPreferencesManager.getTotalLimit(ctx)) {
////                        getInstance(ctx).animateSavePhraseFlow();
//                        return;
//                    }
//                    SharedPreferencesManager.putPhraseWarningTime(ctx, System.currentTimeMillis() / 1000);
//                    AlertDialog alert;
//                    AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
//                    builder.setTitle(ctx.getString(R.string.you_received_bitcoin));
//                    builder.setMessage(String.format(ctx.getString(R.string.write_down_phrase),
//                            ctx.getString(R.string.write_down_phrase_holder1)));
//                    builder.setPositiveButton(ctx.getString(R.string.show_phrase),
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(final DialogInterface dialog, int which) {
//                                    new Thread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            dialog.dismiss();
////                                            BRWalletManager.getInstance().animateSavePhraseFlow();
//                                        }
//                                    }).start();
//                                }
//                            });
//                    builder.setNegativeButton(ctx.getString(R.string.ok),
//                            new DialogInterface.OnClickListener() {
//                                public void onClick(DialogInterface dialog, int which) {
//                                    dialog.dismiss();
//                                }
//                            });
//                    builder.setCancelable(false);
//                    alert = builder.create();
//                    alert.show();
//                }
//            });
//
//        }
//
//    }

    /**
     * Wallet callbacks
     */
    public static void publishCallback(final String message, final int error) {
        Log.e(TAG, "publishCallback: " + message + ", err:" + error);
        final Activity app = BreadWalletApp.getBreadContext();
        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BRAnimator.showBreadSignal(app, error == 0 ? "Send Confirmation" : "Error",
                                error == 0 ? "Money Sent!" : message, error == 0 ? R.drawable.ic_check_mark_white : R.drawable.ic_error_outline_black_24dp, new BROnSignalCompletion() {
                                    @Override
                                    public void onComplete() {
                                        if (app != null && !app.isDestroyed())
                                            app.getFragmentManager().popBackStack();
                                    }
                                });
                    }
                }, 500);

            }
        });

//        PaymentProtocolPostPaymentTask.waiting = false;
//        if (error != 0) {
//            if (!PaymentProtocolPostPaymentTask.waiting && !PaymentProtocolPostPaymentTask.sent) {
//                if (PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.MESSAGE) != null) {
//                    BreadDialog.showCustomDialog(ctx, PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.TITLE),
//                            PaymentProtocolPostPaymentTask.pendingErrorMessages.get(PaymentProtocolPostPaymentTask.MESSAGE), ctx.getString(R.string.ok));
//                    PaymentProtocolPostPaymentTask.pendingErrorMessages = null;
//                } else {
//                    BRToast.showCustomToast(BreadActivity.app, message,
//                            BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, R.drawable.toast_layout_black);
//                }
//            }
//        } else {
//            PaymentProtocolPostPaymentTask.sent = true;
//        }

    }

    public static void onBalanceChanged(final long balance) {
        Log.d(TAG, "onBalanceChanged:  " + balance);
        Activity app = BreadWalletApp.getBreadContext();
        BRWalletManager.getInstance().setBalance(app, balance);

    }

    public static void onTxAdded(byte[] tx, int blockHeight, long timestamp, final long amount, String hash) {
        Log.d(TAG, "onTxAdded: " + String.format("tx.length: %d, blockHeight: %d, timestamp: %d, amount: %d, hash: %s", tx.length, blockHeight, timestamp, amount, hash));

//        if (getInstance().getTxCount() <= 1) {
//            SharedPreferencesManager.putPhraseWarningTime(ctx, System.currentTimeMillis() / 1000);
//            ctx.runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            showWritePhraseDialog(ctx, true);
//                        }
//                    }, 2000);
//                }
//            });
//
//        }
        Activity ctx = BreadWalletApp.getBreadContext();
        if (ctx != null)
            TransactionDataSource.getInstance(ctx).putTransaction(new BRTransactionEntity(tx, blockHeight, timestamp, hash));
        else
            Log.e(TAG, "onTxAdded: ctx is null!");
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
                                if (BRToast.isToastShown()) {
                                    BRToast.showCustomToast(ctx, message,
                                            BreadWalletApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, R.drawable.toast_layout_black);
                                    AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                                    if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                        final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.coinflip);
                                        mp.start();

                                    }
                                    messageId = 0;
                                    if (!BreadActivity.appVisible)
                                        BRNotificationManager.sendNotification(ctx, R.drawable.notification_icon, ctx.getString(R.string.app_name), message, 1);
                                }
                            }
                        }
                    }, 1000);

                }
            });

        } else {
            Log.e(TAG, "showSentReceivedToast: failed, ctx is null");
        }
    }

    public static void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        Log.d(TAG, "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
        Activity ctx = BreadWalletApp.getBreadContext();
        if (ctx != null) {
            TransactionDataSource.getInstance(ctx).updateTxBlockHeight(hash, blockHeight, timeStamp);

        } else {
            Log.e(TAG, "onTxUpdated: Failed, ctx is null");
        }
    }

    public static void onTxDeleted(String hash, int notifyUser, final int recommendRescan) {
        Log.e(TAG, "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d", hash, notifyUser, recommendRescan));
        final Activity ctx = BreadWalletApp.getBreadContext();
        if (ctx != null) {
            TransactionDataSource.getInstance(ctx).deleteTxByHash(hash);
            if (notifyUser == 1) {
                ctx.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AlertDialog alert;
                        AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                        builder.setTitle(R.string.BRWalletManager_transactionRejected_Android);

                        builder.setMessage(recommendRescan == 1 ? ctx.getString(R.string.BRWalletManager_walletOutOfSync_Android) : "");
                        if (recommendRescan == 1)
                            builder.setPositiveButton(R.string.ReScan_alertAction,
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
                        builder.setNegativeButton(ctx.getString(R.string.Button_cancel),
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
        } else {
            Log.e(TAG, "onTxDeleted: Failed! ctx is null");
        }
    }

    public boolean validatePhrase(Context ctx, String phrase) {
        String[] words = new String[0];
        List<String> list;

        String[] cleanWordList = null;
        try {
            boolean isLocal = true;
            String languageCode = ctx.getString(R.string.lang_Android);
            list = Bip39Reader.getWordList(ctx, languageCode);

            String[] phraseWords = phrase.split(" ");
            if (!list.contains(phraseWords[0])) {
                isLocal = false;
            }
            if (!isLocal) {
                String lang = Bip39Reader.getLang(ctx, phraseWords[0]);
                if (lang != null) {
                    list = Bip39Reader.getWordList(ctx, lang);
                }

            }
            words = list.toArray(new String[list.size()]);
            cleanWordList = Bip39Reader.cleanWordList(words);
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

    public void startTheWalletIfExists(final Activity app) {
        final BRWalletManager m = BRWalletManager.getInstance();
        if (!m.isPasscodeEnabled(app)) {
            //Device passcode/password should be enabled for the app to work
            BreadDialog.showCustomDialog(app, "Warning", app.getString(R.string.IntroScreen_encryption_needed_Android),
                    "close", null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            app.finish();
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialog) {
                            app.finish();
                        }
                    }, 0);
        } else {
            if (!m.noWallet(app)) {
                BRAnimator.startBreadActivity(app, true);
            }

        }
    }

    //BLOCKS
    public void setUpTheWallet(final Context ctx) {
        Log.d(TAG, "setUpTheWallet...");
        Assert.assertNotNull(ctx);
        if (ctx == null) return;
        BRWalletManager m = BRWalletManager.getInstance();
        final BRPeerManager pm = BRPeerManager.getInstance();

        if (!m.isCreated()) {
            List<BRTransactionEntity> transactions = TransactionDataSource.getInstance(ctx).getAllTransactions();
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
            List<BRMerkleBlockEntity> blocks = MerkleBlockDataSource.getInstance(ctx).getAllMerkleBlocks();
            List<BRPeerEntity> peers = PeerDataSource.getInstance(ctx).getAllPeers();
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

    public boolean generateQR(Context ctx, String bitcoinURL, ImageView qrcode) {
        if (qrcode == null || bitcoinURL == null || bitcoinURL.isEmpty()) return false;
        WindowManager manager = (WindowManager) ctx.getSystemService(Activity.WINDOW_SERVICE);
        Display display = manager.getDefaultDisplay();
        Point point = new Point();
        display.getSize(point);
        int width = point.x;
        int height = point.y;
        int smallerDimension = width < height ? width : height;
        smallerDimension = (int) (smallerDimension * 0.55f);
        Bitmap bitmap = null;
        bitmap = QRUtils.encodeAsBitmap(bitcoinURL, smallerDimension);
        //qrcode.setPadding(1, 1, 1, 1);
        //qrcode.setBackgroundResource(R.color.gray);
        if (bitmap == null) return false;
        qrcode.setImageBitmap(bitmap);
        return true;

    }

    public void offerToChangeTheAmount(final Context app, final PaymentItem item) {
        BreadDialog.showCustomDialog(app, app.getString(R.string.insufficient_funds), app.getString(R.string.change_payment_amount),
                app.getString(R.string.change), app.getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        BRAnimator.showSendFragment((Activity) app, Utils.createBitcoinUrl(item.addresses[0], 0, null, null, null));
                        brDialogView.dismissWithAnimation();
                    }
                }, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, 0);
    }

    public void addBalanceChangedListener(OnBalanceChanged listener) {
        if (balanceListeners == null) {
            Log.e(TAG, "addBalanceChangedListener: statusUpdateListeners is null");
            return;
        }
        if (!balanceListeners.contains(listener))
            balanceListeners.add(listener);
    }

    public void removeListener(OnBalanceChanged listener) {
        if (balanceListeners == null) {
            Log.e(TAG, "addBalanceChangedListener: statusUpdateListeners is null");
            return;
        }
        balanceListeners.remove(listener);

    }

    public interface OnBalanceChanged {
        void onBalanceChanged(long balance);

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

    public native int feeForTransactionAmount(long amountHolder);

    public native long getMinOutputAmount();

    public native long getMaxOutputAmount();

    public native boolean isCreated();

//    public native boolean transactionIsVerified(String txHash);

    public native byte[] tryTransaction(String addressHolder, long amountHolder);

    // returns the given amount (amount is in satoshis) in local currency units (i.e. pennies, pence)
    // price is local currency units per bitcoin
    public native long localAmount(long amount, double price);

    // returns the given local currency amount in satoshis
    // price is local currency units (i.e. pennies, pence) per bitcoin
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