package com.breadwallet.ui.atm

import cash.just.wac.model.CashStatus
import java.io.Serializable

data class RetryableCashStatus(val secureCode:String, val cashStatus:CashStatus) : Serializable