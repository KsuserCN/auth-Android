package cn.ksuser.auth.android

import cn.ksuser.auth.android.core.env.EnvironmentProvider
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EnvironmentProviderTest {
    @Test
    fun apiBaseUrl_isPresentAndTrimmed() {
        val env = EnvironmentProvider.current
        assertTrue(env.apiBaseUrl.isNotBlank())
        assertFalse(env.apiBaseUrl.endsWith("/"))
    }

    @Test
    fun passkeyFields_arePresent() {
        val env = EnvironmentProvider.current
        assertTrue(env.passkeyRpId.isNotBlank())
        assertTrue(env.passkeyOriginHint.startsWith("http"))
    }
}
