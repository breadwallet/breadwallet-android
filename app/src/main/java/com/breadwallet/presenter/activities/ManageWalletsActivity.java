package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;
import android.view.View;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.adapter.ManageTokenListAdapter;
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.wallets.etherium.WalletEthManager;
import com.breadwallet.wallet.wallets.etherium.WalletTokenManager;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;
import java.util.List;

public class ManageWalletsActivity extends BRActivity {


    private static final String TAG = ManageWalletsActivity.class.getSimpleName();
    private ManageTokenListAdapter mAdapter;
    private RecyclerView mTokenList;
    private List<TokenListMetaData.TokenInfo> mTokens;
    private ItemTouchHelper mItemTouchHelper;
    private ImageButton mBackButton;


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_manage_wallets);

        mTokenList = findViewById(R.id.token_list);
        mBackButton = findViewById(R.id.back_arrow);

        mBackButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                onBackPressed();
            }
        });

    }

    @Override
    protected void onResume() {
        super.onResume();

        final ArrayList<TokenItem> tokenItems = new ArrayList<>();

        mTokens = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this).enabledCurrencies;

        for (int i = 0; i < mTokens.size(); i++) {

            TokenListMetaData.TokenInfo info = mTokens.get(i);
            TokenItem tokenItem = null;
            String tokenSymbol = mTokens.get(i).symbol;

            if (!tokenSymbol.equalsIgnoreCase("btc") && !tokenSymbol.equalsIgnoreCase("bch") && !tokenSymbol.equalsIgnoreCase("eth")) {

                BREthereumToken tk = WalletEthManager.getInstance(this).node.lookupToken(info.contractAddress);
                if (tk == null) {
                    BRReportsManager.reportBug(new NullPointerException("No token for contract: " + info.contractAddress));

                } else
                    tokenItem = new TokenItem(tk.getAddress(), tk.getSymbol(), tk.getName(), null);


            } else if (tokenSymbol.equalsIgnoreCase("btc"))
                tokenItem = new TokenItem(null, "BTC", "Bitcoin", null);

            else if (tokenSymbol.equalsIgnoreCase("bch"))
                tokenItem = new TokenItem(null, "BCH", "Bitcoin Cash", null);
            else if (tokenSymbol.equalsIgnoreCase("eth"))
                tokenItem = new TokenItem(null, "ETH", "Ethereum", null);


            if (tokenItem != null) {
                tokenItems.add(tokenItem);
            }

        }


        mAdapter = new ManageTokenListAdapter(ManageWalletsActivity.this, tokenItems, new ManageTokenListAdapter.OnTokenShowOrHideListener() {
            @Override
            public void onShowToken(TokenItem token) {
                Log.d(TAG, "onShowToken");


                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);


                if (metaData.hiddenCurrencies == null)
                    metaData.hiddenCurrencies = new ArrayList<>();
                metaData.showCurrency(item.symbol);


                mAdapter.notifyDataSetChanged();
                final TokenListMetaData finalMetaData = metaData;
                KVStoreManager.getInstance().putTokenListMetaData(ManageWalletsActivity.this, finalMetaData);

            }

            @Override
            public void onHideToken(TokenItem token) {
                Log.d(TAG, "onHideToken");

                TokenListMetaData metaData = KVStoreManager.getInstance().getTokenListMetaData(ManageWalletsActivity.this);
                TokenListMetaData.TokenInfo item = new TokenListMetaData.TokenInfo(token.symbol, true, token.address);
                if (metaData == null) metaData = new TokenListMetaData(null, null);

                if (metaData.hiddenCurrencies == null)
                    metaData.hiddenCurrencies = new ArrayList<>();

                metaData.hiddenCurrencies.add(item);

                mAdapter.notifyDataSetChanged();
                KVStoreManager.getInstance().putTokenListMetaData(ManageWalletsActivity.this, metaData);

            }
        });

        mTokenList.setLayoutManager(new LinearLayoutManager(ManageWalletsActivity.this));
        mTokenList.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mTokenList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        WalletsMaster.getInstance(this).updateWallets(this);
    }
}
