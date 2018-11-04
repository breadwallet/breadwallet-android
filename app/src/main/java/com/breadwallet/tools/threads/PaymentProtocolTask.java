package com.breadwallet.tools.threads;

import android.app.Activity;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCorePaymentProtocolRequest;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreTransactionOutput;
import com.breadwallet.core.BRCoreWallet;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.tools.exceptions.CertificateChainNotFound;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.BytesUtil;
import com.breadwallet.tools.util.CustomLogger;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.util.Locale;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 5/9/16.
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

public class PaymentProtocolTask extends AsyncTask<String, String, String> {
    private static final String TAG = PaymentProtocolTask.class.getName();
    private HttpURLConnection urlConnection;
    private String certName;
    private BRCorePaymentProtocolRequest paymentProtocolRequest;
    private int certified = 0;
    private Activity app;

    //params[0] = uri, params[1] = label
    @Override
    protected String doInBackground(String... params) {
        app = (Activity) BreadApp.getBreadContext();
        InputStream in;
        try {
            Log.e(TAG, "the uri: " + params[0]);
            URL url = new URL(params[0]);
            BaseWalletManager walletManager = WalletsMaster.getInstance(app).getCurrentWallet(app);
            if (!walletManager.getIso().equalsIgnoreCase(BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE)
                    && !walletManager.getIso().equalsIgnoreCase(BaseBitcoinWalletManager.BITCASH_CURRENCY_CODE)) {
                throw new RuntimeException("Can't happen, Payment protocol for: " + walletManager.getIso());
            }
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/" + walletManager.getName().toLowerCase() + "-paymentrequest");
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            urlConnection.setUseCaches(false);
            in = urlConnection.getInputStream();

            if (in == null) {
                Log.e(TAG, "The inputStream is null!");
                return null;
            }
            byte[] serializedBytes = BytesUtil.readBytesFromStream(in);
            if (Utils.isNullOrEmpty(serializedBytes)) {
                Log.e(TAG, "serializedBytes are null!");
                return null;
            }

            paymentProtocolRequest = new BRCorePaymentProtocolRequest(serializedBytes);
//            //Logging
            BRCoreTransactionOutput[] outputs = paymentProtocolRequest.getOutputs();
            StringBuilder allAddresses = new StringBuilder();
            for (BRCoreTransactionOutput output : outputs) {
                allAddresses.append(output.getAddress()).append(", ");
                if (Utils.isNullOrEmpty(output.getAddress()) || !new BRCoreAddress(output.getAddress()).isValid()) {
                    if (app != null)
                        BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.Send_invalidAddressTitle) + ": " + output.getAddress(), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, null, 0);
                    paymentProtocolRequest = null;
                    return null;
                }
            }

            allAddresses.delete(allAddresses.length() - 2, allAddresses.length());


            long totalAmount = 0;
            for (BRCoreTransactionOutput output : outputs) {
                totalAmount += output.getAmount();
            }

            CustomLogger.logThis("Signature", String.valueOf(paymentProtocolRequest.getSignature().length),
                    "pkiType", paymentProtocolRequest.getPKIType(), "pkiData", String.valueOf(paymentProtocolRequest.getPKIData().length));
            CustomLogger.logThis("network", paymentProtocolRequest.getNetwork(), "time", String.valueOf(paymentProtocolRequest.getTime()),
                    "expires", String.valueOf(paymentProtocolRequest.getExpires()), "memo", paymentProtocolRequest.getMemo(),
                    "paymentURL", paymentProtocolRequest.getPaymentURL(), "merchantDataSize",
                    String.valueOf(paymentProtocolRequest.getMerchantData().length), "address", allAddresses.toString(),
                    "amount", String.valueOf(totalAmount));
            //end logging
            if (paymentProtocolRequest.getExpires() != 0 && paymentProtocolRequest.getTime() > paymentProtocolRequest.getExpires()) {
                Log.e(TAG, "Request is expired");
                if (app != null)
                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.PaymentProtocol_Errors_requestExpired), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                paymentProtocolRequest = null;
                return null;
            }
            //todo fix certs
//            List<X509Certificate> certList = X509CertificateValidator.getCertificateFromBytes(serializedBytes);
//            certName = X509CertificateValidator.certificateValidation(certList, paymentProtocolRequest);

        } catch (Exception e) {
            Log.e(TAG, "doInBackground: ", e);
            if (e instanceof java.net.UnknownHostException) {
                if (app != null)
                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.PaymentProtocol_Errors_corruptedDocument), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                paymentProtocolRequest = null;
            } else if (e instanceof FileNotFoundException) {
                if (app != null)
                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), app.getString(R.string.PaymentProtocol_Errors_badPaymentRequest), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                paymentProtocolRequest = null;
            } else if (e instanceof SocketTimeoutException) {
                if (app != null)
                    BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error), "Connection timed-out", app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);
                paymentProtocolRequest = null;
            } else if (e instanceof CertificateChainNotFound) {
                Log.e(TAG, "No certificates!", e);
            } else {
                if (app != null)
                    BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title), app.getString(R.string.PaymentProtocol_Errors_badPaymentRequest) + ":" + e.getMessage(), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                        @Override
                        public void onClick(BRDialogView brDialogView) {
                            brDialogView.dismissWithAnimation();
                        }
                    }, null, null, 0);

                paymentProtocolRequest = null;
            }
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        if (paymentProtocolRequest == null) return null;
        if (!paymentProtocolRequest.getPKIType().equals("none") && certName == null) {
            certified = 2;
        } else if (!paymentProtocolRequest.getPKIType().equals("none") && certName != null) {
            certified = 1;
        }
        certName = extractCNFromCertName(certName);
        if (certName == null || certName.isEmpty())
            certName = params[1];
        if (certName == null || certName.isEmpty())
            certName = paymentProtocolRequest.getOutputs()[0].getAddress();
        return null;
    }

    @Override
    protected void onPostExecute(String result) {
        super.onPostExecute(result);

        if (app == null) return;
        if (paymentProtocolRequest == null || paymentProtocolRequest.getOutputs() == null ||
                paymentProtocolRequest.getOutputs()[0].getAddress().length() == 0) {
            return;
        }
        final String certification;
        if (certified == 0) {
            certification = certName + "\n";
        } else {
            if (certName == null || certName.isEmpty()) {
                certification = "\u274C " + certName + "\n";
                BRDialog.showCustomDialog(app, app.getString(R.string.PaymentProtocol_Errors_untrustedCertificate), "", app.getString(R.string.JailbreakWarnings_ignore), app.getString(R.string.Button_cancel), new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        continueWithThePayment(app, certification);
                    }
                }, new BRDialogView.BROnClickListener() {
                    @Override
                    public void onClick(BRDialogView brDialogView) {
                        brDialogView.dismissWithAnimation();
                    }
                }, null, 0);
                return;
            } else {
                certification = "\uD83D\uDD12 " + certName + "\n";
            }

        }

        continueWithThePayment(app, certification);

    }

    private String extractCNFromCertName(String str) {
        if (str == null || str.length() < 4) return null;
        String cn = "CN=";
        int index = -1;
        int endIndex = -1;
        for (int i = 0; i < str.length() - 3; i++) {
            if (str.substring(i, i + 3).equalsIgnoreCase(cn)) {
                index = i + 3;
            }
            if (index != -1) {
                if (str.charAt(i) == ',') {
                    endIndex = i;
                    break;
                }

            }
        }
        String cleanCN = str.substring(index, endIndex);
        return (index != -1 && endIndex != -1) ? cleanCN : null;
    }

    private void continueWithThePayment(final Activity app, final String certification) {

        BRCoreTransactionOutput[] outputs = paymentProtocolRequest.getOutputs();
        StringBuilder allAddresses = new StringBuilder();
        for (BRCoreTransactionOutput output : outputs) {
            allAddresses.append(output.getAddress()).append(", ");
        }
        final BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);
        BRCoreWallet coreWallet = wm.getIso().equalsIgnoreCase("BTC") ? ((WalletBitcoinManager) wm).getWallet() : ((WalletBchManager) wm).getWallet();
        final BRCoreTransaction trans = coreWallet.createTransactionForOutputs(paymentProtocolRequest.getOutputs());
        if (trans == null) {
            BRDialog.showSimpleDialog(app, "Insufficient funds", "");
            paymentProtocolRequest = null;
            return;
        }
        final CryptoTransaction tx = new CryptoTransaction(trans);
        final BigDecimal amount = wm.getTransactionAmount(tx).abs();
        final BigDecimal fee = wm.getTxFee(tx);

        allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
//        if (paymentProtocolRequest.getMemo() == null) paymentRequest.memo = "";
        final String memo = (!Utils.isNullOrEmpty(paymentProtocolRequest.getMemo()) ? "\n" : "") + paymentProtocolRequest.getMemo();
        allAddresses = new StringBuilder();

        final String iso = BRSharedPrefs.getPreferredFiatIso(app);
        final StringBuilder finalAllAddresses = allAddresses;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                BigDecimal txAmt = wm.getTransactionAmount(tx).abs();
                BigDecimal minOutput = wm.getMinOutputAmount(app);
                if (txAmt.compareTo(minOutput) < 0) {
                    final String bitcoinMinMessage = String.format(Locale.getDefault(), app.getString(R.string.PaymentProtocol_Errors_smallTransaction),
                            BRConstants.BITS_SYMBOL + minOutput.divide(new BigDecimal("100")));
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BRDialog.showCustomDialog(app, app.getString(R.string.PaymentProtocol_Errors_badPaymentRequest), bitcoinMinMessage, app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                                @Override
                                public void onClick(BRDialogView brDialogView) {
                                    brDialogView.dismissWithAnimation();
                                }
                            }, null, null, 0);
                        }
                    });

                    return;
                }
                WalletsMaster master = WalletsMaster.getInstance(app);

                final BigDecimal total = amount.add(fee);


                BigDecimal bigAm = master.getCurrentWallet(app).getFiatForSmallestCrypto(app, amount, null);
                BigDecimal bigFee = master.getCurrentWallet(app).getFiatForSmallestCrypto(app, fee, null);
                BigDecimal bigTotal = master.getCurrentWallet(app).getFiatForSmallestCrypto(app, total, null);
                final String message = certification + memo + finalAllAddresses.toString() + "\n\n" + "amount: " + CurrencyUtils.getFormattedAmount(app, iso, bigAm)
                        + "\nnetwork fee: +" + CurrencyUtils.getFormattedAmount(app, iso, bigFee)
                        + "\ntotal: " + CurrencyUtils.getFormattedAmount(app, iso, bigTotal);

                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        AuthManager.getInstance().authPrompt(app, "Confirmation", message, false, false, new BRAuthCompletion() {
                            @Override
                            public void onComplete() {
                                PostAuth.getInstance().setTmpPaymentRequestTx(tx);
                                PostAuth.getInstance().onPaymentProtocolRequest(app, false);
                            }

                            @Override
                            public void onCancel() {
                                Log.e(TAG, "onCancel: ");
                            }
                        });
                    }
                });
            }
        });

    }

}
