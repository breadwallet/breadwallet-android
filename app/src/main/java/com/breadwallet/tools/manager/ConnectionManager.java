package com.breadwallet.tools.manager;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

import com.breadwallet.BreadApp;

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

public class ConnectionManager {

    private static final String TAG = ConnectionManager.class.getName();
    public static List<ConnectionReceiverListener> connectionReceiverListeners;

    private static ConnectionManager instance;
    public static final int MY_BACKGROUND_JOB = 0;

    private ConnectionManager() {
        connectionReceiverListeners = new ArrayList<>();

    }

    public static ConnectionManager getInstance() {

        if (instance == null) {
            instance = new ConnectionManager();
        }
        return instance;
    }

    public static void addConnectionListener(ConnectionReceiverListener listener) {
        if (!connectionReceiverListeners.contains(listener))
            connectionReceiverListeners.add(listener);
    }

//    public void startNetworkJob(Context context){
//        JobScheduler js1 =
//                (JobScheduler) context.getSystemService(Context.JOB_SCHEDULER_SERVICE);
//        JobInfo job1 = new JobInfo.Builder(
//                MY_BACKGROUND_JOB,
//                new ComponentName(context, NetworkChangeService.class))
//                .setRequiredNetworkType(JobInfo.NETWORK_TYPE_UNMETERED)
//                .setRequiresCharging(false)
//                .build();
//        js1.schedule(job1);
//
//    }

    public boolean isConnected() {
        ConnectivityManager
                cm = (ConnectivityManager) BreadApp.getInstance()
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null
                && activeNetwork.isConnectedOrConnecting();
    }


    public interface ConnectionReceiverListener {
        void onNetworkConnectionChanged(boolean isConnected);
    }

}