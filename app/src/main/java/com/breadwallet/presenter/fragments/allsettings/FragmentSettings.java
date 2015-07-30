package com.breadwallet.presenter.fragments.allsettings;

import android.content.DialogInterface;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.presenter.fragments.allsettings.settings.FragmentCurrency;
import com.breadwallet.tools.animation.FragmentAnimator;

/**
 * Created by Mihail on 6/29/15.
 */
public class FragmentSettings extends Fragment {
    public static final String TAG = "FragmentSettings";
    private RelativeLayout about;
    private RelativeLayout localCurrency;
    private RelativeLayout recoveryPhrase;
    private RelativeLayout startRecoveryWallet;
    private MainActivity app;
    private FragmentSettings fragmentSettings;
    private TextView currencyName;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragment_settings, container, false);

        return rootView;
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        app = MainActivity.app;
        fragmentSettings = this;
        about = (RelativeLayout) getView().findViewById(R.id.about);
        currencyName = (TextView) getView().findViewById(R.id.three_letters_currency);
        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        final String tmp = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        currencyName.setText(tmp);
        localCurrency = (RelativeLayout) getView().findViewById(R.id.local_currency);
        recoveryPhrase = (RelativeLayout) getView().findViewById(R.id.recovery_phrase);
        startRecoveryWallet = (RelativeLayout) getView().findViewById(R.id.start_recovery_wallet);
        startRecoveryWallet.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.pressWipeWallet(app, app.fragmentWipeWallet);
                app.activityButtonsEnable(false);
            }
        });
        recoveryPhrase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                new AlertDialog.Builder(getActivity())
                        .setTitle("WARNING")
                        .setMessage("DO NOT let anyone see your recovery phrase or they can spend your bitcoins.\n\n" +
                                "NEVER type your recovery phrase into password managers or elsewhere.\n" +
                                "Other devices may be infected.\n\nDO NOT take a screenshot.\nScreenshots are visible to other apps and devices.")
                        .setPositiveButton("show", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                FragmentAnimator.animateSlideToLeft(app, app.fragmentRecoveryPhrase, fragmentSettings);
                            }
                        })
                        .setNegativeButton("cancel", new DialogInterface.OnClickListener() {
                            public void onClick(DialogInterface dialog, int which) {
                                Log.d(TAG, "Canceled the view of the phrase!");
                            }
                        })
                        .show();

            }
        });
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.animateSlideToLeft(app, app.fragmentAbout, fragmentSettings);
            }
        });
        localCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.animateSlideToLeft(app, app.fragmentCurrency, fragmentSettings);
            }
        });
    }

}
