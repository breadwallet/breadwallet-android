package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.activities.intro.IntroActivity;
import com.breadwallet.protocols.messageexchange.MessageExchangeService;
import com.breadwallet.protocols.messageexchange.entities.PairingMetaData;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.ServerBundlesHelper;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.util.CryptoUriParser;
import com.platform.HTTPServer;
import com.platform.tools.BRBitId;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;

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
    private static final String TAG = AppEntryPointHandler.class.getSimpleName();
    private static final String BRD_HOST = "brd.com";
    private static final String BRD_PROTOCOL = "https://";
    private static final String PLATFORM_PATH_PREFIX = "/x/platform/";
    private static final String PLATFORM_DEBUG_PATH_PREFIX = "/x/debug";
    private static final String PLATFORM_URL_FORMAT = "/link?to=%s";
    private static final String PATH_ENCODING = "utf-8";
    private static final String QUERY_PARAM_WEB_BUNDLE = "web_bundle";
    private static final String QUERY_PARAM_BUNDLE_DEBUG_URL = "bundle_debug_url";

    /**
     * A utility class used to process QR codes URL links to start our application.
     */
    private AppEntryPointHandler() {
    }

    /**
     * Returns whether the specified QR code result is supported by our application.
     *
     * @param context The context in which we are operating.
     * @param result  The QR code result to check.
     * @return Returns true if the specified QR code result is supported by our application; false, otherwise.
     */
    public static boolean isSupportedQRCode(Context context, String result) {
        return CryptoUriParser.isCryptoUrl(context, result) || BRBitId.isBitId(result) || isWalletPairUrl(result);
    }

    /**
     * Performs the appropriate processing given a URL parsed from an Intent.
     *
     * @param context The context in which we are operating.
     * @param result The URL that encapsulates the processing to be done.
     */
    private static void processIntentResult(final Context context, String result) {
        if (isDeepLinkPlatformUrl(result)) {
            processPlatformDeepLinkingUrl(context, result);
        } else if (isDeepLinkPlatformDebugUrl(result)) {
            processDeepLinkPlatformDebugUrl(context, result);
        } else if (CryptoUriParser.isCryptoUrl(context, result)) {
            // Handle external click with crypto scheme.
            CryptoUriParser.processRequest(context, result,
                    WalletsMaster.getInstance().getCurrentWallet(context));
        } else if (BRBitId.isBitId(result)) {
            BRBitId.signBitID(context, result, null);
        } else if (isWalletPairUrl(result)) {
            // Handle pairing with another wallet.
            PairingMetaData pairingMetaData = new PairingMetaData(result);
            MessageExchangeService.enqueueWork(context, MessageExchangeService.createIntent(
                    context,
                    MessageExchangeService.ACTION_REQUEST_TO_PAIR,
                    pairingMetaData));
        } else {
            Log.d(TAG, "processIntentResult: unknown url: " + result);
        }
    }

    /**
     * Returns whether the given URL is a deep link to be rendered by the platform.
     *
     * @param url The URL in question.
     * @return  Returns true if the URL is a deep link to be rendered by the platform.
     */
    public static boolean isDeepLinkPlatformUrl(String url) {
        if (!Utils.isNullOrEmpty(url)) {
            Uri uri = Uri.parse(url);
            return BRD_HOST.equalsIgnoreCase(uri.getHost())
                    && !Utils.isNullOrEmpty(uri.getPath())
                    && uri.getPath().startsWith(PLATFORM_PATH_PREFIX);
        }
        return false;
    }

    /**
     * Returns whether the given URL is a deep link encoding a debug setting to be applied.
     *
     * @param url The URL in question.
     * @return Returns true if the URL is a deep link encoding a debug setting to be applied.
     */
    private static boolean isDeepLinkPlatformDebugUrl(String url) {
        if (!Utils.isNullOrEmpty(url)) {
            Uri uri = Uri.parse(url);
            return BRD_HOST.equalsIgnoreCase(uri.getHost())
                    && !Utils.isNullOrEmpty(uri.getPath())
                    && uri.getPath().startsWith(PLATFORM_DEBUG_PATH_PREFIX);
        }
        return false;
    }

    /**
     * Handles routing a platform deep link to an appropriate Activity to be rendered.
     *
     * @param context The context in which we are operating.
     * @param url The URL to be rendered.
     */
    public static void processPlatformDeepLinkingUrl(Context context, String url) {
        Uri uri = Uri.parse(url);
        String platformPath = "/".concat(uri.getPath().replace(PLATFORM_PATH_PREFIX, ""));
        try {
            String encodedPlatformPath = URLEncoder.encode(platformPath, PATH_ENCODING);
            String platformUrl = String.format(PLATFORM_URL_FORMAT, encodedPlatformPath);
            String fullPlatformUrl = HTTPServer.getPlatformBaseUrl() + platformUrl;
            UiUtils.startPlatformBrowser((Activity) context, fullPlatformUrl);
        } catch (UnsupportedEncodingException e) {
            Log.e(TAG, "processPlatformDeepLinkingUrl: ", e);
        }
    }

    /**
     * Handles applying a deep link debug setting and routes to the appropriate landing Activity.
     *
     * @param context The context in which we are operating.
     * @param url The URL encoding the debug setting to be applied.
     */
    private static void processDeepLinkPlatformDebugUrl(Context context, String url) {
        Uri uri = Uri.parse(url);
        String webBundle = uri.getQueryParameter(QUERY_PARAM_WEB_BUNDLE);
        String webBundleUrl = uri.getQueryParameter(QUERY_PARAM_BUNDLE_DEBUG_URL);

        if (!Utils.isNullOrEmpty(webBundleUrl)) {
            ServerBundlesHelper.setWebPlatformDebugURL(context, webBundleUrl);
        } else if (!Utils.isNullOrEmpty(webBundle)) {
            ServerBundlesHelper.setDebugBundle(context, ServerBundlesHelper.Type.WEB, webBundle);
        }

        Intent intent = new Intent(context, HomeActivity.class);
        context.startActivity(intent);
    }

    /**
     * Processes a deep link into the application.
     *
     * @param context The context in which we are operating.
     * @param url     The url for the deep link.
     */
    public static void processDeepLink(final Context context, String url) {
        if (url != null && !url.isEmpty()) {
            if (!WalletsMaster.getInstance().isBrdWalletCreated(context)) {
                // Go to intro screen if the wallet is not create yet.
                Intent introIntent = new Intent(context, IntroActivity.class);
                context.startActivity(introIntent);
            } else {
                processIntentResult(context, url);
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
