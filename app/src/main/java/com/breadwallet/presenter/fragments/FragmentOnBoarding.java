package com.breadwallet.presenter.fragments;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.intro.OnBoardingActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.EventUtils;

public class FragmentOnBoarding extends Fragment {
    private static final String ARGUMENT_POSITION = "com.breadwallet.presenter.fragments.FragmentOnBoarding.POSITION";

    private Button mBuyButton;
    private Button mBrowseButton;
    private NavigationListener onBoardingHost;

    public FragmentOnBoarding() {
    }

    public static FragmentOnBoarding newInstance(int position) {
        FragmentOnBoarding fragment = new FragmentOnBoarding();
        Bundle args = new Bundle();
        args.putInt(ARGUMENT_POSITION, position);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View rootLayout = inflater.inflate(R.layout.fragment_onboarding, container, false);

        int position = 0;
        if (getArguments() != null) {
            position = getArguments().getInt(ARGUMENT_POSITION);
        }

        mBuyButton = rootLayout.findViewById(R.id.button_buy);
        mBrowseButton = rootLayout.findViewById(R.id.button_browse);
        BaseTextView primaryText = rootLayout.findViewById(R.id.primary_text);
        BaseTextView secondaryText = rootLayout.findViewById(R.id.secondary_text);
        BaseTextView lastScreenText = rootLayout.findViewById(R.id.last_screen_title);
        ImageView imageView = rootLayout.findViewById(R.id.image_view);
        switch (position) {
            case 0:
                primaryText.setText(R.string.OnboardingPageTwo_title);
                secondaryText.setText(R.string.OnboardingPageTwo_subtitle);
                break;
            case 1:
                primaryText.setText(R.string.OnboardingPageThree_title);
                secondaryText.setText(R.string.OnboardingPageThree_subtitle);
                imageView.setImageDrawable(getActivity().getDrawable(R.drawable.ic_currencies));
                break;
            case 2:
                lastScreenText.setVisibility(View.VISIBLE);
                mBuyButton.setVisibility(View.VISIBLE);
                mBrowseButton.setVisibility(View.VISIBLE);
                primaryText.setVisibility(View.INVISIBLE);
                secondaryText.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.INVISIBLE);
                lastScreenText.setVisibility(View.VISIBLE);
                mBuyButton.setOnClickListener(view -> {
                    toggleButtons(false);
                    EventUtils.pushEvent(EventUtils.EVENT_FINAL_PAGE_BUY_COIN);
                    if (BRKeyStore.getPinCode(getContext()).length() > 0) {
                        OnBoardingActivity.showBuyScreen(getActivity());
                    } else {
                        onBoardingHost.setupPin(OnBoardingActivity.NextScreen.BUY_SCREEN);
                    }
                });
                mBrowseButton.setOnClickListener(view -> {
                    toggleButtons(false);
                    onBoardingHost.progressToBrowse();
                });
                break;
        }
        return rootLayout;
    }

    @Override
    public void onResume() {
        super.onResume();
        toggleButtons(true);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        onBoardingHost = (NavigationListener)context;
    }

    private void toggleButtons(Boolean isEnabled) {
        mBrowseButton.setEnabled(isEnabled);
        mBuyButton.setEnabled(isEnabled);
    }

    /**
     * Interface to provide interaction for navigation between the fragment and the host activity.
     */
    public interface NavigationListener {
        /**
         * Navigate to PIN setup screen.
         */
        void setupPin(OnBoardingActivity.NextScreen nextScreen);

        /**
         * Navigate to home activity.
         */
        void progressToBrowse();
    }
}
