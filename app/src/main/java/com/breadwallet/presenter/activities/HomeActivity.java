package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.security.keystore.UserNotAuthenticatedException;
import android.support.annotation.NonNull;
import android.support.design.widget.BottomNavigationView;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentTransaction;
import android.util.Log;
import android.view.MenuItem;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.fragments.FragmentExplore;
import com.breadwallet.presenter.fragments.FragmentSetting;
import com.breadwallet.presenter.fragments.FragmentWallet;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.sqlite.ProfileDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.StringUtil;
import com.elastos.jni.Utility;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.elastos.sdk.wallet.Did;
import org.elastos.sdk.wallet.DidManager;
import org.elastos.sdk.wallet.Identity;
import org.elastos.sdk.wallet.IdentityManager;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Home activity that will show a list of a user's wallets
 */

public class HomeActivity extends BRActivity implements InternetManager.ConnectionReceiverListener {

    private static final String TAG = HomeActivity.class.getSimpleName() + "_test";
    private FragmentWallet mWalletFragment;
    private FragmentExplore mExploreFragment;
    private Fragment mSettingFragment;
    private FragmentManager mFragmentManager;
    private BottomNavigationView navigation;

    private BottomNavigationView.OnNavigationItemSelectedListener mOnNavigationItemSelectedListener
            = new BottomNavigationView.OnNavigationItemSelectedListener() {

        @Override
        public boolean onNavigationItemSelected(@NonNull MenuItem item) {
            switch (item.getItemId()) {
                case R.id.navigation_home:
                    showFragment(mWalletFragment);
                    return true;
                case R.id.navigation_explore:
                    showFragment(mExploreFragment);
                    return true;
                case R.id.navigation_notifications:
                    showFragment(mSettingFragment);
                    return true;
            }
            return false;
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        navigation = findViewById(R.id.navigation);
        navigation.setOnNavigationItemSelectedListener(mOnNavigationItemSelectedListener);

        mFragmentManager = getSupportFragmentManager();
        mWalletFragment = FragmentWallet.newInstance("Wallet");
        mExploreFragment = FragmentExplore.newInstance("Explore");
        mSettingFragment = FragmentSetting.newInstance("Setting");

        mCurrentFragment = mWalletFragment;
//        clearFragment();
        FragmentTransaction transaction = mFragmentManager.beginTransaction();
        transaction.add(R.id.frame_layout, mWalletFragment).show(mWalletFragment).commitAllowingStateLoss();

        initDid();
        didIsOnchain();
    }

    class KeyValue {
        public String Key;
        public String Value;
    }

    private String getKeyVale(String path, String value){
        KeyValue key = new KeyValue();
        key.Key = path;
        key.Value = value;
        List<KeyValue> keys = new ArrayList<>();
        keys.add(key);
        return new Gson().toJson(keys, new TypeToken<List<KeyValue>>(){}.getType());
    }

    private Did mDid;
    private String mSeed;
    private String publicKey;
    private void initDid(){
        String mnemonic = getMn();
        String language = Utility.detectLang(HomeActivity.this, mnemonic);
        String words = Utility.getWords(HomeActivity.this,  language +"-BIP39Words.txt");
        mSeed = IdentityManager.getSeed(mnemonic, Utility.getLanguage(language), words, "");
        Identity identity = IdentityManager.createIdentity(getFilesDir().getAbsolutePath());
        DidManager didManager = identity.createDidManager(mSeed);
        mDid = didManager.createDid(0);
        publicKey = Utility.getInstance(HomeActivity.this).getSinglePublicKey(mnemonic);
    }

    private void didIsOnchain(){
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mDid.syncInfo();
                String value = mDid.getInfo("DID/Publickey");
                Log.i("DidOnchain", "value:"+value);
                if(StringUtil.isNullOrEmpty(value) || !value.contains("DID/Publickey")){
                    String data = getKeyVale("DID/Publickey", publicKey);
                    String info = mDid.signInfo(mSeed, data);
                    String txid = ProfileDataSource.getInstance(HomeActivity.this).upchain(info);
                    Log.i("DidOnchain", "txid:"+txid);
                }
            }
        });
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
    protected void onResume() {
        super.onResume();
        boolean iscrash = getIntent().getBooleanExtra("crash", false);
        Log.i(TAG, "iscrash:" + iscrash);
        if (iscrash) navigation.setSelectedItemId(R.id.navigation_home);
        InternetManager.registerConnectionReceiver(this, this);
    }

    @Override
    protected void onPause() {
        super.onPause();
        InternetManager.unregisterConnectionReceiver(this, this);
    }

    private Fragment mCurrentFragment;

    private void showFragment(Fragment fragment) {
        if (mCurrentFragment != fragment) {
            FragmentTransaction transaction = mFragmentManager.beginTransaction();
            transaction.hide(mCurrentFragment);
            mCurrentFragment = fragment;
            if (!fragment.isAdded()) {
                transaction.add(R.id.frame_layout, fragment).show(fragment).commitAllowingStateLoss();
            } else {
                transaction.show(fragment).commitAllowingStateLoss();
            }
        }
    }

    @Override
    public void onConnectionChanged(boolean isConnected) {
        if (mWalletFragment != null)
            mWalletFragment.onConnectionChanged(isConnected);
    }
}