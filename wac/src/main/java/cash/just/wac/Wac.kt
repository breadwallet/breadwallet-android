package cash.just.wac

import cash.just.wac.model.ATMListResponse
import cash.just.wac.model.CashCodeResponse
import cash.just.wac.model.CodeStatusResponse
import cash.just.wac.model.SendCodeResponse
import retrofit2.Call

interface Wac {

    interface OnLoginListener {
        fun onLogin(sessionKey:String)
        fun onError(errorMessage:String?)
    }

    fun login(listener:OnLoginListener)

    fun getAtmList(): Call<ATMListResponse>
    fun getAtmListByLocation(latitude:String, longitude:String): Call<ATMListResponse>
    fun checkCodeStatus(code:String): Call<CodeStatusResponse>
    fun createCode(atmId:String, amount:String, verificationCode:String): Call<CashCodeResponse>
    fun sendVerificationCode(firstName:String, lastName:String, phoneNumber:String?, email:String?): Call<SendCodeResponse>
}