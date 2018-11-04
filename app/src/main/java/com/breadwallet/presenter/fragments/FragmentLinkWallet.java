package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.constraint.ConstraintLayout;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.ScrollView;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.protocols.messageexchange.MessageExchangeNetworkHelper;
import com.breadwallet.protocols.messageexchange.MessageExchangeService;
import com.breadwallet.protocols.messageexchange.entities.LinkMetaData;
import com.breadwallet.protocols.messageexchange.entities.ServiceMetaData;
import com.breadwallet.tools.util.Utils;

import java.util.List;

import static com.breadwallet.tools.util.BRConstants.STRING_RESOURCES_FILENAME;
import static com.breadwallet.tools.util.BRConstants.TYPEFACE_PATH_CIRCULARPRO_BOLD;
import static com.breadwallet.tools.util.BRConstants.TYPEFACE_PATH_CIRCULARPRO_BOOK;


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
        ScrollView appPermissionScrollview = rootView.findViewById(R.id.permission_scrollview_parent);
        BRButton positiveButton = rootView.findViewById(R.id.positive_button);
        BRButton negativeButton = rootView.findViewById(R.id.negative_button);

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
                    domainStringBuilder.append(", ");
                }
            }

            TextView domains = new TextView(getContext());
            domains.setText(domainStringBuilder.toString());
            domains.setTypeface(Typeface.createFromAsset(getResources().getAssets(), TYPEFACE_PATH_CIRCULARPRO_BOLD));

            RelativeLayout.LayoutParams domainParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            domainParams.addRule(RelativeLayout.CENTER_HORIZONTAL, domains.getId());
            validDomainsLayout.addView(domains);
            domains.setLayoutParams(domainParams);


            // Get the app permissions list and add them to the UI
            List<ServiceMetaData.Capability> capabilities = serviceMetaData.getCapabilities();
            StringBuilder capabilityStringBuilder = new StringBuilder();

            for (ServiceMetaData.Capability capability : capabilities) {

                if (!Utils.isNullOrEmpty(capability.getDescription())) {
                    String localizedString = getLocalizedStringFromKey(capability.getDescription());
                    String scopeDescription = capability.getScopes().get(MessageExchangeNetworkHelper.DESCRIPTION);

                    if (!Utils.isNullOrEmpty(scopeDescription)) {
                        localizedString = String.format(localizedString, scopeDescription);
                    }

                    capabilityStringBuilder.append("•" + localizedString + "\n\n");
                }
            }

            RelativeLayout.LayoutParams capabilitiesParams = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            TextView capabilitiesTextView = new TextView(getContext());
            capabilitiesParams.addRule(RelativeLayout.ALIGN_PARENT_START, capabilitiesTextView.getId());
            capabilitiesTextView.setText(capabilityStringBuilder.toString());
            capabilitiesTextView.setTypeface(Typeface.createFromAsset(getResources().getAssets(), TYPEFACE_PATH_CIRCULARPRO_BOOK));
            appPermissionsLayout.addView(capabilitiesTextView);
            capabilitiesParams.setMargins(Utils.getPixelsFromDps(getContext(), 26), 0, 0, Utils.getPixelsFromDps(getContext(), 20));
            capabilitiesTextView.setLayoutParams(capabilitiesParams);

            appPermissionScrollview.measure(0, 0);
            if (appPermissionScrollview.getMeasuredHeight() > getResources().getDimension(R.dimen.permissions_scrollview_max_height)) {
                ConstraintLayout.LayoutParams lp = new ConstraintLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT, (int) getResources().getDimension(R.dimen.permissions_scrollview_max_height));
                lp.topToBottom = validDomainsLayout.getId();
                appPermissionScrollview.setLayoutParams(lp);
            }

            positiveButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleLinkResponse(true);
                }
            });

            negativeButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    handleLinkResponse(false);
                }
            });
        }

        return rootView;

    }

    private String getLocalizedStringFromKey(String key) {
        String packageName = getActivity().getPackageName();
        int resId = getResources().getIdentifier(key, STRING_RESOURCES_FILENAME, packageName);
        return getString(resId);
    }

    private void handleLinkResponse(boolean approve){
        Log.d(TAG, "handleLinkResponse(), " + approve);
        MessageExchangeService.enqueueWork(getContext(), MessageExchangeService.createIntent(getContext(), approve));
        getActivity().finish();
    }


    public static FragmentLinkWallet newInstance(LinkMetaData linkMetaData) {
        Bundle args = new Bundle();
        args.putParcelable(MessageExchangeService.EXTRA_METADATA, linkMetaData);
        FragmentLinkWallet fragment = new FragmentLinkWallet();
        fragment.setArguments(args);
        return fragment;
    }
}
