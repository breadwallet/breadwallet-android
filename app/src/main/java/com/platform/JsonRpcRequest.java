package com.platform;

import android.content.Context;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.presenter.activities.util.ActivityUTILS;

import org.json.JSONObject;

import java.io.IOException;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * Created by byfieldj on 3/26/18.
 */

public class JsonRpcRequest {

    private static final String TAG = "JsonRpcRequest";

    private JsonRpcRequest() {
    }

    public interface JsonRpcRequestListener {

        void onRpcRequestCompleted(String jsonResult);
    }

    @WorkerThread
    public static void makeRpcRequest(Context app, String url, JSONObject payload, JsonRpcRequestListener listener) {
        final MediaType JSON
                = MediaType.parse("application/json; charset=utf-8");

        RequestBody requestBody = RequestBody.create(JSON, payload.toString());

        Request request = new Request.Builder()
                .url(url)
                .header("Content-Type", "application/json; charset=utf-8")
                .header("Accept", "application/json")
                .post(requestBody).build();


        APIClient.BRResponse resp = APIClient.getInstance(app).sendRequest(request, true, 0);
        String responseString = resp.getBodyText();

        if (listener != null) {
            listener.onRpcRequestCompleted(responseString);
        }

    }

}
