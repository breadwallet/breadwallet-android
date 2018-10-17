package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.WorkerThread;
import android.text.format.DateUtils;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.core.BRCoreMasterPubKey;
import com.breadwallet.core.ethereum.BREthereumLightNode;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.PaperKeyActivity;
import com.breadwallet.presenter.activities.PaperKeyProveActivity;
import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.SendManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.entities.GenericTransactionMetaData;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.entities.TxMetaData;
import com.platform.tools.BRBitId;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;
import java.util.Timer;
import java.util.TimerTask;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 4/14/16.
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

public class PostAuth {
    public static final String TAG = PostAuth.class.getName();

    private String mCachedPaperKey;
    public CryptoRequest mCryptoRequest;
    //The user is stuck with endless authentication due to KeyStore bug.
    public static boolean mAuthLoopBugHappened;
    public static TxMetaData mTxMetaData;
    public SendManager.SendCompletion mSendCompletion;
    private BaseWalletManager mWalletManager;

    private CryptoTransaction mPaymentProtocolTx;
    private static PostAuth mInstance;

    private PostAuth() {
    }

    public static PostAuth getInstance() {
        if (mInstance == null) {
            mInstance = new PostAuth();
        }
        return mInstance;
    }

    public void onCreateWalletAuth(final Activity activity, boolean authAsked) {
        boolean success = WalletsMaster.getInstance(activity).generateRandomSeed(activity);
        if (success) {
            BreadApp.initialize(false);

            Intent intent = new Intent(activity, WriteDownActivity.class);
            intent.putExtra(WriteDownActivity.EXTRA_VIEW_REASON, WriteDownActivity.ViewReason.NEW_WALLET.getValue());
            activity.startActivity(intent);
            activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            activity.finish();
        } else {
            if (authAsked) {
                Log.e(TAG, "onCreateWalletAuth: WARNING!!!! LOOP");
                mAuthLoopBugHappened = true;
            }
            return;
        }
    }

    public void onPhraseCheckAuth(Activity activity, boolean authAsked) {
        String cleanPhrase;
        try {
            byte[] raw = BRKeyStore.getPhrase(activity, BRConstants.SHOW_PHRASE_REQUEST_CODE);
            if (raw == null) {
                BRReportsManager.reportBug(new NullPointerException("onPhraseCheckAuth: getPhrase = null"), true);
                return;
            }
            cleanPhrase = new String(raw);
        } catch (UserNotAuthenticatedException e) {
            if (authAsked) {
                Log.e(TAG, "onPhraseCheckAuth: WARNING!!!! LOOP");
                mAuthLoopBugHappened = true;
            }
            return;
        }
        Intent intent = new Intent(activity, PaperKeyActivity.class);
        intent.putExtra("phrase", cleanPhrase);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
    }

    public void onPhraseProveAuth(Activity activity, boolean authAsked) {
        String cleanPhrase;
        try {
            cleanPhrase = new String(BRKeyStore.getPhrase(activity, BRConstants.PROVE_PHRASE_REQUEST));
        } catch (UserNotAuthenticatedException e) {
            if (authAsked) {
                Log.e(TAG, "onPhraseProveAuth: WARNING!!!! LOOP");
                mAuthLoopBugHappened = true;
            }
            return;
        }
        Intent intent = new Intent(activity, PaperKeyProveActivity.class);
        intent.putExtra("phrase", cleanPhrase);
        activity.startActivity(intent);
        activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    public void onBitIDAuth(Context context, boolean authenticated) {
        BRBitId.completeBitID(context, authenticated);
    }

    public void onRecoverWalletAuth(final Activity activity, boolean authAsked) {
        if (Utils.isNullOrEmpty(mCachedPaperKey)) {
            Log.e(TAG, "onRecoverWalletAuth: phraseForKeyStore is null or empty");
            BRReportsManager.reportBug(new NullPointerException("onRecoverWalletAuth: phraseForKeyStore is or empty"));
            return;
        }

        try {
            boolean success = false;
            try {
                success = BRKeyStore.putPhrase(mCachedPaperKey.getBytes(),
                        activity, BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE);
            } catch (UserNotAuthenticatedException e) {
                if (authAsked) {
                    Log.e(TAG, "onRecoverWalletAuth: WARNING!!!! LOOP");
                    mAuthLoopBugHappened = true;

                }
                return;
            }

            if (!success) {
                if (authAsked)
                    Log.e(TAG, "onRecoverWalletAuth, !success && authAsked");
            } else {
                if (mCachedPaperKey.length() != 0) {
                    BRSharedPrefs.putPhraseWroteDown(activity, true);
                    byte[] seed = BRCoreKey.getSeedFromPhrase(mCachedPaperKey.getBytes());
                    byte[] authKey = BRCoreKey.getAuthPrivKeyForAPI(seed);
                    BRKeyStore.putAuthKey(authKey, activity);
                    BRCoreMasterPubKey mpk = new BRCoreMasterPubKey(mCachedPaperKey.getBytes(), true);
                    BRKeyStore.putMasterPublicKey(mpk.serialize(), activity);
                    BreadApp.initialize(false);

                    Intent intent = new Intent(activity, InputPinActivity.class);
                    intent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK);
                    activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    activity.startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);

                    mCachedPaperKey = null;
                }

            }

        } catch (Exception e) {
            e.printStackTrace();
            BRReportsManager.reportBug(e);
        }

    }

    @WorkerThread
    public void onPublishTxAuth(final Context context, final BaseWalletManager wm, final boolean authAsked, final SendManager.SendCompletion completion) {
        if (completion != null) {
            mSendCompletion = completion;
        }
        if (wm != null) mWalletManager = wm;
        final byte[] rawPhrase;
        try {
            rawPhrase = BRKeyStore.getPhrase(context, BRConstants.PAY_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            if (authAsked) {
                Log.e(TAG, "onPublishTxAuth: WARNING! Authentication Loop bug");
                mAuthLoopBugHappened = true;
            }
            return;
        }
        try {
            if (rawPhrase.length > 0) {
                if (mCryptoRequest != null && mCryptoRequest.amount != null && mCryptoRequest.address != null) {
                    final CryptoTransaction tx;
                    if (mCryptoRequest.getGenericTransactionMetaData() == null) {
                        tx = mWalletManager.createTransaction(mCryptoRequest.amount, mCryptoRequest.address);

                        if (tx == null) {
                            BRDialog.showCustomDialog(context, context.getString(R.string.Alert_error), context.getString(R.string.Send_insufficientFunds),
                                    context.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                                        @Override
                                        public void onClick(BRDialogView brDialogView) {
                                            brDialogView.dismiss();
                                        }
                                    }, null, null, 0);
                            return;
                        }

                        mTxMetaData = new TxMetaData();
                        mTxMetaData.comment = mCryptoRequest.message;
                        mTxMetaData.exchangeCurrency = BRSharedPrefs.getPreferredFiatIso(context);
                        BigDecimal fiatExchangeRate = mWalletManager.getFiatExchangeRate(context);
                        mTxMetaData.exchangeRate = fiatExchangeRate == null ? 0 : fiatExchangeRate.doubleValue();
                        mTxMetaData.fee = mWalletManager.getTxFee(tx).toPlainString();
                        mTxMetaData.txSize = tx.getTxSize().intValue();
                        mTxMetaData.blockHeight = BRSharedPrefs.getLastBlockHeight(context, mWalletManager.getIso());
                        mTxMetaData.creationTime = (int) (System.currentTimeMillis() / DateUtils.SECOND_IN_MILLIS);
                        mTxMetaData.deviceId = BRSharedPrefs.getDeviceId(context);
                        mTxMetaData.classVersion = 1;

                    } else {
                        WalletEthManager ethWallet = (WalletEthManager) mWalletManager;
                        GenericTransactionMetaData genericTransactionMetaData = mCryptoRequest.getGenericTransactionMetaData();
                        tx = new CryptoTransaction(ethWallet.getWallet().createTransactionGeneric(
                                genericTransactionMetaData.getTargetAddress(),
                                genericTransactionMetaData.getAmount(),
                                genericTransactionMetaData.getAmountUnit(), String.valueOf(genericTransactionMetaData.getGasPrice()),
                                genericTransactionMetaData.getGasPriceUnit(),
                                String.valueOf(genericTransactionMetaData.getGasLimit()), genericTransactionMetaData.getData()));
                    }

                    // We use dynamic gas for ETH and ERC20 tokens. BUT not when we have a generic transaction CALL_REQUEST.
                    if (mCryptoRequest.getGenericTransactionMetaData() == null && (mWalletManager.getIso().equalsIgnoreCase(WalletEthManager.ETH_CURRENCY_CODE)
                            || WalletsMaster.getInstance(context).isIsoErc20(context, mWalletManager.getIso()))) {
                        final WalletEthManager walletEthManager = WalletEthManager.getInstance(context);
                        final Timer timeoutTimer = new Timer();
                        final WalletEthManager.OnTransactionEventListener onTransactionEventListener = new WalletEthManager.OnTransactionEventListener() {
                            @Override
                            public void onTransactionEvent(BREthereumLightNode.Listener.TransactionEvent event) {
                                switch (event) {
                                    case GAS_ESTIMATE_UPDATED:
                                        Log.d(TAG, "onTransactionEvent: UPDATED");
                                        timeoutTimer.cancel();
                                        continueWithPayment(context, rawPhrase, tx);
                                        break;
                                }
                            }
                        };
                        // If getting the gas estimate takes longer than 30 seconds, show error message.
                        timeoutTimer.schedule(new TimerTask() {
                            @Override
                            public void run() {
                                Log.e(TAG, "timeoutTimer: did not update gas");
                                walletEthManager.removeTransactionEventListener(onTransactionEventListener);
                                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                                    @Override
                                    public void run() {
                                        BRDialog.showSimpleDialog(context, context.getString(R.string.Alerts_sendFailure), context.getString(R.string.Alert_timedOut));
                                    }
                                });
                            }
                        }, DateUtils.MINUTE_IN_MILLIS / 2);

                        walletEthManager.addTransactionEventListener(onTransactionEventListener);
                        walletEthManager.getWallet().estimateGas(tx.getEtherTx());
                    } else {
                        continueWithPayment(context, rawPhrase, tx);
                    }

                } else {
                    throw new NullPointerException("payment item is null");
                }
            } else {
                Log.e(TAG, "onPublishTxAuth: paperKey length is 0!");
                BRReportsManager.reportBug(new NullPointerException("onPublishTxAuth: paperKey length is 0"));
                return;
            }
        } finally {
            mCryptoRequest = null;
        }

    }

    private void continueWithPayment(final Context context, byte[] rawPhrase, CryptoTransaction transaction) {
        if (transaction.getEtherTx() != null) {
            mWalletManager.watchTransactionForHash(transaction, new BaseWalletManager.OnHashUpdated() {
                @Override
                public void onUpdated(String hash) {
                    if (mSendCompletion != null) {
                        mSendCompletion.onCompleted(hash, true);
                        mSendCompletion = null;
                    }
                    stampMetaData(context, hash.getBytes());
                }
            });
        }
        final byte[] txHash = mWalletManager.signAndPublishTransaction(transaction, rawPhrase);
        if (!Utils.isNullOrEmpty(txHash)) {
            if (mSendCompletion != null) {
                mSendCompletion.onCompleted(transaction.getHash(), true);
                mSendCompletion = null;
            }
            stampMetaData(context, txHash);
        }

    }

    public static void stampMetaData(Context activity, byte[] txHash) {
        if (mTxMetaData != null) {
            KVStoreManager.putTxMetaData(activity, mTxMetaData, txHash);
        } else {
            Log.e(TAG, "stampMetaData: mTxMetaData is null!");
        }
    }

    public void onPaymentProtocolRequest(final Activity activity, boolean authAsked) {
        final byte[] paperKey;
        try {
            paperKey = BRKeyStore.getPhrase(activity, BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            if (authAsked) {
                Log.e(TAG, "onPaymentProtocolRequest: WARNING!!!! LOOP");
                mAuthLoopBugHappened = true;
            }
            return;
        }
        if (paperKey == null || paperKey.length < 10 || mPaymentProtocolTx == null) {
            Log.d(TAG, "onPaymentProtocolRequest() returned: rawSeed is malformed: " + (paperKey == null ? "" : paperKey.length));
            return;
        }

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                byte[] txHash = WalletsMaster.getInstance(activity).getCurrentWallet(activity).signAndPublishTransaction(mPaymentProtocolTx, paperKey);
                if (Utils.isNullOrEmpty(txHash)) {
                    Log.e(TAG, "run: txHash is null");
                }
                mPaymentProtocolTx = null;
            }
        });

    }

    public void setCachedPaperKey(String paperKey) {
        this.mCachedPaperKey = paperKey;
    }

    public void setPaymentItem(CryptoRequest cryptoRequest) {
        this.mCryptoRequest = cryptoRequest;
    }

    public void setTmpPaymentRequestTx(CryptoTransaction tx) {
        this.mPaymentProtocolTx = tx;
    }

    public void onCanaryCheck(final Activity activity, boolean authAsked) {
        String canary = null;
        try {
            canary = BRKeyStore.getCanary(activity, BRConstants.CANARY_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            if (authAsked) {
                Log.e(TAG, "onCanaryCheck: WARNING!!!! LOOP");
                mAuthLoopBugHappened = true;
            }
            return;
        }
        if (canary == null || !canary.equalsIgnoreCase(BRConstants.CANARY_STRING)) {
            byte[] phrase;
            try {
                phrase = BRKeyStore.getPhrase(activity, BRConstants.CANARY_REQUEST_CODE);
            } catch (UserNotAuthenticatedException e) {
                if (authAsked) {
                    Log.e(TAG, "onCanaryCheck: WARNING!!!! LOOP");
                    mAuthLoopBugHappened = true;
                }
                return;
            }

            if (phrase != null) {
                Log.e(TAG, "onCanaryCheck: Canary wasn't there, but the phrase persists, adding canary to keystore.");
                try {
                    BRKeyStore.putCanary(BRConstants.CANARY_STRING, activity, 0);
                } catch (UserNotAuthenticatedException e) {
                    if (authAsked) {
                        Log.e(TAG, "onCanaryCheck: WARNING!!!! LOOP");
                        mAuthLoopBugHappened = true;
                    }
                    return;
                }
            }
        }
        WalletsMaster.getInstance(activity).startTheWalletIfExists(activity);
    }

}
