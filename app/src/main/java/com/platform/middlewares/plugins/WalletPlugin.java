package com.platform.middlewares.plugins;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.SendManager;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.platform.APIClient;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Plugin;
import com.platform.tools.BRBitId;

import org.apache.commons.compress.utils.IOUtils;
import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.continuation.ContinuationSupport;
import org.eclipse.jetty.server.Request;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
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
    public boolean handle(String target, final Request baseRequest, HttpServletRequest request, final HttpServletResponse response) {
        if (!target.startsWith("/_wallet")) return false;
        Activity app = (Activity) BreadApp.getBreadContext();

        if (target.startsWith("/_wallet/info") && request.getMethod().equalsIgnoreCase("get")) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            WalletsMaster wm = WalletsMaster.getInstance(app);
            BaseWalletManager w = WalletBitcoinManager.getInstance(app);
            JSONObject jsonResp = new JSONObject();
            try {
                /**whether or not the users wallet is set up yet, or is currently locked*/
                jsonResp.put("no_wallet", wm.noWalletForPlatform(app));

                String address = w.getReceiveAddress(app).stringify();
                if (Utils.isNullOrEmpty(address)) {
                    throw new IllegalArgumentException("Bitcoin address is empty");
                }

                /**the current receive address*/
                jsonResp.put("receive_address", address);

                /**how digits after the decimal point. 2 = bits 8 = btc 6 = mbtc*/
                jsonResp.put("btc_denomiation_digits", w.getMaxDecimalPlaces(app));
                String preferredCode = BRSharedPrefs.getPreferredFiatIso(app);
                Currency fiatCurrency = Currency.getInstance(preferredCode);

                /**the users native fiat currency as an ISO 4217 code. Should be uppercased */
                jsonResp.put("local_currency_code", fiatCurrency.getCurrencyCode().toUpperCase());

                /**the user's fiat precision (e.g. 2 for USD, 0 for JPY, etc)*/
                jsonResp.put("local_currency_precision", fiatCurrency.getDefaultFractionDigits());

                /**the user's native fiat currency symbol*/
                jsonResp.put("local_currency_symbol", fiatCurrency.getSymbol());

                APIClient.BRResponse resp = new APIClient.BRResponse(jsonResp.toString().getBytes(), 200, "application/json");

                return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "handle: json error: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "json error", baseRequest, response);
            }
        } else if (target.startsWith("/_wallet/_event") && request.getMethod().equalsIgnoreCase("get")) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            byte[] rawData = BRHTTPHelper.getBody(request);
            String name = target.replace("/_event/", "");

            Log.e(TAG, "handle: body: " + new String(rawData != null ? rawData : "null".getBytes()));
            JSONObject json = null;
            if (rawData != null) {
                try {
                    json = new JSONObject(new String(rawData));
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            }
            if (json != null) {
                Map<String, String> attr = new HashMap<>();
                while (json.keys().hasNext()) {
                    String key = json.keys().next();
                    try {
                        attr.put(key, json.getString(key));
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, String.format("Failed to get the key: %s, from json: %s", key, json.toString()));
                    }
                }
                BREventManager.getInstance().pushEvent(name, attr);
            } else {
                BREventManager.getInstance().pushEvent(name);
            }
            APIClient.BRResponse resp = new APIClient.BRResponse(null, 200);
            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);

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
                BRBitId.signBitID(app, null, obj);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "handle: Failed to parse Json request body: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(400, "failed to parse json", baseRequest, response);
            }

            return true;
        } else if (target.startsWith("/_wallet/authenticate") && request.getMethod().equalsIgnoreCase("post")) {
            try {
                /**
                 POST /_wallet/authenticate
                 Verify that the current user is the wallet's owner.  Post a request of

                 {prompt: "Promt Text!", id: "<uuidv4>" }.
                 Get back an
                 {
                 "authenticated": true
                 }
                 */
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
                JSONObject obj = new JSONObject(reqBody);
                String authText = obj.getString("prompt");
                continuation = ContinuationSupport.getContinuation(request);
                continuation.suspend(response);
                globalBaseRequest = baseRequest;
                AuthManager.getInstance().authPrompt(app, authText, "", false, false, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject obj = new JSONObject();
                                try {
                                    obj.put("authenticated", true);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                if (continuation != null) {
                                    APIClient.BRResponse resp = new APIClient.BRResponse(obj.toString().getBytes(), 200, "application/json");
                                    BRHTTPHelper.handleSuccess(resp, globalBaseRequest, (HttpServletResponse) continuation.getServletResponse());
                                }
                                cleanUp();
                            }
                        });
                    }

                    @Override
                    public void onCancel() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject obj = new JSONObject();
                                try {
                                    obj.put("authenticated", false);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                APIClient.BRResponse resp = new APIClient.BRResponse(obj.toString().getBytes(), 200, "application/json");
                                BRHTTPHelper.handleSuccess(resp, globalBaseRequest, (HttpServletResponse) continuation.getServletResponse());
                                cleanUp();
                            }
                        });

                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "handle: Failed to parse Json request body: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(400, "failed to parse json", baseRequest, response);
            }
        } else if (target.startsWith("/_wallet/currencies")) {
            JSONArray arr = getCurrencyData(app);
            if (arr.length() == 0) {
                BRReportsManager.reportBug(new IllegalArgumentException("_wallet/currencies created an empty json"));
                return BRHTTPHelper.handleError(500, "Failed to create json", baseRequest, response);
            }
            APIClient.BRResponse resp = new APIClient.BRResponse(arr.toString().getBytes(), 200, "application/json");
            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
        } else if (target.startsWith("/_wallet/transaction")) {
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
                sendTx(app, obj);

                continuation = ContinuationSupport.getContinuation(request);
                continuation.suspend(response);
                globalBaseRequest = baseRequest;

                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return BRHTTPHelper.handleError(500, "Invalid json request", baseRequest, response);

        } else if (target.startsWith("/_wallet/addresses")) {
            String iso = target.substring(target.lastIndexOf("/") + 1);
            BaseWalletManager w = WalletsMaster.getInstance(app).getWalletByIso(app, iso);
            if (w == null) {
                return BRHTTPHelper.handleError(500, "Invalid iso for address: " + iso, baseRequest, response);
            }

            JSONObject obj = new JSONObject();
            try {
                obj.put("currency", w.getIso());
                obj.put("address", w.getReceiveAddress(app).stringify());
            } catch (JSONException e) {
                e.printStackTrace();
            }

            APIClient.BRResponse resp = new APIClient.BRResponse(obj.toString().getBytes(), 200, "application/json");

            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
        }

        Log.e(TAG, "handle: WALLET PLUGIN DID NOT HANDLE: " + target + " " + baseRequest.getMethod());
        return true;
    }

    private void sendTx(final Context app, JSONObject obj) {
        String toAddress = null;
        String toDescription = null;
        String currency = null;
        String numerator = null;
        String denominator = null;
        String txCurrency = null;
        try {
            toAddress = obj.getString("toAddress");
            toDescription = obj.getString("toDescription");
            currency = obj.getString("currency");
            JSONObject amount = obj.getJSONObject("amount");
            numerator = amount.getString("numerator");
            denominator = amount.getString("denominator");
            txCurrency = amount.getString("currency");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "sendTx: " + String.format("address (%s), description (%s), currency (%s), numerator (%s), denominator(%s), txCurrency(%s)",
                toAddress, toDescription, currency, numerator, denominator, txCurrency));

        if (Utils.isNullOrEmpty(toAddress) || Utils.isNullOrEmpty(toDescription) || Utils.isNullOrEmpty(currency)
                || Utils.isNullOrEmpty(numerator) || Utils.isNullOrEmpty(denominator) || Utils.isNullOrEmpty(txCurrency)) {
            return;
        }

        final BaseWalletManager wm = WalletsMaster.getInstance(app).getWalletByIso(app, currency);
        String addr = wm.undecorateAddress(toAddress);
        if (Utils.isNullOrEmpty(addr)) {
            BRDialog.showSimpleDialog(app, "Failed to create tx for exchange!", "Address is empty");
            return;
        }
        BigDecimal bigAmount = WalletsMaster.getInstance(app).isIsoErc20(app, currency) ?
                new BigDecimal(numerator).divide(new BigDecimal(denominator), nrOfZeros(denominator), BRConstants.ROUNDING_MODE) :
                new BigDecimal(numerator);
        final CryptoRequest item = new CryptoRequest(null, false, null, addr, bigAmount);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                SendManager.sendTransaction(app, item, wm, new SendManager.SendCompletion() {
                    @Override
                    public void onCompleted(String hash, boolean succeed) {
                        finalizeTx(succeed, hash);
                    }
                });
            }
        });

    }

    private static int nrOfZeros(String n) {
        int count = 0;
        while (n.charAt(n.length() - 1) == '0') {
            n = new BigDecimal(n).divide(new BigDecimal(10)).toPlainString();
            count++;
        }
        return count;
    }

    private JSONArray getCurrencyData(Context app) {
        JSONArray arr = new JSONArray();
        List<BaseWalletManager> list = WalletsMaster.getInstance(app).getAllWallets(app);
        for (BaseWalletManager w : list) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("id", w.getIso());
                obj.put("ticker", w.getIso());
                obj.put("name", w.getName());

                //Colors
                JSONArray colors = new JSONArray();
                colors.put(w.getUiConfiguration().getStartColor());
                colors.put(w.getUiConfiguration().getEndColor());

                obj.put("colors", colors);

                //Balance
                //TODO Temporary solution due to the fact that the erc20 balances are stored in Decimals rather than smallest currency (WEI, SATOSHIS)
                JSONObject balance = new JSONObject();

                boolean isErc20 = WalletsMaster.getInstance(app).isIsoErc20(app, w.getIso());
                BigDecimal rawBalance = w.getCachedBalance(app);
                String denominator = w.getDenominator();
                balance.put("currency", w.getIso());
                balance.put("numerator", isErc20 ? rawBalance.multiply(new BigDecimal(denominator)).toPlainString() : rawBalance.toPlainString());
                balance.put("denominator", denominator);

                //Fiat balance
                JSONObject fiatBalance = new JSONObject();

                fiatBalance.put("currency", BRSharedPrefs.getPreferredFiatIso(app));
                fiatBalance.put("numerator", w.getFiatBalance(app).multiply(new BigDecimal(100)).toPlainString());
                fiatBalance.put("denominator", String.valueOf(100));

                //Exchange
                JSONObject exchange = new JSONObject();

                exchange.put("currency", BRSharedPrefs.getPreferredFiatIso(app));
                exchange.put("numerator", w.getFiatExchangeRate(app).multiply(new BigDecimal(100)).toPlainString());
                exchange.put("denominator", String.valueOf(100));

                obj.put("balance", balance);
                obj.put("fiatBalance", fiatBalance);
                obj.put("exchange", exchange);
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return arr;
    }

    public static void finalizeTx(final boolean succeed, final String hash) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!succeed || Utils.isNullOrEmpty(hash)) {
                        try {
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(500);
                        } catch (IOException e) {
                            Log.e(TAG, "finalizeTx: failed to send error 500: ", e);
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (continuation == null) {
                        Log.e(TAG, "finalizeTx: WARNING continuation is null");
                        return;
                    }

                    JSONObject result = new JSONObject();
                    try {
                        result.put("hash", hash);
                        result.put("transmitted", true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        continuation.getServletResponse().setContentType("application/json");
                        continuation.getServletResponse().setCharacterEncoding("UTF-8");
                        continuation.getServletResponse().getWriter().print(result.toString());
                        Log.d(TAG, "finalizeTx: finished with writing to the response: " + result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "sendBitIdResponse Failed to send json: ", e);
                    }
                    ((HttpServletResponse) continuation.getServletResponse()).setStatus(200);
                } finally {
                    cleanUp();
                }
            }
        });
    }

    public static void sendBitIdResponse(final JSONObject restJson,
                                         final boolean authenticated) {
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!authenticated) {
                        try {
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(401);
                        } catch (IOException e) {
                            Log.e(TAG, "sendBitIdResponse: failed to send error 401: ", e);
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (restJson == null || restJson.isNull("signature")) {
                        Log.e(TAG, "sendBitIdResponse: WARNING restJson is null: " + restJson);
                        try {
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(500, "json malformed or null");
                        } catch (IOException e) {
                            Log.e(TAG, "sendBitIdResponse: failed to send error 401: ", e);
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (continuation == null) {
                        Log.e(TAG, "sendBitIdResponse: WARNING continuation is null");
                        return;
                    }

                    try {
                        continuation.getServletResponse().setContentType("application/json");
                        continuation.getServletResponse().setCharacterEncoding("UTF-8");
                        continuation.getServletResponse().getWriter().print(restJson);
                        Log.d(TAG, "sendBitIdResponse: finished with writing to the response: " + restJson);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "sendBitIdResponse Failed to send json: ", e);
                    }
                    ((HttpServletResponse) continuation.getServletResponse()).setStatus(200);
                } finally {
                    cleanUp();
                }
            }
        });

    }

    private static void cleanUp() {
        if (globalBaseRequest != null)
            globalBaseRequest.setHandled(true);
        if (continuation != null)
            continuation.complete();
        continuation = null;
        globalBaseRequest = null;
    }
}