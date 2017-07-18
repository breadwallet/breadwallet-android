package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Typeface;
import android.os.Build;
import android.os.Bundle;
import android.app.Fragment;
import android.os.Handler;
import android.util.TypedValue;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.util.TrustedNode;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.BRPeerManager;

import java.util.Locale;

import static android.R.id.input;

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
    private Button trustNode;
    AlertDialog mDialog;
    private int mInterval = 3000;
    private Handler mHandler;
//    private TextView nodeLabel;

    Runnable mStatusChecker = new Runnable() {
        @Override
        public void run() {
            try {
                updateStatus(); //this function can change value of mInterval.
            } finally {
                // 100% guarantee that this always happens, even if
                // your update method throws an exception
                mHandler.postDelayed(mStatusChecker, mInterval);
            }
        }
    };

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {


        View rootView = inflater.inflate(
                R.layout.fragment_about, container, false);
        copyLogs = (Button) rootView.findViewById(R.id.copy_logs);
        trustNode = (Button) rootView.findViewById(R.id.trust_node);

        copyLogs.setVisibility(Utils.isEmulatorOrDebug(getActivity()) ? View.VISIBLE : View.GONE);
//        nodeLabel = (TextView) rootView.findViewById(R.id.node_label);
        trustNode.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!SharedPreferencesManager.getTrustNode((Activity) getContext()).isEmpty()) {
                    createDialog(2);
                } else {
                    createDialog(1);
                }

            }
        });

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

    private void updateStatus() {
        if (trustNode != null)
            trustNode.setText("dl peer: " + BRPeerManager.getInstance(getActivity()).getCurrentPeerName());
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


//    private void askToClearTrustedNode() {
//        final Activity app = getActivity();
//        if (app == null) return;
//
//        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(app);
//        TextView customTitle = new TextView(getContext());
//        customTitle.setGravity(Gravity.CENTER);
//        int pad = Utils.getPixelsFromDps(app, 32);
//        customTitle.setPadding(pad, pad, pad, pad);
//        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
//        customTitle.setText("clear trusted node?");
//        alertDialog.setCustomTitle(customTitle);
//
//        alertDialog.setNegativeButton("cancel",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        dialog.cancel();
//                    }
//                });
//
//        alertDialog.setPositiveButton("clear",
//                new DialogInterface.OnClickListener() {
//                    public void onClick(DialogInterface dialog, int which) {
//                        SharedPreferencesManager.putTrustNode((Activity) getContext(), "");
//                        BRPeerManager.getInstance((Activity) getContext()).updateFixedPeer();
//                    }
//                });
//
//        mDialog = alertDialog.show();
//
//    }

    private void createDialog(final int mode) {

        final Activity app = getActivity();
        if (app == null) return;

        final AlertDialog.Builder alertDialog = new AlertDialog.Builder(app);
        final TextView customTitle = new TextView(getContext());

        customTitle.setGravity(Gravity.CENTER);
        customTitle.setTextAlignment(View.TEXT_ALIGNMENT_CENTER);
        int pad = Utils.getPixelsFromDps(app, 32);
        customTitle.setPadding(pad, pad, pad, pad);
        customTitle.setText(mode == 1 ? "set a trusted node" : "clear trusted node?");
        customTitle.setTextSize(TypedValue.COMPLEX_UNIT_SP, 18);
        customTitle.setTypeface(null, Typeface.BOLD);
        alertDialog.setCustomTitle(customTitle);

        final EditText input = new EditText(app);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.MATCH_PARENT);
        int pix = Utils.getPixelsFromDps(app, 24);

        input.setPadding(pix, 0, pix, pix);
        input.setLayoutParams(lp);
        if (mode == 1)
            alertDialog.setView(input);

        alertDialog.setNegativeButton("cancel",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {
                        dialog.cancel();
                    }
                });

        alertDialog.setPositiveButton(mode == 1 ? "trust" : "clear",
                new DialogInterface.OnClickListener() {
                    public void onClick(DialogInterface dialog, int which) {

                    }
                });

        mDialog = alertDialog.show();


        //Overriding the handler immediately after show is probably a better approach than OnShowListener as described below
        mDialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mode == 1) {
                    String str = input.getText().toString();
                    if (TrustedNode.isValid(str)) {
                        mDialog.setMessage("");
                        SharedPreferencesManager.putTrustNode(app, str);
                        BRPeerManager.getInstance(app).updateFixedPeer();
                        mDialog.dismiss();
                    } else {
                        customTitle.setText("invalid node");
                        new Handler().postDelayed(new Runnable() {
                            @Override
                            public void run() {
                                customTitle.setText("set a trusted node");
                            }
                        }, 1000);
                    }
                } else {
                    SharedPreferencesManager.putTrustNode((Activity) getContext(), "");
                    BRPeerManager.getInstance((Activity) getContext()).updateFixedPeer();
                    mDialog.dismiss();
                }
            }
        });
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(), null);

        mHandler = new Handler();
        startRepeatingTask();
    }

    @Override
    public void onPause() {
        super.onPause();
        stopRepeatingTask();
    }

    void startRepeatingTask() {
        mStatusChecker.run();
    }

    void stopRepeatingTask() {
        mHandler.removeCallbacks(mStatusChecker);
    }
}