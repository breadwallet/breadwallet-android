package com.platform;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 9/29/16.
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
public class APIClient {
    public static final String TAG = APIClient.class.getName();

    // proto is the transport protocol to use for talking to the API (either http or https)
    private static final String PROTO = "https";
    // host is the server(s) on which the API is hosted
    private static final String HOST = "api.breadwallet.com";
    // convenience getter for the API endpoint
    private static final String BASE_URL = PROTO + "://" + HOST;
    //feePerKb url
    private static final String FEE_PER_KB_URL = "/v1/fee-per-kb";
    //singleton instance
    private static APIClient ourInstance;

    public static synchronized APIClient getInstance() {
        if (ourInstance == null) ourInstance = new APIClient();
        return ourInstance;
    }

    private APIClient() {
    }

    //returns the fee per kb or 0 if something went wrong
    public long feePerKb() {
        HttpURLConnection conn = null;
        try {
            URL url = new URL(BASE_URL + FEE_PER_KB_URL);
            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            System.out.println(conn.getURL());

            BufferedReader br = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            StringBuilder builder = new StringBuilder();
            String aux;

            while ((aux = br.readLine()) != null) {
                builder.append(aux);
            }
            JSONObject object = new JSONObject(builder.toString());
            return object.getInt("fee_per_kb");
        } catch (IOException e) {
            e.printStackTrace();
            if (conn != null)
                conn.disconnect();
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return 0;
    }

    public String signRequest(String request) {
        return null;
    }

    public String dataTaskWithRequest() {
        return null;
    }
}
