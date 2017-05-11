package com.breadwallet.presenter.fragments;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Environment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.util.Utils;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 7/14/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentAbout extends Fragment {
    private static final String TAG = FragmentAbout.class.getName();
    private Button copyLogs;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View rootView = inflater.inflate(
                R.layout.fragment_about, container, false);
        copyLogs = (Button) rootView.findViewById(R.id.copy_logs);

        copyLogs.setVisibility(Utils.isEmulatorOrDebug(getActivity()) ? View.VISIBLE : View.GONE);

        copyLogs.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String logs = Utils.getLogs(getActivity());
                if (Utils.isNullOrEmpty(logs)) {
                    Toast.makeText(getActivity().getApplicationContext(), "Logs are empty", Toast.LENGTH_SHORT).show();
                } else {
                    BRClipboardManager.copyToClipboard(getContext(), logs);
                    Toast.makeText(getActivity().getApplicationContext(), "Logs are copied", Toast.LENGTH_SHORT).show();
                }

            }
        });
        PackageInfo pInfo = null;
        int versionCode = 0;
        try {
            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
            versionCode = pInfo.versionCode;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        TextView versionText = (TextView) rootView.findViewById(R.id.about1);
        if (versionText != null)
            versionText.setText(String.format(getString(R.string.breadwallet_v), versionCode));
        TextView support = (TextView) rootView.findViewById(R.id.about4);
        if (support != null)
            support.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    if (BRAnimator.checkTheMultipressingAvailability()) {
                        String to = BRConstants.SUPPORT_EMAIL;
                        PackageInfo pInfo = null;
                        String versionName = "";
                        try {
                            pInfo = getActivity().getPackageManager().getPackageInfo(getActivity().getPackageName(), 0);
                            versionName = pInfo.versionName;
                        } catch (PackageManager.NameNotFoundException e) {
                            e.printStackTrace();
                        }
                        String message = String.format(Locale.getDefault(), "%s / Android %s / breadwallet %s\n\n",
                                Build.MODEL, Build.VERSION.RELEASE, versionName);
                        Intent email = new Intent(Intent.ACTION_SEND);
                        email.putExtra(Intent.EXTRA_EMAIL, new String[]{to});
                        email.putExtra(Intent.EXTRA_TEXT, message);
                        email.putExtra(Intent.EXTRA_SUBJECT, "support request");

                        // need this to prompts email client only
                        email.setType("message/rfc822");
                        startActivity(Intent.createChooser(email, getActivity().getString(R.string.choose_an_email_client)));
                    }
                }
            });

        return rootView;
    }

//    public void sendLogs(Context context, String logs) {
//        //set a file
//        Date datum = new Date();
//        SimpleDateFormat df = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
//        String fullName = df.format(datum) + "appLog.txt";
//        File file = new File(Environment.getExternalStorageDirectory() + File.separator + fullName);
//
//        try {//clears a file
//
//            boolean result = file.createNewFile();
//            Log.e(TAG, "sendLogs: " + result);
//            FileWriter out = new FileWriter(file);
//            out.write(logs);
//            out.close();
////            //Runtime.getRuntime().exec("logcat -d -v time -f "+file.getAbsolutePath());
//        } catch (IOException e) {
//            Toast.makeText(context.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
//            return;
//        }
//
//        //clear the log
//        try {
//            Runtime.getRuntime().exec("logcat -c");
//        } catch (IOException e) {
//            Toast.makeText(context.getApplicationContext(), e.toString(), Toast.LENGTH_SHORT).show();
//        }
//
//        Uri fileUri = Uri.fromFile(file);
//        Intent intent = new Intent(Intent.ACTION_SEND);
//        intent.setType("text/plain");
//        intent.putExtra(Intent.EXTRA_EMAIL, new String[]{"mihail@breadwallet.com"});
//        intent.putExtra(Intent.EXTRA_SUBJECT, "Syncing bug wallet logs");
//        intent.putExtra(Intent.EXTRA_TEXT, "The logs will be attached:");
//        intent.putExtra("exit_on_sent", true);
//        if (!file.exists() || !file.canRead()) {
//            Toast.makeText(context, "Attachment Error", Toast.LENGTH_SHORT).show();
//            return;
//        }
//        intent.putExtra(Intent.EXTRA_STREAM, fileUri);
//        context.startActivity(Intent.createChooser(intent, "Send email..."));
//    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);
    }
}