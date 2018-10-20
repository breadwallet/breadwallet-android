package com.breadwallet.wallet.util;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Handler;
import android.text.format.DateUtils;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.SendManager;
import com.breadwallet.tools.threads.ImportPrivKeyTask;
import com.breadwallet.tools.threads.PaymentProtocolTask;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.BaseBitcoinWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBchManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;

import org.eclipse.jetty.http.HttpScheme;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static com.breadwallet.presenter.activities.WalletActivity.EXTRA_CRYPTO_REQUEST;

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

    public static synchronized boolean processRequest(Context app, String url, BaseWalletManager walletManager) {
        Log.d(TAG, "processRequest -> " + url);
        if (app == null) {
            Log.e(TAG, "processRequest: app is null");
            return false;
        }
        if (url == null) {
            Log.e(TAG, "processRequest: url is null");
            return false;
        }

        if (ImportPrivKeyTask.trySweepWallet(app, url, walletManager) || tryBreadUrl(app, url)) {
            return true;
        }

        CryptoRequest requestObject = parseRequest(app, url);

        if (requestObject == null) {
            BRDialog.showSimpleDialog(app, app.getString(R.string.JailbreakWarnings_title), app.getString(R.string.Send_invalidAddressTitle));
            return false;
        }
        if (requestObject.isPaymentProtocol()) {
            return tryPaymentRequest(requestObject);
        } else if (requestObject.address != null) {
            return tryCryptoUrl(requestObject, app);
        } else {
            BRDialog.showSimpleDialog(app, app.getString(R.string.JailbreakWarnings_title), app.getString(R.string.Send_remoteRequestError));
            return false;
        }
    }

    private static void pushUrlEvent(Uri u) {
        Map<String, String> attr = new HashMap<>();
        attr.put("scheme", u == null ? "null" : u.getScheme());
        attr.put("host", u == null ? "null" : u.getHost());
        attr.put("path", u == null ? "null" : u.getPath());
        BREventManager.getInstance().pushEvent("send.handleURL", attr);
    }

    public static boolean isCryptoUrl(Context app, String url) {
        if (Utils.isNullOrEmpty(url)) {
            return false;
        }
        if (BRCoreKey.isValidBitcoinBIP38Key(url) || BRCoreKey.isValidBitcoinPrivateKey(url))
            return true;

        CryptoRequest requestObject = parseRequest(app, url);
        // return true if the request is valid url and has param: r or param: address
        // return true if it is a valid bitcoinPrivKey
        return (requestObject != null && (requestObject.isPaymentProtocol() || requestObject.hasAddress()));
    }


    public static CryptoRequest parseRequest(Context app, String str) {
        if (str == null || str.isEmpty()) return null;
        CryptoRequest obj = new CryptoRequest();

        String tmp = cleanUrl(str);

        Uri u = Uri.parse(tmp);
        String scheme = u.getScheme();

        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);

        if (scheme == null) {
            scheme = wm.getScheme();
            obj.iso = wm.getIso();

        } else {
            if (scheme.equalsIgnoreCase(HttpScheme.HTTP.asString()) || scheme.equalsIgnoreCase(HttpScheme.HTTPS.asString())) {
                return null;
            }
            List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(app).getAllWallets(app));
            for (BaseWalletManager walletManager : list) {
                if (scheme.equalsIgnoreCase(walletManager.getScheme())) {
                    obj.iso = walletManager.getIso();
                    break;
                }
            }
        }

        obj.scheme = scheme;

        String schemeSpecific = u.getSchemeSpecificPart();
        if (schemeSpecific.startsWith("//")) {
            // Fix invalid bitcoin uri
            schemeSpecific = schemeSpecific.substring(2);
        }

        u = Uri.parse(scheme + "://" + schemeSpecific);

        String host = u.getHost();
        if (host != null) {
            String trimmedHost = host.trim();
            if (obj.iso.equalsIgnoreCase("bch"))
                trimmedHost = scheme + ":" + trimmedHost; //bitcoin cash has the scheme attached to the address
            String addrs = wm.undecorateAddress(trimmedHost);
            if (!Utils.isNullOrEmpty(addrs) && wm.isAddressValid(addrs)) {
                obj.address = addrs;
            }
        }
        String query = u.getQuery();
        pushUrlEvent(u);
        if (query == null) {
            return obj;
        }
        String[] params = query.split("&");
        for (String s : params) {
            String[] keyValue = s.split("=", 2);
            if (keyValue.length != 2) {
                continue;
            }
            if (keyValue[0].trim().equals("amount")) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                    obj.amount = WalletBitcoinManager.getInstance(app).getSmallestCryptoForCrypto(app, bigDecimal);
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                // ETH payment request amounts are called `value`
            } else if (keyValue[0].trim().equals("value")) {
                BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                obj.value = WalletEthManager.getInstance(app).getSmallestCryptoForCrypto(app, bigDecimal);
            } else if (keyValue[0].trim().equals("label")) {
                obj.label = keyValue[1].trim();
            } else if (keyValue[0].trim().equals("message")) {
                obj.message = keyValue[1].trim();
            } else if (keyValue[0].trim().startsWith("req")) {
                obj.req = keyValue[1].trim();
            } else if (keyValue[0].trim().startsWith("r")) {
                obj.r = keyValue[1].trim();
            }
        }
        return obj;
    }

    private static boolean tryBreadUrl(Context context, String url) {
        if (Utils.isNullOrEmpty(url)) {
            return false;
        }

        String tmp = cleanUrl(url);

        Uri u = Uri.parse(tmp);
        String scheme = u.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase("bread")) {
            String schemeSpecific = u.getSchemeSpecificPart();
            if (schemeSpecific != null && schemeSpecific.startsWith("//")) {
                // Fix invalid bitcoin uri
                schemeSpecific = schemeSpecific.substring(2);
            }

            u = Uri.parse(scheme + "://" + schemeSpecific);

            BaseWalletManager wm = WalletsMaster.getInstance(context).getCurrentWallet(context);

            String host = u.getHost();
            if (Utils.isNullOrEmpty(host)) {
                return false;
            }

            switch (host) {
                case "scanqr":
                    UiUtils.openScanner((Activity) context, BRConstants.SCANNER_REQUEST);
                    break;
                case "addressList":
                    //todo implement
                    break;
                case "address":
                    BRClipboardManager.putClipboard(context, wm.decorateAddress(wm.getAddress(context)));

                    break;
            }
            return true;
        }
        return false;

    }

    private static boolean tryPaymentRequest(CryptoRequest requestObject) {
        String theURL = null;
        String url = requestObject.r;
        synchronized (LOCK_OBJECT) {
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

    private static boolean tryCryptoUrl(final CryptoRequest requestObject, final Context context) {
        if (requestObject == null || requestObject.address == null || requestObject.address.isEmpty())
            return false;
        final BaseWalletManager wallet = WalletsMaster.getInstance(context).getCurrentWallet(context);
        if (requestObject.iso != null && !requestObject.iso.equalsIgnoreCase(wallet.getIso())) {
            if (!(WalletsMaster.getInstance(context).isIsoErc20(context, wallet.getIso()) && requestObject.iso.equalsIgnoreCase("ETH"))) {
                BRDialog.showCustomDialog(context, context.getString(R.string.Alert_error),
                        String.format(context.getString(R.string.Send_invalidAddressMessage), wallet.getName()), context.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismiss();
                            }
                        }, null, null, 0);
                return true; //true since it's a crypto url but different iso than the currently chosen one
            } //  else ->   //allow tokens to scan ETH so continue ..
        }

        BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
            @Override
            public void run() {
                BRSharedPrefs.putCurrentWalletIso(context, requestObject.iso);
                Intent newIntent = new Intent(context, WalletActivity.class);
                newIntent.putExtra(EXTRA_CRYPTO_REQUEST, requestObject);
                context.startActivity(newIntent);
            }
        });
        return true;

    }

    public static Uri createCryptoUrl(Context app, BaseWalletManager wm, String
            address, BigDecimal cryptoAmount, String label, String message, String rURL) {
        String iso = wm.getIso();
        Uri.Builder builder = new Uri.Builder();
        String walletScheme = wm.getScheme();
        if (address.contains(":")) {
            address = address.split(":")[1];
        }
        builder = builder.scheme(walletScheme);
        if (!Utils.isNullOrEmpty(address)) {
            builder = builder.appendPath(address);
        }
        if (cryptoAmount.compareTo(BigDecimal.ZERO) != 0) {
            if (iso.equalsIgnoreCase(WalletEthManager.ETH_CURRENCY_CODE)) {
                BigDecimal amount = WalletEthManager.getInstance(app).getCryptoForSmallestCrypto(app, cryptoAmount);
                builder = builder.appendQueryParameter("value", amount.toPlainString());
            } else if (iso.equalsIgnoreCase(WalletBitcoinManager.BITCOIN_CURRENCY_CODE) || iso.equalsIgnoreCase(WalletBchManager.BITCASH_CURRENCY_CODE)) {
                BigDecimal amount = WalletBitcoinManager.getInstance(app).getCryptoForSmallestCrypto(app, cryptoAmount);
                builder = builder.appendQueryParameter("amount", amount.toPlainString());
            } else {
                throw new RuntimeException("URI not supported for: " + iso);
            }
        }
        if (label != null && !label.isEmpty()) {
            builder = builder.appendQueryParameter("label", label);
        }
        if (message != null && !message.isEmpty()) {
            builder = builder.appendQueryParameter("message", message);
        }
        if (rURL != null && !rURL.isEmpty()) {
            builder = builder.appendQueryParameter("r", rURL);
        }

        return Uri.parse(builder.build().toString().replace("/", ""));

    }

    private static String cleanUrl(String url) {
        return url.trim().replaceAll("\n", "").replaceAll(" ", "%20");
    }

}
