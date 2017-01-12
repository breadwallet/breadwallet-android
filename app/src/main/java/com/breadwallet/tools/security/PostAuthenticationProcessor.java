package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.DialogInterface;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.BreadWalletApp;
import com.breadwallet.R;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.activities.PhraseFlowActivity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.threads.PaymentProtocolPostPaymentTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.wallet.BRWalletManager;

import java.security.Key;
import java.util.Arrays;

import static com.breadwallet.wallet.BRWalletManager.getSeedFromPhrase;

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
            }

        }
    }

    public void onRecoverWalletAuth(IntroActivity app, boolean authAsked) {
        if (phraseForKeyStore == null) return;
        byte[] bytePhrase = new byte[0];

        try {
            boolean success = false;
            try {
                success = KeyStoreManager.putKeyStorePhrase(phraseForKeyStore.getBytes(),
                        app, BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE);
            } catch (BRKeystoreErrorException e) {
                e.printStackTrace();
            }

            if (!success) {
                if (authAsked) {
                    showBugAuthLoopErrorMessage(app);
                    Log.e(TAG, "onRecoverWalletAuth,!success && authAsked");
                }
            } else {
                if (phraseForKeyStore.length() != 0) {
                    SharedPreferencesManager.putPhraseWroteDown(app, true);
                    bytePhrase = TypesConverter.getNullTerminatedPhrase(phraseForKeyStore.getBytes());
                    byte[] seed = BRWalletManager.getSeedFromPhrase(bytePhrase);
                    byte[] authKey = BRWalletManager.getAuthPrivKeyForAPI(seed);
                    KeyStoreManager.putAuthKey(authKey, app);
                    byte[] pubKey = BRWalletManager.getInstance(app).getMasterPubKey(bytePhrase);
                    KeyStoreManager.putMasterPublicKey(pubKey, app);
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
        byte[] phrase;
        try {
            phrase = KeyStoreManager.getKeyStorePhrase(app, BRConstants.SHOW_PHRASE_REQUEST_CODE);
            app.showHideFragments(app.fragmentPhraseFlow1);
            app.fragmentPhraseFlow1.setPhrase(phrase);
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
            if (authAsked) {
                showBugAuthLoopErrorMessage(app);
                Log.e(TAG, "onShowPhraseFlowAuth,!success && authAsked");
            }
        }
    }

    public void onPublishTxAuth(MainActivity app, boolean authAsked) {

        BRWalletManager walletManager = BRWalletManager.getInstance(app);
        byte[] rawSeed;
        try {
            rawSeed = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAY_REQUEST_CODE);
        } catch (BRKeystoreErrorException e) {
            if (authAsked) {
                showBugAuthLoopErrorMessage(app);
                Log.e(TAG, "onPublishTxAuth,!success && authAsked");
            }
            e.printStackTrace();
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
            rawSeed = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE);
        } catch (BRKeystoreErrorException e) {
            if (authAsked) {
                showBugAuthLoopErrorMessage(app);
                Log.e(TAG, "onPublishTxAuth,!success && authAsked");
            }
            e.printStackTrace();
            return;
        }
        if (rawSeed == null || rawSeed.length < 10 || paymentRequest.serializedTx == null)
            return;
        if (rawSeed.length < 10) return;

        byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);

        BRWalletManager.getInstance(app).publishSerializedTransaction(paymentRequest.serializedTx, seed);
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


        String canary;
        try {
            canary = KeyStoreManager.getKeyStoreCanary(introActivity, BRConstants.CANARY_REQUEST_CODE);
        } catch (BRKeystoreErrorException e) {
            if (authAsked) {
                showBugAuthLoopErrorMessage(introActivity);
                Log.e(TAG, "onPublishTxAuth,!success && authAsked");
            }
            return;
        }

        if (!canary.equalsIgnoreCase(BRConstants.CANARY_STRING)) {
            Log.e(TAG, "!canary.equalsIgnoreCase(BRConstants.CANARY_STRING)");
            byte[] phrase;
            try {
                phrase = KeyStoreManager.getKeyStorePhrase(introActivity, BRConstants.CANARY_REQUEST_CODE);
            } catch (BRKeystoreErrorException e) {
                Log.e(TAG, "onCanaryCheck: error: " + e.getMessage());
                return;
            }
            String strPhrase = new String(phrase);
            if (strPhrase.isEmpty()) {
                BRWalletManager m = BRWalletManager.getInstance(introActivity);
                m.wipeKeyStore(introActivity);
                m.wipeWalletButKeystore(introActivity);
                BRAnimator.resetFragmentAnimator();
            } else {
                try {
                    KeyStoreManager.putKeyStoreCanary(BRConstants.CANARY_STRING, introActivity, 0);
                } catch (BRKeystoreErrorException e) {
                    e.printStackTrace();
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
            KeyStoreManager.showKeyStoreDialog("Keystore invalidated", "Disable lock screen and all fingerprints, and re-enable to continue.", app.getString(R.string.ok), null,
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
