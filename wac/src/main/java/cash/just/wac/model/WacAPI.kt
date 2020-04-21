package cash.just.wac.model

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST

interface WacAPI {
    @POST("/guest/login")
    fun login(): Call<LoginResponse>

    @GET("/atm/list")
    fun atmList(): Call<ATMListResponse>
}
