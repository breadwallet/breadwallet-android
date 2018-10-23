package com.breadwallet;

import android.app.Activity;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.View;
import android.widget.TextView;

import com.breadwallet.tools.util.Utils;
import com.elastos.jni.Utility;


public class TestActivity extends Activity {
    private TextView mMnemonicTv;
    private TextView mPrivateKeyTv;
    private TextView mPublicKeyTv;
    private TextView mAddressTv;
    private TextView mRawTransactionTv;

    private String mRootPath;
    private String mMmnemonic;
    private String mPrivateKey;
    private String mPublicKey;
    private String mAddress;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.test_activity_layout);

        mMnemonicTv = findViewById(R.id.mnemonic);
        mPrivateKeyTv = findViewById(R.id.private_key);
        mPublicKeyTv = findViewById(R.id.public_key);
        mAddressTv = findViewById(R.id.address);
        mRawTransactionTv = findViewById(R.id.raw_transaction);

        mRootPath = getApplicationContext().getFilesDir().getParent();
    }

    public void generateMmemonic(View view){
        if(mMmnemonic == null){
            mMmnemonic = Utility.generateMnemonic("english", mRootPath);
            mMnemonicTv.setText(mMmnemonic);
        }
    }



    public void generateRawTransaction(View view){

    }
}
