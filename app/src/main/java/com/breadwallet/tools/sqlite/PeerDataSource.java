package com.breadwallet.tools.sqlite;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 9/25/15.
 * Copyright (c) 2016 breadwallet LLC
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

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.util.BRConstants;

import java.util.ArrayList;
import java.util.List;

public class PeerDataSource implements BRDataSourceInterface {
    private static final String TAG = PeerDataSource.class.getName();


    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.PEER_COLUMN_ID,
            BRSQLiteHelper.PEER_ADDRESS,
            BRSQLiteHelper.PEER_PORT,
            BRSQLiteHelper.PEER_TIMESTAMP,
            BRSQLiteHelper.PEER_ISO
    };

    private static PeerDataSource instance;

    public static PeerDataSource getInstance(Context context) {
        if (instance == null) {
            instance = new PeerDataSource(context);
        }
        return instance;
    }

    private PeerDataSource(Context context) {
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public void putPeers(Context app, String iso, PeerEntity[] peerEntities) {

        try {
            database = openDatabase();
            database.beginTransaction();
            for (PeerEntity p : peerEntities) {
//                Log.e(TAG,"sqlite peer saved: " + Arrays.toString(p.getPeerTimeStamp()));
                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.PEER_ADDRESS, p.getPeerAddress());
                values.put(BRSQLiteHelper.PEER_PORT, p.getPeerPort());
                values.put(BRSQLiteHelper.PEER_TIMESTAMP, p.getPeerTimeStamp());
                values.put(BRSQLiteHelper.PEER_ISO, iso.toUpperCase());
                database.insert(BRSQLiteHelper.PEER_TABLE_NAME, null, values);
            }

            database.setTransactionSuccessful();
        } catch (Exception ex) {
            BRReportsManager.reportBug(ex);
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
            closeDatabase();
        }

    }

    public void deletePeer(Context app, String iso, BRPeerEntity peerEntity) {
        try {
            database = openDatabase();
            long id = peerEntity.getId();
            Log.e(TAG, "Peer deleted with id: " + id);
            database.delete(BRSQLiteHelper.PEER_TABLE_NAME, BRSQLiteHelper.PEER_COLUMN_ID
                    + " = ? AND " + BRSQLiteHelper.PEER_ISO + " = ?", new String[]{String.valueOf(id), iso.toUpperCase()});
        } finally {
            closeDatabase();
        }

    }

    public void deleteAllPeers(Context app, String iso) {
        try {
            database = dbHelper.getWritableDatabase();
            database.delete(BRSQLiteHelper.PEER_TABLE_NAME, BRSQLiteHelper.PEER_ISO + " = ?", new String[]{iso.toUpperCase()});
        } finally {
            closeDatabase();
        }
    }

    public List<BRPeerEntity> getAllPeers(Context app, String iso) {
        List<BRPeerEntity> peers = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            cursor = database.query(BRSQLiteHelper.PEER_TABLE_NAME,
                    allColumns, BRSQLiteHelper.PEER_ISO + " = ?", new String[]{iso.toUpperCase()}, null, null, null);

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                BRPeerEntity peerEntity = cursorToPeer(cursor);
                peers.add(peerEntity);
                cursor.moveToNext();
            }
            // make sure to close the cursor

            Log.e(TAG, "peers: " + peers.size());
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return peers;
    }

    private BRPeerEntity cursorToPeer(Cursor cursor) {
        BRPeerEntity peerEntity = new BRPeerEntity(cursor.getBlob(1), cursor.getBlob(2), cursor.getBlob(3));
        peerEntity.setId(cursor.getInt(0));
        return peerEntity;
    }

    @Override
    public SQLiteDatabase openDatabase() {
//        if (mOpenCounter.incrementAndGet() == 1) {
        // Opening new database
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
//        }
//        Log.d("Database open counter: ",  String.valueOf(mOpenCounter.get()));
        return database;
    }

    @Override
    public void closeDatabase() {
//        if (mOpenCounter.decrementAndGet() == 0) {
        // Closing database
//            database.close();

//        }
//        Log.d("Database open counter: " , String.valueOf(mOpenCounter.get()));
    }
}