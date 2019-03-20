package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.ProfileDataSource;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import com.platform.APIClient;

import org.apache.shiro.crypto.hash.SimpleHash;
import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.HDWallet;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;
import org.wallet.library.utils.HexUtils;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;

import okhttp3.MediaType;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

public class TestWalletActivity extends BRActivity {

    private String mWords;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_wallet_activity_layout);

        mWords = getWords();
    }

    private String getWords(){
        InputStream inputStream = null;
        String fileName = "words/zh-BIP39Words.txt";

        StringBuffer content = new StringBuffer();
        try {
            inputStream = getResources().getAssets().open(fileName);
            if (inputStream != null) {
                InputStreamReader inputReader = new InputStreamReader(inputStream);
                BufferedReader buffreader = new BufferedReader(inputReader);
                String line;
                while ((line = buffreader.readLine()) != null)
                    content.append(line).append("\n");
            }
        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return content.toString();

    }

    public void genrateMnemonic(View view){
        String mnemonic = IdentityManager.getMnemonic("chinese", mWords);

        Log.i("test", "test");
    }

    public void singleWallet(View view){
        String mnemonic = "血 坝 告 售 代 讨 转 枝 欧 旦 诚 抱";
        String seed = IdentityManager.getSeed(mnemonic, "chinese", mWords, "");

        Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());

        BlockChainNode node = new BlockChainNode("https://api-wallet-ela-testnet.elastos.org");
        HDWallet singleWallet = identity.createSingleAddressWallet(seed, node);

        singleWallet.syncHistory();

        String address = singleWallet.getAddress(0, 0);

        long balance = singleWallet.getBalance("EdVgb5RTdmwKf79pEUdVNnFprWyvmr1hPc");

        Log.i("test", "test");
    }

    public void hdWallet(View view){
        String mnemonic = "血 坝 告 售 代 讨 转 枝 欧 旦 诚 抱";
        String seed = IdentityManager.getSeed(mnemonic, "chinese", mWords, "");

        Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());

        BlockChainNode node = new BlockChainNode("https://api-wallet-ela-testnet.elastos.org");
        HDWallet hdWallet = identity.createWallet(seed, 0, node);

        String[] usedAddress = hdWallet.getUsedAddresses();

        String[] unusedAddress = hdWallet.getUnUsedAddresses(10);

//        Transaction tx = new Transaction("");
        Log.i("test", "test");
    }

    public void getDid(View view){
        String mnemonic = "血 坝 告 售 代 讨 转 枝 欧 旦 诚 抱";
        String seed = IdentityManager.getSeed(mnemonic, "chinese", mWords, "");

        Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());

        BlockChainNode node = new BlockChainNode("https://api-wallet-ela-testnet.elastos.org");
        HDWallet singleWallet = identity.createSingleAddressWallet(seed, node);

        DidManager didManager = identity.createDidManager(seed);
        Did did = didManager.createDid(0);

        String appId = "fe2dad7890d9cf301be581d5db5ad23a5efac604a9bc6a1ed3d15b24b4782d8da78b5b09eb80134209fd536505658fa151f685a50627b4f32bda209e967fc44a";

        String json = "[{\"Key\":\"" + appId + "/nickName\", \"Value\":\"bob\"}]";
        final String info = did.signInfo(seed, json);

//        final String devUrl = "https://api-wallet-did.elastos.org/api/1/blockagent/upchain/data";
        final String testUrl = "https://api-wallet-did-testnet.elastos.org/api/1/blockagent/upchain/data";
        new Thread(new Runnable() {
            @Override
            public void run() {
                try {
                    String result = urlPost(testUrl, info);
                    Log.i("xidaokun", "result:"+result);
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }).start();

//        String txid = did.setInfo(seed, json, singleWallet);
//
//        did.syncInfo();
//
//        String name = did.getInfo("name");
//
//        Log.i("test", "test");
    }



    public void getAppId(View view){
//        String mn = "刊 属 避 等 非 住 粒 捕 纹 追 岭 索";
        String mn = "血 坝 告 售 代 讨 转 枝 欧 旦 诚 抱";
        String sinInfo = "org.elastos.elephant.wallet";
        String privateKey = Utility.getInstance(this).getSinglePrivateKey(mn);
        byte[] signed = Utility.getInstance(this).sign(privateKey, sinInfo.getBytes());
        final String signedStr = HexUtils.bytesToHex(signed);
        Log.i("xidaokun", "signedStr:"+signedStr);
    }

    public void rsaTest(View view){
        String mn = "血 坝 告 售 代 讨 转 枝 欧 旦 诚 抱";
        String privateKey = Utility.getInstance(this).getSinglePrivateKey(mn);
        String publicKey = Utility.getInstance(this).getPublicKeyFromPrivateKey(privateKey);
        String did = Utility.getInstance(this).getDid(publicKey);

        try {
            BRKeyStore.encryptByPublicKey(this, "aaaa".getBytes(), publicKey.getBytes());

            BRKeyStore.decryptByPrivateKey(this, privateKey.getBytes());

            Log.i("xidaokun", "xidaokun");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public void getValueByKey(View view){
        String mnemonic = "血 坝 告 售 代 讨 转 枝 欧 旦 诚 抱";
        String seed = IdentityManager.getSeed(mnemonic, "chinese", mWords, "");

        Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());

        BlockChainNode node = new BlockChainNode("https://api-wallet-ela-testnet.elastos.org");
        HDWallet singleWallet = identity.createSingleAddressWallet(seed, node);

        DidManager didManager = identity.createDidManager(seed);
        Did did = didManager.createDid(0);

        String appId = "fe2dad7890d9cf301be581d5db5ad23a5efac604a9bc6a1ed3d15b24b4782d8da78b5b09eb80134209fd536505658fa151f685a50627b4f32bda209e967fc44a";

        String json = "[{\"Key\":\"" + appId + "/nickName\", \"Value\":\"bob\"}]";
        final String info = did.signInfo(seed, json);

//        final String devUrl = "https://api-wallet-did.elastos.org/api/1/blockagent/upchain/data";
        final String testUrl = "https://api-wallet-did-testnet.elastos.org/api/1/blockagent/upchain/data";
//        new Thread(new Runnable() {
//            @Override
//            public void run() {
//                try {
//                    String result = ProfileDataSource.getInstance(this).;
//                    Log.i("xidaokun", "result:"+result);
//                } catch (IOException e) {
//                    e.printStackTrace();
//                }
//            }
//        }).start();
    }

    public static final MediaType JSON = MediaType.parse("application/json; charset=utf-8");
    public String urlPost(String url, String json) throws IOException {
        String author = createHeaderAuthor();
        RequestBody body = RequestBody.create(JSON, json);
        Request request = new Request.Builder()
                .url(url)
//                .header("X-Elastos-Agent-Auth", author)
                .post(body)
                .build();
        Response response = APIClient.elaClient.newCall(request).execute();
        if (response.isSuccessful()) {
            return response.body().string();
        } else {
            throw new IOException("Unexpected code " + response);
        }
    }


    static class Author {
        public String id;
        public String time;
        public String auth;
    }
    private String createHeaderAuthor(){
        String acc_id = "unCZRceA8o7dbny";
        String acc_secret = "qtvb4PlRVGLYYYQxyLIo3OgyKI7kUL";

        long time = new Date().getTime();
        String strTime = String.valueOf(time);

        SimpleHash hash = new SimpleHash("md5", acc_secret, strTime);
        String auth = hash.toHex();

        Author author = new Author();
        author.id = acc_id;
        author.auth = auth;
        author.time = strTime;

        return new Gson().toJson(author);
    }

}
