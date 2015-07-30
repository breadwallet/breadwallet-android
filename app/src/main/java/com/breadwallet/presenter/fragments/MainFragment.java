package com.breadwallet.presenter.fragments;

import android.annotation.TargetApi;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;

import com.breadwallet.R;
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
    public EditText addressEditText;


    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        // Inflate the layout for this fragment
        return inflater.inflate(R.layout.fragment_main, container, false);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);
        scanQRButton = (Button) getActivity().findViewById(R.id.mainbuttonscanqrcode);
        payAddressFromClipboardButton = (Button) getActivity().findViewById(R.id.mainbuttonpayaddressfromclipboard);
        scanQRButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (FragmentAnimator.multiplePressingAvailable) {
                    FragmentAnimator.pauseTheAnimationAvailabilityFor(300);
                    FragmentAnimator.animateDecoderFragment();
                    CustomPagerAdapter.adapter.showFragments(false);
                }
            }
        });
        addressEditText = (EditText) getView().findViewById(R.id.addresseditText);
        addressEditText.setGravity(Gravity.CENTER_HORIZONTAL);
        payAddressFromClipboardButton.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event) {
                if (FragmentAnimator.multiplePressingAvailable) {
                    FragmentAnimator.pauseTheAnimationAvailabilityFor(300);
                    String address = MyClipboardManager.readFromClipboard(getActivity());
                    if (checkIfAddressIsValid(address)) {
                        switch (event.getActionMasked()) {
                            case MotionEvent.ACTION_DOWN:
                                payAddressFromClipboardButton.setBackgroundResource(R.drawable.buttonbluepressed);
                                addressEditText.setText(address);
                                new Handler().postDelayed(new Runnable() {
                                    @Override
                                    public void run() {
                                        payAddressFromClipboardButton.setBackgroundResource(R.drawable.buttonblue);
                                    }
                                }, 50);

                                break;
                        }
                    } else {
                        AlertDialog alertDialog = new AlertDialog.Builder(getActivity()).create();
                        alertDialog.setTitle("Alert");
                        alertDialog.setMessage("clipboard doesn't contain a valid bitcoin address");
                        alertDialog.setButton(AlertDialog.BUTTON_NEUTRAL, "OK",
                                new DialogInterface.OnClickListener() {
                                    public void onClick(DialogInterface dialog, int which) {
                                        dialog.dismiss();
                                    }
                                });
                        alertDialog.show();
                    }
                    return false;
                }
                return false;
            }
        });
    }

    public boolean checkIfAddressIsValid(String str) {
        int length = str.length();
        if (length != 34) {
            return false;
        } else {
            for (int i = 0; i < length; i++) {
                if (str.charAt(i) < 48) {
                    Log.e(TAG, "Bad address, char: " + str.charAt(i));
                    return false;
                } else {
                    if (str.charAt(i) > 57 && str.charAt(i) < 65) {
                        Log.e(TAG, "Bad address, char: " + str.charAt(i));
                        return false;
                    }
                    if (str.charAt(i) > 90 && str.charAt(i) < 61) {
                        Log.e(TAG, "Bad address, char: " + str.charAt(i));
                        return false;
                    }
                    if (str.charAt(i) > 122) {
                        Log.e(TAG, "Bad address, char: " + str.charAt(i));
                        return false;
                    }
                }

            }
        }
        return true;
    }
}