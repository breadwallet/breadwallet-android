package com.platform.middlewares.plugins;

import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.breadwallet.app.BreadApp;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.platform.APIClient;
import com.platform.BRHTTPHelper;
import com.platform.LinkBus;
import com.platform.LinkRequestMessage;
import com.platform.interfaces.Plugin;

import org.eclipse.jetty.http.HttpStatus;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
    public static final String TAG = LinkPlugin.class.getName();

    public static final String OPEN_URL_PATH = "/_open_url";
    public static final String OPEN_MAPS_PATH = "/_open_maps";
    public static final String BROWSER_PATH = "/_browser";
    public static final String TEL = "tel";
    public static final String FROM_POINT = "from_point";
    public static final String MAPS_URL_FORMAT = "http://maps.google.com/maps?q=%s&daddr=%s&mode=driving";

    public static boolean hasBrowser;

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (target.startsWith(OPEN_URL_PATH)) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            String url = request.getParameter(BRConstants.URL);

            Context app = BreadApp.getBreadContext();
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(HttpStatus.INTERNAL_SERVER_ERROR_500, "context is null", baseRequest, response);
            }

            if (url != null && url.startsWith(TEL)) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("/", "")));
                app.startActivity(intent);
            } else {
                Log.e(TAG, "handle: could not handle url: " + url);
                BRReportsManager.reportBug(new RuntimeException("could not handle url: " + url));
            }
            APIClient.BRResponse resp = new APIClient.BRResponse(null, HttpStatus.NO_CONTENT_204);
            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
        } else if (target.startsWith(OPEN_MAPS_PATH)) {
            Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
            Context app = BreadApp.getBreadContext();
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(HttpStatus.INTERNAL_SERVER_ERROR_500, "context is null", baseRequest, response);
            }
            String address = baseRequest.getParameter(BRConstants.ADDRESS);
            String fromPoint = baseRequest.getParameter(FROM_POINT);
            if (address == null || fromPoint == null) {
                Log.e(TAG, "handle: bad request: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(HttpStatus.INTERNAL_SERVER_ERROR_500, "bad request", baseRequest, response);
            }
            String uri = String.format(MAPS_URL_FORMAT, fromPoint, address);
            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
            app.startActivity(Intent.createChooser(intent, "Select an application"));
            APIClient.BRResponse resp = new APIClient.BRResponse(null, HttpStatus.NO_CONTENT_204);

            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
        } else if (target.startsWith(BROWSER_PATH)) {
            Context app = BreadApp.getBreadContext();
            if (app == null) {
                Log.e(TAG, "handle: context is null: " + target + " " + baseRequest.getMethod());
                return BRHTTPHelper.handleError(HttpStatus.INTERNAL_SERVER_ERROR_500, "context is null", baseRequest, response);
            }
            switch (request.getMethod()) {
                case BRConstants.GET:
                    // opens the in-app browser for the provided URL
                    Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());

                    if (hasBrowser) {
                        return BRHTTPHelper.handleError(HttpStatus.CONFLICT_409, "Conflict", baseRequest, response);
                    }
                    String getUrl = baseRequest.getParameter(BRConstants.URL);
                    if (Utils.isNullOrEmpty(getUrl)) {
                        return BRHTTPHelper.handleError(HttpStatus.BAD_REQUEST_400, "missing url param", baseRequest, response);
                    }

                    Uri getUri = Uri.parse(getUrl);
                    if (getUri == null || getUri.toString().isEmpty()) {
                        return BRHTTPHelper.handleError(HttpStatus.BAD_REQUEST_400, "failed to escape url", baseRequest, response);
                    }

                    hasBrowser = true;
                    Log.e(TAG, "linkPluginLink: url: " + getUri.toString());
                    LinkBus.INSTANCE.sendMessage(new LinkRequestMessage(getUri.toString(), null));
                    APIClient.BRResponse resp = new APIClient.BRResponse(null, HttpStatus.NO_CONTENT_204);
                    return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
                case BRConstants.POST:
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

                    Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());

                    if (hasBrowser) {
                        return BRHTTPHelper.handleError(HttpStatus.CONFLICT_409, "Conflict", baseRequest, response);
                    }

                    byte[] body = BRHTTPHelper.getBody(request);

                    if (Utils.isNullOrEmpty(body)) {
                        return BRHTTPHelper.handleError(HttpStatus.BAD_REQUEST_400, "missing body", baseRequest, response);
                    }

                    JSONObject json;
                    try {
                        json = new JSONObject(new String(body)); //just check for validity
                    } catch (JSONException e) {
                        e.printStackTrace();
                        Log.e(TAG, "handle: the json is not valid:" + target + " " + baseRequest.getMethod());
                        return BRHTTPHelper.handleError(HttpStatus.BAD_REQUEST_400, "could not deserialize json object ", baseRequest, response);
                    }

                    //check all the params
                    String postUrl;

                    try {
                        postUrl = json.getString(BRConstants.URL);
                        String method = json.getString(BRConstants.METHOD);

                        String closeOn = json.getString(BRConstants.CLOSE_ON);
                        if (Utils.isNullOrEmpty(postUrl) || Utils.isNullOrEmpty(method) || Utils.isNullOrEmpty(closeOn)) {
                            return BRHTTPHelper.handleError(HttpStatus.BAD_REQUEST_400, "malformed json:" + json.toString(), baseRequest, response);
                        }
                    } catch (JSONException e) {
                        e.printStackTrace();
                        return BRHTTPHelper.handleError(HttpStatus.BAD_REQUEST_400, "malformed json:" + json.toString(), baseRequest, response);
                    }

                    hasBrowser = true;
                    LinkBus.INSTANCE.sendMessage(new LinkRequestMessage(postUrl, json.toString()));
                    APIClient.BRResponse brResp = new APIClient.BRResponse(null, HttpStatus.NO_CONTENT_204);
                    return BRHTTPHelper.handleSuccess(brResp, baseRequest, response);
            }
        }
        return false;

    }

}
