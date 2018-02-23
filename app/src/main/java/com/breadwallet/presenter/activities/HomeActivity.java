package com.breadwallet.presenter.activities;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.core.BRCorePeer;
import com.breadwallet.presenter.activities.settings.SecurityCenterActivity;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.tools.adapter.WalletListAdapter;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConnectivityStatus;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;

/**
 * Created by byfieldj on 1/17/18.
 * <p>
 * Dummy Home activity to simulate navigating to and from the new Currency screen
 */

public class HomeActivity extends BRActivity {

    private RecyclerView mWalletRecycler;
    private WalletListAdapter mAdapter;
    private BRText mFiatTotal;
    private RelativeLayout mSettings;
    private RelativeLayout mSecurity;
    private RelativeLayout mSupport;

    private static HomeActivity app;
    private static final String TAG = "HomeActivity";
    private Integer mCurrentlySyncingWalletPos;
    private ArrayList<BaseWalletManager> mWallets;

    public static HomeActivity getApp() {
        return app;
    }


    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_home);

        WalletsMaster.getInstance(HomeActivity.this).initWallets(HomeActivity.this);

        ArrayList<BaseWalletManager> walletList = new ArrayList<>();

        walletList.addAll(WalletsMaster.getInstance(this).getAllWallets());

        mWalletRecycler = findViewById(R.id.rv_wallet_list);
        mFiatTotal = findViewById(R.id.total_assets_usd);

        mSettings = findViewById(R.id.settings_row);
        mSecurity = findViewById(R.id.security_row);
        mSupport = findViewById(R.id.support_row);

        mAdapter = new WalletListAdapter(this, walletList);

        mWalletRecycler.setLayoutManager(new LinearLayoutManager(this));
        mWalletRecycler.setAdapter(mAdapter);

        mWalletRecycler.addOnItemTouchListener(new RecyclerItemClickListener(this, mWalletRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                BRSharedPrefs.putCurrentWalletIso(HomeActivity.this, mAdapter.getItemAt(position).getIso(HomeActivity.this));
                Log.d("HomeActivity", "Saving current wallet ISO as " + mAdapter.getItemAt(position).getIso(HomeActivity.this));

                Intent newIntent = new Intent(HomeActivity.this, WalletActivity.class);
                startActivity(newIntent);
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        mSettings.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SettingsActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
        mSecurity.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(HomeActivity.this, SecurityCenterActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_bottom, R.anim.empty_300);
            }
        });
        mSupport.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(HomeActivity.this, null);
            }
        });


        String lastUsedWalletIso = BRSharedPrefs.getCurrentWalletIso(this);

        BRConnectivityStatus connectivityChecker = new BRConnectivityStatus(this);

        int connectionStatus = connectivityChecker.isMobileDataOrWifiConnected();


        // Mobile or Wifi is ON, sync the last used wallet
        mWallets = mAdapter.getWalletList();

        for (final BaseWalletManager wallet : mWallets) {


            // Get the iso of each wallet
            String iso = wallet.getIso(this);

            // Compare the iso of each wallet to see if it's the same as the last used wallet
            if (iso.equals(lastUsedWalletIso)) {
                // Save the position of the first syncing wallet
                final int walletPosition = mWallets.indexOf(wallet);
                mCurrentlySyncingWalletPos = walletPosition;

                Log.d(TAG, "Current wallet is last used, Connection Status -> " + connectionStatus);
                if (connectionStatus == BRConnectivityStatus.MOBILE_ON || connectionStatus == BRConnectivityStatus.WIFI_ON || connectionStatus == BRConnectivityStatus.MOBILE_WIFI_ON) {

                    mWalletRecycler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            syncWallet(wallet, mWalletRecycler.getChildAt(walletPosition));

                        }
                    }, 50);

                }
            }
        }


    }

    private void syncWallet(final BaseWalletManager wallet, final View holder) {

        final ProgressBar syncProgressBar = holder.findViewById(R.id.sync_progress);
        final BRText syncText = holder.findViewById(R.id.syncing);
        final BRText waitText = holder.findViewById(R.id.wait_syncing);
        final BRText walletBalanceUsd = holder.findViewById(R.id.wallet_balance_usd);
        final BRText walletBalanceCurrency = holder.findViewById(R.id.wallet_balance_currency);

        BRExecutor.getInstance().forBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                if (wallet.getPeerManager().getConnectStatus() == BRCorePeer.ConnectStatus.Disconnected) {
                    wallet.getPeerManager().connect();
                    Log.e(TAG, "run: core connecting");
                }
            }
        });

        final Handler progressHandler = new Handler();
        Runnable progressRunnable = new Runnable() {
            @Override
            public void run() {
                Log.d(TAG, "progressRunnable running!");
                final double syncProgress = wallet.getPeerManager().getSyncProgress(BRSharedPrefs.getStartHeight(HomeActivity.this, wallet.getIso(HomeActivity.this)));


                progressHandler.post(new Runnable() {
                    @Override
                    public void run() {

                        // SYNCING
                        if (syncProgress > 0.0 && syncProgress < 1.0) {
                            int progress = (int) (syncProgress * 100);
                            Log.d(TAG, "Sync progress -> " + syncProgress);
                            Log.d(TAG, "Wallet ISO  -> " + wallet.getIso(HomeActivity.this));
                            Log.d(TAG, "Sync status SYNCING");
                            syncProgressBar.setVisibility(View.VISIBLE);
                            syncText.setVisibility(View.VISIBLE);
                            syncText.setText("Syncing ");
                            syncProgressBar.setProgress(progress);
                            waitText.setVisibility(View.INVISIBLE);
                        }


                        // HAS NOT STARTED SYNCING
                        else if (syncProgress <= 0.0) {
                            Log.d(TAG, "Sync progress -> " + syncProgress);
                            syncProgressBar.setVisibility(View.INVISIBLE);
                            //holder.mSyncing.setText("Waiting to Sync");
                            syncText.setVisibility(View.INVISIBLE);
                            waitText.setVisibility(View.VISIBLE);
                            syncProgressBar.setVisibility(View.INVISIBLE);
                            syncProgressBar.setProgress(0);
                            Log.d(TAG, "Wallet ISO  -> " + wallet.getIso(HomeActivity.this));
                            Log.d(TAG, "Sync status NOT SYNCING");

                            RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                            params.addRule(RelativeLayout.ALIGN_LEFT, walletBalanceUsd.getId());
                            syncText.setLayoutParams(params);


                        }

                        // FINISHED SYNCING
                        else if (syncProgress == 1.0) {
                            Log.d(TAG, "Sync progress -> " + syncProgress);
                            syncProgressBar.setVisibility(View.INVISIBLE);
                            syncText.setVisibility(View.INVISIBLE);
                            waitText.setVisibility(View.INVISIBLE);
                            walletBalanceCurrency.setVisibility(View.VISIBLE);
                            syncProgressBar.setProgress(100);

                            long balance = BRSharedPrefs.getCachedBalance(HomeActivity.this, wallet.getIso(HomeActivity.this));
                            walletBalanceCurrency.setText(wallet.getIso(HomeActivity.this) + String.valueOf(balance));
                            Log.d(TAG, "Wallet ISO  -> " + wallet.getIso(HomeActivity.this));
                            Log.d(TAG, "Sync status FINISHED");

                            progressHandler.removeCallbacks(this);
                            progressHandler.removeMessages(0);


                            // After finished syncing current wallet, start syncing next wallet
                            if (mCurrentlySyncingWalletPos == 0) {
                                syncWallet(mWallets.get(1), mWalletRecycler.getChildAt(1));
                                wallet.getPeerManager().disconnect();
                                Log.d(TAG, "Finished syncing, syncing " + mWallets.get(1).getIso(HomeActivity.this) + " next");
                            } else if (mCurrentlySyncingWalletPos >= 1) {
                                syncWallet(mWallets.get(0), mWalletRecycler.getChildAt(0));
                                wallet.getPeerManager().disconnect();
                                Log.d(TAG, "Finished syncing, syncing " + mWallets.get(0).getIso(HomeActivity.this) + " next");


                            }





                        }
                    }
                });

                progressHandler.postDelayed(this, 500);

            }
        };

        progressHandler.postDelayed(progressRunnable, 500);

    }

    @Override
    protected void onResume() {
        super.onResume();
        app = this;
        BRSharedPrefs.putCurrentWalletIso(HomeActivity.this, "BCH");
        updateUi();
        CurrencyDataSource.getInstance(this).addOnDataChangedListener(new CurrencyDataSource.OnDataChanged() {
            @Override
            public void onChanged() {
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        updateUi();
                    }
                });

            }
        });



    }

    private void updateUi() {
        BigDecimal fiatTotalAmount = WalletsMaster.getInstance(this).getAgregatedFiatBalance(this);
        mFiatTotal.setText(CurrencyUtils.getFormattedCurrencyString(this, BRSharedPrefs.getPreferredFiatIso(this), fiatTotalAmount));
        mAdapter.notifyDataSetChanged();
    }

}
