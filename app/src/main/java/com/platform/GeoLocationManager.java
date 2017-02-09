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
import com.breadwallet.tools.util.Utils;
import com.google.firebase.crash.FirebaseCrash;

import org.eclipse.jetty.continuation.Continuation;
import org.eclipse.jetty.server.Request;
import org.eclipse.jetty.websocket.api.Session;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;

import javax.servlet.http.HttpServletResponse;

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
    private Session session;
    private Continuation continuation;
    private Request baseRequest;
    private LocationManager locationManager;

    private static GeoLocationManager instance;

    public static GeoLocationManager getInstance() {
        if (instance == null) instance = new GeoLocationManager();
        return instance;
    }

    public void getOneTimeGeoLocation(Continuation cont, Request req) {
        this.continuation = cont;
        this.baseRequest = req;
        final MainActivity app = MainActivity.app;
        if (app == null)
            return;
        locationManager = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        if (locationManager == null) {
            Log.e(TAG, "getOneTimeGeoLocation: locationManager is null!");
            return;
        }
        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) !=
                        PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    RuntimeException ex = new RuntimeException("getOneTimeGeoLocation, can't happen");
                    Log.e(TAG, "run: getOneTimeGeoLocation, can't happen");
                    FirebaseCrash.report(ex);
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 0, 0, locationListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 0, 0, locationListener);
            }
        });

    }

    public void startGeoSocket(Session sess) {
        session = sess;

        final MainActivity app = MainActivity.app;
        if (app == null)
            return;
        final LocationManager locationManager = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);

        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    RuntimeException ex = new RuntimeException("startGeoSocket, can't happen");
                    Log.e(TAG, "run: startGeoSocket, can't happen");
                    FirebaseCrash.report(ex);
                    return;
                }
                locationManager.requestLocationUpdates(LocationManager.NETWORK_PROVIDER, 1000, 0, socketLocationListener);
                locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, socketLocationListener);

            }
        });
    }

    public void stopGeoSocket() {
        final MainActivity app = MainActivity.app;
        if (app == null)
            return;
        final LocationManager locationManager = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);

        app.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(app, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                    Log.e(TAG, "stopGeoSocket, can't happen");
                    RuntimeException ex = new RuntimeException("stopGeoSocket, can't happen");
                    FirebaseCrash.report(ex);
                    throw ex;
                }
                locationManager.removeUpdates(socketLocationListener);

            }
        });
    }

    // Define a listener that responds to location updates
    private LocationListener socketLocationListener = new LocationListener() {
        private boolean sending;

        public void onLocationChanged(Location location) {
            // Called when a new location is found by the network location provider.
            if (sending) return;
            sending = true;
            if (session != null && session.isOpen()) {
                final String jsonLocation = getJsonLocation(location);
                new Thread(new Runnable() {
                    @Override
                    public void run() {
                        try {
                            session.getRemote().sendString(jsonLocation);
                        } catch (IOException e) {
                            e.printStackTrace();
                        } finally {
                            sending = false;
                        }
                    }
                }).start();

            } else {
                sending = false;
            }
        }

        public void onStatusChanged(String provider, int status, Bundle extras) {
        }

        public void onProviderEnabled(String provider) {
        }

        public void onProviderDisabled(String provider) {
        }
    };

    private LocationListener locationListener = new LocationListener() {
        private boolean processing;

        public void onLocationChanged(final Location location) {
            if (processing) return;
            processing = true;
            new Thread(new Runnable() {
                @Override
                public void run() {
                    // Called when a new location is found by the network location provider.
                    if (continuation != null && baseRequest != null) {
                        String jsonLocation = getJsonLocation(location);
                        try {
                            if (!Utils.isNullOrEmpty(jsonLocation)) {
                                try {
                                    ((HttpServletResponse) continuation.getServletResponse()).setStatus(200);
                                    continuation.getServletResponse().getOutputStream().write(jsonLocation.getBytes("UTF-8"));
                                    baseRequest.setHandled(true);
                                    continuation.complete();
                                    continuation = null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                            } else {
                                try {
                                    ((HttpServletResponse) continuation.getServletResponse()).sendError(500);
                                    baseRequest.setHandled(true);
                                    continuation.complete();
                                    continuation = null;
                                } catch (IOException e) {
                                    e.printStackTrace();
                                }
                                FirebaseCrash.report(new NullPointerException("onLocationChanged: " + jsonLocation));
                                Log.e(TAG, "onLocationChanged: WARNING respStr is null or empty: " + jsonLocation);
                            }
                        } catch (Exception e) {
                            e.printStackTrace();
                        } finally {

                            processing = false;
                            if (MainActivity.app == null || ActivityCompat.checkSelfPermission(MainActivity.app, Manifest.permission.ACCESS_FINE_LOCATION)
                                    != PackageManager.PERMISSION_GRANTED && ActivityCompat.checkSelfPermission(MainActivity.app,
                                    Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
                                Log.e(TAG, "onLocationChanged: PERMISSION DENIED for removeUpdates");
                            } else {
                                locationManager.removeUpdates(locationListener);

                            }

                        }

                    }
                }
            }).start();

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