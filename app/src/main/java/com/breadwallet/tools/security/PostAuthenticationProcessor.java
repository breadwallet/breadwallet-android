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
                e.printStackTrace();
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

            byte[] pubKey = BRWalletManager.getInstance(app).getMasterPubKey(phraseForKeyStore);
            KeyStoreManager.putMasterPublicKey(pubKey, app);
//                    Log.w(TAG, "The phrase from keystore is: " + KeyStoreManager.getKeyStoreString(getActivity()));
            app.overridePendingTransition(R.anim.fade_in, R.anim.fade_out);
            app.startMainActivity();
            if (!app.isDestroyed()) app.finish();
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
        Log.e(TAG, "onPaymentProtocolRequest");
        HttpURLConnection urlConnection = null;
        String certName = null;
        PaymentRequestWrapper paymentRequest = null;
        InputStream in;
        final MainActivity app = MainActivity.app;
        try {
            Log.e(TAG, "the uri: " + uri);
            if (uri == null) return;
            URL url = new URL(uri);
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setRequestProperty("Accept", "application/bitcoin-paymentrequest");
            urlConnection.setConnectTimeout(3000);
            urlConnection.setReadTimeout(3000);
            urlConnection.setUseCaches(false);
            in = urlConnection.getInputStream();

            String phrase = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAYMENT_PROTOCOL_REQUEST_CODE);
            if (phrase == null) {
                urlConnection.disconnect();
                return;
            }

            if (in == null) {
                Log.e(TAG, "The inputStream is null!");
                return;
            }
            byte[] serializedBytes = IOUtils.toByteArray(in);
            if (serializedBytes == null || serializedBytes.length == 0) {
                Log.e(TAG, "serializedBytes are null!!!");
                return;
            }

            paymentRequest = RequestHandler.parsePaymentRequest(serializedBytes, phrase);

            if (paymentRequest == null || paymentRequest.error == PaymentRequestWrapper.INVALID_REQUEST_ERROR) {
                Log.e(TAG, "paymentRequest is null!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "Invalid request", "ok");
                        }
                    });
                }
                return;
            } else if (paymentRequest.error == PaymentRequestWrapper.INSUFFICIENT_FUNDS_ERROR) {
                Log.e(TAG, "insufficient amount!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "Insufficient amount to satisfy the request", "ok");
                        }
                    });
                }
                return;
            } else if (paymentRequest.error == PaymentRequestWrapper.SIGNING_FAILED_ERROR) {
                Log.e(TAG, "failed to sign tx!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "Failed to sign transaction", "ok");
                        }
                    });
                }
                return;
            } else if (paymentRequest.error == PaymentRequestWrapper.REQUEST_TOO_LONG_ERROR) {
                Log.e(TAG, "failed to sign tx!!!");
                if (app != null) {
                    app.runOnUiThread(new Runnable() {
                        @Override
                        public void run() {
                            ((BreadWalletApp) app.getApplication()).showCustomDialog("Warning", "PaymentRequest message is too large", "ok");
                        }
                    });
                }
                return;
            }

            //Logging
            StringBuilder allAddresses = new StringBuilder();
            for (String s : paymentRequest.addresses) {
                allAddresses.append(s).append(", ");
                if (!BRWalletManager.validateAddress(s)) {
                    if (app != null)
                        ((BreadWalletApp) app.getApplication()).
                                showCustomDialog(app.getString(R.string.attention),
                                        String.format(app.getString(R.string.invalid_address_with_holder), s),
                                        app.getString(R.string.close));
                }
            }
            allAddresses.delete(allAddresses.length() - 2, allAddresses.length());
            //end logging
            if (paymentRequest.time > paymentRequest.expires) {
                Log.e(TAG, "Request is expired");
                return;
            }
            List<X509Certificate> certList = X509CertificateValidator.getCertificateFromBytes(serializedBytes);
            certName = X509CertificateValidator.certificateValidation(certList, paymentRequest);

        } catch (Exception e) {
            if (e instanceof java.net.UnknownHostException) {
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.attention), app.getString(R.string.unknown_host), app.getString(R.string.close));
            } else if (e instanceof FileNotFoundException) {
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.warning), app.getString(R.string.invalid_payment_request), app.getString(R.string.close));
            } else if (e instanceof SocketTimeoutException) {
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.warning), app.getString(R.string.connection_timed_out), app.getString(R.string.close));
            } else if (e instanceof CertificateChainNotFound) {
                Log.e(TAG, "No certificates!", e);
            } else {
                if (app != null)
                    ((BreadWalletApp) app.getApplication()).
                            showCustomDialog(app.getString(R.string.warning), app.getString(R.string.something_went_wrong), app.getString(R.string.close));
            }
            e.printStackTrace();
        } finally {
            if (urlConnection != null) urlConnection.disconnect();
        }
        if (paymentRequest != null && paymentRequest.serializedTx != null) {
            String seed = KeyStoreManager.getKeyStorePhrase(app, BRConstants.PAY_REQUEST_CODE);
            boolean success = BRWalletManager.getInstance(app).publishSerializedTransaction(paymentRequest.serializedTx, seed);
            if (app != null)
                if (!success)
                    ((BreadWalletApp) app.getApplication()).showCustomToast(app, "failed to send", MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
        } else {
            Log.e(TAG, "this.tmpTxObject is null!!!!!!!!!!!!!");
            if (app != null)
                ((BreadWalletApp) app.getApplication()).showCustomToast(app, "failed to send", MainActivity.screenParametersPoint.y / 2, Toast.LENGTH_LONG, 0);
        }

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


    public void clearTmpTxObject() {
        this.tmpTx = null;
    }
}
