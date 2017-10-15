package com.breadwallet.presenter.activities.settings;

import android.os.Bundle;
import android.view.View;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.util.ActivityUTILS;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;

import static com.breadwallet.tools.util.BRConstants.ONE_BITCOIN;


public class SpendLimitActivity extends BRActivity {
    private static final String TAG = SpendLimitActivity.class.getName();
    //    private Button scanButton;
    public static boolean appVisible = false;
    private static SpendLimitActivity app;
    private SeekBar seekBar;
    private TextView label;

    //    private Spinner curSpiner;
    @Override
    protected void onSaveInstanceState(Bundle outState) {
    }

    public static SpendLimitActivity getApp() {
        return app;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_spend_limit);

        label = (TextView) findViewById(R.id.limit_label);
        seekBar = (SeekBar) findViewById(R.id.seekBar);

        ImageButton faq = (ImageButton) findViewById(R.id.faq_button);

        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                BRAnimator.showSupportFragment(app, BRConstants.fingerprintSpendingLimit);
            }
        });
        int progress = getStepFromLimit(BRKeyStore.getSpendLimit(this));
        updateText(progress);
        seekBar.setProgress(progress);
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
        String iso = BRSharedPrefs.getIso(this);
        //amount in satoshis
        BigDecimal satoshis = getAmountBySte(progress);
        //amount in BTC, mBTC or bits
        BigDecimal amount = BRExchange.getAmountFromSatoshis(this, "LTC", satoshis);
        //amount in user preferred ISO (e.g. USD)
        BigDecimal curAmount = BRExchange.getAmountFromSatoshis(this, iso, satoshis);
        //formatted string for the label
        String string = String.format("%s (%s)", BRCurrency.getFormattedCurrencyString(this, "LTC", amount), BRCurrency.getFormattedCurrencyString(this, iso, curAmount));
        label.setText(string);
        BRKeyStore.putSpendLimit(satoshis.longValue(), this);
        updateTotalLimit();
    }

    private void updateTotalLimit() {
        AuthManager.getInstance().setTotalLimit(this, BRWalletManager.getInstance().getTotalSent()
                + BRKeyStore.getSpendLimit(this));
    }

    //satoshis
    private BigDecimal getAmountBySte(int step) {
        BigDecimal result;
        switch (step) {
            case 0:
                result = new BigDecimal(ONE_BITCOIN / 100);//   0.01 BTC
                break;
            case 1:
                result = new BigDecimal(ONE_BITCOIN / 10);//   0.1 BTC
                break;
            case 2:
                result = new BigDecimal(ONE_BITCOIN);//   1 BTC
                break;
            case 3:
                result = new BigDecimal(ONE_BITCOIN * 10);//   10 BTC
                break;

            default:
                result = new BigDecimal(ONE_BITCOIN);//   1 BTC Default
                break;
        }
        return result;
    }

    private int getStepFromLimit(long limit) {
        switch ((int) limit) {
            case ONE_BITCOIN / 100:
                return 0;
            case ONE_BITCOIN / 10:
                return 1;
            case ONE_BITCOIN:
                return 2;
            case ONE_BITCOIN * 10:
                return 3;
            default:
                return 2; //1 BTC Default
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
