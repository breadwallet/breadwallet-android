package com.breadwallet.did;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.google.gson.Gson;
import com.platform.APIClient;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class DidDataSource implements BRDataSourceInterface {

    private static final String TAG = DidDataSource.class.getSimpleName();

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private static DidDataSource mInstance;
    private Context mContext;

    private DidDataSource(Context context){
        this.mContext = context;
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static DidDataSource getInstance(Context context){
        if(mInstance == null) mInstance = new DidDataSource(context);
        return mInstance;
    }

    public void putAuthorApp(AuthorInfo info){
        try {
            database = openDatabase();
            database.beginTransaction();

            ContentValues value = new ContentValues();
            value.put(BRSQLiteHelper.DID_AUTHOR_NICKNAME, info.getNickName());
            value.put(BRSQLiteHelper.DID_AUTHOR_DID, info.getDid());
            value.put(BRSQLiteHelper.DID_AUTHOR_PK, info.getPK());
            value.put(BRSQLiteHelper.DID_AUTHOR_APP_ID, info.getAppId());
            value.put(BRSQLiteHelper.DID_AUTHOR_AUTHOR_TIME, info.getAuthorTime());
            value.put(BRSQLiteHelper.DID_AUTHOR_EXP_TIME, info.getExpTime());
            value.put(BRSQLiteHelper.DID_AUTHOR_APP_NAME, info.getAppName());
            value.put(BRSQLiteHelper.DID_AUTHOR_APP_ICON, info.getAppIcon());
            value.put(BRSQLiteHelper.DID_REQUEST_INFO, info.getRequestInfo());

            long l = database.insertWithOnConflict(BRSQLiteHelper.DID_AUTHOR_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
            Log.i(TAG, "l:"+l);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

    public List<AuthorInfo> getAllInfos(){
        List<AuthorInfo> infos = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.DID_AUTHOR_TABLE_NAME, allColumns, null, null, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                AuthorInfo curEntity = cursorToInfo(cursor);
                infos.add(curEntity);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return infos;
    }

    public AuthorInfo getInfoByDid(String did){
        Cursor cursor = null;
        AuthorInfo result = null;
        try{
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.DID_AUTHOR_TABLE_NAME,
                    allColumns, BRSQLiteHelper.DID_AUTHOR_DID + " = ? ",
                    new String[]{did}, null, null, null);
            cursor.moveToNext();
            if (!cursor.isAfterLast()) {
                result = cursorToInfo(cursor);
            }

            return result;
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return null;
    }

    private final String[] allColumns = {
            BRSQLiteHelper.DID_AUTHOR_NICKNAME,//0
            BRSQLiteHelper.DID_AUTHOR_DID,//1
            BRSQLiteHelper.DID_AUTHOR_PK,//2
            BRSQLiteHelper.DID_AUTHOR_APP_ID,//3
            BRSQLiteHelper.DID_AUTHOR_AUTHOR_TIME,//4
            BRSQLiteHelper.DID_AUTHOR_EXP_TIME,//5
            BRSQLiteHelper.DID_AUTHOR_APP_NAME,//6
            BRSQLiteHelper.DID_AUTHOR_APP_ICON,//7
            BRSQLiteHelper.DID_REQUEST_INFO //8
    };

    private AuthorInfo cursorToInfo(Cursor cursor) {
        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setNickName(cursor.getString(0));
        authorInfo.setDid(cursor.getString(1));
        authorInfo.setPK(cursor.getString(2));
        authorInfo.setAppId(cursor.getString(3));
        authorInfo.setAuthorTime(cursor.getLong(4));
        authorInfo.setExpTime(cursor.getLong(5));
        authorInfo.setAppName(cursor.getString(6));
        authorInfo.setAppIcon(cursor.getString(7));
        authorInfo.setRequestInfo(cursor.getString(8));
        return authorInfo;
    }

    public String callBackUrl(String url, CallbackEntity entity){
        if(entity==null || StringUtil.isNullOrEmpty(url)) return null;
        String params = new Gson().toJson(entity);
        Log.i("DidDataSource", "callBackUrl: "+"url:"+url+" params:"+params);
        String tmp = urlPost(url, params);
        Log.i("DidDataSource", "callBackUrl: result:"+tmp);
        return tmp;
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public synchronized String urlPost(String url, String json) {
        int code;
        try {
            RequestBody body = RequestBody.create(JSON, json);
            Request request = new Request.Builder()
                    .url(url)
                    .post(body)
                    .build();
            Response response = APIClient.elaClient.newCall(request).execute();
            code = response.code();
            if (response.isSuccessful()) {
                return response.body().string();
            } else {
                return "err code:" + code;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    public void callReturnUrl(String url){
        urlGET(url);
    }

    @WorkerThread
    public synchronized String urlGET(String myURL) {

        try {
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return null;
    }

    @Override
    public SQLiteDatabase openDatabase() {
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
        return database;
    }

    @Override
    public void closeDatabase() {

    }
}
