package com.breadwallet.wallet;

import android.app.Activity;
import android.app.AlertDialog;
import android.app.KeyguardManager;
import android.content.DialogInterface;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.SystemClock;
import android.text.InputType;
import android.util.Log;
import android.view.MotionEvent;
import android.view.View;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TableLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroShowPhraseActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.RequestQRActivity;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.ImportPrivKeyEntity;
import com.breadwallet.presenter.entities.PaymentRequestEntity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.presenter.fragments.FragmentSettingsAll;
import com.breadwallet.presenter.fragments.MainFragmentQR;
import com.breadwallet.tools.threads.PassCodeTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.manager.BRNotificationManager;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.WordsReader;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SpringAnimator;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.security.PostAuthenticationProcessor;
import com.breadwallet.tools.sqlite.SQLiteManager;
import com.breadwallet.tools.threads.ImportPrivKeyTask;

import java.io.IOException;
import java.math.BigDecimal;
import java.security.SecureRandom;
import java.util.List;
import java.util.Locale;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan on 9/22/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
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
                                activity.getString(R.string.not_connected), activity.getString(R.string.ok));
                        return;
                    }

                    final AlertDialog.Builder builder = new AlertDialog.Builder(activity);
//                    builder.setTitle("password protected key");

                    // Set up the input

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
                                                ((BreadWalletApp) ctx.getApplication()).showCustomToast(ctx, activity.getString(R.string.scanning_privkey), MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 1);
                                            }
                                        });
                                    if (editText == null) return;

                                    String pass = editText.getText().toString();
                                    String decryptedKey = decryptBip38Key(privKey, pass);

                                    Log.e(TAG, "decryptedKey: " + decryptedKey);

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
//                                        BRAnimator.animateSlideToLeft(app, new FragmentRecoveryPhrase(), null);
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
                    String strToShow = String.format(ctx.getString(amount > 0 ? R.string.received_amount : R.string.sent_amount),
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
        CurrencyManager cm = CurrencyManager.getInstance(ctx);
        BRWalletManager m = BRWalletManager.getInstance(ctx);
        final long feeForTx = m.feeForTransaction(request.addresses[0], request.amount);
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
                    new BigDecimal(minOutput).divide(new BigDecimal("100")));
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
                ((BreadWalletApp) ctx.getApplicationContext()).promptForAuthentication(ctx, BRConstants.AUTH_FOR_PAY, request, message, ctx.getString(R.string.payment_info), null);

            }
        });
    }

    public void pay(final String addressHolder, final BigDecimal bigDecimalAmount, final String cn, final boolean isAmountRequested) {
        if (addressHolder == null || bigDecimalAmount == null) return;
        if (addressHolder.length() < 20) return;
//        final long amountAsLong = bigDecimal.longValue();
        if (bigDecimalAmount.longValue() < 0) return;
        Log.e(TAG, "*********Sending: " + bigDecimalAmount + " to: " + addressHolder);
        final CurrencyManager cm = CurrencyManager.getInstance(ctx);

        if (cm.isNetworkAvailable(ctx)) {
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
//                String strToReduce = String.valueOf(amountToReduce);
                final AlertDialog.Builder builder = new AlertDialog.Builder(ctx);

                builder.setMessage(String.format(ctx.getString(R.string.reduce_payment_amount_by), BRStringFormatter.getFormattedCurrencyString("BTC", amountToReduce)))
                        .setTitle(R.string.insufficient_funds_for_fee)
                        .setCancelable(false)
                        .setNegativeButton(ctx.getString(R.string.cancel), new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int which) {
                                dialog.cancel();
                            }
                        })
                        .setPositiveButton(R.string.confirm, new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int id) {
                                byte[] tmpTx2 = m.tryTransaction(addressHolder, bigDecimalAmount.longValue() - amountToReduce);
                                if (tmpTx2 != null) {
                                    PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx2);
                                    confirmPay(new PaymentRequestEntity(new String[]{addressHolder}, bigDecimalAmount.longValue() - amountToReduce, cn, tmpTx2, isAmountRequested));
                                } else {
                                    Log.e(TAG, "tmpTxObject2 is null!!!");
                                    ((BreadWalletApp) ctx.getApplication()).showCustomToast(ctx, ctx.getString(R.string.failed_to_send_insufficient_funds), MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
                                }
                            }
                        });
                AlertDialog alert = builder.create();
                alert.show();
                return;
            }
            PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx);
            Log.e(TAG, "pay >>>> feeForTx: " + feeForTx + ", amountAsDouble: " + bigDecimalAmount.longValue() +
                    ", CurrencyManager.getInstance(this).getBALANCE(): " + cm.getBALANCE());
            if (feeForTx != 0 && bigDecimalAmount.longValue() + feeForTx < cm.getBALANCE()) {
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
        final int pass = KeyStoreManager.getPassCode(ctx);
        if (pass == 0) {
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
                        ((BreadWalletApp) ctx.getApplication()).showCustomToast(ctx, ctx.getString(R.string.keystore_unavailable),
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

        }
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
