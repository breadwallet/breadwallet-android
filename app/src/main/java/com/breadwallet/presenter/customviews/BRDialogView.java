package com.breadwallet.presenter.customviews;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.DialogFragment;
import android.content.DialogInterface;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.constraint.ConstraintLayout;
import android.text.SpannableString;
import android.text.method.LinkMovementMethod;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.ImageButton;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.util.Utils;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 3/15/17.
 * Copyright (c) 2017 breadwallet LLC
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
public class BRDialogView extends DialogFragment {

    private static final String TAG = BRDialogView.class.getName();

    private String title = "";
    private String message = "";
    private String posButton = "";
    private String negButton = "";
    private BRDialogView.BROnClickListener posListener;
    private BRDialogView.BROnClickListener negListener;
    private BRDialogView.BROnClickListener helpListener;
    private DialogInterface.OnDismissListener dismissListener;
    private int iconRes = 0;
    private BRButton negativeButton;
    private BRButton positiveButton;
    private LinearLayout buttonsLayout;
    private ImageButton helpButton;

    //provide the way to have clickable span in the message
    private SpannableString spanMessage;

    private ConstraintLayout mainLayout;
    private boolean showHelpIcon;

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        // Use the Builder class for convenient dialog construction
        AlertDialog.Builder builder = new AlertDialog.Builder(getActivity());
        LayoutInflater inflater = getActivity().getLayoutInflater();
        View view = inflater.inflate(R.layout.bread_alert_dialog, null);
        TextView titleText = (TextView) view.findViewById(R.id.dialog_title);
        TextView messageText = (TextView) view.findViewById(R.id.dialog_text);
        BRButton positiveButton = view.findViewById(R.id.pos_button);
        negativeButton = view.findViewById(R.id.neg_button);
//        ImageView icon = (ImageView) view.findViewById(R.id.dialog_icon);
        mainLayout = (ConstraintLayout) view.findViewById(R.id.main_layout);
        buttonsLayout = (LinearLayout) view.findViewById(R.id.linearLayout3);
        helpButton = view.findViewById(R.id.help_icon);

        titleText.setText(title);
        messageText.setText(message);
        if (spanMessage != null) {
            messageText.setText(spanMessage);
            messageText.setMovementMethod(LinkMovementMethod.getInstance());
        }
        positiveButton.setHasShadow(false);
        positiveButton.setText(posButton);
        positiveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                if (posListener != null)
                    posListener.onClick(BRDialogView.this);
            }
        });
        if (Utils.isNullOrEmpty(negButton)) {
            Log.e(TAG, "onCreateDialog: removing negative button");
            buttonsLayout.removeView(negativeButton);
            buttonsLayout.requestLayout();
        }

        negativeButton.setHasShadow(false);
        negativeButton.setText(negButton);
        negativeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!BRAnimator.isClickAllowed()) return;
                if (negListener != null)
                    negListener.onClick(BRDialogView.this);
            }
        });
//        if (iconRes != 0)
//            icon.setImageResource(iconRes);

        builder.setView(view);

        if (showHelpIcon) {
            helpButton.setVisibility(View.VISIBLE);

            messageText.setPadding(0, 0,0 , Utils.getPixelsFromDps(getContext(), 16));

            helpButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (!BRAnimator.isClickAllowed()) return;
                    if (helpListener != null)
                        helpListener.onClick(BRDialogView.this);
                }
            });

        } else {
            helpButton.setVisibility(View.INVISIBLE);

        }
//        builder.setOnDismissListener(dismissListener);
        // Create the AlertDialog object and return it
        return builder.create();
    }

    public void showHelpIcon(boolean show) {
        this.showHelpIcon = show;
    }

    @Override
    public void onDismiss(DialogInterface dialog) {
        super.onDismiss(dialog);
        if (dismissListener != null)
            dismissListener.onDismiss(dialog);
    }

    @Override
    public void onActivityCreated(Bundle savedInstanceState) {
        super.onActivityCreated(savedInstanceState);

    }

    public void setTitle(String title) {
        this.title = title;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public void setSpan(SpannableString message) {
        if (message == null) {
            BRReportsManager.reportBug(new NullPointerException("setSpan with null message"));
            return;
        }
        this.spanMessage = message;
    }

    public void setPosButton(@NonNull String posButton) {
        this.posButton = posButton;
    }

    public void setNegButton(String negButton) {
        this.negButton = negButton;
    }

    public void setPosListener(BRDialogView.BROnClickListener posListener) {
        this.posListener = posListener;
    }

    public void setNegListener(BRDialogView.BROnClickListener negListener) {
        this.negListener = negListener;
    }

    public void setHelpListener(BROnClickListener helpListener) {
        this.helpListener = helpListener;
    }

    public void setDismissListener(DialogInterface.OnDismissListener dismissListener) {
        this.dismissListener = dismissListener;
    }

    public void setIconRes(int iconRes) {
        this.iconRes = iconRes;
    }

    public static interface BROnClickListener {
        void onClick(BRDialogView brDialogView);
    }

    public void dismissWithAnimation() {
        BRDialogView.this.dismiss();

    }

//    public interface SpanClickListener {
//        void onClick();
//
//    }

}
