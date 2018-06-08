package com.platform.middlewares;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.util.Utils;
import com.platform.APIClient;
import com.platform.interfaces.Middleware;

import org.apache.commons.io.IOUtils;

import java.io.IOException;
import java.util.Arrays;
import java.util.Enumeration;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 10/19/16.
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
public class APIProxy implements Middleware {
    public static final String TAG = APIProxy.class.getName();

    private APIClient apiInstance;
    private static final String MOUNT_POINT = "/_api";
    private final String SHOULD_VERIFY_HEADER = "x-should-verify";
    private final String SHOULD_AUTHENTICATE = "x-should-authenticate";
    private final String[] bannedSendHeaders = new String[]{
            SHOULD_VERIFY_HEADER,
            SHOULD_AUTHENTICATE,
            "connection",
            "authorization",
            "host",
            "user-agent"};

    private final String[] bannedReceiveHeaders = new String[]{
            "content-length",
            "content-encoding",
            "connection"};

    public APIProxy() {
        Context app = BreadApp.getBreadContext();
        if (app == null) {
            Log.e(TAG, "APIProxy: app is null!");
        }
        apiInstance = APIClient.getInstance(app);
    }

    @Override
    public boolean handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (!target.startsWith(MOUNT_POINT)) return false;
        Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
        String path = target.substring(MOUNT_POINT.length());
        String queryString = baseRequest.getQueryString();
        if (queryString != null && queryString.length() > 0)
            path += "?" + queryString;
        boolean auth = false;
        Request req = jettyToOkHttpRequest(baseRequest, path, request);
        String authHeader = baseRequest.getHeader(SHOULD_AUTHENTICATE);

        if (authHeader != null && (authHeader.toLowerCase().equals("yes") || authHeader.toLowerCase().equals("true"))) {
            auth = true;
        }

        APIClient.BRResponse res = apiInstance.sendRequest(req, auth);

        Map<String, String> headers = res.getHeaders();
        for (String s : headers.keySet()) {
            if (Arrays.asList(bannedReceiveHeaders).contains(s.toLowerCase())) continue;
            response.addHeader(s, res.getHeaders().get(s));
        }

        try {
            response.setStatus(res.getCode());
            if (!Utils.isNullOrEmpty(res.getContentType()))
                response.setContentType(res.getContentType());
            response.getOutputStream().write(res.getBody());
            baseRequest.setHandled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;

    }

    private Request jettyToOkHttpRequest(org.eclipse.jetty.server.Request baseRequest, String path, HttpServletRequest request) {
        Request req;
        Request.Builder builder = new Request.Builder()
                .url(apiInstance.buildUrl(path));

        Enumeration<String> headerNames = baseRequest.getHeaderNames();
        while (headerNames.hasMoreElements()) {
            String hName = headerNames.nextElement();
            if (Arrays.asList(bannedSendHeaders).contains(hName.toLowerCase())) continue;
            String h = baseRequest.getHeader(hName);
            builder.addHeader(hName, h == null ? "" : h);
        }
        byte[] body = new byte[0];
        try {
            body = IOUtils.toByteArray(request.getInputStream());
        } catch (IOException e) {
            e.printStackTrace();
        }
        String contentType = baseRequest.getContentType();
        RequestBody reqBody = RequestBody.create(contentType == null ? null : MediaType.parse(contentType), body);

        switch (baseRequest.getMethod()) {
            case "GET":
                builder.get();
                break;
            case "DELETE":
                builder.delete();
                break;
            case "POST":
                builder.post(reqBody);
                break;
            case "PUT":
                builder.put(reqBody);
                break;
            default:
                Log.e(TAG, "jettyToOkHttpRequest: WARNING: method: " + baseRequest.getMethod());
                break;
        }

        req = builder.build();
        return req;
    }

}
