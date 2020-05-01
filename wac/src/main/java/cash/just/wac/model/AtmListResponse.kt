package cash.just.wac.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class AtmListResponse(
    @field:Json(name = "result") val result: String,
    @field:Json(name = "error") val error: WacError?,
    @field:Json(name = "data") val data: AtmItems
)



@JsonClass(generateAdapter = true)
data class AtmItems(@field:Json(name = "items") val items: List<AtmMachine>)

@JsonClass(generateAdapter = true)
data class AtmMachine(
    @field:Json(name = "atm_id") val atmId: String,
    @field:Json(name = "address_desc") val addressDesc: String,
    @field:Json(name = "address_detail") val detail: String?,
    @field:Json(name = "address_city") val city: String,
    @field:Json(name = "address_zip") val zip: String,
    @field:Json(name = "loc_lon") val longitude: String,
    @field:Json(name = "loc_lat") val latitude: String,
    @field:Json(name = "atm_desc") val desc: String?,
    @field:Json(name = "atm_fees") val fees: String,
    @field:Json(name = "atm_min") val min: String,
    @field:Json(name = "atm_max") val max: String,
    @field:Json(name = "atm_bills") val bills: String,
    @field:Json(name = "atm_currency") val currency: String)