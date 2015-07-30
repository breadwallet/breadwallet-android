package com.breadwallet.presenter;

import android.app.Activity;
import android.app.Application;
import android.os.Handler;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;

/**
 * Created by Mihail on 7/22/15.
 */
public class BreadWalletApp extends Application {
    public static final String TAG = "BreadWalletApp";
    private boolean customToastAvailable = true;
    private String oldMessage;
    private Toast toast;
    ;

    /**
     * Shows a custom toast using the given string as a paramater,
     *
     * @param message the message to be shown in the custom toast
     */
    public void showCustomToast(Activity app, String message, int yOffSet, int duration) {
        if (toast == null) toast = new Toast(getApplicationContext());
        if (customToastAvailable || !oldMessage.equals(message)) {
//            toast.cancel();
            oldMessage = message;
            customToastAvailable = false;
            new Handler().postDelayed(new Runnable() {
                @Override
                public void run() {
                    customToastAvailable = true;
                }
            }, 1000);
            LayoutInflater inflater = app.getLayoutInflater();
            View layout = inflater.inflate(R.layout.toast,
                    (ViewGroup) app.findViewById(R.id.toast_layout_root));
            TextView text = (TextView) layout.findViewById(R.id.toast_text);
            text.setText(message);
            toast.setGravity(Gravity.BOTTOM, 0, yOffSet);
            toast.setDuration(duration);
            toast.setView(layout);
            toast.show();
        }
    }
}
