package com.breadwallet.tools.jsbridge;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.util.Log;
import android.webkit.JavascriptInterface;

import com.breadwallet.presenter.activities.ExploreWebActivity;
import com.breadwallet.presenter.activities.MultiSignCreateActivity;
import com.breadwallet.presenter.activities.MultiSignQrActivity;
import com.breadwallet.presenter.activities.MultiSignTxActivity;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.ElaDataSource;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.breadwallet.wallet.wallets.ela.request.CreateTx;
import com.breadwallet.wallet.wallets.ela.request.Outputs;
import com.breadwallet.wallet.wallets.ela.response.create.ElaTransactionRes;
import com.breadwallet.wallet.wallets.ela.response.create.Meno;
import com.google.gson.Gson;

import org.elastos.sdk.keypair.ElastosKeypair;
import org.elastos.sdk.keypair.ElastosKeypairSign;
import org.json.JSONObject;

public class JsInterface {
    private final String TAG = "JsInterface";
    private Activity mContext;

    public JsInterface(Activity c) {
        mContext = c;
    }

    private static byte[] hexStringToByteArray(String s) {
        int len = s.length();
        byte[] data = new byte[len/2];

        for(int i = 0; i < len; i+=2){
            data[i/2] = (byte) ((Character.digit(s.charAt(i), 16) << 4) + Character.digit(s.charAt(i+1), 16));
        }

        return data;
    }

    private boolean checkAppId(String publicKey, String appName, String appId) {
        if (StringUtil.isNullOrEmpty(appName) || StringUtil.isNullOrEmpty(appId)) {
            return false;
        }

        ElastosKeypair.Data data = new ElastosKeypair.Data();
        data.buf = appName.getBytes();

        ElastosKeypair.Data signed = new ElastosKeypair.Data();
        signed.buf = hexStringToByteArray(appId);

        return ElastosKeypair.verify(publicKey, data, data.buf.length, signed, signed.buf.length);
    }

    @JavascriptInterface
    public String getElaPublicKey(String publicKey, String appName, String appId) {
        if (!checkAppId(publicKey, appName, appId)) {
            Log.e(TAG,"check app id failed");
            return "";
        }
        return WalletElaManager.getInstance(mContext).getPublicKey();
    }


    @JavascriptInterface
    public void createMultiSignWallet(String publicKey, String appName, String appId, String[] publicKeys, int requiredCount) {
        if (!checkAppId(publicKey, appName, appId)) {
            Log.e(TAG,"check app id faile");
            return;
        }

        if (publicKeys == null || publicKeys.length == 0 || requiredCount <= 0) {
            Log.e(TAG, "please send public keys");
            return;
        }

        Intent intent = new Intent();
        intent.setClass(mContext, MultiSignCreateActivity.class);
        intent.putExtra("publicKeys", publicKeys);
        intent .putExtra("requiredCount", requiredCount);
        mContext.startActivityForResult(intent, ExploreWebActivity.REQUEST_CREATE_WALLET);

    }

    @JavascriptInterface
    public String createTransaction(String publicKey, String appName, String appId, String from, String to, long amount, String memo) {
        if (!checkAppId(publicKey, appName, appId)) {
            Log.e(TAG,"check app id failed");
            return "";
        }
        String pref = BRSharedPrefs.getMultiSignInfo(mContext, from);
        if(StringUtil.isNullOrEmpty(pref)) {
            Log.e(TAG, from + " is not created");
            return "";
        }

        String url = ElaDataSource.getUrl("api/1/createVoteTx");

        CreateTx tx = new CreateTx();
        tx.inputs.add(from);

        Outputs outputs = new Outputs();
        outputs.addr = to;
        outputs.amt = amount;

        tx.outputs.add(outputs);

        String body = new Gson().toJson(tx);
        Log.d(TAG, "createTx request json:"+body);
        try {
            String result = ElaDataSource.getInstance(mContext).urlPost(url, body);

            JSONObject jsonObject = new JSONObject(result);
            String transactions = jsonObject.getString("result");
            Log.d(TAG, "transaction:" + transactions);

            ElaTransactionRes res = new Gson().fromJson(transactions, ElaTransactionRes.class);
            if(!StringUtil.isNullOrEmpty(memo)) {
                res.Transactions.get(0).Memo = new Meno("text", memo).toString();
            }

            Intent intent = new Intent();
            intent.setClass(mContext, MultiSignTxActivity.class);
            intent.putExtra("tx", new Gson().toJson(res));
            mContext.startActivity(intent);

        } catch (Exception e) {
            e.printStackTrace();
        }
        return "";
    }

    public static class MultiSignParam {
        public String[] PublicKeys;
        public int RequiredCount;
    }

}
