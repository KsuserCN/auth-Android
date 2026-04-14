package cn.ksuser.auth.android

import cn.ksuser.auth.android.core.session.AccessTokenInspector
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Base64

class AccessTokenInspectorTest {
    @Test
    fun expiredToken_isDetected() {
        val nowSeconds = System.currentTimeMillis() / 1000L
        val token = jwtWithExp(nowSeconds - 120)
        assertTrue(AccessTokenInspector.isExpiredOrBlank(token))
    }

    @Test
    fun validToken_isAccepted() {
        val nowSeconds = System.currentTimeMillis() / 1000L
        val token = jwtWithExp(nowSeconds + 3600)
        assertFalse(AccessTokenInspector.isExpiredOrBlank(token))
    }

    @Test
    fun malformedToken_doesNotForceExpiry() {
        assertFalse(AccessTokenInspector.isExpiredOrBlank("not-a-jwt"))
    }

    private fun jwtWithExp(exp: Long): String {
        val encoder = Base64.getUrlEncoder().withoutPadding()
        val header = encoder.encodeToString("""{"alg":"none"}""".toByteArray())
        val payload = encoder.encodeToString("""{"exp":$exp}""".toByteArray())
        return "$header.$payload.signature"
    }
}
