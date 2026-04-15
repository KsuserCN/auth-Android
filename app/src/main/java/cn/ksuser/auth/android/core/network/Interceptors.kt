package cn.ksuser.auth.android.core.network

import cn.ksuser.auth.android.core.app.AppIdentityProvider
import cn.ksuser.auth.android.core.session.SessionRepository
import cn.ksuser.auth.android.core.session.SecureSessionStorage
import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response

private const val REFRESH_RETRY_HEADER = "X-KSUSER-REFRESH-RETRIED"

class AccessTokenInterceptor(
    private val sessionStorage: SecureSessionStorage,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
        val builder = original.newBuilder()
        if (original.header("User-Agent").isNullOrBlank()) {
            builder.header("User-Agent", AppIdentityProvider.userAgent())
        }

        val token = sessionStorage.getAccessToken()
        if (!token.isNullOrBlank() && original.header("Authorization") == null) {
            builder.header("Authorization", "Bearer $token")
        }
        return chain.proceed(builder.build())
    }
}

class CsrfInterceptor(
    private val cookieStore: CookieStore,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        if (request.method in setOf("GET", "HEAD", "OPTIONS")) {
            return chain.proceed(request)
        }

        var csrfToken = cookieStore.findCookieValue(request.url, "XSRF-TOKEN")
        if (csrfToken.isNullOrBlank()) {
            val bootstrapRequest = request.newBuilder()
                .get()
                .url(
                    request.url.newBuilder()
                        .encodedPath("/auth/csrf-token")
                        .query(null)
                        .build(),
                )
                .removeHeader("X-XSRF-TOKEN")
                .build()
            runCatching {
                chain.proceed(bootstrapRequest).use { /* CSRF cookie will be persisted by CookieJar */ }
            }
            csrfToken = cookieStore.findCookieValue(request.url, "XSRF-TOKEN")
        }

        val enriched = if (csrfToken.isNullOrBlank() || request.header("X-XSRF-TOKEN") != null) {
            request
        } else {
            request.newBuilder()
                .header("X-XSRF-TOKEN", csrfToken)
                .build()
        }
        return chain.proceed(enriched)
    }
}

class UnauthorizedRetryInterceptor(
    private val sessionRepository: SessionRepository,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!shouldRefresh(request, response)) {
            return response
        }

        val requestToken = request.header("Authorization")
            ?.removePrefix("Bearer ")
            ?.trim()
        val refreshedToken = sessionRepository.refreshAccessTokenBlocking(requestToken) ?: return response
        response.close()

        return chain.proceed(
            request.newBuilder()
                .header("Authorization", "Bearer $refreshedToken")
                .header(REFRESH_RETRY_HEADER, "1")
                .build(),
        )
    }

    private fun shouldRefresh(request: Request, response: Response): Boolean {
        if (response.code != 401) return false
        if (request.url.encodedPath.endsWith("/auth/refresh")) return false
        if (request.header("Authorization").isNullOrBlank()) return false
        if (!request.header(REFRESH_RETRY_HEADER).isNullOrBlank()) return false
        return true
    }
}
