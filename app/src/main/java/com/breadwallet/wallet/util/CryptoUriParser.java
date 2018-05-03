package com.breadwallet.wallet.util;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.BRCoreKey;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.CryptoRequest;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.SendManager;
import com.breadwallet.tools.threads.ImportPrivKeyTask;
import com.breadwallet.tools.threads.PaymentProtocolTask;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URLDecoder;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

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
    private static final Object lockObject = new Object();

    public static synchronized boolean processRequest(Context app, String url, BaseWalletManager walletManager) {
        Log.d(TAG, "processRequest -> " + url);
        if (url == null) {
            Log.e(TAG, "processRequest: url is null");
            return false;
        }

        if (ImportPrivKeyTask.trySweepWallet(app, url, walletManager)) return true;

        if (tryBreadUrl(app, url)) return true; //see if it's a bread url

        CryptoRequest requestObject = parseRequest(app, url);

        if (requestObject == null) {
            if (app != null) {
                BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                        app.getString(R.string.Send_invalidAddressTitle), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, null, 0);
            }
            return false;
        }
        if (requestObject.isPaymentProtocol()) {
            return tryPaymentRequest(requestObject);
        } else if (requestObject.address != null) {
            return tryCryptoUrl(requestObject, app);
        } else {
            if (app != null) {
                BRDialog.showCustomDialog(app, app.getString(R.string.JailbreakWarnings_title),
                        app.getString(R.string.Send_remoteRequestError), app.getString(R.string.Button_ok), null, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismissWithAnimation();
                            }
                        }, null, null, 0);
            }
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
        if (Utils.isNullOrEmpty(url)) return false;
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

        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");

        Uri u = Uri.parse(tmp);
        String scheme = u.getScheme();

        BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);

        if (scheme == null) {
            scheme = wm.getScheme(app);
            obj.iso = wm.getIso(app);

        } else {
            List<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(app).getAllWallets(app));
            for (BaseWalletManager walletManager : list) {
                if (scheme.equalsIgnoreCase(walletManager.getScheme(app))) {
                    obj.iso = walletManager.getIso(app);
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
            String addrs = wm.undecorateAddress(app, trimmedHost);
            if (!Utils.isNullOrEmpty(addrs) && wm.isAddressValid(addrs)) {
                obj.address = addrs;
            }
        }
        String query = u.getQuery();
        pushUrlEvent(u);
        if (query == null) return obj;
        String[] params = query.split("&");
        for (String s : params) {
            String[] keyValue = s.split("=", 2);
            if (keyValue.length != 2)
                continue;
            if (keyValue[0].trim().equals("amount")) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                    obj.amount = bigDecimal.multiply(new BigDecimal("100000000"));
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
                // ETH payment request amounts are called `value`
            } else if (keyValue[0].trim().equals("value")) {
                BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                obj.value = bigDecimal;
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

    private static boolean tryBreadUrl(Context app, String url) {
        if (Utils.isNullOrEmpty(url)) return false;
        String tmp = url.trim().replaceAll("\n", "").replaceAll(" ", "%20");

        Uri u = Uri.parse(tmp);
        String scheme = u.getScheme();
        if (scheme != null && scheme.equalsIgnoreCase("bread")) {
            String schemeSpecific = u.getSchemeSpecificPart();
            if (schemeSpecific != null && schemeSpecific.startsWith("//")) {
                // Fix invalid bitcoin uri
                schemeSpecific = schemeSpecific.substring(2);
            }

            u = Uri.parse(scheme + "://" + schemeSpecific);

            BaseWalletManager wm = WalletsMaster.getInstance(app).getCurrentWallet(app);

            String host = u.getHost();
            if (Utils.isNullOrEmpty(host)) return false;

            switch (host) {
                case "scanqr":
                    BRAnimator.openScanner((Activity) app, BRConstants.SCANNER_REQUEST);
                    break;
                case "addressList":
                    //todo implement
                    break;
                case "address":
                    BRClipboardManager.putClipboard(app, wm.decorateAddress(app, BRSharedPrefs.getReceiveAddress(app, wm.getIso(app))));

                    break;
            }

            return true;
        }
        return false;

    }

    private static boolean tryPaymentRequest(CryptoRequest requestObject) {
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

    private static boolean tryCryptoUrl(final CryptoRequest requestObject, Context context) {
        final Activity app;
        if (context instanceof Activity) {
            app = (Activity) context;
        } else {
            Log.e(TAG, "tryCryptoUrl: " + "app isn't activity: " + context.getClass().getSimpleName());
            BRReportsManager.reportBug(new NullPointerException("app isn't activity: " + context.getClass().getSimpleName()));
            return false;
        }
        if (requestObject == null || requestObject.address == null || requestObject.address.isEmpty())
            return false;
        BaseWalletManager wallet = WalletsMaster.getInstance(app).getCurrentWallet(app);
        if (requestObject.iso != null && !requestObject.iso.equalsIgnoreCase(wallet.getIso(app))) {
            if (!(WalletsMaster.getInstance(app).isIsoErc20(app, wallet.getIso(app)) && requestObject.iso.equalsIgnoreCase("ETH"))) {
                BRDialog.showCustomDialog(app, app.getString(R.string.Alert_error),
                        String.format(app.getString(R.string.Send_invalidAddressMessage), wallet.getName(app)), app.getString(R.string.AccessibilityLabels_close), null, new BRDialogView.BROnClickListener() {
                            @Override
                            public void onClick(BRDialogView brDialogView) {
                                brDialogView.dismiss();
                            }
                        }, null, null, 0);
                return true; //true since it's a crypto url but different iso than the currently chosen one
            } //  else ->   //allow tokens to scan ETH so continue ..
        }

        if (requestObject.amount == null || requestObject.amount.compareTo(new BigDecimal(0)) == 0) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BRAnimator.showSendFragment(app, requestObject);
                }
            });
        } else {
            BRAnimator.killAllFragments(app);
            if (Utils.isNullOrEmpty(requestObject.address) || !wallet.isAddressValid(requestObject.address)) {
                BRDialog.showSimpleDialog(app, app.getString(R.string.Send_invalidAddressTitle), "");
                return true;
            }

            SendManager.sendTransaction(app, requestObject, wallet, null);
        }

        return true;

    }

    public static Uri createCryptoUrl(Context app, BaseWalletManager wm, String
            addr, BigDecimal cryptoAmount, String label, String message, String rURL) {
        String iso = wm.getIso(app);
        Uri.Builder builder = new Uri.Builder();
        String walletScheme = wm.getScheme(app);
        String cleanAddress = addr;
        if (addr.contains(":")) {
            cleanAddress = addr.split(":")[1];
        }
        builder = builder.scheme(walletScheme);
        if (!Utils.isNullOrEmpty(cleanAddress))
            builder = builder.appendPath(cleanAddress);
        if (cryptoAmount.compareTo(new BigDecimal(0)) != 0)
            if (iso.equals("ETH")) {

                BigDecimal ethAmount = cryptoAmount.divide(new BigDecimal("1000000000000000000"), 3, RoundingMode.HALF_EVEN);
                builder = builder.appendQueryParameter("value", ethAmount.toPlainString() + "e18");

            } else {
                builder = builder.appendQueryParameter("amount", cryptoAmount.divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE).toPlainString());
            }
        if (label != null && !label.isEmpty())
            builder = builder.appendQueryParameter("label", label);
        if (message != null && !message.isEmpty())
            builder = builder.appendQueryParameter("message", message);
        if (rURL != null && !rURL.isEmpty())
            builder = builder.appendQueryParameter("r", rURL);

        return Uri.parse(builder.build().toString().replace("/", ""));

    }

}
