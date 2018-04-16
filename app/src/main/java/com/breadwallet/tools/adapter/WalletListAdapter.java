package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.GradientDrawable;
import android.os.Handler;
import android.os.Looper;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.services.SyncService;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.util.ArrayList;

/**
 * Created by byfieldj on 1/31/18.
 */

public class WalletListAdapter extends RecyclerView.Adapter<WalletListAdapter.WalletItemViewHolder> {

    public static final String TAG = WalletListAdapter.class.getName();

    private final Context mContext;
    private ArrayList<WalletItem> mWalletItems;
    private WalletItem mCurrentWalletSyncing;
    private boolean mObesrverIsStarting;
    private SyncNotificationBroadcastReceiver mSyncNotificationBroadcastReceiver;

    public WalletListAdapter(Context context, ArrayList<BaseWalletManager> walletList) {
        this.mContext = context;
        mWalletItems = new ArrayList<>();
        for (BaseWalletManager w : walletList) {
            this.mWalletItems.add(new WalletItem(w));
        }

        mSyncNotificationBroadcastReceiver = new SyncNotificationBroadcastReceiver();
    }

    @Override
    public WalletItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.wallet_list_item, parent, false);

        return new WalletItemViewHolder(convertView);
    }

    public BaseWalletManager getItemAt(int pos) {
        return mWalletItems.get(pos).walletManager;
    }

    @Override
    public void onBindViewHolder(final WalletItemViewHolder holder, int position) {

        WalletItem item = mWalletItems.get(position);
        final BaseWalletManager wallet = item.walletManager;
        String name = wallet.getName(mContext);
        final String iso = wallet.getIso(mContext);
        String exchangeRate = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), wallet.getFiatExchangeRate(mContext));
        String fiatBalance = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), wallet.getFiatBalance(mContext));
        String cryptoBalance = CurrencyUtils.getFormattedAmount(mContext, wallet.getIso(mContext), wallet.getCachedBalance(mContext));

        // Set wallet fields
        holder.mWalletName.setText(name);
        holder.mTradePrice.setText(mContext.getString(R.string.Account_exchangeRate, exchangeRate, iso));
        holder.mWalletBalanceUSD.setText(fiatBalance);
        holder.mWalletBalanceCurrency.setText(cryptoBalance);
        holder.mSyncingProgressBar.setVisibility(item.mShowSyncing ? View.VISIBLE : View.INVISIBLE);
        holder.mSyncingProgressBar.setProgress(item.mProgress);
        holder.mSyncingLabel.setVisibility(item.mShowSyncingLabel ? View.VISIBLE : View.INVISIBLE);
        holder.mSyncingLabel.setText(item.mLabelText);
        holder.mWalletBalanceCurrency.setVisibility(item.mShowBalance ? View.VISIBLE : View.INVISIBLE);

        String startColor = wallet.getUiConfiguration().mStartColor;
        String endColor = wallet.getUiConfiguration().mEndColor;

//        Log.e(TAG, "onBindViewHolder: start: " + startColor + ", end: " + endColor);
        Drawable drawable = mContext.getResources().getDrawable(R.drawable.crypto_card_shape, null).mutate();
        //create gradient with 2 colors if exist
        ((GradientDrawable) drawable).setColors(new int[]{Color.parseColor(startColor), Color.parseColor(endColor == null ? startColor : endColor)});
        ((GradientDrawable) drawable).setOrientation(GradientDrawable.Orientation.LEFT_RIGHT);
        holder.mParent.setBackground(drawable);
        Log.e(TAG, "onBindViewHolder: " );

    }

    public void stopObserving() {
        SyncService.unregisterSyncNotificationBroadcastReceiver(mContext.getApplicationContext(), mSyncNotificationBroadcastReceiver);
    }

    public void startObserving() {
        if (mObesrverIsStarting) return;
        mObesrverIsStarting = true;

        SyncService.registerSyncNotificationBroadcastReceiver(mContext.getApplicationContext(), mSyncNotificationBroadcastReceiver);

        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                try {
                    mCurrentWalletSyncing = getNextWalletToSync();
                    if (mCurrentWalletSyncing == null) {
                        new Handler(Looper.getMainLooper()).post(new Runnable() {
                            @Override
                            public void run() {
                                for (WalletItem item : mWalletItems) {

                                    item.updateData(false, false, true, 100, "Done");
                                    notifyDataSetChanged();

                                }
                            }
                        });

                        return;
                    }
                    String walletIso = mCurrentWalletSyncing.walletManager.getIso(mContext);
                    mCurrentWalletSyncing.walletManager.connect(mContext);
                    SyncService.startService(mContext.getApplicationContext(), SyncService.ACTION_START_SYNC_PROGRESS_POLLING, walletIso);
                } finally {
                    mObesrverIsStarting = false;
                }

            }
        });

    }

    private boolean updateUi(WalletItem currentWallet, double syncProgress) {

        if (mCurrentWalletSyncing == null || mCurrentWalletSyncing.walletManager == null) {
            Log.e(TAG, "run: should not happen but ok, ignore it.");
            return false;
        }
        if (syncProgress > 0.0 && syncProgress < 1.0) {
            int progress = (int) (syncProgress * 100);
//            Log.d(TAG, "ISO: " + currentWallet.walletManager.getIso(mContext) + " (" + progress + "%)");

            mCurrentWalletSyncing.updateData(true, true, false, progress, "Syncing");
            notifyDataSetChanged();
        }

        // HAS NOT STARTED SYNCING
        else if (syncProgress == 0.0) {
//            Log.d(TAG, "ISO: " + currentWallet.walletManager.getIso(mContext) + " (0%)");
            mCurrentWalletSyncing.updateData(false, true, false, 0, "Waiting to Sync");
            notifyDataSetChanged();
        }

        // FINISHED SYNCING
        else if (syncProgress == 1.0) {
//            Log.d(TAG, "ISO: " + currentWallet.walletManager.getIso(mContext) + " (100%)");

            //Done should not be seen but if it is because of a bug or something, then let if be a decent explanation
            mCurrentWalletSyncing.updateData(false, false, true, 100, "Done");
            notifyDataSetChanged();

            //start from beginning
            startObserving();
            return false;

        }
        return true;
    }

    //return the next wallet that is not connected or null if all are connected
    private WalletItem getNextWalletToSync() {
        BaseWalletManager currentWallet = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        if (currentWallet.getSyncProgress(BRSharedPrefs.getStartHeight(mContext, currentWallet.getIso(mContext))) == 1) {
            currentWallet = null;
        }

        for (WalletItem w : mWalletItems) {
            if (currentWallet == null) {
                if (w.walletManager.getSyncProgress(BRSharedPrefs.getStartHeight(mContext, w.walletManager.getIso(mContext))) < 1 ||
                        w.walletManager.getConnectStatus() != 2) {
                    w.walletManager.connect(mContext);
                    return w;
                }
            } else {
                if (w.walletManager.getIso(mContext).equalsIgnoreCase(currentWallet.getIso(mContext)))
                    return w;
            }
        }
        return null;
    }

    @Override
    public int getItemCount() {
        return mWalletItems.size();
    }

    public class WalletItemViewHolder extends RecyclerView.ViewHolder {

        public BRText mWalletName;
        public BRText mTradePrice;
        public BRText mWalletBalanceUSD;
        public BRText mWalletBalanceCurrency;
        public RelativeLayout mParent;
        public BRText mSyncingLabel;
        public ProgressBar mSyncingProgressBar;

        public WalletItemViewHolder(View view) {
            super(view);

            mWalletName = view.findViewById(R.id.wallet_name);
            mTradePrice = view.findViewById(R.id.wallet_trade_price);
            mWalletBalanceUSD = view.findViewById(R.id.wallet_balance_usd);
            mWalletBalanceCurrency = view.findViewById(R.id.wallet_balance_currency);
            mParent = view.findViewById(R.id.wallet_card);
            mSyncingLabel = view.findViewById(R.id.syncing_label);
            mSyncingProgressBar = view.findViewById(R.id.sync_progress);
        }
    }

    private class WalletItem {
        public BaseWalletManager walletManager;
        private boolean mShowSyncing = false;
        private boolean mShowSyncingLabel = false;
        private boolean mShowBalance = true;
        private int mProgress = 1; //1 - 100%
        private String mLabelText = "Done";

        public WalletItem(BaseWalletManager walletManager) {
            this.walletManager = walletManager;
        }

        public void updateData(boolean showSyncing, boolean showSyncingLabel, boolean showBalance, int progress, String labelText) {
            mShowSyncing = showSyncing;
            mShowSyncingLabel = showSyncingLabel;
            mShowBalance = showBalance;
            mProgress = progress;
            mLabelText = labelText;
        }
    }

    /**
     * The {@link SyncNotificationBroadcastReceiver} is responsible for receiving updates from the
     * {@link SyncService} and updating the UI accordingly.
     */
    private class SyncNotificationBroadcastReceiver extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            if (SyncService.ACTION_SYNC_PROGRESS_UPDATE.equals(intent.getAction())) {
                String intentWalletIso = intent.getStringExtra(SyncService.EXTRA_WALLET_ISO);
                double progress = intent.getDoubleExtra(SyncService.EXTRA_PROGRESS, SyncService.PROGRESS_NOT_DEFINTED);

                if (mCurrentWalletSyncing == null) {
                    Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: mCurrentWalletSyncing is null. Wallet:" + intentWalletIso + " Progress:" + progress + " Ignored");
                    return;
                }

                String currentWalletISO = mCurrentWalletSyncing.walletManager.getIso(context.getApplicationContext());
                if (currentWalletISO.equals(intentWalletIso)) {
                    if (progress >= SyncService.PROGRESS_START) {
                        updateUi(mCurrentWalletSyncing, progress);
                    } else {
                        Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Progress not set:" + progress);
                    }
                } else {
                    Log.e(TAG, "SyncNotificationBroadcastReceiver.onReceive: Wrong wallet. Expected:" + currentWalletISO + " Actual:" + intentWalletIso + " Progress:" + progress);
                }
            }
        }
    }
}
