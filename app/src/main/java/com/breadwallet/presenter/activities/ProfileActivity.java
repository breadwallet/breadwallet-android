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
import com.breadwallet.tools.sqlite.ProfileDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.SettingsUtil;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;
import com.google.gson.Gson;

import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.HDWallet;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;

import java.util.ArrayList;
import java.util.List;

public class ProfileActivity extends BRActivity {

    private static final String TAG = ProfileActivity.class.getSimpleName();

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

        checkTx();
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

    private String getKeyVale(String path, String value){
        String appId = "fe2dad7890d9cf301be581d5db5ad23a5efac604a9bc6a1ed3d15b24b4782d8da78b5b09eb80134209fd536505658fa151f685a50627b4f32bda209e967fc44a";
        class Key {
            public String Key;
            public String Value;
        }

        Key key = new Key();
        key.Key = appId + "/" + value;
        key.Value = value;
        List<Key> keys = new ArrayList<>();
        keys.add(key);
        return new Gson().toJson(keys);
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("xidaokun", "requestCode:"+requestCode);
        if(resultCode != RESULT_OK) return;

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
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
                DidManager didManager = identity.createDidManager(seed);
                Did did = didManager.createDid(0);

                String json = null;
                if(BRConstants.PROFILE_REQUEST_NICKNAME == requestCode){
                    String value = BRSharedPrefs.getNickname(ProfileActivity.this);
                    json = getKeyVale("nickName", value);
                } else if(BRConstants.PROFILE_REQUEST_EMAIL == requestCode){
                    String value = BRSharedPrefs.getEmail(ProfileActivity.this);
                    json = getKeyVale("email", value);
                } else if(BRConstants.PROFILE_REQUEST_MOBILE == requestCode){
                    String value = BRSharedPrefs.getMobile(ProfileActivity.this);
                    json = getKeyVale("mobile", value);
                } else if(BRConstants.PROFILE_REQUEST_ID == requestCode){
                    String value = BRSharedPrefs.getID(ProfileActivity.this);
                    json = getKeyVale("idCard", value);
                }

                //此处需要判断类型
                String info = did.signInfo(seed, json);
                String txid = ProfileDataSource.getInstance(ProfileActivity.this).upchain(info);
                if(!StringUtil.isNullOrEmpty(txid)){
                    //TODO refresh UI
                    refreshState(requestCode, SettingsUtil.IS_SAVING, txid);
                    if(mHandler !=null){//null when activity finish
                        mHandler.sendEmptyMessage(0x01);
                    }


                }
            }
        });
    }

    private void refreshState(int requestCode, int state, String txid){
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
        if(!StringUtil.isNullOrEmpty(txid)) BRSharedPrefs.putCacheTxid(ProfileActivity.this, key, txid);
    }

    private int getCompleteCount(){
        int count = 0;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.NICKNAME_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.EMAIL_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.MOBILE_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.ID_STATE) == SettingsUtil.IS_COMPLETED) count++;

        return count;
    }

    private void checkTx(){
        boolean isExit = false;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.NICKNAME_STATE) == SettingsUtil.IS_SAVING){
            String txid  = BRSharedPrefs.getCacheTxid(this, BRSharedPrefs.NICKNAME_txid);
            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.NICKNAME_STATE, SettingsUtil.IS_COMPLETED);
            if(!StringUtil.isNullOrEmpty(txid)) {
                isExit = ProfileDataSource.getInstance(ProfileActivity.this).isTxExit(txid);
            }
        }

        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.EMAIL_STATE) == SettingsUtil.IS_SAVING){
            String txid  = BRSharedPrefs.getCacheTxid(this, BRSharedPrefs.EMAIL_txid);
            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.EMAIL_STATE, SettingsUtil.IS_COMPLETED);
            if(!StringUtil.isNullOrEmpty(txid)) {
                isExit = ProfileDataSource.getInstance(ProfileActivity.this).isTxExit(txid);
            }
        }

        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.MOBILE_STATE) == SettingsUtil.IS_SAVING){
            String txid  = BRSharedPrefs.getCacheTxid(this, BRSharedPrefs.MOBILE_txid);
            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.MOBILE_STATE, SettingsUtil.IS_COMPLETED);
            if(!StringUtil.isNullOrEmpty(txid)) {
                isExit = ProfileDataSource.getInstance(ProfileActivity.this).isTxExit(txid);
            }
        } else if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.ID_STATE) == SettingsUtil.IS_SAVING){
            String txid  = BRSharedPrefs.getCacheTxid(this, BRSharedPrefs.ID_txid);
            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.ID_STATE, SettingsUtil.IS_COMPLETED);
            if(!StringUtil.isNullOrEmpty(txid)) {
                isExit = ProfileDataSource.getInstance(ProfileActivity.this).isTxExit(txid);
            }
        }
        if(!isExit) return;
        if(mHandler !=null) mHandler.sendEmptyMessage(0x01);
    }

}
