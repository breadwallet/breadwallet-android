package com.platform.middlewares.plugins;

import android.app.Activity;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
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
import java.util.Currency;
import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import timber.log.Timber;


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
    private static Continuation continuation;
    private static Request globalBaseRequest;

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (!target.startsWith("/_wallet")) return false;
        Activity app = (Activity) BreadApp.getBreadContext();

        if (target.startsWith("/_wallet/info") && request.getMethod().equalsIgnoreCase("get")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
            if (app == null) {
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
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
                jsonResp.put("ltc_denomiation_digits", BRSharedPrefs.getCurrencyUnit(app) == BRConstants.CURRENT_UNIT_LITECOINS ? 8 : 2);

                /**the users native fiat currency as an ISO 4217 code. Should be uppercased */
                jsonResp.put("local_currency_code", Currency.getInstance(Locale.getDefault()).getCurrencyCode().toUpperCase());
                return BRHTTPHelper.handleSuccess(200, jsonResp.toString().getBytes(), baseRequest, response, "application/json");
            } catch (JSONException e) {
                Timber.e(e);
                return BRHTTPHelper.handleError(500, "json error", baseRequest, response);
            }
        } else if (target.startsWith("/_wallet/_event") && request.getMethod().equalsIgnoreCase("get")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
            byte[] rawData = BRHTTPHelper.getBody(request);
            String name = target.replace("/_event/", "");

            Timber.d("handle: body: %s", new String(rawData != null ? rawData : "null".getBytes()));
            JSONObject json = null;
            if (rawData != null) {
                try {
                    json = new JSONObject(new String(rawData));
                } catch (JSONException e) {
                    Timber.e(e);
                }
            }
            if (json != null) {
                Map<String, String> attr = new HashMap<>();
                while (json.keys().hasNext()) {
                    String key = json.keys().next();
                    try {
                        attr.put(key, json.getString(key));
                    } catch (JSONException e) {
                        Timber.e(e);
                    }
                }
                BREventManager.getInstance().pushEvent(name, attr);
            } else {
                BREventManager.getInstance().pushEvent(name);
            }
            return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);

        } else if (target.startsWith("/_wallet/sign_bitid") && request.getMethod().equalsIgnoreCase("post")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
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
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            String contentType = request.getHeader("content-type");
            if (contentType == null || !contentType.equalsIgnoreCase("application/json")) {
                Timber.d("handle: content type is not application/json: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(400, null, baseRequest, response);
            }
            String reqBody = null;
            try {
                reqBody = new String(IOUtils.toByteArray(request.getInputStream()));
            } catch (IOException e) {
                Timber.e(e);
            }
            if (Utils.isNullOrEmpty(reqBody)) {
                Timber.d("handle: reqBody is empty: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(400, null, baseRequest, response);
            }

            try {
                JSONObject obj = new JSONObject(reqBody);
                continuation = ContinuationSupport.getContinuation(request);
                continuation.suspend(response);
                globalBaseRequest = baseRequest;
                BRBitId.signBitID(app, null, obj);
            } catch (JSONException e) {
                Timber.e(e);
                return BRHTTPHelper.handleError(400, "failed to parse json", baseRequest, response);
            }

            return true;
        }

        Timber.d("handle: WALLET PLUGIN DID NOT HANDLE: %s %s", target, baseRequest.getMethod());
        return true;
    }


    public static void sendBitIdResponse(final JSONObject restJson, final boolean authenticated) {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!authenticated) {
                        try {
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(401);
                        } catch (IOException e) {
                            Timber.e(e, "sendBitIdResponse: failed to send error 401");
                        }
                        return;
                    }
                    if (restJson == null || restJson.isNull("signature")) {
                        Timber.d("sendBitIdResponse: WARNING restJson is null: %s", restJson);
                        try {
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(500, "json malformed or null");
                        } catch (IOException e) {
                            Timber.e(e, "sendBitIdResponse: failed to send error 401");
                        }
                        return;
                    }
                    if (continuation == null) {
                        Timber.d("sendBitIdResponse: WARNING continuation is null");
                        return;
                    }

                    try {
                        continuation.getServletResponse().setContentType("application/json");
                        continuation.getServletResponse().setCharacterEncoding("UTF-8");
                        continuation.getServletResponse().getWriter().print(restJson);
                        Timber.d("sendBitIdResponse: finished with writing to the response: %s", restJson);
                    } catch (Exception e) {
                        Timber.e(e, "sendBitIdResponse Failed to send json: ");
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
        });
    }
}