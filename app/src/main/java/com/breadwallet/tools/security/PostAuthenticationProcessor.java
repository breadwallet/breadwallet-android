package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.presenter.activities.PaperKeyActivity;
import com.breadwallet.presenter.activities.PaperKeyProveActivity;
import com.breadwallet.presenter.activities.intro.WriteDownActivity;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.threads.PaymentProtocolPostPaymentTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.wallet.BRWalletManager;

import java.util.Arrays;

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

    public void onCreateWalletAuth(Activity app, boolean authAsked) {
        Log.e(TAG, "onCreateWalletAuth: " + authAsked + ", " + app.getClass().getName());
        long start = System.currentTimeMillis();
        boolean success = BRWalletManager.getInstance().generateRandomSeed(app);
        Log.e(TAG, "generateRandomSeed: took: " + (System.currentTimeMillis() - start));
        if (success) {
            Intent intent = new Intent(app, WriteDownActivity.class);
            app.startActivity(intent);
            app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);

        } else {
            if (authAsked) {
                showBugAuthLoopErrorMessage(app);
                Log.e(TAG, "onCreateWalletAuth,!success && authAsked");
            }
            Log.e(TAG, "onCreateWalletAuth: Failed to generateSeed");

        }
    }

    public void onPhraseCheckAuth(Activity app, boolean authAsked) {
        String cleanPhrase;
        try {
            cleanPhrase = new String(KeyStoreManager.getKeyStorePhrase(app, BRConstants.SHOW_PHRASE_REQUEST_CODE));
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
            return;
        }
        Intent intent = new Intent(app, PaperKeyActivity.class);
        intent.putExtra("phrase", cleanPhrase);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    public void onPhraseProveAuth(Activity app, boolean authAsked) {
        String cleanPhrase;
        try {
            cleanPhrase = new String(KeyStoreManager.getKeyStorePhrase(app, BRConstants.PROVE_PHRASE_REQUEST));
        } catch (BRKeystoreErrorException e) {
            e.printStackTrace();
            return;
        }
        Intent intent = new Intent(app, PaperKeyProveActivity.class);
        intent.putExtra("phrase", cleanPhrase);
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
    }

    public void onBitIDAuth(Activity app){
        BitcoinUrlHandler.processBitIdResponse(app);
    }


    public void onRecoverWalletAuth(Activity app, boolean authAsked) {
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
                    byte[] pubKey = BRWalletManager.getInstance().getMasterPubKey(bytePhrase);
                    KeyStoreManager.putMasterPublicKey(pubKey, app);
//                    app.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                    BRAnimator.startBreadActivity(app, false);
                    if (!app.isDestroyed()) app.finish();
                    phraseForKeyStore = null;
                }

            }

        } finally {
            Arrays.fill(bytePhrase, (byte) 0);
        }

    }

    //BLOCKS
    public void onPublishTxAuth(final Context app, boolean authAsked) {

        final BRWalletManager walletManager = BRWalletManager.getInstance();
        byte[] rawSeed;
        try {
            rawSeed = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAY_REQUEST_CODE);
        } catch (BRKeystoreErrorException e) {
            if (authAsked) {
                showBugAuthLoopErrorMessage((Activity) app);
                Log.e(TAG, "onPublishTxAuth,!success && authAsked: " + e.getMessage());
            }
            e.printStackTrace();
            return;
        }
        if (rawSeed.length < 10) return;
        final byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);
        try {
            if (seed.length != 0) {
                boolean success = false;
                if (tmpTx != null) {
                    success = walletManager.publishSerializedTransaction(tmpTx, seed);
                    tmpTx = null;
                }
                if (!success) {
                    Log.e(TAG, "onPublishTxAuth: publishSerializedTransaction returned FALSE");
                    BRWalletManager.getInstance().offerToChangeTheAmount(app,
                            new PaymentItem(paymentRequest.addresses, paymentRequest.amount, null, paymentRequest.isPaymentRequest));
                    return;
                }
            } else {
                Log.e(TAG, "onPublishTxAuth: seed length is 0!");
                return;
            }
        } finally {
            Arrays.fill(seed, (byte) 0);
        }

    }

    public void onPaymentProtocolRequest(Activity app, boolean authAsked) {

        byte[] rawSeed;
        try {
            rawSeed = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE);
        } catch (BRKeystoreErrorException e) {
            if (authAsked) {
                showBugAuthLoopErrorMessage(app);
                Log.e(TAG, "onPublishTxAuth,!success && authAsked: " + e.getMessage());
            }
            return;
        }
        if (rawSeed == null || rawSeed.length < 10 || paymentRequest.serializedTx == null) {
            Log.d(TAG, "onPaymentProtocolRequest() returned: rawSeed is malformed: " + Arrays.toString(rawSeed));
            return;
        }
        if (rawSeed.length < 10) return;

        final byte[] seed = TypesConverter.getNullTerminatedPhrase(rawSeed);

        new Thread(new Runnable() {
            @Override
            public void run() {
                BRWalletManager.getInstance().publishSerializedTransaction(paymentRequest.serializedTx, seed);
                PaymentProtocolPostPaymentTask.sent = true;
                Arrays.fill(seed, (byte) 0);
                paymentRequest = null;
            }
        }).start();


    }

    public void setPhraseForKeyStore(String phraseForKeyStore) {
        this.phraseForKeyStore = phraseForKeyStore;
    }

    //
    public void setTmpTx(byte[] tmpTx) {
        this.tmpTx = tmpTx;
    }

    //
//    public void setUriAndLabel(String uri, String label) {
//        this.uri = uri;
//        this.label = label;
//    }
//
    public void setTmpPaymentRequest(PaymentRequestWrapper paymentRequest) {
        this.paymentRequest = paymentRequest;
    }

    public void onCanaryCheck(final Activity app, boolean authAsked) {
        String canary;
        try {
            canary = KeyStoreManager.getKeyStoreCanary(app, BRConstants.CANARY_REQUEST_CODE);
        } catch (BRKeystoreErrorException e) {
            if (authAsked) {
                showBugAuthLoopErrorMessage(app);
                Log.e(TAG, "onCanaryCheck: !success && authAsked");
            }
            return;
        }

        if (!canary.equalsIgnoreCase(BRConstants.CANARY_STRING)) {
            Log.e(TAG, "!canary.equalsIgnoreCase(BRConstants.CANARY_STRING)");
            byte[] phrase;
            try {
                phrase = KeyStoreManager.getKeyStorePhrase(app, BRConstants.CANARY_REQUEST_CODE);
            } catch (BRKeystoreErrorException e) {
                Log.e(TAG, "onCanaryCheck: error: " + e.getMessage());
                return;
            }
            String strPhrase = new String(phrase);
            if (strPhrase.isEmpty()) {
                BRWalletManager m = BRWalletManager.getInstance();
                m.wipeKeyStore(app);
                m.wipeWalletButKeystore(app);
            } else {
                try {
                    KeyStoreManager.putKeyStoreCanary(BRConstants.CANARY_STRING, app, 0);
                } catch (BRKeystoreErrorException e) {
                    e.printStackTrace();
                }
            }
        }
        BRWalletManager.getInstance().startTheWalletIfExists(app);
    }

    private void showBugAuthLoopErrorMessage(final Activity app) {
        if (app != null) {
//            BRWalletManager m = BRWalletManager.getInstance();
//            m.wipeKeyStore(app);
//            m.wipeWalletButKeystore(app);
            KeyStoreManager.showKeyStoreDialog(app, "Keystore invalidated", "Disable lock screen and all fingerprints, and re-enable to continue.", app.getString(R.string.Button_ok), null,
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
