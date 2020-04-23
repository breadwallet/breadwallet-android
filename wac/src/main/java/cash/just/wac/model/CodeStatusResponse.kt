package cash.just.wac.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import java.lang.IllegalArgumentException

@JsonClass(generateAdapter = true)
data class CodeStatusResponse(
    @field:Json(name = "result") val result: String,
    @field:Json(name = "error") val error: WacError?,
    @field:Json(name = "data") val data: CodeStatusItems?
)

@JsonClass(generateAdapter = true)
data class CodeStatusItems(@field:Json(name = "items") val items: List<CashStatus>)

@JsonClass(generateAdapter = true)
data class CashStatus(
    @field:Json(name = "pcode") val code: String?,
    @field:Json(name = "status") val status: String,
    @field:Json(name = "address") val address: String,
    @field:Json(name = "usd_amount") val usdAmount: String,
    @field:Json(name = "btc_amount") val btc_amount: String,
    @field:Json(name = "btc_whole_unit_price") val unitPrice: String,
    @field:Json(name = "expiration") val expiration: String,
    @field:Json(name = "atm_id") val atmId: String,
    @field:Json(name = "loc_description") val description: String,
    @field:Json(name = "loc_lat") val latitude: String,
    @field:Json(name = "loc_lon") val longitude: String) {

    fun getCodeStatus():CodeStatus {
        return CodeStatus.resolve(status)
    }
}

enum class CodeStatus(private val statusCode:String){
    NEW_CODE("A"),
    FUNDED_NOT_CONFIRMED("W"),
    FUNDED("V"),
    USED("U"),
    CANCELLED("C");

    companion object {
        fun resolve(status:String) : CodeStatus {
            CodeStatus.values().find { it.statusCode == status }?.let {
                return it
            }?:run {
                throw IllegalArgumentException("not valid code status {$status}")
            }
        }
    }
}