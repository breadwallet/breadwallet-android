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

import android.annotation.SuppressLint;
import android.content.Context;
import android.util.Log;

import androidx.annotation.Nullable;
import androidx.core.util.Preconditions;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.model.TokenItem;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import okhttp3.Request;

import static androidx.core.util.Preconditions.checkNotNull;

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
    private static final String FIELD_CURRENCY_ID = "currency_id";
    private static final String FIELD_TYPE = "type";
    private static final String FIELD_ALTERNATE_NAMES = "alternate_names";
    private static final String FIELD_CRYPTOCOMPARE = "cryptocompare";
    private static final String ICON_DIRECTORY_NAME_WHITE_NO_BACKGROUND = "white-no-bg";
    private static final String ICON_DIRECTORY_NAME_WHITE_SQUARE_BACKGROUND = "white-square-bg";
    private static final String ICON_FILE_NAME_FORMAT = "%s.png";
    private static final int START_COLOR_INDEX = 0;
    private static final int END_COLOR_INDEX = 1;
    private static final String TOKENS_FILENAME = "tokens.json";

    private static final String ETHEREUM = "ethereum";
    private static final String ETHEREUM_TESTNET = "ropsten";
    private static final String TESTNET = "testnet";
    private static final String MAINNET = "mainnet";

    // TODO: In DROID-878 fix this so we don't have to store this mTokenItems... (Should be stored in appropriate wallet.)
    private static List<TokenItem> mTokenItems = new ArrayList<>();
    private static Map<String, TokenItem> mTokenMap = new HashMap<>();

    private TokenUtil() {
    }

    /**
     * When the app first starts, fetch our local copy of tokens.json from the resource folder
     *
     * @param context The Context of the caller
     */
    public static void initialize(Context context, Boolean forceLoad) {
        String filePath = context.getFilesDir().getAbsolutePath() + File.separator + TOKENS_FILENAME;
        File tokensFile = new File(filePath);

        if (!tokensFile.exists() || forceLoad) {
            InputStream tokensInputStream = context.getResources().openRawResource(R.raw.tokens);
            BufferedReader bufferedReader = new BufferedReader(new InputStreamReader(tokensInputStream));
            StringBuilder stringBuilder = new StringBuilder();
            String line;

            try {
                while ((line = bufferedReader.readLine()) != null) {
                    stringBuilder.append(line).append('\n');
                }

                // Copy the APK tokens.json to a file on internal storage
                saveTokenListToFile(context, stringBuilder.toString());
                loadTokens(parseJsonToTokenList(context, stringBuilder.toString()));
            } catch (IOException e) {
                Log.e(TAG, "Could not read from resource file at res/raw/tokens.json ", e);
            }
        }
    }

    /**
     * This method can either fetch the list of supported tokens, or fetch a specific token by saleAddress
     * Request the list of tokens we support from the /currencies endpoint
     *
     * @param context  The Context of the caller
     * @param tokenUrl The URL of the endpoint to get the token metadata from.
     */
    private static APIClient.BRResponse fetchTokensFromServer(Context context, String tokenUrl) {
        Request request = new Request.Builder()
                .url(tokenUrl)
                .header(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON_CHARSET_UTF8)
                .header(BRConstants.HEADER_ACCEPT, BRConstants.CONTENT_TYPE_JSON).get().build();

        return APIClient.getInstance(context).sendRequest(request, true);
    }

    /**
     * This method fetches a specific token by saleAddress
     *
     * @param context     The Context of the caller
     * @param saleAddress Optional sale address value if we are looking for a specific token response.
     */
    public static TokenItem getTokenItem(Context context, String saleAddress) {
        APIClient.BRResponse response = fetchTokensFromServer(context,
                APIClient.getBaseURL() + ENDPOINT_CURRENCIES_SALE_ADDRESS + saleAddress);
        if (response != null && !response.getBodyText().isEmpty()) {
            ArrayList<TokenItem> tokenItems = parseJsonToTokenList(context, response.getBodyText());

            // The response in this case should contain exactly 1 token item.
            return tokenItems == null || tokenItems.size() != 1 ? null : tokenItems.get(0);
        }

        return null;
    }

    public static synchronized List<TokenItem> getTokenItems(Context context) {
        if (mTokenItems == null || mTokenItems.isEmpty()) {
            loadTokens(getTokensFromFile(context));
        }
        return mTokenItems;
    }

    /**
     * Return a TokenItem with the given currency code or null if non TokenItem has the currency code.
     *
     * @param currencyCode The currency code of the token we are looking.
     * @return The TokenItem with the given currency code or null.
     */
    public static TokenItem getTokenItemByCurrencyCode(String currencyCode) {
        return mTokenMap.get(currencyCode.toLowerCase());
    }

    public static void fetchTokensFromServer(Context context) {
        APIClient.BRResponse response = fetchTokensFromServer(context,
                APIClient.getBaseURL() + ENDPOINT_CURRENCIES);

        if (response != null && !response.getBodyText().isEmpty()) {
            // Synchronize on the class object since getTokenItems is static and also synchronizes on the class object
            // rather than on an instance of the class.
            synchronized (TokenItem.class) {
                String responseBody = response.getBodyText();

                // Check if the response from the server is valid JSON before trying to save & parse.
                if (Utils.isValidJSON(responseBody)) {
                    saveTokenListToFile(context, responseBody);
                    loadTokens(parseJsonToTokenList(context, responseBody));
                }
            }
        } else {
            Log.e(TAG, "failed to fetch tokens: " + response.getCode());
        }
    }

    // TODO refactor to avoid passing in context.
    private static ArrayList<TokenItem> parseJsonToTokenList(Context context, String jsonString) {
        ArrayList<TokenItem> tokenItems = new ArrayList<>();
        // Iterate over the token list and announce each token to Core.
        try {
            JSONArray tokenListArray = new JSONArray(jsonString);

            for (int i = 0; i < tokenListArray.length(); i++) {
                JSONObject tokenObject = tokenListArray.getJSONObject(i);
                String address = "";
                String name = "";
                String symbol = "";
                String type = "";
                boolean isSupported = true;
                String cryptocompareAlias = null;

                if (tokenObject.has(FIELD_CONTRACT_ADDRESS)) {
                    address = tokenObject.getString(FIELD_CONTRACT_ADDRESS);
                }

                if (tokenObject.has(FIELD_NAME)) {
                    name = tokenObject.getString(FIELD_NAME);
                }

                if (tokenObject.has(FIELD_CODE)) {
                    symbol = tokenObject.getString(FIELD_CODE);
                }

                if (tokenObject.has(FIELD_IS_SUPPORTED)) {
                    isSupported = tokenObject.getBoolean(FIELD_IS_SUPPORTED);
                }

                if (tokenObject.has(FIELD_ALTERNATE_NAMES)) {
                    JSONObject altNameObject = tokenObject.getJSONObject(FIELD_ALTERNATE_NAMES);
                    if (altNameObject.has(FIELD_CRYPTOCOMPARE)) {
                        cryptocompareAlias = altNameObject.getString(FIELD_CRYPTOCOMPARE);
                    }
                }

                if (tokenObject.has(FIELD_TYPE)) {
                    type = tokenObject.getString(FIELD_TYPE);
                }

                if (!Utils.isNullOrEmpty(name) && !Utils.isNullOrEmpty(symbol)) {
                    String startColor = null;
                    String endColor = null;
                    String currencyId = "";
                    if (tokenObject.has(FIELD_COLORS)) {
                        JSONArray colorsArray = tokenObject.getJSONArray(FIELD_COLORS);
                        startColor = (String) colorsArray.get(START_COLOR_INDEX);
                        endColor = (String) colorsArray.get(END_COLOR_INDEX);
                    }

                    if (tokenObject.has(FIELD_CURRENCY_ID)) {
                        currencyId = tokenObject.getString(FIELD_CURRENCY_ID);

                        if (BuildConfig.BITCOIN_TESTNET) {
                            currencyId = currencyId.replace(MAINNET, currencyId.contains(ETHEREUM) ? ETHEREUM_TESTNET : TESTNET);
                        }
                    }

                    TokenItem item = new TokenItem(address, symbol, name, null, isSupported, currencyId, type, startColor, endColor, cryptocompareAlias);

                    tokenItems.add(item);
                }
            }
        } catch (JSONException e) {
            Log.e(TAG, "Error parsing token list response from server:", e);
        }
        return tokenItems;
    }

    private static void saveTokenListToFile(Context context, String jsonResponse) {
        String filePath = context.getFilesDir().getAbsolutePath() + File.separator + TOKENS_FILENAME;
        try {
            FileWriter fileWriter = new FileWriter(filePath);
            fileWriter.write(jsonResponse);
            fileWriter.flush();
            fileWriter.close();
        } catch (IOException e) {
            Log.e(TAG, "Error writing tokens JSON response to tokens.json:", e);
        }
    }

    private static List<TokenItem> getTokensFromFile(Context context) {
        try {
            File tokensFile = new File(context.getFilesDir().getPath() + File.separator + TOKENS_FILENAME);
            FileInputStream fileInputStream = new FileInputStream(tokensFile);
            int size = fileInputStream.available();
            byte[] fileBytes = new byte[size];
            fileInputStream.read(fileBytes);
            fileInputStream.close();

            return parseJsonToTokenList(context, new String(fileBytes));

        } catch (IOException e) {
            Log.e(TAG, "Error reading tokens.json file: ", e);
            return mTokenItems;
        }
    }

    public static String getTokenIconPath(Context context, String currencyCode, boolean withBackground) {
        String bundleResource = ServerBundlesHelper
                .getExtractedPath(context, ServerBundlesHelper.getBundle(ServerBundlesHelper.Type.TOKEN), null);
        String iconDirectoryName = withBackground
                ? ICON_DIRECTORY_NAME_WHITE_SQUARE_BACKGROUND : ICON_DIRECTORY_NAME_WHITE_NO_BACKGROUND;
        String iconFileName = String.format(ICON_FILE_NAME_FORMAT, currencyCode);
        File bundleDirectory = new File(bundleResource);
        if (bundleDirectory.exists() && bundleDirectory.isDirectory()) {
            for (File iconDirectory : bundleDirectory.listFiles()) {
                if (iconDirectory.getName().equalsIgnoreCase(iconDirectoryName) && iconDirectory.isDirectory()) {
                    for (File iconFile : iconDirectory.listFiles()) {
                        if (iconFile.getName().equalsIgnoreCase(iconFileName)) {
                            return iconFile.getAbsolutePath();
                        }
                    }
                }
            }
        }

        return "";
    }

    @SuppressLint("ResourceType")
    public static String getTokenStartColor(String currencyCode, Context context) {
        TokenItem tokenItem = mTokenMap.get(currencyCode.toLowerCase());
        if (tokenItem != null && !tokenItem.getStartColor().isEmpty()) {
            return tokenItem.getStartColor();
        } else {
            return context.getString(R.color.wallet_delisted_token_background);
        }
    }

    @SuppressLint("ResourceType")
    public static String getTokenEndColor(String currencyCode, Context context) {
        TokenItem tokenItem = mTokenMap.get(currencyCode.toLowerCase());
        if (tokenItem != null && !tokenItem.getEndColor().isEmpty()) {
            return tokenItem.getEndColor();
        } else {
            return context.getString(R.color.wallet_delisted_token_background);
        }
    }

    public static boolean isTokenSupported(String symbol) {
        if (mTokenMap.containsKey(symbol.toLowerCase())) {
            return mTokenMap.get(symbol.toLowerCase()).isSupported();
        }
        return true;
    }

    /**
     * Returns the currency code to be used when fetching the exchange rate for the token with the
     * given currency code.
     *
     * @param currencyCode the currency code
     * @return
     */
    public static String getExchangeRateCode(String currencyCode) {
        return mTokenMap.containsKey(currencyCode.toLowerCase())
                ? mTokenMap.get(currencyCode.toLowerCase()).getExchangeRateCurrencyCode() : currencyCode;
    }

    private static void loadTokens(List<TokenItem> tokenItems) {
        mTokenItems = tokenItems;
        mTokenMap.clear();
        for (TokenItem tokenItem : tokenItems) {
            mTokenMap.put(tokenItem.getSymbol().toLowerCase(), tokenItem);
        }
    }

    @Nullable
    public static TokenItem getTokenItemForCurrencyId(String currencyId) {
        checkNotNull(currencyId);
        for (TokenItem item : mTokenItems) {
            if (item.getCurrencyId().equalsIgnoreCase(currencyId)) {
                return item;
            }
        }
        return null;
    }
}
