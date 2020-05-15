package cash.just.wac

import cash.just.wac.Wac.BtcSERVER
import cash.just.wac.Wac.BtcSERVER.MAIN_NET
import cash.just.wac.model.AtmListResponse
import cash.just.wac.model.CashCodeResponse
import cash.just.wac.model.CashCodeStatusResponse
import cash.just.wac.model.LoginResponse
import cash.just.wac.model.SendVerificationCodeResponse
import cash.just.wac.model.WacAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class WacImpl:Wac {
    private lateinit var sessionKey:String
    private lateinit var retrofit: WacAPI

    override fun createSession(server: BtcSERVER, listener: Wac.SessionCallback) {
        val serverUrl = when(server){
            MAIN_NET -> {
                "https://secure.just.cash/"
            }
            BtcSERVER.TEST_NET -> {
                "https://secure.just.cash/"
            }
        }

        retrofit = Retrofit.Builder().baseUrl(serverUrl)
            .addConverterFactory(MoshiConverterFactory.create())
            .build().create(WacAPI::class.java)

        retrofit.login().enqueue(object: Callback<LoginResponse> {
            override fun onFailure(call: Call<LoginResponse>, t: Throwable) {
                listener.onError(t.message)
            }

            override fun onResponse(call: Call<LoginResponse>, response: Response<LoginResponse>) {
                sessionKey = response.body()!!.data.sessionKey
                listener.onSessionCreated(sessionKey)
            }
        })
    }

    override fun isSessionCreated() : Boolean {
        return this::sessionKey.isInitialized
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