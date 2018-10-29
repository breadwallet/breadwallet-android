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
import com.breadwallet.wallet.wallets.ela.request.Outputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInputs;
import com.breadwallet.wallet.wallets.ela.response.history.TransactionRes;
import com.breadwallet.wallet.wallets.ela.response.history.Txs;
import com.breadwallet.wallet.wallets.ela.response.history.Vin;
import com.breadwallet.wallet.wallets.ela.response.history.Vout;
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

public class ElaDataSource implements BRDataSourceInterface {

    private static final String TAG = ElaDataSource.class.getSimpleName();

    private static final String ELA_SERVER_URL = "http://WalletServiceTest-env-regtest3.jwpzumvc5i.ap-northeast-1.elasticbeanstalk.com:8080";

    private static final String ELA_HISTORY_URL = "http://blockchain-regtest3.elastos.org";

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

    public void getTransactions(String address){
        try {
            String url = ELA_HISTORY_URL+"/api/v1/txs/?address="+address+"&pageNum=0";
            String result = urlGET(url)/*getTxHistory()*/;
            TransactionRes transactionRes = new Gson().fromJson(result, TransactionRes.class);

            List<ElaTransactionEntity> elaTransactionEntities = new ArrayList<>();
            elaTransactionEntities.clear();
            List<Txs> transactions = transactionRes.txs;
            for(Txs txs : transactions){
                ElaTransactionEntity elaTransactionEntity = new ElaTransactionEntity();
                elaTransactionEntity.txReversed = txs.txid;
                elaTransactionEntity.fromAddress = txs.vin.get(0).addr;
                elaTransactionEntity.toAddress = txs.vout.get(0).scriptPubKey.addresses.get(0);
                elaTransactionEntity.isReceived = isReceived(address, txs.vin);
                elaTransactionEntity.fee = new BigDecimal(txs.fees).multiply(new BigDecimal(1000000000)).longValue();
                elaTransactionEntity.blockHeight = txs.confirmations+1;
                elaTransactionEntity.hash = txs.blockhash.getBytes();
                elaTransactionEntity.txSize = txs.size;
                elaTransactionEntity.amount = getAmount(address, elaTransactionEntity.isReceived, txs.vout);
                elaTransactionEntity.balanceAfterTx = 0;
                elaTransactionEntity.isValid = true;
                elaTransactionEntity.timeStamp = txs.time;
                elaTransactionEntities.add(elaTransactionEntity);
            }
            cacheMultTx(elaTransactionEntities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private long getAmount(String address, boolean isReceived, List<Vout> vouts){
        long amount = 0;
        if(isReceived){
            for(Vout vout : vouts){
                if(vout.scriptPubKey.addresses.get(0).equals(address)){
                    amount = amount+Long.valueOf(vout.valueSat);
                }
            }
        } else {
            for(Vout vout : vouts){
                if(!vout.scriptPubKey.addresses.get(0).equals(address)){
                    amount = amount+Long.valueOf(vout.valueSat);
                }
            }
        }

        return amount;
    }

    private boolean isReceived(String address, List<Vin> vins){
        if(vins==null || vins.size()<=0) return true;
        for(Vin vin : vins){
            if(vin.addr!=null && vin.addr.equals(address)) return false;
        }

        return true;
    }

    ElaTransactionEntity elaTransactionEntity = new ElaTransactionEntity();
    public synchronized BRElaTransaction createElaTx(final String inputAddress, final String outputsAddress, final long amount, String memo){
        BRElaTransaction brElaTransaction = null;
        try {
            String url = ELA_SERVER_URL +"/api/1/createTx";

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
        return "{\"pagesTotal\": 1, \"txs\": [{\"valueOut\": 5000000.0, \"isCoinBase\": false, \"vout\": [{\"valueSat\": \"39000000000000\", \"n\": 0, \"value\": 390000.0, \"scriptPubKey\": {\"type\": \"pubkeyhash\", \"addresses\": [\"ELANULLXXXXXXXXXXXXXXXXXXXXXYvs3rr\"]}}, {\"valueSat\": \"460999999999900\", \"n\": 1, \"value\": 4610000.0, \"scriptPubKey\": {\"type\": \"pubkeyhash\", \"addresses\": [\"8K9gkit8NPM5bD84wJuE5n7tS9Rdski5uB\"]}}], \"blockhash\": \"c3cb668258d9aaa2d07f84774153c868a36c66ce9cf8278549cb9134687f73e8\", \"time\": 1534823033, \"vin\": [{\"valueSat\": 500000000000000, \"txid\": \"116a841e8d7c0397070e73eff8a8799188a5fa5fa7f01f64546f8d28f2940d0e\", \"addr\": \"8K9gkit8NPM5bD84wJuE5n7tS9Rdski5uB\", \"value\": 5000000.0, \"n\": 2}], \"txid\": \"d73a3a6b1d25c310c3f6742ec917deba6a0842a8a7cd30819e3ff60b3aaa9275\", \"blocktime\": 1534823033, \"version\": 0, \"confirmations\": 55390, \"fees\": 1e-06, \"blockheight\": 173672, \"locktime\": 0, \"_id\": \"d73a3a6b1d25c310c3f6742ec917deba6a0842a8a7cd30819e3ff60b3aaa9275\", \"size\": 436}, {\"valueOut\": 33000221.0046, \"isCoinBase\": false, \"vout\": [{\"valueSat\": \"350000000000000\", \"n\": 0, \"value\": 3500000.0, \"scriptPubKey\": {\"type\": \"pubkeyhash\", \"addresses\": [\"8S7jTjYjqBhJpS9DxaZEbBLfAhvvyGypKx\"]}}, {\"valueSat\": \"1650000000000000\", \"n\": 1, \"value\": 16500000.0, \"scriptPubKey\": {\"type\": \"pubkeyhash\", \"addresses\": [\"8KNrJAyF4M67HT5tma7ZE4Rx9N9YzaUbtM\"]}}, {\"valueSat\": \"500000000000000\", \"n\": 2, \"value\": 5000000.0, \"scriptPubKey\": {\"type\": \"pubkeyhash\", \"addresses\": [\"8K9gkit8NPM5bD84wJuE5n7tS9Rdski5uB\"]}}, {\"valueSat\": \"800000000000000\", \"n\": 3, \"value\": 8000000.0, \"scriptPubKey\": {\"type\": \"pubkeyhash\", \"addresses\": [\"8cTn9JAGXfqGgu8kVUaPBJXrhSjoJR9ymG\"]}}, {\"valueSat\": \"22100455620\", \"n\": 4, \"value\": 221.0045562, \"scriptPubKey\": {\"type\": \"pubkeyhash\", \"addresses\": [\"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\"]}}], \"blockhash\": \"9f612455418c6853f2f0c92fda0052e37b2c047522e3af6adbb599f30e1b1cd3\", \"time\": 1513945687, \"vin\": [{\"valueSat\": 150684931, \"txid\": \"68be9d433a1268daee743b67ee2b3ee025a1c66417511327c8034e034091075b\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"bd8629d4e8e9bf86e1fbd98fed51b230034c2f024cc4bed3a26eb5a049b9df0d\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"04a6dd1f2264b85a0718b05cc8fdec772d0c61701b3313b4e280f1cb4432d433\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"917107613d122fb2b48d9d5f07033013291677c8018a6940f2a5763ce4547fd1\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"46790cd04a175521492af3c785b2a8b2987f9667eed00fa2c0b018112cec38ee\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"f531e31fa6f060b851cccd29a82c2ec7e31a317a38036a36320e600fbbeb2fdb\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"fdb31d8f9bfce07aada0a0b723e0ae9baaff71778e3c67f07f7f0ef6afc82550\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"604fa61adc5672b1928908fa094bd4dfd38c4f838092943463199b0759310f17\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"51b2b1d3deb91329f8593013b5784fd52f5bd531bf9cc1c43b8d7c70da7106bf\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"8cf498024df41254d6d6e80b7eca4a798d3e33cff3d0c008b778306118aa5640\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"e2351fae01b226c599e6fd26cedc118bb70c9ebd5ad23ff88f96b3726d91340e\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"0928ac4105ab900b8b2bebbf7dcfb49b65678e6fc01d21a7149fc80ee612a64e\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"72ddcc242bb0f164817e061070aca1d184fd20b3c1fb4447b22e0c30b9945b47\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"76aa7adb9923a293c391d9a233b40e7b710b58c81eb126c3c55ab112fff69c7b\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"d3801a816f947aec3f6b5f130ca9ea0afc1782d56bde1a81c4500315d6d4d39c\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"b95e33c8394e8bdf2f0cfaa52f6a27b5f58692775b39061c3c1fbaa7e3c10e03\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"2a4260bf8193211f0cd5e9c633810b77ba503e870794986e5b99364fd2a3d5ba\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"526f5b405660edbd75140e47b8aed1b0e544df979799a20b544b04a36695f0fb\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"a17caece6b1f118e416f99bc2074bee560fb711f8fb307873553d4f1d95a7f1e\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"bd86b5521a5fc104de1eb530b58eb56a12811b21199f13fb217adbb3695b1916\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"941a4a65f3ce30f2ef760d0af8485c572a91989e770048854f164857b488c7d6\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"1d664815cd89674f10da2f7636af3780e504307786d507e28dbf0422d7482909\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"3a762270f86cd9c6703cf806e71f4e01c568daeabc85b2569ddc5ffbdc61321a\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"fdd18bd1d72e3de2f48d3cbf9d5bfcfaa3aed942676886fba583462271b035fe\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"849ad6aeee33b8c118181b52d53d87e88515b9ec5993d0d19b09e79ffaded2df\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"adc61e93900fba0dfcfcf13965cca23519e1ef578147e81f1834b9d6d4a139c8\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"09d7a1c50d4cba3b3bdf940094924cbdc036aba9011ce16eb7edc414a1299f4a\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"5caa7dda5149a006cb0cb9b3da8d9bbdd1d7d7b6014fa3dc76e22ad10ff35232\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"2cf00362bb4b2b79679e3c6853f87b5164da03895dd65ed7b6c1f7c175042310\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"9e2ca1676bdc8bfb5b250b885d3d628aa7e53437bbe7c939934a1a18692fe816\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"fba9d4e944e3398b8676219e0c10f0215b87376e944658900b8c74f24639493d\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"ffe921538af23382c529675dd5f4e545c1900c6f0b4ea8083bab4e1a9d72e5cb\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"a8d0f192babd5d7c94519e89cf0370d55dcfe110a0264b71e344898af3c9e74c\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"e768ef02c9ad9222a28dc0b9d9f46492667db19ba1e64a28bb57996280af5b38\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"0caa80da9b7e29cf0a7461033f6c95d4c39206f450636d172f4aeb5d6f3d2661\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"c8f3a44754abb2c9e18eecb149e3e2615cf994d6011c3c30c9711a05f3402d5b\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"d0fd1e4e96788d7473a2435f9383b4e12e6aef11502a25af9d62ff8535cc0340\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"93502b7d5333369916f7569f0fea1958dd023613fa14c59bf509055be49e35ad\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"e9827723e247fa51ac82001047b0414e9811ffa965664d37948c18200f48443a\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"3c014b8fb922400ddbc7a156c72bba513f8fe868b762ac42b3bbe2dbd49ca197\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"a8b02112def3d0c12d788af3183ae365ddbafdef6c1b6e6c37a493e325a7a382\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"8f1ac4ec3967578c102c5299c67082f09dc8e0f1a30ed3603160d9034e387bdb\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"528af8bd136214ef9a421baf1029af16f964c6c627b1cad6476a0804276a42d8\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 150684931, \"txid\": \"5424ef986fa130564228924a77fa899212a1f26f765eccb5367ea9b6ef7fb202\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 1.50684931, \"n\": 0}, {\"valueSat\": 351598174, \"txid\": \"68be9d433a1268daee743b67ee2b3ee025a1c66417511327c8034e034091075b\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"a8d0f192babd5d7c94519e89cf0370d55dcfe110a0264b71e344898af3c9e74c\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"46790cd04a175521492af3c785b2a8b2987f9667eed00fa2c0b018112cec38ee\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"8cf498024df41254d6d6e80b7eca4a798d3e33cff3d0c008b778306118aa5640\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"8f1ac4ec3967578c102c5299c67082f09dc8e0f1a30ed3603160d9034e387bdb\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"e9827723e247fa51ac82001047b0414e9811ffa965664d37948c18200f48443a\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"a8b02112def3d0c12d788af3183ae365ddbafdef6c1b6e6c37a493e325a7a382\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"04a6dd1f2264b85a0718b05cc8fdec772d0c61701b3313b4e280f1cb4432d433\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"e768ef02c9ad9222a28dc0b9d9f46492667db19ba1e64a28bb57996280af5b38\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"adc61e93900fba0dfcfcf13965cca23519e1ef578147e81f1834b9d6d4a139c8\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"fdd18bd1d72e3de2f48d3cbf9d5bfcfaa3aed942676886fba583462271b035fe\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"0caa80da9b7e29cf0a7461033f6c95d4c39206f450636d172f4aeb5d6f3d2661\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"e2351fae01b226c599e6fd26cedc118bb70c9ebd5ad23ff88f96b3726d91340e\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"3c014b8fb922400ddbc7a156c72bba513f8fe868b762ac42b3bbe2dbd49ca197\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"76aa7adb9923a293c391d9a233b40e7b710b58c81eb126c3c55ab112fff69c7b\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"09d7a1c50d4cba3b3bdf940094924cbdc036aba9011ce16eb7edc414a1299f4a\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"d3801a816f947aec3f6b5f130ca9ea0afc1782d56bde1a81c4500315d6d4d39c\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"fba9d4e944e3398b8676219e0c10f0215b87376e944658900b8c74f24639493d\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"72ddcc242bb0f164817e061070aca1d184fd20b3c1fb4447b22e0c30b9945b47\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"3a762270f86cd9c6703cf806e71f4e01c568daeabc85b2569ddc5ffbdc61321a\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"2cf00362bb4b2b79679e3c6853f87b5164da03895dd65ed7b6c1f7c175042310\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"528af8bd136214ef9a421baf1029af16f964c6c627b1cad6476a0804276a42d8\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"849ad6aeee33b8c118181b52d53d87e88515b9ec5993d0d19b09e79ffaded2df\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"917107613d122fb2b48d9d5f07033013291677c8018a6940f2a5763ce4547fd1\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"bd8629d4e8e9bf86e1fbd98fed51b230034c2f024cc4bed3a26eb5a049b9df0d\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"941a4a65f3ce30f2ef760d0af8485c572a91989e770048854f164857b488c7d6\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"fdb31d8f9bfce07aada0a0b723e0ae9baaff71778e3c67f07f7f0ef6afc82550\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"604fa61adc5672b1928908fa094bd4dfd38c4f838092943463199b0759310f17\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"0928ac4105ab900b8b2bebbf7dcfb49b65678e6fc01d21a7149fc80ee612a64e\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"bd86b5521a5fc104de1eb530b58eb56a12811b21199f13fb217adbb3695b1916\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"1d664815cd89674f10da2f7636af3780e504307786d507e28dbf0422d7482909\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"526f5b405660edbd75140e47b8aed1b0e544df979799a20b544b04a36695f0fb\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"5caa7dda5149a006cb0cb9b3da8d9bbdd1d7d7b6014fa3dc76e22ad10ff35232\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"c8f3a44754abb2c9e18eecb149e3e2615cf994d6011c3c30c9711a05f3402d5b\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"5424ef986fa130564228924a77fa899212a1f26f765eccb5367ea9b6ef7fb202\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"2a4260bf8193211f0cd5e9c633810b77ba503e870794986e5b99364fd2a3d5ba\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"d0fd1e4e96788d7473a2435f9383b4e12e6aef11502a25af9d62ff8535cc0340\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"a17caece6b1f118e416f99bc2074bee560fb711f8fb307873553d4f1d95a7f1e\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"f531e31fa6f060b851cccd29a82c2ec7e31a317a38036a36320e600fbbeb2fdb\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"93502b7d5333369916f7569f0fea1958dd023613fa14c59bf509055be49e35ad\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"b95e33c8394e8bdf2f0cfaa52f6a27b5f58692775b39061c3c1fbaa7e3c10e03\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"51b2b1d3deb91329f8593013b5784fd52f5bd531bf9cc1c43b8d7c70da7106bf\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"ffe921538af23382c529675dd5f4e545c1900c6f0b4ea8083bab4e1a9d72e5cb\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 351598174, \"txid\": \"9e2ca1676bdc8bfb5b250b885d3d628aa7e53437bbe7c939934a1a18692fe816\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 3.51598174, \"n\": 1}, {\"valueSat\": 3300000000000000, \"txid\": \"953aff7026a339d7070e8b8d149ae4470f4a3d99f13298e28246c49020067cb0\", \"addr\": \"8VYXVxKKSAxkmRrfmGpQR2Kc66XhG6m3ta\", \"value\": 33000000.0, \"n\": 0}], \"txid\": \"116a841e8d7c0397070e73eff8a8799188a5fa5fa7f01f64546f8d28f2940d0e\", \"blocktime\": 1513945687, \"version\": 0, \"confirmations\": 228237, \"fees\": 1e-05, \"blockheight\": 825, \"locktime\": 0, \"_id\": \"116a841e8d7c0397070e73eff8a8799188a5fa5fa7f01f64546f8d28f2940d0e\", \"size\": 4074}]}";
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
