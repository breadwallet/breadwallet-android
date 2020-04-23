package cash.just.wac

import cash.just.wac.model.ATMListResponse
import cash.just.wac.model.CashCodeResponse
import cash.just.wac.model.CodeStatusResponse
import cash.just.wac.model.LoginResponse
import cash.just.wac.model.SendCodeResponse
import cash.just.wac.model.WacAPI
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object WAC {
    private lateinit var sessionKey:String

    private val retrofit: WacAPI = Retrofit.Builder()
            .baseUrl("https://secure.just.cash/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build().create(WacAPI::class.java)
    interface WACLogin {
        fun onLogin(sessionKey:String)
        fun onError(errorMessage:String?)
    }

    fun login(listener:WACLogin) {
         retrofit.login().enqueue(object: retrofit2.Callback<LoginResponse> {
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                listener.onError(t.message)
            }

            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                sessionKey = response.body()!!.data.sessionKey
                listener.onLogin(sessionKey)
            }
        })
    }

    fun getAtmList(): Call<ATMListResponse> {
        return retrofit.getAtmList(sessionKey)
    }

    fun getAtmListByLocation(latitude:String, longitude:String): Call<ATMListResponse> {
        return retrofit.getAtmListByLocation(latitude, longitude, sessionKey)
    }

    fun checkCodeStatus(code:String): Call<CodeStatusResponse> {
        return retrofit.checkCodeStatus(code, sessionKey)
    }

    fun createCode(atmId:String, amount:String, verificationCode:String): Call<CashCodeResponse> {
        return retrofit.createCode(sessionKey, atmId, amount, verificationCode)
    }

    fun setVerificationCode(firstName:String, lastName:String, phoneNumber:String?, email:String?): Call<SendCodeResponse> {
        return retrofit.sendVerificationCode(sessionKey, firstName, lastName, phoneNumber, email)
    }
}