package com.breadwallet.wallet.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.ui.wallet.WalletActivity;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.util.EventUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.ImportPrivKeyTask;
import com.breadwallet.tools.threads.PaymentProtocolTask;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static android.content.Intent.FLAG_ACTIVITY_SINGLE_TOP;
import static com.breadwallet.ui.wallet.WalletActivity.EXTRA_CRYPTO_REQUEST;
import static com.breadwallet.tools.util.BRConstants.BREAD;

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

public class CryptoUriParser {
    private static final String TAG = CryptoUriParser.class.getName();
    private static final Object LOCK_OBJECT = new Object();
    public static final String AMPERSAND = "&";
    public static final String EQUAL = "=";

    public static final String AMOUNT = "amount";
    public static final String VALUE = "value";
    public static final String LABEL = "label";
    public static final String MESSAGE = "message";
    public static final String REQ = "req"; // "req" parameter, whose value is a required variable which are prefixed with a req-.
    public static final String R_URL = "r"; // "r" parameter, whose value is a URL from which a PaymentRequest message should be fetched
    public static final String SCAN_QR = "scanqr";
    public static final String ADDRESS_LIST = "addressList";
    public static final String ADDRESS = "address";

    public static synchronized boolean processRequest(Context context, String url, BaseWalletManager walletManager) {
        Log.d(TAG, "processRequest -> " + url);
        if (context == null) {
            Log.e(TAG, "processRequest: app is null");
            return false;
        }
        if (url == null) {
            Log.e(TAG, "processRequest: url is null");
            return false;
        }

        if (ImportPrivKeyTask.trySweepWallet(context, url, walletManager) || tryBreadUrl(context, url)) {
            return true;
        }

        CryptoRequest requestObject = parseRequest(context, url);

        if (requestObject == null) {
            BRDialog.showSimpleDialog(context, context.getString(R.string.JailbreakWarnings_title), context.getString(R.string.Send_invalidAddressTitle));
            return false;
        }
        if (requestObject.isPaymentProtocol()) {
            return tryPaymentRequest(context, requestObject);
        } else if (requestObject.getAddress() != null) {
            return tryCryptoUrl(requestObject, context);
        } else {
            BRDialog.showSimpleDialog(context, context.getString(R.string.JailbreakWarnings_title), context.getString(R.string.Send_remoteRequestError));
            return false;
        }
    }

    private static void pushUrlEvent(Uri u) {
        Map<String, String> attr = new HashMap<>();
        attr.put("scheme", u == null ? "null" : u.getScheme());
        attr.put("host", u == null ? "null" : u.getHost());
        attr.put("path", u == null ? "null" : u.getPath());
        EventUtils.pushEvent(EventUtils.EVENT_SEND_HANDLE_URL, attr);
    }

    public static boolean isCryptoUrl(Context app, String url) {
        if (Utils.isNullOrEmpty(url)) {
            return false;
        }
        if (BRCoreKey.isValidBitcoinBIP38Key(url) || BRCoreKey.isValidBitcoinPrivateKey(url)) {
            return true;
        }

        CryptoRequest requestObject = parseRequest(app, url);
        if (requestObject == null) {
            return false;
        }

        //if the scheme is present, check if it's a valid one.
        if (!Utils.isNullOrEmpty(requestObject.getScheme())) {
            List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance().getAllWallets(app));
            for (BaseWalletManager walletManager : list) {
                if (requestObject.getScheme().equalsIgnoreCase(walletManager.getScheme())) {
                    return requestObject.isPaymentProtocol() || requestObject.hasAddress();
                }
            }
        }
        return false;

    }

    public static CryptoRequest parseRequest(Context context, String str) {
        if (str == null || str.isEmpty()) {
            return null;
        }
        CryptoRequest.Builder cryptoBuilder = new CryptoRequest.Builder();

        String tmp = cleanUrl(str);

        Uri uri = Uri.parse(tmp);

        String schemeSpecific = uri.getSchemeSpecificPart();
        // Fix invalid bitcoin uri
        if (schemeSpecific.startsWith("//")) {
            schemeSpecific = schemeSpecific.substring(2);
        }
        String scheme = uri.getScheme() == null ? "" : uri.getScheme();
        uri = Uri.parse(scheme + "://" + schemeSpecific);
        BaseWalletManager currentWallet = WalletsMaster.getInstance().getCurrentWallet(context);

        cryptoBuilder = sanitizeData(context, currentWallet, uri, cryptoBuilder);
        if (cryptoBuilder == null) {
            return null;
        }
        BaseWalletManager matchingWallet = WalletsMaster.getInstance().getWalletByIso(context, cryptoBuilder.getCurrencyCode());
        //invalid currency code
        if (matchingWallet == null) {
            return null;
        }
        String address = cryptoBuilder.getAddress();
        if (address != null) {
            String trimmedAddress = address.trim();
            String rawAddress = matchingWallet.undecorateAddress(trimmedAddress);
            if (!Utils.isNullOrEmpty(rawAddress) && matchingWallet.isAddressValid(rawAddress)) {
                cryptoBuilder.setAddress(rawAddress);
            }
        }
        String query = uri.getQuery();
        pushUrlEvent(uri);
        if (query == null) {
            return cryptoBuilder.build();
        }
        String[] params = query.split(AMPERSAND);
        for (String s : params) {
            String[] keyValue = s.split(EQUAL, 2);
            if (keyValue.length != 2) {
                continue;
            }
            if (keyValue[0].trim().equals(AMOUNT)) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                    cryptoBuilder.setAmount(WalletBitcoinManager.getInstance(context).getSmallestCryptoForCrypto(context, bigDecimal));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                // ETH payment request amounts are called `value`
            } else if (keyValue[0].trim().equals(VALUE)) {
                BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                cryptoBuilder.setAmount(WalletEthManager.getInstance(context).getSmallestCryptoForCrypto(context, bigDecimal));
            } else if (keyValue[0].trim().equals(LABEL)) {
                cryptoBuilder.setLabel(keyValue[1].trim());
            } else if (keyValue[0].trim().equals(MESSAGE)) {
                cryptoBuilder.setMessage(keyValue[1].trim());
            } else if (keyValue[0].trim().startsWith(REQ)) {
                cryptoBuilder.setReqUrl(keyValue[1].trim());
            } else if (keyValue[0].trim().startsWith(R_URL)) {
                cryptoBuilder.setRUrl(keyValue[1].trim());
            }
        }
        return cryptoBuilder.build();
    }

    private static CryptoRequest.Builder sanitizeData(Context context, BaseWalletManager currentWallet, Uri uri, CryptoRequest.Builder builder) {
        String address = uri.getHost();
        builder.setAddress(address);
        WalletEthManager walletEthManager = WalletEthManager.getInstance(context);
        if (Utils.isNullOrEmpty(uri.getScheme())) {
            WalletBitcoinManager walletBitcoinManager = WalletBitcoinManager.getInstance(context);
            WalletBchManager walletBchManager = WalletBchManager.getInstance(context);
            String potentialBchAddress = walletBchManager.undecorateAddress(walletBchManager.getScheme() + ":" + address);
            if (walletBitcoinManager.isAddressValid(address)) {
                builder.setCurrencyCode(BaseBitcoinWalletManager.BITCOIN_CURRENCY_CODE);
                builder.setScheme(walletBitcoinManager.getScheme());
            } else if (!Utils.isNullOrEmpty(potentialBchAddress)) {
                builder.setCurrencyCode(BaseBitcoinWalletManager.BITCASH_CURRENCY_CODE);
                builder.setScheme(walletBchManager.getScheme());
                builder.setAddress(potentialBchAddress);
            } else if (walletEthManager.isAddressValid(address)) {
                builder.setCurrencyCode(WalletEthManager.ETH_CURRENCY_CODE);
                builder.setScheme(walletEthManager.getScheme());
            }
        } else {
            builder = builder.setScheme(uri.getScheme());
            List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance().getAllWallets(context));
            for (BaseWalletManager walletManager : list) {
                if (uri.getScheme().equalsIgnoreCase(walletManager.getScheme())) {
                    builder.setCurrencyCode(walletManager.getCurrencyCode());
                    break;
                }
            }
        }
        if (Utils.isNullOrEmpty(builder.getCurrencyCode())) {
            return null;
        }
        if (builder.getCurrencyCode().equalsIgnoreCase(WalletEthManager.ETH_CURRENCY_CODE)) {
            boolean isCurrentWalletErc20 = WalletsMaster.getInstance().isCurrencyCodeErc20(context, currentWallet.getCurrencyCode());
            if (!isCurrentWalletErc20) {
                builder.setCurrencyCode(WalletEthManager.ETH_CURRENCY_CODE);
                builder.setScheme(walletEthManager.getScheme());
            } else {
                //else if the current wallet is erc20 then we need to scan a ETH address but still use the erc20 wallet to send funds.
                builder.setCurrencyCode(currentWallet.getCurrencyCode());
                //no scheme for erc20 wallets.
            }
        }
        return builder;
    }

    private static boolean tryBreadUrl(Context context, String url) {
        if (Utils.isNullOrEmpty(url)) {
            return false;
        }

        String tmp = cleanUrl(url);

        Uri u = Uri.parse(tmp);
        String scheme = u.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase(BREAD)) {
            String schemeSpecific = u.getSchemeSpecificPart();
            if (schemeSpecific != null && schemeSpecific.startsWith("//")) {
                // Fix invalid bitcoin uri
                schemeSpecific = schemeSpecific.substring(2);
            }

            u = Uri.parse(scheme + "://" + schemeSpecific);

            BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(context);

            String host = u.getHost();
            if (Utils.isNullOrEmpty(host)) {
                return false;
            }

            switch (host) {
                case SCAN_QR:
                    UiUtils.openScanner((Activity) context);
                    break;
                case ADDRESS_LIST:
                    //todo implement
                    break;
                case ADDRESS:
                    BRClipboardManager.putClipboard(context, wm.decorateAddress(wm.getAddress(context)));
                    break;
            }
            return true;
        }
        return false;

    }

    private static boolean tryPaymentRequest(Context context, CryptoRequest requestObject) {
        String theURL = null;
        String url = requestObject.getRUrl();
        synchronized (LOCK_OBJECT) {
            try {
                theURL = URLDecoder.decode(url, StandardCharsets.UTF_8.name());
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                return false;
            }
            new PaymentProtocolTask(context).execute(theURL, requestObject.getLabel());
        }
        return true;
    }

    private static boolean tryCryptoUrl(final CryptoRequest requestObject, final Context context) {
        if (requestObject == null || requestObject.getAddress() == null || requestObject.getAddress().isEmpty())
            return false;
        BRSharedPrefs.putCurrentWalletCurrencyCode(context, requestObject.getCurrencyCode());
        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                BRSharedPrefs.putCurrentWalletCurrencyCode(context, requestObject.getCurrencyCode());
                Intent newIntent = new Intent(context, WalletActivity.class);
                newIntent.addFlags(FLAG_ACTIVITY_SINGLE_TOP);
                newIntent.putExtra(EXTRA_CRYPTO_REQUEST, requestObject);
                context.startActivity(newIntent);
            }
        });
        return true;
    }

    /**
     * Generate an Uri for a given CryptoRequest including the wallet schema.
     * @param context       Context were was called.
     * @param walletManager Wallet manager.
     * @param request       Request information to be included in the Uri.
     * @return  An Uri with the request information.
     */
    public static Uri createCryptoUrl(Context context,
                                      BaseWalletManager walletManager,
                                      CryptoRequest request) {
        String currencyCode = walletManager.getCurrencyCode();
        Uri.Builder builder = new Uri.Builder();
        String walletScheme = walletManager.getScheme();
        String cleanAddress = request.getAddress(false);
        builder = builder.scheme(walletScheme);
        if (!Utils.isNullOrEmpty(cleanAddress)) {
            builder = builder.appendPath(cleanAddress);
        }
        if (request.getAmount() != null && request.getAmount().compareTo(BigDecimal.ZERO) != 0) {
            if (currencyCode.equalsIgnoreCase(WalletEthManager.ETH_CURRENCY_CODE)) {
                BigDecimal amount = WalletEthManager.getInstance(context).getCryptoForSmallestCrypto(context, request.getAmount());
                builder = builder.appendQueryParameter(VALUE, amount.toPlainString());
            } else if (currencyCode.equalsIgnoreCase(WalletBitcoinManager.BITCOIN_CURRENCY_CODE) || currencyCode.equalsIgnoreCase(WalletBchManager.BITCASH_CURRENCY_CODE)) {
                BigDecimal amount = WalletBitcoinManager.getInstance(context).getCryptoForSmallestCrypto(context, request.getAmount());
                builder = builder.appendQueryParameter(AMOUNT, amount.toPlainString());
            } else {
                throw new RuntimeException("URI not supported for: " + currencyCode);
            }
        }
        if (!Utils.isNullOrEmpty(request.getLabel())) {
            builder = builder.appendQueryParameter(LABEL, request.getLabel());
        }
        if (!Utils.isNullOrEmpty(request.getMessage())) {
            builder = builder.appendQueryParameter(MESSAGE, request.getMessage());
        }
        if (!Utils.isNullOrEmpty(request.getRUrl())) {
            builder = builder.appendQueryParameter(R_URL, request.getRUrl());
        }

        return Uri.parse(builder.build().toString().replace("/", ""));

    }

    private static String cleanUrl(String url) {
        return url.trim().replaceAll("\n", "").replaceAll(" ", "%20");
    }

}
