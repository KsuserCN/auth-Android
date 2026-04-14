package cn.ksuser.auth.android.core.passkey

import android.app.Activity
import androidx.credentials.CreatePublicKeyCredentialRequest
import androidx.credentials.CreatePublicKeyCredentialResponse
import androidx.credentials.CredentialManager
import androidx.credentials.GetCredentialRequest
import androidx.credentials.GetPublicKeyCredentialOption
import androidx.credentials.PublicKeyCredential
import androidx.credentials.exceptions.CreateCredentialCancellationException
import androidx.credentials.exceptions.CreateCredentialProviderConfigurationException
import androidx.credentials.exceptions.CreateCredentialUnsupportedException
import androidx.credentials.exceptions.GetCredentialCancellationException
import androidx.credentials.exceptions.GetCredentialProviderConfigurationException
import androidx.credentials.exceptions.GetCredentialUnsupportedException
import androidx.credentials.exceptions.publickeycredential.CreatePublicKeyCredentialDomException
import androidx.credentials.exceptions.publickeycredential.GetPublicKeyCredentialDomException
import cn.ksuser.auth.android.core.env.KsuserEnvironment
import cn.ksuser.auth.android.data.model.PasskeyAuthenticationOptions
import cn.ksuser.auth.android.data.model.PasskeyAuthenticationPayload
import cn.ksuser.auth.android.data.model.PasskeyAvailability
import cn.ksuser.auth.android.data.model.PasskeyRegistrationOptions
import cn.ksuser.auth.android.data.model.PasskeyRegistrationPayload
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject
import com.google.gson.JsonParser

class PasskeyManager(
    private val credentialManager: CredentialManager,
    private val gson: Gson,
    private val environment: KsuserEnvironment,
) {
    fun availability(): PasskeyAvailability = passkeyAvailability(environment)

    fun availabilityMessage(): String = passkeyAvailabilityMessage(environment)

    suspend fun createForRegistration(
        activity: Activity,
        options: PasskeyRegistrationOptions,
    ): PasskeyRegistrationPayload {
        val requestJson = buildRegistrationRequestJson(options)
        return try {
            val response = credentialManager.createCredential(
                context = activity,
                request = CreatePublicKeyCredentialRequest(requestJson),
            )
            val registrationResponse = response as? CreatePublicKeyCredentialResponse
                ?: error("无法创建 Passkey")
            parseRegistrationResponse(registrationResponse.registrationResponseJson)
        } catch (throwable: Throwable) {
            throw mapPasskeyFailure(throwable)
        }
    }

    suspend fun getForAuthentication(
        activity: Activity,
        options: PasskeyAuthenticationOptions,
    ): PasskeyAuthenticationPayload {
        val requestJson = buildAuthenticationRequestJson(options)
        return try {
            val response = credentialManager.getCredential(
                context = activity,
                request = GetCredentialRequest(
                    listOf(GetPublicKeyCredentialOption(requestJson)),
                ),
            )
            val publicKeyCredential = response.credential as? PublicKeyCredential
                ?: error("未返回 PublicKeyCredential")
            parseAuthenticationResponse(publicKeyCredential.authenticationResponseJson)
        } catch (throwable: Throwable) {
            throw mapPasskeyFailure(throwable)
        }
    }

    private fun buildRegistrationRequestJson(options: PasskeyRegistrationOptions): String {
        val rp = JsonParser.parseString(options.rp).asJsonObject
        val user = JsonParser.parseString(options.user).asJsonObject
        val pubKeyCredParams = JsonParser.parseString(options.pubKeyCredParams).asJsonArray
        val authenticatorSelection = JsonParser.parseString(options.authenticatorSelection).asJsonObject

        authenticatorSelection.remove("authenticatorAttachment")
        val rpId = rp.get("id")
            ?.takeIf { !it.isJsonNull }
            ?.asString
            ?.trim()
            .orEmpty()
            .ifBlank { environment.passkeyRpId.trim() }
        requirePasskeyRpMatch(environment, rpId)
        if (!rp.has("id") || rp.get("id").isJsonNull || rp.get("id").asString.isBlank()) {
            rp.addProperty("id", rpId)
        }

        val json = JsonObject().apply {
            addProperty("challenge", options.challenge)
            add("rp", rp)
            add("user", user)
            add("pubKeyCredParams", pubKeyCredParams)
            addProperty("timeout", options.timeout.toLongOrNull() ?: 300_000L)
            addProperty("attestation", options.attestation)
            add("authenticatorSelection", authenticatorSelection)
        }
        return gson.toJson(json)
    }

    private fun buildAuthenticationRequestJson(options: PasskeyAuthenticationOptions): String {
        val rpId = options.rpId.trim().ifBlank { environment.passkeyRpId.trim() }
        requirePasskeyRpMatch(environment, rpId)
        val json = JsonObject().apply {
            addProperty("challenge", options.challenge)
            addProperty("timeout", options.timeout.toLongOrNull() ?: 300_000L)
            addProperty("rpId", rpId)
            addProperty("userVerification", options.userVerification)
        }
        options.allowCredentials?.takeIf { it.isNotBlank() }?.let {
            json.add("allowCredentials", JsonParser.parseString(it).asJsonArray)
        }
        return gson.toJson(json)
    }

    private fun parseRegistrationResponse(json: String): PasskeyRegistrationPayload {
        val root = JsonParser.parseString(json).asJsonObject
        val response = root.getAsJsonObject("response")
        val transports = response.getAsJsonArray("transports")
            ?.joinToString(",") { it.asString }
            .orEmpty()

        return PasskeyRegistrationPayload(
            credentialRawId = root.getRequiredString("rawId"),
            clientDataJSON = response.getRequiredString("clientDataJSON"),
            attestationObject = response.getRequiredString("attestationObject"),
            transports = transports,
        )
    }

    private fun parseAuthenticationResponse(json: String): PasskeyAuthenticationPayload {
        val root = JsonParser.parseString(json).asJsonObject
        val response = root.getAsJsonObject("response")
        return PasskeyAuthenticationPayload(
            credentialRawId = root.getRequiredString("rawId"),
            clientDataJSON = response.getRequiredString("clientDataJSON"),
            authenticatorData = response.getRequiredString("authenticatorData"),
            signature = response.getRequiredString("signature"),
        )
    }

    private fun mapPasskeyFailure(throwable: Throwable): Throwable {
        if (throwable is IllegalStateException || throwable is IllegalArgumentException) {
            return throwable
        }

        val assetLinksUrl = passkeyAssetLinksUrl(environment.passkeyOriginHint)
        val fallbackMessage = throwable.message ?: "Passkey 操作失败"
        val message = when (throwable) {
            is GetCredentialCancellationException,
            is CreateCredentialCancellationException,
            -> "已取消 Passkey 操作"

            is GetCredentialUnsupportedException,
            is CreateCredentialUnsupportedException,
            -> "当前设备或系统凭据提供方不支持原生 Passkey"

            is GetCredentialProviderConfigurationException,
            is CreateCredentialProviderConfigurationException,
            -> "当前设备没有可用的凭据提供方，请确认 Google Password Manager 已启用"

            is GetPublicKeyCredentialDomException,
            is CreatePublicKeyCredentialDomException,
            -> domExceptionMessage(fallbackMessage, assetLinksUrl)

            is SecurityException ->
                "系统拒绝了当前 Passkey 请求。请确认应用已关联到 ${environment.passkeyRpId.trim()}，并且 $assetLinksUrl 已正确配置包名与签名。"

            else -> {
                if (fallbackMessage.contains("RP ID cannot be validated", ignoreCase = true)) {
                    domExceptionMessage(fallbackMessage, assetLinksUrl)
                } else {
                    fallbackMessage
                }
            }
        }
        return IllegalStateException(message, throwable)
    }

    private fun domExceptionMessage(
        originalMessage: String,
        assetLinksUrl: String?,
    ): String {
        if (originalMessage.contains("RP ID cannot be validated", ignoreCase = true)) {
            return buildString {
                append("RP ID 无法校验。请确认 AndroidManifest 已声明 asset_statements，")
                assetLinksUrl?.let { append(it).append(" 已返回正确的 assetlinks.json，") }
                append("并且其中的包名与签名和当前 APK 完全一致。")
            }
        }
        if (originalMessage.contains("permission", ignoreCase = true)) {
            return "系统拒绝了当前 Passkey 请求。通常是 RP 域、Digital Asset Links 或系统凭据提供方未就绪。"
        }
        return originalMessage
    }
}

private fun JsonObject.getRequiredString(name: String): String {
    return get(name)?.asString ?: error("缺少字段 $name")
}

private fun JsonArray?.joinToString(
    separator: CharSequence,
    transform: (com.google.gson.JsonElement) -> CharSequence,
): String {
    if (this == null) return ""
    return map(transform).joinToString(separator)
}
