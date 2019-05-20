package com.breadwallet.presenter.activities.intro;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.PaperKeyActivity;
import com.breadwallet.presenter.activities.PaperKeyProveActivity;
import com.breadwallet.presenter.activities.util.BRActivity;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.security.AuthManager;
import com.breadwallet.tools.security.PostAuth;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;

public class WriteDownActivity extends BRActivity {
    private static final String TAG = WriteDownActivity.class.getName();
    public static final String EXTRA_VIEW_REASON = "com.breadwallet.EXTRA_VIEW_REASON";

    public enum ViewReason {
        /* Activity was shown because a new wallet was created. */
        NEW_WALLET(0),

        /* Activity was shown from settings.  */
        SETTINGS(1),

        /* Activity was shown from settings.  */
        ON_BOARDING(2),

        /* Invalid reason.  */
        ERROR(-1);

        private int mValue;

        ViewReason(int value) {
            mValue = value;
        }

        public int getValue() {
            return mValue;
        }

        public static ViewReason valueOf(int value) {
            for (ViewReason viewReason : values()) {
                if (viewReason.getValue() == value) {
                    return viewReason;
                }
            }
            return null;
        }
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_write_down);

        Button writeButton = findViewById(R.id.button_write_down);
        ImageButton close = findViewById(R.id.close_button);
        final ViewReason viewReason = ViewReason.valueOf(getIntent().getIntExtra(EXTRA_VIEW_REASON, ViewReason.ERROR.getValue()));
        final String doneAction = getIntent().getStringExtra(PaperKeyProveActivity.EXTRA_DONE_ACTION);

        close.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (PaperKeyActivity.DoneAction.SHOW_BUY_SCREEN.name().equals(doneAction)) {
                    OnBoardingActivity.showBuyScreen(WriteDownActivity.this);
                    WriteDownActivity.this.finishAffinity();
                } else {
                    switch (viewReason) {
                        case NEW_WALLET:
                            UiUtils.startBreadActivity(WriteDownActivity.this, false);
                            break;
                        case ON_BOARDING:
                            UiUtils.startBreadActivity(WriteDownActivity.this, false);
                            break;
                        case SETTINGS:
                            // Fall through
                        default:
                            onBackPressed();
                            break;
                    }
                }

            }
        });

        ImageButton faq = findViewById(R.id.faq_button);
        faq.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                BaseWalletManager wm = WalletsMaster.getInstance().getCurrentWallet(WriteDownActivity.this);
                UiUtils.showSupportFragment(WriteDownActivity.this, BRConstants.FAQ_PAPER_KEY, wm);

            }
        });
        writeButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!UiUtils.isClickAllowed()) return;
                AuthManager.getInstance().authPrompt(WriteDownActivity.this, null,
                        getString(R.string.VerifyPin_continueBody), true, false, new BRAuthCompletion() {
                            @Override
                            public void onComplete() {
                                String extraDoneAction = getIntent().getExtras() == null
                                        ? null
                                        : getIntent().getStringExtra(PaperKeyProveActivity.EXTRA_DONE_ACTION);
                                PostAuth.getInstance().onPhraseCheckAuth(WriteDownActivity.this, false, extraDoneAction);
                            }

                            @Override
                            public void onCancel() {

                            }
                        });
            }
        });
    }

    @Override
    public void onBackPressed() {
        super.onBackPressed();
        overridePendingTransition(R.anim.fade_up, R.anim.exit_to_bottom);
    }

}
