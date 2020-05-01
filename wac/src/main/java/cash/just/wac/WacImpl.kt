package cash.just.wac

import cash.just.wac.model.AtmListResponse
import cash.just.wac.model.CashCodeResponse
import cash.just.wac.model.CashCodeStatusResponse
import cash.just.wac.model.LoginResponse
import cash.just.wac.model.SendVerificationCodeResponse
import cash.just.wac.model.WacAPI
import retrofit2.Call
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WacImpl:Wac {
    private lateinit var sessionKey:String
    private val retrofit: WacAPI = Retrofit.Builder()
        .baseUrl("https://secure.just.cash/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build().create(WacAPI::class.java)

    override fun createSession(listener: Wac.SessionCallback) {
        retrofit.login().enqueue(object: retrofit2.Callback<LoginResponse> {
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                listener.onError(t.message)
            }

            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                sessionKey = response.body()!!.data.sessionKey
                listener.onSessionCreated(sessionKey)
            }
        })
    }

    override fun getAtmList(): Call<AtmListResponse> {
        return retrofit.getAtmList(sessionKey)
    }

    override fun getAtmListByLocation(latitude: String, longitude: String): Call<AtmListResponse> {
        return retrofit.getAtmListByLocation(latitude, longitude, sessionKey)
    }

    override fun checkCashCodeStatus(code: String): Call<CashCodeStatusResponse> {
        return retrofit.checkCodeStatus(code, sessionKey)
    }

    override fun createCashCode(atmId: String, amount: String, verificationCode: String): Call<CashCodeResponse> {
        return retrofit.createCode(sessionKey, atmId, amount, verificationCode)
    }

    override fun sendVerificationCode(
        firstName: String,
        lastName: String,
        phoneNumber: String?,
        email: String?
    ): Call<SendVerificationCodeResponse> {
        return retrofit.sendVerificationCode(sessionKey, firstName, lastName, phoneNumber, email)
    }
}