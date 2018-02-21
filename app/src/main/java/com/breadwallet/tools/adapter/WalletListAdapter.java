package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.manager.BRSharedPrefs;

import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Created by byfieldj on 1/31/18.
 */

public class WalletListAdapter extends RecyclerView.Adapter<WalletListAdapter.WalletItemViewHolder> {

    public static final String TAG = WalletListAdapter.class.getName();

    private final Context mContext;
    private ArrayList<BaseWalletManager> mWalletList;



    public WalletListAdapter(Context context, ArrayList<BaseWalletManager> walletList) {
        this.mContext = context;
        this.mWalletList = walletList;
    }

    @Override
    public WalletItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.item_wallet, parent, false);

        return new WalletItemViewHolder(convertView);
    }

    public BaseWalletManager getItemAt(int pos) {
        return mWalletList.get(pos);
    }

    @Override
    public void onBindViewHolder(final WalletItemViewHolder holder, int position) {



        final BaseWalletManager wallet = mWalletList.get(position);
        String name = wallet.getName(mContext);

        final String iso = wallet.getIso(mContext);
        String exchangeRate = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), new BigDecimal(wallet.getFiatExchangeRate(mContext)));
        String fiatBalance = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), new BigDecimal(wallet.getFiatBalance(mContext)));
        String cryptoBalance = CurrencyUtils.getFormattedAmount(mContext, wallet.getIso(mContext), new BigDecimal(wallet.getCachedBalance(mContext)));


        // Set wallet fields
        holder.mWalletName.setText(name);
        holder.mTradePrice.setText(exchangeRate);
        holder.mWalletBalanceUSD.setText(fiatBalance);
        holder.mWalletBalanceCurrency.setText(cryptoBalance);
        holder.mSyncingProgressBar.setVisibility(View.INVISIBLE);
        holder.mSyncing.setVisibility(View.INVISIBLE);
        holder.mWalletBalanceCurrency.setVisibility(View.VISIBLE);
        holder.mWalletBalanceCurrency.setText(cryptoBalance);
        holder.mWait.setVisibility(View.INVISIBLE);



        if (wallet.getIso(mContext).equalsIgnoreCase(WalletBitcoinManager.getInstance(mContext).getIso(mContext))) {
            holder.mParent.setBackground(mContext.getResources().getDrawable(R.drawable.btc_card_shape, null));
        } else {
            holder.mParent.setBackground(mContext.getResources().getDrawable(R.drawable.bch_card_shape, null));

        }

    }

    public ArrayList<BaseWalletManager> getWalletList() {
        return mWalletList;

    }

    @Override
    public int getItemCount() {
        Log.d(TAG, "Wallet list size -> " + mWalletList.size());
        return mWalletList.size();
    }

    public class WalletItemViewHolder extends RecyclerView.ViewHolder {

        public BRText mWalletName;
        public BRText mTradePrice;
        public BRText mWalletBalanceUSD;
        public BRText mWalletBalanceCurrency;
        public RelativeLayout mParent;
        public BRText mSyncing;
        public ProgressBar mSyncingProgressBar;
        public BRText mWait;


        public WalletItemViewHolder(View view) {
            super(view);

            mWalletName = view.findViewById(R.id.wallet_name);
            mTradePrice = view.findViewById(R.id.wallet_trade_price);
            mWalletBalanceUSD = view.findViewById(R.id.wallet_balance_usd);
            mWalletBalanceCurrency = view.findViewById(R.id.wallet_balance_currency);
            mParent = view.findViewById(R.id.wallet_card);
            mSyncing = view.findViewById(R.id.syncing);
            mSyncingProgressBar = view.findViewById(R.id.sync_progress);
            mWait = view.findViewById(R.id.wait_syncing);

        }
    }
}
