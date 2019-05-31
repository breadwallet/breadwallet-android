/**
 * BreadWallet
 * <p/>
 * Created by Pablo Budelli on <pablo.budelli@breadwallet.com> 5/16/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.ui.launcher

import android.arch.lifecycle.ViewModelProviders
import android.content.Intent
import android.os.Bundle
import android.support.v7.app.AppCompatActivity
import com.breadwallet.BreadApp
import com.breadwallet.R
import com.breadwallet.presenter.activities.LoginActivity
import com.breadwallet.presenter.activities.intro.IntroActivity

/**
 * Launcher activity that performs a device state check and if the state is valid then navigates to
 * LoginActivity or IntroActivity depending on if there is a wallet available or not.
 */
class LauncherActivity : AppCompatActivity() {

    private lateinit var mViewModel: LauncherViewModel

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_launcher)
        mViewModel = ViewModelProviders.of(this).get(LauncherViewModel::class.java)
    }

    override fun onResume() {
        super.onResume()
        // TODO isDeviceStateValid is run twice per launch, here and in BreadApp, this is to avoid
        //  the possibility of bypassing​ the validation.
        if ((application as BreadApp).isDeviceStateValid) {
            val intent = if (mViewModel.hasWallet()) {
                Intent(this, LoginActivity::class.java)
            } else {
                Intent(this, IntroActivity::class.java)
            }
            startActivity(intent)
            finish()
        }
    }
    
}
