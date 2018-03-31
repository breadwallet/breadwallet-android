package com.platform;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.tools.util.Utils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;
import java.util.concurrent.TimeUnit;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by byfieldj on 3/26/18.
 */

public class JsonRpcRequest {

    private static final String TAG = "JsonRpcRequest";
    private JsonRpcRequestListener mRequestListener;

    public JsonRpcRequest() {
    }

    public interface JsonRpcRequestListener {

        void onRpcRequestCompleted(String jsonResult);
    }


    public Response makeRpcRequest(Context app, String url, JSONObject payload, JsonRpcRequestListener listener) {

        this.mRequestListener = listener;

        if (ActivityUTILS.isMainThread()) {
            Log.e(TAG, "makeRpcRequest: network on main thread");
            throw new RuntimeException("network on main thread");
        }

        Map<String, String> headers = BreadApp.getBreadHeaders();


        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody requestBody = RequestBody.create(JSON, payload.toString());
        Log.d(TAG, "JSON params -> " + payload.toString());


        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .post(requestBody);

        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        String response = null;
        Request request = builder.build();
        //Log.d(TAG, "Request body -> " + request.body().);
        //Response resp = APIClient.getInstance(app).sendRequest(request, true, 0);

        Response resp = null;
        OkHttpClient client = new OkHttpClient.Builder().followRedirects(false).connectTimeout(10, TimeUnit.SECONDS)/*.addInterceptor(new LoggingInterceptor())*/.build();

        try {

            request = APIClient.getInstance(app).authenticateRequest(request);
            resp = client.newCall(request).execute();


            mRequestListener.onRpcRequestCompleted(resp.body().string());



            if (resp == null) {

                Log.e(TAG, "makeRpcRequest: " + url + ", resp is null");
                return null;

            }
        } catch (IOException e) {
            e.printStackTrace();
        }


        return resp;

    }


}
