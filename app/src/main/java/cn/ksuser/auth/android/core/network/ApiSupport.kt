package cn.ksuser.auth.android.core.network

import com.google.gson.Gson
import retrofit2.Response

data class ApiEnvelope<T>(
    val code: Int? = null,
    val msg: String? = null,
    val data: T? = null,
)

class ApiException(
    val statusCode: Int,
    override val message: String,
) : RuntimeException(message)

suspend fun <T> executeEnvelope(
    gson: Gson,
    request: suspend () -> Response<ApiEnvelope<T>>,
): ApiEnvelope<T> {
    val response = request()
    val body = response.body()
    if (response.isSuccessful && body != null) {
        return body
    }

    val parsedError = response.errorBody()?.charStream()?.use { reader ->
        runCatching { gson.fromJson(reader, ApiEnvelope::class.java) }.getOrNull()
    }
    val message = parsedError?.msg ?: body?.msg ?: response.message().ifBlank { "请求失败" }
    throw ApiException(response.code(), message)
}

fun requireCode(
    envelope: ApiEnvelope<*>,
    vararg acceptedCodes: Int,
) {
    val code = envelope.code ?: error("缺少响应状态码")
    if (acceptedCodes.contains(code)) return
    throw ApiException(code, envelope.msg ?: "请求失败")
}
