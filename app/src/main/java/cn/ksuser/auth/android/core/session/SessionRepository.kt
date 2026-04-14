package cn.ksuser.auth.android.core.session

import cn.ksuser.auth.android.core.network.ApiException
import cn.ksuser.auth.android.core.network.CookieStore
import cn.ksuser.auth.android.core.network.KsuserApiService
import cn.ksuser.auth.android.core.network.executeEnvelope
import com.google.gson.Gson
import kotlinx.coroutines.runBlocking
import okhttp3.HttpUrl

class SessionRepository(
    private val apiBaseUrl: HttpUrl,
    private val refreshApi: KsuserApiService,
    private val sessionStorage: SecureSessionStorage,
    private val cookieStore: CookieStore,
    private val gson: Gson,
) {
    private val refreshLock = Any()

    suspend fun bootstrap(): String? {
        ensureCsrfToken()
        val existingToken = sessionStorage.getAccessToken()
        if (!existingToken.isNullOrBlank() && !AccessTokenInspector.isExpiredOrBlank(existingToken)) {
            return existingToken
        }

        if (!hasRefreshToken()) {
            clearSession()
            return null
        }

        return refreshAccessTokenBlocking(existingToken)
    }

    suspend fun ensureCsrfToken() {
        if (cookieStore.hasCookie(apiBaseUrl, "XSRF-TOKEN")) {
            return
        }
        runCatching {
            executeEnvelope(gson) { refreshApi.getCsrfToken() }
        }
    }

    fun refreshAccessTokenBlocking(requestToken: String?): String? = synchronized(refreshLock) {
        if (!hasRefreshToken()) {
            clearSession()
            return null
        }

        val latestToken = sessionStorage.getAccessToken()
        if (
            !requestToken.isNullOrBlank() &&
            !latestToken.isNullOrBlank() &&
            requestToken != latestToken &&
            !AccessTokenInspector.isExpiredOrBlank(latestToken)
        ) {
            return latestToken
        }

        runBlocking {
            try {
                ensureCsrfToken()
                val response = executeEnvelope(gson) { refreshApi.refresh() }
                val token = response.data?.accessToken
                if (!token.isNullOrBlank()) {
                    sessionStorage.setAccessToken(token)
                }
                token
            } catch (error: ApiException) {
                if (error.statusCode == 401 || error.statusCode == 403) {
                    clearSession()
                }
                null
            } catch (_: Exception) {
                clearSession()
                null
            }
        }
    }

    fun persistAccessToken(token: String) {
        sessionStorage.setAccessToken(token)
    }

    fun currentAccessToken(): String? = sessionStorage.getAccessToken()

    fun hasRefreshToken(): Boolean = cookieStore.hasCookie(apiBaseUrl, "refreshToken") || cookieStore.hasCookie("refreshToken")

    fun clearSession() {
        sessionStorage.clear()
        cookieStore.clear()
    }
}
