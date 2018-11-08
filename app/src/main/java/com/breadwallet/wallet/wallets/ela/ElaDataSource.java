package com.breadwallet.wallet.wallets.ela;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.wallets.ela.data.ElaTransactionEntity;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInputs;
import com.breadwallet.wallet.wallets.ela.response.history.History;
import com.breadwallet.wallet.wallets.ela.response.history.TxHistory;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 8/4/15.
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
public class ElaDataSource implements BRDataSourceInterface {

    private static final String TAG = ElaDataSource.class.getSimpleName();

    private static final String ELA_SERVER_URL = "http://WalletServiceTest-env-regtest3.jwpzumvc5i.ap-northeast-1.elasticbeanstalk.com:8080";

    private static final String ELA_HISTORY_URL = "http://54.64.220.165:8080";

    private static ElaDataSource mInstance;

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private static Context mContext;

    private ElaDataSource(Context context){
        mContext = context;
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static ElaDataSource getInstance(Context context){
        if(mInstance == null){
            mInstance = new ElaDataSource(context);
        }

        return mInstance;
    }

    private final String[] allColumns = {
            BRSQLiteHelper.ELA_COLUMN_ISRECEIVED,
            BRSQLiteHelper.ELA_COLUMN_TIMESTAMP,
            BRSQLiteHelper.ELA_COLUMN_BLOCKHEIGHT,
            BRSQLiteHelper.ELA_COLUMN_HASH,
            BRSQLiteHelper.ELA_COLUMN_TXREVERSED,
            BRSQLiteHelper.ELA_COLUMN_FEE,
            BRSQLiteHelper.ELA_COLUMN_TO,
            BRSQLiteHelper.ELA_COLUMN_FROM,
            BRSQLiteHelper.ELA_COLUMN_BALANCEAFTERTX,
            BRSQLiteHelper.ELA_COLUMN_TXSIZE,
            BRSQLiteHelper.ELA_COLUMN_AMOUNT,
            BRSQLiteHelper.ELA_COLUMN_ISVALID,
    };

    public void cacheSingleTx(ElaTransactionEntity entity){
        List<ElaTransactionEntity> entities = new ArrayList<>();
        entities.clear();
        entities.add(entity);
        cacheMultTx(entities);
    }

    public synchronized void cacheMultTx(List<ElaTransactionEntity> elaTransactionEntities){
        if(elaTransactionEntities == null) return;
        Cursor cursor = null;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(ElaTransactionEntity entity : elaTransactionEntities){
                cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME,
                        allColumns, BRSQLiteHelper.ELA_COLUMN_TXREVERSED + " = ? COLLATE NOCASE", new String[]{entity.txReversed}, null, null, null);

                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.ELA_COLUMN_ISRECEIVED, entity.isReceived? 1:0);
                value.put(BRSQLiteHelper.ELA_COLUMN_TIMESTAMP, entity.timeStamp);
                value.put(BRSQLiteHelper.ELA_COLUMN_BLOCKHEIGHT, entity.blockHeight);
                value.put(BRSQLiteHelper.ELA_COLUMN_HASH, entity.hash);
                value.put(BRSQLiteHelper.ELA_COLUMN_TXREVERSED, entity.txReversed);
                value.put(BRSQLiteHelper.ELA_COLUMN_FEE, entity.fee);
                value.put(BRSQLiteHelper.ELA_COLUMN_TO, entity.toAddress);
                value.put(BRSQLiteHelper.ELA_COLUMN_FROM, entity.fromAddress);
                value.put(BRSQLiteHelper.ELA_COLUMN_BALANCEAFTERTX, entity.balanceAfterTx);
                value.put(BRSQLiteHelper.ELA_COLUMN_TXSIZE, entity.txSize);
                value.put(BRSQLiteHelper.ELA_COLUMN_AMOUNT, entity.amount);
                value.put(BRSQLiteHelper.ELA_COLUMN_ISVALID, entity.isValid?1:0);

                long count = cursor.getCount();
                if(cursor!=null && count>=1) {
                    if(cursor.moveToFirst()){
                        ElaTransactionEntity curEntity = cursorToCurrency(cursor);
                        if(curEntity.blockHeight <= 0) {
                            int l = database.update(BRSQLiteHelper.ELA_TX_TABLE_NAME, value,BRSQLiteHelper.ELA_COLUMN_TXREVERSED + " = ? COLLATE NOCASE", new String[]{entity.txReversed});
                            Log.i(TAG, "l:" + l);
                        }
                    }
                } else {
                    long l = database.insertWithOnConflict(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                    Log.i(TAG, "l:"+l);
                }
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            database.endTransaction();
            closeDatabase();
            e.printStackTrace();
        } finally {
            cursor.close();
            database.endTransaction();
            closeDatabase();
        }

    }


    public List<ElaTransactionEntity> getAllTransactions(){
        List<ElaTransactionEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, allColumns, null, null, null, null, "timeStamp desc");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                ElaTransactionEntity curEntity = cursorToCurrency(cursor);
                currencies.add(curEntity);
                cursor.moveToNext();
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return currencies;
    }

    private ElaTransactionEntity cursorToCurrency(Cursor cursor) {
        return new ElaTransactionEntity(cursor.getInt(0)==1,
                cursor.getLong(1),
                cursor.getInt(2),
                cursor.getBlob(3),
                cursor.getString(4),
                cursor.getLong(5),
                cursor.getString(6),
                cursor.getString(7),
                cursor.getLong(8),
                cursor.getInt(9),
                cursor.getLong(10),
                cursor.getInt(11)==1);
    }

    @WorkerThread
    public String getElaBalance(String address){
        String balance = null;
        try {
            String url = ELA_SERVER_URL +"/api/1/balance/"+address;
            String result = urlGET(url);JSONObject object = new JSONObject(result);
            balance = object.getString("result");
        } catch (Exception e) {
            e.printStackTrace();
        }
        return balance;
    }

    public void getHistory(String address){
        try {
            String url = ELA_HISTORY_URL+"/history/"+address+"&pageNum=10&pageSize=1";
            String result = urlGET(url)/*getTxHistory()*/;
            JSONObject jsonObject = new JSONObject(result);
            String json = jsonObject.getString("result");
            TxHistory txHistory = new Gson().fromJson(json, TxHistory.class);

            List<ElaTransactionEntity> elaTransactionEntities = new ArrayList<>();
            elaTransactionEntities.clear();
            List<History> transactions = txHistory.History;
            for(History history : transactions){
                ElaTransactionEntity elaTransactionEntity = new ElaTransactionEntity();
                elaTransactionEntity.txReversed = history.Txid;
                elaTransactionEntity.isReceived = isReceived(history.Type);
                elaTransactionEntity.fromAddress = history.Inputs.get(0);
                elaTransactionEntity.toAddress = history.Outputs.get(0);
                elaTransactionEntity.fee = new BigDecimal(history.Fee).multiply(new BigDecimal(1000000000)).longValue();
                elaTransactionEntity.blockHeight = history.Height;
                elaTransactionEntity.hash = history.Txid.getBytes();
                elaTransactionEntity.txSize = 0;
                elaTransactionEntity.amount = new BigDecimal(history.Value).multiply(new BigDecimal(1000000000)).longValue();
                elaTransactionEntity.balanceAfterTx = 0;
                elaTransactionEntity.isValid = true;
                elaTransactionEntity.timeStamp = new BigDecimal(history.CreateTime).longValue();
                elaTransactionEntities.add(elaTransactionEntity);
            }
            cacheMultTx(elaTransactionEntities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private boolean isReceived(String type){
        if(type==null || type.equals("")) return false;
        if(type.equals("spend")) return false;
        if(type.equals("income")) return true;

        return true;
    }

    ElaTransactionEntity elaTransactionEntity = new ElaTransactionEntity();
    public synchronized BRElaTransaction createElaTx(final String inputAddress, final String outputsAddress, final long amount, String memo){
        BRElaTransaction brElaTransaction = null;
        try {
            String url = ELA_SERVER_URL +"/api/1/createTx";

            CreateTx tx = new CreateTx();
            tx.inputs.add(inputAddress);

            com.breadwallet.wallet.wallets.ela.request.Outputs outputs = new com.breadwallet.wallet.wallets.ela.request.Outputs();
            outputs.addr = outputsAddress;
            outputs.amt = amount;

            tx.outputs.add(outputs);

            String json = new Gson().toJson(tx);
            String result = urlPost(url, json)/*getCreateTx()*/;

            JSONObject jsonObject = new JSONObject(result);
            String tranactions = jsonObject.getString("result");
            ElaTransactionRes res = new Gson().fromJson(tranactions, ElaTransactionRes.class);
            List<ElaUTXOInputs> inputs = res.Transactions.get(0).UTXOInputs;
            for(int i=0; i<inputs.size(); i++){
                ElaUTXOInputs utxoInputs = inputs.get(i);
                utxoInputs.privateKey  = WalletElaManager.getPrivateKey();
            }

            String transactionJson =new Gson().toJson(res);

            brElaTransaction = new BRElaTransaction();
            brElaTransaction.setTx(transactionJson);
            brElaTransaction.setTxId(inputs.get(0).txid);

            elaTransactionEntity.txReversed = inputs.get(0).txid;
            elaTransactionEntity.fromAddress = inputAddress;
            elaTransactionEntity.toAddress = outputsAddress;
            elaTransactionEntity.isReceived = false;
            elaTransactionEntity.fee = new BigDecimal(res.Transactions.get(0).Fee).multiply(new BigDecimal(1000000000)).longValue();
            elaTransactionEntity.blockHeight = 0;
            elaTransactionEntity.hash = new byte[1];
            elaTransactionEntity.txSize = 0;
            elaTransactionEntity.amount = amount;
            elaTransactionEntity.balanceAfterTx = 0;
            elaTransactionEntity.timeStamp = System.currentTimeMillis()/1000;
            elaTransactionEntity.isValid = true;
        } catch (Exception e) {
            e.printStackTrace();
        }

        return brElaTransaction;
    }


    public synchronized String sendElaRawTx(final String transaction){

        String result = null;
        try {
            String url = ELA_SERVER_URL +"/api/1/sendRawTx";
            String rawTransaction = Utility.getInstance(mContext).generateRawTransaction(transaction);
            String json = "{"+"\"data\"" + ":" + "\"" + rawTransaction + "\"" +"}";
            Log.i(TAG, "rawTransaction:"+rawTransaction);
            String tmp = urlPost(url, json);
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getString("result");
            if(result==null || result.contains("ERROR") || result.contains(" ")) return null;
            elaTransactionEntity.txReversed = result;
            cacheSingleTx(elaTransactionEntity);
            Log.i("rawTx", "result:"+result);
        } catch (Exception e) {
            e.printStackTrace();
        }

        return result;
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



    //TODO test
    private String getCreateTx(){
        return "{\n" +
                "    \"result\": {\n" +
                "        \"Transactions\": [\n" +
                "            {\n" +
                "                \"UTXOInputs\": [\n" +
                "                    {\n" +
                "                        \"address\": \"EU3e23CtozdSvrtPzk9A1FeC9iGD896DdV\",\n" +
                "                        \"txid\": \"fa9bcb8b2f3a3a1e627284ad8425faf70fa64146b88a3aceac538af8bfeffd91\",\n" +
                "                        \"index\": 1\n" +
                "                    }\n" +
                "                ],\n" +
                "                \"Fee\": 100,\n" +
                "                \"Outputs\": [\n" +
                "                    {\n" +
                "                        \"amount\": 1000,\n" +
                "                        \"address\": \"EPzxJrHefvE7TCWmEGQ4rcFgxGeGBZFSHw\"\n" +
                "                    },\n" +
                "                    {\n" +
                "                        \"amount\": 99997800,\n" +
                "                        \"address\": \"EU3e23CtozdSvrtPzk9A1FeC9iGD896DdV\"\n" +
                "                    }\n" +
                "                ]\n" +
                "            }\n" +
                "        ]\n" +
                "    },\n" +
                "    \"status\": 200\n" +
                "}";
    }

    private String getTranactions(){
        return "{\n" +
                "    \"result\": [\n" +
                "        {\n" +
                "            \"vsize\": 288,\n" +
                "            \"locktime\": 0,\n" +
                "            \"txid\": \"64955791d225fddae4bba01547712c53f97ce3fb38252c01dbb9d6d9b7b982c8\",\n" +
                "            \"confirmations\": 13,\n" +
                "            \"type\": 2,\n" +
                "            \"version\": 0,\n" +
                "            \"vout\": [\n" +
                "                {\n" +
                "                    \"outputlock\": 0,\n" +
                "                    \"address\": \"8NJ7dbKsG2NRiBqdhY6LyKMiWp166cFBiG\",\n" +
                "                    \"assetid\": \"a3d0eaa466df74983b5d7c543de6904f4c9418ead5ffd6d25814234a96db37b0\",\n" +
                "                    \"value\": \"1\",\n" +
                "                    \"n\": 0\n" +
                "                },\n" +
                "                {\n" +
                "                    \"outputlock\": 0,\n" +
                "                    \"address\": \"EbxU18T3M9ufnrkRY7NLt6sKyckDW4VAsA\",\n" +
                "                    \"assetid\": \"a3d0eaa466df74983b5d7c543de6904f4c9418ead5ffd6d25814234a96db37b0\",\n" +
                "                    \"value\": \"977.89999500\",\n" +
                "                    \"n\": 1\n" +
                "                }\n" +
                "            ],\n" +
                "            \"blockhash\": \"75d78222e8f8b7622ab45902fd7a79c03edf08bceb1078335e9a8caf90cee612\",\n" +
                "            \"size\": 288,\n" +
                "            \"blocktime\": 1539919032,\n" +
                "            \"vin\": [\n" +
                "                {\n" +
                "                    \"sequence\": 0,\n" +
                "                    \"txid\": \"f176d04e5980828770acadcfc3e2d471885ab7358cd7d03f4f61a9cd0c593d54\",\n" +
                "                    \"vout\": 1\n" +
                "                }\n" +
                "            ],\n" +
                "            \"payloadversion\": 0,\n" +
                "            \"attributes\": [\n" +
                "                {\n" +
                "                    \"data\": \"e6b58be8af95\",\n" +
                "                    \"usage\": 129\n" +
                "                }\n" +
                "            ],\n" +
                "            \"time\": 1539919032,\n" +
                "            \"programs\": [\n" +
                "                {\n" +
                "                    \"code\": \"21021421976fdbe518ca4e8b91a37f1831ee31e7b4ba62a32dfe2f6562efd57806adac\",\n" +
                "                    \"parameter\": \"403792fa7dd7f29a810ab247e6476ca814ae51c550419f101948db6141004b364b645d84aaecdcb96790bd8cd7606dde04c7ca494ed51b893f460d06517778e8c1\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"hash\": \"64955791d225fddae4bba01547712c53f97ce3fb38252c01dbb9d6d9b7b982c8\"\n" +
                "        },\n" +
                "        \"Unknown Transaction\",\n" +
                "        {\n" +
                "            \"vsize\": 288,\n" +
                "            \"locktime\": 0,\n" +
                "            \"txid\": \"64955791d225fddae4bba01547712c53f97ce3fb38252c01dbb9d6d9b7b982c8\",\n" +
                "            \"confirmations\": 13,\n" +
                "            \"type\": 2,\n" +
                "            \"version\": 0,\n" +
                "            \"vout\": [\n" +
                "                {\n" +
                "                    \"outputlock\": 0,\n" +
                "                    \"address\": \"8NJ7dbKsG2NRiBqdhY6LyKMiWp166cFBiG\",\n" +
                "                    \"assetid\": \"a3d0eaa466df74983b5d7c543de6904f4c9418ead5ffd6d25814234a96db37b0\",\n" +
                "                    \"value\": \"1\",\n" +
                "                    \"n\": 0\n" +
                "                },\n" +
                "                {\n" +
                "                    \"outputlock\": 0,\n" +
                "                    \"address\": \"EbxU18T3M9ufnrkRY7NLt6sKyckDW4VAsA\",\n" +
                "                    \"assetid\": \"a3d0eaa466df74983b5d7c543de6904f4c9418ead5ffd6d25814234a96db37b0\",\n" +
                "                    \"value\": \"977.89999500\",\n" +
                "                    \"n\": 1\n" +
                "                }\n" +
                "            ],\n" +
                "            \"blockhash\": \"75d78222e8f8b7622ab45902fd7a79c03edf08bceb1078335e9a8caf90cee612\",\n" +
                "            \"size\": 288,\n" +
                "            \"blocktime\": 1539919032,\n" +
                "            \"vin\": [\n" +
                "                {\n" +
                "                    \"sequence\": 0,\n" +
                "                    \"txid\": \"f176d04e5980828770acadcfc3e2d471885ab7358cd7d03f4f61a9cd0c593d54\",\n" +
                "                    \"vout\": 1\n" +
                "                }\n" +
                "            ],\n" +
                "            \"payloadversion\": 0,\n" +
                "            \"attributes\": [\n" +
                "                {\n" +
                "                    \"data\": \"e6b58be8af95\",\n" +
                "                    \"usage\": 129\n" +
                "                }\n" +
                "            ],\n" +
                "            \"time\": 1539919032,\n" +
                "            \"programs\": [\n" +
                "                {\n" +
                "                    \"code\": \"21021421976fdbe518ca4e8b91a37f1831ee31e7b4ba62a32dfe2f6562efd57806adac\",\n" +
                "                    \"parameter\": \"403792fa7dd7f29a810ab247e6476ca814ae51c550419f101948db6141004b364b645d84aaecdcb96790bd8cd7606dde04c7ca494ed51b893f460d06517778e8c1\"\n" +
                "                }\n" +
                "            ],\n" +
                "            \"hash\": \"64955791d225fddae4bba01547712c53f97ce3fb38252c01dbb9d6d9b7b982c8\"\n" +
                "        }\n" +
                "    ],\n" +
                "    \"status\": 200\n" +
                "}";
    }

    private String getRawTx(){
        return "{\n" +
                "    \"result\": \"1f4432635bcf8c347f2bc20b7906c8c6c195f51beb3426e5f8d6a9e4cc073cf3\",\n" +
                "    \"status\": 200\n" +
                "}";
    }


    private String getTxHistory(){
        return "{\n" +
                "    \"result\":{\n" +
                "        \"History\":[\n" +
                "            {\n" +
                "                \"Txid\":\"9769f250d46eeb0342fcb34b74debe403e5c865b450e8fd3322e3540f2d1d9c7\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541567799\",\n" +
                "                \"Height\":156007,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"f96c2130021a41239ae064fc906ef1c73954c744278518e0a778ccdfc9408f2d\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541568324\",\n" +
                "                \"Height\":156012,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"bae9a5c6e4383fb3561ad8a21806e07cd8e6fcfea549fee771353cc0c93520b4\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541568639\",\n" +
                "                \"Height\":156017,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"c7dbd8ccda7118230d6f100746713f284adea64a636e44f4eeac71dbce18c3bf\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541569179\",\n" +
                "                \"Height\":156022,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"4677796dfa3b4d6531ad3cef5312efa93d23de376a7ec1c9963f36edbfb33068\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541569770\",\n" +
                "                \"Height\":156027,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"cb36cb4e6680f935bdd2254b1b811010e829f60e1b0dfea3575752e6ab796748\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541570397\",\n" +
                "                \"Height\":156037,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"1caf9a96dab101eee6e13f410e145bfdf4cd4caf792a9d0c3c9cb3a3baf35f6c\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541570904\",\n" +
                "                \"Height\":156042,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"9fb993922da03a97a901d07c6b8b02c54dedb5d0e0e0694328e5a72c6a8ea4f0\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541571444\",\n" +
                "                \"Height\":156047,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"072687994c76c3748cb59c593a5a69875582fa7fea3ad51b2567e8c1d602a9bd\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541571727\",\n" +
                "                \"Height\":156052,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"a73cffc87c8954527be16dea9ce2da59a95072c93629ddeaa9b0796b034d273e\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541572899\",\n" +
                "                \"Height\":156062,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"b8ddc126502bba37bcfbacfd3dadb75614cd072fc34a0abf92d638c1c820b6a6\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541573109\",\n" +
                "                \"Height\":156067,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"00227bd552df86a286849dfd166e345dc9b0ce882f63c610a0856348fbc2e5bf\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541573469\",\n" +
                "                \"Height\":156077,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"9aac13199e059258c4813c4ff0d373c1e62a23f0b31b01b47d85091cf7dafc46\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541573939\",\n" +
                "                \"Height\":156082,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"a60eb024765403ec41c2bef2c90b7c211697da5553fcd80e2f6278d459d4d46f\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541574129\",\n" +
                "                \"Height\":156087,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\",\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"8108a5875f8378ba3ec5553480c3568a934cffd8fe7175707c626c68da72c55d\",\n" +
                "                \"Type\":\"income\",\n" +
                "                \"Value\":\"0.01\",\n" +
                "                \"CreateTime\":\"1541574168\",\n" +
                "                \"Height\":156088,\n" +
                "                \"Fee\":\"0.001\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"ER1ouzeLNKQTqPrDHxgAGw2eiCXPhgznVy\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\",\n" +
                "                    \"ER1ouzeLNKQTqPrDHxgAGw2eiCXPhgznVy\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"4d79ce0da3292d3a4852e6a541b98ee8fdf07c4c67898283b74ac27b7572a8b4\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541574384\",\n" +
                "                \"Height\":156092,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"1e1ae04b87987b3872c5b33cd767601af6027be164e00ea81431b8f0eb6d6693\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541574804\",\n" +
                "                \"Height\":156097,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"bc3588b0fd110f558038bfa4b0ebac066fdf534064713a891d4ea02ba6c4f5cb\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541575437\",\n" +
                "                \"Height\":156102,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"a93aec9c3cb82bb52ba21ed152b3fecac117f8a94dfa952b696e9818bf7d3987\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541575584\",\n" +
                "                \"Height\":156107,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            },\n" +
                "            {\n" +
                "                \"Txid\":\"31164c6ebb9eaadc92b3b7310c2ea162ebc51af00668e2821b6e5e84a1e485e9\",\n" +
                "                \"Type\":\"spend\",\n" +
                "                \"Value\":\"0.0005\",\n" +
                "                \"CreateTime\":\"1541576218\",\n" +
                "                \"Height\":156112,\n" +
                "                \"Fee\":\"0.0005\",\n" +
                "                \"Inputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ],\n" +
                "                \"Outputs\":[\n" +
                "                    \"EN8WSL4Wt1gM3YjTcHgG7ckBiadtcaNgx4\"\n" +
                "                ]\n" +
                "            }\n" +
                "        ],\n" +
                "        \"TotalNum\":216\n" +
                "    },\n" +
                "    \"status\":200\n" +
                "}";
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
