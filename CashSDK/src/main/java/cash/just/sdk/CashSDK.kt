package cash.just.sdk

import cash.just.sdk.model.AtmListResponse
import cash.just.sdk.model.CashCodeResponse
import cash.just.sdk.model.CashCodeStatusResponse
import cash.just.sdk.model.SendVerificationCodeResponse
import retrofit2.Call

object CashSDK : Cash {
    private var cashImpl : Cash = CashImpl()

    override fun createSession(network: Cash.BtcNetwork, listener: Cash.SessionCallback) {
        cashImpl.createSession(network, listener)
    }

    override fun isSessionCreated(): Boolean {
       return cashImpl.isSessionCreated()
    }

    override fun getAtmList(): Call<AtmListResponse> {
        return cashImpl.getAtmList()
    }

    override fun getAtmListByLocation(latitude:String, longitude:String): Call<AtmListResponse> {
        return cashImpl.getAtmListByLocation(latitude, longitude)
    }

    override fun checkCashCodeStatus(code:String): Call<CashCodeStatusResponse> {
        return cashImpl.checkCashCodeStatus(code)
    }

    override fun createCashCode(atmId:String, amount:String, verificationCode:String): Call<CashCodeResponse> {
        return cashImpl.createCashCode(atmId, amount, verificationCode)
    }

    override fun sendVerificationCode(firstName:String, lastName:String, phoneNumber:String?, email:String?): Call<SendVerificationCodeResponse> {
        return cashImpl.sendVerificationCode(firstName, lastName, phoneNumber, email)
    }
}