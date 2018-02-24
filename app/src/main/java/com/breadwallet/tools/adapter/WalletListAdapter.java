package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
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
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConnectivityStatus;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.breadwallet.wallet.wallets.bitcoin.WalletBitcoinManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.Map;

/**
 * Created by byfieldj on 1/31/18.
 */

public class WalletListAdapter extends RecyclerView.Adapter<WalletListAdapter.WalletItemViewHolder> {

    public static final String TAG = WalletListAdapter.class.getName();

    private final Context mContext;
    private ArrayList<WalletItem> mWalletItems;
    private WalletItem mCurrentWalletSyncing;
    private ProgressTask mProgressTask;

//    private String currentlySyncingWallet;
//    private int currentlySyncingWalletPosition;


    public WalletListAdapter(Context context, ArrayList<BaseWalletManager> walletList) {
        this.mContext = context;
        mWalletItems = new ArrayList<>();
        for (BaseWalletManager w : walletList) {
            this.mWalletItems.add(new WalletItem(w, null));
        }

    }

    @Override
    public WalletItemViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.item_wallet, parent, false);

        return new WalletItemViewHolder(convertView);
    }

    public BaseWalletManager getItemAt(int pos) {
        return mWalletItems.get(pos).walletManager;
    }

    @Override
    public void onBindViewHolder(final WalletItemViewHolder holder, int position) {

        final BaseWalletManager wallet = mWalletItems.get(position).walletManager;
        mWalletItems.get(position).viewHolder = holder;
        String name = wallet.getName(mContext);
        String exchangeRate = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), new BigDecimal(wallet.getFiatExchangeRate(mContext)));
        String fiatBalance = CurrencyUtils.getFormattedAmount(mContext, BRSharedPrefs.getPreferredFiatIso(mContext), new BigDecimal(wallet.getFiatBalance(mContext)));
        String cryptoBalance = CurrencyUtils.getFormattedAmount(mContext, wallet.getIso(mContext), new BigDecimal(wallet.getCachedBalance(mContext)));

        final String iso = wallet.getIso(mContext);

        // Set wallet fields
        holder.mWalletName.setText(name);
        holder.mTradePrice.setText(exchangeRate);
        holder.mWalletBalanceUSD.setText(fiatBalance);
        holder.mWalletBalanceCurrency.setText(cryptoBalance);
        holder.mSyncingProgressBar.setVisibility(View.VISIBLE);
        holder.mSyncing.setText("Waiting to Sync");
        holder.mWalletBalanceCurrency.setVisibility(View.INVISIBLE);

        // TODO : Align the "waiting to sync" text with the balance USD

        if (wallet.getIso(mContext).equalsIgnoreCase(WalletBitcoinManager.getInstance(mContext).getIso(mContext))) {
            holder.mParent.setBackground(mContext.getResources().getDrawable(R.drawable.btc_card_shape, null));
        } else {
            holder.mParent.setBackground(mContext.getResources().getDrawable(R.drawable.bch_card_shape, null));
        }

        String lastUsedWalletIso = BRSharedPrefs.getCurrentWalletIso(mContext);

        BRConnectivityStatus connectivityChecker = new BRConnectivityStatus(mContext);

        int connectionStatus = connectivityChecker.isMobileDataOrWifiConnected();

        Log.d(TAG, "Current wallet ISO -> " + iso + ", last used wallet ISO -> " + lastUsedWalletIso);
        Log.d(TAG, "Connection status -> " + connectionStatus);


//        // Mobile or Wifi is ON, sync the last used wallet
//        if (iso.equals(lastUsedWalletIso)) {
//            Log.d(TAG, "Current wallet is last used, Connection Status -> " + connectionStatus);
//            if (connectionStatus == BRConnectivityStatus.MOBILE_ON || connectionStatus == BRConnectivityStatus.WIFI_ON || connectionStatus == BRConnectivityStatus.MOBILE_WIFI_ON) {
//
//                //syncWallet(wallet, holder);
//
//
//            } else {
//                Log.e(TAG, "onBindViewHolder: Can't connect, connectivity status: " + connectionStatus);
//            }
//        } else {
//
//        }

    }

    public void stopObserving() {
        if (mProgressTask != null)
            mProgressTask.interrupt();
    }

    public void startObserving() {
        Log.e(TAG, "startObserving..");
        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                mCurrentWalletSyncing = getNextWalletToSync();
                if (mCurrentWalletSyncing == null) {
                    Log.e(TAG, "startObserving: all wallets synced.");
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            for (WalletItem item : mWalletItems) {
                                item.viewHolder.mSyncingProgressBar.setVisibility(View.INVISIBLE);
                                item.viewHolder.mSyncing.setVisibility(View.INVISIBLE);
                                item.viewHolder.mWalletBalanceCurrency.setVisibility(View.VISIBLE);
                                item.viewHolder.mSyncingProgressBar.setProgress(100);
                            }
                        }
                    });

                    return;
                }

                final BaseWalletManager currentWallet = mCurrentWalletSyncing.walletManager;
                //update the appropriate viewHolder
                for (WalletItem w : mWalletItems) {
                    if (w.walletManager.getIso(mContext).equalsIgnoreCase(mCurrentWalletSyncing.walletManager.getIso(mContext))) {
                        mCurrentWalletSyncing.viewHolder = w.viewHolder;
                        break;
                    }
                }
                Log.e(TAG, "startObserving: connecting: " + mCurrentWalletSyncing.walletManager.getIso(mContext));
                currentWallet.connectWallet(mContext);

                if (mProgressTask != null) mProgressTask.interrupt();
                mProgressTask = new ProgressTask(currentWallet);
                mProgressTask.start();

            }
        });

    }

    class ProgressTask extends Thread {

        private BaseWalletManager mCurrentWallet;
        private boolean mRunning = true;
        private static final int DELAY_MILLIS = 500;

        public ProgressTask(BaseWalletManager currentWallet) {
            mCurrentWallet = currentWallet;
        }

        @Override
        public void run() {
            while (mRunning) {
                final double syncProgress = mCurrentWallet.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(mContext, mCurrentWallet.getIso(mContext)));

                new Handler(Looper.getMainLooper()).post(new Runnable() {
                    @Override
                    public void run() {
                        mRunning = updateUi(mCurrentWallet, syncProgress);

                    }
                });

                try {
                    Thread.sleep(DELAY_MILLIS);
                } catch (InterruptedException e) {
                    e.printStackTrace();
                    new Handler(Looper.getMainLooper()).post(new Runnable() {
                        @Override
                        public void run() {
                            mRunning = updateUi(mCurrentWallet, syncProgress);
                        }
                    });
                }

            }

        }
    }

    private boolean updateUi(BaseWalletManager currentWallet, double syncProgress) {
        if (mCurrentWalletSyncing == null || mCurrentWalletSyncing.viewHolder == null) {
            Log.e(TAG, "run: should not happen but ok, ignore it.");
            return false;
        }
        if (syncProgress > 0.0 && syncProgress < 1.0) {
            int progress = (int) (syncProgress * 100);
            Log.d(TAG, "ISO: " + currentWallet.getIso(mContext) + " (" + progress + "%)");
            mCurrentWalletSyncing.viewHolder.mSyncingProgressBar.setVisibility(View.VISIBLE);
            mCurrentWalletSyncing.viewHolder.mSyncing.setText("Syncing ");
            mCurrentWalletSyncing.viewHolder.mSyncingProgressBar.setProgress(progress);
        }


        // HAS NOT STARTED SYNCING
        else if (syncProgress == 0.0) {
            Log.d(TAG, "ISO: " + currentWallet.getIso(mContext) + " (0%)");
            mCurrentWalletSyncing.viewHolder.mSyncingProgressBar.setVisibility(View.INVISIBLE);
            mCurrentWalletSyncing.viewHolder.mSyncing.setText("Waiting to Sync");
            mCurrentWalletSyncing.viewHolder.mSyncingProgressBar.setProgress(0);
        }

        // FINISHED SYNCING
        else if (syncProgress == 1.0) {
            Log.d(TAG, "ISO: " + currentWallet.getIso(mContext) + " (100%)");
            mCurrentWalletSyncing.viewHolder.mSyncingProgressBar.setVisibility(View.INVISIBLE);
            mCurrentWalletSyncing.viewHolder.mSyncing.setVisibility(View.INVISIBLE);
            mCurrentWalletSyncing.viewHolder.mWalletBalanceCurrency.setVisibility(View.VISIBLE);
            mCurrentWalletSyncing.viewHolder.mSyncingProgressBar.setProgress(100);
            //start from beginning
            startObserving();
            return false;

        }
        return true;
    }

    //return the next wallet that is not connected or null if all are connected
    private WalletItem getNextWalletToSync() {
        BaseWalletManager currentWallet = WalletsMaster.getInstance(mContext).getCurrentWallet(mContext);
        if (currentWallet.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(mContext, currentWallet.getIso(mContext))) == 1)
            currentWallet = null;

        for (WalletItem w : mWalletItems) {
            if (currentWallet == null) {
                if (w.walletManager.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(mContext, w.walletManager.getIso(mContext))) < 1 ||
                        w.walletManager.getPeerManager().getConnectStatus() != BRCorePeer.ConnectStatus.Connected) {
                    w.walletManager.getPeerManager().connect();
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
        public BRText mSyncing;
        public ProgressBar mSyncingProgressBar;

        public WalletItemViewHolder(View view) {
            super(view);

            mWalletName = view.findViewById(R.id.wallet_name);
            mTradePrice = view.findViewById(R.id.wallet_trade_price);
            mWalletBalanceUSD = view.findViewById(R.id.wallet_balance_usd);
            mWalletBalanceCurrency = view.findViewById(R.id.wallet_balance_currency);
            mParent = view.findViewById(R.id.wallet_card);
            mSyncing = view.findViewById(R.id.syncing);
            mSyncingProgressBar = view.findViewById(R.id.sync_progress);
        }
    }

    private class WalletItem {
        public BaseWalletManager walletManager;
        public WalletItemViewHolder viewHolder;

        public WalletItem(BaseWalletManager walletManager, WalletItemViewHolder viewHolder) {
            this.walletManager = walletManager;
            this.viewHolder = viewHolder;
        }
    }
}
