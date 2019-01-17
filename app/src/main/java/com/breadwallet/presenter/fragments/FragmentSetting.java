package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.BuildConfig;
import com.breadwallet.R;
import com.breadwallet.presenter.activities.did.DidNickActivity;
import com.breadwallet.presenter.activities.did.DidQuestionActivity;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.customviews.BaseTextView;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.tools.adapter.SettingsAdapter;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.manager.BRClipboardManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.security.BRKeyStore;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.SettingsUtil;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
import com.elastos.jni.Utility;

import org.w3c.dom.Text;

import java.util.ArrayList;
import java.util.List;

public class FragmentSetting extends Fragment {

    private static final String TAG = FragmentSetting.class.getSimpleName();
    public static final String EXTRA_MODE = "com.breadwallet.presenter.activities.settings.EXTRA_MODE";
    public static final String MODE_SETTINGS = "settings";
    public static final String MODE_PREFERENCES = "preferences";
    public static final String MODE_SECURITY = "security";
    public static final String MODE_CURRENCY_SETTINGS = "currency_settings";
    private boolean mIsButtonBackArrow;
    private View mRootview;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        mRootview = inflater.inflate(R.layout.fragment_settings, container, false);
        return mRootview;
    }

    @Override
    public void onResume() {
        super.onResume();
        setTitleAndList(mRootview);
    }

    private TextView mDidContent;
    private TextView mNickname;
    private void setTitleAndList(View rootView) {
        mDidContent = rootView.findViewById(R.id.did_content);
        String did = "";
        try {
            byte[] phrase = BRKeyStore.getPhrase(getContext(), 0);
            String publickey = Utility.getInstance(getContext()).getSinglePublicKey(new String(phrase));
            if(publickey != null) {
                did = Utility.getInstance(getContext()).getDid(publickey);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        mDidContent.setText("did:ela:"+did);

        mNickname = rootView.findViewById(R.id.did_alias);
        String nickname = BRSharedPrefs.getNickname(getContext());
        mNickname.setText(nickname);

        BaseTextView title = rootView.findViewById(R.id.title);
        ListView settingsList = rootView.findViewById(R.id.settings_list);
        List<BRSettingsItem> settingsItems = new ArrayList<>();
        String mode = SettingsActivity.MODE_SETTINGS;
        if (mode == null) {
            throw new IllegalArgumentException("Need mode for the settings activity");
        }
        switch (mode) {
            case MODE_SETTINGS:
                settingsItems = SettingsUtil.getMainSettings(getActivity());
                title.setText(getString(R.string.Settings_title));
                mIsButtonBackArrow = false;
                break;
            case MODE_PREFERENCES:
                settingsItems = SettingsUtil.getPreferencesSettings(getActivity());
                title.setText(getString(R.string.Settings_preferences));
                mIsButtonBackArrow = true;
                break;
            case MODE_SECURITY:
                settingsItems = SettingsUtil.getSecuritySettings(getActivity());
                title.setText(getString(R.string.MenuButton_security));
                mIsButtonBackArrow = true;
                break;
            case MODE_CURRENCY_SETTINGS:
                BaseWalletManager walletManager = WalletsMaster.getInstance(getActivity()).getCurrentWallet(getActivity());
                settingsItems = walletManager.getSettingsConfiguration().getSettingsList();
                String currencySettingsLabel = String.format("%s %s", walletManager.getName(), getString(R.string.Settings_title));
                title.setText(currencySettingsLabel);
                mIsButtonBackArrow = true;
                break;
        }

        settingsList.setAdapter(new SettingsAdapter(getActivity(), R.layout.settings_list_item, settingsItems));
        rootView.findViewById(R.id.what_did).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), DidQuestionActivity.class);
                getActivity().startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
        rootView.findViewById(R.id.enter_nick).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Intent intent = new Intent(getActivity(), DidNickActivity.class);
                getActivity().startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_right, R.anim.exit_to_left);
            }
        });
        rootView.findViewById(R.id.back_button).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                UiUtils.openScanner(getActivity(), BRConstants.SCANNER_DID_REQUEST);
            }
        });
        rootView.findViewById(R.id.did_copy).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                copyText();
            }
        });
    }

    private void copyText() {
        Activity app = getActivity();
        BRClipboardManager.putClipboard(app, mDidContent.getText().toString());
        //copy the legacy for testing purposes (testnet faucet money receiving)
        if (Utils.isEmulatorOrDebug(app) && BuildConfig.BITCOIN_TESTNET)
            BRClipboardManager.putClipboard(app, WalletsMaster.getInstance(app).getCurrentWallet(app).undecorateAddress(mDidContent.getText().toString()));
        Toast.makeText(getActivity(), "has copy to clipboard", Toast.LENGTH_SHORT).show();

    }


    public static FragmentSetting newInstance(String text) {

        FragmentSetting f = new FragmentSetting();
        Bundle b = new Bundle();
        b.putString("text", text);
        f.setArguments(b);

        return f;
    }

}
