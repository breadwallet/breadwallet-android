package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BREdit;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.adapter.AddTokenListAdapter;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;
import java.util.List;

public class AddWalletsActivity extends BRActivity {

    private AddTokenListAdapter mAdapter;
    private BREdit mSearchView;
    private RecyclerView mRecycler;
    private static final String TAG = AddWalletsActivity.class.getSimpleName();
    private ImageButton mBackButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_add_wallets);

        mRecycler = findViewById(R.id.token_list);
        mSearchView = findViewById(R.id.search_edit);
        mBackButton = findViewById(R.id.back_arrow);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

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

        List<TokenItem> tokenItems = new ArrayList<>();
        TokenListMetaData tokenListMetaData = KVStoreManager.getInstance().getTokenListMetaData(this);
        for (BREthereumToken token : WalletEthManager.getInstance(this).node.tokens) {
            TokenItem tokenItem = new TokenItem(token.getAddress(), token.getSymbol(), token.getName(), null);
            if (!tokenListMetaData.isCurrencyEnabled(tokenItem.symbol)) {
                tokenItems.add(tokenItem);
            }
        }

        mAdapter = new AddTokenListAdapter(this, tokenItems, new AddTokenListAdapter.OnTokenAddOrRemovedListener() {
            @Override
            public void onTokenAdded(TokenItem token) {

                Log.d(TAG, "onTokenAdded, -> " + token.symbol);

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(AddWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);
                if (metaData.enabledCurrencies == null)
                    metaData.enabledCurrencies = new ArrayList<>();
                if (!metaData.isCurrencyEnabled(item.symbol))
                    metaData.enabledCurrencies.add(item);

                KVStoreManager.getInstance().putTokenListMetaData(AddWalletsActivity.this, metaData);

                mAdapter.notifyDataSetChanged();

            }

            @Override
            public void onTokenRemoved(TokenItem token) {
                Log.d(TAG, "onTokenRemoved, -> " + token.name);

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(AddWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);
                metaData.disableCurrency(item.symbol);

                KVStoreManager.getInstance().putTokenListMetaData(AddWalletsActivity.this, metaData);

                mAdapter.notifyDataSetChanged();
            }
        });
        mRecycler.setLayoutManager(new LinearLayoutManager(this));
        mRecycler.setAdapter(mAdapter);

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WalletsMaster.getInstance(this).updateWallets(this);
    }
}
