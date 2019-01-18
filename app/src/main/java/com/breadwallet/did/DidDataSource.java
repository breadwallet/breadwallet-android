package com.breadwallet.did;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;

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
            value.put(BRSQLiteHelper.DID_AUTHOR_AUTHOR_TIME, info.getAuthorTime());
            value.put(BRSQLiteHelper.DID_AUTHOR_APP_NAME, info.getAppName());
            value.put(BRSQLiteHelper.DID_AUTHOR_APP_ICON, info.getAppIcon());

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
            cursor = database.query(BRSQLiteHelper.DID_AUTHOR_TABLE_NAME, allColumns, null, null, null, null, "authortime desc");

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
            BRSQLiteHelper.DID_AUTHOR_AUTHOR_TIME,//3
            BRSQLiteHelper.DID_AUTHOR_EXP_TIME,//4
            BRSQLiteHelper.DID_AUTHOR_APP_NAME,//5
            BRSQLiteHelper.DID_AUTHOR_APP_ICON//6
    };

    private AuthorInfo cursorToInfo(Cursor cursor) {
        AuthorInfo authorInfo = new AuthorInfo();
        authorInfo.setNickName(cursor.getString(0));
        authorInfo.setDid(cursor.getString(1));
        authorInfo.setPK(cursor.getString(2));
        authorInfo.setAuthorTime(cursor.getLong(3));
        authorInfo.setAuthorTime(cursor.getLong(4));
        authorInfo.setAppName(cursor.getString(5));
        authorInfo.setAppIcon(cursor.getString(6));
        return authorInfo;
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
