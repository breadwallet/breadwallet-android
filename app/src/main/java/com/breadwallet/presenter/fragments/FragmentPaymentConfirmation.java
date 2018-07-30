package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.protocols.messageexchange.MessageExchangeService;
import com.breadwallet.protocols.messageexchange.entities.RequestMetaData;
import com.breadwallet.tools.util.Utils;

public class FragmentPaymentConfirmation extends Fragment {


    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_payment_confirmation, container, false);


        Bundle bundle = getArguments();
        BaseTextView priceTextView = rootView.findViewById(R.id.price);
        BaseTextView paymentMethod = rootView.findViewById(R.id.payment_method);

        if (bundle != null) {
            RequestMetaData metaData = bundle.getParcelable(MessageExchangeService.EXTRA_METADATA);
            Log.d("FragmentPaymentConfir", "Price -> " + metaData.getAmount());
            Log.d("FragmentPaymentConfir", "Currency -> " + metaData.getCurrencyCode());

            String price = metaData.getAmount();
            if(!Utils.isNullOrEmpty(price)){
                priceTextView.setText(price);
            }


            String method = metaData.getCurrencyCode();
            if(!Utils.isNullOrEmpty(method)){
                paymentMethod.setText(method);
            }

        }
        return rootView;
    }


    public static FragmentPaymentConfirmation newInstance(RequestMetaData requestMetaData) {
        Bundle args = new Bundle();
        args.putParcelable(MessageExchangeService.EXTRA_METADATA, requestMetaData);
        FragmentPaymentConfirmation fragment = new FragmentPaymentConfirmation();
        fragment.setArguments(args);
        return fragment;
    }
}
