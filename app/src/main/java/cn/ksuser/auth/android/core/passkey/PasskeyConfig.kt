package cn.ksuser.auth.android.core.passkey

import cn.ksuser.auth.android.core.env.KsuserEnvironment
import cn.ksuser.auth.android.data.model.PasskeyAvailability
import java.net.URI

internal fun passkeyAvailability(environment: KsuserEnvironment): PasskeyAvailability {
    val rpId = environment.passkeyRpId.trim()
    if (rpId.isBlank()) {
        return PasskeyAvailability.RpMismatch
    }

    val origin = parsePasskeyOrigin(environment.passkeyOriginHint) ?: return PasskeyAvailability.DomainNotReady
    if (origin.scheme != "https" || origin.host.isNullOrBlank()) {
        return PasskeyAvailability.DomainNotReady
    }

    return if (hostMatchesRpId(origin.host, rpId)) {
        PasskeyAvailability.Available
    } else {
        PasskeyAvailability.RpMismatch
    }
}

internal fun passkeyAvailabilityMessage(environment: KsuserEnvironment): String {
    return when (passkeyAvailability(environment)) {
        PasskeyAvailability.Available -> "设备支持原生 Passkey，当前 RP ID 为 ${environment.passkeyRpId.trim()}。"
        PasskeyAvailability.UnsupportedDevice ->
            "当前设备不支持原生 Passkey。"
        PasskeyAvailability.MissingProvider ->
            "当前设备没有可用的凭据提供方，请确认已启用 Google Password Manager。"
        PasskeyAvailability.RpMismatch ->
            "Passkey RP 配置不一致。PASSKEY_ORIGIN_HINT 必须是 https 域名，且其主机名要能匹配 RP ID ${environment.passkeyRpId.trim()}。"
        PasskeyAvailability.DomainNotReady ->
            "当前 Passkey 域未就绪。Android 原生 Passkey 需要一个已配置 Digital Asset Links 的 https 域名，localhost 不能直接作为原生 RP 使用。"
    }
}

internal fun requirePasskeyRpMatch(
    environment: KsuserEnvironment,
    backendRpId: String?,
) {
    val availability = passkeyAvailability(environment)
    if (availability != PasskeyAvailability.Available) {
        error(passkeyAvailabilityMessage(environment))
    }

    val expectedRpId = environment.passkeyRpId.trim()
    val actualRpId = backendRpId?.trim().orEmpty().ifBlank { expectedRpId }
    if (!hostMatchesRpId(actualRpId, expectedRpId) && !hostMatchesRpId(expectedRpId, actualRpId)) {
        error("Passkey RP 配置不一致：应用期望 $expectedRpId，但后端返回 $actualRpId。")
    }
}

internal fun passkeyAssetLinksUrl(originHint: String): String? {
    val origin = parsePasskeyOrigin(originHint) ?: return null
    if (origin.scheme != "https" || origin.host.isNullOrBlank()) {
        return null
    }
    return "${origin.scheme}://${origin.host}/.well-known/assetlinks.json"
}

private fun parsePasskeyOrigin(originHint: String): URI? {
    return runCatching { URI(originHint.trim()) }.getOrNull()
}

private fun hostMatchesRpId(host: String, rpId: String): Boolean {
    return host == rpId || host.endsWith(".$rpId")
}
