package cash.just.sdk

import cash.just.sdk.Cash.BtcNetwork
import cash.just.sdk.Cash.BtcNetwork.MAIN_NET
import cash.just.sdk.model.AtmListResponse
import cash.just.sdk.model.CashCodeResponse
import cash.just.sdk.model.CashCodeStatusResponse
import cash.just.sdk.model.LoginResponse
import cash.just.sdk.model.SendVerificationCodeResponse
import cash.just.sdk.model.WacAPI
import retrofit2.Call
import retrofit2.Callback
import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

class CashImpl:Cash {
    private lateinit var sessionKey:String
    private lateinit var retrofit: WacAPI

    override fun createSession(network: BtcNetwork, listener: Cash.SessionCallback) {
        val serverUrl = when(network){
            MAIN_NET -> {
                "https://api-prd.just.cash/"
            }
            BtcNetwork.TEST_NET -> {
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