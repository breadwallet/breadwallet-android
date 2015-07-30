package com.breadwallet.presenter.fragments.allsettings.settings;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;

/**
 * Created by Mihail on 7/14/15.
 */
public class FragmentWipeWallet extends Fragment {

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragment_reset_wallet, container, false);

        return rootView;
    }
}
