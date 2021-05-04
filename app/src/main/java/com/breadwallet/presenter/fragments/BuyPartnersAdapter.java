package com.breadwallet.presenter.fragments;

import android.content.Context;
import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.HorizontalScrollView;
import android.widget.ImageView;
import android.widget.RadioGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.StringRes;
import androidx.browser.customtabs.CustomTabColorSchemeParams;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.fragment.app.FragmentActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.breadwallet.R;
import com.breadwallet.presenter.entities.Partner;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.util.BRConstants;

import java.util.List;

/**
 * Litewallet
 * Created by sadia on 2020-January-27
 * email: mosadialiou@gmail.com
 */
class BuyPartnersAdapter extends RecyclerView.Adapter<BuyPartnersAdapter.PartnerViewHolder> {

    private final LayoutInflater inflater;
    private List<Partner> partners;

    BuyPartnersAdapter(Context context, @NonNull List<Partner> partners) {
        inflater = LayoutInflater.from(context);
        this.partners = partners;
    }

    @NonNull
    @Override
    public PartnerViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new PartnerViewHolder(inflater.inflate(R.layout.buy_partner_item, parent, false));
    }

    @Override
    public void onBindViewHolder(@NonNull final PartnerViewHolder holder, int position) {
        final Partner partner = partners.get(position);
        holder.logo.setImageResource(partner.getLogo());
        holder.title.setText(partner.getTitle());
        holder.detail.setText(partner.getDetails());
        holder.fiatOptionHScrollView.post(() -> {
            int checkId = holder.fiatOptions.getCheckedRadioButtonId();
            View option = holder.fiatOptions.findViewById(checkId);
            holder.fiatOptionHScrollView.scrollTo((int) option.getX(), (int) option.getY());
        });

        holder.buyPartnerWrapper.setOnClickListener(v -> {
            int currencyResId = getCurrencyResId(holder.fiatOptions.getCheckedRadioButtonId());
            final Context context = v.getContext();
            String currency = context.getString(currencyResId);
            if (partner.getCode() == FragmentBuy.Partner.MOONPAY) {
                CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder()
                        .setDefaultColorSchemeParams(new CustomTabColorSchemeParams.Builder().setToolbarColor(context.getColor(R.color.litecoin_litewallet_blue)).build())
                        .setUrlBarHidingEnabled(true);
                CustomTabsIntent customTabsIntent = builder.build();
                String buyUrl = FragmentBuy.url(context, partner.getCode(), currency);
                customTabsIntent.launchUrl(context, Uri.parse(buyUrl));
            } else {
                BRAnimator.showBuyFragment((FragmentActivity) context, currency, partner.getCode());
            }
        });
    }

    @StringRes
    private int getCurrencyResId(int checkedOption) {
        int currency;
        switch (checkedOption) {
            case R.id.cad_fiat:
                currency = R.string.cad_currency_code;
                break;

            case R.id.eur_fiat:
                currency = R.string.eur_currency_code;
                break;

            case R.id.jpy_fiat:
                currency = R.string.jpy_currency_code;
                break;

            case R.id.aud_fiat:
                currency = R.string.aud_currency_code;
                break;

            case R.id.gbp_fiat:
                currency = R.string.gbp_currency_code;
                break;

            case R.id.hkd_fiat:
                currency = R.string.hkd_currency_code;
                break;

            case R.id.idr_fiat:
                currency = R.string.idr_currency_code;
                break;

            case R.id.rub_fiat:
                currency = R.string.rub_currency_code;
                break;

            case R.id.sgd_fiat:
                currency = R.string.sgd_currency_code;
                break;

            default:
                currency = R.string.usd_currency_code;
                break;
        }
        return currency;
    }

    @Override
    public int getItemCount() {
        return partners.size();
    }

    static class PartnerViewHolder extends RecyclerView.ViewHolder {

        final ImageView logo;
        final TextView title;
        final TextView detail;
        final RadioGroup fiatOptions;
        final View buyPartnerWrapper;
        final HorizontalScrollView fiatOptionHScrollView;

        PartnerViewHolder(@NonNull View itemView) {
            super(itemView);

            logo = itemView.findViewById(R.id.logo);
            title = itemView.findViewById(R.id.titleLbl);
            detail = itemView.findViewById(R.id.detailLbl);
            fiatOptions = itemView.findViewById(R.id.fiat_option);
            fiatOptionHScrollView = itemView.findViewById(R.id.fiat_option_h_scroll);
            buyPartnerWrapper = itemView.findViewById(R.id.buyPartnerWrapper);
        }
    }
}
