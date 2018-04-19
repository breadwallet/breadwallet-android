package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.graphics.Typeface;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.RelativeLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.presenter.customviews.TokenIconView;
import com.breadwallet.presenter.entities.TokenItem;
import com.breadwallet.tools.util.Utils;

import java.util.ArrayList;

public class AddTokenListAdapter extends RecyclerView.Adapter<AddTokenListAdapter.TokenItemViewHolder> {

    private Context mContext;
    private ArrayList<TokenItem> mTokens;
    private static final String TAG = AddTokenListAdapter.class.getSimpleName();
    private OnTokenAddOrRemovedListener mListener;

    public AddTokenListAdapter(Context context, ArrayList<TokenItem> tokens, OnTokenAddOrRemovedListener listener) {

        this.mContext = context;
        this.mTokens = tokens;
        this.mListener = listener;
    }

    public interface OnTokenAddOrRemovedListener {

        void onTokenAdded(TokenItem token);

        void onTokenRemoved(TokenItem token);
    }


    @Override
    public void onBindViewHolder(final @NonNull AddTokenListAdapter.TokenItemViewHolder holder, final int position) {


        holder.name.setText(mTokens.get(position).name);
        holder.symbol.setText(mTokens.get(position).symbol);
        holder.addRemoveButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                // TODO: Add logic to change add/remove button states and update
                if (!mTokens.get(position).isAdded) {
                    mTokens.get(position).isAdded = true;
                    holder.addRemoveButton.setText("Remove");
                    holder.addRemoveButton.setBackground(mContext.getDrawable(R.drawable.remove_wallet_button));
                    holder.addRemoveButton.setTextColor(mContext.getColor(R.color.red));

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.width = Utils.getPixelsFromDps(mContext, 55);
                    params.height = Utils.getPixelsFromDps(mContext, 30);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, holder.addRemoveButton.getId());
                    params.addRule(RelativeLayout.CENTER_VERTICAL, holder.addRemoveButton.getId());
                    params.setMarginEnd(Utils.getPixelsFromDps(mContext, 12));
                    holder.addRemoveButton.setLayoutParams(params);
                    mListener.onTokenAdded(mTokens.get(position));
                } else {
                    mTokens.get(position).isAdded = false;

                    holder.addRemoveButton.setText("Add");
                    holder.addRemoveButton.setBackground(mContext.getDrawable(R.drawable.add_wallet_button));
                    holder.addRemoveButton.setTextColor(mContext.getColor(R.color.dialog_button_positive));

                    RelativeLayout.LayoutParams params = new RelativeLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
                    params.width = Utils.getPixelsFromDps(mContext, 40);
                    params.height = Utils.getPixelsFromDps(mContext, 30);
                    params.addRule(RelativeLayout.ALIGN_PARENT_END, holder.addRemoveButton.getId());
                    params.addRule(RelativeLayout.CENTER_VERTICAL, holder.addRemoveButton.getId());
                    params.setMarginEnd(Utils.getPixelsFromDps(mContext, 12));
                    holder.addRemoveButton.setLayoutParams(params);
                    mListener.onTokenRemoved(mTokens.get(position));


                }


            }
        });


    }

    @Override
    public int getItemCount() {
        return mTokens.size();
    }

    @NonNull
    @Override
    public AddTokenListAdapter.TokenItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

        LayoutInflater inflater = ((Activity) mContext).getLayoutInflater();
        View convertView = inflater.inflate(R.layout.token_list_item, parent, false);


        return new TokenItemViewHolder(convertView);
    }

    public class TokenItemViewHolder extends RecyclerView.ViewHolder {

        private TokenIconView logo;
        private BRText symbol;
        private BRText name;
        private Button addRemoveButton;

        public TokenItemViewHolder(View view) {
            super(view);

            logo = view.findViewById(R.id.token_icon);
            symbol = view.findViewById(R.id.token_ticker);
            name = view.findViewById(R.id.token_name);
            addRemoveButton = view.findViewById(R.id.add_remove_button);

            Typeface typeface = Typeface.createFromAsset(mContext.getAssets(), "fonts/CircularPro-Book.otf");
            addRemoveButton.setTypeface(typeface);
        }
    }


}
