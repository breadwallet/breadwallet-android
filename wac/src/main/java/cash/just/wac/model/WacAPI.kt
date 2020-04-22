package cash.just.wac.model

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Query

interface WacAPI {
    @POST("/atm/wac/guest/login")
    fun login(): Call<LoginResponse>

    @GET("/atm/wac/atm/list")
    fun atmList(@Query("sessionKey") sessionKey:String): Call<ATMListResponse>
}
