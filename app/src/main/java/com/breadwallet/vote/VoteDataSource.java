package com.breadwallet.vote;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.json.JSONObject;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class VoteDataSource implements BRDataSourceInterface {

    public static final String ELA_NODE = "api-wallet-ela.elastos.org";
    public static final String ELA_NODE_KEY = "elaNodeKey";

    private Context mContext;
    private static VoteDataSource mInstance;

    private BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private VoteDataSource(Context context){
        this.mContext = context;
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static VoteDataSource getInstance(Context context){
        if(mInstance == null) mInstance = new VoteDataSource(context);
        return mInstance;
    }


    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public String urlPost(String url, String json) throws IOException {
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
                .post(body)
                .build();
        Response response = APIClient.elaClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }

    public String getUrl(String api){
        String node = BRSharedPrefs.getElaNode(mContext, ELA_NODE_KEY);
        if(StringUtil.isNullOrEmpty(node)) node = ELA_NODE;
        return new StringBuilder("https://").append(node).append("/").append(api).toString();
    }

    public List<ProducerEntity> getProducers(){
        try {
            String jsonRes = urlGET(getUrl("api/1/dpos/rank/height/9999999999999999"));
            if(!StringUtil.isNullOrEmpty(jsonRes) && jsonRes.contains("result")) {
                ProducersEntity producersEntity = new Gson().fromJson(jsonRes, ProducersEntity.class);
                if(producersEntity != null) return producersEntity.result;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @WorkerThread
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

    @Override
    public SQLiteDatabase openDatabase() {
        return null;
    }

    @Override
    public void closeDatabase() {

    }



}
