package com.breadwallet.tools.manager

import com.breadwallet.BuildConfig
import com.unstoppabledomains.config.network.model.Network
import com.unstoppabledomains.exceptions.NamingServiceException
import com.unstoppabledomains.resolution.DomainResolution
import com.unstoppabledomains.resolution.Resolution
import com.unstoppabledomains.resolution.naming.service.NamingServiceType
import timber.log.Timber


/** Litewallet
 * Created by Mohamed Barry on 12/23/20
 * email: mosadialiou@gmail.com
 * Copyright © 2020 Litecoin Foundation. All rights reserved.
 */
data class ResolutionResult(val error: NamingServiceException?, val address: String?)

class UDResolution {
    private val tool: DomainResolution = Resolution.builder()
            .infura(NamingServiceType.CNS, Network.MAINNET, BuildConfig.INFURA_KEY)
            .build()

    fun resolve(domain: String): ResolutionResult {
        return try {
            ResolutionResult(null, tool.getAddress(domain, "LTC"))
        } catch (err: NamingServiceException) {
            Timber.e(err)
            ResolutionResult(err, null)
        }
    }
}