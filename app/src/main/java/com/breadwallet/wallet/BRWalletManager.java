package com.breadwallet.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.Point;
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
import com.breadwallet.tools.threads.PassCodeTask;
import com.breadwallet.tools.threads.PaymentProtocolPostPaymentTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.BRNotificationManager;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.WordsReader;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.tools.threads.ImportPrivKeyTask;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.MultiFormatWriter;
import com.google.zxing.WriterException;
import com.google.zxing.common.BitMatrix;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.Arrays;
import java.util.EnumMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

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
        byte[] strPhrase = encodeSeed(keyBytes, words);
        if (strPhrase == null || strPhrase.length == 0)
            throw new NullPointerException("failed to encodeSeed");
        boolean success = KeyStoreManager.putKeyStorePhrase(strPhrase, ctx, BRConstants.PUT_PHRASE_NEW_WALLET_REQUEST_CODE);
        if (!success) return false;
        KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis() / 1000), ctx);
        byte[] strBytes = TypesConverter.getNullTerminatedPhrase(strPhrase);
        byte[] authKey = getAuthPrivKeyForAPI(strPhrase);
//        Log.e(TAG,"authKey: " + Arrays.toString(authKey));
        KeyStoreManager.putAuthKey(authKey, ctx);
        byte[] pubKey = BRWalletManager.getInstance(ctx).getMasterPubKey(strBytes);
        KeyStoreManager.putMasterPublicKey(pubKey, ctx);

        return true;

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
            Log.e(TAG, "generated receiveAddress: " + tmpAddr);
            if (tmpAddr == null || tmpAddr.isEmpty()) return;
            SharedPreferencesManager.putReceiveAddress(ctx, tmpAddr);
            if (mainFragmentQR == null) return;
            mainFragmentQR.refreshAddress(tmpAddr);
        } else {
            throw new NullPointerException("Cannot be null");
        }
    }

    public void wipeWalletButKeystore(final Activity activity) {
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

                    if (!((BreadWalletApp) activity.getApplication()).isNetworkAvailable(activity)) {
                        ((BreadWalletApp) activity.getApplication()).showCustomDialog(activity.getString(R.string.warning),
                                activity.getString(R.string.not_connected), activity.getString(R.string.ok));
                        return;
                    }

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
                            new Thread(new Runnable() {
                                @Override
                                public void run() {
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
                            }).start();

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
            Log.e(TAG, "isValidBitcoinPrivateKey true");
            new ImportPrivKeyTask(activity).execute(privKey);
            return true;
        } else {
            return false;
        }
    }

    public static void showWritePhraseDialog(final boolean firstTime) {

        if (ctx == null) ctx = MainActivity.app;
        if (ctx != null) {
            ctx.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    boolean phraseWroteDown = SharedPreferencesManager.getPhraseWroteDown(ctx);
                    if (phraseWroteDown) return;
                    long now = System.currentTimeMillis() / 1000;
                    Log.e(TAG,"balance: " + CurrencyManager.getInstance(ctx).getBALANCE());
                    Log.e(TAG,"limit: " +SharedPreferencesManager.getLimit(ctx));
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
//                                            final RelativeLayout tipsBlockPane = (RelativeLayout) app.findViewById(R.id.tips_block_pane);

//                                            app.runOnUiThread(new Runnable() {
//                                                @Override
//                                                public void run() {
//                                                    tipsBlockPane.setVisibility(View.VISIBLE);
//                                                    new Handler().postDelayed(new Runnable() {
//                                                        @Override
//                                                        public void run() {
//                                                            tipsBlockPane.setVisibility(View.GONE);
//                                                        }
//                                                    }, 5000);
//                                                }
//                                            });
                                            //in case of an error assure the blockPane is gone anyway in 5 sec

//                                            switch (BRAnimator.level) {
//                                                case 0:
//                                                    app.runOnUiThread(new Runnable() {
//                                                        @Override
//                                                        public void run() {
//                                                            BRAnimator.pressMenuButton(app);
//                                                        }
//                                                    });
//
//                                                    try {
//                                                        Thread.sleep(500);
//                                                    } catch (InterruptedException e) {
//                                                        e.printStackTrace();
//                                                    }
//                                                    app.runOnUiThread(new Runnable() {
//                                                        @Override
//                                                        public void run() {
//                                                            FragmentSettingsAll fragmentSettingsAll = (FragmentSettingsAll) app.
//                                                                    getFragmentManager().findFragmentByTag(FragmentSettingsAll.class.getName());
//                                                            BRAnimator.animateSlideToLeft(app, new FragmentSettings(), fragmentSettingsAll);
//                                                        }
//                                                    });
//
//                                                    try {
//                                                        Thread.sleep(500);
//                                                    } catch (InterruptedException e) {
//                                                        e.printStackTrace();
//                                                    }
//
//                                                    app.runOnUiThread(new Runnable() {
//                                                        @Override
//                                                        public void run() {
//                                                            new android.support.v7.app.AlertDialog.Builder(app)
//                                                                    .setTitle(app.getResources().getString(R.string.warning))
//                                                                    .setMessage(app.getResources().getString(R.string.warning_text1) +
//                                                                            app.getResources().getString(R.string.warning_text2) +
//                                                                            app.getResources().getString(R.string.warning_text3))
//                                                                    .setPositiveButton(app.getResources().getString(R.string.show), new DialogInterface.OnClickListener() {
//                                                                        public void onClick(DialogInterface dialog, int which) {
//                                                                            PostAuthenticationProcessor.getInstance().onShowPhraseAuth(app);
//                                                                        }
//                                                                    })
//                                                                    .setNegativeButton(app.getResources().getString(R.string.cancel), new DialogInterface.OnClickListener() {
//                                                                        public void onClick(DialogInterface dialog, int which) {
//                                                                            dialog.dismiss();
//                                                                        }
//                                                                    })
//                                                                    .show();
//                                                            tipsBlockPane.setVisibility(View.GONE);
//                                                        }
//                                                    });

//                                                    break;
//                                            }
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
        if (words.length != 2048)
            throw new IllegalArgumentException("words.length is not 2048");
        return validateRecoveryPhrase(cleanWordList, phrase);
    }

    public void confirmPay(final PaymentRequestEntity request) {
        if (ctx == null) ctx = MainActivity.app;
        if (ctx == null) return;
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
                        BRConstants.AUTH_FOR_PAY, request, message, "", null);
            }
        });
    }

    public void pay(final String addressHolder, final BigDecimal bigDecimalAmount, final String cn, final boolean isAmountRequested) {
        if (addressHolder == null || bigDecimalAmount == null) return;
        if (addressHolder.length() < 20) return;

        int unit = BRConstants.CURRENT_UNIT_BITS;
        Activity context = MainActivity.app;
        String divideBy = "100";
        if (context != null)
            unit = SharedPreferencesManager.getCurrencyUnit(context);
        if (unit == BRConstants.CURRENT_UNIT_MBITS) divideBy = "100000";
        if (unit == BRConstants.CURRENT_UNIT_BITCOINS) divideBy = "100000000";
//        final long amountAsLong = bigDecimal.longValue();
        if (bigDecimalAmount.longValue() < 0) return;
        Log.e(TAG, "*********Sending: " + bigDecimalAmount + " to: " + addressHolder);
        final CurrencyManager cm = CurrencyManager.getInstance(ctx);
        long minAmount = getMinOutputAmountRequested();
        if (bigDecimalAmount.longValue() < minAmount) {
            final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.bitcoin_payment_cant_be_less),
                    BRConstants.bitcoinLowercase + new BigDecimal(minAmount).divide(new BigDecimal(divideBy)));
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
            return;
        }
        if (((BreadWalletApp) ctx.getApplication()).isNetworkAvailable(ctx)) {
            final BRWalletManager m = BRWalletManager.getInstance(ctx);
            byte[] tmpTx = m.tryTransaction(addressHolder, bigDecimalAmount.longValue());
            long feeForTx = m.feeForTransaction(addressHolder, bigDecimalAmount.longValue());

            if (tmpTx == null && bigDecimalAmount.longValue() <= cm.getBALANCE() && bigDecimalAmount.longValue() > 0) {
                final long maxAmountDouble = m.getMaxOutputAmount();
                if (maxAmountDouble < getMinOutputAmount()) {
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

                    return;
                }

                Log.e(TAG, "maxAmountDouble: " + maxAmountDouble);
                final long amountToReduce = bigDecimalAmount.longValue() - maxAmountDouble;
                final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);
                String reduceBits = BRStringFormatter.getFormattedCurrencyString("BTC", amountToReduce);
                String reduceFee = BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx), SharedPreferencesManager.getIso(ctx), new BigDecimal(amountToReduce), ctx);
                String reduceBitsMinus = BRStringFormatter.getFormattedCurrencyString("BTC", -amountToReduce);
                String reduceFeeMinus = BRStringFormatter.getExchangeForAmount(SharedPreferencesManager.getRate(ctx), SharedPreferencesManager.getIso(ctx), new BigDecimal(-amountToReduce), ctx);

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
                                byte[] tmpTx2 = m.tryTransaction(addressHolder, bigDecimalAmount.longValue() - amountToReduce);
                                if (tmpTx2 != null) {
                                    PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx2);
                                    confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue() - amountToReduce, cn, tmpTx2, isAmountRequested));
                                } else {
                                    Log.e(TAG, "tmpTxObject2 is null!!!");
                                    ((BreadWalletApp) ctx.getApplication()).showCustomToast(ctx, ctx.getString(R.string.insufficient_funds),
                                            MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
                                }
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                alert.getButton(AlertDialog.BUTTON_POSITIVE).setAllCaps(false);
                return;
            } else if (tmpTx == null && bigDecimalAmount.longValue() >= cm.getBALANCE() && bigDecimalAmount.longValue() > 0) {
                Log.e(TAG, "addressHolder: " + addressHolder);
                FragmentScanResult.address = addressHolder;
                if (!BreadWalletApp.unlocked) {
                    //let it fail but the after the auth let the user know there is not enough money
                    confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue(), cn, null, isAmountRequested));
                    return;
                } else {
                    BRWalletManager.getInstance(ctx).offerToChangeTheAmount(ctx, ctx.getString(R.string.insufficient_funds));
                    return;
                }

            }
            PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx);
            Log.e(TAG, "pay >>>> feeForTx: " + feeForTx + ", amountAsDouble: " + bigDecimalAmount.longValue() +
                    ", CurrencyManager.getInstance(this).getBALANCE(): " + cm.getBALANCE());
            if ((feeForTx != 0 && bigDecimalAmount.longValue() + feeForTx < cm.getBALANCE()) || (isAmountRequested && !BreadWalletApp.unlocked)) {
                confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue(), cn, tmpTx, isAmountRequested));
            } else {
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
        } else {
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

    }

    public void askForPasscode() {
        if (ctx == null) return;
        final String pass = KeyStoreManager.getPassCode(ctx);
        if (pass == null || pass.length() != 4) {
            new Handler().post(new Runnable() {
                @Override
                public void run() {
                    if (ctx != null) {
                        Log.e(TAG, "PASSCODE: " + pass);
                        new PassCodeTask(ctx).start();
                    }
                }
            });
        }

    }

    public void setUpTheWallet() {
        if (ctx == null) return;
        BRWalletManager m = BRWalletManager.getInstance(ctx);
        final BRPeerManager pm = BRPeerManager.getInstance(ctx);

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
            if (pubkeyEncoded == null || pubkeyEncoded.length == 0) {

                ctx.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        ((BreadWalletApp) ctx.getApplication()).showCustomToast(ctx,
                                ctx.getString(R.string.keystore_unavailable),
                                MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                ctx.finish();
                            }
                        }, 3500);
                    }
                });
                return;
            }
            //Save the first address for future check
            m.createWallet(transactionsCount, pubkeyEncoded);
            String firstAddress = BRWalletManager.getFirstAddress(pubkeyEncoded);
            SharedPreferencesManager.putFirstAddress(ctx, firstAddress);
        }

        long fee = SharedPreferencesManager.getFeePerKb(ctx);
        if (fee == 0) fee = BRConstants.DEFAULT_FEE_PER_KB;
        BRWalletManager.getInstance(ctx).setFeePerKb(fee);

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
            Log.e(TAG, "blocksCount before connecting: " + blocksCount);
            Log.e(TAG, "peersCount before connecting: " + peersCount);

            int walletTimeString = KeyStoreManager.getWalletCreationTime(ctx);
            final int earliestKeyTime = walletTimeString != 0 ? walletTimeString : 0;
            Log.e(TAG, "earliestKeyTime before connecting: " + earliestKeyTime);
            pm.createAndConnect(earliestKeyTime > 0 ? earliestKeyTime : 0, blocksCount, peersCount);
            if (SharedPreferencesManager.getStartHeight(ctx) == 0)
                SharedPreferencesManager.putStartHeight(ctx, BRPeerManager.getCurrentBlockHeight());

        }
    }

    public void generateQR(String bitcoinURL, ImageView qrcode) {
        if (qrcode == null || bitcoinURL == null || bitcoinURL.isEmpty()) return;
        if(ctx == null) ctx = MainActivity.app;
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

    public void offerToChangeTheAmount(Activity app, String title) {

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

    public void animateSavePhraseFlow() {
        PhraseFlowActivity.screenParametersPoint = IntroActivity.screenParametersPoint;
        if (PhraseFlowActivity.screenParametersPoint == null ||
                PhraseFlowActivity.screenParametersPoint.y == 0 ||
                PhraseFlowActivity.screenParametersPoint.x == 0)
            PhraseFlowActivity.screenParametersPoint = MainActivity.screenParametersPoint;
        Log.e(TAG,"PhraseFlowActivity.screenParametersPoint.x: " + PhraseFlowActivity.screenParametersPoint.x);
        Log.e(TAG,"PhraseFlowActivity.screenParametersPoint.y: " + PhraseFlowActivity.screenParametersPoint.y);
        Intent intent;
        intent = new Intent(ctx, PhraseFlowActivity.class);
        ctx.startActivity(intent);
        if (!ctx.isDestroyed()) {
            ctx.finish();
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

    public native byte[] getAuthPrivKeyForAPI(byte[] phrase);

}