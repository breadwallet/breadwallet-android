package com.breadwallet.presenter.fragments.allsettings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
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
        app = MainActivity.getApp();
        fragmentSettings = this;
        about = (RelativeLayout) getView().findViewById(R.id.about);
        currencyName = (TextView) getView().findViewById(R.id.three_letters_currency);
        SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
        String tmp = settings.getString(FragmentCurrency.CURRENT_CURRENCY, "USD");
        currencyName.setText(tmp);
        localCurrency = (RelativeLayout) getView().findViewById(R.id.local_currency);
        about.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.animateSlideToLeft(app, app.getFragmentAbout(), fragmentSettings);
            }
        });
        localCurrency.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                FragmentAnimator.animateSlideToLeft(app, app.getFragmentCurrency(), fragmentSettings);
            }
        });
    }

    public void setCurrencyText(final String selectedCurrency) {
        currencyName.setText(selectedCurrency);
        Log.d(TAG, "in the setCurrencyText() and the selectedCurrency is: " + selectedCurrency);
        Log.d(TAG, "after assigning the text is: " + currencyName.getText().toString());
    }
}
