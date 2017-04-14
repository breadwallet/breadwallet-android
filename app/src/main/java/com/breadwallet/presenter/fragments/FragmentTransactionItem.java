package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.TextUtils;
import android.text.format.DateFormat;
import android.text.style.RelativeSizeSpan;
import android.text.style.StyleSpan;
import android.util.Log;
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

import static com.breadwallet.R.string.to;


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
    private TextView mConfirmationText;
    private TextView mCommentText;
    private TextView mAmountText;
    private TextView mAddressText;
    private TextView mDateText;
    private TextView mToFromBottom;
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
        mToFromBottom = (TextView) rootView.findViewById(R.id.to_from);
        mConfirmationText = (TextView) rootView.findViewById(R.id.confirmation_text);

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        fillTexts();

    }

    private void fillTexts() {
//        Log.e(TAG, "fillTexts fee: " + item.getFee());
//        Log.e(TAG, "fillTexts hash: " + item.getHexId());
        //get the current iso
        String iso = SharedPreferencesManager.getPreferredBTC(getActivity()) ? "BTC" : SharedPreferencesManager.getIso(getContext());
        //get the tx amount
        BigDecimal txAmount = new BigDecimal(item.getReceived() - item.getSent()).abs();
        //see if it was sent
        boolean sent = item.getReceived() - item.getSent() < 0;

        int blockHeight = item.getBlockHeight();
        int confirms = blockHeight == Integer.MAX_VALUE ? 0 : SharedPreferencesManager.getLastBlockHeight(getContext()) - blockHeight + 1;

        //calculated and formatted amount for iso
        String amountWithFee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, txAmount));
        String amount = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, item.getFee() == -1 ? txAmount : txAmount.subtract(new BigDecimal(item.getFee()))));
        //calculated and formatted fee for iso
        String fee = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getFee())));
        //description (Sent $24.32 ....)
        Spannable descriptionString = sent ? new SpannableString("Sent " + amountWithFee) : new SpannableString("Received " + amountWithFee);

        String startingBalance = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(sent? item.getBalanceAfterTx() +  txAmount.longValue() : item.getBalanceAfterTx() - txAmount.longValue())));
        String endingBalance = BRCurrency.getFormattedCurrencyString(getActivity(), iso, BRExchange.getAmountFromSatoshis(getActivity(), iso, new BigDecimal(item.getBalanceAfterTx())));
        String commentString = "For Love";
        String amountString = String.format("%s %s\n\nStarting Balance: %s\nEnding Balance:  %s\n\nExchange Rate on Day-Of-Transaction\n%s", amount, item.getFee() == -1 ? "" : String.format("(%s)", fee), startingBalance, endingBalance, "none for now");


        SpannableString addr = sent ? new SpannableString(item.getTo()[0]) : new SpannableString(item.getFrom()[0]);
        SpannableString toFrom = sent ? new SpannableString("to") : new SpannableString("from");
        toFrom.setSpan(new RelativeSizeSpan(0.80f), 0, toFrom.length(), 0);
        //span a piece of text to be smaller size (the address)
        final StyleSpan norm = new StyleSpan(Typeface.NORMAL);
        addr.setSpan(norm, 0, addr.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);
        addr.setSpan(new RelativeSizeSpan(0.65f), 0, addr.length(), 0);


        if (!item.isValid())
            mConfirmationText.setText("INVALID");
        else if (confirms < 6) {
            if (blockHeight == Integer.MAX_VALUE)
                mConfirmationText.setText("Waiting to be confirmed. Some merchants require confirmation to complete a transaction.  Estimated time: 1-2 hours.");
            else if (confirms == 0)
                mConfirmationText.setText(getActivity().getString(R.string.nr_confirmations0));
            else if (confirms == 1)
                mConfirmationText.setText(getActivity().getString(R.string.nr_confirmations1));
            else
                mConfirmationText.setText(String.format(getActivity().getString(R.string.nr_confirmations), confirms));
        } else {
            mConfirmationText.setText("Completed");
        }


        mToFromBottom.setText(sent ? "from" : "to");
        mDateText.setText(getFormattedDate(item.getTimeStamp()));
        mDescriptionText.setText(TextUtils.concat(descriptionString, "\n", toFrom, ": ", addr));
        mCommentText.setText(commentString);
        mAmountText.setText(amountString);
        mAddressText.setText(sent ? item.getFrom()[0] : item.getTo()[0]);
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

        Date currentLocalTime = new Date(timeStamp == 0 ? System.currentTimeMillis() : timeStamp * 1000);

        SimpleDateFormat date1 = new SimpleDateFormat("MMMM dd, yyyy", Locale.getDefault());
        SimpleDateFormat date2 = new SimpleDateFormat("HH:mm a", Locale.getDefault());

        String str1 = date1.format(currentLocalTime);
        String str2 = date2.format(currentLocalTime);

        return str1 + " at " + str2;
    }


}