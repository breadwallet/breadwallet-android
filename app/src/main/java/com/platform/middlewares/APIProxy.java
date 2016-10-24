package com.platform.middlewares;

import com.platform.APIClient;
import com.platform.interfaces.Middleware;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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
        apiInstance = APIClient.getInstance();
    }

    @Override
    public boolean handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        return false;
    }
}
