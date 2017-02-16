package com.platform.middlewares;

import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Middleware;

import junit.framework.Assert;

import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;

import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import okhttp3.Request;
import okhttp3.Response;

import static android.R.attr.path;
import static com.platform.APIClient.BUNDLES;
import static com.platform.APIClient.extractedFolder;

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
public class HTTPIndexMiddleware implements Middleware {
    public static final String TAG = HTTPIndexMiddleware.class.getName();

    @Override
    public boolean handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
        String indexFile = MainActivity.app.getFilesDir() + "/" + BUNDLES + "/" + extractedFolder + "/index.html";

        File temp = new File(indexFile);
        if (!temp.exists()) {
            Log.e(TAG, "handle: FILE DOES NOT EXIST: " + temp.getAbsolutePath());
            return false;
        }

        try {
            byte[] body = FileUtils.readFileToByteArray(temp);
            Assert.assertNotNull(body);
            Assert.assertNotSame(body.length, 0);
            response.setHeader("Content-Length", String.valueOf(body.length));
            return BRHTTPHelper.handleSuccess(200, body, baseRequest, response, "text/html;charset=utf-8");
        } catch (IOException e) {
            e.printStackTrace();
            Log.d(TAG, "handle: error sending response: " + target + " " + baseRequest.getMethod());
            return BRHTTPHelper.handleError(500, null, baseRequest, response);
        }

    }
}
