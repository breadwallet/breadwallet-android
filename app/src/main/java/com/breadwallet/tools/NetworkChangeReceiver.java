package com.breadwallet.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = NetworkChangeReceiver.class.getName();
    private RelativeLayout networkErrorBar;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final MainActivity app = MainActivity.app;
        final ConnectivityManager connMgr = (ConnectivityManager) context
                .getSystemService(Context.CONNECTIVITY_SERVICE);
        networkErrorBar = (RelativeLayout) app.findViewById(R.id.main_internet_status_bar);
        @SuppressWarnings("deprecation") final android.net.NetworkInfo wifi = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_WIFI);

        @SuppressWarnings("deprecation") final android.net.NetworkInfo mobile = connMgr
                .getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

        if (!wifi.isAvailable() && !mobile.isAvailable()) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.VISIBLE);
                    ((BreadWalletApp) app.getApplication()).showCustomToast(app, app.getString(R.string.no_internet_connection),
                            500, Toast.LENGTH_SHORT);
                }
            });

            Log.d(TAG, "Network Not Available ");
        } else {
            networkErrorBar.setVisibility(View.GONE);
            Log.d(TAG, "Network Available ");
        }
    }


}