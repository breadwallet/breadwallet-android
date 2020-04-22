package cash.just.wac.model

import retrofit2.Call
import retrofit2.http.GET
import retrofit2.http.POST
import retrofit2.http.Path
import retrofit2.http.Query

interface WacAPI {
    @POST("/atm/wac/guest/login")
    fun login(): Call<LoginResponse>

    @GET("/atm/wac/atm/list")
    fun getAtmList(@Query("sessionKey") sessionKey:String): Call<ATMListResponse>

    @GET("/atm/wac/atm/near/latlon/{lat}/{lon}")
    fun getAtmListByLocation(
        @Path(value="lat", encoded=true) lat:String,
        @Path(value="lon", encoded=true) lon:String,
        @Query("sessionKey") sessionKey:String): Call<ATMListResponse>

    @GET("/atm/wac/pcode/{pcode}")
    fun checkCodeStatus(@Path(value="pcode", encoded=true) code:String, @Query("sessionKey") sessionKey:String): Call<CashCodeResponse>

    @POST("/atm/wac/pcode/")
    fun createCode(
        @Query("sessionKey") sessionKey:String,
        @Query(value="atm_id", encoded=true) atmId:String,
        @Query(value="amount", encoded=true) amount:String,
        @Query(value="verification_code", encoded=true) verificationCode:String): Call<CashCodeResponse>

    @POST("/atm/wac/pcode/verify")
    fun sendVerificationCode(
        @Query("sessionKey") sessionKey:String,
        @Query(value="first_name", encoded=true) firstName:String,
        @Query(value="last_name", encoded=true) lastName:String,
        @Query(value="phone_number", encoded=true) phoneNumber:String?,
        @Query(value="email", encoded=true) email:String?
    ): Call<SendCodeResponse>
}
