package com.platform.middlewares.plugins;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.tools.util.Utils;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Plugin;

import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import timber.log.Timber;

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
public class LinkPlugin implements Plugin {
    public static boolean hasBrowser;

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (target.startsWith("/_open_url")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
            String url = request.getParameter("url");

            Context app = BreadApp.getBreadContext();
            if (app == null) {
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }

            if (url != null && url.startsWith("tel")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("/", "")));
                app.startActivity(intent);
            } else {
                Timber.e(new RuntimeException("could not handle url: " + url));
            }

            return BRHTTPHelper.handleSuccess(204, null, baseRequest, response, null);
        } else if (target.startsWith("/_open_maps")) {
            Timber.d("handling: " + target + " " + baseRequest.getMethod());
            Context app = BreadApp.getBreadContext();
            if (app == null) {
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            String address = baseRequest.getParameter("address");
            String fromPoint = baseRequest.getParameter("from_point");
            if (address == null || fromPoint == null) {
                Timber.i("handle: bad request: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "bad request", baseRequest, response);
            }
            String uri = "http://maps.google.com/maps?q=" + fromPoint + "&daddr=" + address + "&mode=driving";
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
            app.startActivity(Intent.createChooser(intent, "Select an application"));
            return BRHTTPHelper.handleSuccess(204, null, baseRequest, response, null);
        } else if (target.startsWith("/_browser")) {
            Context app = BreadApp.getBreadContext();
            if (app == null) {
                Timber.i("handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
            }
            switch (request.getMethod()) {
                case "GET":
                    // opens the in-app browser for the provided URL
                    Timber.d("handling: " + target + " " + baseRequest.getMethod());

                    if (hasBrowser)
                        return BRHTTPHelper.handleError(409, "Conflict", baseRequest, response);
                    String getUrl = baseRequest.getParameter("url");
                    if (Utils.isNullOrEmpty(getUrl))
                        return BRHTTPHelper.handleError(400, "missing url param", baseRequest, response);

                    Uri getUri = Uri.parse(getUrl);
                    if (getUri == null || getUri.toString().isEmpty())
                        return BRHTTPHelper.handleError(400, "failed to escape url", baseRequest, response);

                    hasBrowser = true;
                    Intent getInt = new Intent(app, WebViewActivity.class);
                    getInt.putExtra(WebViewActivity.URL_EXTRA, getUri.toString());
                    app.startActivity(getInt);
                    ((Activity) app).overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                    return BRHTTPHelper.handleSuccess(204, null, baseRequest, response, null);
                case "POST":
                    // opens a browser with a customized request object
                    // params:
                    //  {
                    //    "url": "http://myirl.com",
                    //    "method": "POST",
                    //    "body": "stringified request body...",
                    //    "headers": {"X-Header": "Blerb"}
                    //    "closeOn": "http://someurl",
                    //  }
                    // Only the "url" parameter is required. If only the "url" parameter
                    // is supplied the request acts exactly like the GET /_browser resource above
                    //
                    // When the "closeOn" parameter is provided the web view will automatically close
                    // if the browser navigates to this exact URL. It is useful for oauth redirects
                    // and the like

                    Timber.d("handling: " + target + " " + baseRequest.getMethod());

                    if (hasBrowser)
                        return BRHTTPHelper.handleError(409, "Conflict", baseRequest, response);

                    byte[] body = BRHTTPHelper.getBody(request);

                    if (Utils.isNullOrEmpty(body))
                        return BRHTTPHelper.handleError(400, "missing body", baseRequest, response);

                    JSONObject json;
                    try {
                        json = new JSONObject(new String(body)); //TODO: just check for validity
                    } catch (JSONException e) {
                        Timber.e(e, "handle: the json is not valid: %s %s", target, baseRequest.getMethod());
                        return BRHTTPHelper.handleError(400, "could not deserialize json object ", baseRequest, response);
                    }

                    //check all the params
                    String postUrl;
                    try {
                        postUrl = json.getString("url");
                        String method = json.getString("method");
                        String strBody = json.getString("body");
                        String headers = json.getString("headers");
                        String closeOn = json.getString("closeOn");
                        if (Utils.isNullOrEmpty(postUrl) || Utils.isNullOrEmpty(method) ||
                                Utils.isNullOrEmpty(strBody) || Utils.isNullOrEmpty(headers) || Utils.isNullOrEmpty(closeOn))
                            return BRHTTPHelper.handleError(400, "malformed json:" + json.toString(), baseRequest, response);
                    } catch (JSONException e) {
                        Timber.e(e);
                        return BRHTTPHelper.handleError(400, "malformed json:" + json.toString(), baseRequest, response);
                    }

                    hasBrowser = true;
                    Intent postInt = new Intent(app, WebViewActivity.class);
                    postInt.putExtra(WebViewActivity.URL_EXTRA, postUrl);
                    postInt.putExtra(WebViewActivity.JSON_EXTRA, json.toString());
                    app.startActivity(postInt);
                    ((Activity) app).overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
                    return BRHTTPHelper.handleSuccess(204, null, baseRequest, response, null);
            }
        }
        return false;
    }
}
