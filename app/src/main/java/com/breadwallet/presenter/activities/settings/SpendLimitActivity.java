package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.util.List;

public class SpendLimitActivity extends BRActivity {
    private static final String TAG = SpendLimitActivity.class.getName();
    private ListView listView;
    private LimitAdaptor adapter;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spend_limit);

        ImageButton faq = findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                BaseWalletManager wm = WalletsMaster.getInstance(SpendLimitActivity.this).getCurrentWallet(SpendLimitActivity.this);
                UiUtils.showSupportFragment(SpendLimitActivity.this, BRConstants.FAQ_FINGERPRINT_SPENDING_LIMIT, wm);
            }
        });

        listView = findViewById(R.id.limit_list);
        listView.setFooterDividersEnabled(true);
        adapter = new LimitAdaptor(this);
        final BaseWalletManager wm = WalletsMaster.getInstance(this).getCurrentWallet(this);

        List<BigDecimal> limits = wm.getSettingsConfiguration().getFingerprintLimits();

        adapter.addAll(limits);

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view, int position, long id) {
                BigDecimal limit = adapter.getItem(position);
                if (limit == null) {
                    BRReportsManager.reportBug(new RuntimeException("limit is null for: " + wm.getIso() + ", pos:" + position));
                    Log.e(TAG, "onItemClick: limit is null!");
                    return;
                }
                BRKeyStore.putSpendLimit(SpendLimitActivity.this, limit, wm.getIso());
                BigDecimal totalSent = wm.getTotalSent(SpendLimitActivity.this);
                BRKeyStore.putTotalLimit(SpendLimitActivity.this, totalSent.add(BRKeyStore.getSpendLimit(SpendLimitActivity.this, wm.getIso())), wm.getIso());
                adapter.notifyDataSetChanged();
            }

        });
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();

    }

    private int getStepFromLimit(BigDecimal limit) {
        List<BigDecimal> limits = WalletsMaster.getInstance(this).getCurrentWallet(this).getSettingsConfiguration().getFingerprintLimits();
        for (int i = 0; i < limits.size(); i++) {
            if (limits.get(i).compareTo(limit) == 0) return i;
        }
        return -1;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    public class LimitAdaptor extends ArrayAdapter<BigDecimal> {

        private final Context mContext;
        private final int layoutResourceId;
        private BaseTextView textViewItem;

        public LimitAdaptor(Context mContext) {

            super(mContext, R.layout.currency_list_item);

            this.layoutResourceId = R.layout.currency_list_item;
            this.mContext = mContext;
        }

        @Override
        public View getView(int position, View convertView, @NonNull ViewGroup parent) {
            BaseWalletManager wm = WalletsMaster.getInstance(SpendLimitActivity.this).getCurrentWallet(SpendLimitActivity.this);
            final BigDecimal limit = BRKeyStore.getSpendLimit(SpendLimitActivity.this, wm.getIso());
            if (convertView == null) {
                // inflate the layout
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }
            // get the TextView and then set the text (item name) and tag (item ID) values
            textViewItem = convertView.findViewById(R.id.currency_item_text);
            BigDecimal item = getItem(position);
            BaseWalletManager walletManager = WalletsMaster.getInstance(SpendLimitActivity.this).getCurrentWallet(SpendLimitActivity.this);

            String cryptoAmount = CurrencyUtils.getFormattedAmount(SpendLimitActivity.this, walletManager.getIso(), item);

            String text = String.format(item.compareTo(BigDecimal.ZERO) == 0 ? getString(R.string.TouchIdSpendingLimit) : "%s", cryptoAmount);
            textViewItem.setText(text);
            ImageView checkMark = convertView.findViewById(R.id.currency_checkmark);

            if (position == getStepFromLimit(limit)) {
                checkMark.setVisibility(View.VISIBLE);
            } else {
                checkMark.setVisibility(View.GONE);
            }
            return convertView;

        }

        @Override
        public int getCount() {
            return super.getCount();
        }

        @Override
        public int getItemViewType(int position) {
            return IGNORE_ITEM_VIEW_TYPE;
        }

    }

}
