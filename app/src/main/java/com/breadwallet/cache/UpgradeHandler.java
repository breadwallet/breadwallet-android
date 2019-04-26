package com.breadwallet.cache;

import android.content.Context;
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;

import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;

import static com.breadwallet.tools.sqlite.BRSQLiteHelper.ELA_TX_TABLE_NAME;
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

    public static final String ELA_TX_TABLE_NAME = "elaTransactionTable";
    public static final String ELA_COLUMN_ID = "_id";
    public static final String ELA_COLUMN_ISRECEIVED ="isReceived";//0 false,1 true
    public static final String ELA_COLUMN_TIMESTAMP ="timeStamp";
    public static final String ELA_COLUMN_BLOCKHEIGHT ="blockHeight";
    public static final String ELA_COLUMN_HASH ="hash";
    public static final String ELA_COLUMN_TXREVERSED ="txReversed";
    public static final String ELA_COLUMN_FEE ="fee";
    public static final String ELA_COLUMN_TO ="toAddress";
    public static final String ELA_COLUMN_FROM ="fromAddress";
    public static final String ELA_COLUMN_BALANCEAFTERTX ="balanceAfterTx";
    public static final String ELA_COLUMN_TXSIZE ="txSize";
    public static final String ELA_COLUMN_AMOUNT ="amount";
    public static final String ELA_COLUMN_MENO ="meno";
    public static final String ELA_COLUMN_ISVALID ="isValid";
    public static final String ELA_COLUMN_ISVOTE ="isVote";

    private static final String ELA_TX_DATABASE_CREATE = "create table if not exists " + ELA_TX_TABLE_NAME + " (" +
            ELA_COLUMN_ID + " integer, " +
            ELA_COLUMN_ISRECEIVED + " integer, " +
            ELA_COLUMN_TIMESTAMP + " integer DEFAULT '0' , " +
            ELA_COLUMN_BLOCKHEIGHT + " interger, " +
            ELA_COLUMN_HASH + " blob, " +
            ELA_COLUMN_TXREVERSED+ " text primary key , " +
            ELA_COLUMN_FEE + " real, " +
            ELA_COLUMN_TO + " text, " +
            ELA_COLUMN_FROM + " text, " +
            ELA_COLUMN_BALANCEAFTERTX + " integer, " +
            ELA_COLUMN_TXSIZE + " integer, " +
            ELA_COLUMN_AMOUNT + " real, " +
            ELA_COLUMN_MENO + " text, " +
            ELA_COLUMN_ISVALID + " interger, " +
            ELA_COLUMN_ISVOTE +" integer);";

    public void deleteAllTransactions() {
        try {
            mDatabase = openDatabase();
//            mDatabase.delete(ELA_TX_TABLE_NAME, null, null);
            mDatabase.execSQL("DROP TABLE IF EXISTS " + ELA_TX_TABLE_NAME);
            mDatabase.execSQL(ELA_TX_DATABASE_CREATE);
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
