package cash.just.wac

import cash.just.wac.model.ATMListResponse
import cash.just.wac.model.CashCodeResponse
import cash.just.wac.model.CodeStatusResponse
import cash.just.wac.model.SendCodeResponse
import retrofit2.Call

object WacSDK : Wac {
    private var wacImpl : Wac = WacImpl()

    override fun login(listener: Wac.OnLoginListener) {
        wacImpl.login(listener)
    }

    override fun getAtmList(): Call<ATMListResponse> {
        return wacImpl.getAtmList()
    }

    override fun getAtmListByLocation(latitude:String, longitude:String): Call<ATMListResponse> {
        return wacImpl.getAtmListByLocation(latitude, longitude)
    }

    override fun checkCodeStatus(code:String): Call<CodeStatusResponse> {
        return wacImpl.checkCodeStatus(code)
    }

    override fun createCode(atmId:String, amount:String, verificationCode:String): Call<CashCodeResponse> {
        return wacImpl.createCode(atmId, amount, verificationCode)
    }

    override fun sendVerificationCode(firstName:String, lastName:String, phoneNumber:String?, email:String?): Call<SendCodeResponse> {
        return wacImpl.sendVerificationCode(firstName, lastName, phoneNumber, email)
    }
}