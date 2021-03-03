/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/17/20.
 * Copyright (c) 2020 breadwallet LLC
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 */
package com.breadwallet.ui.settings.analytics

import android.os.Bundle
import android.view.View
import com.breadwallet.databinding.ControllerShareDataBinding
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.ui.BaseController

class ShareDataController(args: Bundle? = null) : BaseController(args) {

    private val binding by viewBinding(ControllerShareDataBinding::inflate)

    override fun onCreateView(view: View) {
        super.onCreateView(view)
        binding.toggleButton.isChecked = BRSharedPrefs.getShareData()
        binding.toggleButton.setOnCheckedChangeListener { _, isChecked ->
            BRSharedPrefs.putShareData(isChecked)
        }

        binding.backButton.setOnClickListener {
            router.popCurrentController()
        }
    }
}
