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
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.R.id.step;
import static com.breadwallet.tools.util.BRExchange.getAmountFromSatoshis;


public class SpendLimitActivity extends AppCompatActivity {
    private static final String TAG = SpendLimitActivity.class.getName();
    //    private Button scanButton;
    public static boolean appVisible = false;
    private static SpendLimitActivity app;
    private SeekBar seekBar;
    private TextView label;
    //    private Spinner curSpiner;
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
//        curSpiner = (Spinner) findViewById(R.id.cur_spinner);

//        final List<String> curList = new ArrayList<>();
//        curList.add("BTC");
//        curList.addAll(CurrencyDataSource.getInstance(this).getAllISOs());

//        final ArrayAdapter<String> adapter = new ArrayAdapter<>(this, R.layout.bread_spinner_item, curList);
//        curSpiner.setAdapter(adapter);
//        curSpiner.setAdapter(adapter);
//        curSpiner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
//            @Override
//            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
//                updateText(0);
//            }
//
//            @Override
//            public void onNothingSelected(AdapterView<?> parent) {
//                updateText(0);
//            }
//        });
        updateText(getStepFromLimit(KeyStoreManager.getSpendLimit(this)));
        seekBar.setMax(3);
        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {

            @Override
            public void onProgressChanged(SeekBar seekBar, int progressValue, boolean fromUser) {
                updateText(progressValue);
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
            }
        });


    }

    private void updateText(int progress) {
        //user preferred ISO
        String iso = SharedPreferencesManager.getIso(this);
        //amount in satoshis
        BigDecimal satoshis = getAmountBySte(progress);
        //amount in BTC, mBTC or bits
        BigDecimal amount = BRExchange.getAmountFromSatoshis(this, "BTC", satoshis);
        //amount in user preferred ISO (e.g. USD)
        BigDecimal curAmount = BRExchange.getAmountFromSatoshis(this, iso, satoshis);
        //formatted string for the label
        String string = String.format("%s (%s)", BRCurrency.getFormattedCurrencyString(this, "BTC", amount), BRCurrency.getFormattedCurrencyString(this, iso, curAmount));
        label.setText(string);
        KeyStoreManager.putSpendLimit(satoshis.longValue(), this);
    }

    //satoshis
    private BigDecimal getAmountBySte(int step) {
        BigDecimal result;
        switch (step) {
            case 0:
                result = new BigDecimal(1000000);//   0.01 BTC
                break;
            case 1:
                result = new BigDecimal(10000000);//   0.1 BTC
                break;
            case 2:
                result = new BigDecimal(100000000);//   1 BTC
                break;
            case 3:
                result = new BigDecimal(1000000000);//   10 BTC
                break;

            default:
                result = new BigDecimal(100000000);//   1 BTC Default
                break;
        }
        return result;
    }

    private int getStepFromLimit(long limit) {
        switch ((int) limit) {
            case 1000000://   0.01 BTC
                return 0;
            case 10000000://   0.1 BTC
                return 1;
            case 100000000://   1 BTC
                return 2;
            case 1000000000://   10 BTC
                return 3;
            default:
                return 2;//   1 BTC Default
        }
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
