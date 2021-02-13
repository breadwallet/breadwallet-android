/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/11/19.
 * Copyright (c) 2019 breadwallet LLC
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
package com.breadwallet.ui.addwallets

import android.view.View
import androidx.recyclerview.widget.LinearLayoutManager
import com.breadwallet.databinding.ControllerAddWalletsBinding
import com.breadwallet.tools.util.Utils
import com.breadwallet.ui.BaseMobiusController
import com.breadwallet.ui.addwallets.AddWallets.E
import com.breadwallet.ui.addwallets.AddWallets.F
import com.breadwallet.ui.addwallets.AddWallets.M
import com.breadwallet.ui.flowbind.clicks
import com.breadwallet.ui.flowbind.textChanges
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import org.kodein.di.direct
import org.kodein.di.erased.instance

class AddWalletsController : BaseMobiusController<M, E, F>() {

    private val binding by viewBinding(ControllerAddWalletsBinding::inflate)

    override val defaultModel = M.createDefault()
    override val init = AddWalletsInit
    override val update = AddWalletsUpdate
    override val flowEffectHandler
        get() = createAddWalletsHandler(
            checkNotNull(applicationContext),
            direct.instance(),
            direct.instance()
        )

    override fun onDetach(view: View) {
        super.onDetach(view)
        Utils.hideKeyboard(activity)
    }

    override fun bindView(modelFlow: Flow<M>): Flow<E> {
        return with(binding) {
            tokenList.layoutManager = LinearLayoutManager(checkNotNull(activity))
            searchEdit.setOnFocusChangeListener { _, hasFocus ->
                if (!hasFocus) {
                    Utils.hideKeyboard(activity)
                }
            }

            merge(
                searchEdit.textChanges().map { E.OnSearchQueryChanged(it) },
                backArrow.clicks().map { E.OnBackClicked },
                bindTokenList(modelFlow)
            )
        }
    }

    private fun bindTokenList(
        modelFlow: Flow<M>
    ) = callbackFlow<E> {
        val adapter = AddTokenListAdapter(
            tokensFlow = modelFlow
                .map { model -> model.tokens }
                .distinctUntilChanged(),
            sendChannel = channel
        )
        binding.tokenList.adapter = adapter

        awaitClose { binding.tokenList.adapter = null }
    }
}
