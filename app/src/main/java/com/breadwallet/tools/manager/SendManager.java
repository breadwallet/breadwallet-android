package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.exceptions.AmountSmallerThanMinException;
import com.breadwallet.wallet.wallets.exceptions.FeeNeedsAdjust;
import com.breadwallet.wallet.wallets.exceptions.FeeOutOfDate;
import com.breadwallet.wallet.wallets.exceptions.InsufficientFundsException;
import com.breadwallet.wallet.wallets.exceptions.SomethingWentWrong;
import com.breadwallet.wallet.wallets.exceptions.SpendingNotAllowed;
import com.google.firebase.crash.FirebaseCrash;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 2/20/18.
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
public class SendManager {
    private static final String TAG = SendManager.class.getSimpleName();

    private static boolean timedOut;
    private static boolean sending;
    private final static long FEE_EXPIRATION_MILLIS = 72 * 60 * 60 * 1000L;

    public static boolean sendTransaction(final Context app, final PaymentItem payment, final BaseWalletManager walletManager) {
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
                    if (now - BRSharedPrefs.getFeeTime(app, walletManager.getIso(app)) >= FEE_EXPIRATION_MILLIS) {
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
                        walletManager.updateFee(app);
                        //if the fee is STILL out of date then fail with network problem message
                        long time = BRSharedPrefs.getFeeTime(app, walletManager.getIso(app));
                        if (time <= 0 || now - time >= FEE_EXPIRATION_MILLIS) {
                            Log.e(TAG, "sendTransaction: fee out of date even after fetching...");
                            throw new FeeOutOfDate(BRSharedPrefs.getFeeTime(app, walletManager.getIso(app)), now);
                        }
                    }
                    if (!timedOut)
                        tryPay(app, payment, walletManager);
                    else
                        FirebaseCrash.report(new NullPointerException("did not send, timedOut!"));
                    return; //return so no error is shown
                } catch (InsufficientFundsException ignored) {
                    errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                    errMessage[0] = "Insufficient Funds";
                } catch (AmountSmallerThanMinException e) {
                    long minAmount = walletManager.getWallet().getMinOutputAmount();
                    errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                    errMessage[0] = String.format(Locale.getDefault(), app.getString(R.string.PaymentProtocol_Errors_smallPayment),
                            BRConstants.symbolBits + new BigDecimal(minAmount).divide(new BigDecimal(100), BRConstants.ROUNDING_MODE));
                } catch (SpendingNotAllowed spendingNotAllowed) {
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
                    return;
                } catch (FeeNeedsAdjust feeNeedsAdjust) {
                    //offer to change amount, so it would be enough for fee
//                    showFailed(app); //just show failed for now
                    showAdjustFee((Activity) app, payment, walletManager);
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


    /**
     * Try transaction and throw appropriate exceptions if something was wrong
     * BLOCKS
     */
    private static void tryPay(final Context app, final PaymentItem paymentRequest, final BaseWalletManager walletManager) throws InsufficientFundsException,
            AmountSmallerThanMinException, SpendingNotAllowed, FeeNeedsAdjust, SomethingWentWrong {
        if (paymentRequest == null || paymentRequest.tx == null) {
            Log.e(TAG, "tryPay: ERROR: paymentRequest: " + paymentRequest);
            String message = paymentRequest == null ? "paymentRequest is null" : "tx is null";
            BRReportsManager.reportBug(new RuntimeException("paymentRequest is malformed: " + message), true);
            throw new SomethingWentWrong("wrong parameters: paymentRequest");
        }
//        long amount = paymentRequest.amount;
        long balance = walletManager.getCachedBalance(app);
        long minOutputAmount = walletManager.getWallet().getMinOutputAmount();
        final long maxOutputAmount = walletManager.getWallet().getMaxOutputAmount();

        if (paymentRequest.tx == null) {
            throw new SomethingWentWrong("transaction is null");
        }
        // check if spending is allowed
        if (!BRSharedPrefs.getAllowSpend(app, walletManager.getIso(app))) {
            throw new SpendingNotAllowed();
        }

        //check if amount isn't smaller than the min amount
        if (paymentRequest.isSmallerThanMin(app, walletManager)) {
            throw new AmountSmallerThanMinException(Math.abs(walletManager.getWallet().getTransactionAmount(paymentRequest.tx)), minOutputAmount);
        }

        //amount is larger than balance
        if (paymentRequest.isLargerThanBalance(app, walletManager)) {
            throw new InsufficientFundsException(Math.abs(walletManager.getWallet().getTransactionAmount(paymentRequest.tx)), balance);
        }

        //not enough for fee
        if (paymentRequest.notEnoughForFee(app, walletManager)) {
            //weird bug when the core WalletsMaster is NULL
            if (maxOutputAmount == -1) {
                BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"), true);
            }
            // max you can spend is smaller than the min you can spend
            if (maxOutputAmount == 0 || maxOutputAmount < minOutputAmount) {
                throw new InsufficientFundsException(Math.abs(walletManager.getWallet().getTransactionAmount(paymentRequest.tx)), balance);
            }

            long feeForTx = walletManager.getWallet().getTransactionFee(paymentRequest.tx);
            throw new FeeNeedsAdjust(Math.abs(walletManager.getWallet().getTransactionAmount(paymentRequest.tx)), balance, feeForTx);
        }
        // payment successful
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
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
                PostAuth.getInstance().setPaymentItem(paymentRequest);
                confirmPay(app, paymentRequest, walletManager);
            }
        });

    }

    private static void showAdjustFee(final Activity app, PaymentItem item, BaseWalletManager walletManager) {
        WalletsMaster m = WalletsMaster.getInstance(app);
        long maxAmountDouble = walletManager.getWallet().getMaxOutputAmount();
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


    private static void confirmPay(final Context ctx, final PaymentItem request, final BaseWalletManager walletManager) {
        if (ctx == null) {
            Log.e(TAG, "confirmPay: context is null");
            return;
        }

        String message = createConfirmation(ctx, request, walletManager);

        double minOutput;
        if (request.isAmountRequested) {
            minOutput = BRCoreTransaction.getMinOutputAmount();
        } else {
            minOutput = walletManager.getWallet().getMinOutputAmount();
        }

        //amount can't be less than the min
        if (Math.abs(walletManager.getWallet().getTransactionAmount(request.tx)) < minOutput) {
            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.PaymentProtocol_Errors_smallTransaction),
                    BRConstants.symbolBits + new BigDecimal(minOutput).divide(new BigDecimal("100")));


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

        if (Utils.isEmulatorOrDebug(ctx)) {
            Log.e(TAG, "confirmPay: totalSent: " + walletManager.getWallet().getTotalSent());
            Log.e(TAG, "confirmPay: request.amount: " + walletManager.getWallet().getTransactionAmount(request.tx));
            Log.e(TAG, "confirmPay: total limit: " + AuthManager.getInstance().getTotalLimit(ctx));
            Log.e(TAG, "confirmPay: limit: " + BRKeyStore.getSpendLimit(ctx));
        }

        if (walletManager.getWallet().getTotalSent() + Math.abs(walletManager.getWallet().getTransactionAmount(request.tx)) > AuthManager.getInstance().getTotalLimit(ctx)) {
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


    private static String createConfirmation(Context ctx, PaymentItem request, final BaseWalletManager walletManager) {

        String receiver;
        boolean certified = false;
        if (request.cn != null && request.cn.length() != 0) {
            certified = true;
        }
        receiver = walletManager.getWallet().getTransactionAddress(request.tx).stringify();
        if (certified) {
            receiver = "certified: " + request.cn + "\n";
        }


        String iso = BRSharedPrefs.getPreferredFiatIso(ctx);
        BaseWalletManager wallet = WalletsMaster.getInstance(ctx).getCurrentWallet(ctx);
        long feeForTx = walletManager.getWallet().getTransactionFee(request.tx);
        if (feeForTx <= 0) {
            long maxAmount = walletManager.getWallet().getMaxOutputAmount();
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
            request.tx = walletManager.getWallet().createTransaction(maxAmount, wallet.getWallet().getTransactionAddress(request.tx));
            feeForTx = walletManager.getWallet().getTransactionFee(request.tx);
            feeForTx += (walletManager.getCachedBalance(ctx) - Math.abs(walletManager.getWallet().getTransactionAmount(request.tx))) % 100;
        }
        long amount = Math.abs(walletManager.getWallet().getTransactionAmount(request.tx));
        final long total = amount + feeForTx;
        String formattedAmountBTC = CurrencyUtils.getFormattedAmount(ctx, walletManager.getIso(ctx), wallet.getCryptoForSmallestCrypto(ctx, new BigDecimal(amount)));
        String formattedFeeBTC = CurrencyUtils.getFormattedAmount(ctx, walletManager.getIso(ctx), wallet.getCryptoForSmallestCrypto(ctx, new BigDecimal(feeForTx)));
        String formattedTotalBTC = CurrencyUtils.getFormattedAmount(ctx, walletManager.getIso(ctx), wallet.getCryptoForSmallestCrypto(ctx, new BigDecimal(total)));

        String formattedAmount = CurrencyUtils.getFormattedAmount(ctx, iso, wallet.getFiatForSmallestCrypto(ctx, new BigDecimal(amount)));
        String formattedFee = CurrencyUtils.getFormattedAmount(ctx, iso, wallet.getFiatForSmallestCrypto(ctx, new BigDecimal(feeForTx)));
        String formattedTotal = CurrencyUtils.getFormattedAmount(ctx, iso, wallet.getFiatForSmallestCrypto(ctx, new BigDecimal(total)));

        //formatted text
        return receiver + "\n\n"
                + ctx.getString(R.string.Confirmation_amountLabel) + " " + formattedAmountBTC + " (" + formattedAmount + ")"
                + "\n" + ctx.getString(R.string.Confirmation_feeLabel) + " " + formattedFeeBTC + " (" + formattedFee + ")"
                + "\n" + ctx.getString(R.string.Confirmation_totalLabel) + " " + formattedTotalBTC + " (" + formattedTotal + ")"
                + (request.comment == null ? "" : "\n\n" + request.comment);
    }

}
