package com.breadwallet.tools.adapter;

import android.app.Fragment;
import android.app.FragmentManager;
import android.support.v13.app.FragmentPagerAdapter;

import com.breadwallet.presenter.entities.TxItem;
import com.breadwallet.presenter.fragments.FragmentTransactionItem;

import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 4/11/17.
 * Copyright (c) 2017 breadwallet LLC
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
public class TransactionPagerAdapter extends FragmentPagerAdapter {
    private static final String TAG = TransactionPagerAdapter.class.getName();
    private List<TxItem> items;

    public TransactionPagerAdapter(FragmentManager fm, List<TxItem> items) {
        super(fm);
        this.items = items;

    }

    public TransactionPagerAdapter(FragmentManager fm){
        super(fm);
    }

    @Override
    public Fragment getItem(int pos) {
        return FragmentTransactionItem.newInstance(items.get(pos));
    }

    @Override
    public int getCount() {
        return items == null ? 0 : items.size();
    }

}
