package com.breadwallet.cache;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;

import static com.platform.sqlite.PlatformSqliteHelper.KV_STORE_TABLE_NAME;

public class UpgradeHandler implements BRDataSourceInterface {

    private static Context mContext;

    private static UpgradeHandler mInstance;

    private static BRSQLiteHelper dbHelper;

    private SQLiteDatabase mDatabase;

    private UpgradeHandler(Context context){
        this.mContext = context;
    }


    public static UpgradeHandler getInstance(Context context){
        if(mInstance == null) mInstance = new UpgradeHandler(context);
        dbHelper = BRSQLiteHelper.getInstance(context);
        return mInstance;
    }

    public SQLiteDatabase getWritable() {
//        if (mOpenCounter.incrementAndGet() == 1) {
        // Opening new database
//        if (ActivityUTILS.isMainThread()) throw new NetworkOnMainThreadException();
        if (mDatabase == null || !mDatabase.isOpen())
            mDatabase = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
//        }
//        Log.d(TAG, "getWritable open counter: " + String.valueOf(mOpenCounter.get()));
        return mDatabase;
    }

    public synchronized void deleteAllKVs() {
        try {
            SQLiteDatabase db = getWritable();
            db.execSQL("DROP TABLE IF EXISTS " + KV_STORE_TABLE_NAME);
        } catch (SQLException e) {
            e.printStackTrace();
        } finally {
//            dbLock.unlock();
        }
    }

    public void deleteAllTransactions() {
        try {
            mDatabase = openDatabase();
            mDatabase.delete(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, null);
        } finally {
            closeDatabase();
        }
    }

    @Override
    public SQLiteDatabase openDatabase() {
        if (mDatabase == null || !mDatabase.isOpen())
            mDatabase = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
        return mDatabase;
    }

    @Override
    public void closeDatabase() {

    }
}
