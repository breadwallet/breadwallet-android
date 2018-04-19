package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.adapter.AddTokenListAdapter;
import com.breadwallet.tools.threads.executor.BRExecutor;

import java.util.ArrayList;

public class AddWalletsActivity extends BRActivity {


    private BREthereumToken[] mTokens;
    private AddTokenListAdapter mAdapter;
    private BREdit mSearchView;
    private RecyclerView mRecycler;
    private static final String TAG = AddWalletsActivity.class.getSimpleName();


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallets);

        mRecycler = findViewById(R.id.token_list);
        mSearchView = findViewById(R.id.search_edit);
    }

    @Override
    protected void onResume() {
        super.onResume();

        final ArrayList<TokenItem> tokenItems = new ArrayList<>();

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mTokens = BREthereumToken.tokens;

                Log.d(TAG, "Token list -> " + mTokens.toString());
                Log.d(TAG, "Token list length -> " + mTokens.length);

                //if (mAdapter == null && mTokens != null) {
                Log.d(TAG, "Token list not empty, setting up adapter");

                for (int i = 0; i < mTokens.length; i++) {

                    BREthereumToken token = mTokens[i];
                    TokenItem tokenItem = new TokenItem(token.getAddress(), token.getSymbol(), token.getName(), null);
                    tokenItems.add(tokenItem);
                    //}
                }

                Log.d(TAG, "Token list size -> " + tokenItems.size());



            }
        });


        mAdapter = new AddTokenListAdapter(this, tokenItems);
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

    }
}
