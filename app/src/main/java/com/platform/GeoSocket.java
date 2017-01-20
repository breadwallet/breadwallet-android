//package com.platform;
//
//import android.util.Log;
//
//import com.platform.middlewares.plugins.GeoLocationPlugin;
//
//import org.eclipse.jetty.websocket.api.Session;
//import org.eclipse.jetty.websocket.api.WebSocketAdapter;
//
///**
// * BreadWallet
// * <p/>
// * Created by Mihail Gutan on <mihail@breadwallet.com> 1/6/17.
// * Copyright (c) 2017 breadwallet LLC
// * <p/>
// * Permission is hereby granted, free of charge, to any person obtaining a copy
// * of this software and associated documentation files (the "Software"), to deal
// * in the Software without restriction, including without limitation the rights
// * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
// * copies of the Software, and to permit persons to whom the Software is
// * furnished to do so, subject to the following conditions:
// * <p/>
// * The above copyright notice and this permission notice shall be included in
// * all copies or substantial portions of the Software.
// * <p/>
// * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
// * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
// * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
// * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
// * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
// * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
// * THE SOFTWARE.
// */
//
//public class GeoSocket extends WebSocketAdapter {
//    private static final String TAG = GeoSocket.class.getName();
//
//    @Override
//    public void onWebSocketConnect(final Session sess) {
//        super.onWebSocketConnect(sess);
//        Log.e(TAG, "Socket Connected: " + sess);
//        GeoLocationManager.getInstance().startGeoSocket(sess);
//    }
//
//    @Override
//    public void onWebSocketText(String message) {
//        super.onWebSocketText(message);
//        Log.e(TAG, "Received TEXT message: " + message);
//    }
//
//    @Override
//    public void onWebSocketClose(int statusCode, String reason) {
//        super.onWebSocketClose(statusCode, reason);
//        Log.e(TAG, "Socket Closed: [" + statusCode + "] " + reason);
//    }
//
//    @Override
//    public void onWebSocketError(Throwable cause) {
//        super.onWebSocketError(cause);
//        Log.e(TAG, "onWebSocketError: ");
//        cause.printStackTrace(System.err);
//    }
//}