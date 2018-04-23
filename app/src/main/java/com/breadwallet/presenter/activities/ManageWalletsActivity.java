package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.adapter.ManageTokenListAdapter;
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;
import java.util.List;

public class ManageWalletsActivity extends BRActivity {

    private ManageTokenListAdapter mAdapter;
    private RecyclerView mTokenList;
    private List<TokenListMetaData.TokenInfo> mTokens;
    private ItemTouchHelper mItemTouchHelper;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_wallets);

        mTokenList = findViewById(R.id.token_list);

    }

    @Override
    protected void onResume() {
        super.onResume();

        final ArrayList<TokenItem> tokenItems = new ArrayList<>();


        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mTokens = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this).enabledCurrencies;
                List<TokenListMetaData.TokenInfo> hiddenTokens = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this).hiddenCurrencies;

                if(hiddenTokens != null && !hiddenTokens.isEmpty()){
                    mTokens.addAll(hiddenTokens);
                }


                for (int i = 0; i < mTokens.size(); i++) {

                    TokenListMetaData.TokenInfo tokenInfo = mTokens.get(i);
                    TokenListMetaData.TokenInfo info = mTokens.get(i);
                    TokenItem tokenItem;
                    Log.d("ManageWalletsActivity", "Looking up -> " + mTokens.get(i).symbol);

                    if (mTokens.get(i).symbol.equalsIgnoreCase("btc")) {
                        tokenItem = new TokenItem(null, tokenInfo.symbol, "Bitcoin", null);
                    } else if (mTokens.get(i).symbol.equalsIgnoreCase("bch")) {
                        tokenItem = new TokenItem(null, tokenInfo.symbol, "Bitcoin Cash", null);

                    } else if (mTokens.get(i).symbol.equalsIgnoreCase("eth")) {
                        tokenItem = new TokenItem(null, tokenInfo.symbol, "Ethereum", null);

                    } else {

                        BREthereumToken tk = BREthereumToken.lookup(info.contractAddress);
                        tokenItem = new TokenItem(tk.getAddress(), tk.getSymbol(), tk.getName(), null);

                    }
                    tokenItems.add(tokenItem);

                }


            }
        });

        mAdapter = new ManageTokenListAdapter(ManageWalletsActivity.this, tokenItems, new ManageTokenListAdapter.OnTokenShowOrHideListener() {
            @Override
            public void onShowToken(TokenItem token) {

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);
                if (metaData.isCurrencyHidden(item.symbol))
                    metaData.hiddenCurrencies.remove(item);
                //metaData.enabledCurrencies.add(item);
                KVStoreManager.getInstance().putTokenListMetaData(ManageWalletsActivity.this, metaData);


            }

            @Override
            public void onHideToken(TokenItem token) {

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);
                if (!metaData.isCurrencyHidden(token.symbol))
                    metaData.disableCurrency(item.symbol);
                KVStoreManager.getInstance().putTokenListMetaData(ManageWalletsActivity.this, metaData);


            }
        });

        mTokenList.setLayoutManager(new LinearLayoutManager(ManageWalletsActivity.this));
        mTokenList.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mTokenList);
    }
}
