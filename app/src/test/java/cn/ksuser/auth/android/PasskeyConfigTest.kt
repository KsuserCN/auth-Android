package cn.ksuser.auth.android

import cn.ksuser.auth.android.core.env.KsuserEnvironment
import cn.ksuser.auth.android.core.passkey.passkeyAssetLinksUrl
import cn.ksuser.auth.android.core.passkey.passkeyAvailability
import cn.ksuser.auth.android.core.passkey.requirePasskeyRpMatch
import cn.ksuser.auth.android.data.model.PasskeyAvailability
import org.junit.Assert.assertEquals
import org.junit.Test

class PasskeyConfigTest {
    @Test
    fun httpsOriginMatchingRpId_isAvailable() {
        val environment = testEnvironment(
            passkeyRpId = "auth.ksuser.cn",
            passkeyOriginHint = "https://auth.ksuser.cn",
        )

        assertEquals(PasskeyAvailability.Available, passkeyAvailability(environment))
        requirePasskeyRpMatch(environment, "auth.ksuser.cn")
    }

    @Test(expected = IllegalStateException::class)
    fun mismatchedBackendRpId_isRejected() {
        val environment = testEnvironment(
            passkeyRpId = "auth.ksuser.cn",
            passkeyOriginHint = "https://auth.ksuser.cn",
        )

        requirePasskeyRpMatch(environment, "localhost")
    }

    @Test
    fun localhostOrigin_isMarkedNotReady() {
        val environment = testEnvironment(
            passkeyRpId = "localhost",
            passkeyOriginHint = "http://localhost:5173",
        )

        assertEquals(PasskeyAvailability.DomainNotReady, passkeyAvailability(environment))
        assertEquals(null, passkeyAssetLinksUrl(environment.passkeyOriginHint))
    }

    private fun testEnvironment(
        passkeyRpId: String,
        passkeyOriginHint: String,
    ) = KsuserEnvironment(
        apiBaseUrl = "https://api.ksuser.cn",
        passkeyRpId = passkeyRpId,
        passkeyOriginHint = passkeyOriginHint,
        appEnv = "test",
        enableHttpLogging = false,
    )
}
