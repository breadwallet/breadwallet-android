package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.math.BigDecimal;
import java.util.Locale;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 4/25/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class BRSender {
    private static final String TAG = BRSender.class.getName();

    private static BRSender instance;

    private BRSender() {
    }

    public static BRSender getInstance() {
        if (instance == null) instance = new BRSender();
        return instance;
    }

    /**
     * Create tx from the PaymentItem object and try to send it
     */
    public void sendTransaction(final Context app, final PaymentItem request) {
        //array in order to be able to modify the first element from an inner block (can't be final)
        final String[] errTitle = {null};
        final String[] errMessage = {null};
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    tryPay(app, request);
                    return; //return so no error is shown
                } catch (InsufficientFundsException ignored) {
                    errTitle[0] = app.getString(R.string.Alerts_sendFailure);
//                    errMessage[0] = app.getString(R.string.insufficient_funds);
                } catch (AmountSmallerThanMinException e) {
                    long minAmount = BRWalletManager.getInstance().getMinOutputAmountRequested();
                    errTitle[0] = app.getString(R.string.Alerts_sendFailure);
                    errMessage[0] = String.format(Locale.getDefault(), app.getString(R.string.PaymentProtocol_Errors_smallPayment),
                            BRConstants.litecoinLowercase + new BigDecimal(minAmount).divide(new BigDecimal(100), BRConstants.ROUNDING_MODE));
                } catch (SpendingNotAllowed spendingNotAllowed) {
                    showSpendNotAllowed(app);
                    return;
                } catch (FeeNeedsAdjust feeNeedsAdjust) {
                    //offer to change amount, so it would be enough for fee
                    showFailed(app); //just show failed for now
                    return;
                }

                //show the message if we have one to show
                if (errTitle[0] != null && errMessage[0] != null)
                    ((Activity) app).runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BRDialog.showCustomDialog(app, errTitle[0], errMessage[0], "Ok", null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismiss();
                                }
                            }, null, null, 0);
                        }
                    });

            }
        }).start();

    }

    /**
     * Try transaction and throw appropriate exceptions if something was wrong
     * BLOCKS
     */
    public void tryPay(final Context app, final PaymentItem paymentRequest) throws InsufficientFundsException,
            AmountSmallerThanMinException, SpendingNotAllowed, FeeNeedsAdjust {
        if (paymentRequest == null || paymentRequest.addresses == null) {
            Log.e(TAG, "handlePay: WRONG PARAMS");
            String message = paymentRequest == null ? "paymentRequest is null" : "addresses is null";
            RuntimeException ex = new RuntimeException("paymentRequest is malformed: " + message);
            FirebaseCrash.report(ex);
            throw ex;
        }
        long amount = paymentRequest.amount;
        long balance = BRWalletManager.getInstance().getBalance(app);
        final BRWalletManager m = BRWalletManager.getInstance();
        long minOutputAmount = BRWalletManager.getInstance().getMinOutputAmount();
        final long maxOutputAmount = BRWalletManager.getInstance().getMaxOutputAmount();

        // check if spending is allowed
        if (!BRSharedPrefs.getAllowSpend(app)) {
            throw new SpendingNotAllowed();
        }

        //check if amount isn't smaller than the min amount
        if (isSmallerThanMin(app, paymentRequest)) {
            throw new AmountSmallerThanMinException(amount, balance);
        }

        //amount is larger than balance
        if (isLargerThanBalance(app, paymentRequest)) {
            throw new InsufficientFundsException(amount, balance);
        }

        //not enough for fee
        if (notEnoughForFee(app, paymentRequest)) {
            //weird bug when the core BRWalletManager is NULL
            if (maxOutputAmount == -1) {
                RuntimeException ex = new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL");
                FirebaseCrash.report(ex);
                throw ex;
            }
            // max you can spend is smaller than the min you can spend
            if (maxOutputAmount < minOutputAmount) {
                throw new InsufficientFundsException(amount, balance);
            }

            long feeForTx = m.feeForTransaction(paymentRequest.addresses[0], paymentRequest.amount);
            throw new FeeNeedsAdjust(amount, balance, feeForTx);
        }
        // payment successful
        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] tmpTx = m.tryTransaction(paymentRequest.addresses[0], paymentRequest.amount);
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
        }).start();


    }

    private void showFailed(final Context app) {
        BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), "",
                app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, null, 0);
//        final long maxOutputAmount = BRWalletManager.getInstance().getMaxOutputAmount();
//        final BRWalletManager m = BRWalletManager.getInstance();
//        final long amountToReduce = request.amount - maxOutputAmount;
//        String iso = BRSharedPrefs.getIso(app);
//        final String reduceBits = BRCurrency.getFormattedCurrencyString(app, "BTC", BRExchange.getAmountFromSatoshis(app, "BTC", new BigDecimal(amountToReduce)));
//        final String reduceCurrency = BRCurrency.getFormattedCurrencyString(app, iso, BRExchange.getAmountFromSatoshis(app, iso, new BigDecimal(amountToReduce)));
//        final String reduceBitsMinus = BRCurrency.getFormattedCurrencyString(app, "BTC", BRExchange.getAmountFromSatoshis(app, "BTC", new BigDecimal(amountToReduce).negate()));
//        final String reduceCurrencyMinus = BRCurrency.getFormattedCurrencyString(app, iso, BRExchange.getAmountFromSatoshis(app, iso, new BigDecimal(amountToReduce).negate()));
//
//        ((Activity) app).runOnUiThread(new Runnable() {
//            @Override
//            public void run() {
//                BRDialog.showCustomDialog(app, app.getString(R.string.Alerts_sendFailure), String.format(app.getString(R.string.reduce_payment_amount_by),
//                        reduceBits, reduceCurrency), String.format("%s (%s)", reduceBitsMinus, reduceCurrencyMinus), "Cancel", new BRDialogView.BROnClickListener() {
//                    @Override
//                    public void onClick(BRDialogView brDialogView) {
//
//                        new Thread(new Runnable() {
//                            @Override
//                            public void run() {
//                                final long newAmount = request.amount - amountToReduce;
//                                final byte[] tmpTx2 = m.tryTransaction(request.addresses[0], newAmount);
//
//                                if (tmpTx2 != null) {
//                                    request.serializedTx = tmpTx2;
//                                    PostAuth.getInstance().setPaymentItem(request);
//                                    request.amount = newAmount;
//                                    confirmPay(app, request);
//                                } else {
//                                    ((Activity) app).runOnUiThread(new Runnable() {
//                                        @Override
//                                        public void run() {
//                                            Log.e(TAG, "tmpTxObject2 is null!");
//                                            BRToast.showCustomToast(app, app.getString(R.string.Alerts_sendFailure),
//                                                    BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
//                                        }
//                                    });
//                                }
//                            }
//                        }).start();
//                        brDialogView.dismiss();
//                    }
//                }, new BRDialogView.BROnClickListener() {
//                    @Override
//                    public void onClick(BRDialogView brDialogView) {
//                        brDialogView.dismiss();
//                    }
//                }, null, 0);
//            }
//        });

    }

    //BLOCKS
    public void confirmPay(final Context ctx, final PaymentItem request) {
        if (ctx == null) {
            Log.e(TAG, "confirmPay: context is null");
            return;
        }

        String message = createConfirmation(ctx, request);

        double minOutput;
        if (request.isAmountRequested) {
            minOutput = BRWalletManager.getInstance().getMinOutputAmountRequested();
        } else {
            minOutput = BRWalletManager.getInstance().getMinOutputAmount();
        }

        //amount can't be less than the min
        if (request.amount < minOutput) {
            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.PaymentProtocol_Errors_smallTransaction),
                    BRConstants.litecoinLowercase + new BigDecimal(minOutput).divide(new BigDecimal("100")));


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

        Log.e(TAG, "confirmPay: totalSent: " + BRWalletManager.getInstance().getTotalSent());
        Log.e(TAG, "confirmPay: request.amount: " + request.amount);
        Log.e(TAG, "confirmPay: total limit: " + AuthManager.getInstance().getTotalLimit(ctx));
        Log.e(TAG, "confirmPay: limit: " + BRKeyStore.getSpendLimit(ctx));

        if (BRWalletManager.getInstance().getTotalSent() + request.amount > AuthManager.getInstance().getTotalLimit(ctx)) {
            forcePin = true;
        }

        //successfully created the transaction, authenticate user
        AuthManager.getInstance().authPrompt(ctx, "", message, forcePin, new BRAuthCompletion() {
            @Override
            public void onComplete() {
                PostAuth.getInstance().onPublishTxAuth(ctx, true);
                BRAnimator.killAllFragments((Activity) ctx);
                BRAnimator.startBreadIfNotStarted((Activity) ctx);
            }

            @Override
            public void onCancel() {
                //nothing
            }
        });

    }

    public String createConfirmation(Context ctx, PaymentItem request) {
        String receiver = getReceiver(request);

        String iso = BRSharedPrefs.getIso(ctx);

        BRWalletManager m = BRWalletManager.getInstance();
        long feeForTx = m.feeForTransaction(request.addresses[0], request.amount);
        if (feeForTx == 0) {
            long maxAmount = m.getMaxOutputAmount();
            if (maxAmount == -1) {
                RuntimeException ex = new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL");
                FirebaseCrash.report(ex);
                throw ex;
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
            feeForTx = m.feeForTransaction(request.addresses[0], maxAmount);
            feeForTx += (BRWalletManager.getInstance().getBalance(ctx) - request.amount) % 100;
        }
        final long total = request.amount + feeForTx;
        String formattedAmountBTC = BRCurrency.getFormattedCurrencyString(ctx, "LTC", BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(request.amount)));
        String formattedFeeBTC = BRCurrency.getFormattedCurrencyString(ctx, "LTC", BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(feeForTx)));
        String formattedTotalBTC = BRCurrency.getFormattedCurrencyString(ctx, "LTC", BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(total)));

        String formattedAmount = BRCurrency.getFormattedCurrencyString(ctx, iso, BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(request.amount)));
        String formattedFee = BRCurrency.getFormattedCurrencyString(ctx, iso, BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(feeForTx)));
        String formattedTotal = BRCurrency.getFormattedCurrencyString(ctx, iso, BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(total)));

        //formatted text
        return receiver + "\n\n"
                + "amount: " + formattedAmountBTC + " (" + formattedAmount + ")"
                + "\nnetwork fee: +" + formattedFeeBTC + " (" + formattedFee + ")"
                + "\ntotal: " + formattedTotalBTC + " (" + formattedTotal + ")"
                + (request.comment == null ? "" : "\n\n" + request.comment);
    }

    public String getReceiver(PaymentItem item) {
        String receiver;
        boolean certified = false;
        if (item.cn != null && item.cn.length() != 0) {
            certified = true;
        }
        StringBuilder allAddresses = new StringBuilder();
        for (String s : item.addresses) {
            allAddresses.append(s + ", ");
        }
        receiver = allAddresses.toString();
        allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
        if (certified) {
            receiver = "certified: " + item.cn + "\n";
        }
        return receiver;
    }

    public boolean isSmallerThanMin(Context app, PaymentItem paymentRequest) {
        long minAmount = BRWalletManager.getInstance().getMinOutputAmountRequested();
        return paymentRequest.amount < minAmount;
    }

    public boolean isLargerThanBalance(Context app, PaymentItem paymentRequest) {
        return paymentRequest.amount > BRWalletManager.getInstance().getBalance(app) && paymentRequest.amount > 0;
    }

    public boolean notEnoughForFee(Context app, PaymentItem paymentRequest) {
        BRWalletManager m = BRWalletManager.getInstance();
        long feeForTx = m.feeForTransaction(paymentRequest.addresses[0], paymentRequest.amount);
        if (feeForTx == 0) {
            feeForTx = m.feeForTransaction(paymentRequest.addresses[0], m.getMaxOutputAmount());
            return feeForTx != 0;
        }
        return false;
    }

    public static void showSpendNotAllowed(final Context app) {
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

        public AmountSmallerThanMinException(long amount, long balance) {
            super("Balance: " + balance + " satoshis, amount: " + amount + " satoshis.");
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
}
