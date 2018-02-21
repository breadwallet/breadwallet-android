package com.platform;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import io.digibyte.DigiByte;
import io.digibyte.tools.threads.BRExecutor;
import io.digibyte.tools.util.Utils;
import com.platform.interfaces.Middleware;
import com.platform.interfaces.Plugin;
import com.platform.middlewares.APIProxy;
import com.platform.middlewares.HTTPFileMiddleware;
import com.platform.middlewares.HTTPIndexMiddleware;
import com.platform.middlewares.HTTPRouter;
import com.platform.middlewares.plugins.CameraPlugin;
import com.platform.middlewares.plugins.GeoLocationPlugin;
import com.platform.middlewares.plugins.KVStorePlugin;
import com.platform.middlewares.plugins.LinkPlugin;
import com.platform.middlewares.plugins.WalletPlugin;

import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.server.Server;
import org.eclipse.jetty.server.handler.AbstractHandler;
import org.eclipse.jetty.server.handler.HandlerCollection;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Set;

import javax.servlet.ServletException;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;


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

    private static Set<Middleware> middlewares;
    private static Server server;
    public static final int PORT = 31120;
    public static final String URL_EA = "http://localhost:" + PORT + "/ea";
    public static final String URL_BUY = "http://localhost:" + PORT + "/buy";
    public static final String URL_SUPPORT = "http://localhost:" + PORT + "/support";
    public static ServerMode mode;

    public enum ServerMode {
        SUPPORT,
        BUY,
        EA
    }

    public HTTPServer() {
        init();
    }

    private static void init() {
        middlewares = new LinkedHashSet<>();
        server = new Server(PORT);
        try {
            server.dump(System.err);
        } catch (IOException e) {
            e.printStackTrace();
        }

        HandlerCollection handlerCollection = new HandlerCollection();

        WebSocketHandler wsHandler = new WebSocketHandler() {
            @Override
            public void configure(WebSocketServletFactory factory) {
                factory.register(BRGeoWebSocketHandler.class);
            }
        };

        ServerHandler serverHandler = new ServerHandler();
        handlerCollection.addHandler(serverHandler);
        handlerCollection.addHandler(wsHandler);

        server.setHandler(handlerCollection);

        setupIntegrations();

    }

    public synchronized static void startServer() {
        Log.d(TAG, "startServer");
        try {
            if (server != null && server.isStarted()) {
                return;
            }
            if (server == null) init();
            server.start();
            server.join();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
    }

    public static void stopServer() {
        Log.d(TAG, "stopServer");
        try {
            if (server != null)
                server.stop();
        } catch (Exception e) {
            e.printStackTrace();
        }
        server = null;
    }

    public boolean isStarted() {
        return server != null && server.isStarted();
    }

    private static class ServerHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response)
                throws IOException, ServletException {
            boolean success;
            success = dispatch(target, baseRequest, request, response);
            if (!success) {
                Log.e(TAG, "handle: NO MIDDLEWARE HANDLED THE REQUEST: " + target);
            }
        }
    }

    private static boolean dispatch(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        Log.d(TAG, "TRYING TO HANDLE: " + target + " (" + request.getMethod() + ")");
        final Context app = DigiByte.getBreadContext();
        boolean result = false;
        if (target.equalsIgnoreCase("/_close")) {
            if (app != null) {
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        ((Activity) app).onBackPressed();
                    }
                });
                return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);
            }
            return true;
        } else if (target.toLowerCase().startsWith("/_email")) {
            Log.e(TAG, "dispatch: uri: " + baseRequest.getUri().toString());
            String address = Uri.parse(baseRequest.getUri().toString()).getQueryParameter("address");
            Log.e(TAG, "dispatch: address: " + address);
            if (Utils.isNullOrEmpty(address)) {
                return BRHTTPHelper.handleError(400, "no address", baseRequest, response);
            }

            Intent email = new Intent(Intent.ACTION_SEND);
            email.putExtra(Intent.EXTRA_EMAIL, new String[]{address});

            //need this to prompts email client only
            email.setType("message/rfc822");

            app.startActivity(Intent.createChooser(email, "Choose an Email client :"));
            return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);
        } else if (target.toLowerCase().startsWith("/didload")) {
            return BRHTTPHelper.handleSuccess(200, null, baseRequest, response, null);
        }

        for (Middleware m : middlewares) {
            result = m.handle(target, baseRequest, request, response);
            if (result) {
                String className = m.getClass().getName().substring(m.getClass().getName().lastIndexOf(".") + 1);
                if (!className.contains("HTTPRouter"))
                    Log.d(TAG, "dispatch: " + className + " succeeded:" + request.getRequestURL());
                break;
            }
        }
        return result;
    }

    private static void setupIntegrations() {
        // proxy api for signing and verification
        APIProxy apiProxy = new APIProxy();
        middlewares.add(apiProxy);

        // http router for native functionality
        HTTPRouter httpRouter = new HTTPRouter();
        middlewares.add(httpRouter);

        // basic file server for static assets
        HTTPFileMiddleware httpFileMiddleware = new HTTPFileMiddleware();
        middlewares.add(httpFileMiddleware);

        // middleware to always return index.html for any unknown GET request (facilitates window.history style SPAs)
        HTTPIndexMiddleware httpIndexMiddleware = new HTTPIndexMiddleware();
        middlewares.add(httpIndexMiddleware);

        // geo plugin provides access to onboard geo location functionality
        Plugin geoLocationPlugin = new GeoLocationPlugin();
        httpRouter.appendPlugin(geoLocationPlugin);

        // camera plugin
        Plugin cameraPlugin = new CameraPlugin();
        httpRouter.appendPlugin(cameraPlugin);

        // wallet plugin provides access to the wallet
        Plugin walletPlugin = new WalletPlugin();
        httpRouter.appendPlugin(walletPlugin);

        // link plugin which allows opening links to other apps
        Plugin linkPlugin = new LinkPlugin();
        httpRouter.appendPlugin(linkPlugin);

        // kvstore plugin provides access to the shared replicated kv store
        Plugin kvStorePlugin = new KVStorePlugin();
        httpRouter.appendPlugin(kvStorePlugin);
    }

}
