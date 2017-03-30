package com.breadwallet.presenter.activities.settings;

import android.app.Activity;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.ListView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.tools.adapter.CurrencyListAdapter;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRCurrency;

import java.math.BigDecimal;

import static com.breadwallet.presenter.activities.BreadActivity.app;

public class DefaultCurrencyActivity extends Activity {
    private static final String TAG = DefaultCurrencyActivity.class.getName();
    private TextView exchangeText;
    private ListView listView;

    private CurrencyListAdapter adapter;
    private String ISO;
    private float rate;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_default_currency);
        setStatusBarColor(android.R.color.transparent);

        exchangeText = (TextView) findViewById(R.id.exchange_text);
        listView = (ListView) findViewById(R.id.currency_list_view);
        adapter = new CurrencyListAdapter(this);
        adapter.addAll(CurrencyDataSource.getInstance(this).getAllCurrencies());

        //set the rate from the last saved
        String iso = SharedPreferencesManager.getIso(this);
        CurrencyEntity entity = CurrencyDataSource.getInstance(this).getCurrencyByIso(iso);
        if(entity != null) {
            String finalExchangeRate = BRCurrency.getFormattedCurrencyString(DefaultCurrencyActivity.this, SharedPreferencesManager.getIso(this), new BigDecimal(entity.rate));
            exchangeText.setText(finalExchangeRate + " = 1BTC");
        }

        listView.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                TextView currencyItemText = (TextView) view.findViewById(R.id.currency_item_text);
                final String selectedCurrency = currencyItemText.getText().toString();
                ISO = selectedCurrency.substring(0, 3);
//                lastItemsPosition = position;
                CurrencyEntity item = adapter.getItem(position);
                rate = item == null ? 0 : item.rate;
                SharedPreferencesManager.putIso(app, ISO);
                SharedPreferencesManager.putCurrencyListPosition(DefaultCurrencyActivity.this, position);
//                SharedPreferencesManager.putRate(app, rate);
                String finalExchangeRate = BRCurrency.getFormattedCurrencyString(DefaultCurrencyActivity.this, ISO, new BigDecimal(rate));
                exchangeText.setText(finalExchangeRate + " = 1BTC");
//                MiddleViewAdapter.resetMiddleView(app, finalExchangeRate);
                adapter.notifyDataSetChanged();

            }

        });
        listView.setAdapter(adapter);
        adapter.notifyDataSetChanged();
        Log.e(TAG, "onCreate: " + listView.getCount());

    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
    }

}
