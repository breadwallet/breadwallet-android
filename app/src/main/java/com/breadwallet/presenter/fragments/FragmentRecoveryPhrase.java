
package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import android.widget.Toast;

import com.breadwallet.R;
import com.breadwallet.presenter.BreadWalletApp;
import com.breadwallet.tools.BRClipboardManager;
import com.breadwallet.tools.adapter.MiddleViewAdapter;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 7/22/15.
 * Copyright (c) 2015 Mihail Gutan <mihail@breadwallet.com>
 * <p/>
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 * <p/>
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 * <p/>
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */

public class FragmentRecoveryPhrase extends Fragment {
    public static final String TAG = FragmentRecoveryPhrase.class.getName();
    private TextView thePhrase;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View rootView = inflater.inflate(
                R.layout.fragment_recovery_phrase, container, false);
        BRWalletManager m = BRWalletManager.getInstance(getActivity());
        thePhrase = (TextView) rootView.findViewById(R.id.the_phrase);

        //TODO delete this code below which is for testing reasons only
        thePhrase.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                BRClipboardManager.copyToClipboard(getActivity(),thePhrase.getText().toString());
                ((BreadWalletApp)getActivity().getApplication()).showCustomToast(getActivity(),
                        getString(R.string.copied), 300, Toast.LENGTH_SHORT,0);
            }
        });

        final long startTime = System.currentTimeMillis();
        //return the new method if the API is 23+

        thePhrase.setText(KeyStoreManager.getKeyStoreString(getActivity()));
        return rootView;
    }

    @Override
    public void onResume() {
        super.onResume();
        MiddleViewAdapter.resetMiddleView(getActivity(),null);
    }
}
