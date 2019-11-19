/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 10/24/19.
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
package com.breadwallet.effecthandler.metadata

import com.breadwallet.app.BreadApp
import com.breadwallet.ext.bindConsumerIn
import com.breadwallet.logger.logError
import com.platform.entities.TxMetaData
import com.platform.interfaces.AccountMetaDataProvider
import com.spotify.mobius.Connection
import com.spotify.mobius.functions.Consumer
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.channels.BroadcastChannel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.asFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.mapLatest
import kotlinx.coroutines.flow.onEach

class MetaDataEffectHandler(
    private val output: Consumer<MetaDataEvent>,
    private val metaDataProvider: AccountMetaDataProvider
) : Connection<MetaDataEffect>, CoroutineScope {

    companion object {
        private const val COMMENT_UPDATE_DEBOUNCE = 500L
    }

    override val coroutineContext =
        SupervisorJob() + Dispatchers.Default +
            CoroutineExceptionHandler { _, throwable ->
                logError("Error in coroutine", throwable)
            }

    private val commentUpdateChannel = BroadcastChannel<CommentUpdatePair>(Channel.BUFFERED)

    init {
        commentUpdateChannel
            .asFlow()
            .debounce(COMMENT_UPDATE_DEBOUNCE)
            .onEach {
                val metaData = metaDataProvider.getTxMetaData(it.transactionHash) ?: TxMetaData()
                metaDataProvider.putTxMetaData(
                    metaData.copy(
                        comment = it.comment
                    ),
                    it.transactionHash
                )
            }
            .launchIn(this)
    }

    override fun accept(effect: MetaDataEffect) {
        when (effect) {
            MetaDataEffect.RecoverMetaData -> recoverMetaData()
            is MetaDataEffect.LoadTransactionMetaData ->
                loadTransactionMetaData(effect.transactionHash)
            is MetaDataEffect.UpdateTransactionComment ->
                updateTransactionComment(effect.transactionHash, effect.comment)
        }
    }

    override fun dispose() {
        coroutineContext.cancel()
    }

    private fun recoverMetaData() =
        metaDataProvider
            .recoverAll()
            .flowOn(Dispatchers.IO)
            .launchIn(BreadApp.applicationScope)

    private fun loadTransactionMetaData(transactionHash: String) {
        metaDataProvider
            .txMetaData(transactionHash)
            .mapLatest {
                MetaDataEvent.OnTransactionMetaDataUpdated(it)
            }
            .bindConsumerIn(output, this)
    }

    private fun updateTransactionComment(transactionHash: String, comment: String) {
        commentUpdateChannel.offer(
            CommentUpdatePair(
                transactionHash,
                comment
            )
        )
    }
}

data class CommentUpdatePair(
    val transactionHash: String,
    val comment: String
)