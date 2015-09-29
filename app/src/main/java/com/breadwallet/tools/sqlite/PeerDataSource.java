package com.breadwallet.tools.sqlite;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/25/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
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

import com.breadwallet.tools.sqlite.entities.BRPeerEntity;

import java.util.ArrayList;
import java.util.List;

public class PeerDataSource {
    public static final String TAG = PeerDataSource.class.getName();

    // Database fields
    private SQLiteDatabase database;
    private BRSQLiteHelper dbHelper;
    private String[] allColumns = {
            BRSQLiteHelper.PEER_COLUMN_ID, BRSQLiteHelper.PEER_ADDRESS,
            BRSQLiteHelper.PEER_MISBEHAVIN, BRSQLiteHelper.PEER_PORT,
            BRSQLiteHelper.PEER_SERVICES, BRSQLiteHelper.PEER_TIME_STAMP
    };

    public PeerDataSource(Context context) {
        dbHelper = new BRSQLiteHelper(context);
    }

    public void open() throws SQLException {
        database = dbHelper.getWritableDatabase();
    }

    public void close() {
        dbHelper.close();
    }

    public BRPeerEntity createPeer(BRPeerEntity peer) {
        ContentValues values = new ContentValues();
//        values.put(BRSQLiteHelper.PEER_COLUMN_ID, peer.getId());
        values.put(BRSQLiteHelper.PEER_ADDRESS, peer.getAddress());
        values.put(BRSQLiteHelper.PEER_MISBEHAVIN, peer.getMisbehavin());
        values.put(BRSQLiteHelper.PEER_PORT, peer.getPort());
        values.put(BRSQLiteHelper.PEER_SERVICES, peer.getServices());
        values.put(BRSQLiteHelper.PEER_TIME_STAMP, peer.getTimeStamp());

        long insertId = database.insert(BRSQLiteHelper.PEER_TABLE_NAME, null, values);
        Cursor cursor = database.query(BRSQLiteHelper.PEER_TABLE_NAME,
                allColumns, BRSQLiteHelper.PEER_COLUMN_ID + " = " + insertId, null,
                null, null, null);
        cursor.moveToFirst();
        BRPeerEntity peerEntity = cursorToPeer(cursor);
        cursor.close();
        return peerEntity;
    }

    public void deletePeer(BRPeerEntity peerEntity) {
        long id = peerEntity.getId();
        Log.e(TAG, "Peer deleted with id: " + id);
        database.delete(BRSQLiteHelper.PEER_TABLE_NAME, BRSQLiteHelper.PEER_COLUMN_ID
                + " = " + id, null);
    }

    public List<BRPeerEntity> getAllPeers() {
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
        cursor.close();
        return peers;
    }

    private BRPeerEntity cursorToPeer(Cursor cursor) {
        BRPeerEntity peerEntity = new BRPeerEntity();
        peerEntity.setId(cursor.getInt(0));
        peerEntity.setAddress(cursor.getInt(1));
        peerEntity.setMisbehavin(cursor.getShort(2));
        peerEntity.setPort(cursor.getShort(3));
        peerEntity.setServices(cursor.getShort(4));
        peerEntity.setTimeStamp(cursor.getLong(5));
        return peerEntity;
    }
}