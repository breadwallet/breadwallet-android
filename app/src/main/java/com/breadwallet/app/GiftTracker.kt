/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 1/5/2021.
 * Copyright (c) 2021 breadwallet LLC
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
package com.breadwallet.app

import com.breadwallet.breadbox.BreadBox
import com.breadwallet.breadbox.createSweeper
import com.breadwallet.crypto.Amount
import com.breadwallet.crypto.Key
import com.breadwallet.crypto.errors.WalletSweeperError
import com.breadwallet.crypto.errors.WalletSweeperInsufficientFundsError
import com.breadwallet.platform.entities.GiftMetaData
import com.breadwallet.platform.entities.TxMetaDataValue
import com.breadwallet.platform.interfaces.AccountMetaDataProvider
import com.breadwallet.tools.manager.BRSharedPrefs
import com.breadwallet.tools.util.btc
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first

private const val UNCLAIMED_GIFT_DELAY_MS = 3_600_000L

class GiftTracker(
    private val breadBox: BreadBox,
    private val metaDataManager: AccountMetaDataProvider
) {

    suspend fun checkUnclaimedGifts() {
        val currentMs = System.currentTimeMillis()
        val millisSinceLastCheck = currentMs - BRSharedPrefs.lastGiftCheckTime
        if (millisSinceLastCheck < UNCLAIMED_GIFT_DELAY_MS) {
            return
        }
        breadBox.walletSyncState(btc).first { !it.isSyncing }

        val unclaimedGifts = metaDataManager
            .txMetaData(onlyGifts = true)
            .mapNotNull { (key, metadata) ->
                val gift = (metadata as TxMetaDataValue).gift!!
                if (gift.claimed || gift.reclaimed) null else key to metadata
            }
            .toMap()

        if (unclaimedGifts.isNotEmpty()) {
            val btcWallet = breadBox.wallet(btc).first()
            val btcManager = btcWallet.walletManager

            unclaimedGifts.forEach { (key, metadata) ->
                val gift = metadata.gift!!
                val coreKey = gift.coreKey() ?: return@forEach
                try {
                    val sweeper = btcManager.createSweeper(btcWallet, coreKey)
                    val balance = sweeper.balance.orNull() ?: Amount.create(0, btcWallet.unit)
                    if (balance.isZero) {
                        markGiftClaimed(key, metadata)
                    }
                } catch (e: WalletSweeperError) {
                    if (e is WalletSweeperInsufficientFundsError) {
                        markGiftClaimed(key, metadata)
                    }
                }
            }
            BRSharedPrefs.lastGiftCheckTime = currentMs
        }
    }

    suspend fun markGiftReclaimed(transferHash: String) {
        val transfer = breadBox.walletTransfer(btc, transferHash).first()
        val metadata = metaDataManager.txMetaData(transfer)
            .filterIsInstance<TxMetaDataValue>()
            .first()
        metaDataManager.putTxMetaData(
            transfer,
            metadata.copy(gift = metadata.gift!!.copy(reclaimed = true))
        )
    }

    private suspend fun markGiftClaimed(key: String, metadata: TxMetaDataValue) {
        metaDataManager.putTxMetaData(
            key = key,
            isErc20 = false,
            newTxMetaData = metadata.copy(
                gift = metadata.gift!!.copy(
                    claimed = true
                )
            )
        )
    }

    private fun GiftMetaData.coreKey(): Key? =
        keyData
            ?.toByteArray(Charsets.UTF_8)
            ?.run(Key::createFromPrivateKeyString)
            ?.orNull()
}
