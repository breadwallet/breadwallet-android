package com.breadwallet.tools.threads;

import android.content.Context;
import android.os.AsyncTask;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.BRCoreAddress;
import com.breadwallet.core.BRCorePaymentProtocolRequest;
import com.breadwallet.core.BRCoreTransaction;
import com.breadwallet.core.BRCoreTransactionOutput;
import com.breadwallet.core.BRCoreWallet;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.protocols.paymentrequest.Protos;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.security.X509CertificateValidator;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.CustomLogger;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.CryptoTransaction;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.google.protobuf.ByteString;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.math.BigDecimal;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import okhttp3.Request;

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
    private static final String ACCEPT_HEADER_FORMAT = "application/%s-paymentrequest";
    private static final String ACCEPT_HEADER_BITPAY = "application/payment-request";
    private static final String PAYMENT_MESSAGE_FORMAT = "%s\n%s\n%s\n\namount: %s\nnetwork fee: %s\ntotal: %s";
    private static final String DATE_FORMAT = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX";
    private static final String NETWORK = "network";
    private static final String CURRENCY = "currency";
    private static final String REQUIRED_FEE_RATE = "requiredFeeRate";
    private static final String OUTPUTS = "outputs";
    private static final String AMOUNT = "amount";
    private static final String ADDRESS = "address";
    private static final String TIME = "time";
    private static final String EXPIRES = "expires";
    private static final String MEMO = "memo";
    private static final String PAYMENT_URL = "paymentUrl";
    private static final String PAYMENT_ID = "paymentId";
    private static final int KB_IN_B = 1000;
    private static final int ONE_HUNDRED = 100;
    private static final String UNTRUSTED_LOCK_SYMBOL = "\u274C";
    private static final String TRUSTED_LOCK_SYMBOL = "\uD83D\uDD12";
    private static final String NONE = "none";
    private static final String COMMON_NAME_ATTRIBUTE_KEY = "CN=";
    private Context mContext;
    private String mCommonName;
    private BRCorePaymentProtocolRequest mPaymentProtocolRequest;
    private PaymentRequestExtra mPaymentRequestExtra;

    public PaymentProtocolTask(Context context) {
        mContext = context;
    }

    //params[0] = uri, params[1] = label
    @Override
    protected String doInBackground(String... params) {
        String distinguishedName = null;
        try {
            URL url = new URL(params[0]);
            BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(mContext);

            String acceptHeaderValue = walletManager.getCurrencyCode().equalsIgnoreCase(WalletBitcoinManager.BITCOIN_CURRENCY_CODE)
                    ? String.format(ACCEPT_HEADER_FORMAT, walletManager.getName().toLowerCase())
                    : ACCEPT_HEADER_BITPAY;
            Request request = new Request.Builder().url(url).get().addHeader(BRConstants.HEADER_ACCEPT, acceptHeaderValue).build();
            APIClient.BRResponse response = APIClient.getInstance(mContext).sendRequest(request, false);

            if (!response.isSuccessful()) {
                Log.e(TAG, "doInBackground: The response is empty");
                BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
                    BRDialog.showSimpleDialog(mContext, mContext.getString(R.string.Send_remoteRequestError),
                            String.format(Locale.getDefault(), "(%d) %s", response.getCode(), response.getBodyText()));
                });
                return null;
            }
            byte[] paymentData = null;
            JSONObject jsonPaymentData = null;
            try {
                String data = response.getBodyText();
                jsonPaymentData = new JSONObject(data);
                //did not throw, meaning there is a valid Json data and needs to be parsed into a protobuf payment protocol
            } catch (JSONException ex) {
                //no valid json format, assume it's protobuf data.
                paymentData = response.getBody();
            }
            if (paymentData == null) {
                try {
                    paymentData = buildPaymentData(walletManager, jsonPaymentData);
                } catch (final JSONException e) {
                    Log.e(TAG, "doInBackground: ", e);
                    BRReportsManager.reportBug(e);
                    BRExecutor.getInstance().forMainThreadTasks().execute((() -> BRDialog.showSimpleDialog(mContext, mContext.getString(R.string.Send_remoteRequestError), "")));
                    return null;
                }
            }

            mPaymentProtocolRequest = new BRCorePaymentProtocolRequest(paymentData);

            BRCoreTransactionOutput[] outputs = mPaymentProtocolRequest.getOutputs();
            StringBuilder allAddresses = new StringBuilder();
            for (BRCoreTransactionOutput output : outputs) {
                allAddresses.append(output.getAddress()).append(", ");
                if (Utils.isNullOrEmpty(output.getAddress()) || !new BRCoreAddress(output.getAddress()).isValid()) {
                    if (mContext != null) {
                        showErrorDialog(mContext.getString(R.string.Alert_error),
                                mContext.getString(R.string.Send_invalidAddressTitle) + ": " + output.getAddress());
                    }
                    mPaymentProtocolRequest = null;
                    return null;
                }
            }

            allAddresses.delete(allAddresses.length() - 2, allAddresses.length());

            long totalAmount = 0;
            for (BRCoreTransactionOutput output : outputs) {
                totalAmount += output.getAmount();
            }

            logPaymentData(allAddresses, totalAmount);
            if (mPaymentProtocolRequest.getExpires() != 0 && mPaymentProtocolRequest.getTime() > mPaymentProtocolRequest.getExpires()) {
                Log.e(TAG, "Request is expired");
                if (mContext != null) {
                    showErrorDialog(mContext.getString(R.string.Alert_error), mContext.getString(R.string.PaymentProtocol_Errors_requestExpired));
                }
                mPaymentProtocolRequest = null;
                return null;
            }
            List<X509Certificate> certificateChain = X509CertificateValidator.validateCertificateChain(response.getBody());
            if (certificateChain != null && certificateChain.size() > 0) {
                distinguishedName = X509CertificateValidator.certificateValidation(certificateChain, mPaymentProtocolRequest);
            }
        } catch (Exception e) {
            Log.e(TAG, "doInBackground: ", e);
            BRReportsManager.reportBug(e);
            if (mContext != null) {
                showErrorDialog(mContext.getString(R.string.Alert_error),
                        mContext.getString(R.string.PaymentProtocol_Errors_badPaymentRequest) + ":" + e.getMessage());
            }
        }

        if (mPaymentProtocolRequest == null) {
            return null;
        }
        mCommonName = getAttributeFromDistinguishedName(distinguishedName, COMMON_NAME_ATTRIBUTE_KEY);
        if (mCommonName == null || mCommonName.isEmpty()) {
            mCommonName = params[1];
        }
        if (mCommonName == null || mCommonName.isEmpty()) {
            mCommonName = mPaymentProtocolRequest.getOutputs()[0].getAddress();
        }
        return null;
    }

    /**
     * Build paymentData by creating a protobuf with the json object.
     *
     * @param walletManager Manager from the wallet we are using to pay.
     * @param jsonPaymentData    Object with the payment data.
     * @return The payment data as an array of bytes.
     * @throws JSONException
     * @throws ParseException
     */
    private byte[] buildPaymentData(BaseWalletManager walletManager, JSONObject jsonPaymentData) throws JSONException, ParseException {
        byte[] paymentData;
        String network = jsonPaymentData.has(NETWORK) ? jsonPaymentData.getString(NETWORK) : null;
        String currency = jsonPaymentData.has(CURRENCY) ? jsonPaymentData.getString(CURRENCY) : null;
        WalletsMaster walletsMaster = WalletsMaster.getInstance();
        if (walletsMaster.hasWallet(currency)) {
            walletManager = walletsMaster.getWalletByIso(mContext, currency);
            if (walletManager != null) {
                BRSharedPrefs.putCurrentWalletCurrencyCode(mContext, currency);
            }
        }
        String requiredFeeRate = jsonPaymentData.has(REQUIRED_FEE_RATE) ? jsonPaymentData.getString(REQUIRED_FEE_RATE) : null;
        String paymentId = jsonPaymentData.has(PAYMENT_ID) ? jsonPaymentData.getString(PAYMENT_ID) : null;
        mPaymentRequestExtra = new PaymentRequestExtra(requiredFeeRate, currency, paymentId);
        List<Protos.Output> outputs = new ArrayList<>();
        if (jsonPaymentData.has(OUTPUTS)) {
            JSONArray jsonOutputs = jsonPaymentData.getJSONArray(OUTPUTS);
            for (int i = 0; i < jsonOutputs.length(); i++) {
                JSONObject jsonOutput = jsonOutputs.getJSONObject(i);
                long amount = jsonOutput.getLong(AMOUNT);
                String legacyAddress = walletManager.undecorateAddress(jsonOutput.getString(ADDRESS));
                ByteString script = ByteString.copyFrom(new BRCoreAddress(legacyAddress).getPubKeyScript());
                outputs.add(Protos.Output.newBuilder().setAmount(amount).setScript(script).build());
            }
        }
        DateFormat dateFormat = new SimpleDateFormat(DATE_FORMAT, Locale.getDefault());
        long time = jsonPaymentData.has(TIME) ? dateFormat.parse(jsonPaymentData.getString(TIME)).getTime() : 0;
        long expires = jsonPaymentData.has(EXPIRES) ? dateFormat.parse(jsonPaymentData.getString(EXPIRES)).getTime() : 0;

        String memo = jsonPaymentData.has(MEMO) ? jsonPaymentData.getString(MEMO) : null;
        String paymentUrl = jsonPaymentData.has(PAYMENT_URL) ? jsonPaymentData.getString(PAYMENT_URL) : null;

        Protos.PaymentDetails paymentDetails = Protos.PaymentDetails.newBuilder()
                .setNetwork(network)
                .setTime(time)
                .setExpires(expires)
                .setMemo(memo)
                .setPaymentUrl(paymentUrl)
                .addAllOutputs(outputs).build();

        Protos.PaymentRequest paymentRequestProto = Protos.PaymentRequest.getDefaultInstance().toBuilder()
                .setSerializedPaymentDetails(paymentDetails.toByteString())
                .build();
        paymentData = paymentRequestProto.toByteArray();
        return paymentData;
    }

    private void logPaymentData(StringBuilder allAddresses, long totalAmount) {
        CustomLogger.logThis("Signature", String.valueOf(mPaymentProtocolRequest.getSignature().length),
                "pkiType", mPaymentProtocolRequest.getPKIType(), "pkiData", String.valueOf(mPaymentProtocolRequest.getPKIData().length));
        CustomLogger.logThis("network", mPaymentProtocolRequest.getNetwork(), "time", String.valueOf(mPaymentProtocolRequest.getTime()),
                "expires", String.valueOf(mPaymentProtocolRequest.getExpires()), "memo", mPaymentProtocolRequest.getMemo(),
                "paymentURL", mPaymentProtocolRequest.getPaymentURL(), "merchantDataSize",
                String.valueOf(mPaymentProtocolRequest.getMerchantData().length), "address", allAddresses.toString(),
                "amount", String.valueOf(totalAmount));
    }

    private void showErrorDialog(String title, String message) {
        BRDialog.showCustomDialog(mContext, title, message,
                mContext.getString(R.string.Button_ok), null, BRDialogView::dismissWithAnimation, null, null, 0);
    }

    @Override
    protected void onPostExecute(String result) {
        if (mContext != null && mPaymentProtocolRequest != null && mPaymentProtocolRequest.getOutputs() != null
                && mPaymentProtocolRequest.getOutputs()[0].getAddress().length() != 0) {

            if (mPaymentProtocolRequest.getPKIType().equals(NONE) || Utils.isNullOrEmpty(mCommonName)) {
                mCommonName = String.format("%s %s", UNTRUSTED_LOCK_SYMBOL, mCommonName);
                BRDialog.showCustomDialog(mContext, mContext.getString(R.string.PaymentProtocol_Errors_untrustedCertificate),
                        "", mContext.getString(R.string.JailbreakWarnings_ignore),
                        mContext.getString(R.string.Button_cancel), brDialogView -> continueWithThePayment(), BRDialogView::dismissWithAnimation, null, 0);
            } else {
                mCommonName = String.format("%s %s", TRUSTED_LOCK_SYMBOL, mCommonName);
                continueWithThePayment();
            }
        }
    }

    private String getAttributeFromDistinguishedName(String distinguishedName, String attributeKey) {
        if (distinguishedName != null) {
            String[] distinguishedNameAttributes = distinguishedName.split(",");
            for (String distinguishedNameAttribute : distinguishedNameAttributes) {
                if (distinguishedNameAttribute.contains(attributeKey)) {
                    return distinguishedNameAttribute.split(attributeKey)[0];
                }
            }
        }
        return null;
    }


    private void continueWithThePayment() {
        BRCoreTransactionOutput[] outputs = mPaymentProtocolRequest.getOutputs();
        StringBuilder allAddresses = new StringBuilder();
        for (BRCoreTransactionOutput output : outputs) {
            allAddresses.append(output.getAddress()).append(", ");
        }
        final BaseWalletManager walletManager = WalletsMaster.getInstance().getCurrentWallet(mContext);
        BRCoreWallet coreWallet = walletManager.getCurrencyCode().equalsIgnoreCase(WalletBitcoinManager.BITCOIN_CURRENCY_CODE)
                ? ((WalletBitcoinManager) walletManager).getWallet()
                : ((WalletBchManager) walletManager).getWallet();
        final BRCoreTransaction transactionForOutputs = coreWallet.createTransactionForOutputs(mPaymentProtocolRequest.getOutputs());
        if (transactionForOutputs == null) {
            BRDialog.showSimpleDialog(mContext, mContext.getString(R.string.Send_insufficientFunds), "");
            mPaymentProtocolRequest = null;
            return;
        }
        long originalFeePerKb = coreWallet.getFeePerKb();
        if (mPaymentRequestExtra != null) {
            long customFeePerKb = Long.valueOf(mPaymentRequestExtra.mRequiredFeeRate) * KB_IN_B;
            if (originalFeePerKb < customFeePerKb) {
                //set the required custom fee rate.
                coreWallet.setFeePerKb(customFeePerKb);
            }
        }
        final CryptoTransaction transaction = new CryptoTransaction(transactionForOutputs);
        //put the original fee rate back.
        coreWallet.setFeePerKb(originalFeePerKb);

        final BigDecimal amount = walletManager.getTransactionAmount(transaction).abs();
        final BigDecimal fee = walletManager.getTxFee(transaction);

        allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
        final String memo = (!Utils.isNullOrEmpty(mPaymentProtocolRequest.getMemo()) ? "\n" : "") + mPaymentProtocolRequest.getMemo();
        allAddresses = new StringBuilder();

        final String iso = BRSharedPrefs.getPreferredFiatIso(mContext);
        final StringBuilder finalAllAddresses = allAddresses;
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
            BigDecimal txAmt = walletManager.getTransactionAmount(transaction).abs();
            BigDecimal minOutput = walletManager.getMinOutputAmount(mContext);
            if (txAmt.compareTo(minOutput) < 0) {
                final String bitcoinMinMessage = String.format(Locale.getDefault(), mContext.getString(R.string.PaymentProtocol_Errors_smallTransaction),
                        BRConstants.BITS_SYMBOL + minOutput.divide(new BigDecimal(ONE_HUNDRED), BRConstants.ROUNDING_MODE));
                BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
                    BRDialog.showCustomDialog(mContext, mContext.getString(R.string.PaymentProtocol_Errors_badPaymentRequest),
                            bitcoinMinMessage, mContext.getString(R.string.Button_ok), null, BRDialogView::dismissWithAnimation, null, null, 0);
                });
                return;
            }
            WalletsMaster master = WalletsMaster.getInstance();

            final BigDecimal total = amount.add(fee);

            BigDecimal bigAm = master.getCurrentWallet(mContext).getFiatForSmallestCrypto(mContext, amount, null);
            BigDecimal bigFee = master.getCurrentWallet(mContext).getFiatForSmallestCrypto(mContext, fee, null);
            BigDecimal bigTotal = master.getCurrentWallet(mContext).getFiatForSmallestCrypto(mContext, total, null);
            final String message = String.format(PAYMENT_MESSAGE_FORMAT, mCommonName,
                    memo, finalAllAddresses.toString(), CurrencyUtils.getFormattedAmount(mContext, iso, bigAm),
                    CurrencyUtils.getFormattedAmount(mContext, iso, bigFee),
                    CurrencyUtils.getFormattedAmount(mContext, iso, bigTotal));

            BRExecutor.getInstance().forMainThreadTasks().execute(() -> {
                AuthManager.getInstance().authPrompt(mContext, mContext.getString(R.string.Confirmation_title),
                        message, false, false, new BRAuthCompletion() {
                            @Override
                            public void onComplete() {
                                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(() -> {
                                    PostAuth.getInstance().onPaymentProtocolRequest(mContext, false, transaction);
                                });
                            }

                            @Override
                            public void onCancel() {
                                Log.e(TAG, "onCancel: ");
                            }
                        });
            });
        });

    }

    private static class PaymentRequestExtra {
        private String mRequiredFeeRate;
        private String mCurrency;
        private String mPaymentId;

        PaymentRequestExtra(String requiredFeeRate, String currency, String paymentId) {
            mRequiredFeeRate = requiredFeeRate;
            mCurrency = currency;
            mPaymentId = paymentId;
        }


        public String getRequiredFeeRate() {
            return mRequiredFeeRate;
        }

        public String getCurrency() {
            return mCurrency;
        }

        public String getPaymentId() {
            return mPaymentId;
        }
    }

}