package com.platform.middlewares.plugins;

import android.content.Intent;
import android.location.Address;
import android.location.Geocoder;
import android.net.Uri;

import com.breadwallet.presenter.activities.MainActivity;
import com.google.firebase.crash.FirebaseCrash;
import com.platform.interfaces.Plugin;

import junit.framework.Assert;

import org.eclipse.jetty.server.Request;

import java.io.IOException;
import java.util.List;
import java.util.Locale;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

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
public class LinkPlugin implements Plugin {
    public static final String TAG = LinkPlugin.class.getName();

    @Override
    public boolean handle(String target, Request baseRequest, HttpServletRequest request, HttpServletResponse response) {
        if (target.startsWith("/_open_url")) {
            String url = request.getParameter("url");

            MainActivity app = MainActivity.app;
            if (app == null) {
                try {
                    response.sendError(500, "context is null");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }

            if (url != null && url.startsWith("tel")) {
                Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url.replace("/","")));
                app.startActivity(intent);
            } else {
//                FirebaseCrash.log("could not handle url: " + url);
            }

            return true;
        } else if (target.startsWith("/_open_maps")) {
            MainActivity app = MainActivity.app;
            if (app == null) {
                try {
                    response.sendError(500, "context is null");
                } catch (IOException e) {
                    e.printStackTrace();
                }
                return true;
            }
            double destLatitude = 0;
            double destLongitude = 0;
            Geocoder geocoder = new Geocoder(app, Locale.getDefault());
            try {
                List<Address> list = geocoder.getFromLocationName("", 1);
                destLatitude = list.get(0).getLatitude();
                destLongitude = list.get(0).getLongitude();
            } catch (IOException e) {
                e.printStackTrace();
            }
            Assert.assertTrue(destLatitude != 0);
            Assert.assertTrue(destLongitude != 0);
            //todo finish
//            String uri = "http://maps.google.com/maps?f=d&hl=en&saddr=" + latitude1 + "," + longitude1 + "&daddr=" + destLatitude + "," + destLongitude;
//            Intent intent = new Intent(android.content.Intent.ACTION_VIEW, Uri.parse(uri));
//            startActivity(Intent.createChooser(intent, "Select an application"));
            return true;
        } else return false;

    }
}
