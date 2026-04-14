package cn.ksuser.auth.android.core.session

import com.google.gson.JsonParser
import java.util.Base64

internal object AccessTokenInspector {
    private const val EXPIRY_SKEW_MS = 30_000L

    fun isExpiredOrBlank(
        token: String?,
        nowMillis: Long = System.currentTimeMillis(),
    ): Boolean {
        val normalizedToken = token?.trim()
        if (normalizedToken.isNullOrBlank()) {
            return true
        }

        val expirySeconds = parseExpirySeconds(normalizedToken) ?: return false
        return expirySeconds * 1000L <= nowMillis + EXPIRY_SKEW_MS
    }

    fun parseExpirySeconds(token: String): Long? {
        val segments = token.split(".")
        if (segments.size < 2) {
            return null
        }

        val payload = runCatching {
            val normalized = normalizeBase64Url(segments[1])
            String(Base64.getUrlDecoder().decode(normalized))
        }.getOrNull() ?: return null

        val root = runCatching { JsonParser.parseString(payload).asJsonObject }.getOrNull() ?: return null
        return root.get("exp")
            ?.takeIf { !it.isJsonNull }
            ?.asLong
    }

    private fun normalizeBase64Url(value: String): String {
        val padding = (4 - value.length % 4) % 4
        return value + "=".repeat(padding)
    }
}
