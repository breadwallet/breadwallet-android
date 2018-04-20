package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.adapter.AddTokenListAdapter;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;
import java.util.List;

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


        mSearchView.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {

            }

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {

                String query = mSearchView.getText().toString();

                if (mAdapter != null) {
                    mAdapter.filter(query);
                }

                if (query.equals("")) {
                    mAdapter.resetFilter();
                }

            }

            @Override
            public void afterTextChanged(Editable s) {


            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        final ArrayList<TokenItem> tokenItems = new ArrayList<>();

        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mTokens = BREthereumToken.tokens;


                for (int i = 0; i < mTokens.length; i++) {

                    BREthereumToken token = mTokens[i];
                    TokenItem tokenItem = new TokenItem(token.getAddress(), token.getSymbol(), token.getName(), null);
                    tokenItems.add(tokenItem);

                }


            }
        });


        mAdapter = new AddTokenListAdapter(this, tokenItems, new AddTokenListAdapter.OnTokenAddOrRemovedListener() {
            @Override
            public void onTokenAdded(TokenItem token) {

                Log.d(TAG, "onTokenAdded, -> " + token.name);

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(AddWalletsActivity.this);
                TokenListMetaData.TokenItem item = new TokenListMetaData.TokenItem(token.name, true, token.address);

                if (metaData != null && metaData.enabledCurrencies != null) {
                    Log.d(TAG, "onTokenAdded(): TokenListMetaData not null");
                    Log.d(TAG, "onTokenAdded() : Adding token to KV store list -> " + item.name);
                    metaData.enabledCurrencies.add(item);
                    KVStoreManager.getInstance().putTokenListMetaData(AddWalletsActivity.this, metaData);
                } else if (metaData == null) {
                    Log.d(TAG, "onTokenAdded(): TokenListMetaData is null");
                    metaData = new TokenListMetaData();
                    metaData.enabledCurrencies = new ArrayList<>();
                    metaData.enabledCurrencies.add(item);
                    KVStoreManager.getInstance().putTokenListMetaData(AddWalletsActivity.this, metaData);
                }


            }

            @Override
            public void onTokenRemoved(TokenItem token) {
                Log.d(TAG, "onTokenRemoved, -> " + token.name);

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(AddWalletsActivity.this);
                TokenListMetaData.TokenItem item = new TokenListMetaData.TokenItem(token.name, true, token.address);

                if (metaData != null && metaData.hiddenCurrencies != null) {
                    Log.d(TAG, "onTokenRemoved() : Removing token from KV Store list -> " + item.name);
                    metaData.hiddenCurrencies.add(item);
                    KVStoreManager.getInstance().putTokenListMetaData(AddWalletsActivity.this, metaData);

                }


            }
        });
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

    }
}
