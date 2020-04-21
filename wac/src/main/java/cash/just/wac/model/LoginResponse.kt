package cash.just.wac.model

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class LoginResponse(@field:Json(name = "result") val result: String,
    @field:Json(name = "error") val error: String,
    @field:Json(name = "data") val data: LoginData
)


@JsonClass(generateAdapter = true)
data class LoginData(@field:Json(name = "sessionKey") val sessionKey: String)