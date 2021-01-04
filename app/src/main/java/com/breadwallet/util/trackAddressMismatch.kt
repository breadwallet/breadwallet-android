/**
 * BreadWallet
 *
 * Created by Drew Carlson <drew.carlson@breadwallet.com> on 2/27/20.
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
package com.breadwallet.util

import com.breadwallet.tools.crypto.CryptoHelper.hexEncode
import com.breadwallet.tools.crypto.CryptoHelper.keccak256
import com.breadwallet.tools.util.EventUtils
import com.google.firebase.crashlytics.FirebaseCrashlytics

private const val ETH_ADDRESS_BYTES = 20

fun ByteArray?.pubKeyToEthAddress(): String? = when {
    this == null || isEmpty() -> null
    else -> {
        val addressBytes = keccak256(sliceArray(1..lastIndex))
            ?.takeLast(ETH_ADDRESS_BYTES)
            ?.toByteArray()
        if (addressBytes?.size == ETH_ADDRESS_BYTES) {
            "0x${hexEncode(addressBytes)}"
        } else null
    }
}

private fun sendMismatchEvent(
    ethAddressHash: String,
    rewardsIdHash: String,
    ethBalance: String,
    tokenBalances: List<Pair<String, String>>
) {
    val tokens = tokenBalances.map { (currencyCode, balance) ->
        "has_balance_$currencyCode" to balanceString(balance)
    }
    EventUtils.pushEvent(
        EventUtils.EVENT_PUB_KEY_MISMATCH,
        mapOf(
            EventUtils.EVENT_ATTRIBUTE_REWARDS_ID_HASH to rewardsIdHash,
            EventUtils.EVENT_ATTRIBUTE_ADDRESS_HASH to ethAddressHash,
            "has_balance_eth" to balanceString(ethBalance)
        ) + tokens
    )

    FirebaseCrashlytics.getInstance().apply {
        log("rewards_id_hash = $rewardsIdHash")
        log("old_address_hash = $ethAddressHash")
        log("has_balance_eth = ${balanceString(ethBalance)}")
        tokens.forEach { (key, balance) ->
            log("$key = $balance")
        }
        recordException(IllegalStateException("eth address mismatch"))
    }
}

private fun balanceString(string: String) = when (string) {
    "unknown" -> "unknown"
    "0x0", "0" -> "no"
    else -> "yes"
}
