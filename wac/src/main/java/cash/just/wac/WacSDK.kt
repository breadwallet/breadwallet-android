package cash.just.wac

import cash.just.wac.model.AtmListResponse
import cash.just.wac.model.CashCodeResponse
import cash.just.wac.model.CashCodeStatusResponse
import cash.just.wac.model.SendVerificationCodeResponse
import retrofit2.Call

object WacSDK : Wac {
    private var wacImpl : Wac = WacImpl()

    override fun createSession(listener: Wac.SessionCallback) {
        wacImpl.createSession(listener)
    }

    override fun isSessionCreated(): Boolean {
       return wacImpl.isSessionCreated()
    }

    override fun getAtmList(): Call<AtmListResponse> {
        return wacImpl.getAtmList()
    }

    override fun getAtmListByLocation(latitude:String, longitude:String): Call<AtmListResponse> {
        return wacImpl.getAtmListByLocation(latitude, longitude)
    }

    override fun checkCashCodeStatus(code:String): Call<CashCodeStatusResponse> {
        return wacImpl.checkCashCodeStatus(code)
    }

    override fun createCashCode(atmId:String, amount:String, verificationCode:String): Call<CashCodeResponse> {
        return wacImpl.createCashCode(atmId, amount, verificationCode)
    }

    override fun sendVerificationCode(firstName:String, lastName:String, phoneNumber:String?, email:String?): Call<SendVerificationCodeResponse> {
        return wacImpl.sendVerificationCode(firstName, lastName, phoneNumber, email)
    }
}