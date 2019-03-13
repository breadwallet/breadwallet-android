package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Message;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.ListView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.tools.adapter.SettingsAdapter;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.SettingsUtil;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;

import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.HDWallet;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BRActivity {

    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i("xidaokun", "adapter notify");
            mData.clear();
            mData.addAll(SettingsUtil.getProfileSettings(ProfileActivity.this));
            mAdapter.notifyDataSetChanged();
            mCountTv.setText(String.valueOf(getCompleteCount()));
        }
    };

    private List<BRSettingsItem> mData = new ArrayList<>();
    private SettingsAdapter mAdapter;
    private TextView mCountTv;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_layout);
        ListView listView = findViewById(R.id.profile_list);
        mData.addAll(SettingsUtil.getProfileSettings(ProfileActivity.this));
        mAdapter = new SettingsAdapter(this, R.layout.settings_list_item, mData);
        listView.setAdapter(mAdapter);
        findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                finish();
            }
        });
        mCountTv = findViewById(R.id.profile_complete_count);

        mCountTv.setText(String.valueOf(getCompleteCount()));
    }

    private String getMn(){
        byte[] phrase = null;
        try {
            phrase = BRKeyStore.getPhrase(this, 0);
            if(phrase != null) {
                return new String(phrase);
            }
        } catch (UserNotAuthenticatedException e) {
            e.printStackTrace();
        }
        return null;
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("xidaokun", "requestCode:"+requestCode);
        if(resultCode != RESULT_OK) return;

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                refreshState(requestCode, SettingsUtil.IS_SAVING);
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
                if(mHandler !=null){//null when activity finish
                    mHandler.sendEmptyMessage(0x01);
                }

                String mnemonic = getMn();
                String words = Utility.getWords(ProfileActivity.this, Utility.detectLang(ProfileActivity.this, mnemonic));
                String seed = IdentityManager.getSeed(mnemonic, "chinese", words, "");
                Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());
                BlockChainNode node = new BlockChainNode("https://api-wallet-ela-testnet.elastos.org");
                HDWallet singleWallet = identity.createSingleAddressWallet(seed, node);
                DidManager didManager = identity.createDidManager(seed);
                Did did = didManager.createDid(0);
                String json = "[{\"Key\":\"name\", \"Value\":\"bob\"}]";
                String info = did.signInfo(seed, json);

                String txid = did.setInfo(seed, json, singleWallet);

                did.syncInfo();


                //TODO 成功为IS_COMPLETED， 失败为IS_PENDING
                refreshState(requestCode, SettingsUtil.IS_COMPLETED);
                Log.i("xidaokun", "5s finish");
                if(mHandler !=null){//null when activity finish
                    mHandler.sendEmptyMessage(0x01);
                }
            }
        });
    }

    private void refreshState(int requestCode, int state){
        String key = null;
        if(BRConstants.PROFILE_REQUEST_NICKNAME == requestCode){
            key = BRSharedPrefs.NICKNAME_STATE;
        } else if(BRConstants.PROFILE_REQUEST_EMAIL == requestCode){
            key = BRSharedPrefs.EMAIL_STATE;
        } else if(BRConstants.PROFILE_REQUEST_MOBILE == requestCode){
            key = BRSharedPrefs.MOBILE_STATE;
        } else if(BRConstants.PROFILE_REQUEST_ID == requestCode){
            key = BRSharedPrefs.ID_STATE;
        }
        if(!StringUtil.isNullOrEmpty(key)) BRSharedPrefs.putProfileState(ProfileActivity.this, key, state);
    }

    private int getCompleteCount(){
        int count = 0;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.NICKNAME_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.EMAIL_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.MOBILE_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.ID_STATE) == SettingsUtil.IS_COMPLETED) count++;

        return count;
    }

}
