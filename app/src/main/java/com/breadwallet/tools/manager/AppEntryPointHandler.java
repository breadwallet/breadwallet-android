package com.breadwallet.tools.manager;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.protocols.messageexchange.MessageExchangeService;
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.util.CryptoUriParser;
import com.platform.tools.BRBitId;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/10/18.
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
public final class AppEntryPointHandler {
    /**
     * A utility class used to process QR codes URL links to start our application.
     */
    private AppEntryPointHandler() {
    }

    /**
     * Returns whether the specified QR code result is supported by our application.
     *
     * @param context The context in which we are operating.
     * @param result The QR code result to check.
     * @return Returns true if the specified QR code result is supported by our application; false, otherwise.
     */
    public static boolean isSupportedQRCode(Context context, String result) {
        return CryptoUriParser.isCryptoUrl(context, result) || BRBitId.isBitId(result) || isWalletPairUrl(result);
    }

    public static void processQrResult(final Context context, String result) {
        if (CryptoUriParser.isCryptoUrl(context, result)) {
            // Handle external click with crypto scheme.
            CryptoUriParser.processRequest(context, result,
                    WalletsMaster.getInstance(context).getCurrentWallet(context));
        } else if (BRBitId.isBitId(result)) {
            BRBitId.signBitID(context, result, null);
        } else if (isWalletPairUrl(result)) {
            // Handle pairing with another wallet.
            PairingMetaData pairingMetaData = new PairingMetaData(result);
            MessageExchangeService.enqueueWork(context, MessageExchangeService.createIntent(
                    context,
                    MessageExchangeService.ACTION_REQUEST_TO_PAIR,
                    pairingMetaData));
        }
    }

    /**
     * Processes a deep link into the application.
     *
     * @param context The context in which we are operating.
     * @param intent The intent containing data (URL) for the deep link.
     */
    public static void processDeepLink(final Context context, Intent intent) {
        final Uri data = intent.getData();
        intent.setData(null);
        if (data != null && !data.toString().isEmpty()) {
            if (!WalletsMaster.getInstance(context).isBrdWalletCreated(context)) {
                // Go to intro screen if the wallet is not create yet.
                Intent introIntent = new Intent(context, IntroActivity.class);
                context.startActivity(introIntent);
            } else {
                processQrResult(context, data.toString());
            }
        }
    }

    /**
     * Returns whether the specified URL is a remote wallet linking/pairing URL.
     *
     * @param url The URL to check.
     * @return Returns true if the specified URL is a remote wallet linking/pairing URL; false, otherwise.
     */
    private static boolean isWalletPairUrl(String url) {
        if (Utils.isNullOrEmpty(url)) {
            return false;
        }
        Uri uri = Uri.parse(url);
        if (uri == null) {
            return false;
        }
        String path = uri.getPath();
        String host = uri.getHost();
        if (path == null || host == null) {
            return false;
        }
        return host.equalsIgnoreCase(BRConstants.URL_BRD_HOST)
                && (path.contains(BRConstants.WALLET_PAIR_PATH) || path.contains(BRConstants.WALLET_LINK_PATH));
    }
}
