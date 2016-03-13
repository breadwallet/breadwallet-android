package com.breadwallet.presenter.fragments;

import android.app.Fragment;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.IntroActivity;
import com.breadwallet.tools.security.KeyStoreManager;
import com.breadwallet.wallet.BRWalletManager;

import java.text.Normalizer;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail on 9/15/15.
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
public class IntroNewWalletFragment extends Fragment {
    private static final String TAG = IntroNewWalletFragment.class.getName();
    public Button introGenerate;
    private BRWalletManager m;

    @Override
    public View onCreateView(LayoutInflater inflater,
                             ViewGroup container, Bundle savedInstanceState) {
        // The last two arguments ensure LayoutParams are inflated
        // properly.
        m = BRWalletManager.getInstance(getActivity());
        View rootView = inflater.inflate(R.layout.intro_fragment_new_wallet, container, false);
        introGenerate = (Button) rootView.findViewById(R.id.intro_new_wallet_generate);
        introGenerate.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                String phrase = m.generateRandomSeed();
                KeyStoreManager.putWalletCreationTime((int) (System.currentTimeMillis()/1000), getActivity());
                String normalizedPhrase = Normalizer.normalize(phrase, Normalizer.Form.NFKD);
                String pubKey = m.getMasterPubKey(normalizedPhrase);
                KeyStoreManager.putMasterPublicKey(pubKey, getActivity());
                Log.w(TAG, "The phrase from keystore is: " + KeyStoreManager.getKeyStoreString(getActivity()));
                ((IntroActivity) getActivity()).showWarningFragment();
            }
        });
        return rootView;
    }

}
