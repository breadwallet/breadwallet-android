package cash.just.sdk.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class CashCodeResponse(
    @field:Json(name = "result") val result: String,
    @field:Json(name = "error") val error: WacError?,
    @field:Json(name = "data") val data: CashCodeItems
)

@JsonClass(generateAdapter = true)
data class SendVerificationCodeResponse(
    @field:Json(name = "result") val result: String,
    @field:Json(name = "error") val error: WacError?,
    @field:Json(name = "data") val data: SendCodeItems
)

@JsonClass(generateAdapter = true)
data class SendCodeItems(@field:Json(name = "items") val items: List<CashSendCode>)

@JsonClass(generateAdapter = true)
data class CashSendCode(@field:Json(name = "result") val result: String)


@JsonClass(generateAdapter = true)
data class CashCodeItems(@field:Json(name = "items") val items: List<CashCode>)


@JsonClass(generateAdapter = true)
data class CashCode(
    @field:Json(name = "secure_code") val secureCode: String,
    @field:Json(name = "address") val address: String,
    @field:Json(name = "usd_amount") val usdAmount: String,
    @field:Json(name = "btc_amount") val btc_amount: String,
    @field:Json(name = "btc_whole_unit_price") val unitPrice: String)

