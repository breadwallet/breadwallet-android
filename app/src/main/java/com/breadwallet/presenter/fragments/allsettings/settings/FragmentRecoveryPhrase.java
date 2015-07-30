package com.breadwallet.presenter.fragments.allsettings.settings;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;

/**
 * Created by Mihail on 7/22/15.
 */
public class FragmentRecoveryPhrase extends Fragment {

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_recovery_phrase, container, false);
        return rootView;
    }
}
