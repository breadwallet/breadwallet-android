package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.breadwallet.R;
import com.breadwallet.tools.util.BRConstants;

import java.util.ArrayList;

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

        if (bundle != null) {
        }

        return rootView;

    }


    public static FragmentLinkWallet newInstance(Bundle bundle) {
        FragmentLinkWallet fragment = new FragmentLinkWallet();
        fragment.setArguments(bundle);
        return fragment;
    }
}
