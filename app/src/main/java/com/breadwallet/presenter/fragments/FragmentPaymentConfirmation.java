package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.Nullable;
import android.text.format.DateUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.breadwallet.BreadApp;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.HomeActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.protocols.messageexchange.MessageExchangeService;
import com.breadwallet.protocols.messageexchange.entities.RequestMetaData;
import com.breadwallet.tools.threads.executor.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;

public class FragmentPaymentConfirmation extends Fragment {

    private static final String TAG = FragmentPaymentConfirmation.class.getSimpleName();
    private static final String ICON_RESOURCE = "ccc";
    private static final String ICO_NAME = "Container Crypto Coin";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_payment_confirmation, container, false);

        Bundle bundle = getArguments();
        ImageView icoIcon = rootView.findViewById(R.id.ico_icon);
        BaseTextView icoName = rootView.findViewById(R.id.ico_name);
        BaseTextView icoPriceInformation = rootView.findViewById(R.id.ico_price_information);
        BRButton positiveButton = rootView.findViewById(R.id.positive_button);
        BRButton negativeButton = rootView.findViewById(R.id.negative_button);

        if (bundle != null) {
            final RequestMetaData metaData = bundle.getParcelable(MessageExchangeService.EXTRA_METADATA);
            Log.d("FragmentPaymentConfir", "Price -> " + metaData.getAmount());
            Log.d("FragmentPaymentConfir", "Currency -> " + metaData.getCurrencyCode());

            String currencyCode = metaData.getCurrencyCode();
            String amount = metaData.getAmount();
            int iconResourceId = getActivity().getResources().getIdentifier(ICON_RESOURCE, BRConstants.DRAWABLE, getActivity().getPackageName());
            if (iconResourceId > 0) {
                icoIcon.setBackground(getActivity().getDrawable(iconResourceId));
            }

            if (!Utils.isNullOrEmpty(amount)) {
                String formattedPrice = CurrencyUtils.getFormattedAmount(getActivity(), currencyCode, new BigDecimal(amount));
                icoPriceInformation.setText(String.format(getString(R.string.PaymentConfirmation_amountText), formattedPrice, currencyCode));
            }

            icoName.setText(ICO_NAME);

            positiveButton.setText(getResources().getString(R.string.Button_buy));
            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handlePaymentConfirmation(metaData, true);
                }
            });

            negativeButton.setText(getResources().getString(R.string.Button_cancel));
            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handlePaymentConfirmation(metaData, false);
                }
            });

        }
        return rootView;
    }

    private void handlePaymentConfirmation(final RequestMetaData metaData, final boolean approved) {
        Log.d(TAG, "handlePaymentApproved()");

        // Check if ConfirmationActivity is at the root of the current task. If so, take the user home
        // when they press "Cancel" or back button
        if (getActivity().isTaskRoot()) {
            Log.d(TAG, "Parent was task root, going Home");
            Intent intent = new Intent(getActivity(), HomeActivity.class);
            startActivity(intent);
        } else {
            getActivity().onBackPressed();
        }

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {
                MessageExchangeService.enqueueWork(BreadApp.getBreadContext(), MessageExchangeService.createIntent(BreadApp.getBreadContext(), metaData, approved));

            }
        }, DateUtils.SECOND_IN_MILLIS);

    }

    public static FragmentPaymentConfirmation newInstance(RequestMetaData requestMetaData) {
        Bundle args = new Bundle();
        args.putParcelable(MessageExchangeService.EXTRA_METADATA, requestMetaData);
        FragmentPaymentConfirmation fragment = new FragmentPaymentConfirmation();
        fragment.setArguments(args);
        return fragment;
    }
}
