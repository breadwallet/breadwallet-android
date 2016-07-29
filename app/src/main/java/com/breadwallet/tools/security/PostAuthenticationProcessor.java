package com.breadwallet.tools.security;

import android.app.Activity;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.fragments.FragmentRecoveryPhrase;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.IntroWelcomeFragment;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.threads.PaymentProtocolPostPaymentTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.threads.PaymentProtocolTask;
import com.breadwallet.wallet.BRWalletManager;

import java.lang.reflect.Array;
import java.util.Arrays;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 4/14/16.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
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

    private static PostAuthenticationProcessor instance;

    private PostAuthenticationProcessor() {
    }

    public static PostAuthenticationProcessor getInstance() {
        if (instance == null) {
            instance = new PostAuthenticationProcessor();
        }
        return instance;
    }

    public void onCreateWalletAuth(IntroActivity app) {
        Log.e(TAG, "onCreateWalletAuth");
        boolean success = BRWalletManager.getInstance(app).generateRandomSeed();
        if (success) {
            app.showWarningFragment();
        }
    }

    public void onRecoverWalletAuth(IntroActivity app) {
        Log.e(TAG, "onRecoverWalletAuth");
        if (phraseForKeyStore == null) return;

        try {
            boolean success = KeyStoreManager.putKeyStorePhrase(phraseForKeyStore.getBytes(), app, BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE);
            if (!success)
                return;
            if (phraseForKeyStore.length() != 0) {
                byte[] pubKey = BRWalletManager.getInstance(app).getMasterPubKey(phraseForKeyStore.getBytes());
                KeyStoreManager.putMasterPublicKey(pubKey, app);
                app.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                app.startMainActivity();
                if (!app.isDestroyed()) app.finish();
                phraseForKeyStore = null;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onShowPhraseAuth(MainActivity app) {
        Log.e(TAG, "onShowPhraseAuth");
        byte[] phrase;
        try {
            phrase = KeyStoreManager.getKeyStorePhrase(app, BRConstants.SHOW_PHRASE_REQUEST_CODE);
            Log.e(TAG, "phrase.length: " + phrase.length);
            if (phrase.length < 10) return;
            FragmentRecoveryPhrase.phrase = phrase;
            ((BreadWalletApp) app.getApplicationContext()).promptForAuthentication(app, BRConstants.AUTH_FOR_PHRASE, null, null, null, null);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPublishTxAuth(MainActivity app) {
        BRWalletManager walletManager = BRWalletManager.getInstance(app);
        byte[] seed = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAY_REQUEST_CODE);
        try {
            if (seed.length != 0) {
                boolean success = false;
                if (tmpTx != null) {
                    success = walletManager.publishSerializedTransaction(tmpTx, seed);
                    tmpTx = null;
                }
                if (!success) {
                    ((BreadWalletApp) app.getApplication()).showCustomToast(app,
                            app.getString(R.string.failed_to_send), MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
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

    public void onPaymentProtocolRequest(MainActivity app) {
        byte[] phrase = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE);
        if (phrase == null || phrase.length < 10 || paymentRequest.serializedTx == null)
            return;
        BRWalletManager.getInstance(app).publishSerializedTransaction(paymentRequest.serializedTx, phrase);
        PaymentProtocolPostPaymentTask.sent = true;
        Arrays.fill(phrase, (byte) 0);
        paymentRequest = null;

    }

    public void setPhraseForKeyStore(String phraseForKeyStore) {
        Log.e(TAG, "setPhraseForKeyStore");
        this.phraseForKeyStore = phraseForKeyStore;
    }

    public void setTmpTx(byte[] tmpTx) {
        Log.e(TAG, "tmpTx: " + Arrays.toString(tmpTx));
        this.tmpTx = tmpTx;
    }

    public void setUriAndLabel(String uri, String label) {
        this.uri = uri;
        this.label = label;
    }

    public void setTmpPaymentRequest(PaymentRequestWrapper paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public void onCanaryCheck(IntroActivity introActivity) {
        String canary = KeyStoreManager.getKeyStoreCanary(introActivity, BRConstants.CANARY_REQUEST_CODE);
        if (canary.equalsIgnoreCase(KeyStoreManager.NO_AUTH)) return;
        if (!canary.equalsIgnoreCase(BRConstants.CANARY_STRING)) {
            Log.e(TAG, "!canary.equalsIgnoreCase(BRConstants.CANARY_STRING)");
            byte[] phrase = KeyStoreManager.getKeyStorePhrase(introActivity, BRConstants.CANARY_REQUEST_CODE);
            String strPhrase = new String(phrase);
            if (strPhrase.equalsIgnoreCase(KeyStoreManager.NO_AUTH)) return;
            if (strPhrase.isEmpty()) {
                Log.e(TAG, "phrase == null || phrase.isEmpty() : " + strPhrase);
                BRWalletManager m = BRWalletManager.getInstance(introActivity);
                m.wipeKeyStore();
                m.wipeWalletButKeystore(introActivity);
                BRAnimator.resetFragmentAnimator();
            } else {
                Log.e(TAG, "phrase != null : " + strPhrase);
                KeyStoreManager.putKeyStoreCanary(BRConstants.CANARY_STRING, introActivity, 0);
            }
        }
        introActivity.startTheWalletIfExists();

    }
}
