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
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInputs;
import com.breadwallet.wallet.wallets.ela.response.tx.TransactionEntity;
import com.breadwallet.wallet.wallets.ela.response.tx.TransactionRes;
import com.breadwallet.wallet.wallets.ela.response.tx.Vout;
import com.breadwallet.wallet.wallets.ela.response.utxo.UtxoEntity;
import com.breadwallet.wallet.wallets.ela.response.utxo.UtxoRes;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.BitSet;
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

    public void putTransaction(List<ElaTransactionEntity> elaTransactionEntities){
        if(elaTransactionEntities == null) return;
        Cursor cursor = null;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(ElaTransactionEntity entity : elaTransactionEntities){
                cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME,
                        allColumns, BRSQLiteHelper.ELA_COLUMN_TXREVERSED + " = ?", new String[]{entity.txReversed}, null, null, null);

                if(cursor==null || cursor.getCount()>=1) continue;

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

                database.insertWithOnConflict(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
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


    public UtxoEntity getUtxos(String address){
        UtxoRes utxoEntity = null;
        try{
            String url = ELA_SERVIER_URL+"/api/1/utxos/"+address;
            String result = urlGET(url);
            utxoEntity = new Gson().fromJson(result, UtxoRes.class);
        } catch (Exception e) {
            e.printStackTrace();
        }

        if (utxoEntity==null || utxoEntity.result==null || utxoEntity.result.size()<=0) return null;
        return utxoEntity.result.get(0);

    }

    public TransactionEntity getTransactionsByTxids(List<String> txIds){
        if(txIds==null) return null;
        TransactionEntity transaction = null;
        try{
            String url = ELA_SERVIER_URL+"/api/1/tx";

            String json = new Gson().toJson(txIds);
            String result = urlPost(url, json);
            TransactionRes transactionRes = new Gson().fromJson(result, TransactionRes.class);


            if(transactionRes==null || transactionRes.result==null || transactionRes.result.size()<=0) return null;
            //test
            List<ElaTransactionEntity> elaTransactionEntities = new ArrayList<>();
            elaTransactionEntities.clear();
            long tmp = 0;
            BigDecimal balance = BRSharedPrefs.getCachedBalance(mContext, "ELA");
            for(TransactionEntity entity : transactionRes.result){
                BigDecimal b = new BigDecimal(entity.vout.get(1).value).multiply(new BigDecimal(100000000));
                ElaTransactionEntity elaTransactionEntity = new ElaTransactionEntity();
                elaTransactionEntity.toAddress = entity.vout.get(0).address;
                elaTransactionEntity.fromAddress = entity.vout.get(1).address;
                elaTransactionEntity.txSize = entity.vsize;
                elaTransactionEntity.hash = entity.blockhash.getBytes();
                elaTransactionEntity.blockHeight = entity.confirmations;
                elaTransactionEntity.timeStamp = entity.time;
                elaTransactionEntity.txReversed = entity.txid;
                elaTransactionEntity.balanceAfterTx = elaTransactionEntity.balanceAfterTx + b.longValue();
                elaTransactionEntity.fee = 100;
                elaTransactionEntity.isValid = true;
                elaTransactionEntity.isReceived = false;
                elaTransactionEntity.amount = balance.longValue() - elaTransactionEntity.balanceAfterTx;

                tmp = elaTransactionEntity.balanceAfterTx;
                elaTransactionEntities.add(elaTransactionEntity);
            }
            BRSharedPrefs.putCachedBalance(mContext, "ELA", new BigDecimal(tmp));
            putTransaction(elaTransactionEntities);
        }catch (Exception e){
            e.printStackTrace();
        }

        return transaction;
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
