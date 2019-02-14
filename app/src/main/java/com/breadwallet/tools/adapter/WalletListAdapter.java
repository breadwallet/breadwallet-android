package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.support.v7.widget.RecyclerView;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.model.Wallet;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.configs.WalletUiConfiguration;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.math.BigDecimal;
import java.text.NumberFormat;
import java.util.ArrayList;
import java.util.List;

/**
 * Renders a given list of wallets with their icon, balances, and exchange rate. A wallet that is
 * currently sync'ing will display its progress.
 * <p>
 * Created by byfieldj on 1/31/18.
 */

public class WalletListAdapter extends RecyclerView.Adapter<WalletListAdapter.WalletItemViewHolder> {
    public static final String TAG = WalletListAdapter.class.getName();

    private static final int VIEW_TYPE_WALLET = 0;
    private static final int VIEW_TYPE_ADD_WALLET = 1;

    private final Context mContext;
    private List<Wallet> mWallets;

    /**
     * Instantiates the adapter with an empty list of wallets.
     *
     * @param context The application context.
     */
    public WalletListAdapter(Context context) {
        this.mContext = context;
        mWallets = new ArrayList<>();
    }

    /**
     * Sets the wallets that the adapter is responsible for rendering.
     *
     * @param wallets The wallets to render.
     */
    public void setWallets(List<Wallet> wallets) {
        mWallets = wallets;
        notifyDataSetChanged();
    }

    /**
     * Creates a view holder that will display a list item (e.g., a wallet or 'Add Wallets' button display).
     *
     * @param parent   The parent view group.
     * @param viewType The view type for the holder (indicates either wallet or 'Add Wallets')
     * @return The created wallet-specific view holder.
     */
    @Override
    public WalletItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView;

        if (viewType == VIEW_TYPE_WALLET) {
            // Inflate wallet view
            convertView = inflater.inflate(R.layout.wallet_list_item, parent, false);
            return new WalletItemViewHolder(convertView);
        } else {
            // Inflate 'Add Wallets' view
            convertView = inflater.inflate(R.layout.add_wallets_item, parent, false);
            return new AddWalletItemViewHolder(convertView);
        }
    }

    /**
     * Returns a list item's view type. In the case of the last item in the list, the view type
     * will correspond to the 'Add Wallets' type.
     *
     * @param position The index of list item in question.
     * @return The view type of given list item.
     */
    @Override
    public int getItemViewType(int position) {
        if (position < mWallets.size()) {
            return VIEW_TYPE_WALLET;
        } else {
            return VIEW_TYPE_ADD_WALLET;
        }
    }

    /**
     * Returns the wallet at the given index.
     *
     * @param position The index of the wallet to return.
     * @return The wallet at the given index.
     */
    public Wallet getItemAt(int position) {
        if (position < mWallets.size()) {
            return mWallets.get(position);
        }

        return null;
    }

    /**
     * Binds the wallet data, specified by the given index, to the view holder, as well as rendering
     * any other display elements (icons, colours, etc.).
     *
     * @param holder   The view holder to be bound and rendered.
     * @param position The index of the view holder (and wallet).
     */
    @Override
    public void onBindViewHolder(final WalletItemViewHolder holder, int position) {
        if (getItemViewType(position) == VIEW_TYPE_WALLET) {
            Wallet wallet = mWallets.get(position);
            String name = wallet.getName();
            String currencyCode = wallet.getCurrencyCode();

            BigDecimal bigExchangeRate = wallet.getExchangeRate();
            BigDecimal bigFiatBalance = wallet.getFiatBalance();

            // Format numeric data
            String exchangeRate = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), bigExchangeRate);
            String fiatBalance = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), bigFiatBalance);
            String cryptoBalance = CurrencyUtils.getFormattedAmount(mContext, wallet.getCurrencyCode(), wallet.getCryptoBalance());

            if (Utils.isNullOrZero(bigExchangeRate)) {
                holder.mWalletBalanceFiat.setVisibility(View.INVISIBLE);
                holder.mTradePrice.setVisibility(View.INVISIBLE);
            } else {
                holder.mWalletBalanceFiat.setVisibility(View.VISIBLE);
                holder.mTradePrice.setVisibility(View.VISIBLE);
            }

            // Set wallet fields
            holder.mWalletName.setText(name);
            holder.mTradePrice.setText(mContext.getString(R.string.Account_exchangeRate, exchangeRate, currencyCode));
            holder.mWalletBalanceFiat.setText(fiatBalance);
            holder.mWalletBalanceFiat.setTextColor(mContext.getResources().getColor(wallet.isSyncing() ? R.color.wallet_balance_fiat_syncing : R.color.wallet_balance_fiat));
            holder.mWalletBalanceCurrency.setText(cryptoBalance);
            holder.mWalletBalanceCurrency.setVisibility(!wallet.isSyncing() ? View.VISIBLE : View.INVISIBLE);
            holder.mSyncingProgressBar.setVisibility(wallet.isSyncing() ? View.VISIBLE : View.INVISIBLE);
            holder.mSyncingLabel.setVisibility(wallet.isSyncing() ? View.VISIBLE : View.INVISIBLE);
            if (wallet.isSyncing()) {
                StringBuffer labelText = new StringBuffer(mContext.getString(R.string.SyncingView_syncing));
                labelText.append(' ')
                        .append(NumberFormat.getPercentInstance().format(wallet.getSyncProgress()));
                holder.mSyncingLabel.setText(labelText);
            }

            // Get icon for currency
            String tokenIconPath = TokenUtil.getTokenIconPath(mContext, currencyCode, false);

            if (!Utils.isNullOrEmpty(tokenIconPath)) {
                File iconFile = new File(tokenIconPath);
                Picasso.get().load(iconFile).into(holder.mLogoIcon);
                holder.mIconLetter.setVisibility(View.GONE);
                holder.mLogoIcon.setVisibility(View.VISIBLE);
            } else {
                // If no icon is present, then use the capital first letter of the token currency code instead.
                holder.mIconLetter.setVisibility(View.VISIBLE);
                holder.mLogoIcon.setVisibility(View.GONE);
                holder.mIconLetter.setText(currencyCode.substring(0, 1).toUpperCase());
            }

            WalletUiConfiguration uiConfiguration = WalletsMaster.getInstance(mContext).getWalletByIso(mContext, wallet.getCurrencyCode()).getUiConfiguration();
            String startColor = uiConfiguration.getStartColor();
            String endColor = uiConfiguration.getEndColor();
            Drawable drawable = mContext.getResources().getDrawable(R.drawable.crypto_card_shape, null).mutate();

            if (TokenUtil.isTokenSupported(currencyCode)) {
                // Create gradient if 2 colors exist.
                ((GradientDrawable) drawable).setColors(new int[]{Color.parseColor(startColor), Color.parseColor(endColor == null ? startColor : endColor)});
                ((GradientDrawable) drawable).setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
                holder.mParent.setBackground(drawable);

                setWalletItemColors(holder, R.dimen.token_background_no_alpha);

            } else {
                // To ensure that the unsupported wallet card has the same shape as the supported wallet card, we reuse the drawable.
                ((GradientDrawable) drawable).setColors(new int[]{mContext.getResources().getColor(R.color.wallet_delisted_token_background), mContext.getResources().getColor(R.color.wallet_delisted_token_background)});
                holder.mParent.setBackground(drawable);
                setWalletItemColors(holder, R.dimen.token_background_with_alpha);
            }
        }
    }

    /**
     * Set colors on view holder.
     *
     * @param viewHolder The view holder to color.
     * @param alpha
     */
    private void setWalletItemColors(WalletItemViewHolder viewHolder, int alpha) {
        TypedValue typedValue = new TypedValue();
        mContext.getResources().getValue(alpha, typedValue, true);
        float background = typedValue.getFloat();

        viewHolder.mLogoIcon.setAlpha(background);
        viewHolder.mWalletName.setAlpha(background);
        viewHolder.mTradePrice.setAlpha(background);
        viewHolder.mWalletBalanceFiat.setAlpha(background);
        viewHolder.mWalletBalanceCurrency.setAlpha(background);
    }

    /**
     * Returns the number of display items (*not* just wallets) in the list.
     *
     * @return The number of display items.
     */
    @Override
    public int getItemCount() {
        return mWallets.size() + 1; //  number of wallets plus the 'Add Wallets' item
    }

    /**
     * Container class for Wallet-specific View Holder.
     */
    public class WalletItemViewHolder extends RecyclerView.ViewHolder {
        private BaseTextView mWalletName;
        private BaseTextView mTradePrice;
        private BaseTextView mWalletBalanceFiat;
        private BaseTextView mWalletBalanceCurrency;
        private RelativeLayout mParent;
        private BaseTextView mSyncingLabel;
        private ProgressBar mSyncingProgressBar;
        private ImageView mLogoIcon;
        private BaseTextView mIconLetter;

        public WalletItemViewHolder(View view) {
            super(view);

            mWalletName = view.findViewById(R.id.wallet_name);
            mTradePrice = view.findViewById(R.id.wallet_trade_price);
            mWalletBalanceFiat = view.findViewById(R.id.wallet_balance_fiat);
            mWalletBalanceCurrency = view.findViewById(R.id.wallet_balance_currency);
            mParent = view.findViewById(R.id.wallet_card);
            mSyncingLabel = view.findViewById(R.id.syncing_label);
            mSyncingProgressBar = view.findViewById(R.id.sync_progress);
            mLogoIcon = view.findViewById(R.id.currency_icon_white);
            mIconLetter = view.findViewById(R.id.icon_letter);
        }
    }

    /**
     * Container class for 'Add Wallets'-specific View Holder.
     */
    public class AddWalletItemViewHolder extends WalletItemViewHolder {
        public AddWalletItemViewHolder(View view) {
            super(view);
        }
    }
}
