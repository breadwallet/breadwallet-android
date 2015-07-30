package com.breadwallet.presenter.fragments;

/**
 * Created by Mihail on 7/24/15.
 */

import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.tools.others.MyClipboardManager;

public class SharingFragment extends DialogFragment {

    private TextView copyAddress;
    private TextView sendEmail;
    private TextView sendMessage;
    private TextView requestAmount;
    private boolean customToastAvailable = true;
    private String theAddress;
    private String theMessage;

    public SharingFragment() {
        // Empty constructor required for DialogFragment
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_sharing_options, container);
        copyAddress = (TextView) view.findViewById(R.id.copy_address);
        sendEmail = (TextView) view.findViewById(R.id.send_email);
        sendMessage = (TextView) view.findViewById(R.id.send_message);
        requestAmount = (TextView) view.findViewById(R.id.request_amount);
        theMessage = "Hi, here is my bitcoin address: ";
        getDialog().setTitle("Receive bitcoins");

        copyAddress.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                MyClipboardManager.copyToClipboard(getActivity(), theAddress);
                if (customToastAvailable) {
                    customToastAvailable = false;
                    new Handler().postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            customToastAvailable = true;
                        }
                    }, 2000);
                    ((BreadWalletApp) getActivity().getApplicationContext()).
                            showCustomToast(getActivity(), "Address copied to clipboard", 360, Toast.LENGTH_SHORT);
                    getDialog().cancel();
                }
            }

        });

        sendEmail.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent emailIntent = new Intent(Intent.ACTION_SENDTO, Uri.fromParts(
                        "mailto", "abc@gmail.com", null));
                emailIntent.putExtra(Intent.EXTRA_SUBJECT, "Bitcoin Address");
                emailIntent.putExtra(Intent.EXTRA_TEXT, "Hi this is my bitcoin address: " + theAddress);
                startActivity(Intent.createChooser(emailIntent, "Send your bitcoin address"));
                getDialog().cancel();
            }
        });

        sendMessage.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent sendIntent = new Intent(Intent.ACTION_VIEW);
                sendIntent.setData(Uri.parse("sms:"));
                sendIntent.putExtra("sms_body", theMessage + theAddress);
                startActivity(sendIntent);
                getDialog().cancel();
            }
        });

        requestAmount.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                //TODO: actions on requesting amount
            }
        });

        return view;
    }

    public void setTheAddress(String address) {
        theAddress = address;
    }
}