package com.platform.middlewares.plugins;

import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.interfaces.Plugin;

import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.breadwallet.tools.util.BRStringFormatter.getFormattedCurrencyString;
import static com.google.firebase.analytics.FirebaseAnalytics.getInstance;
import static org.eclipse.jetty.http.HttpMethod.POST;

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

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (!target.startsWith("/_wallet")) return false;

        if (target.startsWith("/_wallet/info") && request.getMethod().equalsIgnoreCase("get")) {
            final MainActivity app = MainActivity.app;
            if (app == null) {
                try {
                    response.sendError(500, "context is null");
                    baseRequest.setHandled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            BRWalletManager wm = BRWalletManager.getInstance(app);
            JSONObject jsonResp = new JSONObject();
            try {
                jsonResp.put("no_wallet", wm.noWalletForPlatform(app));
                jsonResp.put("watch_only", false);
                jsonResp.put("receive_address", BRWalletManager.getReceiveAddress());
                response.setStatus(200);
                response.getWriter().write(jsonResp.toString());
                baseRequest.setHandled(true);
            } catch (JSONException e) {
                e.printStackTrace();
                try {
                    response.sendError(500, "json error");
                    baseRequest.setHandled(true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    response.sendError(500, "IO exception: " + e.getMessage());
                    baseRequest.setHandled(true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            return true;
        } else if (target.startsWith("/_wallet/format") && request.getMethod().equalsIgnoreCase("get")) {
            String amount = request.getParameter("amount");
            if (Utils.isNullOrEmpty(amount)) {
                try {
                    response.sendError(400);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                baseRequest.setHandled(true);
                return true;
            }
            long satAmount;

            if (amount.contains(".")) {
                // assume full bitcoins
                satAmount = new BigDecimal(amount).multiply(new BigDecimal("100000000")).longValue();
            } else {
                satAmount = Long.valueOf(amount);
            }

            try {
                response.setStatus(200);
                response.getWriter().write(BRStringFormatter.getFormattedCurrencyString(Locale.getDefault().getISO3Language(), satAmount));
                baseRequest.setHandled(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
        } else if (target.startsWith("/_wallet/sign_bitid") && request.getMethod().equalsIgnoreCase("post")) {
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
            String contentType = request.getHeader("content-type");
            if (contentType == null || !contentType.equalsIgnoreCase("application/json")) {
                try {
                    response.sendError(400);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                baseRequest.setHandled(true);
                return true;
            }
            String strResp = null;
            try {
                strResp = new String(IOUtils.toByteArray(request.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Utils.isNullOrEmpty(strResp)) {
                try {
                    response.sendError(400);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                baseRequest.setHandled(true);
                return true;
            }

            String stringToSign = null;
            String bitIdUrl = null;
            int bitIdIndex = 0;

            try {
                JSONObject obj = new JSONObject(strResp);

            } catch (JSONException e) {
                e.printStackTrace();
            }

            return true;
        }

        Log.e(TAG, "handle: WALLET PLUGIN DID NOT HANDLE: " + target + " (" + request.getMethod() + ")");
        return true;
    }
}
