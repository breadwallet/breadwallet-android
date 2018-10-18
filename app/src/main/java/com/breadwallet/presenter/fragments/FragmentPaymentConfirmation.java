package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
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
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.TokenUtil;
import com.breadwallet.tools.util.Utils;
import com.squareup.picasso.Picasso;

import java.io.File;
import java.math.BigDecimal;

public class FragmentPaymentConfirmation extends Fragment {

    private static final String TAG = FragmentPaymentConfirmation.class.getSimpleName();

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
            final String amount = metaData.getAmount();

            // Load the token icon using its symbol
            final String tokenIconPath = TokenUtil.getTokenIconPath(getContext(), metaData.getTokenSymbol(), true);

            if (!Utils.isNullOrEmpty(tokenIconPath)) {
                File iconFile = new File(tokenIconPath);
                Picasso.get().load(iconFile).into(icoIcon);
            }

            // Display the token purchase amount
            if (!Utils.isNullOrEmpty(amount)) {
                String formattedPrice = CurrencyUtils.getFormattedAmount(getActivity(), metaData.getCurrencyCode(), new BigDecimal(amount));
                icoPriceInformation.setText(String.format(getString(R.string.PaymentConfirmation_amountText), formattedPrice, metaData.getTokenSymbol().toUpperCase()));
            }

            // Display the token name below the icon
            if (!Utils.isNullOrEmpty(metaData.getTokenName())) {
                icoName.setText(metaData.getTokenName());
            }

            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handlePaymentConfirmation(metaData, true);
                }
            });

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

        MessageExchangeService.enqueueWork(BreadApp.getBreadContext(), MessageExchangeService.createIntent(BreadApp.getBreadContext(), metaData, approved));

    }

    public static FragmentPaymentConfirmation newInstance(RequestMetaData requestMetaData) {
        Bundle args = new Bundle();
        args.putParcelable(MessageExchangeService.EXTRA_METADATA, requestMetaData);
        FragmentPaymentConfirmation fragment = new FragmentPaymentConfirmation();
        fragment.setArguments(args);
        return fragment;
    }
}
