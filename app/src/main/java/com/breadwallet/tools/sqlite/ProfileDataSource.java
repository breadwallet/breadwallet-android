package com.breadwallet.tools.sqlite;

import android.content.Context;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.json.JSONObject;

import java.io.IOException;
import java.util.Date;
import java.util.Iterator;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ProfileDataSource {

    private Context mContext;

//    final String DID_URL = "https://api-wallet-did-testnet.elastos.org/";
    public static final String DID_URL = "https://api-wallet-did.elastos.org/";
//    final String elaTestUrl = "https://api-wallet-ela-testnet.elastos.org/";

    private static ProfileDataSource instance;

    public static ProfileDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new ProfileDataSource(context);
        }
        return instance;
    }

    public ProfileDataSource(Context context) {
        this.mContext = context;
    }

    static class ProfileResponse {
        public String result;
        public int status;
    }

    public String upchain(String data){
        try {
            Log.i("ProfileFunction", "upchain data:"+data);
            ProfileResponse result = urlPost(DID_URL +"api/1/blockagent/upchain/data", data);
            Log.i("ProfileFunction", "result:"+result);
            if(200 == result.status) return result.result;
        } catch (IOException e) {
            Log.i("ProfileFunction", "upchain exception");
            e.printStackTrace();
        }

        return null;
    }

    static class Transaction {
        public String txid;
        public int confirmations;
        public int payloadversion;
        public int type;
    }

    public boolean isTxExit(String txid){
        Transaction transaction = getTransaction(txid);
        boolean is = !(transaction==null || StringUtil.isNullOrEmpty(transaction.txid));
        Log.i("ProfileFunction", "isTxExit:"+is);
        return is;
    }

    public String getProfileValue(String did, String key){
        String url = DID_URL + "/api/1/did/"+did+"/"+key;

        try {
            String result = urlGET(url);
            if(!StringUtil.isNullOrEmpty(result) && result.contains("200")) {
                JSONObject jsonObject = new JSONObject(result);
                result = jsonObject.getString("result");
                return result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;

    }

    private Transaction getTransaction(String txid){
        Log.i("ProfileFunction", "getTransaction:"+txid);
        String url = DID_URL + "api/1/tx/" + txid;
        String result = null;
        try {
            result = urlGET(url);
            if(!StringUtil.isNullOrEmpty(result) && result.contains("200")){
                JSONObject jsonObject = new JSONObject(result);
                result = jsonObject.getString("result");
                return new Gson().fromJson(result, Transaction.class);
            }
        } catch (Exception e) {
            Log.i("ProfileFunction", "getTransaction IOException");
            e.printStackTrace();
        }
        return null;
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public ProfileResponse urlPost(String url, String json) throws IOException {
        String author = createHeaderAuthor();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .header("X-Elastos-Agent-Auth", author)
                .post(body)
                .build();
        Response response = APIClient.elaClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return new Gson().fromJson(response.body().string(), ProfileResponse.class);
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    public String urlGET(String myURL) throws IOException {
        Map<String, String> headers = BreadApp.getBreadHeaders();

        Request.Builder builder = new Request.Builder()
                .url(myURL)
                .header("Content-Type", "application/json")
                .header("Accept", "application/json")
                .header("User-agent", Utils.getAgentString(mContext, "android/HttpURLConnection"))
                .get();
        Iterator it = headers.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry pair = (Map.Entry) it.next();
            builder.header((String) pair.getKey(), (String) pair.getValue());
        }

        Request request = builder.build();
        Response response = APIClient.elaClient.newCall(request).execute();

        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }


    static class Author {
        public String id;
        public String time;
        public String auth;
    }
    private String createHeaderAuthor(){
        String acc_id = "unCZRceA8o7dbny";
        String acc_secret = "qtvb4PlRVGLYYYQxyLIo3OgyKI7kUL";

        long time = new Date().getTime();
        String strTime = String.valueOf(time);

        SimpleHash hash = new SimpleHash("md5", acc_secret, strTime);
        String auth = hash.toHex();

        Author author = new Author();
        author.id = acc_id;
        author.auth = auth;
        author.time = strTime;

        return new Gson().toJson(author);
    }
}
