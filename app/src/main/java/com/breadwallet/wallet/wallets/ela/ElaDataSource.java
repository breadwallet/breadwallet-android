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
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.BRDataSourceInterface;
import com.breadwallet.tools.sqlite.BRSQLiteHelper;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.vote.ProducerEntity;
import com.breadwallet.vote.ProducersEntity;
import com.breadwallet.wallet.wallets.ela.data.HistoryTransactionEntity;
import com.breadwallet.wallet.wallets.ela.data.MultiTxProducerEntity;
import com.breadwallet.wallet.wallets.ela.data.TxProducerEntity;
import com.breadwallet.wallet.wallets.ela.data.TxProducersEntity;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.request.Outputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaOutputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.ElaUTXOInputs;
import com.breadwallet.wallet.wallets.ela.response.create.Meno;
import com.breadwallet.wallet.wallets.ela.response.create.Payload;
import com.breadwallet.wallet.wallets.ela.response.history.History;
import com.breadwallet.wallet.wallets.ela.response.history.TxHistory;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.elastos.sdk.keypair.ElastosKeypairSign;
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
    public static final String ELA_NODE = "api-wallet-ela.elastos.org" /*"api-wallet-ela-testnet.elastos.org"*/;

    private static ElaDataSource mInstance;

    private final BRSQLiteHelper dbHelper;

    private SQLiteDatabase database;

    private static Context mContext;

    private static Activity mActivity;

    private ElaDataSource(Context context){
        mContext = context;
        if(context instanceof Activity) mActivity = findActivity(context);
        dbHelper = BRSQLiteHelper.getInstance(context);
    }


    private static Activity findActivity(Context context) {
        if (context instanceof Activity) {
            return (Activity) context;
        }
        return HomeActivity.mHomeActivity;
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
            BRSQLiteHelper.ELA_COLUMN_ISVOTE
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


    public void cacheSingleTx(HistoryTransactionEntity entity){
        List<HistoryTransactionEntity> entities = new ArrayList<>();
        entities.clear();
        entities.add(entity);
        cacheMultTx(entities);
    }

    public synchronized void cacheMultTx(List<HistoryTransactionEntity> elaTransactionEntities){
        if(elaTransactionEntities == null) return;
//        Cursor cursor = null;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(HistoryTransactionEntity entity : elaTransactionEntities){
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
                value.put(BRSQLiteHelper.ELA_COLUMN_ISVOTE, entity.isVote?1:0);

                long l = database.insertWithOnConflict(BRSQLiteHelper.ELA_TX_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                Log.i(TAG, "l:"+l);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
//            cursor.close();
            database.endTransaction();
            closeDatabase();
        }

    }

//    public List<ProducerEntity> getCacheProducers(){
//        List<ProducerEntity> producers = new ArrayList<>();
//        Cursor cursor = null;
//        try {
//            database = openDatabase();
//            cursor = database.query(BRSQLiteHelper.ELA_PRODUCER_TABLE_NAME, allColumns, null, null, null, null, "rank desc");
//            if(null == cursor) return null;
//            cursor.moveToFirst();
//            while(cursor.isAfterLast()) {
//                ProducerEntity entity = cursorToProducerEntity(cursor);
//                producers.add(entity);
//            }
//            return producers;
//        } catch (Exception e) {
//            e.printStackTrace();
//        } finally {
//            if (cursor != null)
//                cursor.close();
//            closeDatabase();
//        }
//
//        return null;
//    }

    private TxProducerEntity cursorToTxProducerEntity(Cursor cursor){
        return new TxProducerEntity(cursor.getString(1),
                cursor.getString(2),
                cursor.getString(3));
    }

    private ProducerEntity cursorToProducerEntity(Cursor cursor) {
        return new ProducerEntity(cursor.getString(0),
                cursor.getString(1),
                cursor.getInt(2),
                cursor.getString(3),
                cursor.getString(4),
                cursor.getString(5));
    }

    public List<HistoryTransactionEntity> getHistoryTransactions(){
        List<HistoryTransactionEntity> currencies = new ArrayList<>();
        Cursor cursor = null;

        try {
            database = openDatabase();
            cursor = database.query(BRSQLiteHelper.ELA_TX_TABLE_NAME, allColumns, null, null, null, null, "timeStamp desc");

            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                HistoryTransactionEntity curEntity = cursorToTxEntity(cursor);
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

    private HistoryTransactionEntity cursorToTxEntity(Cursor cursor) {
        return new HistoryTransactionEntity(cursor.getInt(0)==1,
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
                cursor.getInt(12)==1,
                cursor.getInt(13)==1);
    }

    private void toast(final String message){
        Log.i("ElaDataApi", "message:"+message);
        if(mActivity !=null)
            mActivity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    Toast.makeText(mContext, message, Toast.LENGTH_SHORT).show();
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

    static class ProducerTxid {
        public List<String> txid;
    }

    public void getProducerByTxid(){
        if(mVoteTxid.size() <= 0) return;
        MultiTxProducerEntity multiTxProducerEntity = null;
        try {
            ProducerTxid producerTxid = new ProducerTxid();
            producerTxid.txid = mVoteTxid;
            String json = new Gson().toJson(producerTxid);
            String url = getUrl("api/1/dpos/transaction/producer");
            String result = urlPost(url, json);
            multiTxProducerEntity = new Gson().fromJson(result, MultiTxProducerEntity.class);
        } catch (Exception e) {
            e.printStackTrace();
        }
        if(multiTxProducerEntity==null || multiTxProducerEntity.result==null) return;
        cacheMultiTxProducer(multiTxProducerEntity.result);
    }

    //TODO test
    public void getProducerByTxid(String txid){
        try {
            ProducerTxid producerTxid = new ProducerTxid();
            producerTxid.txid = new ArrayList<>();
            producerTxid.txid.add(txid);
            String json = new Gson().toJson(producerTxid);
            String url = getUrl("api/1/dpos/transaction/producer");
            String result = urlPost(url, json);
            Log.i("test", "test");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<String> mVoteTxid = new ArrayList<>();
    public void getHistory(String address){
        if(StringUtil.isNullOrEmpty(address)) return;
        mVoteTxid.clear();
        try {
            String url = getUrl("api/1/history/"+address /*+"?pageNum=1&pageSize=10"*/);
            Log.i(TAG, "history url:"+url);
            String result = urlGET(url);
            JSONObject jsonObject = new JSONObject(result);
            String json = jsonObject.getString("result");
            TxHistory txHistory = new Gson().fromJson(json, TxHistory.class);

            List<HistoryTransactionEntity> elaTransactionEntities = new ArrayList<>();
            elaTransactionEntities.clear();
            List<History> transactions = txHistory.History;
            for(History history : transactions){
                HistoryTransactionEntity historyTransactionEntity = new HistoryTransactionEntity();
                historyTransactionEntity.txReversed = history.Txid;
                historyTransactionEntity.isReceived = isReceived(history.Type);
                historyTransactionEntity.fromAddress = isReceived(history.Type) ? history.Inputs.get(0) : history.Outputs.get(0);
                historyTransactionEntity.toAddress = isReceived(history.Type) ? history.Inputs.get(0) : history.Outputs.get(0);
                historyTransactionEntity.fee = new BigDecimal(history.Fee).longValue();
                historyTransactionEntity.blockHeight = history.Height;
                historyTransactionEntity.hash = history.Txid.getBytes();
                historyTransactionEntity.txSize = 0;
                historyTransactionEntity.amount = isReceived(history.Type) ? new BigDecimal(history.Value).longValue() : new BigDecimal(history.Value).subtract(new BigDecimal(history.Fee)).longValue();
                historyTransactionEntity.balanceAfterTx = 0;
                historyTransactionEntity.isValid = true;
                historyTransactionEntity.isVote = !isReceived(history.Type) && isVote(history.TxType);
                historyTransactionEntity.timeStamp = new BigDecimal(history.CreateTime).longValue();
                historyTransactionEntity.memo = getMeno(history.Memo);
                elaTransactionEntities.add(historyTransactionEntity);
                if(historyTransactionEntity.isVote) mVoteTxid.add(history.Txid);
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
        if(StringUtil.isNullOrEmpty(type)) return false;
        if(type.equals("spend")) return false;
        if(type.equals("income")) return true;

        return true;
    }

    private boolean isVote(String type){
        if(!StringUtil.isNullOrEmpty(type)){
            if(type.equals("Vote")) return true;
        }
        return false;
    }

    public synchronized BRElaTransaction createElaTx(final String inputAddress, final String outputsAddress, final long amount, String memo){
        return createElaTx(inputAddress, outputsAddress, amount, memo, null);
    }

    HistoryTransactionEntity historyTransactionEntity = new HistoryTransactionEntity();
    public synchronized BRElaTransaction createElaTx(final String inputAddress, final String outputsAddress, final long amount, String memo, List<String> payload){
        if(StringUtil.isNullOrEmpty(inputAddress) || StringUtil.isNullOrEmpty(outputsAddress)) return null;
        BRElaTransaction brElaTransaction = null;
        if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_sending));
        try {
            String url = getUrl("api/1/createVoteTx");
            Log.i(TAG, "create tx url:"+url);
            CreateTx tx = new CreateTx();
            tx.inputs.add(inputAddress);

            Outputs outputs = new Outputs();
            outputs.addr = outputsAddress;
            outputs.amt = amount;

            tx.outputs.add(outputs);

            String json = new Gson().toJson(tx);
            Log.d("posvote", "request json:"+json);
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

            if(null!=payload && payload.size()>0){
                List<ElaOutputs> outputsR = res.Transactions.get(0).Outputs;
                if(outputsR.size() > 1) {
                    ElaOutputs output = outputsR.get(1);
                    Payload tmp = new Payload();
                    tmp.candidatePublicKeys = payload;
                    output.payload = tmp;
                }
            }

            String transactionJson =new Gson().toJson(res);
            Log.d("posvote", "create json:"+transactionJson);

            brElaTransaction = new BRElaTransaction();
            brElaTransaction.setTx(transactionJson);
            brElaTransaction.setTxId(inputs.get(0).txid);

            historyTransactionEntity.txReversed = inputs.get(0).txid;
            historyTransactionEntity.fromAddress = inputAddress;
            historyTransactionEntity.toAddress = outputsAddress;
            historyTransactionEntity.isReceived = false;
            historyTransactionEntity.fee = new BigDecimal("4860").longValue();
            historyTransactionEntity.blockHeight = 0;
            historyTransactionEntity.hash = new byte[1];
            historyTransactionEntity.txSize = 0;
            historyTransactionEntity.amount = new BigDecimal(amount).longValue();
            historyTransactionEntity.balanceAfterTx = 0;
            historyTransactionEntity.timeStamp = System.currentTimeMillis()/1000;
            historyTransactionEntity.isValid = true;
            historyTransactionEntity.isVote = (payload!=null && payload.size()>0);
            historyTransactionEntity.memo = memo;
        } catch (Exception e) {
            if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_failed));
            e.printStackTrace();
        }

        return brElaTransaction;
    }


    public synchronized String sendElaRawTx(final String transaction){

        String result = null;
        try {
            String url = getUrl("api/1/sendRawTx");
            Log.i(TAG, "send raw url:"+url);
            String rawTransaction = ElastosKeypairSign.generateRawTransaction(transaction);
            String json = "{"+"\"data\"" + ":" + "\"" + rawTransaction + "\"" +"}";
            Log.i(TAG, "rawTransaction:"+rawTransaction);
            String tmp = urlPost(url, json);
            JSONObject jsonObject = new JSONObject(tmp);
            result = jsonObject.getString("result");
            if(result==null || result.contains("ERROR") || result.contains(" ")) {
                Thread.sleep(3000);
                if(mActivity!=null) toast(mActivity.getString(R.string.double_spend));
//                toast(result);
                return null;
            }
            historyTransactionEntity.txReversed = result;
            cacheSingleTx(historyTransactionEntity);
            Log.d("posvote", "txId:"+result);
        } catch (Exception e) {
            if(mActivity!=null) toast(mActivity.getResources().getString(R.string.SendTransacton_failed));
            e.printStackTrace();
        }

        return result;
    }

    public void getProducers(){
        try {
            String jsonRes = urlGET(getUrl("api/1/dpos/rank/height/9999999999999999"));
            if(!StringUtil.isNullOrEmpty(jsonRes) && jsonRes.contains("result")) {
                ProducersEntity producersEntity = new Gson().fromJson(jsonRes, ProducersEntity.class);
                List list = producersEntity.result;
                if(list==null || list.size()<=0) return;
                cacheProducer(list);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public List<TxProducerEntity> getTxProducerByTxid(String txid){
        if(StringUtil.isNullOrEmpty(txid)) return null;
        List<TxProducerEntity> entities = new ArrayList<>();
        Cursor cursor = null;
        try {
            cursor = database.query(BRSQLiteHelper.HISTORY_PRODUCER_TABLE_NAME,
                    null, BRSQLiteHelper.HISTORY_PRODUCER_TXID + " = ?", new String[]{txid},
                    null, null, null);
            cursor.moveToFirst();
            while (!cursor.isAfterLast()) {
                TxProducerEntity producerEntity = cursorToTxProducerEntity(cursor);
                entities.add(producerEntity);
                cursor.moveToNext();
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return entities;
    }

    public List<ProducerEntity> getProducersByPK(List<String> publicKeys){
        if(publicKeys==null || publicKeys.size()<=0) return null;
        List<ProducerEntity> entities = new ArrayList<>();
        Cursor cursor = null;
        try {
            database = openDatabase();

            for(String publickey : publicKeys){
                cursor = database.query(BRSQLiteHelper.ELA_PRODUCER_TABLE_NAME,
                        null, BRSQLiteHelper.PEODUCER_PUBLIC_KEY + " = ?", new String[]{publickey},
                        null, null, null);
                cursor.moveToFirst();
                while (!cursor.isAfterLast()) {
                    ProducerEntity producerEntity = cursorToProducerEntity(cursor);
                    entities.add(producerEntity);
                    cursor.moveToNext();
                }
            }
        } catch (Exception e){
            e.printStackTrace();
        } finally {
            if (cursor != null)
                cursor.close();
            closeDatabase();
        }

        return entities;
    }

    public void deleteAllTxProducer() {
        try {
            database = openDatabase();
            database.delete(BRSQLiteHelper.HISTORY_PRODUCER_TABLE_NAME, null, null);
        } finally {
            closeDatabase();
        }
    }

    public void cacheMultiTxProducer(List<TxProducersEntity> entities){
        if(entities==null || entities.size()<=0) return;
        try {
            database = openDatabase();
            database.beginTransaction();
            for(TxProducersEntity txProducersEntity : entities){
                if(null==txProducersEntity.Producer || StringUtil.isNullOrEmpty(txProducersEntity.Txid)) break;
                for(TxProducerEntity txProducerEntity : txProducersEntity.Producer){
                    ContentValues value = new ContentValues();
                    value.put(BRSQLiteHelper.HISTORY_PRODUCER_TXID, txProducersEntity.Txid);
                    value.put(BRSQLiteHelper.HISTORY_PRODUCER_OWN_PUBLICKEY, txProducerEntity.Ownerpublickey);
                    value.put(BRSQLiteHelper.HISTORY_PRODUCER_NOD_PUBLICKEY, txProducerEntity.Nodepublickey);
                    value.put(BRSQLiteHelper.HISTORY_PRODUCER_NICKNAME, txProducerEntity.Nickname);
                    long l = database.insertWithOnConflict(BRSQLiteHelper.HISTORY_PRODUCER_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
                }
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
    }

    public synchronized void cacheProducer(List<ProducerEntity> values){
        if(values==null || values.size()<=0) return;
        try {
            database = openDatabase();
            database.beginTransaction();

            for(ProducerEntity entity : values) {
                ContentValues value = new ContentValues();
                value.put(BRSQLiteHelper.PEODUCER_PUBLIC_KEY, entity.Producer_public_key);
                value.put(BRSQLiteHelper.PEODUCER_VALUE, entity.Value);
                value.put(BRSQLiteHelper.PEODUCER_RANK, entity.Rank);
                value.put(BRSQLiteHelper.PEODUCER_ADDRESS, entity.Address);
                value.put(BRSQLiteHelper.PEODUCER_NICKNAME, entity.Nickname);
                value.put(BRSQLiteHelper.PEODUCER_VOTES, entity.Votes);
                long l = database.insertWithOnConflict(BRSQLiteHelper.ELA_PRODUCER_TABLE_NAME, null, value, SQLiteDatabase.CONFLICT_REPLACE);
            }
            database.setTransactionSuccessful();
        } catch (Exception e) {
            closeDatabase();
            e.printStackTrace();
        } finally {
            database.endTransaction();
            closeDatabase();
        }
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
