package com.platform;

import android.util.Log;

import org.eclipse.jetty.websocket.api.Session;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketClose;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketConnect;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketError;
import org.eclipse.jetty.websocket.api.annotations.OnWebSocketMessage;
import org.eclipse.jetty.websocket.api.annotations.WebSocket;

import java.io.IOException;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/19/17.
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

@WebSocket
public class BRGeoWebSocketHandler {
    public static final String TAG = BRGeoWebSocketHandler.class.getName();

    @OnWebSocketClose
    public void onClose(int statusCode, String reason) {
        Log.d(TAG, "GeoSocketClosed: statusCode=" + statusCode + ", reason=" + reason);
        GeoLocationManager.getInstance().stopGeoSocket();
    }

    @OnWebSocketError
    public void onError(Throwable t) {
        Log.e(TAG, "GeoSocketError: " + t.getMessage());
        GeoLocationManager.getInstance().stopGeoSocket();
    }

    @OnWebSocketConnect
    public void onConnect(Session session) {
        Log.d(TAG, "GeoSocketConnected: " + session.getRemoteAddress().getAddress());
        GeoLocationManager.getInstance().startGeoSocket(session);
    }

    @OnWebSocketMessage
    public void onMessage(String message) {
        Log.d(TAG, "GeoSocketMessage: " + message);
    }
}
