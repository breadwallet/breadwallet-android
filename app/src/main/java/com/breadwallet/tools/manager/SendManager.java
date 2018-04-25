package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseTransaction;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.exceptions.AmountSmallerThanMinException;
import com.breadwallet.wallet.exceptions.FeeNeedsAdjust;
import com.breadwallet.wallet.exceptions.FeeOutOfDate;
import com.breadwallet.wallet.exceptions.InsufficientFundsException;
import com.breadwallet.wallet.exceptions.SomethingWentWrong;
import com.breadwallet.wallet.exceptions.SpendingNotAllowed;
import com.breadwallet.wallet.wallets.etherium.WalletEthManager;
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

    public static boolean sendTransaction(final Context app, final CryptoRequest payment, final BaseWalletManager walletManager) {
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
                    //if the fee (for BTC and BCH only) was updated more than 24 hours ago then try updating the fee
                    if (walletManager.getIso(app).equalsIgnoreCase("BTC") || walletManager.getIso(app).equalsIgnoreCase("BCH")) {
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
                    BigDecimal minAmount = walletManager.getMinOutputAmount(app);
                    errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                    errMessage[0] = String.format(Locale.getDefault(), app.getString(R.string.PaymentProtocol_Errors_smallPayment),
                            BRConstants.symbolBits + minAmount.divide(new BigDecimal(100), BRConstants.ROUNDING_MODE));
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
    private static void tryPay(final Context app, final CryptoRequest paymentRequest, final BaseWalletManager walletManager) throws InsufficientFundsException,
            AmountSmallerThanMinException, SpendingNotAllowed, FeeNeedsAdjust, SomethingWentWrong {
        if (paymentRequest == null) {
            Log.e(TAG, "tryPay: ERROR: paymentRequest: null");
            String message = "paymentRequest is null";
            BRReportsManager.reportBug(new RuntimeException("paymentRequest is malformed: " + message), true);
            throw new SomethingWentWrong("wrong parameters: paymentRequest");
        }
//        long amount = paymentRequest.amount;
        BigDecimal balance = walletManager.getCachedBalance(app);
        BigDecimal minOutputAmount = walletManager.getMinOutputAmount(app);
        final BigDecimal maxOutputAmount = walletManager.getMaxOutputAmount(app);

        //not enough for fee
        if (paymentRequest.notEnoughForFee(app, walletManager)) {
            //weird bug when the core WalletsMaster is NULL
            if (maxOutputAmount.compareTo(new BigDecimal(-1)) == 0) {
                BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"), true);
                throw new SomethingWentWrong("getMaxOutputAmount is -1, meaning _wallet is NULL");
            }
            // max you can spend is smaller than the min you can spend
            if (maxOutputAmount.compareTo(new BigDecimal(0)) == 0 || maxOutputAmount.compareTo(minOutputAmount) < 0) {
                throw new InsufficientFundsException(paymentRequest.amount, balance);
            }

            throw new FeeNeedsAdjust(paymentRequest.amount, balance, new BigDecimal(-1));
        }

        // check if spending is allowed
        if (!BRSharedPrefs.getAllowSpend(app, walletManager.getIso(app))) {
            throw new SpendingNotAllowed();
        }

        //check if amount isn't smaller than the min amount
        if (paymentRequest.isSmallerThanMin(app, walletManager)) {
            throw new AmountSmallerThanMinException(paymentRequest.amount, minOutputAmount);
        }

        //amount is larger than balance
        if (paymentRequest.isLargerThanBalance(app, walletManager)) {
            throw new InsufficientFundsException(paymentRequest.amount, balance);
        }

        // payment successful
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                PostAuth.getInstance().setPaymentItem(paymentRequest);
                confirmPay(app, paymentRequest, walletManager);
            }
        });

    }

    private static void showAdjustFee(final Activity app, final CryptoRequest item, final BaseWalletManager walletManager) {
        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
        BigDecimal maxAmountDouble = walletManager.getMaxOutputAmount(app);
        if (maxAmountDouble.compareTo(new BigDecimal(-1)) == 0) {
            BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"));
            return;
        }
        if (maxAmountDouble.compareTo(new BigDecimal(0)) == 0) {
            BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), app.getString(R.string.Send_nilFeeError), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismissWithAnimation();
                }
            }, null, null, 0);
        } else {
            if (Utils.isNullOrEmpty(item.address)) throw new RuntimeException("can't happen");
            BigDecimal fee = wm.getEstimatedFee(maxAmountDouble, item.address);
            if (fee.compareTo(new BigDecimal(0)) <= 0) {
                BRReportsManager.reportBug(new RuntimeException("fee is weird:  " + fee));
                BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), app.getString(R.string.Send_nilFeeError), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, null, 0);
                return;
            }

            String formattedCrypto = CurrencyUtils.getFormattedAmount(app, wm.getIso(app), maxAmountDouble.negate());
            String formattedFiat = CurrencyUtils.getFormattedAmount(app, BRSharedPrefs.getPreferredFiatIso(app), wm.getFiatForSmallestCrypto(app, maxAmountDouble, null).negate());

            String posButtonText = String.format("%s (%s)", formattedCrypto, formattedFiat);

            item.amount = maxAmountDouble;

            BRDialog.showCustomDialog(app, app.getString(R.string.Send_nilFeeError), "Send max?", posButtonText, "No thanks", new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismissWithAnimation();
                    PostAuth.getInstance().setPaymentItem(item);
                    confirmPay(app, item, walletManager);

                }
            }, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismissWithAnimation();
                }
            }, null, 0);
        }

    }

    private static void confirmPay(final Context ctx, final CryptoRequest request, final BaseWalletManager wm) {
        if (ctx == null) {
            Log.e(TAG, "confirmPay: context is null");
            return;
        }

        String message = createConfirmation(ctx, request, wm);

        BigDecimal minOutput = request.isAmountRequested ? wm.getMinOutputAmountPossible() : wm.getMinOutputAmount(ctx);

        //amount can't be less than the min
        if (minOutput != null && request.amount.abs().compareTo(minOutput) <= 0) {
            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.PaymentProtocol_Errors_smallTransaction),
                    CurrencyUtils.getFormattedAmount(ctx, wm.getIso(ctx), minOutput));

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
            Log.e(TAG, "confirmPay: totalSent: " + wm.getTotalSent(ctx));
            Log.e(TAG, "confirmPay: request.amount: " + request.amount);
            Log.e(TAG, "confirmPay: total limit: " + BRKeyStore.getTotalLimit(ctx, wm.getIso(ctx)));
            Log.e(TAG, "confirmPay: limit: " + BRKeyStore.getSpendLimit(ctx, wm.getIso(ctx)));
        }

        if (wm.getTotalSent(ctx).add(request.amount).compareTo(BRKeyStore.getTotalLimit(ctx, wm.getIso(ctx))) > 0) {
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

    private static String createConfirmation(Context ctx, CryptoRequest request, final BaseWalletManager wm) {

        String receiver;
        boolean certified = false;
        if (request.cn != null && request.cn.length() != 0) {
            certified = true;
        }
        receiver = wm.decorateAddress(ctx, request.address);
        if (certified) {
            receiver = "certified: " + request.cn + "\n";
        }

        String iso = BRSharedPrefs.getPreferredFiatIso(ctx);
        BigDecimal feeForTx = wm.getEstimatedFee(request.amount, request.address);
        if (feeForTx.compareTo(new BigDecimal(0)) <= 0) {
            BigDecimal maxAmount = wm.getMaxOutputAmount(ctx);
            if (maxAmount != null && maxAmount.compareTo(new BigDecimal(-1)) == 0) {
                BRReportsManager.reportBug(new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL"), true);
            }
            if (maxAmount != null && maxAmount.compareTo(new BigDecimal(0)) == 0) {
                BRDialog.showCustomDialog(ctx, "", ctx.getString(R.string.Alerts_sendFailure), ctx.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismiss();
                    }
                }, null, null, 0);

                return null;
            }
            if (maxAmount != null) {
                feeForTx = wm.getEstimatedFee(request.amount, request.address);
                feeForTx = feeForTx.add(wm.getCachedBalance(ctx).subtract(request.amount.abs()));
            }
        }
        BigDecimal amount = request.amount.abs();
        final BigDecimal total = amount.add(feeForTx);
        String formattedCryptoAmount = CurrencyUtils.getFormattedAmount(ctx, wm.getIso(ctx), amount);
        String formattedCryptoFee = CurrencyUtils.getFormattedAmount(ctx, wm.getIso(ctx), feeForTx);
        String formattedCryptoTotal = CurrencyUtils.getFormattedAmount(ctx, wm.getIso(ctx), total);

        String formattedAmount = CurrencyUtils.getFormattedAmount(ctx, iso, wm.getFiatForSmallestCrypto(ctx, amount, null));
        String formattedFee = CurrencyUtils.getFormattedAmount(ctx, iso, wm.getFiatForSmallestCrypto(ctx, feeForTx, null));
        String formattedTotal = CurrencyUtils.getFormattedAmount(ctx, iso, wm.getFiatForSmallestCrypto(ctx, total, null));

        if (WalletsMaster.getInstance(ctx).isIsoErc20(ctx, wm.getIso(ctx))) {
            formattedCryptoTotal = "";
            formattedTotal = "";
            BaseWalletManager ethWm = WalletEthManager.getInstance(ctx);
            formattedCryptoFee = CurrencyUtils.getFormattedAmount(ctx, ethWm.getIso(ctx), feeForTx);
            formattedFee = CurrencyUtils.getFormattedAmount(ctx, iso, ethWm.getFiatForSmallestCrypto(ctx, feeForTx, null));
        }

        //formatted text
        return receiver + "\n\n"
                + ctx.getString(R.string.Confirmation_amountLabel) + " " + formattedCryptoAmount + " (" + formattedAmount + ")"
                + "\n" + ctx.getString(R.string.Confirmation_feeLabel) + " " + formattedCryptoFee + " (" + formattedFee + ")"
                + "\n" + ctx.getString(R.string.Confirmation_totalLabel) + " " + formattedCryptoTotal + " (" + formattedTotal + ")"
                + (request.message == null ? "" : "\n\n" + request.message);
    }

}
