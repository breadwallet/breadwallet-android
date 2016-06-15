package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.os.Handler;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.entities.TmpTxObject;
import com.breadwallet.presenter.exceptions.CertificateChainNotFound;
import com.breadwallet.presenter.fragments.FragmentRecoveryPhrase;
import com.breadwallet.presenter.fragments.FragmentSettings;
import com.breadwallet.presenter.fragments.IntroWelcomeFragment;
import com.breadwallet.tools.BRConstants;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.threads.PaymentProtocolTask;
import com.breadwallet.wallet.BRWalletManager;

import org.apache.commons.io.IOUtils;

import java.io.FileNotFoundException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.SocketTimeoutException;
import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.List;

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
    private String uri;

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
            try {
                app.showWarningFragment();
            } catch (Exception e) {
                BRWalletManager m = BRWalletManager.getInstance(app);
                m.wipeKeyStore();
                m.wipeWalletButKeystore(app);
                FragmentAnimator.resetFragmentAnimator();
                app.finish();
            }
        } else {
            throw new NullPointerException("failed to generate seed");
        }
    }

    public void onRecoverWalletAuth(IntroActivity app) {
        Log.e(TAG, "onRecoverWalletAuth");
        if (phraseForKeyStore == null) return;

        try {
            boolean success = KeyStoreManager.putKeyStorePhrase(phraseForKeyStore, app, BRConstants.PUT_PHRASE_RECOVERY_WALLET_REQUEST_CODE);
            boolean success2 = false;
            if (success)
                success2 = KeyStoreManager.putKeyStoreCanary(BRConstants.CANARY_STRING, app, 0);
            if (!success || !success2)
                return;
            if (phraseForKeyStore.length() != 0) {
                byte[] pubKey = BRWalletManager.getInstance(app).getMasterPubKey(phraseForKeyStore);
                KeyStoreManager.putMasterPublicKey(pubKey, app);
//                    Log.w(TAG, "The phrase from keystore is: " + KeyStoreManager.getKeyStoreString(getActivity()));
                app.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
                app.startMainActivity();
                if (!app.isDestroyed()) app.finish();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            phraseForKeyStore = null;
        }
    }

    public void onShowPhraseAuth(MainActivity app) {
        Log.e(TAG, "onShowPhraseAuth");
        try {
            FragmentAnimator.animateSlideToLeft(app, new FragmentRecoveryPhrase(), new FragmentSettings());
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void onPublishTxAuth(MainActivity app) {
        Log.e(TAG, "onPublishTxAuth");
        if (this.tmpTx != null) {
            String seed = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAY_REQUEST_CODE);
            boolean success = BRWalletManager.getInstance(app).publishSerializedTransaction(tmpTx, seed);
            tmpTx = null;
            FragmentAnimator.hideScanResultFragment();
            if (!success)
                ((BreadWalletApp) app.getApplication()).showCustomToast(app, "failed to send", MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
        } else {
            Log.e(TAG, "this.tmpTxObject is null!!!!!!!!!!!!!");
        }
    }

    public void onCanaryCheckAuth(IntroActivity app) {
        Log.e(TAG, "onCanaryCheckAuth");

        try {
            String canary = KeyStoreManager.getKeyStoreCanary(app, BRConstants.CANARY_REQUEST_CODE);
            if (canary.equals("noauth")) return;
            byte[] masterPubKey = KeyStoreManager.getMasterPublicKey(app);
            boolean isFirstAddressCorrect = false;
            if (masterPubKey != null && masterPubKey.length != 0) {
                isFirstAddressCorrect = app.checkFirstAddress(masterPubKey);
            }
            Log.e(TAG, "isFirstAddressCorrect: " + isFirstAddressCorrect);
            if (!isFirstAddressCorrect) {
                Log.e(TAG, "CLEARING THE WALLET");
                BRWalletManager.getInstance(app).wipeWalletButKeystore(app);
            }

            if (canary.equals("none")) {
                BRWalletManager m = BRWalletManager.getInstance(app);
                m.wipeWalletButKeystore(app);
                m.wipeKeyStore();

            }

            app.getFragmentManager().beginTransaction().add(R.id.intro_layout, new IntroWelcomeFragment(),
                    IntroWelcomeFragment.class.getName()).commit();
            app.startTheWalletIfExists();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void onPaymentProtocolRequest() {
        new PaymentProtocolTask().execute(uri);
    }


    public void setPhraseForKeyStore(String phraseForKeyStore) {
        Log.e(TAG, "setPhraseForKeyStore");
        this.phraseForKeyStore = phraseForKeyStore;
    }

    public void setTmpTx(byte[] tmpTx) {
        Log.e(TAG, "tmpTx: " + Arrays.toString(tmpTx));
        this.tmpTx = tmpTx;
    }

    public void setUri(String uri) {
        this.uri = uri;
    }

}
