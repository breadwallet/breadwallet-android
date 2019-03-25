package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.widget.CardView;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.AddWalletsActivity;
import com.breadwallet.presenter.activities.WalletActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BRNotificationBar;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.adapter.WalletListAdapter;
import com.breadwallet.tools.listeners.RecyclerItemClickListener;
import com.breadwallet.tools.manager.BREventManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.InternetManager;
import com.breadwallet.tools.manager.PromptManager;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.util.ArrayList;

public class FragmentWallet extends Fragment implements RatesDataSource.OnDataChanged{

    private static final String TAG = FragmentWallet.class.getSimpleName();

    private RecyclerView mWalletRecycler;
    private WalletListAdapter mAdapter;
    private BaseTextView mFiatTotal;
    private PromptManager.PromptItem mCurrentPrompt;
    public BRNotificationBar mNotificationBar;

    private BaseTextView mPromptTitle;
    private BaseTextView mPromptDescription;
    private BRButton mPromptContinue;
    private BRButton mPromptDismiss;
    private CardView mPromptCard;

    private View mAddWallet;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_wallet, container, false);
        initView(rootView);
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        long start = System.currentTimeMillis();

        showNextPromptIfNeeded();

        populateWallets();

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                mAdapter.startObserving();
            }
        }, 500);

        updateUi();
        Activity activity = getActivity();
        if(activity == null) return;
        RatesDataSource.getInstance(activity).addOnDataChangedListener(this);
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if(activity == null) return;
                BaseWalletManager walletManager = WalletsMaster.getInstance(activity).getCurrentWallet(activity);
                WalletsMaster.getInstance(activity).refreshBalances(activity);
                if(walletManager != null) WalletsMaster.getInstance(activity).getCurrentWallet(activity).refreshAddress(activity);
            }
        });

        onConnectionChanged(InternetManager.getInstance().isConnected(activity));
    }

    @Override
    public void onPause() {
        super.onPause();
        mAdapter.stopObserving();
    }

    private void initView(View rootView) {
        mWalletRecycler = rootView.findViewById(R.id.rv_wallet_list);
        mFiatTotal = rootView.findViewById(R.id.total_assets_usd);

        mNotificationBar = rootView.findViewById(R.id.notification_bar);

        mPromptCard = rootView.findViewById(R.id.prompt_card);
        mPromptTitle = rootView.findViewById(R.id.prompt_title);
        mPromptDescription = rootView.findViewById(R.id.prompt_description);
        mPromptContinue = rootView.findViewById(R.id.continue_button);
        mPromptDismiss = rootView.findViewById(R.id.dismiss_button);

        mAddWallet = rootView.findViewById(R.id.add_wallets);

        mAddWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(BreadApp.mContext, AddWalletsActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });


        LinearLayoutManager linearLayoutManager = new LinearLayoutManager(getActivity(), LinearLayoutManager.VERTICAL, false) {
            @Override
            public boolean canScrollVertically() {
                return false;
            }
        };
        mWalletRecycler.setLayoutManager(/*new LinearLayoutManager(BreadApp.mContext)*/linearLayoutManager);
        mWalletRecycler.setNestedScrollingEnabled(false);
        mWalletRecycler.addOnItemTouchListener(new RecyclerItemClickListener(BreadApp.mContext, mWalletRecycler, new RecyclerItemClickListener.OnItemClickListener() {
            @Override
            public void onItemClick(View view, int position, float x, float y) {
                if (position >= mAdapter.getItemCount() || position < 0) return;

                if(mAdapter.getItemAt(position).getIso().equalsIgnoreCase("ioex")) {
                    return;
                }

                if (mAdapter.getItemViewType(position) == 0) {
                    BRSharedPrefs.putCurrentWalletIso(BreadApp.mContext, mAdapter.getItemAt(position).getIso());
                    Intent newIntent = new Intent(BreadApp.mContext, WalletActivity.class);
                    startActivity(newIntent);
                    getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                }
            }

            @Override
            public void onLongItemClick(View view, int position) {

            }
        }));

        mPromptDismiss.setColor(Color.parseColor("#b3c0c8"));
        mPromptDismiss.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                hidePrompt();
            }
        });

        mPromptContinue.setColor(Color.parseColor("#4b77f3"));
        mPromptContinue.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                PromptManager.PromptInfo info = PromptManager.getInstance().promptInfo(getActivity(), mCurrentPrompt);
                if (info.listener != null)
                    info.listener.onClick(mPromptContinue);
            }
        });
    }

    public void hidePrompt() {
        mPromptCard.setVisibility(View.GONE);
        if (mCurrentPrompt == PromptManager.PromptItem.SHARE_DATA) {
            BRSharedPrefs.putPromptDismissed(BreadApp.mContext, "shareData", true);
        } else if (mCurrentPrompt == PromptManager.PromptItem.FINGER_PRINT) {
            BRSharedPrefs.putPromptDismissed(BreadApp.mContext, "fingerprint", true);
        }
        if (mCurrentPrompt != null)
            BREventManager.getInstance().pushEvent("prompt." + PromptManager.getInstance().getPromptName(mCurrentPrompt) + ".dismissed");
        mCurrentPrompt = null;

    }

    private void populateWallets() {
        Activity activity = getActivity();
        if(activity == null) return;
        ArrayList<BaseWalletManager> list = new ArrayList<>(WalletsMaster.getInstance(activity).getAllWallets(BreadApp.mContext));
        mAdapter = new WalletListAdapter(getActivity(), list);
        mWalletRecycler.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();
    }

    private void showNextPromptIfNeeded() {
        PromptManager.PromptItem toShow = PromptManager.getInstance().nextPrompt(BreadApp.mContext);
        if (toShow != null) {
            mCurrentPrompt = toShow;
//            Log.d(TAG, "showNextPrompt: " + toShow);
            PromptManager.PromptInfo promptInfo = PromptManager.getInstance().promptInfo(getActivity(), toShow);
            mPromptCard.setVisibility(View.VISIBLE);
            mPromptTitle.setText(promptInfo.title);
            mPromptDescription.setText(promptInfo.description);
            mPromptContinue.setOnClickListener(promptInfo.listener);

        } else {
            Log.i(TAG, "showNextPrompt: nothing to show");
        }
    }

    private void updateUi() {
        BRExecutor.getInstance().forLightWeightBackgroundTasks().execute(new Runnable() {
            @Override
            public void run() {
                Activity activity = getActivity();
                if(activity == null) return;
                final BigDecimal fiatTotalAmount = WalletsMaster.getInstance(activity).getAggregatedFiatBalance(BreadApp.mContext);
                if (fiatTotalAmount == null) {
                    Log.e(TAG, "updateUi: fiatTotalAmount is null");
                    return;
                }
                BRExecutor.getInstance().forMainThreadTasks().execute(new Runnable() {
                    @Override
                    public void run() {
                        mFiatTotal.setText(CurrencyUtils.getFormattedAmount(BreadApp.mContext,
                                BRSharedPrefs.getPreferredFiatIso(BreadApp.mContext), fiatTotalAmount));
                        mAdapter.notifyDataSetChanged();
                    }
                });

            }
        });
    }

    public static FragmentWallet newInstance(String text) {

        FragmentWallet f = new FragmentWallet();
        Bundle b = new Bundle();
        b.putString("text", text);

        f.setArguments(b);

        return f;
    }

    public void onConnectionChanged(boolean isConnected) {
        Log.d(TAG, "onConnectionChanged: isConnected: " + isConnected);
        if (isConnected) {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.INVISIBLE);
            }

            if (mAdapter != null) {
                mAdapter.startObserving();
            }
        } else {
            if (mNotificationBar != null) {
                mNotificationBar.setVisibility(View.VISIBLE);
            }
        }
    }

    @Override
    public void onChanged() {
        updateUi();
    }
}
