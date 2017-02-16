package com.platform;

import android.util.Log;

import com.breadwallet.tools.util.Utils;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.util.StringUtil;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 2/16/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class BRHTTPHelper {
    public static final String TAG = BRHTTPHelper.class.getName();

    public static boolean handleError(int err, String errMess, Request req, HttpServletResponse resp) {
        try {
            req.setHandled(true);
            if (Utils.isNullOrEmpty(errMess))
                resp.sendError(err);
            else
                resp.sendError(err, errMess);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
//    return BRHTTPHelper.handleError(500, "context is null", baseRequest, response);
//    return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);

    public static boolean handleSuccess(int code, byte[] body, Request req, HttpServletResponse resp, String contentType) {
        try {
            resp.setStatus(code);
            if(contentType != null && !contentType.isEmpty())
                resp.setContentType(contentType);
            if (body != null)
                resp.getOutputStream().write(body);
            req.setHandled(true);
        } catch (IOException e) {
            e.printStackTrace();
        }
        return true;
    }
}
