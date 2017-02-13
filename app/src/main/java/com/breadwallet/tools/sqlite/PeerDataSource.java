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
import android.database.SQLException;
import android.database.sqlite.SQLiteDatabase;
import android.util.Log;

import com.breadwallet.presenter.entities.BRPeerEntity;
import com.breadwallet.presenter.entities.PeerEntity;
import com.google.firebase.crash.FirebaseCrash;

import java.util.ArrayList;
import java.util.List;

class PeerDataSource {
    private static final String TAG = PeerDataSource.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private final BRSQLiteHelper dbHelper;
    private final String[] allColumns = {
            BRSQLiteHelper.PEER_COLUMN_ID,
            BRSQLiteHelper.PEER_ADDRESS,
            BRSQLiteHelper.PEER_PORT,
            BRSQLiteHelper.PEER_TIMESTAMP
    };

    public PeerDataSource(Context context) {
        dbHelper = new BRSQLiteHelper(context);
    }

    public void putPeers(PeerEntity[] peerEntities) {
        database = dbHelper.getWritableDatabase();
        database.beginTransaction();
        try {
            for (PeerEntity p : peerEntities) {
//                Log.e(TAG,"sqlite peer saved: " + Arrays.toString(p.getPeerTimeStamp()));
                ContentValues values = new ContentValues();
                values.put(BRSQLiteHelper.PEER_ADDRESS, p.getPeerAddress());
                values.put(BRSQLiteHelper.PEER_PORT, p.getPeerPort());
                values.put(BRSQLiteHelper.PEER_TIMESTAMP, p.getPeerTimeStamp());
                database.insert(BRSQLiteHelper.PEER_TABLE_NAME, null, values);
            }

            database.setTransactionSuccessful();
        } catch (Exception ex) {
            FirebaseCrash.report(ex);
            Log.e(TAG, "Error inserting into SQLite", ex);
            //Error in between database transaction
        } finally {
            database.endTransaction();
        }

    }

    public void deletePeer(BRPeerEntity peerEntity) {
        database = dbHelper.getWritableDatabase();
        long id = peerEntity.getId();
        Log.e(TAG, "Peer deleted with id: " + id);
        database.delete(BRSQLiteHelper.PEER_TABLE_NAME, BRSQLiteHelper.PEER_COLUMN_ID
                + " = " + id, null);
    }

    public void deleteAllPeers() {
        database = dbHelper.getWritableDatabase();
        database.delete(BRSQLiteHelper.PEER_TABLE_NAME, BRSQLiteHelper.PEER_COLUMN_ID + " <> -1", null);
    }

    public List<BRPeerEntity> getAllPeers() {
        database = dbHelper.getReadableDatabase();
        List<BRPeerEntity> peers = new ArrayList<>();

        Cursor cursor = database.query(BRSQLiteHelper.PEER_TABLE_NAME,
                allColumns, null, null, null, null, null);

        cursor.moveToFirst();
        while (!cursor.isAfterLast()) {
            BRPeerEntity peerEntity = cursorToPeer(cursor);
            peers.add(peerEntity);
            cursor.moveToNext();
        }
        // make sure to close the cursor

        Log.e(TAG, "peers: " + peers.size());
        cursor.close();
        return peers;
    }

    private BRPeerEntity cursorToPeer(Cursor cursor) {
        BRPeerEntity peerEntity = new BRPeerEntity(cursor.getBlob(1), cursor.getBlob(2), cursor.getBlob(3));
        peerEntity.setId(cursor.getInt(0));
        return peerEntity;
    }
}