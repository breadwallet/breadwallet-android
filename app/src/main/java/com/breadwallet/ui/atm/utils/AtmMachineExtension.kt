package com.breadwallet.ui.atm.utils

import cash.just.sdk.model.AtmMachine

fun AtmMachine.getFullAddress() : String {
    return if (addressDesc.contains(city)) {
        addressDesc
    } else {
        "$addressDesc $city"
    }
}