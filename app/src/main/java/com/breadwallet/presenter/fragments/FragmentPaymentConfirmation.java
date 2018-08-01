package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.protocols.messageexchange.MessageExchangeService;
import com.breadwallet.protocols.messageexchange.entities.RequestMetaData;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

import java.math.BigDecimal;

public class FragmentPaymentConfirmation extends Fragment {

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

        if (bundle != null) {
            RequestMetaData metaData = bundle.getParcelable(MessageExchangeService.EXTRA_METADATA);
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
                icoPriceInformation.setText(String.format("Send %s to purchase CCC", formattedPrice));
            }

            icoName.setText(ICO_NAME);

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
