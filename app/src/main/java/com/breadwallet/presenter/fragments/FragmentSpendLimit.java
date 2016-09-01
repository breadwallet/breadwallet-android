
package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.security.PassCodeManager;

import java.math.BigDecimal;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/22/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentSpendLimit extends Fragment {
    public static final String TAG = FragmentSpendLimit.class.getName();

    private ImageView checkMark1;
    private ImageView checkMark2;
    private ImageView checkMark3;
    private ImageView checkMark4;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_fingerprint_limit, container, false);
        RelativeLayout alwaysPasscode = (RelativeLayout) rootView.findViewById(R.id.always_passcode);
        RelativeLayout limit100k = (RelativeLayout) rootView.findViewById(R.id.limit_100k);
        RelativeLayout limit1B = (RelativeLayout) rootView.findViewById(R.id.limit_1B);
        RelativeLayout limit10B = (RelativeLayout) rootView.findViewById(R.id.limit_10B);

        TextView alwaysPasscodeText = (TextView) rootView.findViewById(R.id.always_passcode_text);
        TextView limit100kText = (TextView) rootView.findViewById(R.id.limit_100k_text);
        TextView limit1BText = (TextView) rootView.findViewById(R.id.limit_1B_text);
        TextView limit10BText = (TextView) rootView.findViewById(R.id.limit_10B_text);

        checkMark1 = (ImageView) rootView.findViewById(R.id.checkmark1);
        checkMark2 = (ImageView) rootView.findViewById(R.id.checkmark2);
        checkMark3 = (ImageView) rootView.findViewById(R.id.checkmark3);
        checkMark4 = (ImageView) rootView.findViewById(R.id.checkmark4);

        CurrencyManager cm = CurrencyManager.getInstance(getActivity());
        String iso = SharedPreferencesManager.getIso(getActivity());
        double rate = SharedPreferencesManager.getRate(getActivity());

        String alwaysPasscodeString = "always require passcode";
        String limit100kString = String.format("%s    (%s)", BRStringFormatter.getFormattedCurrencyString("BTC", BRConstants.limit1),
                BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(String.valueOf(BRConstants.limit1)), getActivity()));
        String limit1BString = String.format("%s   (%s)", BRStringFormatter.getFormattedCurrencyString("BTC", BRConstants.limit2),
                BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(String.valueOf(BRConstants.limit2)), getActivity()));
        String limit10BString = String.format("%s  (%s)", BRStringFormatter.getFormattedCurrencyString("BTC", BRConstants.limit3),
                BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(String.valueOf(BRConstants.limit3)),getActivity()));

        alwaysPasscodeText.setText(alwaysPasscodeString);
        limit100kText.setText(limit100kString);
        limit1BText.setText(limit1BString);
        limit10BText.setText(limit10BString);

        setInitialCheckMark();

        alwaysPasscode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelected(1);
            }
        });
        limit100k.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelected(2);
            }
        });
        limit1B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelected(3);
            }
        });
        limit10B.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                setSelected(4);
            }
        });

        return rootView;
    }

    private void setSelected(int mode) {
        checkMark1.setVisibility(View.GONE);
        checkMark2.setVisibility(View.GONE);
        checkMark3.setVisibility(View.GONE);
        checkMark4.setVisibility(View.GONE);
        switch (mode) {
            case 1:
                checkMark1.setVisibility(View.VISIBLE);
                PassCodeManager.getInstance().setLimit(getActivity(), 0);
                break;
            case 2:
                checkMark2.setVisibility(View.VISIBLE);
                PassCodeManager.getInstance().setLimit(getActivity(), BRConstants.limit1);
                break;
            case 3:
                checkMark3.setVisibility(View.VISIBLE);
                PassCodeManager.getInstance().setLimit(getActivity(), BRConstants.limit2);
                break;
            case 4:
                checkMark4.setVisibility(View.VISIBLE);
                PassCodeManager.getInstance().setLimit(getActivity(), BRConstants.limit3);
                break;
        }
    }

    private void setInitialCheckMark() {
        int limit = PassCodeManager.getInstance().getLimit(getActivity());
        switch (limit) {
            case 0:
                checkMark1.setVisibility(View.VISIBLE);
                break;
            case BRConstants.limit1:
                checkMark2.setVisibility(View.VISIBLE);
                break;
            case BRConstants.limit2:
                checkMark3.setVisibility(View.VISIBLE);
                break;
            case BRConstants.limit3:
                checkMark4.setVisibility(View.VISIBLE);
                break;

        }
    }

    @Override
    public void onResume() {
        super.onResume();
        Activity app = getActivity();
        if (app == null) app = MainActivity.app;
        if (app != null)
            ((BreadWalletApp) app.getApplication()).hideKeyboard(app);
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
    }
}
