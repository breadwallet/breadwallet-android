package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.format.DateFormat;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.BRCurrency;
import com.breadwallet.tools.util.BRExchange;

import java.math.BigDecimal;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;


/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
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

public class FragmentTransactionItem extends Fragment {
    private static final String TAG = FragmentTransactionItem.class.getName();

    public TextView mTitle;
    private TextView mDescriptionText;
    private TextView mCommentText;
    private TextView mAmountText;
    private TextView mAddressText;
    private TextView mDateText;
    private TransactionListItem item;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.transaction_details_item, container, false);
        mTitle = (TextView) rootView.findViewById(R.id.title);
        mDescriptionText = (TextView) rootView.findViewById(R.id.description_text);
        mCommentText = (TextView) rootView.findViewById(R.id.comment_text);
        mAmountText = (TextView) rootView.findViewById(R.id.amount_text);
        mAddressText = (TextView) rootView.findViewById(R.id.address_text);
        mDateText = (TextView) rootView.findViewById(R.id.date_text);
        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fillTexts();

    }

    private void fillTexts() {
        String iso = SharedPreferencesManager.getPreferredBTC(getActivity()) ? "BTC" : SharedPreferencesManager.getIso(getContext());
        BigDecimal txAmount = new BigDecimal(item.getReceived() - item.getSent());

        String amount = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, txAmount));
        String fee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getFee())));
        Spannable descriptionString = item.getSent() - item.getReceived() < 0 ? new SpannableString("Sent") : new SpannableString("Received");
        String startingBalance = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getBalanceAfterTx() - txAmount.longValue())));
        String endingBalance = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getBalanceAfterTx())));
//        if(mBodyText.getSelectionEnd() > mBodyText.getSelectionStart())
//            str.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC),
//                    mBodyText.getSelectionStart(), mBodyText.getSelectionEnd(),
//                    Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
//        else
//            str.setSpan(new StyleSpan(android.graphics.Typeface.ITALIC),
//                    mBodyText.getSelectionEnd(),
//                    mBodyText.getSelectionStart(), Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);
        String commentString = "For Love";
//        BigDecimal
        String amountString = String.format("%s (%s fee)\n\nStarting Balance: %s\nEnding Balance: %s\n\nExchange Rate on Day-Of-Transaction\n%s", amount, fee, startingBalance, endingBalance, "none for now");

        mDateText.setText(getFormattedDate(item.getTimeStamp()));
        mDescriptionText.setText(descriptionString);
        mCommentText.setText(commentString);
        mAmountText.setText(amountString);
        mAddressText.setText(item.getTo()[0]);
    }

    @Override
    public void onStop() {
        super.onStop();
    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    public static FragmentTransactionItem newInstance(TransactionListItem item) {

        FragmentTransactionItem f = new FragmentTransactionItem();
        f.setItem(item);

        return f;
    }

    public void setItem(TransactionListItem item) {
        this.item = item;

    }

    private String getFormattedDate(long timeStamp) {
        Calendar cal = Calendar.getInstance();
        Date currentLocalTime = cal.getTime();

        SimpleDateFormat date = new SimpleDateFormat("dd-MM-yyy HH:mm:ss z", Locale.getDefault());

        return date.format(currentLocalTime);
    }


}