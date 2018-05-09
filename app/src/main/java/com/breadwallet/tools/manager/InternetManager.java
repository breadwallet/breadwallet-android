package com.breadwallet.tools.manager;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.util.Log;

import java.util.ArrayList;
import java.util.List;


/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
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

public class InternetManager extends BroadcastReceiver {

    private static final String TAG = InternetManager.class.getName();

    private static List<ConnectionReceiverListener> mConnectionReceiverListeners;
    private static InternetManager mInstance;

    private InternetManager() {
        mConnectionReceiverListeners = new ArrayList<>();
    }

    public synchronized static InternetManager getInstance() {
        if (mInstance == null) {
            mInstance = new InternetManager();
        }
        return mInstance;
    }

    public static void registerConnectionReceiver(Context context, ConnectionReceiverListener connectionReceiverListener) {
        IntentFilter intentFilter = new IntentFilter(ConnectivityManager.CONNECTIVITY_ACTION);
        context.getApplicationContext().registerReceiver(InternetManager.getInstance(), intentFilter);

        addConnectionListener(connectionReceiverListener);
    }

    public static void unregisterConnectionReceiver(Context context, ConnectionReceiverListener connectionReceiverListener) {
        removeConnectionListener(connectionReceiverListener);
        context.getApplicationContext().unregisterReceiver(InternetManager.getInstance());
    }

    private static void addConnectionListener(ConnectionReceiverListener listener) {
        if (!mConnectionReceiverListeners.contains(listener)) {
            mConnectionReceiverListeners.add(listener);
        }
    }

    private static void removeConnectionListener(ConnectionReceiverListener listener) {
        if (mConnectionReceiverListeners.contains(listener)) {
            mConnectionReceiverListeners.remove(listener);
        }
    }

    @Override
    public void onReceive(final Context context, final Intent intent) {
        boolean connected = false;
        if (intent.getAction().equals(ConnectivityManager.CONNECTIVITY_ACTION)) {
            NetworkInfo networkInfo = intent.getParcelableExtra(ConnectivityManager.EXTRA_NETWORK_INFO);
            if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.CONNECTED) {
                connected = true;
//                WalletsMaster.getInstance(context).getCurrentWallet(context).getPeerManager().connect();
                Log.e(TAG, "onReceive: core connecting");
            } else if (networkInfo != null && networkInfo.getDetailedState() == NetworkInfo.DetailedState.DISCONNECTED) {

                connected = false;
            }

            BREventManager.getInstance().pushEvent(connected ? "reachability.isReachble" : "reachability.isNotReachable");
            for (ConnectionReceiverListener listener : mConnectionReceiverListeners) {
                listener.onConnectionChanged(connected);
            }
            Log.e(TAG, "onReceive: " + connected);
        }
    }

    public boolean isConnected(Context app) {
        if (app == null) return false;
        ConnectivityManager cm = (ConnectivityManager) app.getApplicationContext().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();
    }


    public interface ConnectionReceiverListener {
        void onConnectionChanged(boolean isConnected);
    }

}