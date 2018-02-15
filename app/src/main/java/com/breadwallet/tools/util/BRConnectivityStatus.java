package com.breadwallet.tools.util;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * Created by byfieldj on 2/15/18.
 * <p>
 * <p>
 * Reusable class to quick check whether user is connect to Wifi or mobile data
 */

public class BRConnectivityStatus {


    private Context mContext;


    public BRConnectivityStatus(Context context) {
        this.mContext = context;
    }


    // Returns true if connected to wifi, false if connected to mobile data
    public boolean isWifiOrMobileDataConntected() {
        ConnectivityManager cm = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            android.net.NetworkInfo wifi =
                    cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile =
                    cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            if ((mobile != null && mobile.isConnectedOrConnecting()) || (wifi != null &&
                    wifi.isConnectedOrConnecting())) {
                return true;
            } else {
                return false;
            }
        } else {
            return false;
        }
    }


}
