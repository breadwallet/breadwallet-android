package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;

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
    public void onBindViewHolder(WalletItemViewHolder holder, int position) {

        BaseWalletManager wallet = mWalletList.get(position);

        // Set wallet fields
        holder.mWalletName.setText(wallet.getName(mContext));
        holder.mTradePrice.setText(CurrencyUtils.getFormattedCurrencyString(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), new BigDecimal(wallet.getFiatExchangeRate(mContext))));
        holder.mWalletBalanceUSD.setText(CurrencyUtils.getFormattedCurrencyString(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), new BigDecimal(wallet.getFiatBalance(mContext))));
//        holder.mWalletBalanceCurrency.setText(wallet.getWalletBalanceCurrency());

        if (wallet.getIso(mContext).equalsIgnoreCase("BTC")) {
            holder.mParent.setBackground(mContext.getResources().getDrawable(R.drawable.btc_card_shape, null));
        } else {
            holder.mParent.setBackground(mContext.getResources().getDrawable(R.drawable.bch_card_shape, null));

        }
    }

    @Override
    public int getItemCount() {
        return mWalletList.size();
    }

    public class WalletItemViewHolder extends RecyclerView.ViewHolder {

        public BRText mWalletName;
        public BRText mTradePrice;
        public BRText mWalletBalanceUSD;
        public BRText mWalletBalanceCurrency;
        public RelativeLayout mParent;

        public WalletItemViewHolder(View view) {
            super(view);

            mWalletName = view.findViewById(R.id.wallet_name);
            mTradePrice = view.findViewById(R.id.wallet_trade_price);
            mWalletBalanceUSD = view.findViewById(R.id.wallet_balance_usd);
            mWalletBalanceCurrency = view.findViewById(R.id.wallet_balance_currency);
            mParent = view.findViewById(R.id.wallet_card);
        }
    }
}
