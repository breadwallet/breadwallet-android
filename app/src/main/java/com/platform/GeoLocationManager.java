package com.platform;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.os.Bundle;
import android.support.v4.app.ActivityCompat;
import android.util.Log;


import com.breadwallet.presenter.activities.MainActivity;
import com.google.firebase.crash.FirebaseCrash;

import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/12/17.
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
public class GeoLocationManager {
    private static final String TAG = GeoLocationManager.class.getName();
    private LocationManager locationManager;
    private Session session;

    private static GeoLocationManager instance;

    public static GeoLocationManager getInstance() {
        if (instance == null) instance = new GeoLocationManager();
        return instance;
    }

    public void startGeoSocket(Session sess) {
        session = sess;
        Log.e(TAG, "getLatestLocation: ");

        MainActivity app = MainActivity.app;
        if (app == null)
            return;
        locationManager = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        if (ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            RuntimeException ex = new RuntimeException("startGeoSocket, can't happen");
            FirebaseCrash.report(ex);
            throw ex;
        }
        locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, locationListener);
    }

    // Define a listener that responds to location updates
    private LocationListener locationListener = new LocationListener() {
        public void onLocationChanged(Location location) {
            Log.e(TAG, "onLocationChanged: ");
            // Called when a new location is found by the network location provider.
            if (session != null && session.isOpen()) {
                String jsonLocation = getJsonLocation(location);
                try {
                    session.getRemote().sendString(jsonLocation);
                } catch (IOException e) {
                    e.printStackTrace();
                }

            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    public static String getJsonLocation(Location location) {
        try {
            JSONObject responseJson = new JSONObject();

            JSONObject coordObj = new JSONObject();
            coordObj.put("latitude", location.getLatitude());
            coordObj.put("longitude", location.getLongitude());

            responseJson.put("timestamp", location.getTime());
            responseJson.put("coordinate", coordObj);
            responseJson.put("altitude", location.getAltitude());
            responseJson.put("horizontal_accuracy", location.getAccuracy());
            responseJson.put("description", "");
            return responseJson.toString();
        } catch (JSONException e) {
            Log.e(TAG, "handleLocation: Failed to create json response");
            e.printStackTrace();
        }
        return null;

    }

}