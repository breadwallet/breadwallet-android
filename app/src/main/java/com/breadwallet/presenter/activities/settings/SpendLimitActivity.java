package com.breadwallet.presenter.activities.settings;

import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.ActivityUTILS;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.tools.util.BRExchange.getSatoshisFromAmount;
import static java.security.AccessController.getContext;

public class SpendLimitActivity extends AppCompatActivity {
    private static final String TAG = SpendLimitActivity.class.getName();
    //    private Button scanButton;
    public static boolean appVisible = false;
    private static SpendLimitActivity app;
    private SeekBar seekBar;
    private TextView label;
    private Spinner curSpiner;
    private static final long MAX_AMOUNT_SATOSHIS = 1000000000; //10 BTC

    public static SpendLimitActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spend_limit);

        label = (TextView) findViewById(R.id.limit_label);
        seekBar = (SeekBar) findViewById(R.id.seekBar);
        curSpiner = (Spinner) findViewById(R.id.cur_spinner);

        final List<String> curList = new ArrayList<>();
        curList.add("BTC");
        curList.addAll(CurrencyDataSource.getInstance(this).getAllISOs());

        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.bread_spinner_item, curList);
        curSpiner.setAdapter(adapter);
        curSpiner.setAdapter(adapter);
        curSpiner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                updateText(0);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                updateText(0);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            public int progress = 0;

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                progress = progresValue / 100 * 100;

                updateText(progress);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });

    }

    private int getMax(String iso) {
        BigDecimal result = BRExchange.getAmountFromSatoshis(this, iso, new BigDecimal(MAX_AMOUNT_SATOSHIS));
        return result.intValue();
    }

    private void updateText(int progress) {
        String iso = (String) curSpiner.getSelectedItem();
        seekBar.setMax(getMax(iso));
        BigDecimal amount = new BigDecimal(progress);
        label.setText(BRCurrency.getFormattedCurrencyString(this, iso, amount));
        BigDecimal satoshis = BRExchange.getSatoshisFromAmount(this, iso, amount);
        KeyStoreManager.putSpendLimit(satoshis.longValue(), this);
    }

    @Override
    protected void onResume() {
        super.onResume();
        appVisible = true;
        app = this;
        ActivityUTILS.init(this);

    }

    @Override
    protected void onPause() {
        super.onPause();
        appVisible = false;
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.enter_from_left, R.anim.exit_to_right);
    }

}
