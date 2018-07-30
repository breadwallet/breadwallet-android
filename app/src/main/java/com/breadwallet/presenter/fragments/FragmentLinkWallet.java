package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.protocols.messageexchange.MessageExchangeService;
import com.breadwallet.protocols.messageexchange.entities.LinkMetaData;
import com.breadwallet.protocols.messageexchange.entities.ServiceMetaData;

import java.util.List;


public class FragmentLinkWallet extends Fragment {

    private static final String TAG = FragmentLinkWallet.class.getSimpleName();

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(R.layout.fragment_link_wallet, container, false);

        Bundle bundle = getArguments();
        RelativeLayout validDomainsLayout = rootView.findViewById(R.id.valid_domains_layout);
        RelativeLayout appPermissionsLayout = rootView.findViewById(R.id.permission_layout);

        if (bundle != null) {
            Log.d(TAG, "Found Bundle!");
            LinkMetaData linkMetaData = bundle.getParcelable(MessageExchangeService.EXTRA_METADATA);

            // Get the valid domains list and add them to the UI
            ServiceMetaData serviceMetaData = linkMetaData.getServiceMetaData();
            List<String> validDomains = serviceMetaData.getDomains();
            StringBuilder domainStringBuilder = new StringBuilder();

            boolean needsComma = validDomains.size() > 1 ? true : false;

            for (String domain : validDomains) {
                domainStringBuilder.append(domain);
                if (needsComma) {
                    domainStringBuilder.append(",");
                }
            }

            TextView domains = new TextView(getContext());
            domains.setText(domainStringBuilder.toString());
            domains.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "fonts/CircularPro-Book.otf"));

            RelativeLayout.LayoutParams domainParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            domainParams.addRule(RelativeLayout.CENTER_HORIZONTAL, domains.getId());
            validDomainsLayout.addView(domains);
            domains.setLayoutParams(domainParams);

            // Get the app permissions("capabilities") list and add them to the UI
            List<ServiceMetaData.Capability> capabilities = serviceMetaData.getCapabilities();
            StringBuilder capabilityStringBuilder = new StringBuilder();

            for (ServiceMetaData.Capability capability : capabilities) {
                capabilityStringBuilder.append(capability.getDescription() + "\n");
            }

            RelativeLayout.LayoutParams capabilitiesParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView capabilitiesTextView = new TextView(getContext());
            capabilitiesTextView.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
            capabilitiesParams.addRule(RelativeLayout.CENTER_HORIZONTAL, capabilitiesTextView.getId());
            capabilitiesTextView.setText(capabilityStringBuilder.toString());
            capabilitiesTextView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), "fonts/CircularPro-Book.otf"));
            appPermissionsLayout.addView(capabilitiesTextView);
            capabilitiesTextView.setLayoutParams(capabilitiesParams);


        }

        return rootView;

    }


    public static FragmentLinkWallet newInstance(LinkMetaData linkMetaData) {
        Bundle args = new Bundle();
        args.putParcelable(MessageExchangeService.EXTRA_METADATA, linkMetaData);
        FragmentLinkWallet fragment = new FragmentLinkWallet();
        fragment.setArguments(args);
        return fragment;
    }
}
