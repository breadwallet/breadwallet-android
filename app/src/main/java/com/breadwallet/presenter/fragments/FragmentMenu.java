package com.breadwallet.presenter.fragments;

import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import androidx.annotation.LayoutRes;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.browser.customtabs.CustomTabsIntent;
import androidx.constraintlayout.widget.ConstraintLayout;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.SecurityCenterActivity;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.activities.settings.WebViewActivity;
import com.breadwallet.presenter.entities.BRMenuItem;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.SlideDetector;
import com.breadwallet.tools.util.BRConstants;
import com.platform.APIClient;
import com.platform.HTTPServer;

import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p>
 * Created by Mihail Gutan <mihail@breadwallet.com> on 6/29/15.
 * Copyright (c) 2016 breadwallet LLC
 * <p>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentMenu extends Fragment {

    public TextView mTitle;
    public ListView mListView;
    public RelativeLayout background;
    public List<BRMenuItem> itemList;
    public ConstraintLayout signalLayout;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {

        View rootView = inflater.inflate(R.layout.fragment_menu, container, false);
        background = rootView.findViewById(R.id.layout);
        signalLayout = rootView.findViewById(R.id.signal_layout);
        background.setOnClickListener(v -> {
            if (!BRAnimator.isClickAllowed()) return;
            getActivity().onBackPressed();
        });

        itemList = new ArrayList<>();
        boolean buyBitcoinEnabled = APIClient.getInstance(getActivity()).isFeatureEnabled(APIClient.FeatureFlags.BUY_BITCOIN.toString());
        if (buyBitcoinEnabled) {
            itemList.add(new BRMenuItem(getString(R.string.MenuButton_buy), R.drawable.buy_bitcoin, v -> {
                Intent intent = new Intent(getActivity(), WebViewActivity.class);
                intent.putExtra(WebViewActivity.URL_EXTRA, HTTPServer.URL_BUY);
                launchActivity(intent);
            }));
        }

        itemList.add(new BRMenuItem(getString(R.string.MenuButton_security), R.drawable.ic_shield, v -> {
            Intent intent = new Intent(getActivity(), SecurityCenterActivity.class);
            launchActivity(intent);
        }));

        itemList.add(new BRMenuItem(getString(R.string.MenuButton_support), R.drawable.faq_question_black, v -> {
            CustomTabsIntent.Builder builder = new CustomTabsIntent.Builder();
            CustomTabsIntent customTabsIntent = builder.build();
            customTabsIntent.launchUrl(getContext(), Uri.parse(BRConstants.CUSTOMER_SUPPORT_LINK));
        }));

        itemList.add(new BRMenuItem(getString(R.string.MenuButton_settings), R.drawable.ic_settings, v -> {
            Intent intent = new Intent(getActivity(), SettingsActivity.class);
            launchActivity(intent);
        }));

        itemList.add(new BRMenuItem(getString(R.string.MenuButton_lock), R.drawable.ic_lock, v -> {
            final Activity from = getActivity();
            from.getFragmentManager().popBackStack();
            BRAnimator.startBreadActivity(from, true);
        }));

        rootView.findViewById(R.id.close_button).setOnClickListener(v -> {
            Activity app = getActivity();
            app.getFragmentManager().popBackStack();
        });

        mTitle = rootView.findViewById(R.id.title);
        mListView = rootView.findViewById(R.id.menu_listview);
        mListView.setAdapter(new MenuListAdapter(getContext(), R.layout.menu_list_item, itemList));
        signalLayout.setOnTouchListener(new SlideDetector(getContext(), signalLayout));

        return rootView;
    }

    private void launchActivity(Intent intent) {
        Activity app = getActivity();
        app.startActivity(intent);
        app.overridePendingTransition(R.anim.enter_from_bottom, R.anim.fade_down);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = mListView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                if (observer.isAlive()) {
                    observer.removeOnGlobalLayoutListener(this);
                }
                BRAnimator.animateBackgroundDim(background, false);
                BRAnimator.animateSignalSlide(signalLayout, false, null);
            }
        });
    }

    public static class MenuListAdapter extends ArrayAdapter<BRMenuItem> {

        private final LayoutInflater mInflater;

        public MenuListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<BRMenuItem> items) {
            super(context, resource, items);
            mInflater = ((Activity) context).getLayoutInflater();
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {
            if (convertView == null) {
                convertView = mInflater.inflate(R.layout.menu_list_item, parent, false);
            }
            TextView text = convertView.findViewById(R.id.item_text);
            ImageView icon = convertView.findViewById(R.id.item_icon);

            final BRMenuItem item = getItem(position);
            text.setText(item.text);
            icon.setImageResource(item.resId);
            convertView.setOnClickListener(item.listener);
            return convertView;
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        BRAnimator.animateBackgroundDim(background, true);
        BRAnimator.animateSignalSlide(signalLayout, true, () -> {
            if (getActivity() != null) {
                getActivity().getFragmentManager().popBackStack();
            }
        });
    }
}