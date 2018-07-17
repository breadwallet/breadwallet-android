package com.breadwallet.presenter.activities;

import android.os.Bundle;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.helper.ItemTouchHelper;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.core.ethereum.BREthereumToken;
import com.breadwallet.presenter.activities.settings.BaseSettingsActivity;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.adapter.ManageTokenListAdapter;
import com.breadwallet.tools.animation.SimpleItemTouchHelperCallback;
import com.breadwallet.tools.listeners.OnStartDragListener;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.wallets.ethereum.WalletEthManager;
import com.platform.entities.TokenListMetaData;
import com.platform.tools.KVStoreManager;

import java.util.ArrayList;
import java.util.List;

public class ManageWalletsActivity extends BaseSettingsActivity implements OnStartDragListener {

    private static final String TAG = ManageWalletsActivity.class.getSimpleName();
    private ManageTokenListAdapter mAdapter;
    private RecyclerView mTokenList;
    private List<TokenListMetaData.TokenInfo> mTokens;
    private ItemTouchHelper mItemTouchHelper;

    @Override
    public int getLayoutId() {
        return R.layout.activity_manage_wallets;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mTokenList = findViewById(R.id.token_list);

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

            if (!tokenSymbol.equalsIgnoreCase("btc") && !tokenSymbol.equalsIgnoreCase("bch") &&
                    !tokenSymbol.equalsIgnoreCase("eth") && !tokenSymbol.equalsIgnoreCase("brd")) {

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
            else if (tokenSymbol.equalsIgnoreCase("brd"))
                tokenItem = new TokenItem(null, "BRD", "BRD", null);


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

                final TokenListMetaData finalMetaData = metaData;
                KVStoreManager.getInstance().putTokenListMetaData(ManageWalletsActivity.this, finalMetaData);
                mAdapter.notifyDataSetChanged();

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

                KVStoreManager.getInstance().putTokenListMetaData(ManageWalletsActivity.this, metaData);
                mAdapter.notifyDataSetChanged();

            }
        }, this);

        mTokenList.setLayoutManager(new LinearLayoutManager(ManageWalletsActivity.this));
        mTokenList.setAdapter(mAdapter);

        ItemTouchHelper.Callback callback = new SimpleItemTouchHelperCallback(mAdapter);
        mItemTouchHelper = new ItemTouchHelper(callback);
        mItemTouchHelper.attachToRecyclerView(mTokenList);
    }

    @Override
    protected void onPause() {
        super.onPause();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
        WalletsMaster.getInstance(this).updateWallets(this);
    }

    @Override
    public void onStartDrag(RecyclerView.ViewHolder viewHolder) {
        mItemTouchHelper.startDrag(viewHolder);
    }
}
