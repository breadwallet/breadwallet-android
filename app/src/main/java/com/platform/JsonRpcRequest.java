package com.platform;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.tools.util.Utils;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.Map;

import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by byfieldj on 3/26/18.
 */

public class JsonRpcRequest {

    private static final String TAG = "JsonRpcRequest";

    public JsonRpcRequest() {
    }


    public Response makeRpcRequest(Context app, String url, Map<String, String> params) {

        if (ActivityUTILS.isMainThread()) {
            Log.e(TAG, "makeRpcRequest: network on main thread");
            throw new RuntimeException("network on main thread");
        }

        Map<String, String> headers = BreadApp.getBreadHeaders();


        // Package up rpc request params and put them in the POST request
        JSONObject postJson = new JSONObject();
        Iterator paramIt = params.entrySet().iterator();

        try {
            while (paramIt.hasNext()) {
                Map.Entry pair = (Map.Entry) paramIt.next();
                postJson.put((String) pair.getKey(), pair.getValue());
                Log.d(TAG, "Rpc params -> " + pair.getKey() + ", " + pair.getValue());
            }
        } catch (JSONException e) {
            e.printStackTrace();
        }

        RequestBody requestBody = RequestBody.create(null, postJson.toString());


        Request.Builder builder = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(app, "android/HttpURLConnection"))
                .post(requestBody);

        /*Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }*/

        Request request = builder.build();
        Response resp = APIClient.getInstance(app).sendRequest(request, true, 0);
        try {
            Log.d(TAG, "Rpc Response -> " + resp.body().string());
        } catch (IOException e) {
            e.printStackTrace();
        }

        if (resp == null) {
            Log.e(TAG, "makeRpcRequest: " + url + ", resp is null");
            return null;
        }


        return resp;

    }


}
