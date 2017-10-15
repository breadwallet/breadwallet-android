package com.breadwallet.tools.security;

import android.app.Activity;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.entities.PaymentRequestWrapper;
import com.breadwallet.presenter.entities.RequestObject;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.threads.PaymentProtocolTask;
import com.breadwallet.wallet.BRWalletManager;

import java.io.UnsupportedEncodingException;
import java.math.BigDecimal;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URLDecoder;
import java.util.HashMap;
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

public class BitcoinUrlHandler {
    private static final String TAG = BitcoinUrlHandler.class.getName();
    private static final Object lockObject = new Object();

    public static synchronized boolean processRequest(Activity app, String url) {
        if (url == null) {
            Log.e(TAG, "processRequest: url is null");
            return false;
        }

        Map<String, String> attr = new HashMap<>();
        URI uri = null;
        try {
            uri = new URI(url);
        } catch (URISyntaxException e) {
            e.printStackTrace();
        }
        attr.put("scheme", uri == null ? "null" : uri.getScheme());
        attr.put("host", uri == null ? "null" : uri.getHost());
        attr.put("path", uri == null ? "null" : uri.getPath());
        BREventManager.getInstance().pushEvent("send.handleURL", attr);

        RequestObject requestObject = getRequestFromString(url);
        if (BRWalletManager.getInstance().confirmSweep(app, url)) {
            return true;
        }
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
        if (requestObject.r != null) {
            return tryPaymentRequest(requestObject);
        } else if (requestObject.address != null) {
            return tryBitcoinURL(url, app);
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

    public static boolean isBitcoinUrl(String url) {
        RequestObject requestObject = getRequestFromString(url);
        // return true if the request is valid url and has param: r or param: address
        // return true if it is a valid bitcoinPrivKey
        return (requestObject != null && (requestObject.r != null || requestObject.address != null)
                || BRWalletManager.getInstance().isValidBitcoinBIP38Key(url)
                || BRWalletManager.getInstance().isValidBitcoinPrivateKey(url));
    }


    public static RequestObject getRequestFromString(String str) {
        if (str == null || str.isEmpty()) return null;
        RequestObject obj = new RequestObject();

        String tmp = str.trim().replaceAll("\n", "").replaceAll(" ", "%20");

        if (!tmp.startsWith("litecoin://")) {
            if (!tmp.startsWith("litecoin:"))
                tmp = "litecoin://".concat(tmp);
            else
                tmp = tmp.replace("litecoin:", "litecoin://");
        }
        URI uri = URI.create(tmp);

        String host = uri.getHost();
        if (host != null) {
            String addrs = host.trim();
            if (BRWalletManager.validateAddress(addrs)) {
                obj.address = addrs;
            }
        }
        String query = uri.getQuery();
        if (query == null) return obj;
        String[] params = query.split("&");
        for (String s : params) {
            String[] keyValue = s.split("=", 2);
            if (keyValue.length != 2)
                continue;
            if (keyValue[0].trim().equals("amount")) {
                try {
                    BigDecimal bigDecimal = new BigDecimal(keyValue[1].trim());
                    obj.amount = bigDecimal.multiply(new BigDecimal("100000000")).toString();
                } catch (NumberFormatException e) {
                    e.printStackTrace();
                }
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

    private static boolean tryPaymentRequest(RequestObject requestObject) {
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

    private static boolean tryBitcoinURL(final String url, final Activity app) {
        RequestObject requestObject = getRequestFromString(url);
        if (requestObject == null || requestObject.address == null || requestObject.address.isEmpty())
            return false;
        final String[] addresses = new String[1];
        addresses[0] = requestObject.address;

        String amount = requestObject.amount;

        if (amount == null || amount.isEmpty() || new BigDecimal(amount).doubleValue() == 0) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    BRAnimator.showSendFragment(app, url);
                }
            });
        } else {
            if (app != null) {
                BRAnimator.killAllFragments(app);
                BRSender.getInstance().sendTransaction(app, new PaymentItem(addresses, null, new BigDecimal(amount).longValue(), null, true));
            }
        }

        return true;

    }

    public static native PaymentRequestWrapper parsePaymentRequest(byte[] req);

    public static native String parsePaymentACK(byte[] req);

    public static native byte[] getCertificatesFromPaymentRequest(byte[] req, int index);

}
