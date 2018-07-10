package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.content.Context;
import android.graphics.Point;
import android.os.Bundle;
import android.util.TypedValue;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.manager.FontManager;
import com.breadwallet.tools.sqlite.RatesDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;
import java.util.Currency;
import java.util.Iterator;
import java.util.List;


public class DisplayCurrencyActivity extends BaseSettingsActivity {
    private static final String TAG = DisplayCurrencyActivity.class.getName();
    private TextView mExchangeText;
    private ListView mListView;
    private CurrencyListAdapter mAdapter;
    private Button mLeftButton;
    private Button mRightButton;

    @Override
    public int getLayoutId() {
        return R.layout.activity_display_currency;
    }

    @Override
    public int getBackButtonId() {
        return R.id.back_button;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        ImageButton faq = findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                BaseWalletManager wm = WalletsMaster.getInstance(DisplayCurrencyActivity.this).getCurrentWallet(DisplayCurrencyActivity.this);
                UiUtils.showSupportFragment(DisplayCurrencyActivity.this, BRConstants.FAQ_DISPLAY_CURRENCY, wm);
            }
        });

        mExchangeText = findViewById(R.id.exchange_text);
        mListView = findViewById(R.id.currency_list_view);
        mAdapter = new CurrencyListAdapter(this);
        List<CurrencyEntity> currencies = RatesDataSource.getInstance(this).getAllCurrencies(this, "BTC");
        List<CurrencyEntity> cleanList = cleanList(currencies);
        mAdapter.addAll(cleanList);
        mLeftButton = findViewById(R.id.left_button);
        mRightButton = findViewById(R.id.right_button);
        mLeftButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButton(true);
            }
        });

        mRightButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setButton(false);
            }
        });

        int unit = BRSharedPrefs.getCryptoDenomination(this, "BTC"); // any iso, using one for all for now
        if (unit == BRConstants.CURRENT_UNIT_BITS) {
            setButton(true);
        } else {
            setButton(false);
        }
        updateExchangeRate();
        mListView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                TextView currencyItemText = view.findViewById(R.id.currency_item_text);
                final String selectedCurrency = currencyItemText.getText().toString();
                String iso = selectedCurrency.substring(0, 3);
                BRSharedPrefs.putPreferredFiatIso(DisplayCurrencyActivity.this, iso);

                updateExchangeRate();

            }

        });
        mListView.addFooterView(new View(this), null, true);
        mListView.addHeaderView(new View(this), null, true);
        mListView.setAdapter(mAdapter);
        mAdapter.notifyDataSetChanged();

    }

    private List<CurrencyEntity> cleanList(List<CurrencyEntity> list) {

        Iterator<CurrencyEntity> iter = list.iterator();
        while (iter.hasNext()) {
            CurrencyEntity ent = iter.next();
            if (WalletsMaster.getInstance(this).isIsoCrypto(this, ent.name)) {
                iter.remove();
            }
        }
        return list;
    }

    private void updateExchangeRate() {
        //set the rate from the last saved
        String iso = BRSharedPrefs.getPreferredFiatIso(this);
        CurrencyEntity entity = RatesDataSource.getInstance(this).getCurrencyByCode(this, "BTC", iso);//hard code BTC for this one
        if (entity != null) {
            String formattedExchangeRate = CurrencyUtils.getFormattedAmount(DisplayCurrencyActivity.this, BRSharedPrefs.getPreferredFiatIso(this), new BigDecimal(entity.rate));
            mExchangeText.setText(String.format("%s = %s", CurrencyUtils.getFormattedAmount(this, "BTC", new BigDecimal(100000000)), formattedExchangeRate));
        }
        mAdapter.notifyDataSetChanged();
    }

    private void setButton(boolean left) {
        if (left) {
            BRSharedPrefs.putCryptoDenomination(this, "BTC", BRConstants.CURRENT_UNIT_BITS);
            mLeftButton.setTextColor(getColor(R.color.white));
            mLeftButton.setBackground(getDrawable(R.drawable.b_half_left_blue));
            mRightButton.setTextColor(getColor(R.color.dark_blue));
            mRightButton.setBackground(getDrawable(R.drawable.b_half_right_blue_stroke));
        } else {
            BRSharedPrefs.putCryptoDenomination(this, "BTC", BRConstants.CURRENT_UNIT_BITCOINS);
            mLeftButton.setTextColor(getColor(R.color.dark_blue));
            mLeftButton.setBackground(getDrawable(R.drawable.b_half_left_blue_stroke));
            mRightButton.setTextColor(getColor(R.color.white));
            mRightButton.setBackground(getDrawable(R.drawable.b_half_right_blue));
        }
        updateExchangeRate();

    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    public class CurrencyListAdapter extends ArrayAdapter<CurrencyEntity> {
        public final String TAG = CurrencyListAdapter.class.getName();

        private final Context mContext;
        private final int layoutResourceId;
        private TextView textViewItem;
        private final Point displayParameters = new Point();

        public CurrencyListAdapter(Context mContext) {

            super(mContext, R.layout.currency_list_item);

            this.layoutResourceId = R.layout.currency_list_item;
            this.mContext = mContext;
            ((Activity) mContext).getWindowManager().getDefaultDisplay().getSize(displayParameters);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {

            final String oldIso = BRSharedPrefs.getPreferredFiatIso(mContext);
            if (convertView == null) {
                // inflate the layout
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(layoutResourceId, parent, false);
            }
            // get the TextView and then set the text (item name) and tag (item ID) values
            textViewItem = convertView.findViewById(R.id.currency_item_text);
            FontManager.overrideFonts(textViewItem);
            String iso = getItem(position).code;
            Currency c = null;
            try {
                c = Currency.getInstance(iso);
            } catch (IllegalArgumentException ignored) {
            }
            textViewItem.setText(c == null ? iso : String.format("%s (%s)", iso, c.getSymbol()));
            ImageView checkMark = convertView.findViewById(R.id.currency_checkmark);

            if (iso.equalsIgnoreCase(oldIso)) {
                checkMark.setVisibility(View.VISIBLE);
            } else {
                checkMark.setVisibility(View.GONE);
            }
            normalizeTextView();
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

        private boolean isTextSizeAcceptable(TextView textView) {
            textView.measure(0, 0);
            int textWidth = textView.getMeasuredWidth();
            int checkMarkWidth = 76 + 20;
            return (textWidth <= (displayParameters.x - checkMarkWidth));
        }

        private boolean normalizeTextView() {
            int count = 0;
//        Log.d(TAG, "Normalizing the text view !!!!!!");
            while (!isTextSizeAcceptable(textViewItem)) {
                count++;
                float textSize = textViewItem.getTextSize();
//            Log.e(TAG, "The text size is: " + String.valueOf(textSize));
                textViewItem.setTextSize(TypedValue.COMPLEX_UNIT_PX, textSize - 2);
                this.notifyDataSetChanged();
            }
            return (count > 0);
        }

    }

}
