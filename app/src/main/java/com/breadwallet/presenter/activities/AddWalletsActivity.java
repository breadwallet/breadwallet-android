package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.adapter.AddWalletListAdapter;

import java.util.ArrayList;
import java.util.Arrays;

public class AddWalletsActivity extends BRActivity {


    private BREthereumToken[] mTokens;
    private AddWalletListAdapter mAdapter;
    private BREdit mSearchView;
    private RecyclerView mRecycler;


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

        mTokens = BREthereumToken.tokens;
        if (mAdapter == null && mTokens != null) {

            ArrayList<TokenItem> tokenItems = new ArrayList<>();
            for (BREthereumToken token : mTokens) {

                TokenItem tokenItem = new TokenItem(token.getAddress(), token.getSymbol(), token.getName(), null);
                tokenItems.add(tokenItem);
            }

            mAdapter = new AddWalletListAdapter(this, tokenItems);
            mRecycler.setLayoutManager(new LinearLayoutManager(this));
            mRecycler.setAdapter(mAdapter);
        }
    }
}
