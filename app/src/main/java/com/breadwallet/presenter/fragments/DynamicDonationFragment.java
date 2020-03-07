package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Pair;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.SeekBar;
import android.widget.Spinner;
import android.widget.TextView;

import androidx.annotation.Nullable;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRSender;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;

/**
 * Litewallet
 * Created by Mohamed Barry on 3/2/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
public class DynamicDonationFragment extends Fragment {

    static final long BALANCE_STEP = 1_000_000;

    private long currentBalance;

    private TextView addressVal;
    private TextView amountVal;
    private TextView feeVal;
    private TextView totalVal;

    private TextView amountSliderVal;

    private SeekBar seekBar;
    private String selectedIso;
    private boolean isLTCSwap = true;
    private Pair<String, String> chosenAddress;
    private long mDonationAmount;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_dynamic_donation, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        selectedIso = BRSharedPrefs.getIso(getContext());
        isLTCSwap = BRSharedPrefs.getPreferredLTC(getContext());

        addressVal = view.findViewById(R.id.addressVal);

        chosenAddress = BRConstants.DONATION_ADDRESSES[0];
        addressVal.setText(chosenAddress.second);

        Spinner spinner = view.findViewById(R.id.spinnerAddresses);
        spinner.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                chosenAddress = BRConstants.DONATION_ADDRESSES[position];
                addressVal.setText(chosenAddress.second);
            }

            @Override
            public void onNothingSelected(AdapterView<?> parent) {
                //NO-OP
            }
        });
        ArrayAdapter adapter = new ArrayAdapter(getContext(), android.R.layout.simple_spinner_item, addresses());
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);
        spinner.setAdapter(adapter);

        TextView processingTimeLbl = view.findViewById(R.id.processingTimeLbl);
        processingTimeLbl.setText(getString(R.string.Confirmation_processingAndDonationTime, "2.5-5"));

        amountVal = view.findViewById(R.id.amountVal);
        feeVal = view.findViewById(R.id.feeVal);
        totalVal = view.findViewById(R.id.totalVal);

        Button cancelBut = view.findViewById(R.id.cancelBut);
        cancelBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                getActivity().onBackPressed();
            }
        });

        Button donateBut = view.findViewById(R.id.donateBut);
        donateBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String memo = getString(R.string.Donate_toThe) + chosenAddress.first;
                PaymentItem request = new PaymentItem(new String[]{chosenAddress.second}, null, mDonationAmount, null, false, memo);
                BRSender.getInstance().sendTransaction(getContext(), request);
            }
        });

        amountSliderVal = view.findViewById(R.id.amountSliderVal);

        seekBar = view.findViewById(R.id.seekBar);

        ImageButton upAmountBut = view.findViewById(R.id.upAmountBut);
        ImageButton downAmountBut = view.findViewById(R.id.downAmountBut);

        upAmountBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekBar.incrementProgressBy(diff());

                long newAmount = newAmount(seekBar.getProgress());
                updateDonationValues(newAmount);
            }
        });

        downAmountBut.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                seekBar.incrementProgressBy(-diff());

                long newAmount = newAmount(seekBar.getProgress());
                updateDonationValues(newAmount);
            }
        });

        seekBar.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            @Override
            public void onProgressChanged(SeekBar seekBar, int progress, boolean fromUser) {
                updateDonationValues(newAmount(progress));
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
                //NO-OP
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) {
                //NO-OP
            }
        });

        currentBalance = BRSharedPrefs.getCatchedBalance(getContext());

        updateDonationValues(BRConstants.DONATION_AMOUNT);
    }

    private int diff() {
        float step = (currentBalance - BRConstants.DONATION_AMOUNT) * 1f / BALANCE_STEP;
        int diff = (int) (seekBar.getMax() * 1f / step);
        return Math.max(diff, 1);
    }

    private long newAmount(int progress) {
        long maxFee = BRWalletManager.getInstance().feeForTransactionAmount(currentBalance);
        long adjustedAmount = (long) ((progress * 1f / seekBar.getMax()) * (currentBalance - BRConstants.DONATION_AMOUNT - maxFee));
        return adjustedAmount + BRConstants.DONATION_AMOUNT;
    }

    private String[] addresses() {
        String[] addresses = new String[BRConstants.DONATION_ADDRESSES.length];
        for (int i = 0; i < BRConstants.DONATION_ADDRESSES.length; i++) {
            addresses[i] = getString(R.string.Donate_toThe) + BRConstants.DONATION_ADDRESSES[i].first;
        }
        return addresses;
    }

    private void updateDonationValues(long donationAmount) {
        mDonationAmount = donationAmount;
        final BigDecimal donation = new BigDecimal(donationAmount);

        long feeAmount = BRWalletManager.getInstance().feeForTransactionAmount(donationAmount);
        final BigDecimal fee = new BigDecimal(feeAmount);

        final BigDecimal total = new BigDecimal(donationAmount + feeAmount);

        amountVal.setText(formatResultAmount(formatLtcAmount(donation), formatIsoAmount(donation)));
        feeVal.setText(formatResultAmount(formatLtcAmount(fee), formatIsoAmount(fee)));
        totalVal.setText(formatResultAmount(formatLtcAmount(total), formatIsoAmount(total)));

        amountSliderVal.setText(totalVal.getText());
    }

    private String formatLtcAmount(BigDecimal amount) {
        BigDecimal ltcAmount = BRExchange.getBitcoinForSatoshis(getContext(), amount);
        return BRCurrency.getFormattedCurrencyString(getContext(), "LTC", ltcAmount);
    }

    private String formatIsoAmount(BigDecimal amount) {
        BigDecimal fiatAmount = BRExchange.getAmountFromSatoshis(getContext(), selectedIso, amount);
        return BRCurrency.getFormattedCurrencyString(getContext(), selectedIso, fiatAmount);
    }

    private String formatResultAmount(String ltcAmount, String isoAmount) {
        String format = "%s (%s)";
        if (isLTCSwap) {
            return String.format(format, ltcAmount, isoAmount);
        } else {
            return String.format(format, isoAmount, ltcAmount);
        }
    }
}
