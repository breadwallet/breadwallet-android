package com.breadwallet.wallet.wallets.bitcoincash;

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
import com.breadwallet.core.BRCoreChainParams;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.BRCoreMerkleBlock;
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.core.BRCorePeerManager;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreWallet;
import com.breadwallet.core.BRCoreWalletManager;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.entities.BRMerkleBlockEntity;
import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.BRTransactionEntity;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.entities.TxUiHolder;
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
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.sqlite.MerkleBlockDataSource;
import com.breadwallet.tools.sqlite.PeerDataSource;
import com.breadwallet.tools.sqlite.TransactionDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.wallet.abstracts.OnBalanceChangedListener;
import com.breadwallet.wallet.wallets.bitcoin.BitcoinUriParser;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWallet;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.configs.WalletUiConfiguration;
import com.breadwallet.wallet.wallets.exceptions.AmountSmallerThanMinException;
import com.breadwallet.wallet.wallets.exceptions.FeeNeedsAdjust;
import com.breadwallet.wallet.wallets.exceptions.FeeOutOfDate;
import com.breadwallet.wallet.wallets.exceptions.InsufficientFundsException;
import com.breadwallet.wallet.wallets.exceptions.SomethingWentWrong;
import com.breadwallet.wallet.wallets.exceptions.SpendingNotAllowed;
import com.google.firebase.crash.FirebaseCrash;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

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
public class WalletBitcoinCash extends BRCoreWalletManager implements BaseWallet {

    private static final String TAG = WalletBitcoinManager.class.getName();

    private static String BCH = "BCH";
    private static String mBCH = "mBCH";
    private static String Bits = "Bits";

    public final long MAX_BTC = 21000000;
    private final static long FEE_EXPIRATION_MILLIS = 72 * 60 * 60 * 1000L;

    private static WalletBitcoinCash instance;
    private WalletUiConfiguration uiConfig;

    private boolean isInitiatingWallet;
    public List<OnBalanceChangedListener> balanceListeners = new ArrayList<>();
    private boolean timedOut;
    private boolean sending;


    public static WalletBitcoinCash getInstance(Context app) {
        if (instance == null) {
            byte[] rawPubKey = BRKeyStore.getMasterPublicKey(app);
            BRCoreMasterPubKey pubKey = new BRCoreMasterPubKey(rawPubKey, false);
            long time = BRKeyStore.getWalletCreationTime(app);

            instance = new WalletBitcoinCash(app, pubKey, /**BuildConfig.BITCOIN_TESTNET ? BRCoreChainParams.bcashChainParams :*/BRCoreChainParams.bcashChainParams, time);
        }
        return instance;
    }

    private WalletBitcoinCash(final Context app, BRCoreMasterPubKey masterPubKey,
                              BRCoreChainParams chainParams,
                              double earliestPeerTime) {
        super(masterPubKey, chainParams, earliestPeerTime);
        String firstAddress = masterPubKey.getPubKeyAsCoreKey().address();
        BRSharedPrefs.putFirstAddress(app, firstAddress);
        long fee = BRSharedPrefs.getFeePerKb(app, BCH);
        long economyFee = BRSharedPrefs.getEconomyFeePerKb(app, BCH);
        if (fee == 0) {
            fee = getWallet().getDefaultFeePerKb();
            BREventManager.getInstance().pushEvent("wallet.didUseDefaultFeePerKB");
        }
        getWallet().setFeePerKb(BRSharedPrefs.getFavorStandardFee(app, BCH) ? fee : economyFee);
        if (BRSharedPrefs.getStartHeight(app, BCH) == 0)
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    BRSharedPrefs.putStartHeight(app, BCH, getPeerManager().getLastBlockHeight());
                }
            });

//            BRPeerManager.getInstance().updateFixedPeer(ctx);//todo reimplement the fixed peer
//        balanceListeners = new ArrayList<>();
        uiConfig = new WalletUiConfiguration("#478559", true, true, false);
    }

//
//    public void setBalance(final Context context, long balance) {
//        if (context == null) {
//            Log.e(TAG, "setBalance: FAILED TO SET THE BALANCE");
//            return;
//        }
//        BRSharedPrefs.putCachedBalance(context, BCH, balance);
//        WalletsMaster.refreshAddress(context);
//
//        for (OnBalanceChanged listener : balanceListeners) {
//            if (listener != null) listener.onBalanceChanged(BCH, balance);
//
//        }
//    }

//    public void refreshBalance(Activity app) {
//        long natBal = WalletsMaster.getInstance().nativeBalance();
//        if (natBal != -1) {
//            setBalance(app, natBal);
//        } else {
//            Log.e(TAG, "UpdateUI, nativeBalance is -1 meaning _wallet was null!");
//        }
//    }


    @Override
    public void addBalanceChangedListener(OnBalanceChangedListener list) {
        if (list != null && !balanceListeners.contains(list))
            balanceListeners.add(list);
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
                    if (now - BRSharedPrefs.getFeeTime(app, BCH) >= FEE_EXPIRATION_MILLIS) {
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
                        long time = BRSharedPrefs.getFeeTime(app, BCH);
                        if (time <= 0 || now - time >= FEE_EXPIRATION_MILLIS) {
                            Log.e(TAG, "sendTransaction: fee out of date even after fetching...");
                            throw new FeeOutOfDate(BRSharedPrefs.getFeeTime(app, BCH), now);
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
                    long minAmount = getWallet().getMinOutputAmount();
                    errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                    errMessage[0] = String.format(Locale.getDefault(), app.getString(R.string.PaymentProtocol_Errors_smallPayment),
                            BRConstants.symbolBits + new BigDecimal(minAmount).divide(new BigDecimal(100), BRConstants.ROUNDING_MODE));
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
    public BRCoreTransaction[] getTransactions() {
        return getWallet().getTransactions();
    }

    @Override
    public void updateFee(Context app) {
        //todo implement that
    }

    @Override
    public void refreshAddress(Context app) {
        String address = getReceiveAddress(app);
        if (Utils.isNullOrEmpty(address)) {
            Log.e(TAG, "refreshAddress: WARNING, retrieved address:" + address);
        }
        BRSharedPrefs.putReceiveAddress(app, address, BCH);
    }

    @Override
    public List<TxUiHolder> getTxUiHolders() {
        return null;
    }

    @Override
    public boolean generateWallet(Context app) {
        //no need, using the same private key as Bitcoin
        return false;
    }

    @Override
    public boolean initWallet(final Context app) {
        if (ActivityUTILS.isMainThread()) throw new NetworkOnMainThreadException();
        if (isInitiatingWallet) return false;
        isInitiatingWallet = true;
        try {
            Log.d(TAG, "initWallet:" + Thread.currentThread().getName());
            if (app == null) {
                Log.e(TAG, "initWallet: app is null");
                return false;
            }
            getPeerManager().connect();

        } finally {
            isInitiatingWallet = false;
        }

        return true;
    }

    @Override
    public String getSymbol(Context app) {

        String currencySymbolString = BRConstants.symbolBits;
        if (app != null) {
            int unit = BRSharedPrefs.getCryptoDenomination(app, BCH);
            switch (unit) {
                case BRConstants.CURRENT_UNIT_BITS:
                    currencySymbolString = BRConstants.symbolBits + "c";
                    break;
                case BRConstants.CURRENT_UNIT_MBITS:
                    currencySymbolString = "m" + BRConstants.symbolBitcoin + "C";
                    break;
                case BRConstants.CURRENT_UNIT_BITCOINS:
                    currencySymbolString = BRConstants.symbolBitcoin + "C";
                    break;
            }
        }
        return currencySymbolString;
    }

    @Override
    public String getIso(Context app) {
        return BCH;
    }

    @Override
    public String getName(Context app) {
        return "BitcoinCash";
    }

    @Override
    public String getDenomination(Context app) {
        return null;
    }

    @Override
    public String getReceiveAddress(Context app) {
        return getWallet().getReceiveAddress().stringify();
    }

    @Override
    public int getMaxDecimalPlaces(Context app) {
        int unit = BRSharedPrefs.getCryptoDenomination(app, BCH);
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
        return BRSharedPrefs.getCachedBalance(app, BCH);
    }

    @Override
    public long getTotalSent(Context app) {
        return getWallet().getTotalSent();
    }

    @Override
    public void wipeData(Context app) {
        //todo implement
    }

    @Override
    public boolean trySweepWallet(Context app, String privKey) {
        return false;
    }

    @Override
    public BRCoreTransaction[] loadTransactions() {
        return new BRCoreTransaction[0];
    }

    @Override
    public BRCoreMerkleBlock[] loadBlocks() {
        return new BRCoreMerkleBlock[0];
    }

    @Override
    public BRCorePeer[] loadPeers() {
        return new BRCorePeer[0];
    }

    @Override
    public void setCashedBalance(Context app, long balance) {
        BRSharedPrefs.putCachedBalance(app, BCH, balance);
        refreshAddress(app);
        for (OnBalanceChangedListener listener : balanceListeners) {
            if (listener != null) listener.onBalanceChanged(BCH, balance);
        }
    }

    @Override
    public BigDecimal getMaxAmount(Context app) {
        //return max bitcoin
        return new BigDecimal(MAX_BTC);
    }

    @Override
    public boolean tryUri(Context app, String uriStr) {
        return false;
    }

    @Override
    public WalletUiConfiguration getUiConfiguration() {
        return uiConfig;
    }

    @Override
    public long getFiatBalance(Context app) {
        return getFiatForSmallestCrypto(app, new BigDecimal(getCachedBalance(app))).longValue();
    }

    @Override
    public BigDecimal getFiatForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return null;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByIso(iso);
        if (ent == null) return null;
        double rate = ent.rate;
        //get crypto amount
        BigDecimal cryptoAmount = amount.divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE);
        return cryptoAmount.multiply(new BigDecimal(rate)).multiply(new BigDecimal(100));
    }

    @Override
    public BigDecimal getCryptoForFiat(Context app, BigDecimal amount) {
//        if (amount.doubleValue() == 0) return null;
//        String iso = BRSharedPrefs.getPreferredFiatIso(app);
//        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByIso(iso);
//        if (ent == null) return null;
//        double rate = ent.rate;
//        //convert c to $.
//        BigDecimal fiatAmount = amount.divide(new BigDecimal(100), ROUNDING_MODE);
//        int unit = BRSharedPrefs.getCryptoDenomination(app, BCH);
//        BigDecimal result = new BigDecimal(0);
//        switch (unit) {
//            case BRConstants.CURRENT_UNIT_BITS:
//                result = fiatAmount.divide(new BigDecimal(rate), 2, ROUNDING_MODE).multiply(new BigDecimal("1000000"));
//                break;
//            case BRConstants.CURRENT_UNIT_MBITS:
//                result = fiatAmount.divide(new BigDecimal(rate), 5, ROUNDING_MODE).multiply(new BigDecimal("1000"));
//                break;
//            case BRConstants.CURRENT_UNIT_BITCOINS:
//                result = fiatAmount.divide(new BigDecimal(rate), 8, ROUNDING_MODE);
//                break;
//        }
//        return result;
        //todo implement
        return null;

    }

    @Override
    public BigDecimal getCryptoForSmallestCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return null;
        BigDecimal result = new BigDecimal(0);
        int unit = BRSharedPrefs.getCryptoDenomination(app, BCH);
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
    public BigDecimal getSmallestCryptoForCrypto(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return null;
        BigDecimal result = new BigDecimal(0);
        int unit = BRSharedPrefs.getCryptoDenomination(app, BCH);
        switch (unit) {
            case BRConstants.CURRENT_UNIT_BITS:
                result = amount.multiply(new BigDecimal("100"));
                break;
            case BRConstants.CURRENT_UNIT_MBITS:
                result = amount.multiply(new BigDecimal("100000"));
                break;
            case BRConstants.CURRENT_UNIT_BITCOINS:
                result = amount.multiply(new BigDecimal("100000000"));
                break;
        }
        return result;
    }

    @Override
    public BigDecimal getSmallestCryptoForFiat(Context app, BigDecimal amount) {
        if (amount.doubleValue() == 0) return null;
        String iso = BRSharedPrefs.getPreferredFiatIso(app);
        CurrencyEntity ent = CurrencyDataSource.getInstance(app).getCurrencyByIso(iso);
        if (ent == null) return null;
        double rate = ent.rate;
        //convert c to $.
        BigDecimal fiatAmount = amount.divide(new BigDecimal(100), ROUNDING_MODE);
        return fiatAmount.divide(new BigDecimal(rate), 8, ROUNDING_MODE).multiply(new BigDecimal("100000000"));
    }

    @Override
    public void txPublished(final String error) {
        super.txPublished(error);
        final Context app = BreadApp.getBreadContext();
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (app instanceof Activity)
                    BRAnimator.showBreadSignal((Activity) app, Utils.isNullOrEmpty(error) ? app.getString(R.string.Alerts_sendSuccess) : app.getString(R.string.Alert_error),
                            Utils.isNullOrEmpty(error) ? app.getString(R.string.Alerts_sendSuccessSubheader) : "Error: " + error, Utils.isNullOrEmpty(error) ? R.drawable.ic_check_mark_white : R.drawable.ic_error_outline_black_24dp, new BROnSignalCompletion() {
                                @Override
                                public void onComplete() {
                                    if (!((Activity) app).isDestroyed())
                                        ((Activity) app).getFragmentManager().popBackStack();
                                }
                            });
            }
        });
    }

    @Override
    public void balanceChanged(long balance) {
        super.balanceChanged(balance);
        Context app = BreadApp.getBreadContext();
        setCashedBalance(app, balance);
    }

    @Override
    public void onTxAdded(BRCoreTransaction transaction) {
        super.onTxAdded(transaction);
//        Log.d(TAG, "onTxAdded: " + String.format("tx.length: %d, blockHeight: %d, timestamp: %d, amount: %d, hash: %s", tx.length, blockHeight, timestamp, amount, hash));
//
//        final Context ctx = BreadApp.getBreadContext();
//        if (amount > 0) {
//            BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
//                @Override
//                public void run() {
//                    WalletsMaster master = WalletsMaster.getInstance();
//                    String am = CurrencyUtils.getFormattedCurrencyString(ctx, "BTC", master.getCurrentWallet(ctx).getCryptoForSmallestCrypto(ctx, new BigDecimal(amount)));
//                    String amCur = CurrencyUtils.getFormattedCurrencyString(ctx, BRSharedPrefs.getPreferredFiatIso(ctx), master.getCurrentWallet(ctx).getFiatForSmallestCrypto(ctx, new BigDecimal(amount)));
//                    String formatted = String.format("%s (%s)", am, amCur);
//                    final String strToShow = String.format(ctx.getString(R.string.TransactionDetails_received), formatted);
//
//                    new Handler().postDelayed(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (!BRToast.isToastShown()) {
//                                BRToast.showCustomToast(ctx, strToShow,
//                                        BreadApp.DISPLAY_HEIGHT_PX / 2, Toast.LENGTH_LONG, R.drawable.toast_layout_black);
//                                AudioManager audioManager = (AudioManager) ctx.getSystemService(Context.AUDIO_SERVICE);
//                                if (audioManager.getRingerMode() == AudioManager.RINGER_MODE_NORMAL) {
//                                    final MediaPlayer mp = MediaPlayer.create(ctx, R.raw.coinflip);
//                                    if (mp != null) try {
//                                        mp.start();
//                                    } catch (IllegalArgumentException ex) {
//                                        Log.e(TAG, "run: ", ex);
//                                    }
//                                }
//
//                                if (!BreadApp.isAppInBackground(ctx) && BRSharedPrefs.getShowNotification(ctx))
//                                    BRNotificationManager.sendNotification((Activity) ctx, R.drawable.notification_icon, ctx.getString(R.string.app_name), strToShow, 1);
//                            }
//                        }
//                    }, 1000);
//
//
//                }
//            });
//        }
//        if (ctx != null)
//            TransactionDataSource.getInstance(ctx).putTransaction(new BRTransactionEntity(tx, blockHeight, timestamp, hash));
//        else
//            Log.e(TAG, "onTxAdded: ctx is null!");
        //todo implement
    }

    @Override
    public void onTxUpdated(String hash, int blockHeight, int timeStamp) {
        super.onTxUpdated(hash, blockHeight, timeStamp);
        //        Log.d(TAG, "onTxUpdated: " + String.format("hash: %s, blockHeight: %d, timestamp: %d", hash, blockHeight, timeStamp));
//        Context ctx = BreadApp.getBreadContext();
//        if (ctx != null) {
//            TransactionDataSource.getInstance(ctx).updateTxBlockHeight(hash, blockHeight, timeStamp);
//
//        } else {
//            Log.e(TAG, "onTxUpdated: Failed, ctx is null");
//        }
    }

    @Override
    public void onTxDeleted(String hash, int notifyUser, int recommendRescan) {
        super.onTxDeleted(hash, notifyUser, recommendRescan);
//               Log.e(TAG, "onTxDeleted: " + String.format("hash: %s, notifyUser: %d, recommendRescan: %d", hash, notifyUser, recommendRescan));
//        final Context ctx = BreadApp.getBreadContext();
//        if (ctx != null) {
//            BRSharedPrefs.putScanRecommended(ctx, BCH, true);
//        } else {
//            Log.e(TAG, "onTxDeleted: Failed! ctx is null");
//        }
    }


    /**
     * Try transaction and throw appropriate exceptions if something was wrong
     * BLOCKS
     */
    private void tryPay(final Context app, final PaymentItem paymentRequest) throws InsufficientFundsException,
            AmountSmallerThanMinException, SpendingNotAllowed, FeeNeedsAdjust, SomethingWentWrong {
//        if (paymentRequest == null || paymentRequest.address == null) {
//            Log.e(TAG, "tryPay: ERROR: paymentRequest: " + paymentRequest);
//            String message = paymentRequest == null ? "paymentRequest is null" : "addresses is null";
//            BRReportsManager.reportBug(new RuntimeException("paymentRequest is malformed: " + message), true);
//            throw new SomethingWentWrong("wrong parameters: paymentRequest");
//        }
//        long amount = paymentRequest.amount;
//        long balance = getCachedBalance(app);
//        final WalletsMaster m = WalletsMaster.getInstance();
//        long minOutputAmount = WalletsMaster.getInstance().getMinOutputAmount();
//        final long maxOutputAmount = WalletsMaster.getInstance().getMaxOutputAmount();
//
//        // check if spending is allowed
//        if (!BRSharedPrefs.getAllowSpend(app, BCH)) {
//            throw new SpendingNotAllowed();
//        }
//
//        //check if amount isn't smaller than the min amount
//        if (isSmallerThanMin(app, paymentRequest)) {
//            throw new AmountSmallerThanMinException(amount, minOutputAmount);
//        }
//
//        //amount is larger than balance
//        if (isLargerThanBalance(app, paymentRequest)) {
//            throw new InsufficientFundsException(amount, balance);
//        }
//
//        //not enough for fee
//        if (notEnoughForFee(app, paymentRequest)) {
//            //weird bug when the core WalletsMaster is NULL
//            if (maxOutputAmount == -1) {
//                BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"), true);
//            }
//            // max you can spend is smaller than the min you can spend
//            if (maxOutputAmount == 0 || maxOutputAmount < minOutputAmount) {
//                throw new InsufficientFundsException(amount, balance);
//            }
//
//            long feeForTx = m.feeForTransaction(paymentRequest.address, paymentRequest.amount);
//            throw new FeeNeedsAdjust(amount, balance, feeForTx);
//        }
//        // payment successful
//        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
//            @Override
//            public void run() {
//                byte[] tmpTx = m.tryTransaction(paymentRequest.address, paymentRequest.amount);
//                if (tmpTx == null) {
//                    //something went wrong, failed to create tx
//                    ((Activity) app).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            BRDialog.showCustomDialog(app, "", app.getString(R.string.Alerts_sendFailure), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
//                                @Override
//                                public void onClick(BRDialogView brDialogView) {
//                                    brDialogView.dismiss();
//                                }
//                            }, null, null, 0);
//
//                        }
//                    });
//                    return;
//                }
//                paymentRequest.serializedTx = tmpTx;
//                PostAuth.getInstance().setPaymentItem(paymentRequest);
//                confirmPay(app, paymentRequest);
//            }
//        });

    }

    private void showAdjustFee(final Activity app, PaymentItem item) {
//        WalletsMaster m = WalletsMaster.getInstance();
//        long maxAmountDouble = m.getMaxOutputAmount();
//        if (maxAmountDouble == -1) {
//            BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"));
//            return;
//        }
//        if (maxAmountDouble == 0) {
//            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), "Insufficient amount for transaction fee", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
//                @Override
//                public void onClick(BRDialogView brDialogView) {
//                    brDialogView.dismissWithAnimation();
//                }
//            }, null, null, 0);
//        } else {
////            long fee = m.feeForTransaction(item.addresses[0], maxAmountDouble);
////            feeForTx += (m.getBalance(app) - request.amount) % 100;
////            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), "Insufficient amount for transaction fee", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
////                @Override
////                public void onClick(BRDialogView brDialogView) {
////                    brDialogView.dismissWithAnimation();
////                }
////            }, null, null, 0);
//            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), "Insufficient amount for transaction fee", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
//                @Override
//                public void onClick(BRDialogView brDialogView) {
//                    brDialogView.dismissWithAnimation();
//                }
//            }, null, null, 0);
//            //todo fix this fee adjustment
//        }

    }


//    private void confirmPay(final Context ctx, final PaymentItem request) {
//        if (ctx == null) {
//            Log.e(TAG, "confirmPay: context is null");
//            return;
//        }
//
//        String message = createConfirmation(ctx, request);
//
//        double minOutput;
//        if (request.isAmountRequested) {
//            minOutput = WalletsMaster.getInstance().getMinOutputAmountRequested();
//        } else {
//            minOutput = WalletsMaster.getInstance().getMinOutputAmount();
//        }
//
//        //amount can't be less than the min
//        if (request.amount < minOutput) {
//            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.PaymentProtocol_Errors_smallTransaction),
//                    BRConstants.symbolBits + new BigDecimal(minOutput).divide(new BigDecimal("100")));
//
//
//            ((Activity) ctx).runOnUiThread(new Runnable() {
//                @Override
//                public void run() {
//                    BRDialog.showCustomDialog(ctx, ctx.getString(R.string.Alerts_sendFailure), bitcoinMinMessage, ctx.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
//                        @Override
//                        public void onClick(BRDialogView brDialogView) {
//                            brDialogView.dismiss();
//                        }
//                    }, null, null, 0);
//                }
//            });
//            return;
//        }
//        boolean forcePin = false;
//
//        Log.e(TAG, "confirmPay: totalSent: " + WalletsMaster.getInstance().getTotalSent());
//        Log.e(TAG, "confirmPay: request.amount: " + request.amount);
//        Log.e(TAG, "confirmPay: total limit: " + AuthManager.getInstance().getTotalLimit(ctx));
//        Log.e(TAG, "confirmPay: limit: " + BRKeyStore.getSpendLimit(ctx));
//
//        if (WalletsMaster.getInstance().getTotalSent() + request.amount > AuthManager.getInstance().getTotalLimit(ctx)) {
//            forcePin = true;
//        }
//
//        //successfully created the transaction, authenticate user
//        AuthManager.getInstance().authPrompt(ctx, "", message, forcePin, false, new BRAuthCompletion() {
//            @Override
//            public void onComplete() {
//                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
//                    @Override
//                    public void run() {
//                        PostAuth.getInstance().onPublishTxAuth(ctx, false);
//                        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
//                            @Override
//                            public void run() {
//                                BRAnimator.killAllFragments((Activity) ctx);
//                                BRAnimator.startBreadIfNotStarted((Activity) ctx);
//                            }
//                        });
//
//                    }
//                });
//
//            }
//
//            @Override
//            public void onCancel() {
//                //nothing
//            }
//        });
//
//    }

//    private String createConfirmation(Context ctx, PaymentItem request) {
//        String receiver = getReceiver(request);
//
//        String iso = BRSharedPrefs.getPreferredFiatIso(ctx);
//
//        WalletsMaster m = WalletsMaster.getInstance();
//        long feeForTx = m.feeForTransaction(request.address, request.amount);
//        if (feeForTx == 0) {
//            long maxAmount = m.getMaxOutputAmount();
//            if (maxAmount == -1) {
//                BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"), true);
//            }
//            if (maxAmount == 0) {
//                BRDialog.showCustomDialog(ctx, "", ctx.getString(R.string.Alerts_sendFailure), ctx.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
//                    @Override
//                    public void onClick(BRDialogView brDialogView) {
//                        brDialogView.dismiss();
//                    }
//                }, null, null, 0);
//
//                return null;
//            }
//            feeForTx = m.feeForTransaction(request.address, maxAmount);
//            feeForTx += (getCachedBalance(ctx) - request.amount) % 100;
//        }
//        final long total = request.amount + feeForTx;
//        String formattedAmountBTC = CurrencyUtils.getFormattedCurrencyString(ctx, getIso(ctx), m.getCurrentWallet(ctx).getCryptoForSmallestCrypto(ctx, new BigDecimal(request.amount)));
//        String formattedFeeBTC = CurrencyUtils.getFormattedCurrencyString(ctx, getIso(ctx), m.getCurrentWallet(ctx).getCryptoForSmallestCrypto(ctx, new BigDecimal(feeForTx)));
//        String formattedTotalBTC = CurrencyUtils.getFormattedCurrencyString(ctx, getIso(ctx), m.getCurrentWallet(ctx).getCryptoForSmallestCrypto(ctx, new BigDecimal(total)));
//
//        String formattedAmount = CurrencyUtils.getFormattedCurrencyString(ctx, iso, m.getCurrentWallet(ctx).getFiatForSmallestCrypto(ctx, new BigDecimal(request.amount)));
//        String formattedFee = CurrencyUtils.getFormattedCurrencyString(ctx, iso, m.getCurrentWallet(ctx).getFiatForSmallestCrypto(ctx, new BigDecimal(feeForTx)));
//        String formattedTotal = CurrencyUtils.getFormattedCurrencyString(ctx, iso, m.getCurrentWallet(ctx).getFiatForSmallestCrypto(ctx, new BigDecimal(total)));
//
//        //formatted text
//        return receiver + "\n\n"
//                + ctx.getString(R.string.Confirmation_amountLabel) + " " + formattedAmountBTC + " (" + formattedAmount + ")"
//                + "\n" + ctx.getString(R.string.Confirmation_feeLabel) + " " + formattedFeeBTC + " (" + formattedFee + ")"
//                + "\n" + ctx.getString(R.string.Confirmation_totalLabel) + " " + formattedTotalBTC + " (" + formattedTotal + ")"
//                + (request.comment == null ? "" : "\n\n" + request.comment);
//    }
//
//    private String getReceiver(PaymentItem item) {
//        String receiver;
//        boolean certified = false;
//        if (item.cn != null && item.cn.length() != 0) {
//            certified = true;
//        }
//        receiver = item.address;
//        if (certified) {
//            receiver = "certified: " + item.cn + "\n";
//        }
//        return receiver;
//    }

//    private boolean isSmallerThanMin(Context app, PaymentItem paymentRequest) {
//        long minAmount = WalletsMaster.getInstance().getMinOutputAmount();
//        return paymentRequest.amount < minAmount;
//    }
//
//    private boolean isLargerThanBalance(Context app, PaymentItem paymentRequest) {
//        return paymentRequest.amount > getCachedBalance(app) && paymentRequest.amount > 0;
//    }
//
//    private boolean notEnoughForFee(Context app, PaymentItem paymentRequest) {
//        WalletsMaster m = WalletsMaster.getInstance();
//        long feeForTx = m.feeForTransaction(paymentRequest.address, paymentRequest.amount);
//        if (feeForTx == 0) {
//            long maxOutput = m.getMaxOutputAmount();
//            feeForTx = m.feeForTransaction(paymentRequest.address, maxOutput);
//            return feeForTx != 0;
//        }
//        return false;
//    }

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


}
