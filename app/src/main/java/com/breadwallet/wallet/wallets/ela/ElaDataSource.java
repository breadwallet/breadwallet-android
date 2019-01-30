package com.breadwallet.wallet.wallets.ela;

import android.app.Activity;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.support.annotation.WorkerThread;
import android.util.Log;
import android.widget.Toast;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.wallets.ela.data.ElaTransactionEntity;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInputs;
import com.breadwallet.wallet.wallets.ela.response.create.Meno;
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

    public static final String ELA_NODE_KEY = "elaNodeKey";
//    https://api-wallet-ela.elastos.org
//    https://api-wallet-did.elastos.org
//    hw-ela-api-test.elastos.org
//    https://api-wallet-ela-testnet.elastos.org/api/1/currHeight
//    https://api-wallet-did-testnet.elastos.org/api/1/currHeight
    public static final String ELA_NODE = "api-wallet-ela.elastos.org";

    private static ElaDataSource mInstance;

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private static Context mContext;

    private static Activity mActivity;

    private ElaDataSource(Context context){
        mContext = context;
        if(context instanceof Activity) mActivity = (Activity) context;
        dbHelper = BRSQLiteHelper.getInstance(context);
    }

    public static ElaDataSource getInstance(Context context){
        if(mInstance == null){
            mInstance = new ElaDataSource(context);
        }

        return mInstance;
    }

    public static String getUrl(String api){
        String node = BRSharedPrefs.getElaNode(mContext, ELA_NODE_KEY);
        if(StringUtil.isNullOrEmpty(node)) node = ELA_NODE;
        return new StringBuilder("https://").append(node).append("/").append(api).toString();
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
            BRSQLiteHelper.ELA_COLUMN_MENO,
            BRSQLiteHelper.ELA_COLUMN_ISVALID,
    };

    public void deleteElaTable(){
        try {
            database = openDatabase();
            database.execSQL("drop table " + BRSQLiteHelper.ELA_TX_TABLE_NAME);
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            closeDatabase();
        }

    }

    public void deleteAllTransactions() {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, null);
        } finally {
            closeDatabase();
        }
    }


    public void cacheSingleTx(ElaTransactionEntity entity){
        List<ElaTransactionEntity> entities = new ArrayList<>();
        entities.clear();
        entities.add(entity);
        cacheMultTx(entities);
    }

    public synchronized void cacheMultTx(List<ElaTransactionEntity> elaTransactionEntities){
        if(elaTransactionEntities == null) return;
//        Cursor cursor = null;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(ElaTransactionEntity entity : elaTransactionEntities){
//                cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME,
//                        allColumns, BRSQLiteHelper.ELA_COLUMN_TXREVERSED + " = ? COLLATE NOCASE", new String[]{entity.txReversed}, null, null, null);

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
                value.put(BRSQLiteHelper.ELA_COLUMN_MENO, entity.memo);
                value.put(BRSQLiteHelper.ELA_COLUMN_ISVALID, entity.isValid?1:0);

                long l = database.insertWithOnConflict(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                Log.i(TAG, "l:"+l);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            database.endTransaction();
            closeDatabase();
            e.printStackTrace();
        } finally {
//            cursor.close();
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
                cursor.getString(11),
                cursor.getInt(12)==1);
    }

    private void toast(final String message){
        if(mActivity !=null)
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mActivity, message, Toast.LENGTH_SHORT).show();
                }
            });
    }

    @WorkerThread
    public String getElaBalance(String address){
        if(address==null || address.isEmpty()) return null;
        String balance = null;
        try {
            String url = getUrl("api/1/balance/"+address);
            Log.i(TAG, "balance url:"+url);
            String result = urlGET(url);
            JSONObject object = new JSONObject(result);
            balance = object.getString("result");
            Log.i(TAG, "balance:"+balance);
            int status = object.getInt("status");
            if(result==null || !result.contains("result") || !result.contains("status") || balance==null || status!=200) {
                toast("balance crash result:");
                throw new Exception("address:"+ address + "\n" + "result:" +result);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return balance;
    }

    public void getHistory(String address){
        if(address == null) return;
        try {
            String url = getUrl("api/1/history/"+address /*+"?pageNum=1&pageSize=10"*/);
            Log.i(TAG, "history url:"+url);
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
                elaTransactionEntity.fromAddress = isReceived(history.Type) ? history.Inputs.get(0) : history.Outputs.get(0);
                elaTransactionEntity.toAddress = isReceived(history.Type) ? history.Inputs.get(0) : history.Outputs.get(0);
                elaTransactionEntity.fee = new BigDecimal(history.Fee).longValue();
                elaTransactionEntity.blockHeight = history.Height;
                elaTransactionEntity.hash = history.Txid.getBytes();
                elaTransactionEntity.txSize = 0;
                elaTransactionEntity.amount = isReceived(history.Type) ? new BigDecimal(history.Value).longValue() : new BigDecimal(history.Value).subtract(new BigDecimal(history.Fee)).longValue();
                elaTransactionEntity.balanceAfterTx = 0;
                elaTransactionEntity.isValid = true;
                elaTransactionEntity.timeStamp = new BigDecimal(history.CreateTime).longValue();
                elaTransactionEntity.memo = getMeno(history.Memo);
                elaTransactionEntities.add(elaTransactionEntity);
            }
            cacheMultTx(elaTransactionEntities);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private String getMeno(String value){
        if(value==null || !value.contains("msg") || !value.contains("type") || !value.contains(",")) return "";
        if(value.contains("msg:")){
            String[] msg = value.split("msg:");
            if(msg!=null && msg.length==2){
                return msg[1];
            }
        }
        return "";
    }

    //true is receive
    private boolean isReceived(String type){
        if(type==null || type.equals("")) return false;
        if(type.equals("spend")) return false;
        if(type.equals("income")) return true;

        return true;
    }

    ElaTransactionEntity elaTransactionEntity = new ElaTransactionEntity();
    public synchronized BRElaTransaction createElaTx(final String inputAddress, final String outputsAddress, final long amount, String memo){
        if(StringUtil.isNullOrEmpty(inputAddress) || StringUtil.isNullOrEmpty(outputsAddress)) return null;
        BRElaTransaction brElaTransaction = null;
        if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_sending));
        try {
            String url = getUrl("api/1/createTx");
            Log.i(TAG, "create tx url:"+url);
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
            if(!StringUtil.isNullOrEmpty(memo)) res.Transactions.get(0).Memo = new Meno("text", memo).toString();
            List<ElaUTXOInputs> inputs = res.Transactions.get(0).UTXOInputs;
            for(int i=0; i<inputs.size(); i++){
                ElaUTXOInputs utxoInputs = inputs.get(i);
                utxoInputs.privateKey  = WalletElaManager.getInstance(mContext).getPrivateKey();
            }

            String transactionJson =new Gson().toJson(res);

            brElaTransaction = new BRElaTransaction();
            brElaTransaction.setTx(transactionJson);
            brElaTransaction.setTxId(inputs.get(0).txid);

            elaTransactionEntity.txReversed = inputs.get(0).txid;
            elaTransactionEntity.fromAddress = inputAddress;
            elaTransactionEntity.toAddress = outputsAddress;
            elaTransactionEntity.isReceived = false;
            elaTransactionEntity.fee = new BigDecimal("4860").longValue();
            elaTransactionEntity.blockHeight = 0;
            elaTransactionEntity.hash = new byte[1];
            elaTransactionEntity.txSize = 0;
            elaTransactionEntity.amount = new BigDecimal(amount).longValue();
            elaTransactionEntity.balanceAfterTx = 0;
            elaTransactionEntity.timeStamp = System.currentTimeMillis()/1000;
            elaTransactionEntity.isValid = true;
            elaTransactionEntity.memo = memo;
        } catch (Exception e) {
            toast(/*mActivity.getResources().getString(R.string.SendTransacton_failed)*/e.getMessage());
            e.printStackTrace();
        }

        return brElaTransaction;
    }


    public synchronized String sendElaRawTx(final String transaction){

        String result = null;
        try {
            String url = getUrl("api/1/sendRawTx");
            Log.i(TAG, "send raw url:"+url);
            String rawTransaction = Utility.getInstance(mContext).generateRawTransaction(transaction);
            String json = "{"+"\"data\"" + ":" + "\"" + rawTransaction + "\"" +"}";
            Log.i(TAG, "rawTransaction:"+rawTransaction);
            String tmp = urlPost(url, json);
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getString("result");
            if(result==null || result.contains("ERROR") || result.contains(" ")) {
                toast(result);
                return null;
            }
            elaTransactionEntity.txReversed = result;
            cacheSingleTx(elaTransactionEntity);
            Log.i("rawTx", "result:"+result);
        } catch (Exception e) {
            toast(/*mActivity.getResources().getString(R.string.SendTransacton_failed)*/e.getMessage());
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
