package cash.just.wac.model

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass
import okhttp3.ResponseBody

@JsonClass(generateAdapter = true)
data class WacError(
    @field:Json(name = "code") val code: String,
    @field:Json(name = "server_message") val server_message: String)

data class WacErrorResponse(val result:String, val error:WacError)

fun ResponseBody.parseError() : WacErrorResponse {
    val type = object : TypeToken<WacErrorResponse>() {}.type
    return Gson().fromJson(this.charStream(), type)
}