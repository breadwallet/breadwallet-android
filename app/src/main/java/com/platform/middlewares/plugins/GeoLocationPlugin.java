package com.platform.middlewares.plugins;

import android.Manifest;
import android.content.Context;
import android.content.pm.PackageManager;
import android.location.LocationManager;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.util.Log;

import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.BRCompressor;
import com.platform.APIClient;
import com.platform.interfaces.Plugin;
import com.platform.kvstore.CompletionObject;
import com.platform.kvstore.RemoteKVStore;
import com.platform.kvstore.ReplicatedKVStore;
import com.platform.sqlite.KVEntity;

import org.apache.commons.io.IOUtils;
import org.eclipse.jetty.server.Request;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.text.SimpleDateFormat;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import static android.R.attr.enabled;
import static android.R.attr.key;
import static com.breadwallet.R.string.status;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 11/2/16.
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
public class GeoLocationPlugin implements Plugin {
    public static final String TAG = GeoLocationPlugin.class.getName();

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {

        if (target.startsWith("/_permissions/geo")) {
            MainActivity app = MainActivity.app;
            if (app == null) {
                try {
                    response.sendError(500, "context is null");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            switch (request.getMethod()) {
                // GET /_permissions/geo
                //
                // Call this method to retrieve the current permission status for geolocation.
                // The returned JSON dictionary contains the following keys:
                //
                // "status" = "denied" | "restricted | "undetermined" | "inuse" | "always"
                // "user_queried" = true | false
                // "location_enabled" = true | false
                //
                // The status correspond to those found in the apple CLLocation documentation: http://apple.co/1O0lHFv
                //
                // "user_queried" indicates whether or not the user has already been asked for geolocation
                // "location_enabled" indicates whether or not the user has geo location enabled on their phone
                case "GET":
                    JSONObject jsonResult = new JSONObject();
                    String status;
                    boolean enabled;
                    int permissionCheck = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION);
                    if (permissionCheck == PackageManager.PERMISSION_GRANTED) {
                        status = "always";
                        enabled = true;
                    } else {
                        status = "denied";
                        enabled = false;
                    }
                    try {
                        jsonResult.put("status", status);
                        jsonResult.put("user_queried", SharedPreferencesManager.getGeoPermissionsRequested(app));
                        jsonResult.put("location_enabled", enabled);
                        response.setStatus(200);
                    } catch (JSONException e) {
                        e.printStackTrace();
                        try {
                            response.sendError(500);
                        } catch (IOException e1) {
                            e1.printStackTrace();
                        }
                    }
                    return true;
                // GET /_geo
                //
                // Calling this method will query CoreLocation for a location object. The returned value may not be returned
                // very quick (sometimes getting a geo lock takes some time) so be sure to display to the user some status
                // while waiting for a response.
                //
                // Response Object:
                //
                // "coordinates" = { "latitude": double, "longitude": double }
                // "altitude" = double
                // "description" = "a string representation of this object"
                // "timestamp" = "ISO-8601 timestamp of when this location was generated"
                // "horizontal_accuracy" = double
                case "POST":
                    try {
                        if (ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION)
                                != PackageManager.PERMISSION_GRANTED) {

                            ActivityCompat.requestPermissions(app, new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, 0);
                        }
                        SharedPreferencesManager.putGeoPermissionsRequested(app, true);
                        response.setStatus(204);
                    } catch (Exception e) {
                        e.printStackTrace();
                        response.setStatus(400);
                    }
                    // Read from request
                    return true;
                default:
                    throw new RuntimeException("something has gone wrong");

            }
        } else if (target.startsWith("/_geo")) {
            MainActivity app = MainActivity.app;
            if (app == null) {
                try {
                    response.sendError(500, "context is null");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            // GET /_geo
            //
            // Calling this method will query CoreLocation for a location object. The returned value may not be returned
            // very quick (sometimes getting a geo lock takes some time) so be sure to display to the user some status
            // while waiting for a response.
            //
            // Response Object:
            //
            // "coordinates" = { "latitude": double, "longitude": double }
            // "altitude" = double
            // "description" = "a string representation of this object"
            // "timestamp" = "ISO-8601 timestamp of when this location was generated"
            // "horizontal_accuracy" = double
            if (request.getMethod().equalsIgnoreCase("GET")) {
                JSONObject obj = getAuthorizationError(app);
                if (obj != null) {
                    try {
                        response.getWriter().write(obj.toString());
                    } catch (IOException e) {
                        e.printStackTrace();
                    }
                    return true;
                }
            }
        } else if (target.startsWith("/_geosocket")){
            // GET /_geosocket
            //
            // This opens up a websocket to the location manager. It will return a new location every so often (but with no
            // predetermined interval) with the same exact structure that is sent via the GET /_geo call.
            //
            // It will start the location manager when there is at least one client connected and stop the location manager
            // when the last client disconnects.

        }

        return false;
    }

    private JSONObject getAuthorizationError(Context app) {
        String error = null;

        LocationManager lm = (LocationManager) app.getSystemService(Context.LOCATION_SERVICE);
        boolean gps_enabled = false;
        boolean network_enabled = false;

        try {
            gps_enabled = lm.isProviderEnabled(LocationManager.GPS_PROVIDER);
        } catch (Exception ex) {
        }

        try {
            network_enabled = lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER);
        } catch (Exception ex) {
        }
        if (!gps_enabled && !network_enabled) {
            error = "Location services are disabled";
        }
        int permissionCheck = ContextCompat.checkSelfPermission(app, Manifest.permission.ACCESS_FINE_LOCATION);
        if (permissionCheck != PackageManager.PERMISSION_GRANTED) {
            error = "Location services are not authorized";
        }

        if (error != null) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("error", error);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            return obj;
        } else {
            return null;
        }

    }
}
