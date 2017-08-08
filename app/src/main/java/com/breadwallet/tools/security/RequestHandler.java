package com.breadwallet.tools.security;

import android.app.Activity;
import android.net.Uri;
import android.os.Handler;
import android.security.keystore.UserNotAuthenticatedException;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.exceptions.BRKeystoreErrorException;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.threads.PaymentProtocolTask;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.jniwrappers.BRBIP32Sequence;
import com.jniwrappers.BRKey;
import com.platform.APIClient;
import com.platform.middlewares.plugins.WalletPlugin;
import com.platform.tools.BRBitId;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.Arrays;
import java.util.List;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

import static com.breadwallet.tools.util.BRConstants.AUTH_FOR_BIT_ID;
import static com.breadwallet.tools.util.BRConstants.REQUEST_PHRASE_BITID;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 10/19/15.
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

public class RequestHandler {
    private static final String TAG = RequestHandler.class.getName();
    private static final Object lockObject = new Object();

    private static String _bitUri;
    private static String _bitCallback;
    private static String _strToSign = null;
    private static String _authString = null;
    private static int _index = 0;

    public static synchronized boolean processRequest(MainActivity app, String uri) {
        if (uri == null) return false;

        RequestObject requestObject = getRequestFromString(uri);
        if (requestObject == null) {
            if (app != null) {
                ((BreadWalletApp) app.getApplication()).showCustomDialog(app.getString(R.string.warning),
                        app.getString(R.string.invalid_address), app.getString(R.string.ok));
            }
            return false;
        }
        if (requestObject.r != null) {
            return tryAndProcessRequestURL(requestObject);
        } else if (requestObject.address != null) {
            return tryAndProcessBitcoinURL(requestObject, app);
        } else {
            if (app != null) {
                ((BreadWalletApp) app.getApplication()).showCustomDialog(app.getString(R.string.warning),
                        app.getString(R.string.bad_payment_request), app.getString(R.string.ok));
            }
            return false;
        }
    }

    public static boolean tryBitIdUri(final Activity app, String uri, JSONObject jsonBody) {
        if (uri == null) return false;
        boolean isBitUri = false;

        URI bitIdUri = null;
        try {
            bitIdUri = new URI(uri);
            if ("bitid".equals(bitIdUri.getScheme())) isBitUri = true;
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }

        _bitUri = uri;

        if (jsonBody != null) {
            try {
                _authString = jsonBody.getString("prompt_string");
                _bitCallback = jsonBody.getString("bitid_url");
                _index = jsonBody.getInt("bitid_index");
                _strToSign = jsonBody.getString("string_to_sign");
            } catch (JSONException e) {
                e.printStackTrace();
            }
        } else if (bitIdUri != null && "bitid".equals(bitIdUri.getScheme())) {
            if (app == null) {
                Log.e(TAG, "tryBitIdUri: app is null, returning true still");
                return isBitUri;
            }

            //ask for phrase, will system auth if needed

            _authString = "BitID Authentication Request";
        }

//        Log.e(TAG, "tryBitIdUri: _bitUri: " + _bitUri);
//        Log.e(TAG, "tryBitIdUri: _strToSign: " + _strToSign);
//        Log.e(TAG, "tryBitIdUri: _index: " + _index);

        new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] phrase = null;
                try {
                    Thread.sleep(500);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                Uri tmpUri = Uri.parse(_bitUri);
                try {
                    phrase = KeyStoreManager.getPhrase(app, REQUEST_PHRASE_BITID);
                    ((BreadWalletApp) app.getApplicationContext()).promptForAuthentication(app, AUTH_FOR_BIT_ID, null, tmpUri.getHost(), _authString, null, false);
                } catch (UserNotAuthenticatedException e) {
                    //asked the system, no need for local auth
                } finally {
                    //free the phrase
                    if (phrase != null) Arrays.fill(phrase, (byte) 0);
                }
            }
        }).start();
        return isBitUri;

    }

    public static void processBitIdResponse(final Activity app) {
        final byte[] phrase;
        final byte[] nulTermPhrase;
        final byte[] seed;
        if (app == null) {
            Log.e(TAG, "processBitIdResponse: app is null");
            return;
        }
        if (_bitUri == null) {
            Log.e(TAG, "processBitIdResponse: _bitUri is null");
            return;
        }

        final Uri uri = Uri.parse(_bitUri);
        try {
            phrase = KeyStoreManager.getPhrase(app, REQUEST_PHRASE_BITID);
        } catch (UserNotAuthenticatedException e) {
            return;
        }
        nulTermPhrase = TypesConverter.getNullTerminatedPhrase(phrase);
        seed = BRWalletManager.getSeedFromPhrase(nulTermPhrase);
        if (seed == null) {
            Log.e(TAG, "processBitIdResponse: seed is null!");
            return;
        }

        //run the callback
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (_strToSign == null) {
                        //meaning it's a link handling
                        String nonce = null;
                        String scheme = "https";
                        String query = uri.getQuery();
                        if (query == null) {
                            Log.e(TAG, "run: Malformed URI");
                            return;
                        }

                        String u = uri.getQueryParameter("u");
                        if (u != null && u.equalsIgnoreCase("1")) {
                            scheme = "http";
                        }

                        String x = uri.getQueryParameter("x");
                        if (Utils.isNullOrEmpty(x)) {

                            nonce = newNonce(app, uri.getHost() + uri.getPath());     // we are generating our own nonce
                        } else {
                            nonce = x;              // service is providing a nonce
                        }

                        String callbackUrl = String.format("%s://%s%s", scheme, uri.getHost(), uri.getPath());

                        // build a payload consisting of the signature, address and signed uri

                        String uriWithNonce = String.format("bitid://%s%s?x=%s", uri.getHost(), uri.getPath(), nonce);


                        final byte[] key = BRBIP32Sequence.getInstance().bip32BitIDKey(seed, _index, callbackUrl);

                        if (key == null) {
                            Log.d(TAG, "processBitIdResponse: key is null!");
                            return;
                        }

//                    Log.e(TAG, "run: uriWithNonce: " + uriWithNonce);

                        final String sig = BRBitId.signMessage(_strToSign == null ? uriWithNonce : _strToSign, new BRKey(key));
                        final String address = new BRKey(key).address();

                        JSONObject postJson = new JSONObject();
                        try {
                            postJson.put("address", address);
                            postJson.put("signature", sig);
                            if (_strToSign == null)
                                postJson.put("uri", uriWithNonce);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }

                        RequestBody requestBody = RequestBody.create(null, postJson.toString());
                        Request request = new Request.Builder()
                                .url(callbackUrl + "?x=" + nonce)
                                .post(requestBody)
                                .header("Content-Type", "application/json")
                                .build();
                        Response res = APIClient.getInstance(app).sendRequest(request, true, 0);
                        Log.e(TAG, "processBitIdResponse: res.code: " + res.code());
                        Log.e(TAG, "processBitIdResponse: res.code: " + res.message());
                        try {
                            Log.e(TAG, "processBitIdResponse: body: " + res.body().string());
                        } catch (IOException e) {
                            e.printStackTrace();
                        }
                    } else {
                        //meaning its the wallet plugin, glidera auth
                        String biUri = uri.getHost() == null ? uri.toString() : uri.getHost();
                        final byte[] key = BRBIP32Sequence.getInstance().bip32BitIDKey(seed, _index, biUri);
                        if (key == null) {
                            Log.d(TAG, "processBitIdResponse: key is null!");
                            return;
                        }

//                    Log.e(TAG, "run: uriWithNonce: " + uriWithNonce);
                        final String sig = BRBitId.signMessage(_strToSign, new BRKey(key));
                        final String address = new BRKey(key).address();

                        JSONObject postJson = new JSONObject();
                        try {
                            postJson.put("address", address);
                            postJson.put("signature", sig);
                        } catch (JSONException e) {
                            e.printStackTrace();
                        }
                        WalletPlugin.handleBitId(postJson);
                    }

                } finally {
                    //release everything
                    _bitUri = null;
                    _strToSign = null;
                    _bitCallback = null;
                    _authString = null;
                    _index = 0;
                    if (phrase != null) Arrays.fill(phrase, (byte) 0);
                    if (nulTermPhrase != null) Arrays.fill(nulTermPhrase, (byte) 0);
                    if (seed != null) Arrays.fill(seed, (byte) 0);
                }
            }
        }).start();

    }

    public static String newNonce(Activity app, String nonceKey) {
        // load previous nonces. we save all nonces generated for each service
        // so they are not used twice from the same device
        List<Integer> existingNonces = SharedPreferencesManager.getBitIdNonces(app, nonceKey);

        String nonce = "";
        while (existingNonces.contains(Integer.valueOf(nonce))) {
            nonce = String.valueOf(System.currentTimeMillis() / 1000);
        }
        existingNonces.add(Integer.valueOf(nonce));
        SharedPreferencesManager.putBitIdNonces(app, existingNonces, nonceKey);

        return nonce;
    }

    public static RequestObject getRequestFromString(String str) {
        if (str == null || str.isEmpty()) return null;
        RequestObject obj = new RequestObject();

        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");
        URI uri;
        try {
            uri = URI.create(tmp);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }

        if (uri.getScheme() == null || !uri.getScheme().equals("bitcoin")) {
            tmp = "bitcoin://".concat(tmp);
        } else {
            tmp = tmp.replace("bitcoin:", "bitcoin://");
        }
        uri = URI.create(tmp);
//        String[] parts = tmp.split("\\?", 2);
        String host = uri.getHost();
        if (host != null) {
            String addrs = host.trim();
            if (BRWalletManager.validateAddress(addrs)) {
                obj.address = addrs;
            }
        }
        String query = uri.getQuery();
        if (query == null) return obj;
        String[] params = query.split("&");
        for (String s : params) {
            String[] keyValue = s.split("=", 2);
            if (keyValue.length != 2)
                continue;
            if (keyValue[0].trim().equals("amount")) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(keyValue[1]);
                    obj.amount = bigDecimal.multiply(new BigDecimal("100000000")).toString();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
//                Log.e(TAG, "amount: " + obj.amount);
            } else if (keyValue[0].trim().equals("label")) {
                obj.label = keyValue[1];
//                Log.e(TAG, "label: " + obj.label);
            } else if (keyValue[0].trim().equals("message")) {
                obj.message = keyValue[1];
//                Log.e(TAG, "message: " + obj.message);
            } else if (keyValue[0].trim().startsWith("req")) {
                obj.req = keyValue[1];
//                Log.e(TAG, "req: " + obj.req);
            } else if (keyValue[0].trim().startsWith("r")) {
                obj.r = keyValue[1];
//                Log.e(TAG, "r: " + obj.r);
            }
        }
//        Log.e(TAG, "obj.address: " + obj.address);
        return obj;
    }

    private static boolean tryAndProcessRequestURL(RequestObject requestObject) {
        String theURL = null;
        String url = requestObject.r;
        synchronized (lockObject) {
            try {
                theURL = URLDecoder.decode(url, "UTF-8");
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }
            new PaymentProtocolTask().execute(theURL, requestObject.label);
        }
        return true;
    }

    private static boolean tryAndProcessBitcoinURL(RequestObject requestObject, MainActivity app) {
        /** use the C implementation to check it */
        final String str = requestObject.address;
        if (str == null) return false;
        final String[] addresses = new String[1];
        addresses[0] = str;
        if (requestObject.amount != null) {
            BigDecimal bigDecimal = new BigDecimal(requestObject.amount);
            long amount = bigDecimal.longValue();
            if (amount == 0 && app != null) {
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FragmentScanResult.address = str;
                        //TODO find a better way
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BRAnimator.animateScanResultFragment();
                            }
                        }, 500);

                    }
                });
                return false;
            }
            String strAmount = String.valueOf(amount);
            if (app != null) {
                BRWalletManager.getInstance(app).pay(addresses[0], new BigDecimal(strAmount), null, true);
            }
        } else {
            if (app != null)
                app.runOnUiThread(new Runnable() {
                    @Override
                    public void run() {
                        FragmentScanResult.address = str;
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                BRAnimator.animateScanResultFragment();
                            }
                        }, 1000);
                    }
                });
        }
        return true;
    }

    public static native PaymentRequestWrapper parsePaymentRequest(byte[] req);

    public static native String parsePaymentACK(byte[] req);

    public static native byte[] getCertificatesFromPaymentRequest(byte[] req, int index);

}
