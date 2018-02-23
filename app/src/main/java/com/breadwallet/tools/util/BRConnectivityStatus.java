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
    public static final int NO_NETWORK = 0;
    public static final int WIFI_ON = 1;
    public static final int MOBILE_ON = 2;
    public static final int MOBILE_WIFI_ON = 3;


    public BRConnectivityStatus(Context context) {
        this.mContext = context;
    }


    // Returns true if connected to wifi, false if connected to mobile data
    public int isMobileDataOrWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager)
                mContext.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netinfo = cm.getActiveNetworkInfo();

        if (netinfo != null && netinfo.isConnectedOrConnecting()) {
            android.net.NetworkInfo wifi =
                    cm.getNetworkInfo(ConnectivityManager.TYPE_WIFI);
            android.net.NetworkInfo mobile =
                    cm.getNetworkInfo(ConnectivityManager.TYPE_MOBILE);

            // Mobile Data is on only
            if ((mobile != null && mobile.isConnectedOrConnecting()) && (wifi == null ||
                    !wifi.isConnectedOrConnecting())) {
                return MOBILE_ON;

                // Wifi is on only
            } else if (wifi != null && wifi.isConnectedOrConnecting() && (mobile == null || !mobile.isConnectedOrConnecting())) {
                return WIFI_ON;

                // Both mobile and wifi is on
            } else if (mobile != null && wifi != null && mobile.isConnectedOrConnecting() && wifi.isConnectedOrConnecting()) {

                return MOBILE_WIFI_ON;
            }
        }

        return NO_NETWORK;
    }




}
