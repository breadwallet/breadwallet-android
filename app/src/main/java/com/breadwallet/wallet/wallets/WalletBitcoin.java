package com.breadwallet.wallet.wallets;

import android.app.Activity;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.os.Handler;
import android.os.NetworkOnMainThreadException;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.presenter.interfaces.BROnSignalCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.manager.BRNotificationManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.uri.BitcoinUriParser;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.interfaces.BaseWallet;
import com.breadwallet.wallet.interfaces.OnBalanceChanged;
import com.breadwallet.wallet.wallets.configs.WalletUiConfiguration;
import com.google.firebase.crash.FirebaseCrash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import static com.breadwallet.presenter.fragments.FragmentSend.isEconomyFee;
import static com.breadwallet.tools.util.BRConstants.ROUNDING_MODE;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/22/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class WalletBitcoin implements BaseWallet {

    private static final String TAG = WalletBitcoin.class.getName();

    public static final long MAX_BTC = 21000000;
    private final static long FEE_EXPIRATION_MILLIS = 72 * 60 * 60 * 1000L;

    private boolean itInitiatingWallet;
    public List<OnBalanceChanged> balanceListeners;
    private static WalletBitcoin instance;
    private boolean timedOut;
    private boolean sending;
    private WalletUiConfiguration uiConfig;

    public static WalletBitcoin getInstance() {
        if (instance == null) instance = new WalletBitcoin();
        return instance;
    }

    private WalletBitcoin() {
        balanceListeners = new ArrayList<>();
        uiConfig = new WalletUiConfiguration("#f29500", true, true, true);
    }

    public void setBalance(final Context context, long balance) {
        if (context == null) {
            Log.e(TAG, "setBalance: FAILED TO SET THE BALANCE");
            return;
        }
        BRSharedPrefs.putCachedBalance(context, BRSharedPrefs.getCurrentWalletIso(context), balance);
        WalletsMaster.refreshAddress(context);

        for (OnBalanceChanged listener : balanceListeners) {
            if (listener != null) listener.onBalanceChanged("BTC", balance);

        }
    }

    public void refreshBalance(Activity app) {
        long natBal = WalletsMaster.getInstance().nativeBalance();
        if (natBal != -1) {
            setBalance(app, natBal);
        } else {
            Log.e(TAG, "UpdateUI, nativeBalance is -1 meaning _wallet was null!");
        }
    }

    /**
     * Create tx from the PaymentItem object and try to send it
     */
    @Override
    public boolean sendTransaction(final Context app, final PaymentItem payment) {
        //array in order to be able to modify the first element from an inner block (can't be final)
        final String[] errTitle = {null};
        final String[] errMessage = {null};
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (sending) {
                        Log.e(TAG, "sendTransaction: already sending..");
                        return;
                    }
                    sending = true;
                    long now = System.currentTimeMillis();
                    //if the fee was updated more than 24 hours ago then try updating the fee
                    if (now - BRSharedPrefs.getFeeTime(app, BRSharedPrefs.getCurrentWalletIso(app)) >= FEE_EXPIRATION_MILLIS) {
                        new Thread(new Runnable() {
                            @Override
                            public void run() {
                                try {
                                    Thread.sleep(3000);
                                } catch (InterruptedException e) {
                                    e.printStackTrace();
                                }

                                if (sending) timedOut = true;
                            }
                        }).start();
                        BRApiManager.updateFeePerKb(app);
                        //if the fee is STILL out of date then fail with network problem message
                        long time = BRSharedPrefs.getFeeTime(app, BRSharedPrefs.getCurrentWalletIso(app));
                        if (time <= 0 || now - time >= FEE_EXPIRATION_MILLIS) {
                            Log.e(TAG, "sendTransaction: fee out of date even after fetching...");
                            throw new FeeOutOfDate(BRSharedPrefs.getFeeTime(app, BRSharedPrefs.getCurrentWalletIso(app)), now);
                        }
                    }
                    if (!timedOut)
                        tryPay(app, payment);
                    else
                        FirebaseCrash.report(new NullPointerException("did not send, timedOut!"));
                    return; //return so no error is shown
                } catch (InsufficientFundsException ignored) {
                    errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                    errMessage[0] = "Insufficient Funds";
                } catch (AmountSmallerThanMinException e) {
                    long minAmount = WalletsMaster.getInstance().getMinOutputAmount();
                    errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                    errMessage[0] = String.format(Locale.getDefault(), app.getString(R.string.PaymentProtocol_Errors_smallPayment),
                            BRConstants.bitcoinLowercase + new BigDecimal(minAmount).divide(new BigDecimal(100), BRConstants.ROUNDING_MODE));
                } catch (SpendingNotAllowed spendingNotAllowed) {
                    showSpendNotAllowed(app);
                    return;
                } catch (FeeNeedsAdjust feeNeedsAdjust) {
                    //offer to change amount, so it would be enough for fee
//                    showFailed(app); //just show failed for now
                    showAdjustFee((Activity) app, payment);
                    return;
                } catch (FeeOutOfDate ex) {
                    //Fee is out of date, show not connected error
                    FirebaseCrash.report(ex);
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), app.getString(R.string.NodeSelector_notConnected), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);
                        }
                    });
                    return;
                } catch (SomethingWentWrong somethingWentWrong) {
                    somethingWentWrong.printStackTrace();
                    FirebaseCrash.report(somethingWentWrong);
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), "Something went wrong", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);
                        }
                    });
                    return;
                } finally {
                    sending = false;
                    timedOut = false;
                }

                //show the message if we have one to show
                if (errTitle[0] != null && errMessage[0] != null)
                    BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                        @Override
                        public void run() {
                            BRDialog.showCustomDialog(app, errTitle[0], errMessage[0], app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);
                        }
                    });

            }
        });
        return true;
    }

    @Override
    public boolean generateWallet(Context app) {
        //todo implement
        return false;
    }

    @Override
    public boolean initWallet(final Context ctx) {

        if (ActivityUTILS.isMainThread()) throw new NetworkOnMainThreadException();
        if (itInitiatingWallet) return false;
        itInitiatingWallet = true;
        try {
            Log.d(TAG, "initWallet:" + Thread.currentThread().getName());
            if (ctx == null) {
                Log.e(TAG, "initWallet: ctx is null");
                return false;
            }
            WalletsMaster m = WalletsMaster.getInstance();
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

                byte[] pubkeyEncoded = BRKeyStore.getMasterPublicKey(ctx);
                if (Utils.isNullOrEmpty(pubkeyEncoded)) {
                    Log.e(TAG, "initWallet: pubkey is missing");
                    return false;
                }
                //Save the first address for future check
                m.createWallet(transactionsCount, pubkeyEncoded);
                String firstAddress = WalletsMaster.getFirstAddress(pubkeyEncoded);
                BRSharedPrefs.putFirstAddress(ctx, firstAddress);
                long fee = BRSharedPrefs.getFeePerKb(ctx);
                if (fee == 0) {
                    fee = WalletsMaster.getInstance().defaultFee();
                    BREventManager.getInstance().pushEvent("wallet.didUseDefaultFeePerKB");
                }
                WalletsMaster.getInstance().setFeePerKb(fee, isEconomyFee);
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

                int walletTime = BRKeyStore.getWalletCreationTime(ctx);

                Log.e(TAG, "initWallet: walletTime: " + walletTime);
                pm.create(walletTime, blocksCount, peersCount);
                BRPeerManager.getInstance().updateFixedPeer(ctx);
            }

            pm.connect();
            if (BRSharedPrefs.getStartHeight(ctx, BRSharedPrefs.getCurrentWalletIso(ctx)) == 0)
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        BRSharedPrefs.putStartHeight(ctx, BRSharedPrefs.getCurrentWalletIso(ctx), BRPeerManager.getCurrentBlockHeight());
                    }
                });
        } finally {
            itInitiatingWallet = false;
        }

        return true;
    }

    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.bitcoinLowercase;
        if (app != null) {
            int unit = BRSharedPrefs.getBitcoinUnit(app);
            switch (unit) {
                case BRConstants.CURRENT_UNIT_BITS:
                    currencySymbolString = BRConstants.bitcoinLowercase;
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + BRConstants.bitcoinUppercase;
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString = BRConstants.bitcoinUppercase;
                    break;
            }
        }
        return currencySymbolString;
    }

    @Override
    public String getIso(Context app) {
        return "BTC";
    }

    @Override
    public String getName(Context app) {
        if (app == null) return null;
        int unit = BRSharedPrefs.getBitcoinUnit(app);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return "Bits";
            case BRConstants.CURRENT_UNIT_MBITS:
                return "MBits";
            default:
                return "Bitcoin";
        }
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        int unit = BRSharedPrefs.getBitcoinUnit(app);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                return 2;
            case BRConstants.CURRENT_UNIT_MBITS:
                return 5;
            default:
                return 8;
        }
    }

    @Override
    public long getCachedBalance(Context app) {
        return BRSharedPrefs.getCachedBalance(app, "BTC");
    }

    @Override
    public void setCashedBalance(Context app, long balance) {
        BRSharedPrefs.putCachedBalance(app, "BTC", balance);
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        //return max bitcoin
        return new BigDecimal(MAX_BTC);
    }

    @Override
    public boolean tryUri(Context app, String uriStr) {
        return BitcoinUriParser.processRequest(app, uriStr);
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return uiConfig;
    }

    @Override
    public long getFiatBalance(Context app) {
        return getFiatForCrypto(app, new BigDecimal(getCachedBalance(app))).longValue();
    }

    @Override
    public BigDecimal getFiatForCrypto(Context app, BigDecimal amount) {
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByIso(iso);
        if (ent == null) return null;
        double rate = ent.rate;
        BigDecimal cryptoAmount = getCryptoForSmallestCrypto(app, amount);
        return amount.divide(cryptoAmount, BRConstants.ROUNDING_MODE).multiply(new BigDecimal(rate)).multiply(new BigDecimal(100));
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByIso(iso);
        if (ent == null) return null;
        double rate = ent.rate;
        //convert c to $.
        BigDecimal fiatAmount = amount.divide(new BigDecimal(100), ROUNDING_MODE);
        int unit = BRSharedPrefs.getBitcoinUnit(app);
        BigDecimal result = new BigDecimal(0);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = fiatAmount.multiply(new BigDecimal(rate)).divide(new BigDecimal("100"), ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = fiatAmount.multiply(new BigDecimal(rate)).divide(new BigDecimal("100000"), 5, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = fiatAmount.multiply(new BigDecimal(rate)).divide(new BigDecimal("100000000"), 8, ROUNDING_MODE);
                break;
        }
        return result;

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        BigDecimal result = new BigDecimal(0);
        int unit = BRSharedPrefs.getBitcoinUnit(app);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = amount.divide(new BigDecimal("100"), 2, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = amount.divide(new BigDecimal("100000"), 5, ROUNDING_MODE);
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = amount.divide(new BigDecimal("100000000"), 8, ROUNDING_MODE);
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        return null;
    }

    public void addBalanceChangedListener(OnBalanceChanged listener) {
        if (balanceListeners == null) balanceListeners = new ArrayList<>();
        if (!balanceListeners.contains(listener))
            balanceListeners.add(listener);
    }

    public void removeListener(OnBalanceChanged listener) {
        if (balanceListeners == null) {
            return;
        }
        if (balanceListeners.contains(listener))
            balanceListeners.remove(listener);

    }

    /**
     * Wallet callbacks
     */
    public static void publishCallback(final String message, final int error, byte[] txHash) {
        Log.e(TAG, "publishCallback: " + message + ", err:" + error + ", txHash: " + Arrays.toString(txHash));
        final Context app = BreadApp.getBreadContext();
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (app instanceof Activity)
                    BRAnimator.showBreadSignal((Activity) app, error == 0 ? app.getString(R.string.Alerts_sendSuccess) : app.getString(R.string.Alert_error),
                            error == 0 ? app.getString(R.string.Alerts_sendSuccessSubheader) : message, error == 0 ? R.drawable.ic_check_mark_white : R.drawable.ic_error_outline_black_24dp, new BROnSignalCompletion() {
                                @Override
                                public void onComplete() {
                                    if (!((Activity) app).isDestroyed())
                                        ((Activity) app).getFragmentManager().popBackStack();
                                }
                            });
            }
        });

    }

    public static void onBalanceChanged(final long balance) {
        Log.d(TAG, "onBalanceChanged:  " + balance);
        Context app = BreadApp.getBreadContext();
        getInstance().setBalance(app, balance);

    }

    public static void onTxAdded(byte[] tx, int blockHeight, long timestamp, final long amount, String hash) {
        Log.d(TAG, "onTxAdded: " + String.format("tx.length: %d, blockHeight: %d, timestamp: %d, amount: %d, hash: %s", tx.length, blockHeight, timestamp, amount, hash));

        final Context ctx = BreadApp.getBreadContext();
        if (amount > 0) {
            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                @Override
                public void run() {
                    WalletsMaster master = WalletsMaster.getInstance();
                    String am = CurrencyUtils.getFormattedCurrencyString(ctx, "BTC", master.getCurrentWallet(ctx).getCryptoForSmallestCrypto(ctx, new BigDecimal(amount)));
                    String amCur = CurrencyUtils.getFormattedCurrencyString(ctx, BRSharedPrefs.getPreferredFiatIso(ctx), master.getCurrentWallet(ctx).getFiatForCrypto(ctx, new BigDecimal(amount)));
                    String formatted = String.format("%s (%s)", am, amCur);
                    final String strToShow = String.format(ctx.getString(R.string.TransactionDetails_received), formatted);

                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            if (!BRToast.isToastShown()) {
                                BRToast.showCustomToast(ctx, strToShow,
                                        BreadApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, R.drawable.toast_layout_black);
                                AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
                                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
                                    final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.coinflip);
                                    if (mp != null) try {
                                        mp.start();
                                    } catch (IllegalArgumentException ex) {
                                        Log.e(TAG, "run: ", ex);
                                    }
                                }

                                if (!BreadActivity.appVisible && BRSharedPrefs.getShowNotification(ctx))
                                    BRNotificationManager.sendNotification(ctx, R.drawable.notification_icon, ctx.getString(R.string.app_name), strToShow, 1);
                            }
                        }
                    }, 1000);


                }
            });
        }
        if (ctx != null)
            TransactionDataSource.getInstance(ctx).putTransaction(new BRTransactionEntity(tx, blockHeight, timestamp, hash));
        else
            Log.e(TAG, "onTxAdded: ctx is null!");
    }


    public static void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        Log.d(TAG, "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
        Context ctx = BreadApp.getBreadContext();
        if (ctx != null) {
            TransactionDataSource.getInstance(ctx).updateTxBlockHeight(hash, blockHeight, timeStamp);

        } else {
            Log.e(TAG, "onTxUpdated: Failed, ctx is null");
        }
    }

    public static void onTxDeleted(String hash, int notifyUser, final int recommendRescan) {
        Log.e(TAG, "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d", hash, notifyUser, recommendRescan));
        final Context ctx = BreadApp.getBreadContext();
        if (ctx != null) {
            BRSharedPrefs.putScanRecommended(ctx, BRSharedPrefs.getCurrentWalletIso(ctx), true);
        } else {
            Log.e(TAG, "onTxDeleted: Failed! ctx is null");
        }
    }


    /**
     * Try transaction and throw appropriate exceptions if something was wrong
     * BLOCKS
     */
    private void tryPay(final Context app, final PaymentItem paymentRequest) throws InsufficientFundsException,
            AmountSmallerThanMinException, SpendingNotAllowed, FeeNeedsAdjust, SomethingWentWrong {
        if (paymentRequest == null || paymentRequest.address == null) {
            Log.e(TAG, "tryPay: ERROR: paymentRequest: " + paymentRequest);
            String message = paymentRequest == null ? "paymentRequest is null" : "addresses is null";
            BRReportsManager.reportBug(new RuntimeException("paymentRequest is malformed: " + message), true);
            throw new SomethingWentWrong("wrong parameters: paymentRequest");
        }
        long amount = paymentRequest.amount;
        long balance = WalletsMaster.getInstance().getWalletByIso(BRSharedPrefs.getCurrentWalletIso(app)).getCachedBalance(app);
        final WalletsMaster m = WalletsMaster.getInstance();
        long minOutputAmount = WalletsMaster.getInstance().getMinOutputAmount();
        final long maxOutputAmount = WalletsMaster.getInstance().getMaxOutputAmount();

        // check if spending is allowed
        if (!BRSharedPrefs.getAllowSpend(app, BRSharedPrefs.getCurrentWalletIso(app))) {
            throw new SpendingNotAllowed();
        }

        //check if amount isn't smaller than the min amount
        if (isSmallerThanMin(app, paymentRequest)) {
            throw new AmountSmallerThanMinException(amount, minOutputAmount);
        }

        //amount is larger than balance
        if (isLargerThanBalance(app, paymentRequest)) {
            throw new InsufficientFundsException(amount, balance);
        }

        //not enough for fee
        if (notEnoughForFee(app, paymentRequest)) {
            //weird bug when the core WalletsMaster is NULL
            if (maxOutputAmount == -1) {
                BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"), true);
            }
            // max you can spend is smaller than the min you can spend
            if (maxOutputAmount == 0 || maxOutputAmount < minOutputAmount) {
                throw new InsufficientFundsException(amount, balance);
            }

            long feeForTx = m.feeForTransaction(paymentRequest.address, paymentRequest.amount);
            throw new FeeNeedsAdjust(amount, balance, feeForTx);
        }
        // payment successful
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                byte[] tmpTx = m.tryTransaction(paymentRequest.address, paymentRequest.amount);
                if (tmpTx == null) {
                    //something went wrong, failed to create tx
                    ((Activity) app).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BRDialog.showCustomDialog(app, "", app.getString(R.string.Alerts_sendFailure), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);

                        }
                    });
                    return;
                }
                paymentRequest.serializedTx = tmpTx;
                PostAuth.getInstance().setPaymentItem(paymentRequest);
                confirmPay(app, paymentRequest);
            }
        });

    }

    private void showAdjustFee(final Activity app, PaymentItem item) {
        WalletsMaster m = WalletsMaster.getInstance();
        long maxAmountDouble = m.getMaxOutputAmount();
        if (maxAmountDouble == -1) {
            BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"));
            return;
        }
        if (maxAmountDouble == 0) {
            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), "Insufficient amount for transaction fee", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismissWithAnimation();
                }
            }, null, null, 0);
        } else {
//            long fee = m.feeForTransaction(item.addresses[0], maxAmountDouble);
//            feeForTx += (m.getBalance(app) - request.amount) % 100;
//            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), "Insufficient amount for transaction fee", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
//                @Override
//                public void onClick(BRDialogView brDialogView) {
//                    brDialogView.dismissWithAnimation();
//                }
//            }, null, null, 0);
            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), "Insufficient amount for transaction fee", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismissWithAnimation();
                }
            }, null, null, 0);
            //todo fix this fee adjustment
        }

    }


    private void confirmPay(final Context ctx, final PaymentItem request) {
        if (ctx == null) {
            Log.e(TAG, "confirmPay: context is null");
            return;
        }

        String message = createConfirmation(ctx, request);

        double minOutput;
        if (request.isAmountRequested) {
            minOutput = WalletsMaster.getInstance().getMinOutputAmountRequested();
        } else {
            minOutput = WalletsMaster.getInstance().getMinOutputAmount();
        }

        //amount can't be less than the min
        if (request.amount < minOutput) {
            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.PaymentProtocol_Errors_smallTransaction),
                    BRConstants.bitcoinLowercase + new BigDecimal(minOutput).divide(new BigDecimal("100")));


            ((Activity) ctx).runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BRDialog.showCustomDialog(ctx, ctx.getString(R.string.Alerts_sendFailure), bitcoinMinMessage, ctx.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismiss();
                        }
                    }, null, null, 0);
                }
            });
            return;
        }
        boolean forcePin = false;

        Log.e(TAG, "confirmPay: totalSent: " + WalletsMaster.getInstance().getTotalSent());
        Log.e(TAG, "confirmPay: request.amount: " + request.amount);
        Log.e(TAG, "confirmPay: total limit: " + AuthManager.getInstance().getTotalLimit(ctx));
        Log.e(TAG, "confirmPay: limit: " + BRKeyStore.getSpendLimit(ctx));

        if (WalletsMaster.getInstance().getTotalSent() + request.amount > AuthManager.getInstance().getTotalLimit(ctx)) {
            forcePin = true;
        }

        //successfully created the transaction, authenticate user
        AuthManager.getInstance().authPrompt(ctx, "", message, forcePin, false, new BRAuthCompletion() {
            @Override
            public void onComplete() {
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        PostAuth.getInstance().onPublishTxAuth(ctx, false);
                        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                BRAnimator.killAllFragments((Activity) ctx);
                                BRAnimator.startBreadIfNotStarted((Activity) ctx);
                            }
                        });

                    }
                });

            }

            @Override
            public void onCancel() {
                //nothing
            }
        });

    }

    private String createConfirmation(Context ctx, PaymentItem request) {
        String receiver = getReceiver(request);

        String iso = BRSharedPrefs.getPreferredFiatIso(ctx);

        WalletsMaster m = WalletsMaster.getInstance();
        BaseWallet wallet = WalletsMaster.getInstance().getWalletByIso(BRSharedPrefs.getCurrentWalletIso(ctx));
        long feeForTx = m.feeForTransaction(request.address, request.amount);
        if (feeForTx == 0) {
            long maxAmount = m.getMaxOutputAmount();
            if (maxAmount == -1) {
                BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"), true);
            }
            if (maxAmount == 0) {
                BRDialog.showCustomDialog(ctx, "", ctx.getString(R.string.Alerts_sendFailure), ctx.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);

                return null;
            }
            feeForTx = m.feeForTransaction(request.address, maxAmount);
            feeForTx += (wallet.getCachedBalance(ctx) - request.amount) % 100;
        }
        final long total = request.amount + feeForTx;
        String formattedAmountBTC = CurrencyUtils.getFormattedCurrencyString(ctx, wallet.getIso(ctx), m.getCurrentWallet(ctx).getCryptoForSmallestCrypto(ctx, new BigDecimal(request.amount)));
        String formattedFeeBTC = CurrencyUtils.getFormattedCurrencyString(ctx, wallet.getIso(ctx), m.getCurrentWallet(ctx).getCryptoForSmallestCrypto(ctx, new BigDecimal(feeForTx)));
        String formattedTotalBTC = CurrencyUtils.getFormattedCurrencyString(ctx, wallet.getIso(ctx), m.getCurrentWallet(ctx).getCryptoForSmallestCrypto(ctx, new BigDecimal(total)));

        String formattedAmount = CurrencyUtils.getFormattedCurrencyString(ctx, iso, m.getCurrentWallet(ctx).getFiatForCrypto(ctx, new BigDecimal(request.amount)));
        String formattedFee = CurrencyUtils.getFormattedCurrencyString(ctx, iso, m.getCurrentWallet(ctx).getFiatForCrypto(ctx, new BigDecimal(feeForTx)));
        String formattedTotal = CurrencyUtils.getFormattedCurrencyString(ctx, iso, m.getCurrentWallet(ctx).getFiatForCrypto(ctx, new BigDecimal(total)));

        //formatted text
        return receiver + "\n\n"
                + ctx.getString(R.string.Confirmation_amountLabel) + " " + formattedAmountBTC + " (" + formattedAmount + ")"
                + "\n" + ctx.getString(R.string.Confirmation_feeLabel) + " " + formattedFeeBTC + " (" + formattedFee + ")"
                + "\n" + ctx.getString(R.string.Confirmation_totalLabel) + " " + formattedTotalBTC + " (" + formattedTotal + ")"
                + (request.comment == null ? "" : "\n\n" + request.comment);
    }

    private String getReceiver(PaymentItem item) {
        String receiver;
        boolean certified = false;
        if (item.cn != null && item.cn.length() != 0) {
            certified = true;
        }
        receiver = item.address;
        if (certified) {
            receiver = "certified: " + item.cn + "\n";
        }
        return receiver;
    }

    private boolean isSmallerThanMin(Context app, PaymentItem paymentRequest) {
        long minAmount = WalletsMaster.getInstance().getMinOutputAmount();
        return paymentRequest.amount < minAmount;
    }

    private boolean isLargerThanBalance(Context app, PaymentItem paymentRequest) {
        BaseWallet wallet = WalletsMaster.getInstance().getWalletByIso(BRSharedPrefs.getCurrentWalletIso(app));
        return paymentRequest.amount > wallet.getCachedBalance(app) && paymentRequest.amount > 0;
    }

    private boolean notEnoughForFee(Context app, PaymentItem paymentRequest) {
        WalletsMaster m = WalletsMaster.getInstance();
        long feeForTx = m.feeForTransaction(paymentRequest.address, paymentRequest.amount);
        if (feeForTx == 0) {
            long maxOutput = m.getMaxOutputAmount();
            feeForTx = m.feeForTransaction(paymentRequest.address, maxOutput);
            return feeForTx != 0;
        }
        return false;
    }

    private static void showSpendNotAllowed(final Context app) {
        Log.d(TAG, "showSpendNotAllowed");
        ((Activity) app).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.Send_isRescanning), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, null, 0);
            }
        });
    }


    private class InsufficientFundsException extends Exception {

        public InsufficientFundsException(long amount, long balance) {
            super("Balance: " + balance + " satoshis, amount: " + amount + " satoshis.");
        }

    }

    private class AmountSmallerThanMinException extends Exception {

        public AmountSmallerThanMinException(long amount, long min) {
            super("Min: " + min + " satoshis, amount: " + amount + " satoshis.");
        }

    }

    private class SpendingNotAllowed extends Exception {

        public SpendingNotAllowed() {
            super("spending is not allowed at the moment");
        }

    }

    private class FeeNeedsAdjust extends Exception {

        public FeeNeedsAdjust(long amount, long balance, long fee) {
            super("Balance: " + balance + " satoshis, amount: " + amount + " satoshis, fee: " + fee + " satoshis.");
        }

    }

    private class FeeOutOfDate extends Exception {

        public FeeOutOfDate(long timestamp, long now) {
            super("FeeOutOfDate: timestamp: " + timestamp + ",now: " + now);
        }

    }

    private class SomethingWentWrong extends Exception {

        public SomethingWentWrong(String mess) {
            super(mess);
        }

    }

}
