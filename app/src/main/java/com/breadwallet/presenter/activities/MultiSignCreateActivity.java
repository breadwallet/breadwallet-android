package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.Gravity;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.jsbridge.JsInterface;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.google.gson.Gson;

import org.elastos.sdk.keypair.ElastosKeypairSign;

public class MultiSignCreateActivity extends BRActivity {
    private final String TAG = "MultiSignCreateActivity";

    private int mRequiredCount;
    private String[] mPublicKeys;
    private String mAddress;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_sign_create);

        Intent intent = getIntent();
        mPublicKeys = intent.getStringArrayExtra("publicKeys");
        mRequiredCount = intent.getIntExtra("requiredCount", 0);
        if (mRequiredCount == 0 || mPublicKeys == null || mPublicKeys.length == 0) {
            Log.e(TAG, "no public keys");
            setResultAndFinish(-1, "no public keys");
            return;
        }

        mAddress = ElastosKeypairSign.getMultiSignAddress(mPublicKeys, mPublicKeys.length, mRequiredCount);
        if(StringUtil.isNullOrEmpty(mAddress)) {
            Log.e(TAG, "get multi sign wallet address failed");
            setResultAndFinish(-2, "get wallet address failed");
            return;
        }

        initView();
        initPublicKey();

    }

    private void setResultAndFinish(int code, String data) {
        Intent intent = new Intent();
        intent.putExtra("data", data);
        setResult(code, intent);
        finish();
    }

    private void initView() {
        ImageButton backButton = findViewById(R.id.back_button);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

        Button denyBtn = findViewById(R.id.multisign_create_deny);
        denyBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        Button createBtn = findViewById(R.id.multisign_create);
        createBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                create();
            }
        });

        TextView addr = findViewById(R.id.multisign_create_addr);
        addr.setText(mAddress);

        TextView pubkeyTitle = findViewById(R.id.multisign_create_pubkeys);
        pubkeyTitle.setText(String.format(getResources().getString(R.string.multisign_create_keys), mPublicKeys.length));

        TextView required = findViewById(R.id.multisign_create_required);
        required.setText(String.format(getResources().getString(R.string.multisign_create_unlock_keys), mRequiredCount));
    }

    private void initPublicKey() {
        String myPublicKey = WalletElaManager.getInstance(this).getPublicKey();
        LinearLayout page = findViewById(R.id.multisign_create_page);

        for (String publicKey : mPublicKeys) {
            TextView lableView = new TextView(this);
            if (myPublicKey.equals(publicKey)) {
                lableView.setText(myPublicKey + "(me)");
            } else {
                lableView.setText(publicKey);
            }

            LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT, LinearLayout.LayoutParams.WRAP_CONTENT);
            lp.topMargin = getResources().getDimensionPixelOffset(R.dimen.radius);

            lableView.setTextSize(12);
            lableView.setTextColor(getResources().getColor(R.color.black_333333));

            lableView.setLayoutParams(lp);
            page.addView(lableView);
        }
    }

    private void create() {
        JsInterface.MultiSignParam param = new JsInterface.MultiSignParam();
        param.PublicKeys = mPublicKeys;
        param.RequiredCount = mRequiredCount;

        BRSharedPrefs.putMultiSignInfo(this, mAddress, new Gson().toJson(param));

        Toast toast = Toast.makeText(getApplicationContext(),
                    R.string.multisign_created, Toast.LENGTH_LONG);
        toast.setGravity(Gravity.CENTER, 0, 0);
        toast.show();

        setResultAndFinish(0, mAddress);
    }
}
