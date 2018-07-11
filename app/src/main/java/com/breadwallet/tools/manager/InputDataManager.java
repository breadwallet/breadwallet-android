package com.breadwallet.tools.manager;

import android.app.Activity;
import android.content.Context;
import android.net.Uri;
import android.util.Log;

import com.breadwallet.protocols.messageexchange.PwbMaster;
import com.breadwallet.protocols.messageexchange.entities.PairingObject;
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
public final class InputDataManager {
    private static final String TAG = InputDataManager.class.getSimpleName();

    private InputDataManager() {
    }

    public static void processQrResult(Context context, String result) {
        if (CryptoUriParser.isCryptoUrl(context, result)) {
            CryptoUriParser.processRequest(context, result,
                    WalletsMaster.getInstance(context).getCurrentWallet(context));
        } else if (BRBitId.isBitId(result)) {
            BRBitId.signBitID(context, result, null);
        } else if (isWalletPair(context, result)) {
            PairingObject pairingObject = PairingObject.parseUriString(result);
            PwbMaster.startPairing(context, pairingObject);
        }
    }

    public static boolean isWalletPair(Context context, String result) {
        if (Utils.isNullOrEmpty(result)) {
            return false;
        }
        Uri uri = Uri.parse(result);
        String path = uri.getPath();
        if (uri.getHost().equalsIgnoreCase(BRConstants.URL_BRD_HOST) && (path.contains(BRConstants.WALLET_PAIR_PATH) || path.contains(BRConstants.WALLET_LINK_PATH))) {
            Log.d(TAG, "isWalletPair: true");
            return true;
        }
        return false;
    }
}
