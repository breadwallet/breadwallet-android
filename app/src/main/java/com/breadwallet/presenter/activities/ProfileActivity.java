package com.breadwallet.presenter.activities;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
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
import java.util.Timer;
import java.util.TimerTask;

public class ProfileActivity extends BRActivity {

    private static final String TAG = ProfileActivity.class.getSimpleName();

    @SuppressLint("HandlerLeak")
    private Handler mHandler = new Handler(){
        @Override
        public void handleMessage(Message msg) {
            super.handleMessage(msg);
            Log.i("xidaokun", "adapter notify");
            mData.clear();
            mData.addAll(SettingsUtil.getProfileSettings(ProfileActivity.this));
            mAdapter.notifyDataSetChanged();
            mCountTv.setText(String.valueOf(getCompleteCount()));
            if(isSavingExit()) {
                startTimer();
            } else {
                stopTimerTask();
            }
        }
    };

    private List<BRSettingsItem> mData = new ArrayList<>();
    private SettingsAdapter mAdapter;
    private TextView mCountTv;

    private Timer timer;
    private TimerTask timerTask;

    public void startTimer() {
        Log.i("xidaokun", "startTimer");
        if (timer != null) return;
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, 30*1000);
    }

    public void stopTimerTask() {
        Log.i("xidaokun", "stopTimerTask");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.i("xidaokun", "isMainThread:"+(Looper.getMainLooper().getThread() == Thread.currentThread()));
                syncCheckTx();
            }
        };
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_profile_layout);
        ListView listView = findViewById(R.id.profile_list);
//        mData.addAll(SettingsUtil.getProfileSettings(ProfileActivity.this));
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

        mHandler.sendEmptyMessage(0x01);
        initDid();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimerTask();
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
        class KeyValue {
            public String Key;
            public String Value;
        }

        KeyValue key = new KeyValue();
        key.Key = appId + "/" + path;
        key.Value = value;
        List<KeyValue> keys = new ArrayList<>();
        keys.add(key);
        return new Gson().toJson(keys);
    }

    private String getIdKeyValue(String path, String name, String code){
        String appId = "fe2dad7890d9cf301be581d5db5ad23a5efac604a9bc6a1ed3d15b24b4782d8da78b5b09eb80134209fd536505658fa151f685a50627b4f32bda209e967fc44a";
        class IDcard {
            public String name;
            public String code;
        }
        class KeyValue {
            public String Key;
            public IDcard Value;
        }

        IDcard iDcard = new IDcard();
        iDcard.name = name;
        iDcard.code = code;

        KeyValue keyValue = new KeyValue();
        keyValue.Key = appId + "/" + path;
        keyValue.Value = iDcard;

        List<KeyValue> keyValues = new ArrayList<>();
        return new Gson().toJson(keyValues);
    }

    private Did mDid;
    private String mSeed;
    private void initDid(){
        String mnemonic = getMn();
        String language = Utility.detectLang(ProfileActivity.this, mnemonic);
        String words = Utility.getWords(ProfileActivity.this, language);
        mSeed = IdentityManager.getSeed(mnemonic, language, words, "");
        Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());
        DidManager didManager = identity.createDidManager(mSeed);
        mDid = didManager.createDid(0);
    }

    private String uploadData(String data){
        Log.i("xidaokun", "uploadData");
        String info = mDid.signInfo(mSeed, data);
        String txid = ProfileDataSource.getInstance(ProfileActivity.this).upchain(info);
        Log.i("xidaokun", "txid:"+txid);

        return txid;
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("xidaokun", "requestCode:"+requestCode);
        if(resultCode != RESULT_OK) return;
        if(null == data) return;

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    Thread.sleep(1000);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }

                String txid = null;
                if(BRConstants.PROFILE_REQUEST_NICKNAME == requestCode){
                    String nickname = data.getStringExtra("nickname");
                    if(StringUtil.isNullOrEmpty(nickname)) return;
                    String data = getKeyVale("NickName", nickname);
                    txid = uploadData(data);
                    if(!StringUtil.isNullOrEmpty(txid)){
                        BRSharedPrefs.putNickname(ProfileActivity.this, nickname);
                        BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.NICKNAME_STATE, SettingsUtil.IS_SAVING);
                        BRSharedPrefs.putCacheTxid(ProfileActivity.this, BRSharedPrefs.NICKNAME_txid, txid);
                    }

                } else if(BRConstants.PROFILE_REQUEST_EMAIL == requestCode){
                    String email = data.getStringExtra("email");
                    if(StringUtil.isNullOrEmpty(email)) return;
                    String data = getKeyVale("Email", email);
                    txid = uploadData(data);
                    if(!StringUtil.isNullOrEmpty(txid)){
                        BRSharedPrefs.putEmail(ProfileActivity.this, email);
                        BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.EMAIL_STATE, SettingsUtil.IS_SAVING);
                        BRSharedPrefs.putCacheTxid(ProfileActivity.this, BRSharedPrefs.EMAIL_txid, txid);
                    }

                } else if(BRConstants.PROFILE_REQUEST_MOBILE == requestCode){
                    String mobile = data.getStringExtra("mobile");
                    if(StringUtil.isNullOrEmpty(mobile)) return;
                    String data = getKeyVale("Mobile", mobile);
                    txid = uploadData(data);
                    if(!StringUtil.isNullOrEmpty(txid)){
                        BRSharedPrefs.putMobile(ProfileActivity.this, mobile);
                        BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.MOBILE_STATE, SettingsUtil.IS_SAVING);
                        BRSharedPrefs.putCacheTxid(ProfileActivity.this, BRSharedPrefs.MOBILE_txid, txid);
                    }

                } else if(BRConstants.PROFILE_REQUEST_ID == requestCode){
                    String realname = data.getStringExtra("realname");
                    String idcard = data.getStringExtra("idcard");
                    if(StringUtil.isNullOrEmpty(realname) || StringUtil.isNullOrEmpty(idcard)) return;
                    String data = getIdKeyValue("/ChineseIDCard", realname, idcard);
                    txid = uploadData(data);
                    if(!StringUtil.isNullOrEmpty(txid)){
                        BRSharedPrefs.putRealname(ProfileActivity.this, realname);
                        BRSharedPrefs.putID(ProfileActivity.this, idcard);
                        BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.EMAIL_STATE, SettingsUtil.IS_SAVING);
                        BRSharedPrefs.putCacheTxid(ProfileActivity.this, BRSharedPrefs.ID_txid, txid);
                    }
                }

                if(!StringUtil.isNullOrEmpty(txid)){
                    //TODO refresh UI
                    if(mHandler !=null){//null when activity finish
                        mHandler.sendEmptyMessage(0x01);
                    }
                }
            }
        });
    }

    private int getCompleteCount(){
        int count = 0;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.NICKNAME_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.EMAIL_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.MOBILE_STATE) == SettingsUtil.IS_COMPLETED) count++;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.ID_STATE) == SettingsUtil.IS_COMPLETED) count++;

        return count;
    }

    private boolean isSavingExit(){
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.NICKNAME_STATE) == SettingsUtil.IS_SAVING) return true;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.EMAIL_STATE) == SettingsUtil.IS_SAVING) return true;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.MOBILE_STATE) == SettingsUtil.IS_SAVING) return true;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.ID_STATE) == SettingsUtil.IS_SAVING) return true;

        return false;
    }

    private void syncCheckTx(){
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                checkTx();
            }
        });
    }

    private void checkTx(){
        Log.i("xidaokun", "checkTx");
        boolean isExit = false;
        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.NICKNAME_STATE) == SettingsUtil.IS_SAVING){
            String txid  = BRSharedPrefs.getCacheTxid(this, BRSharedPrefs.NICKNAME_txid);
            if(!StringUtil.isNullOrEmpty(txid)) {
                isExit = ProfileDataSource.getInstance(ProfileActivity.this).isTxExit(txid);
                if(isExit) BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.NICKNAME_STATE, SettingsUtil.IS_COMPLETED);
            }
        }

        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.EMAIL_STATE) == SettingsUtil.IS_SAVING){
            String txid  = BRSharedPrefs.getCacheTxid(this, BRSharedPrefs.EMAIL_txid);
            if(!StringUtil.isNullOrEmpty(txid)) {
                isExit = ProfileDataSource.getInstance(ProfileActivity.this).isTxExit(txid);
                if(isExit) BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.EMAIL_STATE, SettingsUtil.IS_COMPLETED);
            }
        }

        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.MOBILE_STATE) == SettingsUtil.IS_SAVING){
            String txid  = BRSharedPrefs.getCacheTxid(this, BRSharedPrefs.MOBILE_txid);
            if(!StringUtil.isNullOrEmpty(txid)) {
                isExit = ProfileDataSource.getInstance(ProfileActivity.this).isTxExit(txid);
                if(isExit) BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.MOBILE_STATE, SettingsUtil.IS_COMPLETED);
            }
        }

        if(BRSharedPrefs.getProfileState(this, BRSharedPrefs.ID_STATE) == SettingsUtil.IS_SAVING){
            String txid  = BRSharedPrefs.getCacheTxid(this, BRSharedPrefs.ID_txid);
            if(!StringUtil.isNullOrEmpty(txid)) {
                isExit = ProfileDataSource.getInstance(ProfileActivity.this).isTxExit(txid);
                if(isExit) BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.ID_STATE, SettingsUtil.IS_COMPLETED);
            }
        }
        if(!isExit) return;
        Log.i("xidaokun", "tx is exit");
        if(mHandler !=null) mHandler.sendEmptyMessage(0x01);
    }

}
