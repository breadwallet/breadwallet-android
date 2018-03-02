package com.breadwallet.presenter.fragments;

import android.app.Dialog;
import android.app.DialogFragment;
import android.content.Context;
import android.graphics.Typeface;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.text.Spannable;
import android.text.SpannableStringBuilder;
import android.text.Spanned;
import android.text.style.TypefaceSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.RelativeLayout;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.presenter.entities.CurrencyEntity;
import com.breadwallet.presenter.entities.TxUiHolder;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.sqlite.CurrencyDataSource;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.BRDateUtil;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.CustomTypefaceSpan;
import com.breadwallet.wallet.WalletsMaster;
import com.breadwallet.wallet.abstracts.BaseWalletManager;
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
    private BRText mAmountWhenSent;
    private BRText mAmountNow;

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
        mAmountWhenSent = rootView.findViewById(R.id.tx_amount_when_sent);
        mAmountNow = rootView.findViewById(R.id.tx_amount_now);

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
            } else {
                mTxAction.setText("Received");
                mToFrom.setText("Via ");
            }
            mToFromAddress.setText(mTransaction.getTo()[0]);

            formattedAmount = CurrencyUtils.getFormattedAmount(getActivity(), currentIso, master.getCurrentWallet(getActivity()).getCryptoForFiat(getActivity(), new BigDecimal(sent ? mTransaction.getSent() : mTransaction.getReceived())));
            mTxAmount.setText(formattedAmount);


            String whenSent = " when sent ";
            String now = " now";
            amountWhenSent = CurrencyUtils.getFormattedAmount(getActivity(), BRSharedPrefs.getPreferredFiatIso(getContext()), master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), new BigDecimal(sent ? mTransaction.getSent() : mTransaction.getReceived())));
            amountNow = CurrencyUtils.getFormattedAmount(getActivity(), BRSharedPrefs.getPreferredFiatIso(getContext()), master.getCurrentWallet(getActivity()).getFiatForSmallestCrypto(getActivity(), new BigDecimal(sent ? mTransaction.getSent() : mTransaction.getReceived())));

            /*String finalString = amountWhenSent + whenSent;

            // Make the amount when sent, and amount now values CircularPro-Bold Typeface
            Typeface cirularProTypeFace = Typeface.createFromAsset(getActivity().getAssets(), "fonts/CircularPro-Bold.otf");
            TypefaceSpan customTypefaceSpan = new CustomTypefaceSpan("", cirularProTypeFace);

            SpannableStringBuilder tx1 = new SpannableStringBuilder();
            //tx1.setSpan(customTypefaceSpan, 0, finalString.indexOf(amountWhenSent) + amountWhenSent.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);

            tx1.append(amountWhenSent, customTypefaceSpan, Spanned.SPAN_INCLUSIVE_INCLUSIVE).append(whenSent).append(amountNow, customTypefaceSpan, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE).append(now);

            //SpannableStringBuilder tx2 = new SpannableStringBuilder(amountNow);
            //tx1.setSpan(customTypefaceSpan, finalString.indexOf(amountNow), finalString.indexOf(amountNow) + amountNow.length(), Spannable.SPAN_INCLUSIVE_INCLUSIVE);*/

            mAmountWhenSent.setText(amountWhenSent);
            mAmountNow.setText(amountNow);


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

                // TODO: Need to check if exchange rate is properly stored in TxMetaData
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


    }

    private BigDecimal getHistoricalTxAmount(BaseWalletManager wallet, String iso, BigDecimal amount) {
        CurrencyEntity ent = CurrencyDataSource.getInstance(getContext()).getCurrencyByCode(getContext(), wallet, iso);
        if (ent == null) return null;
        double rate = ent.rate;
        //get crypto amount (since the rate is in BTC not satoshis)
        BigDecimal cryptoAmount = amount.divide(new BigDecimal(100000000), 8, BRConstants.ROUNDING_MODE);
        return cryptoAmount.multiply(new BigDecimal(rate));


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
