package com.breadwallet.ui.atm.model

import cash.just.sdk.model.AtmMachine
fun getAtmMachineMock() : AtmMachine {
    return AtmMachine("34",
        "bay street, 23", "white bulding",
        "Toronto", "M4B24T",
        "0.234", "0.343", "atm machine blue",
        "2", "20.0", "60.0", "20", "USD",1)
}