package com.breadwallet.tools.adapter;

import android.app.Activity;
import android.content.Context;
import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRText;
import com.breadwallet.presenter.customviews.TokenIconView;
import com.breadwallet.presenter.entities.TokenItem;

import java.util.ArrayList;

public class AddWalletListAdapter extends RecyclerView.Adapter<AddWalletListAdapter.TokenItemViewHolder> {

    private Context mContext;
    private ArrayList<TokenItem> mTokens;
    private static final String TAG = AddWalletListAdapter.class.getSimpleName();

    public AddWalletListAdapter(Context context, ArrayList<TokenItem> tokens) {

        this.mContext = context;
        this.mTokens = tokens;
    }


    @Override
    public void onBindViewHolder(@NonNull AddWalletListAdapter.TokenItemViewHolder holder, int position) {

        holder.name.setText(mTokens.get(position).name);
        holder.symbol.setText(mTokens.get(position).symbol);


    }

    @Override
    public int getItemCount() {
        return mTokens.size();
    }

    @NonNull
    @Override
    public AddWalletListAdapter.TokenItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {

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
        }
    }


}
