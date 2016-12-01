package com.breadwallet.tools.security;

import android.app.AlertDialog;
import android.content.DialogInterface;
import android.os.Handler;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.presenter.fragments.FragmentScanResult;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.threads.PaymentProtocolTask;
import com.breadwallet.wallet.BRWalletManager;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URLDecoder;
import java.security.InvalidAlgorithmParameterException;

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

    public static synchronized boolean processRequest(MainActivity app, String address) {
        RequestObject requestObject = getRequestFromString(address);
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

    public static RequestObject getRequestFromString(String str)
            {
        if (str == null || str.isEmpty()) return null;
        RequestObject obj = new RequestObject();

        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");
        URI uri = URI.create(tmp);

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
