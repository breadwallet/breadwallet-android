package com.breadwallet.presenter.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.util.Log;
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
import com.platform.entities.TxMetaData;
import com.platform.tools.KVStoreManager;

import java.math.BigDecimal;

/**
 * Created by byfieldj on 2/26/18.
 * <p>
 * Reusable dialog fragment that display details about a particular transaction
 */

public class FragmentTxDetails extends DialogFragment {

    private static final String EXTRA_TX_ITEM = "tx_item";
    private static final String TAG = "FragmentTxDetails";

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

            boolean cryptoPreferred = BRSharedPrefs.isCryptoPreferred(getActivity());
            boolean sent = mTransaction.getReceived() - mTransaction.getSent() < 0;
            String amountWhenSent;
            String amountNow;
            String formattedAmount;
            String startingBalance;
            String endingBalance;
            String exchangeRateFormatted;


            if (!mTransaction.isValid()) {
                mTxStatus.setCompoundDrawablesWithIntrinsicBounds(0, 0, 0, 0);
            }
            String currentIso = BRSharedPrefs.getCurrentWalletIso(getActivity());
            String cryptoSymbol = CurrencyUtils.getSymbolByIso(getActivity(), currentIso);
            WalletsMaster master = WalletsMaster.getInstance(getActivity());
            String iso = BRSharedPrefs.isCryptoPreferred(getActivity()) ? currentIso : BRSharedPrefs.getPreferredFiatIso(getContext());


            BigDecimal txAmount = new BigDecimal(mTransaction.getAmount()).abs();
            long satoshisAmount = sent ? mTransaction.getSent() : mTransaction.getReceived();
            Log.d(TAG, "Satoshis Amount -> " + satoshisAmount);


            if (!cryptoPreferred) {
                startingBalance = CurrencyUtils.getFormattedAmount(getActivity(), iso, master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), new BigDecimal(sent ? mTransaction.getBalanceAfterTx() + txAmount.longValue() + mTransaction.getFee() : mTransaction.getBalanceAfterTx() - txAmount.longValue())));
                mStartingBalance.setText(startingBalance);
            } else {
                startingBalance = CurrencyUtils.getFormattedAmount(getActivity(), iso, new BigDecimal(sent ? mTransaction.getBalanceAfterTx() + txAmount.longValue() - mTransaction.getFee() : mTransaction.getBalanceAfterTx() - txAmount.longValue()));
                mStartingBalance.setText(startingBalance);
            }

            if (!cryptoPreferred) {
                endingBalance = CurrencyUtils.getFormattedAmount(getActivity(), iso, master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), new BigDecimal(mTransaction.getBalanceAfterTx())));
                mEndingBalance.setText(endingBalance);
            } else {

                endingBalance = CurrencyUtils.getFormattedAmount(getActivity(), iso, new BigDecimal(mTransaction.getBalanceAfterTx()));
                mEndingBalance.setText(endingBalance);
            }


            if (sent) {
                mTxAction.setText("Sent");
                mToFrom.setText("To ");
                mToFromAddress.setText(mTransaction.getTo()[0]);
            } else {
                mTxAction.setText("Received");
                mToFrom.setText("Via ");
                mToFromAddress.setText(mTransaction.getFrom()[0]);
            }


            formattedAmount = CurrencyUtils.getFormattedAmount(getActivity(), currentIso, master.getCurrentWallet(getActivity()).getCryptoForFiat(getActivity(), new BigDecimal(sent ? mTransaction.getSent() : mTransaction.getReceived())));
            mTxAmount.setText(formattedAmount);

            amountNow = CurrencyUtils.getFormattedAmount(getActivity(), iso, master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), new BigDecimal(sent ? mTransaction.getSent() : mTransaction.getReceived())));
            mPriceStamp.setText("X when sent " + amountNow + " now");


            if (!sent) {
                mTxAmount.setTextColor(getContext().getColor(R.color.transaction_amount_received_color));
            }

            // Set the memo text if one is available
            String memo;
            TxMetaData txMetaData = KVStoreManager.getInstance().getTxMetaData(getActivity(), mTransaction.getTxHash());

            if (txMetaData != null) {
                Log.d(TAG, "TxMetaData not null");
                if (txMetaData.comment != null) {
                    Log.d(TAG, "Comment not null");
                    memo = txMetaData.comment;
                    mMemoText.setText(memo);
                } else {
                    Log.d(TAG, "Comment is null");
                    mMemoText.setText("");
                }

                exchangeRateFormatted = CurrencyUtils.getFormattedAmount(getActivity(), iso, master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), new BigDecimal(txMetaData.exchangeRate)));
                mExchangeRate.setText(exchangeRateFormatted);
            } else {
                Log.d(TAG, "TxMetaData is null");
                mMemoText.setText("");

            }

            // Set the transaction date
            mTxDate.setText(BRDateUtil.getLongDate(mTransaction.getTimeStamp() * 1000));

            // Set the transaction id
            mTransactionId.setText(mTransaction.getTxHashHexReversed());

            // Set the transaction block number
            mConfirmedInBlock.setText("" + mTransaction.getBlockHeight());


        } else {

            Toast.makeText(getContext(), "Error getting transaction data", Toast.LENGTH_SHORT).show();
        }

        RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.WRAP_CONTENT, ViewGroup.LayoutParams.WRAP_CONTENT);
        params.setMargins(0,0,0, Utils.getPixelsFromDps(getContext(), 12));
        params.addRule(RelativeLayout.CENTER_HORIZONTAL);
        params.addRule(RelativeLayout.ALIGN_PARENT_BOTTOM);
        mShowHide.setLayoutParams(params);

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
