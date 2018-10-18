package com.breadwallet.wallet.wallets.ela;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;

import com.breadwallet.BreadApp;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.wallets.ela.data.ElaTransactionEntity;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.request.Outputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaOutputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInputs;
import com.breadwallet.wallet.wallets.ela.response.history.HistoryTx;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.Iterator;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class ElaDataSource implements BRDataSourceInterface {

    private static final String TAG = ElaDataSource.class.getSimpleName();

    private static final String ELA_SERVIER_URL = "http://WalletServiceTest-env.jwpzumvc5i.ap-northeast-1.elasticbeanstalk.com:8080";

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

    public void putTransaction(ElaTransactionEntity entity){
        if(entity == null) return;
        try {
            database = openDatabase();
            database.beginTransaction();

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

            long failed = database.insertWithOnConflict(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
            if (failed != 0) Log.e(TAG, "putCurrencies: failed:" + failed);
            database.setTransactionSuccessful();
        } catch (Exception e) {
            database.endTransaction();
            closeDatabase();
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }

    }

    public List<ElaTransactionEntity> getAllTransactions(){
        List<ElaTransactionEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, allColumns, null, null, null, null, null);

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
            String url = ELA_SERVIER_URL+"/api/1/balance/"+address;
            String result = urlGET(url);
            JSONObject object = new JSONObject(result);
            balance = object.getString("result");
        } catch (JSONException e) {
            e.printStackTrace();
        }
        return balance;
    }

    public void getHistoryTx(String txId){
        try {
            String url = ELA_SERVIER_URL+"/api/1/tx/"+txId;
            String result = urlGET(url)/*getHistoryTx()*/;
            JSONObject object = new JSONObject(result);
            String tmp = object.getString("result");
            HistoryTx historyTx = new Gson().fromJson(tmp, HistoryTx.class);

            ElaTransactionEntity transactionEntity = new ElaTransactionEntity();
            transactionEntity.amount = historyTx.payload.CrossChainAmounts.get(0);
            transactionEntity.balanceAfterTx = 1;
            transactionEntity.blockHeight = 0;
            transactionEntity.fee = 100;
            transactionEntity.fromAddress = historyTx.payload.CrossChainAddresses.get(0);
            transactionEntity.toAddress = historyTx.vout.get(0).address;
            transactionEntity.hash = historyTx.hash.getBytes();
            transactionEntity.isValid = true;
            transactionEntity.timeStamp = historyTx.time;
            transactionEntity.txReversed = historyTx.txid;
            transactionEntity.isReceived = false;
            transactionEntity.txSize = historyTx.size;

            putTransaction(transactionEntity);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }


    public BRElaTransaction createElaTx(final String inputAddress, final String outputsAddress, final int amount, String memo){
        BRElaTransaction brElaTransaction = null;
        try {
            String url = ELA_SERVIER_URL+"/api/1/createTx";

            CreateTx tx = new CreateTx();
            tx.inputs.add(inputAddress);

            Outputs outputs = new Outputs();
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
        } catch (Exception e) {
            e.printStackTrace();
        }

        return brElaTransaction;
    }


    public String sendElaRawTx(final String transaction){

        String result = null;
        try {
            String url = ELA_SERVIER_URL+"/api/1/sendRawTx";
            String rawTransaction = Utility.generateRawTransaction(transaction);
            String json = "{"+"\"data\"" + ":" + "\"" + rawTransaction + "\"" +"}";
            String tmp = urlPost(url, json)/*getRawTx()*/;
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getString("result");
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
    public String urlGET(String myURL) {
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
        String bodyText = null;
        APIClient.BRResponse resp = APIClient.getInstance(mContext).sendRequest(request, false);

        try {
            bodyText = resp.getBodyText();
            String strDate = resp.getHeaders().get("date");
            if (strDate == null) {
                Log.e(TAG, "urlGET: strDate is null!");
                return bodyText;
            }
            SimpleDateFormat formatter = new SimpleDateFormat("EEE, dd MMM yyyy HH:mm:ss z", Locale.US);
            Date date = formatter.parse(strDate);
            long timeStamp = date.getTime();
            BRSharedPrefs.putSecureTime(mContext, timeStamp);
        } catch (ParseException e) {
            e.printStackTrace();
        }
        return bodyText;
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

    private String getRawTx(){
        return "{\n" +
                "    \"result\": \"1f4432635bcf8c347f2bc20b7906c8c6c195f51beb3426e5f8d6a9e4cc073cf3\",\n" +
                "    \"status\": 200\n" +
                "}";
    }

    private String getHistoryTx(){
        return "  {\n" +
                "    \"result\":{\n" +
                "        \"vsize\":346,\n" +
                "        \"locktime\":0,\n" +
                "        \"txid\":\"62637968e72b06e4fa1de91542a3b71bd2462ba1d29e9c14c2ecfd042d1937ab\",\n" +
                "        \"confirmations\":6756,\n" +
                "        \"type\":8,\n" +
                "        \"version\":0,\n" +
                "        \"vout\":[\n" +
                "            {\n" +
                "                \"outputlock\":0,\n" +
                "                \"address\":\"XQd1DCi6H62NQdWZQhJCRnrPn7sF9CTjaU\",\n" +
                "                \"assetid\":\"a3d0eaa466df74983b5d7c543de6904f4c9418ead5ffd6d25814234a96db37b0\",\n" +
                "                \"value\":\"0.10010000\",\n" +
                "                \"n\":0\n" +
                "            },\n" +
                "            {\n" +
                "                \"outputlock\":0,\n" +
                "                \"address\":\"EbxU18T3M9ufnrkRY7NLt6sKyckDW4VAsA\",\n" +
                "                \"assetid\":\"a3d0eaa466df74983b5d7c543de6904f4c9418ead5ffd6d25814234a96db37b0\",\n" +
                "                \"value\":\"0.50249300\",\n" +
                "                \"n\":1\n" +
                "            }\n" +
                "        ],\n" +
                "        \"blockhash\":\"4021e5c0ace86221016d3aa2b114adbd84bb03692bb6ddc6034794260834c570\",\n" +
                "        \"size\":346,\n" +
                "        \"blocktime\":1538279155,\n" +
                "        \"payload\":{\n" +
                "            \"CrossChainAddresses\":[\n" +
                "                \"EHLhCEbwViWBPwh1VhpECzYEA7jQHZ4zLv\"\n" +
                "            ],\n" +
                "            \"OutputIndexes\":[\n" +
                "                0\n" +
                "            ],\n" +
                "            \"CrossChainAmounts\":[\n" +
                "                10000000\n" +
                "            ]\n" +
                "        },\n" +
                "        \"vin\":[\n" +
                "            {\n" +
                "                \"sequence\":0,\n" +
                "                \"txid\":\"ba7bd41aae0a1371d9689ad04508f0754bb4a5333386411bccbdec718ce61625\",\n" +
                "                \"vout\":1\n" +
                "            }\n" +
                "        ],\n" +
                "        \"payloadversion\":0,\n" +
                "        \"attributes\":[\n" +
                "            {\n" +
                "                \"data\":\"32323432343239353130383035363838303230\",\n" +
                "                \"usage\":0\n" +
                "            }\n" +
                "        ],\n" +
                "        \"time\":1538279155,\n" +
                "        \"programs\":[\n" +
                "            {\n" +
                "                \"code\":\"21021421976fdbe518ca4e8b91a37f1831ee31e7b4ba62a32dfe2f6562efd57806adac\",\n" +
                "                \"parameter\":\"40cf6b8a18c861fcad1c23816221cc40a0d2e7d43065c070e66905ff7d6c634068542dd2a9b0bbb24de6a5a547b57767f908fc384cd6dc06298de11ebc3338aa79\"\n" +
                "            }\n" +
                "        ],\n" +
                "        \"hash\":\"62637968e72b06e4fa1de91542a3b71bd2462ba1d29e9c14c2ecfd042d1937ab\"\n" +
                "    },\n" +
                "    \"status\":200\n" +
                "}";
    }

    @Override
    public SQLiteDatabase openDatabase() {
        if (database == null || !database.isOpen())
            database = dbHelper.getWritableDatabase();
        dbHelper.setWriteAheadLoggingEnabled(BRConstants.WRITE_AHEAD_LOGGING);
//        }
//        Log.d("Database open counter: ",  String.valueOf(mOpenCounter.get()));
        return database;
    }

    @Override
    public void closeDatabase() {

    }
}
