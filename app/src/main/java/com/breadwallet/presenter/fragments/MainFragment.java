package com.breadwallet.presenter.fragments;

import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.adapter.CustomPagerAdapter;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.others.MyClipboardManager;

/**
 * The main fragment shown in the MainActivity
 */
public class MainFragment extends Fragment {
    public static final String TAG = "MainFragment";
    private Button scanQRButton;
    private Button payAddressFromClipboardButton;
    private MainActivity mainActivity;
    private EditText addressEditText;

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
        payAddressFromClipboardButton = (Button) getActivity().findViewById(R.id.mainbuttonpayaddressfromclipboard);
        mainActivity = MainActivity.getApp();

        scanQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                FragmentAnimator.animateDecoderFragment();
                CustomPagerAdapter.getAdapter().showFragments(false);
            }
        });
        addressEditText = (EditText) getView().findViewById(R.id.addresseditText);
        addressEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        addressEditText.setShowSoftInputOnFocus(false);
        payAddressFromClipboardButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                switch (event.getActionMasked()) {
                    case MotionEvent.ACTION_DOWN:
                        payAddressFromClipboardButton.setBackgroundResource(R.drawable.buttonbluepressed);
                        addressEditText.setText(MyClipboardManager.readFromClipboard(getActivity()));
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                payAddressFromClipboardButton.setBackgroundResource(R.drawable.buttonblue);
                            }
                        }, 50);

                        break;
                }
                return false;
            }
        });
    }

    public EditText getAddressEditText() {
        return addressEditText;
    }
}