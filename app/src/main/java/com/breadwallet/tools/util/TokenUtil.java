/**
 * BreadWallet
 * <p/>
 * Created by Jade Byfield <jade@breadwallet.com> on 9/13/2018.
 * Copyright (c) 2018 breadwallet LLC
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

package com.breadwallet.tools.util;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.util.ArrayList;

import okhttp3.Request;

public final class TokenUtil {

    private static final String TAG = TokenUtil.class.getSimpleName();

    private static final String ENDPOINT_CURRENCIES = "/currencies";
    private static final String ENDPOINT_CURRENCIES_SALE_ADDRESS = "/currencies?saleAddress=";
    private static final String FIELD_CODE = "code";
    private static final String FIELD_NAME = "name";
    private static final String FIELD_SCALE = "scale";
    private static final String FIELD_CONTRACT_ADDRESS = "contract_address";
    private static final String FIELD_IS_SUPPORTED = "is_supported";
    private static final String FIELD_SALE_ADDRESS = "sale_address";
    private static final String FIELD_CONTRACT_INITIAL_VALUE = "contract_initial_value";
    private static final String FIELD_COLORS = "colors";
    private static final String TOKEN_ICONS_WHITE_NO_BACKGROUND = "white-no-bg";
    private static final String TOKEN_ICONS_WHITE_SQUARE_BACKGROUND = "white-square-bg";
    private static final int START_COLOR_INDEX = 0;
    private static final int END_COLOR_INDEX = 1;

    // TODO: In DROID-878 fix this so we don't have to store this mTokenItems... (Should be stored in appropriate wallet.)
    private static ArrayList<TokenItem> mTokenItems = new ArrayList<>();

    private TokenUtil() {
    }

    /**
     * This method can either fetch the list of supported tokens, or fetch a specific token by saleAddress
     * Request the list of tokens we support from the /currencies endpoint
     *
     * @param context     The Context of the caller
     * @param saleAddress Optional sale address value if we are looking for a specific token response.
     */
    public static ArrayList<TokenItem> getTokenList(final Context context, final String saleAddress) {
        ArrayList<TokenItem> tokenItems = new ArrayList<>();
        final String tokenUrl;

        if (Utils.isNullOrEmpty(saleAddress)) {
            tokenUrl = BRConstants.HTTPS_PROTOCOL + BreadApp.HOST + ENDPOINT_CURRENCIES;
        } else {
            tokenUrl = BRConstants.HTTPS_PROTOCOL + BreadApp.HOST + ENDPOINT_CURRENCIES_SALE_ADDRESS + saleAddress;
        }

        Request request = new Request.Builder()
                .url(tokenUrl)
                .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON)
                .header(BRConstants.HEADER_ACCEPT, BRConstants.HEADER_VALUE_ACCEPT).get().build();

        APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, true);

        // Iterate over the token list and announce each token to Core
        try {
            JSONArray tokenListArray = new JSONArray(response.getBodyText());
            WalletEthManager ethWalletManager = WalletEthManager.getInstance(context);

            for (int i = 0; i < tokenListArray.length(); i++) {

                JSONObject tokenObject = tokenListArray.getJSONObject(i);
                String address = "";
                String name = "";
                String symbol = "";
                String startColor = "";
                String endColor = "";
                String contractInitialValue = "";
                int decimals = 0;

                if (tokenObject.has(FIELD_CONTRACT_ADDRESS)) {
                    address = tokenObject.getString(FIELD_CONTRACT_ADDRESS);
                }

                if (tokenObject.has(FIELD_NAME)) {
                    name = tokenObject.getString(FIELD_NAME);
                }

                if (tokenObject.has(FIELD_CODE)) {
                    symbol = tokenObject.getString(FIELD_CODE);
                }

                if (tokenObject.has(FIELD_SCALE)) {
                    decimals = tokenObject.getInt(FIELD_SCALE);
                }

                if (tokenObject.has(FIELD_CONTRACT_INITIAL_VALUE)) {
                    contractInitialValue = tokenObject.getString(FIELD_CONTRACT_INITIAL_VALUE);
                }

                if (!Utils.isNullOrEmpty(address) && !Utils.isNullOrEmpty(name) && !Utils.isNullOrEmpty(symbol)) {

                    ethWalletManager.node.announceToken(address, symbol, name, "", decimals, null, null, 0);

                    // Keep a local reference to the token list, so that we can make token symbols to their
                    // gradient colors in WalletListAdapter
                    TokenItem item = new TokenItem(address, symbol, name, null);

                    if (tokenObject.has(FIELD_COLORS)) {
                        JSONArray colorsArray = tokenObject.getJSONArray(FIELD_COLORS);
                        item.setStartColor((String) colorsArray.get(START_COLOR_INDEX));
                        item.setEndColor((String) colorsArray.get(END_COLOR_INDEX));
                    }
                    item.setContractInitialValue(contractInitialValue);
                    tokenItems.add(item);
                }
            }

        } catch (JSONException e) {
            Log.e(TAG, "Error parsing token list response from server " + e);
        }

        return tokenItems;
    }

    public static void getTokenList(final Context context) {
        mTokenItems = getTokenList(context, null);
    }

    public static String getTokenIconPath(Context context, String symbol, boolean withBackground) {
        String bundleResource = APIClient.getInstance(context).getExtractedPath(context, APIClient.TOKEN_ASSETS_BUNDLE_NAME, null);
        String iconsDirectory;

        if (!withBackground) {
            iconsDirectory = TOKEN_ICONS_WHITE_NO_BACKGROUND;
        } else {
            iconsDirectory = TOKEN_ICONS_WHITE_SQUARE_BACKGROUND;
        }

        File directory = new File(bundleResource);
        if (directory.exists() && directory.isDirectory()) {
            for (File file : directory.listFiles()) {
                if (file.getName().equalsIgnoreCase(iconsDirectory)) {
                    if (file.isDirectory()) {
                        for (File iconFile : file.listFiles()) {
                            if (iconFile.getName().contains(symbol.toLowerCase())) {
                                return iconFile.getAbsolutePath();
                            }
                        }
                    }
                }
            }
        }

        return "";
    }

    public static String getTokenStartColor(String currencyCode) {
        for (TokenItem token : mTokenItems) {
            if (token.symbol.equalsIgnoreCase(currencyCode)) {
                return token.getStartColor();
            }
        }

        return "";
    }

    public static String getTokenEndColor(String currencyCode) {
        for (TokenItem token : mTokenItems) {
            if (token.symbol.equalsIgnoreCase(currencyCode)) {
                return token.getEndColor();
            }
        }

        return "";
    }
}
