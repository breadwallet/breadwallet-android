package cash.just.wac

import cash.just.wac.model.ATMListResponse
import cash.just.wac.model.LoginResponse
import cash.just.wac.model.WacAPI
import retrofit2.Call
import retrofit2.Retrofit
import retrofit2.converter.moshi.MoshiConverterFactory

object WAC {
    private val retrofit: WacAPI = Retrofit.Builder()
            .baseUrl("https://secure.just.cash/atm/wac/")
            .addConverterFactory(MoshiConverterFactory.create())
            .build().create(WacAPI::class.java)

    fun login(): Call<LoginResponse> {
        return retrofit.login()
    }

    fun getAtmList(): Call<ATMListResponse> {
        return retrofit.atmList()
    }
}