package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.did.DidDataSource;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.customviews.LoadingDialog;
import com.breadwallet.presenter.entities.VoteEntity;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.adapter.VoteNodeAdapter;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.StringUtil;
import com.breadwallet.vote.ProducerEntity;
import com.breadwallet.wallet.wallets.ela.BRElaTransaction;
import com.breadwallet.wallet.wallets.ela.ElaDataSource;
import com.breadwallet.wallet.wallets.ela.WalletElaManager;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import org.wallet.library.AuthorizeManager;
import org.wallet.library.entity.UriFactory;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

public class VoteActivity extends BaseSettingsActivity {

    private String mUri;
    private UriFactory uriFactory;
    private TextView mVoteCountTv;
    private TextView mBalanceTv;
    private TextView mVoteElaAmountTv;
    private Button mCancleBtn;
    private Button mConfirmBtn;
    private ListView mVoteNodeLv;
    private TextView mVotePasteTv;
    private BaseTextView mNodeListTitle;

    private LoadingDialog mLoadingDialog;

    @Override
    public int getLayoutId() {
        return R.layout.activity_vote_layout;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

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
        mVoteCountTv = findViewById(R.id.voting_hint);
        mBalanceTv = findViewById(R.id.vote_ela_balance);
        mVoteElaAmountTv = findViewById(R.id.vote_ela_amount);
        mVotePasteTv = findViewById(R.id.vote_paste_tv);
        mNodeListTitle = findViewById(R.id.vote_nodes_list_title);
        mCancleBtn = findViewById(R.id.vote_cancle_btn);
        mConfirmBtn = findViewById(R.id.vote_confirm_btn);
        mVoteNodeLv = findViewById(R.id.vote_node_lv);
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
                BigDecimal balance = BRSharedPrefs.getCachedBalance(VoteActivity.this, "ELA");
                if(balance.longValue() <= 0){
                    Toast.makeText(VoteActivity.this, getString(R.string.vote_balance_not_insufficient), Toast.LENGTH_SHORT).show();
                    return;
                }
                if(verifyUri()){
                    sendTx();
                }
            }
        });

        mVotePasteTv.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                copyText();
            }
        });
    }

    private void copyText() {
        BRClipboardManager.putClipboard(this, new Gson().toJson(mProducers));
        Toast.makeText(this, "has copy to clipboard", Toast.LENGTH_SHORT).show();

    }

    private boolean verifyUri(){
        String did = uriFactory.getDID();
        String appId = uriFactory.getAppID();
        String appName = uriFactory.getAppName();
        String PK = uriFactory.getPublicKey();
        boolean isValid = AuthorizeManager.verify(this, did, PK, appName, appId);

        return isValid;
    }


    private void callReturnUrl(String txId){
        if(StringUtil.isNullOrEmpty(txId)) return;
        String returnUrl = uriFactory.getReturnUrl();
        String url;
        if (returnUrl.contains("?")) {
            url = returnUrl + "&TXID=" + txId;
        } else {
            url = returnUrl + "?TXID=" + txId;
        }
        DidDataSource.getInstance(VoteActivity.this).callReturnUrl(url);
    }

    private void callBackUrl(String txid){
        if(StringUtil.isNullOrEmpty(txid)) return;
        String backurl = uriFactory.getCallbackUrl();
        if(StringUtil.isNullOrEmpty(backurl)) return;
        VoteEntity txEntity = new VoteEntity();
        txEntity.TXID = txid;
        String ret = DidDataSource.getInstance(this).urlPost(backurl, new Gson().toJson(txEntity));
    }

    private void sendTx(){
        if(null==mCandidates || mCandidates.size()<=0) return;
        if(mCandidates.size()>36) {
            Toast.makeText(this, getString(R.string.beyond_max_vote_node), Toast.LENGTH_SHORT).show();
            return;
        }
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
                        callBackUrl(mRwTxid);
                        callReturnUrl(mRwTxid);
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
    private VoteNodeAdapter mAdapter;
    private List<String> mCandidates;
    private List<ProducerEntity> mProducers = new ArrayList<>();
    private void initData(){
        if (StringUtil.isNullOrEmpty(mUri)) return;
        uriFactory = new UriFactory();
        uriFactory.parse(mUri);

        mCandidatesStr = uriFactory.getCandidatePublicKeys();
        if(StringUtil.isNullOrEmpty(mCandidatesStr)) return;
        mCandidates = new Gson().fromJson(mCandidatesStr, new TypeToken<List<String>>(){}.getType());

        BigDecimal balance = BRSharedPrefs.getCachedBalance(this, "ELA");

        mVoteCountTv.setText(String.format(getString(R.string.vote_nodes_count), mCandidates.size()));
        mNodeListTitle.setText(String.format(getString(R.string.node_list_title), mCandidates.size()));
        mBalanceTv.setText(String.format(getString(R.string.vote_balance), balance.toString()));
        mAmount = balance.subtract(new BigDecimal(0.0001));
        mVoteElaAmountTv.setText(mAmount.longValue()+"");

        List<ProducerEntity> tmp = ElaDataSource.getInstance(VoteActivity.this).getProducersByPK(mCandidates);
        if(tmp!=null && tmp.size()>0) {
            mProducers.clear();
            mProducers.addAll(tmp);
        }
        mAdapter = new VoteNodeAdapter(this, mProducers);
        mVoteNodeLv.setAdapter(mAdapter);


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


    @Override
    protected void onDestroy() {
        super.onDestroy();
        if(null != mLoadingDialog){
            mLoadingDialog.dismiss();
            mLoadingDialog = null;
        }
    }
}
