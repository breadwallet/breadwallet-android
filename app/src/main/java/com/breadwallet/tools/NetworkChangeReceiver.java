package com.breadwallet.tools;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.util.Log;
import android.view.View;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.wallet.BRPeerManager;

public class NetworkChangeReceiver extends BroadcastReceiver {
    private static final String TAG = NetworkChangeReceiver.class.getName();
    private RelativeLayout networkErrorBar;

    @Override
    public void onReceive(final Context context, final Intent intent) {
        final MainActivity app = MainActivity.app;
        if(app == null) return;
        networkErrorBar = (RelativeLayout) app.findViewById(R.id.main_internet_status_bar);
        final ConnectivityManager connectivityManager = ((ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE));
        boolean isConnected = connectivityManager.getActiveNetworkInfo() != null && connectivityManager.getActiveNetworkInfo().isConnected();
        BRPeerManager.getInstance(context).connect();
        if (!isConnected) {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.VISIBLE);
//                    ((BreadWalletApp) app.getApplication()).showCustomToast(app, app.getString(R.string.no_internet_connection),
//                            500, Toast.LENGTH_SHORT,0);
                }
            });

            Log.e(TAG, "Network Not Available ");

        } else {
            app.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    networkErrorBar.setVisibility(View.GONE);
//                    ((BreadWalletApp) app.getApplication()).showCustomToast(app, app.getString(R.string.no_internet_connection),
//                            500, Toast.LENGTH_SHORT,0);
                }
            });
            Log.e(TAG, "Network Available ");
        }
    }


}