package com.breadwallet.presenter.activities;

import android.annotation.SuppressLint;
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

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
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
import com.google.gson.reflect.TypeToken;

import org.elastos.sdk.wallet.BlockChainNode;
import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
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
            Log.i("ProfileFunction", "adapter notify");
            mData.clear();
            mData.addAll(SettingsUtil.getProfileSettings(ProfileActivity.this));
            mAdapter.notifyDataSetChanged();
            int count = getCompleteCount();
            mCountTv.setText(String.valueOf(count));
            mCreditsTv.setBackgroundResource((count==0)?R.drawable.ic_profile_credits_gray_icon:R.drawable.ic_profile_credits_golden_icon);
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
    private BaseTextView mCreditsTv;

    public void startTimer() {
        Log.i("ProfileFunction", "startTimer");
        if (timer != null) return;
        timer = new Timer();
        initializeTimerTask();
        timer.schedule(timerTask, 1000, 30*1000);
    }

    public void stopTimerTask() {
        Log.i("ProfileFunction", "stopTimerTask");
        if (timer != null) {
            timer.cancel();
            timer = null;
        }
    }

    private void initializeTimerTask() {
        timerTask = new TimerTask() {
            public void run() {
                Log.i("ProfileFunction", "isMainThread:"+(Looper.getMainLooper().getThread() == Thread.currentThread()));
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
        mCreditsTv = findViewById(R.id.profile_credits_icon);

        int count = getCompleteCount();
        mCountTv.setText(String.valueOf(count));
        mCreditsTv.setBackgroundResource((count==0)?R.drawable.ic_profile_credits_gray_icon:R.drawable.ic_profile_credits_golden_icon);

        mHandler.sendEmptyMessage(0x01);
    }

    @Override
    protected void onResume() {
        super.onResume();
        initDid();
        initProfile();
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

    private static final String APPID = BuildConfig.APP_ID;

    class KeyValue {
        public String Key;
        public String Value;
    }

    private String getKeyVale(String path, String value){
        KeyValue key = new KeyValue();
        key.Key = APPID + "/" + path;
        key.Value = value;
        List<KeyValue> keys = new ArrayList<>();
        keys.add(key);

        return new Gson().toJson(keys, new TypeToken<List<KeyValue>>(){}.getType());
    }

    class IDcard {
        public String name;
        public String code;
    }
    class KeyValueId {
        public String Key;
        public IDcard Value;
    }

    private String getIdKeyValue(String path, String name, String code){
        IDcard iDcard = new IDcard();
        iDcard.name = name;
        iDcard.code = code;

        KeyValueId keyValueId = new KeyValueId();
        keyValueId.Key = APPID + "/" + path;
        keyValueId.Value = iDcard;

        List<KeyValue> keyValues = new ArrayList<>();
        return new Gson().toJson(keyValues, new TypeToken<List<KeyValue>>(){}.getType());
    }

    private Did mDid;
    private String mSeed;
    private void initDid(){
        if(mDid == null){
            String mnemonic = getMn();
            if(StringUtil.isNullOrEmpty(mnemonic)) return;
            String language = Utility.detectLang(ProfileActivity.this, mnemonic);
            Log.i("ProfileFunction", "language:"+ language);
            if(StringUtil.isNullOrEmpty(language)) return;
            String words = Utility.getWords(ProfileActivity.this,  language +"-BIP39Words.txt");
            Log.i("ProfileFunction", "words is null:"+ (null==words));
            mSeed = IdentityManager.getSeed(mnemonic, Utility.getLanguage(language), words, "");
            if(StringUtil.isNullOrEmpty(mSeed)) return;
            Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());
            BlockChainNode node = new BlockChainNode(ProfileDataSource.DID_URL);
            DidManager didManager = identity.createDidManager(mSeed);
            mDid = didManager.createDid(0);
            mDid.setNode(node);
        }
    }

    class PayloadInfo {
        public long blockTime;
        public String did;
        public String key;
        public String txid;
        public String value;
    }

    class PayloadInfoId {
        public long blockTime;
        public String did;
        public String key;
        public String txid;
        public IDcard value;
    }

    private PayloadInfo getPayloadInfo(String value){
        if(StringUtil.isNullOrEmpty(value)) return null;
        return new Gson().fromJson(value, PayloadInfo.class);
    }

    private PayloadInfoId getPayloadInfoId(String value){
        if(StringUtil.isNullOrEmpty(value)) return null;
        return new Gson().fromJson(value, PayloadInfoId.class);
    }

    private void initProfile(){
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mDid.syncInfo();
                PayloadInfo payloadInfo = null;
                String nickname = mDid.getInfo(APPID+"/Nickname");
                payloadInfo = getPayloadInfo(nickname);
                String nickTxid = BRSharedPrefs.getCacheTxid(ProfileActivity.this, BRSharedPrefs.NICKNAME_txid);
                if(null!=payloadInfo && (StringUtil.isNullOrEmpty(nickTxid) || nickTxid.equals(payloadInfo.txid))){
                    BRSharedPrefs.putNickname(ProfileActivity.this, payloadInfo.value);
                    BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.NICKNAME_STATE, SettingsUtil.IS_COMPLETED);
                }

                String email = mDid.getInfo(APPID+"/Email");
                payloadInfo = getPayloadInfo(email);
                String emailTxid = BRSharedPrefs.getCacheTxid(ProfileActivity.this, BRSharedPrefs.EMAIL_txid);
                if(null!=payloadInfo && (StringUtil.isNullOrEmpty(emailTxid) || emailTxid.equals(payloadInfo.txid))) {
                    BRSharedPrefs.putEmail(ProfileActivity.this, payloadInfo.value);
                    BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.EMAIL_STATE, SettingsUtil.IS_COMPLETED);
                }

                String mobile = mDid.getInfo(APPID+"/Mobile");
                payloadInfo = getPayloadInfo(mobile);
                String mobileTxid = BRSharedPrefs.getCacheTxid(ProfileActivity.this, BRSharedPrefs.MOBILE_txid);
                if(null!=payloadInfo && (StringUtil.isNullOrEmpty(mobileTxid) || mobileTxid.equals(payloadInfo.txid))) {
                    BRSharedPrefs.putMobile(ProfileActivity.this, payloadInfo.value);
                    BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.MOBILE_STATE, SettingsUtil.IS_COMPLETED);
                }

                String idCard = mDid.getInfo(APPID+"/ChineseIDCard");
                PayloadInfoId payloadInfoId = getPayloadInfoId(idCard);
                String idTxid = BRSharedPrefs.getCacheTxid(ProfileActivity.this, BRSharedPrefs.ID_txid);
                if(null!=payloadInfoId && null!=payloadInfoId.value && (StringUtil.isNullOrEmpty(idTxid) || idTxid.equals(payloadInfo.txid))) {
                    BRSharedPrefs.putRealname(ProfileActivity.this, payloadInfoId.value.name);
                    BRSharedPrefs.putID(ProfileActivity.this, payloadInfoId.value.code);
                    BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.ID_STATE, SettingsUtil.IS_COMPLETED);
                }
                mHandler.sendEmptyMessage(0x01);
            }
        });
    }

    private String uploadData(String data){
        Log.i("ProfileFunction", "upload Data:"+ data);
        String info = mDid.signInfo(mSeed, data);
        Log.i("ProfileFunction", "sign info:"+info);
        String txid = ProfileDataSource.getInstance(ProfileActivity.this).upchain(info);
        Log.i("ProfileFunction", "txid:"+txid);

        return txid;
    }

    @Override
    protected void onActivityResult(final int requestCode, int resultCode, final Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        Log.i("ProfileFunction", "requestCode:"+requestCode);
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
                boolean canRefresh = false;
                if(BRConstants.PROFILE_REQUEST_NICKNAME == requestCode){
                    String nickname = data.getStringExtra("nickname");
                    Log.i("ProfileFunction", "nickname:"+nickname);
                    if(null == nickname) return;
                    String data = getKeyVale("Nickname", nickname);
                    if(BuildConfig.CAN_UPLOAD.contains("nickname")) txid = uploadData(data);
                    if(!StringUtil.isNullOrEmpty(txid) || !BuildConfig.CAN_UPLOAD.contains("nickname")){
                        canRefresh = true;
                        if(nickname.equals("")){//处理清空操作
                            BRSharedPrefs.putNickname(ProfileActivity.this, "Your Nickname");
                            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.NICKNAME_STATE,
                                    BuildConfig.CAN_UPLOAD.contains("nickname")?SettingsUtil.IS_SAVING:SettingsUtil.IS_PENDING);
                        } else {
                            BRSharedPrefs.putNickname(ProfileActivity.this, nickname);
                            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.NICKNAME_STATE,
                                    BuildConfig.CAN_UPLOAD.contains("nickname")?SettingsUtil.IS_SAVING:SettingsUtil.IS_COMPLETED);
                        }

                        BRSharedPrefs.putCacheTxid(ProfileActivity.this, BRSharedPrefs.NICKNAME_txid, txid);
                    }

                } else if(BRConstants.PROFILE_REQUEST_EMAIL == requestCode){
                    String email = data.getStringExtra("email");
                    Log.i("ProfileFunction", "email:"+email);
                    if(null == email) return;
                    String data = getKeyVale("Email", email);
                    if(BuildConfig.CAN_UPLOAD.contains("email")) txid = uploadData(data);
                    if(!StringUtil.isNullOrEmpty(txid) || !BuildConfig.CAN_UPLOAD.contains("email")){
                        canRefresh = true;
                        BRSharedPrefs.putEmail(ProfileActivity.this, email);
                        if(email.equals("")){
                            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.EMAIL_STATE,
                                    BuildConfig.CAN_UPLOAD.contains("email")?SettingsUtil.IS_SAVING:SettingsUtil.IS_PENDING);
                        } else {
                            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.EMAIL_STATE,
                                    BuildConfig.CAN_UPLOAD.contains("email")?SettingsUtil.IS_SAVING:SettingsUtil.IS_COMPLETED);
                        }
                        BRSharedPrefs.putCacheTxid(ProfileActivity.this, BRSharedPrefs.EMAIL_txid, txid);
                    }

                } else if(BRConstants.PROFILE_REQUEST_MOBILE == requestCode){
                    String mobile = data.getStringExtra("mobile");
                    Log.i("ProfileFunction", "mobile:"+mobile);
                    if(null == mobile) return;
                    String data = getKeyVale("Mobile", mobile);
                    if(BuildConfig.CAN_UPLOAD.contains("mobile")) txid = uploadData(data);
                    if(!StringUtil.isNullOrEmpty(txid) || !BuildConfig.CAN_UPLOAD.contains("mobile")){
                        canRefresh = true;
                        BRSharedPrefs.putMobile(ProfileActivity.this, mobile);
                        if(mobile.equals("86,")){
                            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.MOBILE_STATE,
                                    BuildConfig.CAN_UPLOAD.contains("mobile")?SettingsUtil.IS_SAVING:SettingsUtil.IS_PENDING);
                        } else {
                            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.MOBILE_STATE,
                                    BuildConfig.CAN_UPLOAD.contains("mobile")?SettingsUtil.IS_SAVING:SettingsUtil.IS_COMPLETED);
                        }
                        BRSharedPrefs.putCacheTxid(ProfileActivity.this, BRSharedPrefs.MOBILE_txid, txid);
                    }

                } else if(BRConstants.PROFILE_REQUEST_ID == requestCode){
                    String realname = data.getStringExtra("realname");
                    String idcard = data.getStringExtra("idcard");
                    Log.i("ProfileFunction", "realname:"+realname);
                    Log.i("ProfileFunction", "idcard:"+idcard);
                    if(null==realname || null==idcard) return;
                    String data = getIdKeyValue("/ChineseIDCard", realname, idcard);
                    if(BuildConfig.CAN_UPLOAD.contains("ChineseIDCard")) txid = uploadData(data);
                    if(!StringUtil.isNullOrEmpty(txid) || !BuildConfig.CAN_UPLOAD.contains("ChineseIDCard")){
                        canRefresh = true;
                        BRSharedPrefs.putRealname(ProfileActivity.this, realname);
                        BRSharedPrefs.putID(ProfileActivity.this, idcard);
                        if(realname.equals("") || idcard.equals("")){
                            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.ID_STATE,
                                    BuildConfig.CAN_UPLOAD.contains("ChineseIDCard")?SettingsUtil.IS_SAVING:SettingsUtil.IS_PENDING);
                        } else {
                            BRSharedPrefs.putProfileState(ProfileActivity.this, BRSharedPrefs.ID_STATE,
                                    BuildConfig.CAN_UPLOAD.contains("ChineseIDCard")?SettingsUtil.IS_SAVING:SettingsUtil.IS_COMPLETED);
                        }
                        BRSharedPrefs.putCacheTxid(ProfileActivity.this, BRSharedPrefs.ID_txid, txid);
                    }
                }

                if(canRefresh){
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
        Log.i("ProfileFunction", "checkTx");
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
        Log.i("ProfileFunction", "tx is exit");
        if(mHandler !=null) mHandler.sendEmptyMessage(0x01);
    }

}
