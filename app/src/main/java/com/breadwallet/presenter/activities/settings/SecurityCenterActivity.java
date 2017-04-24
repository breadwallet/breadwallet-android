package com.breadwallet.presenter.activities.settings;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.hardware.fingerprint.FingerprintManager;
import android.os.Bundle;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroWriteDownActivity;
import com.breadwallet.presenter.activities.UpdatePitActivity;
import com.breadwallet.presenter.entities.BRSecurityCenterItem;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.manager.SharedPreferencesManager;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

import java.util.ArrayList;
import java.util.List;

public class SecurityCenterActivity extends AppCompatActivity {
    private static final String TAG = SecurityCenterActivity.class.getName();

    public ListView mListView;
    public RelativeLayout layout;
    public List<BRSecurityCenterItem> itemList;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_security_center);
//        setStatusBarColor(android.R.color.transparent);

        itemList = new ArrayList<>();
        mListView = (ListView) findViewById(R.id.menu_listview);
        updateList();

    }

    @Override
    protected void onResume() {
        super.onResume();
        updateList();
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        BRAnimator.startBreadActivity(this, false);
    }

    private void setStatusBarColor(int color) {
        Window window = getWindow();
        window.clearFlags(WindowManager.LayoutParams.FLAG_TRANSLUCENT_STATUS);
        window.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS);
        window.setStatusBarColor(getColor(color));
    }

    public RelativeLayout getMainLayout() {
        return layout;
    }

    public class SecurityCenterListAdapter extends ArrayAdapter<BRSecurityCenterItem> {

        private List<BRSecurityCenterItem> items;
        private Context mContext;
        private int defaultLayoutResource = R.layout.security_center_list_item;

        public SecurityCenterListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<BRSecurityCenterItem> items) {
            super(context, resource);
            this.items = items;
            this.mContext = context;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

//            Log.e(TAG, "getView: pos: " + position + ", item: " + items.get(position));
            if (convertView == null) {
                // inflate the layout
                LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
                convertView = inflater.inflate(defaultLayoutResource, parent, false);
            }
            TextView title = (TextView) convertView.findViewById(R.id.item_title);
            TextView text = (TextView) convertView.findViewById(R.id.item_text);
            ImageView checkMark = (ImageView) convertView.findViewById(R.id.check_mark);

            title.setText(items.get(position).title);
            text.setText(items.get(position).text);
            checkMark.setImageResource(items.get(position).checkMarkResId);
            convertView.setOnClickListener(items.get(position).listener);
            return convertView;

        }

        @Override
        public int getCount() {
            return items == null ? 0 : items.size();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
    }

    private void updateList() {
        boolean isPinSet = KeyStoreManager.getPinCode(this).length() == 6;
        itemList.clear();
        itemList.add(new BRSecurityCenterItem("6-Digit PIN", "Unlocks your Bread, authorizes send money.",
                isPinSet ? R.drawable.ic_check_mark_blue : R.drawable.ic_check_mark_grey, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: 6-Digit PIN");
                Intent intent = new Intent(SecurityCenterActivity.this, UpdatePitActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }));

        boolean isFingerPrintAvailable = false;
        // Check if we're running on Android 6.0 (M) or higher
        //Fingerprint API only available on from Android 6.0 (M)
        FingerprintManager fingerprintManager = (FingerprintManager) getSystemService(Context.FINGERPRINT_SERVICE);
        if (ActivityCompat.checkSelfPermission(this, Manifest.permission.USE_FINGERPRINT) != PackageManager.PERMISSION_GRANTED) {
            // TODO: Consider calling
            //    ActivityCompat#requestPermissions
            // here to request the missing permissions, and then overriding
            //   public void onRequestPermissionsResult(int requestCode, String[] permissions,
            //                                          int[] grantResults)
            // to handle the case where the user grants the permission. See the documentation
            // for ActivityCompat#requestPermissions for more details.
            if (!fingerprintManager.isHardwareDetected()) {
                // Device doesn't support fingerprint authentication
            } else if (!fingerprintManager.hasEnrolledFingerprints()) {
                // User hasn't enrolled any fingerprints to authenticate with
            } else {
                // Everything is ready for fingerprint authentication
                isFingerPrintAvailable = true;
            }
        }

        itemList.add(new BRSecurityCenterItem("FingerPrint", "Unlocks your Bread, authorizes send money to set limit.",
                isFingerPrintAvailable ? R.drawable.ic_check_mark_blue : R.drawable.ic_check_mark_grey, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Touch ID");
            }
        }));

        boolean isPaperKeySet = SharedPreferencesManager.getPhraseWroteDown(this);
        itemList.add(new BRSecurityCenterItem("Paper Key", "Restores your Bread on new devices and after software updates.",
                isPaperKeySet ? R.drawable.ic_check_mark_blue : R.drawable.ic_check_mark_grey, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Paper Key");
                Intent intent = new Intent(SecurityCenterActivity.this, IntroWriteDownActivity.class);
                startActivity(intent);
                overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        }));

        mListView.setAdapter(new SecurityCenterListAdapter(this, R.layout.menu_list_item, itemList));
    }
}
