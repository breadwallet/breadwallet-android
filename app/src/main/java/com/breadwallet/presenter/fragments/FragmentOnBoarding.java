package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.InputPinActivity;
import com.breadwallet.presenter.activities.intro.OnBoardingActivity;
import com.breadwallet.presenter.customviews.BRButton;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.security.PostAuth;
import com.platform.APIClient;

public class FragmentOnBoarding extends Fragment {
    private static final String ARGUMENT_POSITION = "com.breadwallet.presenter.fragments.FragmentOnBoarding.POSITION";

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

        BRButton buttonBuy = rootLayout.findViewById(R.id.button_buy);
        BRButton buttonBrowse = rootLayout.findViewById(R.id.button_browse);
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
                buttonBuy.setVisibility(View.VISIBLE);
                buttonBrowse.setVisibility(View.VISIBLE);
                primaryText.setVisibility(View.INVISIBLE);
                secondaryText.setVisibility(View.INVISIBLE);
                imageView.setVisibility(View.INVISIBLE);
                lastScreenText.setVisibility(View.VISIBLE);
                buttonBuy.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRKeyStore.getPinCode(getContext()).length() > 0) {
                            OnBoardingActivity.showBuyScreen(getActivity());
                        } else {
                            OnBoardingActivity.setNextScreen(OnBoardingActivity.NextScreen.BUY_SCREEN);
                            setupPin();
                        }
                    }
                });
                buttonBrowse.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (BRKeyStore.getPinCode(getActivity()).length() > 0) {
                            UiUtils.startBreadActivity(getActivity(), true);
                        } else {
                            OnBoardingActivity.setNextScreen(OnBoardingActivity.NextScreen.HOME_SCREEN);
                            setupPin();
                        }
                    }
                });
                break;
        }

        return rootLayout;
    }

    private void setupPin() {
        PostAuth.getInstance().onCreateWalletAuth(getActivity(), false, new PostAuth.AuthenticationSuccessListener() {
            @Override
            public void onAuthenticatedSuccess() {
                Activity activity = getActivity();
                if (activity != null) {
                    APIClient.getInstance(activity).updatePlatform(getActivity());
                    Intent intent = new Intent(activity, InputPinActivity.class);
                    activity.overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
                    activity.startActivityForResult(intent, InputPinActivity.SET_PIN_REQUEST_CODE);
                }
            }
        });
    }

}
