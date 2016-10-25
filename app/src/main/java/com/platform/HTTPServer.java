package com.platform;

import android.util.Log;

import com.platform.interfaces.Middleware;
import com.platform.middlewares.APIProxy;
import com.platform.middlewares.HTTPFileMiddleware;
import com.platform.middlewares.HTTPIndexMiddleware;
import com.platform.middlewares.HTTPRouter;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;

import java.io.IOException;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.Stack;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static com.breadwallet.R.string.request;

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
public class HTTPServer {
    public static final String TAG = HTTPServer.class.getName();

    private Set<Middleware> middlewares;
    private Server server;
    public static final int PORT = 31120;
    public static final String URL_EA = "http://localhost:" + PORT + "/ea";

    public HTTPServer() {
        init();
    }

    private void init() {
        middlewares = new LinkedHashSet<>();
        server = new Server(PORT);
        server.setHandler(new ServerHandler());

        setupIntegrations();
    }

    public void startServer() {
        Log.e(TAG, "startServer");
        try {
            if (server != null && !server.isStarted())
                server.start();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    public void stopServer() {
        Log.e(TAG, "stopServer");
        try {
            if (server != null)
                server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }

    }

    private class ServerHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            Log.e(TAG, "handle: " + target);
            boolean success;
            success = dispatch(target, baseRequest, request, response);
            if (!success) Log.e(TAG, "handle: NO MIDDLEWARE HANDLED THE REQUEST!");
        }

    }

    private boolean dispatch(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        boolean result = false;
        for (Middleware m : middlewares) {
            result = m.handle(target, baseRequest, request, response);
            if (result) {
                Log.e(TAG, "dispatch: " + m.getClass().getName().substring(m.getClass().getName().lastIndexOf(".") + 1) + " succeeded\n" + request.getRequestURL());
                break;
            }
        }
        return result;
    }

    private void setupIntegrations() {
        middlewares.add(new APIProxy());
        middlewares.add(new HTTPRouter());
        middlewares.add(new HTTPFileMiddleware());
        middlewares.add(new HTTPIndexMiddleware());
    }

}
