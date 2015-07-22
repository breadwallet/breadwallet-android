package com.breadwallet.presenter.fragments.allsettings.settings;

import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.presenter.activities.MainActivity;
import com.breadwallet.tools.animation.FragmentAnimator;
import com.breadwallet.tools.others.CurrencyManager;

/**
 * Created by Mihail on 7/14/15.
 */
public class FragmentCurrency extends Fragment {
    public static final String TAG = "FragmentCurrency";
    public static final String CURRENT_CURRENCY = "currentCurrency";
    private ListView currencyList;
    private MainActivity app;
    private Button currencyRefresh;
    private TextView noInternetConnection;
    private ArrayAdapter adapter;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        View rootView = inflater.inflate(
                R.layout.fragment_local_currency, container, false);
        app = MainActivity.getApp();
        currencyList = (ListView) rootView.findViewById(R.id.currency_list_view);
        currencyRefresh = (Button) rootView.findViewById(R.id.currencyRefresh);
        noInternetConnection = (TextView) rootView.findViewById(R.id.noInternetConnectionText);

        currencyRefresh.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                adapter = CurrencyManager.getCurrencyAddapterIfReady();
                tryAndSetAdapter();
            }
        });
        adapter = CurrencyManager.getCurrencyAddapterIfReady();
        tryAndSetAdapter();
        currencyList.setOnItemClickListener(new AdapterView.OnItemClickListener() {
            @Override
            public void onItemClick(AdapterView<?> parent, View view,
                                    int position, long id) {
                TextView tmp = (TextView) view.findViewById(R.id.currency_item_text);
                final String selectedCurrency = tmp.getText().toString();
                SharedPreferences settings = getActivity().getSharedPreferences(MainActivity.PREFS_NAME, 0);
                SharedPreferences.Editor editor = settings.edit();
                editor.putString(CURRENT_CURRENCY, selectedCurrency.substring(0, 3));
                editor.commit();
                FragmentAnimator.animateSlideToRight(app);
                Log.d(TAG, "Selected item's text: " + selectedCurrency);
            }
        });

        return rootView;
    }

    public void tryAndSetAdapter() {
        if (adapter.getCount() > 0) {
            currencyList.setAdapter(adapter);
            currencyRefresh.setVisibility(View.GONE);
            noInternetConnection.setVisibility(View.GONE);
        } else {
            ((BreadWalletApp) app.getApplicationContext()).showCustomToast(getActivity(), "No internet connection", 500, Toast.LENGTH_SHORT);
            currencyRefresh.setVisibility(View.VISIBLE);
            noInternetConnection.setVisibility(View.VISIBLE);
        }
    }

}
