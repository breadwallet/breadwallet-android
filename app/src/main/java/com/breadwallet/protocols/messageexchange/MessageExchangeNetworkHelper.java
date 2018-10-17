package com.breadwallet.protocols.messageexchange;

import android.content.Context;
import android.util.Log;

import com.breadwallet.core.BRCoreKey;
import com.breadwallet.protocols.messageexchange.entities.InboxEntry;
import com.breadwallet.protocols.messageexchange.entities.ServiceMetaData;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.platform.APIClient;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 7/13/18.
 * Copyright (c) 2018 breadwallet LLC
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
public final class MessageExchangeNetworkHelper {

    private static final String TAG = MessageExchangeNetworkHelper.class.getSimpleName();

    // Backend API paths
    public static final String INBOX_PATH = "/inbox";
    public static final String MESSAGE_PATH = "/message";
    public static final String ACK_PATH = "/ack";
    public static final String SERVICE_PATH = "/external/service";
    public static final String ASSOCIATED_KEYS = "/me/associated-keys";

    public static final String RECEIVED_TIME = "received_time";
    public static final String ACKNOWLEDGED = "acknowledged";
    public static final String ACKNOWLEDGED_TIME = "acknowledged_time";
    public static final String MESSAGE = "message";
    public static final String CURSOR = "cursor";
    public static final String SERVICE_URL = "service_url";

    public static final String ENTRIES_NAME = "entries";
    public static final String CONTENT_TYPE_PROTOBUF = "application/x-protobuf";

    // Inbox Parameters
    public static final String INBOX_PATH_AFTER_PARAMETER = "%s?after=%s";

    // Error parameters
    public static final String ERROR = "error";
    public static final String ERROR_MESSAGE = "message";
    public static final String ERROR_CODE = "code";

    // Service parameters
    public static final String URL = "url";
    public static final String NAME = "name";
    public static final String HASH = "hash";
    public static final String CREATED_TIME = "created_time";
    public static final String UPDATED_TIME = "updated_time";
    public static final String LOGO = "logo";
    public static final String DESCRIPTION = "description";
    public static final String DOMAINS = "domains";
    public static final String CAPABILITIES = "capabilities";
    public static final String SCOPES = "scopes";

    private MessageExchangeNetworkHelper() {
    }

    // TODO if 100 entries received, then fetch again.
    public static List<InboxEntry> fetchInbox(Context context, String afterCursor) {
        List<InboxEntry> inboxEntries = new ArrayList<>();
        String inboxUrl = APIClient.BASE_URL + INBOX_PATH;

        if (afterCursor != null) {
            inboxUrl = String.format(INBOX_PATH_AFTER_PARAMETER, inboxUrl, afterCursor);
        }

        Request request = new Request.Builder()
                .url(inboxUrl)
                .get()
                .build();
        APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, true);
        if (Utils.isNullOrEmpty(response.getBody())) {
            return null;
        }
        try {
            ErrorObject errorObject = getError(response.getBodyText());
            if (errorObject != null) {
                Log.e(TAG, "fetchInbox: " + errorObject.mMessage);
                return null;
            }
            JSONObject fullObj = new JSONObject(response.getBodyText());
            JSONArray inboxEntriesArray = fullObj.getJSONArray(ENTRIES_NAME);
            for (int i = 0; i < inboxEntriesArray.length(); i++) {
                String receivedTime = null;
                boolean acknowledged = false;
                String acknowledgedTime = null;
                String message = null;
                String cursor = null;
                String serviceUrl = null;
                JSONObject obj = inboxEntriesArray.getJSONObject(i);
                if (obj.has(RECEIVED_TIME)) {
                    receivedTime = obj.getString(RECEIVED_TIME);
                }
                if (obj.has(ACKNOWLEDGED)) {
                    acknowledged = obj.getBoolean(ACKNOWLEDGED);
                }
                if (obj.has(ACKNOWLEDGED_TIME)) {
                    acknowledgedTime = obj.getString(ACKNOWLEDGED_TIME);
                }
                if (obj.has(MESSAGE)) {
                    message = obj.getString(MESSAGE);
                }
                if (obj.has(CURSOR)) {
                    cursor = obj.getString(CURSOR);
                }
                if (obj.has(SERVICE_URL)) {
                    serviceUrl = obj.getString(SERVICE_URL);
                }
                InboxEntry inboxEntry = new InboxEntry(receivedTime, acknowledged, acknowledgedTime, message, cursor, serviceUrl);
                inboxEntries.add(inboxEntry);

            }
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return inboxEntries;
    }

    public static void sendEnvelope(Context context, byte[] data) {
        String messageUrl = APIClient.BASE_URL + MESSAGE_PATH;
        MediaType contentType = MediaType.parse(CONTENT_TYPE_PROTOBUF);
        RequestBody requestBody = RequestBody.create(contentType, data);
        Request request = new Request.Builder()
                .url(messageUrl)
                .post(requestBody)
                .header(BRConstants.HEADER_CONTENT_TYPE, CONTENT_TYPE_PROTOBUF)
                .build();
        APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, true);
        ErrorObject errorObject = getError(response.getBodyText());
        logError(errorObject, "sendEnvelope", response);

    }

    public static void sendAck(Context context, List<String> cursors) {
        JSONArray ackJsonArray = new JSONArray();
        for (String cursor : cursors) {
            ackJsonArray.put(cursor);
        }
        String ackUrl = APIClient.BASE_URL + ACK_PATH;
        RequestBody requestBody = RequestBody.create(null, ackJsonArray.toString());
        Request request = new Request.Builder()
                .url(ackUrl)
                .post(requestBody)
                .addHeader(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_JSON)
                .build();
        APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, true);
        ErrorObject errorObject = getError(response.getBodyText());
        logError(errorObject, "sendAck", response);
    }

    public static void sendAssociatedKey(Context context, byte[] publicKey) {
        String associatedUrl = APIClient.BASE_URL + ASSOCIATED_KEYS;
        String base58PublicKey = BRCoreKey.encodeBase58(publicKey);
        RequestBody requestBody = RequestBody.create(null, base58PublicKey);
        Request request = new Request.Builder()
                .url(associatedUrl)
                .post(requestBody)
                .addHeader(BRConstants.HEADER_CONTENT_TYPE, BRConstants.CONTENT_TYPE_TEXT)
                .build();
        APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, true);
        ErrorObject errorObject = getError(response.getBodyText());
        logError(errorObject, "sendAssociatedKey", response);
    }

    public static void getAssociatedKeys(Context context) {
        String associatedUrl = APIClient.BASE_URL + ASSOCIATED_KEYS;
        Request request = new Request.Builder()
                .url(associatedUrl)
                .get()
                .build();
        APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, true);
        Log.e(TAG, "getAssociatedKeys: " + response.getCode() + ", " + response.getBodyText());
        ErrorObject errorObject = getError(response.getBodyText());
        logError(errorObject, "getAssociatedKeys", response);

    }

    public static ServiceMetaData getService(Context context, String serviceId) {
        String serviceUrl = APIClient.BASE_URL + SERVICE_PATH + "/" + serviceId;
        Request request = new Request.Builder()
                .url(serviceUrl)
                .get()
                .build();
        APIClient.BRResponse response = APIClient.getInstance(context).sendRequest(request, true);
        ErrorObject errorObject = getError(response.getBodyText());
        if (errorObject != null) {
            Log.e(TAG, "getService error:" + errorObject.mMessage);
            return null;
        }
        try {
            JSONObject object = new JSONObject(response.getBodyText());
            String url = object.has(URL) ? object.getString(URL) : null;
            String name = object.has(NAME) ? object.getString(NAME) : null;
            String hash = object.has(HASH) ? object.getString(HASH) : null;
            String createdTime = object.has(CREATED_TIME) ? object.getString(CREATED_TIME) : null;
            String updatedTime = object.has(UPDATED_TIME) ? object.getString(UPDATED_TIME) : null;
            String logo = object.has(LOGO) ? object.getString(LOGO) : null;
            String description = object.has(DESCRIPTION) ? object.getString(DESCRIPTION) : null;
            List<String> domains = new ArrayList<>();
            if (object.has(DOMAINS)) {
                JSONArray domainsJsonArray = object.getJSONArray(DOMAINS);
                for (int i = 0; i < domainsJsonArray.length(); i++) {
                    domains.add(domainsJsonArray.getString(i));
                }
            }
            List<ServiceMetaData.Capability> capabilities = new ArrayList<>();
            if (object.has(CAPABILITIES)) {
                JSONArray capabilitiesJsonArray = object.getJSONArray(CAPABILITIES);
                for (int i = 0; i < capabilitiesJsonArray.length(); i++) {
                    JSONObject capabilityJson = capabilitiesJsonArray.getJSONObject(i);
                    String capabilityName = capabilityJson.has(NAME) ? capabilityJson.getString(NAME) : null;
                    JSONArray scopesJsonArray = capabilityJson.has(SCOPES) ? capabilityJson.getJSONArray(SCOPES) : new JSONArray();
                    HashMap<String, String> scopes = new HashMap<>();
                    for (int j = 0; j < scopesJsonArray.length(); j++) {
                        JSONObject scopeObject = scopesJsonArray.getJSONObject(j);
                        String scopeName = scopeObject.has(NAME) ? scopeObject.getString(NAME) : null;
                        String scopeDescription = scopeObject.has(DESCRIPTION) ? scopeObject.getString(DESCRIPTION) : null;
                        scopes.put(NAME, scopeName);
                        scopes.put(DESCRIPTION, scopeDescription);
                    }
                    String capabilityDescription = capabilityJson.has(DESCRIPTION) ? capabilityJson.getString(DESCRIPTION) : null;
                    ServiceMetaData.Capability capability = new ServiceMetaData.Capability(capabilityName, scopes, capabilityDescription);
                    capabilities.add(capability);
                }
            }

            return new ServiceMetaData(url, name, hash, createdTime, updatedTime, logo, description, domains, capabilities);
        } catch (JSONException e) {
            Log.e(TAG, "getService: ", e);
        }
        return null;
    }

    private static void logError(ErrorObject errorObject, String tag, APIClient.BRResponse response) {
        if (errorObject != null) {
            Log.e(TAG, tag + ": " + errorObject.mMessage);
        }
        if (!response.isSuccessful()) {
            Log.e(TAG, tag + ": " + String.valueOf(response.getCode()));
        }
    }


    /**
     * Content-Type: application/json
     * {
     * "error": "err_too_many_messages",
     * "message": "Too many messages."
     * "code": 429
     * }
     */
    private static ErrorObject getError(String response) {
        String err = null;
        try {
            JSONObject jsonObject = new JSONObject(response);
            return new ErrorObject(jsonObject.getString(ERROR), jsonObject.getString(ERROR_MESSAGE), jsonObject.getInt(ERROR_CODE));
        } catch (Exception ignored) {
        }
        return null;
    }

    static class ErrorObject {
        private String mError;
        private String mMessage;
        private int mCode;

        public ErrorObject(String error, String message, int code) {
            this.mError = error;
            this.mMessage = message;
            this.mCode = code;
        }

        public String getError() {
            return mError;
        }

        public String getMessage() {
            return mMessage;
        }

        public int getCode() {
            return mCode;
        }
    }

}
