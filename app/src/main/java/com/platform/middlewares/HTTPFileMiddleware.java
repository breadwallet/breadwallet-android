package com.platform.middlewares;

import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.util.TypesConverter;
import com.platform.interfaces.Middleware;


import org.apache.commons.io.FileUtils;
import org.apache.commons.io.FilenameUtils;

import java.io.File;
import java.io.IOException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.platform.APIClient.BUNDLES;
import static com.platform.APIClient.extractedFolder;

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
            baseRequest.setHandled(true);
            return true;
        }

        String requestedFile = MainActivity.app.getFilesDir() + "/" + BUNDLES + "/" + extractedFolder + target;
        File temp = new File(requestedFile);
        if (!temp.exists()) {
            return false;
        }
        Log.e(TAG, "handling: " + target + " " + baseRequest.getMethod());
        boolean modified = true;
        StringBuilder sb = new StringBuilder();
        try {
            MessageDigest digest = MessageDigest.getInstance("MD5");
            byte[] md5 = digest.digest(TypesConverter.long2byteArray(temp.lastModified()));

            for (byte b : md5) {
                sb.append(String.format("%02X", b));
            }
            response.setHeader("ETag", sb.toString());
        } catch (NoSuchAlgorithmException e) {
            e.printStackTrace();
            throw new RuntimeException("should not throw");
        }
        if (sb.toString().isEmpty()) throw new RuntimeException("can't happen");

        // if the client sends an if-none-match header, determine if we have a newer version of the file
        String etag = request.getHeader("if-none-match");
        if (etag != null && etag.equalsIgnoreCase(sb.toString())) modified = false;
        response.setContentType(detectContentType(temp));
//        if (modified) {
            try {
                response.setStatus(200);
                response.getOutputStream().write(FileUtils.readFileToByteArray(temp));
                baseRequest.setHandled(true);
            } catch (IOException e) {
                e.printStackTrace();
            }
            return true;
//        } else {
//            response.setStatus(304);
//            return true;
//        }
        //todo finish the implementation
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
