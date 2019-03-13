package com.breadwallet.presenter.activities;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;

import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.HDWallet;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

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
        String json = "[{\"Key\":\"nickName\", \"Value\":\"bob\"}]";
        String info = did.signInfo(seed, json);




//        String txid = did.setInfo(seed, json, singleWallet);
//
//        did.syncInfo();
//
//        String name = did.getInfo("name");
//
//        Log.i("test", "test");
    }
}
