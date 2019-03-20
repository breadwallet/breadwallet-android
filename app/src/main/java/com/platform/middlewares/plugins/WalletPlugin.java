package com.platform.middlewares.plugins;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.util.EventUtils;
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
import java.nio.charset.StandardCharsets;
import java.util.Currency;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static javax.servlet.http.HttpServletResponse.*;
import static org.eclipse.jetty.http.HttpMethod.GET;
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
    private static Continuation continuation;
    private static Request globalBaseRequest;
    private static final String PATH_BASE = "/_wallet";
    private static final String PATH_INFO = "/info";
    private static final String PATH_EVENT = "/_event";
    private static final String PATH_SIGN_BITID = "/sign_bitid";
    private static final String PATH_AUTHENTICATE = "/authenticate";
    private static final String PATH_CURRENCIES = "/currencies";
    private static final String PATH_TRANSACTION = "/transaction";
    private static final String PATH_ADDRESSES = "/addresses";

    private static final String KEY_NO_WALLET = "no_wallet";
    private static final String KEY_RECEIVE_ADDRESS = "receive_address";
    private static final String KEY_BTC_DENOMINATION_DIGITS = "btc_denomination_digits";
    private static final String KEY_LOCAL_CURRENCY_CODE = "local_currency_code";
    private static final String KEY_LOCAL_CURRENCY_PRECISION = "local_currency_precision";
    private static final String KEY_LOCAL_CURRENCY_SYMBOL = "local_currency_symbol";
    private static final String KEY_PROMPT = "prompt";
    private static final String KEY_AUTHENTICATED = "authenticated";
    private static final String KEY_CURRENCY = "currency";
    private static final String KEY_ADDRESS = "address";
    private static final String KEY_TO_ADDRESS = "toAddress";
    private static final String KEY_TO_DESCRIPTION = "toDescription";
    private static final String KEY_AMOUNT = "amount";
    private static final String KEY_NUMERATOR = "numerator";
    private static final String KEY_DENOMINATOR = "denominator";

    private static final String KEY_ID = "id";
    private static final String KEY_TICKER = "ticker";
    private static final String KEY_NAME = "name";
    private static final String KEY_COLORS = "colors";
    private static final String KEY_BALANCE = "balance";
    private static final String KEY_FIAT_BALANCE = "fiatBalance";
    private static final String KEY_EXCHANGE = "exchange";
    private static final String KEY_HASH = "hash";
    private static final String KEY_TRANSMITTED = "transmitted";
    private static final String KEY_SIGNATURE = "signature";

    private static final int DOLLAR_IN_CENTS = 100;

    @Override
    public boolean handle(String target, final Request baseRequest, HttpServletRequest request, final HttpServletResponse response) {
        if (!target.startsWith(PATH_BASE)) {
            return false;
        }
        Activity context = (Activity) BreadApp.getBreadContext();

        if (target.startsWith(PATH_BASE + PATH_INFO) && request.getMethod().equalsIgnoreCase(GET.asString())) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            if (context == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(SC_INTERNAL_SERVER_ERROR, "context is null", baseRequest, response);
            }
            WalletsMaster walletsMaster = WalletsMaster.getInstance();
            BaseWalletManager walletManager = WalletBitcoinManager.getInstance(context);
            JSONObject jsonResp = new JSONObject();
            try {
                /**whether or not the users wallet is set up yet, or is currently locked*/
                jsonResp.put(KEY_NO_WALLET, walletsMaster.noWalletForPlatform(context));

                String address = getLegacyAddress(context, walletManager);
                if (Utils.isNullOrEmpty(address)) {
                    throw new IllegalArgumentException("Address is empty");
                }

                /**the current receive address*/
                jsonResp.put(KEY_RECEIVE_ADDRESS, address);

                /**how digits after the decimal point. 2 = bits 8 = btc 6 = mbtc*/
                jsonResp.put(KEY_BTC_DENOMINATION_DIGITS, walletManager.getMaxDecimalPlaces(context));
                String preferredCode = BRSharedPrefs.getPreferredFiatIso(context);
                Currency fiatCurrency = Currency.getInstance(preferredCode);

                /**the users native fiat currency as an ISO 4217 code. Should be uppercased */
                jsonResp.put(KEY_LOCAL_CURRENCY_CODE, fiatCurrency.getCurrencyCode().toUpperCase());

                /**the user's fiat precision (e.g. 2 for USD, 0 for JPY, etc)*/
                jsonResp.put(KEY_LOCAL_CURRENCY_PRECISION, fiatCurrency.getDefaultFractionDigits());

                /**the user's native fiat currency symbol*/
                jsonResp.put(KEY_LOCAL_CURRENCY_SYMBOL, fiatCurrency.getSymbol());

                APIClient.BRResponse resp = new APIClient.BRResponse(jsonResp.toString().getBytes(),
                        SC_OK, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8);

                return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "handle: json error: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(SC_INTERNAL_SERVER_ERROR, "json error", baseRequest, response);
            }
        } else if (target.startsWith(PATH_BASE + PATH_EVENT) && request.getMethod().equalsIgnoreCase(GET.asString())) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            byte[] rawData = BRHTTPHelper.getBody(request);
            String name = target.replace(PATH_EVENT + "/", "");

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
                EventUtils.pushEvent(name, attr);
            } else {
                EventUtils.pushEvent(name);
            }
            APIClient.BRResponse resp = new APIClient.BRResponse(null, SC_OK);
            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);

        } else if (target.startsWith(PATH_BASE + PATH_SIGN_BITID) && request.getMethod().equalsIgnoreCase(POST.asString())) {
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
            if (context == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(SC_INTERNAL_SERVER_ERROR, "context is null", baseRequest, response);
            }
            String contentType = request.getHeader(BRConstants.HEADER_CONTENT_TYPE);
            if (contentType == null || !contentType.equalsIgnoreCase(BRConstants.CONTENT_TYPE_JSON)) {
                Log.e(TAG, "handle: content type is not application/json: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(SC_BAD_REQUEST, null, baseRequest, response);
            }
            String reqBody = null;
            try {
                reqBody = new String(IOUtils.toByteArray(request.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Utils.isNullOrEmpty(reqBody)) {
                Log.e(TAG, "handle: reqBody is empty: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(SC_BAD_REQUEST, null, baseRequest, response);
            }

            try {
                JSONObject obj = new JSONObject(reqBody);
                continuation = ContinuationSupport.getContinuation(request);
                continuation.suspend(response);
                globalBaseRequest = baseRequest;
                BRBitId.signBitID(context, null, obj);
            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "handle: Failed to parse Json request body: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(SC_BAD_REQUEST, "failed to parse json", baseRequest, response);
            }

            return true;
        } else if (target.startsWith(PATH_BASE + PATH_AUTHENTICATE) && request.getMethod().equalsIgnoreCase(POST.asString())) {
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
                    return BRHTTPHelper.handleError(SC_BAD_REQUEST, null, baseRequest, response);
                }
                JSONObject obj = new JSONObject(reqBody);
                String authText = obj.getString(KEY_PROMPT);
                continuation = ContinuationSupport.getContinuation(request);
                continuation.suspend(response);
                globalBaseRequest = baseRequest;
                AuthManager.getInstance().authPrompt(context, authText, "", false, false, new BRAuthCompletion() {
                    @Override
                    public void onComplete() {
                        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                            @Override
                            public void run() {
                                JSONObject obj = new JSONObject();
                                try {
                                    obj.put(KEY_AUTHENTICATED, true);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                if (continuation != null) {
                                    APIClient.BRResponse resp = new APIClient.BRResponse(obj.toString().getBytes(), SC_OK, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8);
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
                                    obj.put(KEY_AUTHENTICATED, false);
                                } catch (JSONException e) {
                                    e.printStackTrace();
                                }
                                APIClient.BRResponse resp = new APIClient.BRResponse(obj.toString().getBytes(), SC_OK,  BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8);
                                BRHTTPHelper.handleSuccess(resp, globalBaseRequest, (HttpServletResponse) continuation.getServletResponse());
                                cleanUp();
                            }
                        });

                    }
                });

            } catch (JSONException e) {
                e.printStackTrace();
                Log.e(TAG, "handle: Failed to parse Json request body: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(SC_BAD_REQUEST, "failed to parse json", baseRequest, response);
            }
        } else if (target.startsWith(PATH_BASE + PATH_CURRENCIES)) {
            JSONArray arr = getCurrencyData(context);
            if (arr.length() == 0) {
                BRReportsManager.reportBug(new IllegalArgumentException("_wallet/currencies created an empty json"));
                return BRHTTPHelper.handleError(SC_INTERNAL_SERVER_ERROR, "Failed to create json", baseRequest, response);
            }
            APIClient.BRResponse resp = new APIClient.BRResponse(arr.toString().getBytes(), SC_OK,  BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8);
            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
        } else if (target.startsWith(PATH_BASE + PATH_TRANSACTION)) {
            String reqBody = null;
            try {
                reqBody = new String(IOUtils.toByteArray(request.getInputStream()));
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (Utils.isNullOrEmpty(reqBody)) {
                Log.e(TAG, "handle: reqBody is empty: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(SC_BAD_REQUEST, null, baseRequest, response);
            }

            try {
                JSONObject obj = new JSONObject(reqBody);
                sendTx(context, obj);

                continuation = ContinuationSupport.getContinuation(request);
                continuation.suspend(response);
                globalBaseRequest = baseRequest;

                return true;
            } catch (JSONException e) {
                e.printStackTrace();
            }

            return BRHTTPHelper.handleError(SC_INTERNAL_SERVER_ERROR, "Invalid json request", baseRequest, response);

        } else if (target.startsWith(PATH_BASE + PATH_ADDRESSES)) {
            String iso = target.substring(target.lastIndexOf("/") + 1);
            BaseWalletManager walletManager = WalletsMaster.getInstance().getWalletByIso(context, iso);
            if (walletManager == null) {
                return BRHTTPHelper.handleError(SC_INTERNAL_SERVER_ERROR, "Invalid iso for address: " + iso, baseRequest, response);
            }
            String address = getLegacyAddress(context, walletManager);
            JSONObject obj = new JSONObject();
            try {
                obj.put(KEY_CURRENCY, walletManager.getCurrencyCode());
                obj.put(KEY_ADDRESS, address);
            } catch (JSONException e) {
                e.printStackTrace();
            }

            APIClient.BRResponse resp = new APIClient.BRResponse(obj.toString().getBytes(), SC_OK, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8);
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
            toAddress = obj.getString(KEY_TO_ADDRESS);
            toDescription = obj.getString(KEY_TO_DESCRIPTION);
            currency = obj.getString(KEY_CURRENCY);
            JSONObject amount = obj.getJSONObject(KEY_AMOUNT);
            numerator = amount.getString(KEY_NUMERATOR);
            denominator = amount.getString(KEY_DENOMINATOR);
            txCurrency = amount.getString(KEY_CURRENCY);
        } catch (JSONException e) {
            e.printStackTrace();
        }
        Log.e(TAG, "sendTx: " + String.format("address (%s), description (%s), currency (%s), numerator (%s), denominator(%s), txCurrency(%s)",
                toAddress, toDescription, currency, numerator, denominator, txCurrency));

        if (Utils.isNullOrEmpty(toAddress) || Utils.isNullOrEmpty(toDescription) || Utils.isNullOrEmpty(currency)
                || Utils.isNullOrEmpty(numerator) || Utils.isNullOrEmpty(denominator) || Utils.isNullOrEmpty(txCurrency)) {
            return;
        }

        final BaseWalletManager wm = WalletsMaster.getInstance().getWalletByIso(app, currency);
        String addr = wm.undecorateAddress(toAddress);
        if (Utils.isNullOrEmpty(addr)) {
            BRDialog.showSimpleDialog(app, "Failed to create tx for exchange!", "Address is empty");
            return;
        }
        final CryptoRequest item = new CryptoRequest.Builder().setAddress(addr).setAmount(new BigDecimal(numerator)).build();
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

    private JSONArray getCurrencyData(Context app) {
        JSONArray arr = new JSONArray();
        List<BaseWalletManager> list = WalletsMaster.getInstance().getAllWallets(app);
        for (BaseWalletManager walletManager : list) {
            JSONObject obj = new JSONObject();
            try {
                obj.put(KEY_ID, walletManager.getCurrencyCode());
                obj.put(KEY_TICKER, walletManager.getCurrencyCode());
                obj.put(KEY_NAME, walletManager.getName());

                //Colors
                JSONArray colors = new JSONArray();
                colors.put(walletManager.getUiConfiguration().getStartColor());
                colors.put(walletManager.getUiConfiguration().getEndColor());

                obj.put(KEY_COLORS, colors);

                JSONObject balance = new JSONObject();

                BigDecimal rawBalance = walletManager.getBalance();
                String denominator = walletManager.getDenominator();
                balance.put(KEY_CURRENCY, walletManager.getCurrencyCode());
                balance.put(KEY_NUMERATOR, rawBalance.toPlainString());
                balance.put(KEY_DENOMINATOR, denominator);

                //Fiat balance
                JSONObject fiatBalance = new JSONObject();
                fiatBalance.put(KEY_CURRENCY, BRSharedPrefs.getPreferredFiatIso(app));
                fiatBalance.put(KEY_NUMERATOR, walletManager.getFiatBalance(app).multiply(new BigDecimal(DOLLAR_IN_CENTS)).toPlainString());
                fiatBalance.put(KEY_DENOMINATOR, String.valueOf(DOLLAR_IN_CENTS));

                //Exchange
                JSONObject exchange = new JSONObject();
                exchange.put(KEY_CURRENCY, BRSharedPrefs.getPreferredFiatIso(app));
                exchange.put(KEY_NUMERATOR, walletManager.getFiatExchangeRate(app).multiply(new BigDecimal(DOLLAR_IN_CENTS)).toPlainString());
                exchange.put(KEY_DENOMINATOR, String.valueOf(DOLLAR_IN_CENTS));

                obj.put(KEY_BALANCE, balance);
                obj.put(KEY_FIAT_BALANCE, fiatBalance);
                obj.put(KEY_EXCHANGE, exchange);
                arr.put(obj);
            } catch (JSONException e) {
                e.printStackTrace();
            }

        }
        return arr;
    }

    private static void finalizeTx(final boolean succeed, final String hash) {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    if (!succeed || Utils.isNullOrEmpty(hash)) {
                        try {
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(SC_INTERNAL_SERVER_ERROR);
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
                        result.put(KEY_HASH, hash);
                        result.put(KEY_TRANSMITTED, true);
                    } catch (JSONException e) {
                        e.printStackTrace();
                    }

                    try {
                        continuation.getServletResponse().setContentType(BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8);
                        continuation.getServletResponse().setCharacterEncoding(StandardCharsets.UTF_8.name());
                        continuation.getServletResponse().getWriter().print(result.toString());
                        Log.d(TAG, "finalizeTx: finished with writing to the response: " + result);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "sendBitIdResponse Failed to send json: ", e);
                    }
                    ((HttpServletResponse) continuation.getServletResponse()).setStatus(SC_OK);
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
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(SC_UNAUTHORIZED);
                        } catch (IOException e) {
                            Log.e(TAG, "sendBitIdResponse: failed to send error 401: ", e);
                            e.printStackTrace();
                        }
                        return;
                    }
                    if (restJson == null || restJson.isNull(KEY_SIGNATURE)) {
                        Log.e(TAG, "sendBitIdResponse: WARNING restJson is null: " + restJson);
                        try {
                            ((HttpServletResponse) continuation.getServletResponse()).sendError(SC_INTERNAL_SERVER_ERROR, "json malformed or null");
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
                        continuation.getServletResponse().setContentType(BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8);
                        continuation.getServletResponse().setCharacterEncoding(StandardCharsets.UTF_8.name());
                        continuation.getServletResponse().getWriter().print(restJson);
                        Log.d(TAG, "sendBitIdResponse: finished with writing to the response: " + restJson);
                    } catch (Exception e) {
                        e.printStackTrace();
                        Log.e(TAG, "sendBitIdResponse Failed to send json: ", e);
                    }
                    ((HttpServletResponse) continuation.getServletResponse()).setStatus(SC_OK);
                } finally {
                    cleanUp();
                }
            }
        });

    }

    private static void cleanUp() {
        if (globalBaseRequest != null) {
            globalBaseRequest.setHandled(true);
        }
        if (continuation != null) {
            continuation.complete();
        }
        continuation = null;
        globalBaseRequest = null;
    }

    private static String getLegacyAddress(Context context, BaseWalletManager walletManager) {
        return walletManager instanceof WalletBitcoinManager
                ? ((WalletBitcoinManager) walletManager).getWallet().getLegacyAddress().stringify()
                : walletManager.getReceiveAddress(context).stringify();
    }
}