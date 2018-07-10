package com.breadwallet.wallet.configs;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.view.View;

import com.breadwallet.R;
import com.breadwallet.presenter.activities.settings.ImportActivity;
import com.breadwallet.presenter.activities.settings.SpendLimitActivity;
import com.breadwallet.presenter.activities.settings.SyncBlockchainActivity;
import com.breadwallet.presenter.entities.BRSettingsItem;
import com.breadwallet.presenter.interfaces.BRAuthCompletion;
import com.breadwallet.tools.animation.UiUtils;
import com.breadwallet.tools.security.AuthManager;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

/**
 * BreadWallet
 * <p/>
 * Created by Mihail Gutan on <mihail@breadwallet.com> 1/25/18.
 * Copyright (c) 2018 breadwallet LLC
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
public class WalletSettingsConfiguration {

    private List<BRSettingsItem> mSettingList;
    private List<BigDecimal> mFingerprintLimits;

    public WalletSettingsConfiguration() {
        mSettingList = new ArrayList<>();
        mFingerprintLimits = new ArrayList<>();
    }

    public WalletSettingsConfiguration(Context context, String iso, List<BRSettingsItem> settingList, List<BigDecimal> limits) {
        mFingerprintLimits = limits;
        mSettingList = settingList;
    }

    public List<BRSettingsItem> getSettingsList() {
        return mSettingList;
    }

    public List<BigDecimal> getFingerprintLimits() {
        return mFingerprintLimits;
    }

}
