package com.breadwallet.presenter.activities.util;

import android.arch.lifecycle.Lifecycle;
import android.arch.lifecycle.LifecycleObserver;
import android.arch.lifecycle.OnLifecycleEvent;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.platform.UserMetricsManager;

public class ApplicationLifecycleObserver implements LifecycleObserver {

    private static final String TAG = ApplicationLifecycleObserver.class.getSimpleName();

    private void sendUserMetricsRequest() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {

                //First, check if we have a wallet ID already stored in SharedPrefs
                String walletId = BRSharedPrefs.getWalletRewardId(BreadApp.getBreadContext());

                // Only make this request if we have a valid wallet ID
                if (walletId != null && !walletId.isEmpty()) {
                    UserMetricsManager.makeUserMetricsRequest(BreadApp.getBreadContext(), UserMetricsManager.METRIC_LAUNCH);
                }
            }
        });
    }

    @OnLifecycleEvent(Lifecycle.Event.ON_CREATE)
    public void onLaunch() {
        Log.d(TAG, "onLaunch");
        sendUserMetricsRequest();

    }

    @OnLifecycleEvent(Lifecycle.Event.ON_START)
    public void onEnterForeground() {
        Log.d(TAG, "onEnterForeground");

        sendUserMetricsRequest();
    }


    @OnLifecycleEvent(Lifecycle.Event.ON_STOP)
    public void onEnterBackground() {
        Log.d(TAG, "onEnterBackground");

    }
}
