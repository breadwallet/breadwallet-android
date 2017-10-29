package com.breadwallet.tools.manager;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.util.Utils;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import static com.platform.APIClient.BASE_URL;
import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 8/3/17.
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
public class BREventManager implements BreadApp.OnAppBackgrounded {
    private static final String TAG = BREventManager.class.getName();

    private static BREventManager instance;
    private String sessionId;
    private List<Event> events = new ArrayList<>();

    private BREventManager() {
        sessionId = UUID.randomUUID().toString();
        BreadApp.addOnBackgroundedListener(this);
    }

    public static BREventManager getInstance() {
        if (instance == null) instance = new BREventManager();
        return instance;
    }

    public void pushEvent(String eventName, Map<String, String> attributes) {
        Log.d(TAG, "pushEvent: " + eventName);
        Event event = new Event(sessionId, System.currentTimeMillis() * 1000, eventName, attributes);
        events.add(event);
    }

    public void pushEvent(String eventName) {
        Log.d(TAG, "pushEvent: " + eventName);
        Event event = new Event(sessionId, System.currentTimeMillis() * 1000, eventName, null);
        events.add(event);
    }

    @Override
    public void onBackgrounded() {
        Log.e(TAG, "onBackgrounded: ");
        saveEvents();
        pushToServer();
    }

    private void saveEvents() {
        Log.d(TAG, "saveEvents: ");
        JSONArray array = new JSONArray();
        for (Event event : events) {
            JSONObject obj = new JSONObject();
            try {
                obj.put("sessionId", event.sessionId);
                obj.put("time", event.time);
                obj.put("eventName", event.eventName);
                JSONObject mdObj = new JSONObject();
                if (event.attributes != null && event.attributes.size() > 0) {
                    for (Map.Entry<String, String> entry : event.attributes.entrySet()) {
//                        System.out.println(entry.getKey() + "/" + entry.getValue());
                        mdObj.put(entry.getKey(), entry.getValue());
                    }
                }
                obj.put("metadata", mdObj);
            } catch (JSONException e) {
                e.printStackTrace();
            }
            Log.e(TAG, "saveEvents: insert json to array: " + obj);
            array.put(obj);
        }
        Context app = BreadApp.getInstance();
        if (app != null) {
            String fileName = app.getFilesDir().getAbsolutePath() + "/events/" + UUID.randomUUID().toString();
            writeEventsToDisk(fileName, array.toString());
        } else {
            Log.e(TAG, "saveEvents: FAILED TO WRITE EVENTS TO FILE: app is null");
        }
    }

    private void pushToServer() {
        Log.d(TAG, "pushToServer");
        Context app = BreadApp.getInstance();
        if (app != null) {
            List<JSONArray> arrs = getEventsFromDisk(app);
            int fails = 0;
            for (JSONArray arr : arrs) {
                JSONObject obj = new JSONObject();
                try {
                    obj.put("deviceType", 1);
                    int verCode = -1;
                    try {
                        PackageInfo pInfo = app.getPackageManager().getPackageInfo(app.getPackageName(), 0);
                        verCode = pInfo.versionCode;
                    } catch (PackageManager.NameNotFoundException e) {
                        e.printStackTrace();
                    }
                    obj.put("appVersion", verCode);
                    obj.put("events", arr);

                    String strUtl = BASE_URL + "/events";

                    final MediaType JSON = MediaType.parse("application/json");
                    RequestBody requestBody = RequestBody.create(JSON, obj.toString());
                    Request request = new Request.Builder()
                            .url(strUtl)
                            .header("Content-Type", "application/json")
                            .header("Accept", "application/json")
                            .post(requestBody).build();
                    String strResponse = null;
                    Response response;
                    try {
                        response = APIClient.getInstance().sendRequest(request, true, 0);
                        if (response != null)
                            strResponse = response.body().string();
                    } catch (IOException e) {
                        e.printStackTrace();
                        fails++;
                    }
                    if (Utils.isNullOrEmpty(strResponse)) {
                        Log.e(TAG, "pushToServer: response is empty");
                        fails++;
                    }

                } catch (JSONException e) {
                    e.printStackTrace();
                    fails++;
                }
            }
            if (fails == 0) {
                //if no fails then remove the local files.
                File dir = new File(app.getFilesDir().getAbsolutePath() + "/events/");
                if (dir.isDirectory()) {
                    String[] children = dir.list();
                    for (int i = 0; i < children.length; i++) {
                        new File(dir, children[i]).delete();
                    }
                } else {
                    Log.e(TAG, "pushToServer:  HUH?");
                }
            } else {
                Log.e(TAG, "pushToServer: FAILED with:" + fails + " fails");
            }
        } else {
            Log.e(TAG, "pushToServer: Failed to push, app is null");
        }
    }

    private boolean writeEventsToDisk(String fileName, String json) {
        Log.e(TAG, "saveEvents: eventsFile: " + fileName + ", \njson: " + json);
        try {
            FileWriter file = new FileWriter(fileName);
            file.write(json);
            file.flush();
            file.close();
            return true;
        } catch (IOException e) {
            Log.e("TAG", "Error in Writing: " + e.getLocalizedMessage());
        }
        return false;
    }

    //returns the list of JSONArray which consist of Event arrays
    private static List<JSONArray> getEventsFromDisk(Context context) {
        List<JSONArray> result = new ArrayList<>();
        File dir = new File(context.getFilesDir().getAbsolutePath() + "/events/");
        for (File f : dir.listFiles()) {
            if (f.isFile()) {
                String name = f.getName();
                Log.e(TAG, "getEventsFromDisk: name:" + name);
                try {
                    JSONArray arr = new JSONArray(readFile(name));
                    result.add(arr);
                } catch (JSONException e) {
                    e.printStackTrace();
                }
            } else {
                Log.e(TAG, "getEventsFromDisk: Unexpected directory where file is expected: " + f.getName());
            }
        }
        return result;
    }

    private static String readFile(String fileName) {
        try {
            File f = new File(fileName);
            //check whether file exists
            FileInputStream is = new FileInputStream(f);
            int size = is.available();
            byte[] buffer = new byte[size];
            is.read(buffer);
            is.close();
            return new String(buffer);
        } catch (IOException e) {
            Log.e("TAG", "Error in Reading: " + e.getLocalizedMessage());
            return null;
        }
    }

    public class Event {
        public String sessionId;
        public long time;
        public String eventName;
        public Map<String, String> attributes;

        public Event(String sessionId, long time, String eventName, Map<String, String> attributes) {
            this.sessionId = sessionId;
            this.time = time;
            this.eventName = eventName;
            this.attributes = attributes;
        }
    }
}
