package com.platform;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

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

    private static final String GET = "GET";
    private static final String POST = "POST";

    public static synchronized APIClient getInstance() {
        if (ourInstance == null) ourInstance = new APIClient();
        return ourInstance;
    }

    private APIClient() {
    }

    //returns the fee per kb or 0 if something went wrong
    public long feePerKb() {

        try {
            String strUtl = BASE_URL + FEE_PER_KB_URL;
            HTTPRequest request = new HTTPRequest(strUtl, HTTPRequest.GET, true, true);
            String response = sendRequest(request);

            JSONObject object = new JSONObject(response);
            return (long) object.getInt("fee_per_kb");
        } catch (JSONException e) {
            e.printStackTrace();

        }
        return 0;
    }

    public String sendRequest(HTTPRequest req) {
        StringBuilder builder = new StringBuilder();
        BufferedReader bufferedReader = null;
        HttpURLConnection conn = null;
        try {
            URL url = new URL(req.getUrl());

            conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod(req.getMethod());
            conn.setDoOutput(req.isDoOutput());
            conn.setDoInput(req.isDoInput());
            if (req.getProperties() != null) {
                Set set = req.getProperties().entrySet();
                Iterator iterator = set.iterator();
                while (iterator.hasNext()) {
                    Map.Entry entries = (Map.Entry) iterator.next();
                    conn.setRequestProperty(entries.getKey().toString(), entries.getValue().toString());
                }
            }
            if (req.getMessage() != null) {
                OutputStream os = conn.getOutputStream();
                os.write(req.getMessage());
                os.flush();
            }
            String aux = null;
            bufferedReader = new BufferedReader(new InputStreamReader(
                    (conn.getInputStream())));

            while ((aux = bufferedReader.readLine()) != null) {
                builder.append(aux);
            }
            System.out.println(conn.getURL());
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (conn != null)
                conn.disconnect();
        }

        return builder.toString();
    }

    public int getToken() {

        return null;

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
            return (long) object.getInt("fee_per_kb");
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

//    public String
}
