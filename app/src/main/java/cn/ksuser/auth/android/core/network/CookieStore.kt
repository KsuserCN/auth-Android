package cn.ksuser.auth.android.core.network

import android.content.Context
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import okhttp3.Cookie
import okhttp3.CookieJar
import okhttp3.HttpUrl

private const val COOKIE_PREFS_NAME = "ksuser_cookie_store"
private const val COOKIE_PREFS_KEY = "cookies"

data class SerializableCookie(
    val name: String,
    val value: String,
    val expiresAt: Long,
    val domain: String,
    val path: String,
    val secure: Boolean,
    val httpOnly: Boolean,
    val persistent: Boolean,
    val hostOnly: Boolean,
) {
    fun toCookie(): Cookie {
        val builder = Cookie.Builder()
            .name(name)
            .value(value)
            .path(path)
            .expiresAt(expiresAt)

        if (hostOnly) {
            builder.hostOnlyDomain(domain)
        } else {
            builder.domain(domain)
        }
        if (secure) builder.secure()
        if (httpOnly) builder.httpOnly()

        return builder.build()
    }

    companion object {
        fun fromCookie(cookie: Cookie): SerializableCookie = SerializableCookie(
            name = cookie.name,
            value = cookie.value,
            expiresAt = cookie.expiresAt,
            domain = cookie.domain,
            path = cookie.path,
            secure = cookie.secure,
            httpOnly = cookie.httpOnly,
            persistent = cookie.persistent,
            hostOnly = cookie.hostOnly,
        )
    }
}

class CookieStore(
    context: Context,
    private val gson: Gson,
) {
    private val prefs = EncryptedSharedPreferences.create(
        context,
        COOKIE_PREFS_NAME,
        MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build(),
        EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
        EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
    )
    private val lock = Any()

    fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        synchronized(lock) {
            val current = readCookies()
                .filterNot { existing ->
                    cookies.any {
                        it.name == existing.name &&
                            it.domain == existing.domain &&
                            it.path == existing.path
                    }
                }
                .filter { !isExpired(it) }
                .toMutableList()

            current.addAll(
                cookies
                    .map(SerializableCookie::fromCookie)
                    .filterNot(::isExpired),
            )

            writeCookies(current)
        }
    }

    fun loadForRequest(url: HttpUrl): List<Cookie> = synchronized(lock) {
        val current = readCookies()
        val valid = current.filterNot(::isExpired)
        if (valid.size != current.size) {
            writeCookies(valid)
        }
        valid.map(SerializableCookie::toCookie).filter { it.matches(url) }
    }

    fun findCookieValue(url: HttpUrl, name: String): String? {
        return loadForRequest(url).firstOrNull { it.name == name }?.value
    }

    fun hasCookie(url: HttpUrl, name: String): Boolean {
        return loadForRequest(url).any { it.name == name }
    }

    fun hasCookie(name: String): Boolean = synchronized(lock) {
        val current = readCookies()
        val valid = current.filterNot(::isExpired)
        if (valid.size != current.size) {
            writeCookies(valid)
        }
        valid.any { it.name == name }
    }

    fun clear() {
        synchronized(lock) {
            prefs.edit().remove(COOKIE_PREFS_KEY).apply()
        }
    }

    private fun readCookies(): List<SerializableCookie> {
        val raw = prefs.getString(COOKIE_PREFS_KEY, null) ?: return emptyList()
        val type = object : TypeToken<List<SerializableCookie>>() {}.type
        return gson.fromJson(raw, type) ?: emptyList()
    }

    private fun writeCookies(cookies: List<SerializableCookie>) {
        prefs.edit().putString(COOKIE_PREFS_KEY, gson.toJson(cookies)).apply()
    }

    private fun isExpired(cookie: SerializableCookie): Boolean = cookie.expiresAt <= System.currentTimeMillis()
}

class PersistentCookieJar(
    private val cookieStore: CookieStore,
) : CookieJar {
    override fun saveFromResponse(url: HttpUrl, cookies: List<Cookie>) {
        cookieStore.saveFromResponse(url, cookies)
    }

    override fun loadForRequest(url: HttpUrl): List<Cookie> = cookieStore.loadForRequest(url)
}
