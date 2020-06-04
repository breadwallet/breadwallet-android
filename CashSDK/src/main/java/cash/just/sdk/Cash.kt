package cash.just.sdk

import cash.just.sdk.model.AtmListResponse
import cash.just.sdk.model.CashCodeResponse
import cash.just.sdk.model.CashCodeStatusResponse
import cash.just.sdk.model.SendVerificationCodeResponse
import retrofit2.Call

interface Cash {

    enum class BtcNetwork {
        MAIN_NET,
        TEST_NET
    }

    interface SessionCallback {
        fun onSessionCreated(sessionKey:String)
        fun onError(errorMessage:String?)
    }

    fun createSession(network:BtcNetwork, listener:SessionCallback)
    fun isSessionCreated(): Boolean
    fun getAtmList(): Call<AtmListResponse>
    fun getAtmListByLocation(latitude:String, longitude:String): Call<AtmListResponse>
    fun checkCashCodeStatus(code:String): Call<CashCodeStatusResponse>
    fun createCashCode(atmId:String, amount:String, verificationCode:String): Call<CashCodeResponse>
    fun sendVerificationCode(firstName:String, lastName:String, phoneNumber:String?, email:String?): Call<SendVerificationCodeResponse>
}