package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.did.DidAuthorizeActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.wallet.wallets.ela.BRElaTransaction;
import com.breadwallet.wallet.wallets.ela.ElaDataSource;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wallet.library.AuthorizeManager;
import org.wallet.library.entity.UriFactory;

import java.math.BigDecimal;
import java.util.List;

public class VoteActivity extends BRActivity {

    private String mUri;
    private UriFactory uriFactory;
    private TextView mVoteCountTv;
    private TextView mBalanceTv;
    private TextView mVoteElaAmountTv;
    private Button mCancleBtn;
    private Button mConfirmBtn;

    private LoadingDialog mLoadingDialog;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_vote_layout);

        Intent intent = getIntent();
        if (intent != null) {
            String action = intent.getAction();
            if (!StringUtil.isNullOrEmpty(action) && action.equals(Intent.ACTION_VIEW)) {
                Uri uri = intent.getData();
                mUri = uri.toString();
            } else {
                mUri = intent.getStringExtra("vote_scheme_uri");
            }
        }
        Log.i("VoteActivity", "uri:"+mUri);

        findView();
        initListener();
        initData();

    }


    private void findView(){
        mVoteCountTv = findViewById(R.id.vote_nodes_count);
        mBalanceTv = findViewById(R.id.vote_ela_balance);
        mVoteElaAmountTv = findViewById(R.id.vote_ela_amount);
        mCancleBtn = findViewById(R.id.vote_cancle_btn);
        mConfirmBtn = findViewById(R.id.vote_confirm_btn);
        mLoadingDialog = new LoadingDialog(this, R.style.progressDialog);
        mLoadingDialog.setCanceledOnTouchOutside(false);
    }

    private void initListener(){
        mCancleBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                finish();
            }
        });
        mConfirmBtn.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(verifyUri()){
                    sendTx();
                }
            }
        });
    }

    private boolean verifyUri(){
        final String did = uriFactory.getDID();
        final String appId = uriFactory.getAppID();
        String sign = uriFactory.getSignature();
        String PK = uriFactory.getPublicKey();
        boolean isValid = AuthorizeManager.verify(this, did, PK, appId, sign);

        return isValid;
    }

    private void sendTx(){
        if(null==mCandidates || mCandidates.size()<=0) return;

        AuthManager.getInstance().authPrompt(this, this.getString(R.string.pin_author_vote), getString(R.string.pin_author_vote_msg), true, false, new BRAuthCompletion() {
            @Override
            public void onComplete() {
                showDialog();
                BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        Log.d("posvote", "mCandidatesStr:"+mCandidatesStr);
                        BRSharedPrefs.cacheCandidate(VoteActivity.this, mCandidatesStr);
                        String address = WalletElaManager.getInstance(VoteActivity.this).getAddress();
                        BRElaTransaction transaction = ElaDataSource.getInstance(VoteActivity.this).createElaTx(address, address, 0, "vote", mCandidates);
                        if(null == transaction) return;
                        String txId = transaction.getTx();
                        if(StringUtil.isNullOrEmpty(txId)) return;
                        String mRwTxid = ElaDataSource.getInstance(VoteActivity.this).sendElaRawTx(txId);
                        dismissDialog();
                        finish();
                    }
                });
            }

            @Override
            public void onCancel() {
                //nothing
            }
        });

    }

    private BigDecimal mAmount;
    private String  mCandidatesStr;
    private List<String> mCandidates;
    private void initData(){
        if (StringUtil.isNullOrEmpty(mUri)) return;
        uriFactory = new UriFactory();
        uriFactory.parse(mUri);

        mCandidatesStr = uriFactory.getCandidatePublicKeys();
        Log.d("posvote", "candidateValue:"+mCandidatesStr);
        BRSharedPrefs.cacheCandidate(this, mCandidatesStr);
        if(StringUtil.isNullOrEmpty(mCandidatesStr)) return;
        mCandidates = new Gson().fromJson(mCandidatesStr, new TypeToken<List<String>>(){}.getType());

        BigDecimal balance = BRSharedPrefs.getCachedBalance(this, "ELA");

        mVoteCountTv.setText(String.format(getString(R.string.vote_nodes_count), mCandidates.size()));
        mBalanceTv.setText(balance.toString());
        mAmount = balance.subtract(new BigDecimal(0.0001));
        mVoteElaAmountTv.setText(mAmount.longValue()+"");
    }

    private void dismissDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing())
                    mLoadingDialog.dismiss();
            }
        });
    }


    private void showDialog() {
        runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (!isFinishing())
                    mLoadingDialog.show();
            }
        });
    }


}
