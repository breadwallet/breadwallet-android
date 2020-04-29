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

class WacImpl:Wac {
    private lateinit var sessionKey:String
    private val retrofit: WacAPI = Retrofit.Builder()
        .baseUrl("https://secure.just.cash/")
        .addConverterFactory(MoshiConverterFactory.create())
        .build().create(WacAPI::class.java)

    override fun login(listener: Wac.OnLoginListener) {
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

    override fun getAtmList(): Call<ATMListResponse> {
        return retrofit.getAtmList(sessionKey)
    }

    override fun getAtmListByLocation(latitude: String, longitude: String): Call<ATMListResponse> {
        return retrofit.getAtmListByLocation(latitude, longitude, sessionKey)
    }

    override fun checkCodeStatus(code: String): Call<CodeStatusResponse> {
        return retrofit.checkCodeStatus(code, sessionKey)
    }

    override fun createCode(atmId: String, amount: String, verificationCode: String): Call<CashCodeResponse> {
        return retrofit.createCode(sessionKey, atmId, amount, verificationCode)
    }

    override fun sendVerificationCode(
        firstName: String,
        lastName: String,
        phoneNumber: String?,
        email: String?
    ): Call<SendCodeResponse> {
        return retrofit.sendVerificationCode(sessionKey, firstName, lastName, phoneNumber, email)
    }
}