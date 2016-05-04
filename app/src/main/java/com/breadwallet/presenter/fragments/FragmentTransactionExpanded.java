
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.entities.TransactionListItem;
import com.breadwallet.tools.CurrencyManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.wallet.BRWalletManager;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on 6/29/15.
 * Copyright (c) 2016 breadwallet llc <mihail@breadwallet.com>
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
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
        TextView hashText = (TextView) rootView.findViewById(R.id.tx_hash_text);
        TextView statusText = (TextView) rootView.findViewById(R.id.tx_status_text);
        TextView amountText = (TextView) rootView.findViewById(R.id.tx_amount_text);
        TextView exchangeText = (TextView) rootView.findViewById(R.id.tx_exchange_text);
//        TextView fromText = (TextView) rootView.findViewById(R.id.tx_from_text);
//        TextView fromDescription = (TextView) rootView.findViewById(R.id.tx_from_description);
//        TextView toText = (TextView) rootView.findViewById(R.id.tx_to_text);
//        TextView toDescription = (TextView) rootView.findViewById(R.id.tx_to_description);
//        TextView toAmountText = (TextView) rootView.findViewById(R.id.tx_to_amount_text);
//        TextView toExchangeText = (TextView) rootView.findViewById(R.id.tx_to_exchange_text);

        LinearLayout generalTxFrom = (LinearLayout) rootView.findViewById(R.id.general_tx_from_layout);
        LinearLayout generalTxTo = (LinearLayout) rootView.findViewById(R.id.general_tx_to_layout);
        CurrencyManager m = CurrencyManager.getInstance(getActivity());
        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        final double rate = settings.getFloat(FragmentCurrency.RATE, 0);
        final String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        int blockHeight = item.getBlockHeight();
        if (!BRWalletManager.getInstance(getActivity()).transactionIsVerified(item.getHexId())) {
            statusText.setText(R.string.unverified_by_peers);
        } else if (blockHeight == Integer.MAX_VALUE) {
            statusText.setText(R.string.verified_waiting);
        } else {
            statusText.setText(String.format(Locale.getDefault(), "confirmed in block #%d\n%s", blockHeight,
                    FragmentSettingsAll.getFormattedDateFromLong(item.getTimeStamp())));
        }

        if (received) {

            long amount = item.getReceived();
            Log.e(TAG, "Tx Detail received!!!! amount: " + amount + " item.getBlockHeight(): " + item.getBlockHeight());

            hashText.setText(item.getHexId());
            amountText.setText(m.getFormattedCurrencyString("BTC", amount));
            exchangeText.setText(String.format("(%s)", m.getExchangeForAmount(rate, iso, new BigDecimal(amount))));

            String fromAddresses[] = item.getFrom();
            setReceivedFromAddresses(generalTxFrom, fromAddresses);

            String toAddresses[] = item.getTo();
            setReceivedToAddresses(generalTxTo, toAddresses, item.getOutAmounts());
        } else {
            TextView toFeeAmountText = (TextView) rootView.findViewById(R.id.tx_to_fee_amount_text);
            TextView toFeeExchangeText = (TextView) rootView.findViewById(R.id.tx_to_fee_exchange_text);

            long amount = item.getSent() - item.getReceived();

            Log.e(TAG, "Tx Detail sent!!!! amount: " + amount + " tempFee: " + item.getFee() + " tempSent: "
                    + item.getSent() + " item.getBlockHeight(): " + item.getBlockHeight());

            hashText.setText(item.getHexId());

//            statusText.setText(String.format("confirmed in block #%d\n%s", item.getBlockHeight(),
//                    FragmentSettingsAll.getFormattedDateFromLong(item.getTimeStamp())));
            amountText.setText(String.format("-%s", m.getFormattedCurrencyString("BTC", amount)));
            exchangeText.setText(String.format("(-%s)", m.getExchangeForAmount(rate, iso, new BigDecimal(amount))));
            String fromAddresses[] = item.getFrom();
            long[] outAmounts = item.getOutAmounts();
            setSentFromAddresses(generalTxFrom, fromAddresses);
            String toAddresses[] = item.getTo();
            setSentToAddresses(generalTxTo, toAddresses, outAmounts);
//            toDescription.setText(getString(R.string.payment_address));
//            toAmountText.setText(m.getFormattedCurrencyString("BTC", String.valueOf(m.getBitsFromSatoshi(amount))));
//            toExchangeText.setText(String.format("(%s)", m.getExchangeForAmount(rate, iso, String.valueOf(amount))));
            toFeeAmountText.setText(String.format("-%s", m.getFormattedCurrencyString("BTC", item.getFee())));
            toFeeExchangeText.setText(String.format("(-%s)", m.getExchangeForAmount(rate, iso, new BigDecimal(item.getFee()))));

        }

        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
    }

    @Override
    public void onPause() {
        super.onPause();
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
            TextView txFrom = (TextView) addressBlock.findViewById(R.id.tx_from_text);
            TextView txFromDescription = (TextView) addressBlock.findViewById(R.id.tx_from_description);
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
            CurrencyManager m = CurrencyManager.getInstance(getActivity());
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            final double rate = settings.getFloat(FragmentCurrency.RATE, 0);
            final String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");

//            amountText.setText(m.getFormattedCurrencyString("BTC", String.valueOf(m.getBitsFromSatoshi(amount))));
//            exchangeText.setText(String.format("(%s)", m.getExchangeForAmount(rate, iso, String.valueOf(amount))));

            TextView txTo = (TextView) addressBlock.findViewById(R.id.tx_to_text);
            TextView txToDescription = (TextView) addressBlock.findViewById(R.id.tx_to_description);
            TextView txToAmount = (TextView) addressBlock.findViewById(R.id.tx_to_amount_text);
            TextView txToExchange = (TextView) addressBlock.findViewById(R.id.tx_to_exchange_text);

            if (addresses[i] != null && !addresses[i].isEmpty()) {
                txTo.setText(addresses[i]);
                txToDescription.setText(getString(R.string.wallet_address));
                txToAmount.setText(m.getFormattedCurrencyString("BTC", amounts[i]));
                txToExchange.setText(String.format("(%s)", m.getExchangeForAmount(rate, iso, new BigDecimal(amounts[i]))));

                view.addView(addressBlock);
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
            SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
            final double rate = settings.getFloat(FragmentCurrency.RATE, 0);
            final String iso = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");

//            amountText.setText(m.getFormattedCurrencyString("BTC", String.valueOf(m.getBitsFromSatoshi(amount))));
//            exchangeText.setText(String.format("(%s)", m.getExchangeForAmount(rate, iso, String.valueOf(amount))));

            TextView txTo = (TextView) addressBlock.findViewById(R.id.tx_to_text);
            TextView txToDescription = (TextView) addressBlock.findViewById(R.id.tx_to_description);
            TextView txToAmount = (TextView) addressBlock.findViewById(R.id.tx_to_amount_text);
            TextView txToExchange = (TextView) addressBlock.findViewById(R.id.tx_to_exchange_text);

            if (addresses[i] != null && !addresses[i].isEmpty()) {
                txTo.setText(addresses[i]);
                txToDescription.setText(getString(R.string.payment_address));
                txToAmount.setText(String.format("-%s", m.getFormattedCurrencyString("BTC", amounts[i])));
                txToExchange.setText(String.format("(-%s)", m.getExchangeForAmount(rate, iso, new BigDecimal(amounts[i]))));

                view.addView(addressBlock);
                view.addView(FragmentSettingsAll.getSeparationLine(0, getActivity()));
            }
        }
    }

}
