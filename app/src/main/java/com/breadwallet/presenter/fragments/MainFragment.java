package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;

/**
 * The main fragment shown in the MainActivity
 */
public class MainFragment extends Fragment {
    public static final String TAG = "MainFragment";
    private Button scanQRButton;
//    private Button payAddressFromClipboardButton;
    private MainActivity mainActivity;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        scanQRButton = (Button) getActivity().findViewById(R.id.mainbuttonscanqrcode);
//        payAddressFromClipboardButton = (Button) getActivity().findViewById(R.id.mainbuttonpayaddressfromclipboard);
        mainActivity = (MainActivity) getActivity();
        scanQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                mainActivity.animateDecoderFragment();
            }
        });

    }
}