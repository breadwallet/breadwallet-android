package cash.just.wac.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class ATMListResponse(@field:Json(name = "result") val result: String,
    @field:Json(name = "error") val error: String,
    @field:Json(name = "data") val data: List<AtmMachine>
)


@JsonClass(generateAdapter = true)
data class AtmMachine(@field:Json(name = "atm_id") val atmId: String)