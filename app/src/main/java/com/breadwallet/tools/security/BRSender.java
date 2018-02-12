package com.breadwallet.tools.security;

import android.app.Activity;
import android.content.Context;
import android.util.Log;

import com.breadwallet.R;
import com.breadwallet.presenter.customviews.BRDialogView;
import com.breadwallet.presenter.entities.PaymentItem;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.BRAnimator;
import com.breadwallet.tools.animation.BRDialog;
import com.breadwallet.tools.manager.BRApiManager;
import com.breadwallet.tools.manager.BRReportsManager;
import com.breadwallet.tools.manager.BRSharedPrefs;
import com.breadwallet.tools.threads.BRExecutor;
import com.breadwallet.tools.util.BRConstants;
import com.breadwallet.tools.util.CurrencyUtils;
import com.breadwallet.tools.util.ExchangeUtils;
import com.breadwallet.wallet.BRWalletManager;
import com.google.firebase.crash.FirebaseCrash;

import java.math.BigDecimal;
import java.util.Locale;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 4/25/17.
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
public class BRSender {
    private static final String TAG = BRSender.class.getName();

    private static BRSender instance;


    private BRSender() {
    }

    public static BRSender getInstance() {
        if (instance == null) instance = new BRSender();
        return instance;
    }


}
