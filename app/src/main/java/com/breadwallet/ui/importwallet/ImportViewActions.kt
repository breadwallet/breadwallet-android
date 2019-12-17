package com.breadwallet.ui.importwallet


interface ImportViewActions {

    fun showNoWalletsEnabled()
    fun showConfirmImport(receiveAmount: String, feeAmount: String)
    fun showNoBalance()
    fun showKeyInvalid()
    fun showPasswordInvalid()
    fun showImportFailed()
    fun showPasswordInput()
    fun showBalanceTooLow()
    fun showImportSuccess()
}
