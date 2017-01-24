package com.platform.middlewares.plugins;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.wallet.BRWalletManager;
import com.platform.interfaces.Plugin;

import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.google.firebase.analytics.FirebaseAnalytics.getInstance;

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

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {

        if (target.startsWith("/_wallet/info")) {
            final MainActivity app = MainActivity.app;
            if (app == null) {
                try {
                    response.sendError(500, "context is null");
                    baseRequest.setHandled(true);
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            BRWalletManager wm = BRWalletManager.getInstance(app);
            JSONObject jsonResp = new JSONObject();
            try {
                jsonResp.put("no_wallet", wm.noWalletForPlatform(app));
                jsonResp.put("watch_only", false);
                jsonResp.put("receive_address", BRWalletManager.getReceiveAddress());
                response.setStatus(200);
                response.getWriter().write(jsonResp.toString());
                baseRequest.setHandled(true);
            } catch (JSONException e) {
                e.printStackTrace();
                try {
                    response.sendError(500, "json error");
                    baseRequest.setHandled(true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            } catch (IOException e) {
                e.printStackTrace();
                try {
                    response.sendError(500, "IO exception: " + e.getMessage());
                    baseRequest.setHandled(true);
                } catch (IOException ex) {
                    ex.printStackTrace();
                }
            }

            return true;
        } else if (target.startsWith("/_wallet/format")) {

            return true;
        }
        return false;
    }
}
