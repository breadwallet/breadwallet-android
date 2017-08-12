package com.platform.middlewares;

import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.crypto.CryptoHelper;
import com.breadwallet.tools.util.TypesConverter;
import com.breadwallet.tools.util.Utils;
import com.platform.BRHTTPHelper;
import com.platform.interfaces.Middleware;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;
import org.eclipse.jetty.http.MimeTypes;
import org.eclipse.jetty.server.Response;

import java.io.File;
import java.io.IOException;
import java.io.RandomAccessFile;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Arrays;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.platform.APIClient.extractedFolder;
import okhttp3.Request;

import static android.R.attr.baseline;
import static android.R.attr.handle;
import static com.platform.APIClient.BUNDLES;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 10/17/16.
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
public class HTTPFileMiddleware implements Middleware {
    public static final String TAG = HTTPFileMiddleware.class.getName();


    @Override
    public boolean handle(String target, org.eclipse.jetty.server.Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (target.equals("/")) return false;
        if (target.equals("/favicon.ico")) {
            return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);
        }

        String requestedFile = MainActivity.app.getFilesDir() + "/" + BUNDLES + "/" + extractedFolder + target;
        File temp = new File(requestedFile);
        if (!temp.exists()) {
            return false;
        }
        Log.i(TAG, "handling: " + target + " " + baseRequest.getMethod());
        boolean modified = true;
        byte[] md5 = CryptoHelper.md5(TypesConverter.long2byteArray(temp.lastModified()));
        String hexEtag = Utils.bytesToHex(md5);
        response.setHeader("ETag", hexEtag);

        // if the client sends an if-none-match header, determine if we have a newer version of the file
        String etag = request.getHeader("if-none-match");
        if (etag != null && etag.equalsIgnoreCase(hexEtag)) modified = false;

        byte[] body = null;
        if (modified) {
            try {
                body = FileUtils.readFileToByteArray(temp);
            } catch (IOException e) {
                e.printStackTrace();
            }
            if (body == null) {
                return BRHTTPHelper.handleError(400, "could not read the file", baseRequest, response);
            }
        } else {
            return BRHTTPHelper.handleSuccess(304, null, baseRequest, response, null);
        }
        response.setContentType(detectContentType(temp));


        String rangeString = request.getHeader("range");
        if (!Utils.isNullOrEmpty(rangeString)) {
            // Range header should match format "bytes=n-n,n-n,n-n...". If not, then return 416.
            return handlePartialRequest(baseRequest, response, temp);
        } else {
            return BRHTTPHelper.handleSuccess(200, body, baseRequest, response, null);
        }
    }

    private boolean handlePartialRequest(org.eclipse.jetty.server.Request request, HttpServletResponse response, File file) {
        try {
            String rangeHeader = request.getHeader("range");
            String rangeValue = rangeHeader.trim()
                    .substring("bytes=".length());
            int fileLength = (int) file.length();
            int start, end;
            if (rangeValue.startsWith("-")) {
                end = fileLength - 1;
                start = fileLength - 1
                        - Integer.parseInt(rangeValue.substring("-".length()));
            } else {
                String[] range = rangeValue.split("-");
                start = Integer.parseInt(range[0]);
                end = range.length > 1 ? Integer.parseInt(range[1])
                        : fileLength - 1;
            }
            if (end > fileLength - 1) {
                end = fileLength - 1;
            }
            if (start <= end) {
                int contentLength = end - start + 1;
                response.setHeader("Content-Length", contentLength + "");
                response.setHeader("Content-Range", "bytes " + start + "-"
                        + end + "/" + fileLength);
                byte[] respBody = Arrays.copyOfRange(FileUtils.readFileToByteArray(file), start, contentLength);
                return BRHTTPHelper.handleSuccess(206, respBody, request, response, detectContentType(file));
            }
        } catch (Exception e) {
            e.printStackTrace();
            try {
                request.setHandled(true);
                response.getWriter().write("Invalid Range Header");
                response.sendError(400, "Bad Request");
            } catch (IOException e1) {
                e1.printStackTrace();
            }

            return true;
        }
        return BRHTTPHelper.handleError(500, "unknown error", request, response);
    }

    private String detectContentType(File file) {
        String extension = FilenameUtils.getExtension(file.getAbsolutePath());
        switch (extension) {
            case "ttf":
                return "application/font-truetype";
            case "woff":
                return "application/font-woff";
            case "otf":
                return "application/font-opentype";
            case "svg":
                return "image/svg+xml";
            case "html":
                return "text/html";
            case "png":
                return "image/png";
            case "jpeg":
                return "image/jpeg";
            case "jpg":
                return "image/jpeg";
            case "css":
                return "text/css";
            case "js":
                return "application/javascript";
            case "json":
                return "application/json";
            default:
                break;
        }
        return "application/octet-stream";
    }
}
