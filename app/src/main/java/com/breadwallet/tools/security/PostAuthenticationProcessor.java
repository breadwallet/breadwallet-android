package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.DialogInterface;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.PhraseFlowActivity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.fragments.FragmentWithdrawBch;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.threads.PaymentProtocolPostPaymentTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.APIClient;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Arrays;

import static com.platform.APIClient.BASE_URL;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

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

public class PostAuthenticationProcessor {
    public static final String TAG = PostAuthenticationProcessor.class.getName();

    private String phraseForKeyStore;
    private byte[] tmpTx;
    private PaymentRequestWrapper paymentRequest;
    private String uri;
    private String label;
    private String bchAddress;

    private static PostAuthenticationProcessor instance;

    private PostAuthenticationProcessor() {
    }

    public static PostAuthenticationProcessor getInstance() {
        if (instance == null) {
            instance = new PostAuthenticationProcessor();
        }
        return instance;
    }

    public void onCreateWalletAuth(IntroActivity app, boolean authAsked) {
        boolean success = BRWalletManager.getInstance(app).generateRandomSeed();
        if (success) {
            app.showWarningFragment();
        } else {
            if (authAsked) {
                showBugAuthLoopErrorMessage(app);
                Log.e(TAG, "onCreateWalletAuth,!success && authAsked");
            } else {
                Log.e(TAG, "onCreateWalletAuth: !success, maybe keyStore needs auth");
            }

        }
    }

    public void onRecoverWalletAuth(IntroActivity app, boolean authAsked) {
        if (phraseForKeyStore == null) return;
        byte[] bytePhrase = new byte[0];

        try {
            boolean success = false;
            try {
                success = KeyStoreManager.putPhrase(phraseForKeyStore.getBytes(),
                        app, BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE);
            } catch (UserNotAuthenticatedException e) {
                return;
            }

            if (!success) {
                BRKeystoreErrorException ex = new BRKeystoreErrorException("onRecoverWalletAuth putPhrase failed");
                BRErrorPipe.parseError(app, "onPaymentProtocolRequest: rawSeed", ex, true);
                return;
            } else {
                if (phraseForKeyStore.length() != 0) {
                    SharedPreferencesManager.putPhraseWroteDown(app, true);
                    bytePhrase = TypesConverter.getNullTerminatedPhrase(phraseForKeyStore.getBytes());
                    byte[] seed = BRWalletManager.getSeedFromPhrase(bytePhrase);
                    if (Utils.isNullOrEmpty(seed)) {
                        RuntimeException ex = new RuntimeException("seed is malformed:" + (seed == null ? null : seed.length));
                        BRErrorPipe.parseError(app, "error 001", ex, true);
                        return;
                    }
                    byte[] authKey = BRWalletManager.getAuthPrivKeyForAPI(seed);
                    if (Utils.isNullOrEmpty(authKey)) {
                        RuntimeException ex = new RuntimeException("authKey is malformed:" + (authKey == null ? null : authKey.length));
                        BRErrorPipe.parseError(app, "error 002", ex, true);
                        return;
                    }
                    boolean putAuthB = KeyStoreManager.putAuthKey(authKey, app);
                    if (!putAuthB) {
                        RuntimeException ex = new RuntimeException("failed to put authKey");
                        BRErrorPipe.parseError(app, "error 003", ex, true);
                        return;
                    }
                    byte[] pubKey = BRWalletManager.getInstance(app).getMasterPubKey(bytePhrase);
                    if (Utils.isNullOrEmpty(pubKey)) {
                        RuntimeException ex = new RuntimeException("pubkey is malformed: " + Arrays.toString(pubKey));
                        BRErrorPipe.parseError(app, "error 004", ex, true);
                        return;
                    }
                    boolean b = KeyStoreManager.putMasterPublicKey(pubKey, app);
                    if (!b) {
                        RuntimeException ex = new RuntimeException("failed to put master pub key");
                        BRErrorPipe.parseError(app, "error 005", ex, true);
                        return;
                    }
                    app.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    app.startMainActivity();
                    if (!app.isDestroyed()) app.finish();
                    phraseForKeyStore = null;
                }

            }

        } finally {
            Arrays.fill(bytePhrase, (byte) 0);
        }


    }

    public void onShowPhraseFlowAuth(PhraseFlowActivity app, boolean authAsked) {
        byte[] phrase = null;
        try {
            phrase = KeyStoreManager.getPhrase(app, BRConstants.SHOW_PHRASE_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "onShowPhraseFlowAuth: getPhrase with no auth");
            return;
        }
        if (Utils.isNullOrEmpty(phrase)) {
            RuntimeException ex = new RuntimeException("phrase is malformed: " + (phrase == null ? null : phrase.length));
            BRErrorPipe.parseError(app, "error 006", ex, true);
            return;
        }
        app.showHideFragments(app.fragmentPhraseFlow1);
        app.fragmentPhraseFlow1.setPhrase(phrase);
    }

    public void onSendBch(final Activity app, boolean authAsked, String bchAddress) {
        this.bchAddress = bchAddress;
        byte[] phrase = null;
        try {
            phrase = KeyStoreManager.getPhrase(app, BRConstants.SEND_BCH_REQUEST);
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "onShowPhraseFlowAuth: getPhrase with no auth");
            return;
        }
        if (Utils.isNullOrEmpty(phrase)) {
            RuntimeException ex = new RuntimeException("phrase is malformed: " + (phrase == null ? null : phrase.length));
            BRErrorPipe.parseError(app, "error 006", ex, true);
            return;
        }

        byte[] nullTerminatedPhrase = TypesConverter.getNullTerminatedPhrase(phrase);
        final byte[] serializedTx = BRWalletManager.sweepBCash(KeyStoreManager.getMasterPublicKey(app), bchAddress, nullTerminatedPhrase);
        assert (serializedTx != null);
        if (serializedTx == null) {
            Log.e(TAG, "onSendBch:serializedTx is null");
            BRErrorPipe.showKeyStoreDialog(app, "No balance", "You have 0 BCH", "close", null,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }, null, null);
        } else {
            Log.e(TAG, "onSendBch:serializedTx is:" + serializedTx.length);
            new Thread(new Runnable() {
                @Override
                public void run() {
                    String title = "Failed";
                    String message = "";
                    String strUtl = BASE_URL + "/bch/publish-transaction";
                    Log.e(TAG, "url: " + strUtl);
                    final MediaType type
                            = MediaType.parse("application/bchdata");
                    RequestBody requestBody = RequestBody.create(type, serializedTx);
                    Request request = new Request.Builder()
                            .url(strUtl)
                            .header("Content-Type", "application/bchdata")
                            .post(requestBody).build();
                    Response response = APIClient.getInstance(app).sendRequest(request, true, 0);
                    String responseBody = null;
                    try {
                        responseBody = response == null ? null : response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    Log.e(TAG, "onSendBch:" + (response == null ? "resp is null" : response.code() + ":" + response.message()));
                    boolean success = true;
                    if (response != null) {
                        title = "Failed";
                        if (response.isSuccessful()) {
                            title = "Success";
                            message = "";
                        } else if (response.code() == 503) {
                            message = "Your BCH has already been sent, or your wallet did not contain BCH before the fork.";
                        } else {
                            success = false;
                            message = "(" + response.code() + ")" + "[" + response.message() + "]" + responseBody;
                        }
                    } else {
                        title = "Failed to send";
                        message = "Something went wrong";
                    }
                    if (!success) {
                        SharedPreferencesManager.putBCHTxId(app, "");
                        FragmentWithdrawBch.updateUi();
                    }

                    final String finalTitle = title;
                    final String finalMessage = message;
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            BRErrorPipe.showKeyStoreDialog(app, finalTitle, finalMessage, "close", null,
                                    new DialogInterface.OnClickListener() {
                                        public void onClick(DialogInterface dialog, int which) {
                                            dialog.cancel();
                                        }
                                    }, null, null);
                        }
                    });

                }
            }).start();

        }


    }

    public void onPublishTxAuth(MainActivity app, boolean authAsked) {

        BRWalletManager walletManager = BRWalletManager.getInstance(app);
        byte[] rawSeed;
        try {
            rawSeed = KeyStoreManager.getPhrase(app, BRConstants.PAY_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "onPublishTxAuth: getPhrase with no auth");
            return;
        }
        if (rawSeed.length < 10) return;
        byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);
        try {
            if (seed.length != 0) {
                boolean success = false;
                if (tmpTx != null) {
                    success = walletManager.publishSerializedTransaction(tmpTx, seed);
                    tmpTx = null;
                }
                if (!success) {
                    BRWalletManager.getInstance(app).offerToChangeTheAmount(app, app.getString(R.string.insufficient_funds));
                    return;
                }
            } else {
                return;
            }
            BRAnimator.hideScanResultFragment();
        } finally {
            Arrays.fill(seed, (byte) 0);
        }
    }

    public void onPaymentProtocolRequest(MainActivity app, boolean authAsked) {

        byte[] rawSeed;
        try {
            rawSeed = KeyStoreManager.getPhrase(app, BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "onPaymentProtocolRequest: getPhrase with no auth");
            return;
        }
        if (Utils.isNullOrEmpty(rawSeed) || paymentRequest.serializedTx == null) {
            BRKeystoreErrorException ex = new BRKeystoreErrorException("onPaymentProtocolRequest phrase length:" + (rawSeed == null) + ", tx:" + (paymentRequest.serializedTx == null));
            BRErrorPipe.parseError(app, "onPaymentProtocolRequest: rawSeed", ex, true);
            return;
        }

        byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);

        boolean success = BRWalletManager.getInstance(app).publishSerializedTransaction(paymentRequest.serializedTx, seed);
        if (!success) {
            BRKeystoreErrorException ex = new BRKeystoreErrorException("publishSerializedTransaction:" + rawSeed.length);
            BRErrorPipe.parseError(app, "onPaymentProtocolRequest :publish", ex, true);
            return;
        }
        PaymentProtocolPostPaymentTask.sent = true;
        Arrays.fill(seed, (byte) 0);
        paymentRequest = null;

    }

    public void setPhraseForKeyStore(String phraseForKeyStore) {
        this.phraseForKeyStore = phraseForKeyStore;
    }

    public void setTmpTx(byte[] tmpTx) {
        this.tmpTx = tmpTx;
    }

    public void setUriAndLabel(String uri, String label) {
        this.uri = uri;
        this.label = label;
    }

    public void setTmpPaymentRequest(PaymentRequestWrapper paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public void onCanaryCheck(final IntroActivity introActivity, boolean authAsked) {
        String canary = null;
        try {
            canary = KeyStoreManager.getCanary(introActivity, BRConstants.CANARY_REQUEST_CODE);
        } catch (UserNotAuthenticatedException e) {
            Log.e(TAG, "onCanaryCheck: getCanary with no auth");
            return;
        }

        if (canary == null || !canary.equalsIgnoreCase(BRConstants.CANARY_STRING)) {
            Log.e(TAG, "!canary.equalsIgnoreCase(BRConstants.CANARY_STRING)");
            byte[] phrase;
            try {
                phrase = KeyStoreManager.getPhrase(introActivity, BRConstants.CANARY_REQUEST_CODE);
            } catch (UserNotAuthenticatedException e) {
                Log.e(TAG, "onCanaryCheck: getPhrase with no auth");
                return;
            }
            if (Utils.isNullOrEmpty(phrase)) phrase = new byte[0];
            String strPhrase = new String(phrase);
            if (strPhrase.isEmpty()) {
                Log.e(TAG, "onCanaryCheck: strPhrase is empty, Clearing the wallet!");
                BRWalletManager m = BRWalletManager.getInstance(introActivity);
                m.wipeKeyStore(introActivity);
                m.wipeWalletButKeystore(introActivity);
                BRAnimator.resetFragmentAnimator();
            } else {
                boolean success;
                try {
                    success = KeyStoreManager.putCanary(BRConstants.CANARY_STRING, introActivity, 0);
                } catch (UserNotAuthenticatedException e) {
                    return;
                }
                if (!success) {
                    BRKeystoreErrorException ex = new BRKeystoreErrorException("onCanaryCheck: failed to put canary");
                    BRErrorPipe.parseError(introActivity, ex.getMessage(), ex, true);
                }
            }
        }
        introActivity.startTheWalletIfExists();

    }

    private void showBugAuthLoopErrorMessage(final Activity app) {
        if (app != null) {
            BRWalletManager m = BRWalletManager.getInstance(app);
            m.wipeKeyStore(app);
            m.wipeWalletButKeystore(app);
            BRAnimator.resetFragmentAnimator();
            BRErrorPipe.showKeyStoreDialog(app, "Keystore invalidated", "Disable lock screen and all fingerprints, and re-enable to continue.", app.getString(R.string.ok), null,
                    new DialogInterface.OnClickListener() {
                        public void onClick(DialogInterface dialog, int which) {
                            dialog.cancel();
                        }
                    }, null, new DialogInterface.OnDismissListener() {
                        @Override
                        public void onDismiss(DialogInterface dialogInterface) {
                            app.finish();
                        }
                    });
        }
    }
}
