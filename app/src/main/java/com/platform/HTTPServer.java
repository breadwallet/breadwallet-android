package com.platform;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.crashlytics.android.Crashlytics;
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
import org.eclipse.jetty.util.component.AbstractLifeCycle;
import org.eclipse.jetty.websocket.server.WebSocketHandler;
import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;

import java.io.IOException;
import java.util.LinkedHashSet;
import java.util.Random;
import java.util.Set;

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
public class HTTPServer extends AbstractLifeCycle {
    public static final String TAG = HTTPServer.class.getSimpleName();

    private static final String PLATFORM_BASE_URL = "http://localhost:";
    public static final String URL_BUY = "/buy";
    public static final String URL_TRADE = "/trade";
    public static final String URL_SELL = "/sell";
    public static final String URL_SUPPORT = "/support";
    public static final String URL_REWARDS = "/rewards";
    private static final int MIN_PORT = 8000;
    private static final int MAX_PORT = 49152;
    private static final int MAX_RETRIES = 10;

    private static HTTPServer mInstance;
    private int mPort;
    private Server mServer;
    private static Set<Middleware> middlewares;
    private static OnCloseListener mOnCloseListener;
    private Context mContext; // TODO Inject when implementing dependency injection.

    private HTTPServer() {
    }

    public static synchronized HTTPServer getInstance() {
        if (mInstance == null) {
            mInstance = new HTTPServer();
        }

        return mInstance;
    }

    /**
     * Get base url where the platform is hosted.
     *
     * @return Platform's base url.
     */
    public static String getPlatformBaseUrl() {
        return PLATFORM_BASE_URL + mInstance.mPort;
    }

    /**
     * Get the platform's url for a given endpoint.
     *
     * @param endpoint The platform's endpoint.
     * @return The url where the endpoint is hosted.
     */
    public static String getPlatformUrl(String endpoint) {
        return getPlatformBaseUrl() + endpoint;
    }

    private void init(int port) {
        middlewares = new LinkedHashSet<>();
        mServer = new Server(port);
        try {
            mServer.dump(System.err);
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

        mServer.setHandler(handlerCollection);

        setupIntegrations();
    }

    /**
     * Start the server.
     *
     * @param context Application context.
     */
    public void startServer(Context context) {
        mContext = context;
        try {
            mInstance.start();
        } catch (Exception e) {
            Log.e(TAG, "startServer: Error starting the local server.", e);
            Crashlytics.logException(e);
        }
    }

    public void stopServer() {
        try {
            mInstance.stop();
        } catch (Exception e) {
            Log.e(TAG, "stopServer: Error stopping the local server.", e);
            Crashlytics.logException(e);
        }
    }

    @Override
    protected void doStart() {
        Log.d(TAG, "doStart");
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                int retries = 0;
                while (!doStartServer() && retries < MAX_RETRIES) { // if failed to start the server retry with new port
                    retries++;
                }
                if (retries == MAX_RETRIES) {
                    Log.e(TAG, "doStart: Failed to start local server, MAX_RETRIES reached.");
                }
            }
        });
    }

    @Override
    protected void doStop() {
        Log.d(TAG, "doStop");
        if (mServer != null && !mServer.isStopped() && !mServer.isStopping()) {
            BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                @Override
                public void run() {
                    try {
                        if (mServer != null) {
                            mServer.stop();
                            mServer = null;
                        }
                    } catch (Exception e) {
                        Log.e(TAG, "doStop: Error stopping the local server.", e);
                        Crashlytics.logException(e);
                    }
                }
            });
        }
    }

    /**
     * Attempt to start the server on the current port, returns true if a retry is required.
     *
     * @return True if server was started; false, otherwise.
     */
    private boolean doStartServer() {
        Log.d(TAG, "doStartServer: Starting the server in port: " + mPort);
        try {
            // Get the last port used in case we are restarting the server.
            int port = BRSharedPrefs.getHttpServerPort(mContext);
            if (port < MIN_PORT || port > MAX_PORT) {
                Random rand = new Random();
                port = rand.nextInt((MAX_PORT - MIN_PORT)) + MIN_PORT;
            }

            init(port);
            mServer.start();

            // Save the port for future restarts.
            mPort = port;
            BRSharedPrefs.putHttpServerPort(mContext, port);

            mServer.join();
            Log.d(TAG, "doStartServer: Server started in port " + mPort);
            return true;
        } catch (Exception e) {
            Log.e(TAG, "doStart: Error starting the local server. Trying again on new port.", e);
            Crashlytics.logException(e);
            return false;
        }
    }

    private static class ServerHandler extends AbstractHandler {
        public void handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
            if (!dispatch(target, baseRequest, request, response)) {
                Log.e(TAG, "handle: NO MIDDLEWARE HANDLED THE REQUEST, 404-ing: " + target);
                BRHTTPHelper.handleError(404, "No middleware could handle the request.", baseRequest, response);
            }
        }
    }

    private static boolean dispatch(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        Log.d(TAG, "TRYING TO HANDLE: " + target + " (" + request.getMethod() + ")");
        final Context app = BreadApp.getBreadContext();
        boolean result = false;
        if (target.equalsIgnoreCase(BRConstants.CLOSE)) {
            if (app != null) {
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        if (mOnCloseListener != null) {
                            mOnCloseListener.onClose();
                        } else {
                            ((Activity) app).onBackPressed();
                        }
                    }
                });
                APIClient.BRResponse resp = new APIClient.BRResponse(null, 200);
                return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
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
            APIClient.BRResponse resp = new APIClient.BRResponse(null, 200);
            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
        } else if (target.toLowerCase().startsWith("/didload")) {
            APIClient.BRResponse resp = new APIClient.BRResponse(null, 200);
            return BRHTTPHelper.handleSuccess(resp, baseRequest, response);
        }

        for (Middleware m : middlewares) {
            result = m.handle(target, baseRequest, request, response);
            if (result) {
                String className = m.getClass().getName().substring(m.getClass().getName().lastIndexOf(".") + 1);
                if (!className.contains("HTTPRouter")) {
                    Log.d(TAG, "dispatch: " + className + " succeeded:" + request.getRequestURL());
                }
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

    public int getPort() {
        return mPort;
    }

    public static void setOnCloseListener(OnCloseListener listener) {
        mOnCloseListener = listener;
    }

    public interface OnCloseListener {
        void onClose();
    }

}
