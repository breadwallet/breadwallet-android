/**
 * BreadWallet
 *
 * Created by Ahsan Butt <ahsan.butt@breadwallet.com> on 9/16/20.
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

import com.breadwallet.ui.send.AddressType

sealed class AddressResult {
    data class Success(val address: String, val destinationTag: String?) : AddressResult()
    object Invalid : AddressResult()
    object ExternalError : AddressResult()
    object NoAddress : AddressResult()
}

interface AddressResolverService {
    /** Resolves [target] to an [AddressResult] **/
    suspend fun resolveAddress(target: String, currencyCode: CurrencyCode, nativeCurrencyCode: CurrencyCode) : AddressResult
}

class AddressResolverServiceLocator (
    private val payIdService: PayIdService,
    private val fioService: FioService
) {

    /** Returns the appropriate [AddressResolverService] for a given [addressType], null if none found **/
    fun getService(addressType: AddressType): AddressResolverService? = when (addressType) {
        is AddressType.Resolvable.PayId -> payIdService
        is AddressType.Resolvable.Fio -> fioService
        else -> null
    }
}