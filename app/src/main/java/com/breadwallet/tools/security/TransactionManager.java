package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.BreadActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.customviews.BRToast;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BreadDialog;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.math.BigDecimal;
import java.util.Locale;

import static com.breadwallet.R.string.pay;
import static com.breadwallet.R.string.request;
import static com.breadwallet.R.string.syncing;
import static com.breadwallet.R.string.syncing_in_progress;


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
public class TransactionManager {
    private static final String TAG = TransactionManager.class.getName();

    private static TransactionManager instance;

    private TransactionManager() {
    }

    public static TransactionManager getInstance() {
        if (instance == null) instance = new TransactionManager();
        return instance;
    }


    public void sendTrandaction(Context context, final PaymentItem request) {

    }

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
            final String bitcoinMinMessage = String.format(Locale.getDefault(), ctx.getString(R.string.bitcoin_payment_cant_be_less),
                    BRConstants.bitcoinLowercase + new BigDecimal(minOutput).divide(new BigDecimal("100")));


            BreadDialog.showCustomDialog(ctx, ctx.getString(R.string.payment_failed), bitcoinMinMessage, "Close", null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismiss();
                }
            }, null, null, 0);

            return;
        }

        //successfully created the transaction, authenticate user
        AuthManager.getInstance().authPrompt(ctx, "", message, false, new BRAuthCompletion() {
            @Override
            public void onComplete() {
                PostAuthenticationProcessor.getInstance().onPublishTxAuth(ctx, false);
            }

            @Override
            public void onCancel() {
                //nothing
            }
        });

    }

    public String createConfirmation(Context ctx, PaymentItem request) {
        String receiver = getReceiver(request);

        String iso = SharedPreferencesManager.getIso(ctx);

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
                BreadDialog.showCustomDialog(ctx, "", ctx.getString(R.string.insufficient_funds_for_fee), "Close", null, new BRDialogView.BROnClickListener() {
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
        String formattedAmountBTC = BRCurrency.getFormattedCurrencyString(ctx, "BTC", BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(request.amount)));
        String formattedFeeBTC = BRCurrency.getFormattedCurrencyString(ctx, "BTC", BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(feeForTx)));
        String formattedTotalBTC = BRCurrency.getFormattedCurrencyString(ctx, "BTC", BRExchange.getBitcoinForSatoshis(ctx, new BigDecimal(total)));

        String formattedAmount = BRCurrency.getFormattedCurrencyString(ctx, iso, BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(request.amount)));
        String formattedFee = BRCurrency.getFormattedCurrencyString(ctx, iso, BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(feeForTx)));
        String formattedTotal = BRCurrency.getFormattedCurrencyString(ctx, iso, BRExchange.getAmountFromSatoshis(ctx, iso, new BigDecimal(total)));

        //formatted text
        return receiver + "\n\n"
                + "amount: " + formattedAmountBTC + " (" + formattedAmount + ")"
                + "\nnetwork fee: +" + formattedFeeBTC + " (" + formattedFee + ")"
                + "\ntotal: " + formattedTotalBTC + " (" + formattedTotal + ")";
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
        if (paymentRequest.amount < minAmount) {
            Log.e(TAG, "pay: FAIL: bitcoin payment is less than the minimum.");
            final String bitcoinMinMessage = String.format(Locale.getDefault(), app.getString(R.string.bitcoin_payment_cant_be_less),
                    BRConstants.bitcoinLowercase + new BigDecimal(minAmount).divide(new BigDecimal(100), BRConstants.ROUNDING_MODE));
            BreadDialog.showCustomDialog(app, app.getString(R.string.could_not_make_payment), bitcoinMinMessage, "Cancel", null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismiss();
                }
            }, null, null, 0);
            return true;
        } else {
            return false;
        }
    }

    public boolean isLargerThanBalance(Context app, PaymentItem paymentRequest) {
        if (paymentRequest.amount > BRWalletManager.getInstance().getBalance(app) && paymentRequest.amount > 0) {
            Log.e(TAG, "pay: FAIL: bitcoin payment is less than the minimum.");
            BreadDialog.showCustomDialog(app, app.getString(R.string.could_not_make_payment), app.getString(R.string.insufficient_funds), "Ok", null, new BRDialogView.BROnClickListener() {
                @Override
                public void onClick(BRDialogView brDialogView) {
                    brDialogView.dismiss();
                }
            }, null, null, 0);
            return true;
        } else {
            return false;
        }
    }
//    public boolean haveEnoughForSendingMin(Context app, PaymentItem paymentRequest){
//        return paymentRequest.amount <= BRWalletManager.getInstance().getBalance(app) && paymentRequest.amount > 0;
//    }

    public void tryPay(final Context app, final PaymentItem paymentRequest) {
        if (paymentRequest == null || paymentRequest.addresses == null || paymentRequest.amount <= 0) {
            Log.e(TAG, "handlePay: WRONG PARAMS");
            return;
        }

        // check if spending is allowed
        if (!SharedPreferencesManager.getAllowSpend(app)) {
            showSpendNotAllowed(app);
            return;
        }

        //check if amount isn't smaller than the min amount
        if (isSmallerThanMin(app, paymentRequest)) {
            return;
        }

        //amount is larger than balance
        if (isLargerThanBalance(app, paymentRequest)) {
            return;
        }


//        final BRWalletManager m = BRWalletManager.getInstance();
//        byte[] tmpTx = m.tryTransaction(paymentRequest.addresses[0], paymentRequest.amount);
//        long feeForTx = m.feeForTransaction(paymentRequest.addresses[0], paymentRequest.amount);
//
//        //try transaction failed so check why
//        if (tmpTx == null && paymentRequest.amount <= BRWalletManager.getInstance().getBalance(app) && paymentRequest.amount > 0) {
//            final long maxOutputAmount = m.getMaxOutputAmount();
//            if (maxOutputAmount == -1) {
//                RuntimeException ex = new RuntimeException("getMaxOutputAmount is -1, meaning _wallet is NULL");
//                FirebaseCrash.report(ex);
//                throw ex;
//            }
//            //check if max you can send isn't smaller than the min amount
//            if (maxOutputAmount < BRWalletManager.getInstance().getMinOutputAmount()) {
//                Log.e(TAG, "pay: FAIL: insufficient funds for fee.");
//
//                BreadDialog.showCustomDialog(app, app.getString(R.string.insufficient_funds), app.getString(R.string.insufficient_funds_for_fee), "Cancel", null, new BRDialogView.BROnClickListener() {
//                    @Override
//                    public void onClick(BRDialogView brDialogView) {
//                        brDialogView.dismiss();
//                    }
//                }, null, null, 0);
//
//                return;
//            }
//
//            //offer to change amount, so it would be enough for fee
//            final long amountToReduce = paymentRequest.amount - maxOutputAmount;
//            String iso = SharedPreferencesManager.getIso(app);
//            //BigDecimal rate = BigDecimal.valueOf(CurrencyDataSource.getInstance(app).getCurrencyByIso(iso).rate);
//            String reduceBits = BRCurrency.getFormattedCurrencyString(app, "BTC", BRExchange.getAmountFromSatoshis(app, "BTC", new BigDecimal(amountToReduce)));
//            String reduceCurrency = BRCurrency.getFormattedCurrencyString(app, iso, BRExchange.getAmountFromSatoshis(app, iso, new BigDecimal(amountToReduce)));
//            String reduceBitsMinus = BRCurrency.getFormattedCurrencyString(app, "BTC", BRExchange.getAmountFromSatoshis(app, "BTC", new BigDecimal(amountToReduce).negate()));
//            String reduceCurrencyMinus = BRCurrency.getFormattedCurrencyString(app, iso, BRExchange.getAmountFromSatoshis(app, iso, new BigDecimal(amountToReduce).negate()));
//
//            BreadDialog.showCustomDialog(app, app.getString(R.string.insufficient_funds_for_fee), String.format(app.getString(R.string.reduce_payment_amount_by),
//                    reduceBits, reduceCurrency), String.format("%s (%s)", reduceBitsMinus, reduceCurrencyMinus), "Cancel", new BRDialogView.BROnClickListener() {
//                @Override
//                public void onClick(BRDialogView brDialogView) {
//
//                    long newAmount = paymentRequest.amount - amountToReduce;
//                    byte[] tmpTx2 = m.tryTransaction(paymentRequest.addresses[0], newAmount);
//                    if (tmpTx2 != null) {
//                        PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx2);
//                        paymentRequest.amount = newAmount;
//                        confirmPay(app, paymentRequest);
//                    } else {
//                        Log.e(TAG, "tmpTxObject2 is null!");
//                        BRToast.showCustomToast(app, app.getString(R.string.insufficient_funds),
//                                BreadActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
//                    }
//                    brDialogView.dismiss();
//                }
//            }, new BRDialogView.BROnClickListener() {
//                @Override
//                public void onClick(BRDialogView brDialogView) {
//                    brDialogView.dismiss();
//                }
//            }, null, 0);
//
//            return;
//
//            //Insufficient funds, show message to user
//        } else if (tmpTx == null && paymentRequest.amount >= BRWalletManager.getInstance().getBalance(app) && paymentRequest.amount > 0) {
//            Log.e(TAG, "pay: FAIL: offer To Change The Amount.");
//            BRWalletManager.getInstance().offerToChangeTheAmount(app, app.getString(R.string.insufficient_funds));
//            return;
//        }
//
//        // payment successful
//        PostAuthenticationProcessor.getInstance().setTmpTx(tmpTx);
//        Log.d(TAG, "pay: feeForTx: " + feeForTx + ", amount: " + paymentRequest.amount +
//                ", getBalance(): " + BRWalletManager.getInstance().getBalance(app));
//        if ((feeForTx != 0 && paymentRequest.amount + feeForTx < BRWalletManager.getInstance().getBalance(app)) || (paymentRequest.isAmountRequested)) {
//            Log.d(TAG, "pay: SUCCESS: going to confirmPay");
//            confirmPay(app, paymentRequest);
//        } else {
//            Log.d(TAG, "pay: FAIL: insufficient funds");
//
//            BreadDialog.showCustomDialog(app, "", app.getString(R.string.insufficient_funds), "Close", null, new BRDialogView.BROnClickListener() {
//                @Override
//                public void onClick(BRDialogView brDialogView) {
//                    brDialogView.dismiss();
//                }
//            }, null, null, 0);
//
//
//        }

    }

    private static void showSpendNotAllowed(final Context app) {
        Log.d(TAG, "showSpendNotAllowed");
        ((Activity) app).runOnUiThread(new Runnable() {
            @Override
            public void run() {
                BreadDialog.showCustomDialog(app, app.getString(syncing_in_progress), app.getString(R.string.wait_for_sync_to_finish), app.getString(R.string.ok), null, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, null, 0);
            }
        });
    }


}
