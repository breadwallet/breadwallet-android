package com.breadwallet.presenter.fragments;

import android.animation.ArgbEvaluator;
import android.animation.ValueAnimator;
import android.app.Activity;
import android.app.Fragment;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.LayoutRes;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.view.animation.OvershootInterpolator;
import android.widget.ArrayAdapter;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.SecurityCenterActivity;
import com.breadwallet.presenter.activities.settings.SettingsActivity;
import com.breadwallet.presenter.entities.BRMenuItem;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.wallet.BRWalletManager;

import java.util.ArrayList;
import java.util.List;

import static com.breadwallet.R.id.menu_listview;
import static com.breadwallet.presenter.fragments.FragmentSend.ANIMATION_DURATION;

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
    private static final String TAG = FragmentMenu.class.getName();

    public TextView mTitle;
    public ListView mListView;
    public RelativeLayout layout;
    public List<BRMenuItem> itemList;
    public LinearLayout signalLayout;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.

        View rootView = inflater.inflate(R.layout.fragment_menu, container, false);
        layout = (RelativeLayout) rootView.findViewById(R.id.layout);
        signalLayout = (LinearLayout) rootView.findViewById(R.id.signal_layout);
        layout.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if(!BRAnimator.isClickAllowed()) return;
                getActivity().onBackPressed();
            }
        });

        itemList = new ArrayList<>();
        itemList.add(new BRMenuItem("Security Center", R.drawable.ic_shield, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Security Center");
                getActivity().onBackPressed();
                Intent intent = new Intent(getActivity(), SecurityCenterActivity.class);
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_bottom, R.anim.exit_to_bottom);
            }
        }));
        itemList.add(new BRMenuItem("Support", R.drawable.ic_question_mark, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Support");
            }
        }));
        itemList.add(new BRMenuItem("Settings", R.drawable.ic_settings, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Settings");
                Intent intent = new Intent(getActivity(), SettingsActivity.class);
                getActivity().onBackPressed();
                startActivity(intent);
                getActivity().overridePendingTransition(R.anim.enter_from_bottom, 0);
            }
        }));
        itemList.add(new BRMenuItem("Lock Wallet", R.drawable.ic_lock, new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Log.e(TAG, "onClick: Lock Wallet");
                final Activity from = getActivity();
                from.getFragmentManager().popBackStack();
                BRAnimator.showBreadSignal(getActivity(), "Wallet Locked", "Wallet Locked", R.drawable.ic_wallet_locked);
                new Handler().postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        BRWalletManager.getInstance().startBreadActivity(from, true);
                    }
                }, 1000);
            }
        }));

        mTitle = (TextView) rootView.findViewById(R.id.title);
        mListView = (ListView) rootView.findViewById(menu_listview);
        mListView.setAdapter(new MenuListAdapter(getContext(), R.layout.menu_list_item, itemList));

        return rootView;
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        final ViewTreeObserver observer = mListView.getViewTreeObserver();
        observer.addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
            @Override
            public void onGlobalLayout() {
                observer.removeGlobalOnLayoutListener(this);
                animateBackgroundDim();
                animateSignalSlide();
            }
        });
    }

    private void animateSignalSlide() {
        float translationY = signalLayout.getTranslationY();
        float signalHeight = signalLayout.getHeight();
        signalLayout.setTranslationY(translationY + signalHeight);
        signalLayout.animate().translationY(translationY).setDuration(ANIMATION_DURATION).setInterpolator(new OvershootInterpolator(0.7f));
    }

    private void animateBackgroundDim() {
        int transColor = android.R.color.transparent;
        int blackTransColor = R.color.black_trans;

        ValueAnimator anim = new ValueAnimator();
        anim.setIntValues(transColor, blackTransColor);
        anim.setEvaluator(new ArgbEvaluator());
        anim.addUpdateListener(new ValueAnimator.AnimatorUpdateListener() {
            @Override
            public void onAnimationUpdate(ValueAnimator valueAnimator) {
                layout.setBackgroundColor((Integer) valueAnimator.getAnimatedValue());
            }
        });

        anim.setDuration(ANIMATION_DURATION);
        anim.start();
    }

    public RelativeLayout getMainLayout() {
        return layout;
    }

    public class MenuListAdapter extends ArrayAdapter<BRMenuItem> {

        private List<BRMenuItem> items;
        private Context mContext;
        private int defaultLayoutResource = R.layout.menu_list_item;

        public MenuListAdapter(@NonNull Context context, @LayoutRes int resource, @NonNull List<BRMenuItem> items) {
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
            TextView text = (TextView) convertView.findViewById(R.id.item_text);
            ImageView icon = (ImageView) convertView.findViewById(R.id.item_icon);

            text.setText(items.get(position).text);
            icon.setImageResource(items.get(position).resId);
            convertView.setOnClickListener(items.get(position).listener);
//            applyBlur();
            return convertView;

        }

        @Override
        public int getCount() {
            return items == null ? 0 : items.size();
        }
    }


//    private void applyBlur() {
//        layout.getViewTreeObserver().addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
//            @Override
//            public boolean onPreDraw() {
//                layout.getViewTreeObserver().removeOnPreDrawListener(this);
//                layout.buildDrawingCache();
//
//                Bitmap bmp = layout.getDrawingCache();
//                blur(bmp, layout);
//                return true;
//            }
//        });
//    }
//
//    @TargetApi(Build.VERSION_CODES.JELLY_BEAN_MR1)
//    private void blur(Bitmap bkg, View view) {
//        long startMs = System.currentTimeMillis();
//
//        float radius = 20;
//
//        Bitmap overlay = Bitmap.createBitmap((int) (view.getMeasuredWidth()),
//                (int) (view.getMeasuredHeight()), Bitmap.Config.ARGB_8888);
//
//        Canvas canvas = new Canvas(overlay);
//
//        canvas.translate(-view.getLeft(), -view.getTop());
//        canvas.drawBitmap(bkg, 0, 0, null);
//
//        RenderScript rs = RenderScript.create(getActivity());
//
//        Allocation overlayAlloc = Allocation.createFromBitmap(
//                rs, overlay);
//
//        ScriptIntrinsicBlur blur = ScriptIntrinsicBlur.create(
//                rs, overlayAlloc.getElement());
//
//        blur.setInput(overlayAlloc);
//
//        blur.setRadius(radius);
//
//        blur.forEach(overlayAlloc);
//
//        overlayAlloc.copyTo(overlay);
//
//        view.setBackground(new BitmapDrawable(
//                getResources(), overlay));
//
//        rs.destroy();
////        statusText.setText(System.currentTimeMillis() - startMs + "ms");
//    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
//        animateBackgroundDarker(true);
    }


}