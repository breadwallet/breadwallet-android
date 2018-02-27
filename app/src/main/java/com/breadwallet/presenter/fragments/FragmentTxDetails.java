package com.breadwallet.presenter.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.Utils;
import com.breadwallet.wallet.WalletsMaster;

import java.math.BigDecimal;

/**
 * Created by byfieldj on 2/26/18.
 * <p>
 * Reusable dialog fragment that display details about a particular transaction
 */

public class FragmentTxDetails extends DialogFragment {

    private static final String EXTRA_TX_ITEM = "tx_item";

    private TxUiHolder mTransaction;

    private BRText mTxAction;
    private BRText mTxAmount;
    private BRText mPriceStamp;
    private BRText mTxStatus;
    private BRText mTxDate;
    private BRText mToFrom;
    private BRText mToFromAddress;
    private BRText mMemoText;

    private BRText mStartingBalance;
    private BRText mEndingBalance;
    private BRText mExchangeRate;
    private BRText mConfirmedInBlock;
    private BRText mTransactionId;
    private BRText mShowHide;

    private ImageButton mCloseButton;
    private RelativeLayout mDetailsContainer;

    boolean mDetailsShowing = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setStyle(STYLE_NO_TITLE, 0);

    }

    @Override
    public Dialog onCreateDialog(Bundle savedInstanceState) {
        return super.onCreateDialog(savedInstanceState);
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.transaction_details, container, false);

        mTxAction = rootView.findViewById(R.id.tx_action);
        mTxAmount = rootView.findViewById(R.id.tx_amount);
        mPriceStamp = rootView.findViewById(R.id.tx_price_stamp);
        mTxStatus = rootView.findViewById(R.id.tx_status);
        mTxDate = rootView.findViewById(R.id.tx_date);
        mToFrom = rootView.findViewById(R.id.tx_to_from);
        mToFromAddress = rootView.findViewById(R.id.tx_to_from_address);
        mMemoText = rootView.findViewById(R.id.memo);
        mStartingBalance = rootView.findViewById(R.id.tx_starting_balance);
        mEndingBalance = rootView.findViewById(R.id.tx_ending_balance);
        mExchangeRate = rootView.findViewById(R.id.exchange_rate);
        mConfirmedInBlock = rootView.findViewById(R.id.confirmed_in_block_number);
        mTransactionId = rootView.findViewById(R.id.transaction_id);
        mShowHide = rootView.findViewById(R.id.show_hide_details);
        mDetailsContainer = rootView.findViewById(R.id.details_container);
        mCloseButton = rootView.findViewById(R.id.close_button);

        mCloseButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                dismiss();
            }
        });

        mShowHide.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (!mDetailsShowing) {
                    mDetailsContainer.setVisibility(View.VISIBLE);
                    mDetailsShowing = true;
                    mShowHide.setText("Hide Details");
                } else {
                    mDetailsContainer.setVisibility(View.GONE);
                    mDetailsShowing = false;
                    mShowHide.setText("Show Details");
                }
            }
        });

        updateUi();
        return rootView;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
    }

    public void setTransaction(TxUiHolder item) {

        this.mTransaction = item;

    }

    private void updateUi() {

        // Set mTransction fields
        if (mTransaction != null) {

            if (!mTransaction.isValid()) {
                mTxStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            String currentIso = BRSharedPrefs.getCurrentWalletIso(getActivity());
            WalletsMaster master = WalletsMaster.getInstance(getActivity());
            String iso = BRSharedPrefs.isCryptoPreferred(getActivity()) ? currentIso : BRSharedPrefs.getPreferredFiatIso(getContext());



            BigDecimal txAmount = new BigDecimal(mTransaction.getAmount()).abs();
            String formattedAmount = CurrencyUtils.getFormattedAmount(getActivity(), iso, master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), mTransaction.getFee() == -1 ? txAmount : txAmount.subtract(new BigDecimal(mTransaction.getFee()))));

            boolean sent = mTransaction.getReceived() - mTransaction.getSent() < 0;


            String amountWithFee = CurrencyUtils.getFormattedAmount(getActivity(), iso, master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), txAmount));

            String startingBalance = CurrencyUtils.getFormattedAmount(getActivity(), iso, master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), new BigDecimal(sent ? mTransaction.getBalanceAfterTx() + txAmount.longValue() : mTransaction.getBalanceAfterTx() - txAmount.longValue())));
            mStartingBalance.setText(startingBalance);

            String endingBalance = CurrencyUtils.getFormattedAmount(getActivity(), iso, master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), new BigDecimal(mTransaction.getBalanceAfterTx())));
            mEndingBalance.setText(endingBalance);

            if (sent) {
                mTxAction.setText("Sent");
                mToFrom.setText("To " + mTransaction.getTo()[0]);
            } else {
                mTxAction.setText("Received");
                mToFrom.setText("From " + mTransaction.getFrom()[0]);
            }

            mTxAmount.setText(formattedAmount + iso.toUpperCase());

            mTxDate.setText(BRDateUtil.getShortDate(mTransaction.getTimeStamp() * 1000));
            mTransactionId.setText(mTransaction.getTxHashHexReversed());
            mConfirmedInBlock.setText("" + mTransaction.getBlockHeight());
        } else {

            Toast.makeText(getContext(), "Error getting transaction data", Toast.LENGTH_SHORT).show();
        }


    }

    @Override
    public void onResume() {

//        ViewGroup.LayoutParams params = getDialog().getWindow().getAttributes();
//        params.width = Utils.getPixelsFromDps(getContext(), 350);
//        params.height = RelativeLayout.LayoutParams.WRAP_CONTENT;
//        getDialog().getWindow().setAttributes((android.view.WindowManager.LayoutParams) params);

        super.onResume();

        //getDialog().getWindow().setLayout(Utils.getPixelsFromDps(getContext(), 400),Utils.getPixelsFromDps(getContext(), 350));


    }
}
