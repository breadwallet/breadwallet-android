/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 12/09/20.
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
package com.breadwallet.ui.uigift

import android.content.SharedPreferences
import androidx.core.content.edit

private const val KEY_GIFT_PREFIX = "gift-pk-"
private const val KEY_GIFT_STATE_PREFIX = "gift-state-"

data class GiftCopy(
    val address: String,
    val privateKey: String,
    val isUsed: Boolean = false
)

interface GiftBackup {
    fun putGift(gift: GiftCopy)
    fun removeUnusedGift(address: String)
    fun markGiftIsUsed(address: String)
    fun getAllGifts(): List<GiftCopy>
}

class SharedPrefsGiftBackup(
    private val createStore: () -> SharedPreferences?
) : GiftBackup {

    private val store: SharedPreferences? by lazy {
        createStore()
    }

    override fun putGift(gift: GiftCopy) {
        checkNotNull(store).edit {
            putString(gift.address.giftKey(), gift.privateKey)
            putBoolean(gift.address.giftStateKey(), gift.isUsed)
        }
    }

    override fun removeUnusedGift(address: String) {
        val store = checkNotNull(store)
        if (!store.getBoolean(address.giftStateKey(), false)) {
            store.edit {
                remove(address.giftKey())
                remove(address.giftStateKey())
            }
        }
    }

    override fun markGiftIsUsed(address: String) {
        checkNotNull(store).edit {
            putBoolean(address.giftStateKey(), true)
        }
    }

    override fun getAllGifts(): List<GiftCopy> {
        val all = checkNotNull(store).all
        return all.keys
            .filter { it.startsWith(KEY_GIFT_PREFIX) }
            .map { key ->
                val address = key.substringAfter(KEY_GIFT_PREFIX)
                GiftCopy(
                    address,
                    all[key] as String,
                    all[address.giftStateKey()] as Boolean
                )
            }
    }

    private fun String.giftKey() = "$KEY_GIFT_PREFIX$this"
    private fun String.giftStateKey() = "$KEY_GIFT_STATE_PREFIX$this"
}
