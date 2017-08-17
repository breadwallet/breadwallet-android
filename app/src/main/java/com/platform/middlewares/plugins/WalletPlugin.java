package com.platform.middlewares.plugins;

import android.app.Activity;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BitcoinUrlHandler;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Plugin;
import com.platform.tools.BRBitId;

import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/2/16.
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
public class WalletPlugin implements Plugin {
    public static final String TAG = WalletPlugin.class.getName();
    private static Continuation continuation;
    private static Request globalBaseRequest;

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (!target.startsWith("/_wallet")) return false;
        Activity app = BreadApp.getBreadContext();

        if (target.startsWith("/_wallet/info") && request.getMethod().equalsIgnoreCase("get")) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            BRWalletManager wm = BRWalletManager.getInstance();
            JSONObject jsonResp = new JSONObject();
            try {
                /**whether or not the users wallet is set up yet, or is currently locked*/
                jsonResp.put("no_wallet", wm.noWalletForPlatform(app));

                /**the current receive address*/
                jsonResp.put("receive_address", BRWalletManager.getReceiveAddress());

                /**how digits after the decimal point. 2 = bits 8 = btc 6 = mbtc*/
                jsonResp.put("btc_denomiation_digits", BRSharedPrefs.getCurrencyUnit(app) == BRConstants.CURRENT_UNIT_BITCOINS ? 8 : 2);

                /**the users native fiat currency as an ISO 4217 code. Should be uppercased */
                jsonResp.put("local_currency_code", Currency.getInstance(Locale.getDefault()).getCurrencyCode().toUpperCase());
                return BRHTTPHelper.handleSuccess(200, jsonResp.toString().getBytes(), baseRequest, response, "application/json");
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "handle: json error: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "json error", baseRequest, response);
            }
        } else if (target.startsWith("/_event") && request.getMethod().equalsIgnoreCase("get")) {
            //todo refactor this for events
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            byte[] rawData = null;
            try {
                InputStream body = request.getInputStream();
                rawData = IOUtils.toByteArray(body);
            } catch (IOException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "handle: body: " + new String(rawData != null ? rawData : "null".getBytes()));
//            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
//            String amount = request.getParameter("amount");
//            if (Utils.isNullOrEmpty(amount)) {
//                Log.e(TAG, "handle: amount is not specified: " + target + " " + baseRequest.getMethod());
//                return BRHTTPHelper.handleError(400, null, baseRequest, response);
//            }
//            long satAmount;
//
//            if (amount.contains(".")) {
//                // assume full bitcoins
//                satAmount = new BigDecimal(amount).multiply(new BigDecimal("100000000")).longValue();
//            } else {
//                satAmount = Long.valueOf(amount);
//            }
//            return BRHTTPHelper.handleSuccess(200, BRCurrency.getFormattedCurrencyString(app, Locale.getDefault().getISO3Language(), new BigDecimal(satAmount)).getBytes(), baseRequest, response, null);
        } else if (target.startsWith("/_wallet/sign_bitid") && request.getMethod().equalsIgnoreCase("post")) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            /**
             * POST /_wallet/sign_bitid

             Sign a message using the user's BitID private key. Calling this WILL trigger authentication

             Request body: application/json
             {
             "prompt_string": "Sign in to My Service", // shown to the user in the authentication prompt
             "string_to_sign": "https://bitid.org/bitid?x=2783408723", // the string to sign
             "bitid_url": "https://bitid.org/bitid", // the bitid url for deriving the private key
             "bitid_index": "0" // the bitid index as a string (just pass "0")
             }

             Response body: application/json
             {
             "signature": "oibwaeofbawoefb" // base64-encoded signature
             }
             */
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            String contentType = request.getHeader("content-type");
            if (contentType == null || !contentType.equalsIgnoreCase("application/json")) {
                Log.e(TAG, "handle: content type is not application/json: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(400, null, baseRequest, response);
            }
            String reqBody = null;
            try {
                reqBody = new String(IOUtils.toByteArray(request.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Utils.isNullOrEmpty(reqBody)) {
                Log.e(TAG, "handle: reqBody is empty: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(400, null, baseRequest, response);
            }

            try {
                JSONObject obj = new JSONObject(reqBody);
                continuation = ContinuationSupport.getContinuation(request);
                continuation.suspend(response);
                globalBaseRequest = baseRequest;
                BRBitId.tryBitIdUri(app, obj.getString("bitid_url"), obj);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "handle: Failed to parse Json request body: " + target + " " + baseRequest.getMethod());
                return true;
            }

            return true;
        }

        Log.e(TAG, "handle: WALLET PLUGIN DID NOT HANDLE: " + target + " " + baseRequest.getMethod());
        return true;
    }


    public static void handleBitId(final JSONObject restJson, final boolean authenticated) {

        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!authenticated) {
                        try {
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(401);
                        } catch (IOException e) {
                            Log.e(TAG, "handleBitId: failed to send error 401: ", e);
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (restJson == null || restJson.isNull("signature")) {
                        Log.e(TAG, "handleBitId: WARNING restJson is null: " + restJson);
                        return;
                    }
                    if (continuation == null) {
                        Log.e(TAG, "handleBitId: WARNING continuation is null");
                        return;
                    }

                    try {
                        continuation.getServletResponse().setContentType("application/json");
                        continuation.getServletResponse().setCharacterEncoding("UTF-8");
                        continuation.getServletResponse().getWriter().print(restJson);
                        Log.d(TAG, "handleBitId: finished with writing to the response: " + restJson);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "handleBitId Failed to send json: ", e);
                    }
                    ((HttpServletResponse) continuation.getServletResponse()).setStatus(200);
                } finally {
                    if (globalBaseRequest != null)
                        globalBaseRequest.setHandled(true);
                    if (continuation != null)
                        continuation.complete();
                    continuation = null;
                    globalBaseRequest = null;
                }
            }
        }).start();

    }
}