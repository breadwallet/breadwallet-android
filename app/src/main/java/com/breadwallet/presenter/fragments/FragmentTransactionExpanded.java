
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRStringFormatter;
import com.breadwallet.tools.manager.CurrencyManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
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

public class FragmentTransactionExpanded extends Fragment {
    private static final String TAG = FragmentTransactionExpanded.class.getName();
    private TransactionListItem item;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        if (item == null) return null;
        boolean received = item.getSent() == 0;
        View rootView;
        if (received) {
            rootView = inflater.inflate(R.layout.transaction_item_expanded_received, container, false);
        } else {
            rootView = inflater.inflate(R.layout.transaction_item_expanded_sent, container, false);
        }
        final TextView hashText = (TextView) rootView.findViewById(R.id.tx_hash_text);
        TextView statusText = (TextView) rootView.findViewById(R.id.tx_status_text);
        TextView amountText = (TextView) rootView.findViewById(R.id.tx_amount_text);
        TextView exchangeText = (TextView) rootView.findViewById(R.id.tx_exchange_text);
        hashText.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "tx id: " + hashText.getText().toString());
            }
        });

        LinearLayout generalTxFrom = (LinearLayout) rootView.findViewById(R.id.general_tx_from_layout);
        LinearLayout generalTxTo = (LinearLayout) rootView.findViewById(R.id.general_tx_to_layout);
        CurrencyManager m = CurrencyManager.getInstance(getActivity());
        final double rate = SharedPreferencesManager.getRate(getActivity());
        final String iso = SharedPreferencesManager.getIso(getActivity());
        int blockHeight = item.getBlockHeight();
        if (!BRWalletManager.getInstance(getActivity()).transactionIsVerified(item.getHexId())) {
            statusText.setText(R.string.unverified);
        } else if (blockHeight == Integer.MAX_VALUE) {
            statusText.setText(R.string.verified_waiting);
        } else {
            String statusString = String.format(Locale.getDefault(), getActivity().getString(R.string.confirmed_in_block_nr),
                    blockHeight) + "\n" + String.valueOf(item.getTimeStamp() != 0 ?
                    Utils.getFormattedDateFromLong(item.getTimeStamp() * 1000) : Utils.getFormattedDateFromLong(System.currentTimeMillis()));
            statusText.setText(statusString);
        }
        String rawHash = BRWalletManager.getInstance(getActivity()).reverseTxHash(item.getHexId());
        final int mid = rawHash.length() / 2;
        String hashString = rawHash.substring(0, mid) + "\n" + rawHash.substring(mid);

        hashText.setText(hashString);
        BRAnimator.showCopyBubble(getActivity(), rootView.findViewById(R.id.tx_id), hashText);

        if (received) {

            long amount = item.getReceived();
//            Log.e(TAG, "Tx Detail received!!!! amount: " + amount + " item.getBlockHeight(): " + item.getBlockHeight());

            amountText.setText(BRStringFormatter.getFormattedCurrencyString("BTC", amount));
            exchangeText.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(amount), getActivity())));

            String fromAddresses[] = item.getFrom();
            setReceivedFromAddresses(generalTxFrom, fromAddresses);

            String toAddresses[] = item.getTo();
            setReceivedToAddresses(generalTxTo, toAddresses, item.getOutAmounts());
        } else {
            TextView toFeeAmountText = (TextView) rootView.findViewById(R.id.tx_to_fee_amount_text);
            TextView toFeeExchangeText = (TextView) rootView.findViewById(R.id.tx_to_fee_exchange_text);

            long amount = item.getSent() - item.getReceived();

//            Log.e(TAG, "Tx Detail sent!!!! amount: " + amount + " tempFee: " + item.getFee() + " tempSent: "
//                    + item.getSent() + " item.getBlockHeight(): " + item.getBlockHeight());

            amountText.setText(String.format("%s", BRStringFormatter.getFormattedCurrencyString("BTC", -amount)));
            exchangeText.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(-amount), getActivity())));
            String fromAddresses[] = item.getFrom();
            long[] outAmounts = item.getOutAmounts();
            setSentFromAddresses(generalTxFrom, fromAddresses);
            String toAddresses[] = item.getTo();
            setSentToAddresses(generalTxTo, toAddresses, outAmounts);
            toFeeAmountText.setText(String.format("%s", BRStringFormatter.getFormattedCurrencyString("BTC", -item.getFee())));
            toFeeExchangeText.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(-item.getFee()), getActivity())));

        }
        setHideCopyBubbleListeners(rootView.findViewById(R.id.tx_amount),
                rootView.findViewById(R.id.tx_fee_layout), rootView.findViewById(R.id.tx_status),
                rootView.findViewById(R.id.transactions_item));


        return rootView;
    }

    private void setHideCopyBubbleListeners(View... views) {
        for (View v : views) {
            if (v != null) v.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    BRAnimator.hideCopyBubble(getActivity());
                }
            });
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
    }

    @Override
    public void onPause() {
        super.onPause();
        BRAnimator.hideCopyBubble(getActivity());
    }


    public void setCurrentObject(TransactionListItem item) {
        this.item = item;
    }

    private void setReceivedFromAddresses(LinearLayout view, String[] addresses) {
        view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        for (String address : addresses) {
            LinearLayout addressBlock = (LinearLayout) inflater.inflate(R.layout.
                    transaction_received_from_addresses, null);
            TextView txFrom = (TextView) addressBlock.findViewById(R.id.tx_from_text);
            TextView txFromDescription = (TextView) addressBlock.findViewById(R.id.tx_from_description);
            BRAnimator.showCopyBubble(getActivity(), addressBlock, txFrom);
            if (address != null && !address.isEmpty()) {
                txFrom.setText(address);
                txFromDescription.setText(getString(R.string.spent_address));
                view.addView(addressBlock);
                view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
            }
        }
    }

    private void setSentFromAddresses(LinearLayout view, String[] addresses) {
        view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        for (String address : addresses) {
            RelativeLayout addressBlock = (RelativeLayout) inflater.inflate(R.layout.
                    transaction_sent_from_addresses, null);
            final TextView txFrom = (TextView) addressBlock.findViewById(R.id.tx_from_text);
            TextView txFromDescription = (TextView) addressBlock.findViewById(R.id.tx_from_description);
            BRAnimator.showCopyBubble(getActivity(), addressBlock, txFrom);
            if (address != null && !address.isEmpty()) {
                txFrom.setText(address);
                txFromDescription.setText(getString(R.string.wallet_address));
                view.addView(addressBlock);
                view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
            }
        }
    }

    private void setReceivedToAddresses(LinearLayout view, String[] addresses, long[] amounts) {
        view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        for (int i = 0; i < addresses.length; i++) {
            RelativeLayout addressBlock = (RelativeLayout) inflater.inflate(R.layout.
                    transaction_received_to_addresses, null);
            final double rate = SharedPreferencesManager.getRate(getActivity());
            final String iso = SharedPreferencesManager.getIso(getActivity());

            final TextView txTo = (TextView) addressBlock.findViewById(R.id.tx_to_text);
            TextView txToDescription = (TextView) addressBlock.findViewById(R.id.tx_to_description);
            TextView txToAmount = (TextView) addressBlock.findViewById(R.id.tx_to_amount_text);
            TextView txToExchange = (TextView) addressBlock.findViewById(R.id.tx_to_exchange_text);
            BRAnimator.showCopyBubble(getActivity(), addressBlock, txTo);

            if (addresses[i] != null && !addresses[i].isEmpty()) {
                txTo.setText(addresses[i]);
                txToDescription.setText(getString(R.string.wallet_address));
                txToAmount.setText(BRStringFormatter.getFormattedCurrencyString("BTC", amounts[i]));
                txToExchange.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(amounts[i]), getActivity())));
                view.addView(addressBlock);
//                if (i != addresses.length - 1)
                view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
            }
        }
    }

    private void setSentToAddresses(LinearLayout view, String[] addresses, long[] amounts) {
        view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
        LayoutInflater inflater = getActivity().getLayoutInflater();
        for (int i = 0; i < addresses.length; i++) {
            RelativeLayout addressBlock = (RelativeLayout) inflater.inflate(R.layout.
                    transaction_sent_to_addresses, null);
            CurrencyManager m = CurrencyManager.getInstance(getActivity());
            final double rate = SharedPreferencesManager.getRate(getActivity());
            final String iso = SharedPreferencesManager.getIso(getActivity());

            TextView txTo = (TextView) addressBlock.findViewById(R.id.tx_to_text);
            TextView txToDescription = (TextView) addressBlock.findViewById(R.id.tx_to_description);
            TextView txToAmount = (TextView) addressBlock.findViewById(R.id.tx_to_amount_text);
            TextView txToExchange = (TextView) addressBlock.findViewById(R.id.tx_to_exchange_text);
            BRAnimator.showCopyBubble(getActivity(), addressBlock, txTo);

            if (addresses[i] != null && !addresses[i].isEmpty()) {
                txTo.setText(addresses[i]);
                txToDescription.setText(getString(R.string.payment_address));
                txToAmount.setText(String.format("%s", BRStringFormatter.getFormattedCurrencyString("BTC", -amounts[i])));
                txToExchange.setText(String.format("(%s)", BRStringFormatter.getExchangeForAmount(rate, iso, new BigDecimal(-amounts[i]), getActivity())));
                view.addView(addressBlock);
                if (i != addresses.length - 1)
                    view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
            }
        }
    }
}
