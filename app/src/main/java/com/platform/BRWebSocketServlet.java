//package com.platform;
//
//import android.util.Log;
//
//import org.eclipse.jetty.websocket.servlet.WebSocketServlet;
//import org.eclipse.jetty.websocket.servlet.WebSocketServletFactory;
//
//import javax.servlet.annotation.WebServlet;
//
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
//@SuppressWarnings("serial")
//@WebServlet(name = "Geo Socket", urlPatterns = { "/_geosocket" })
//public class BRWebSocketServlet extends WebSocketServlet {
//    private static final String TAG = BRWebSocketServlet.class.getName();
//    @Override
//    public void configure(WebSocketServletFactory factory) {
//        Log.e(TAG, "configure: ");
//        // register MyEchoSocket as the WebSocket to create on Upgrade
//
//        // set a 10 second timeout
//        factory.getPolicy().setIdleTimeout(10000);
//
//        factory.register(GeoSocket.class);
//    }
//}