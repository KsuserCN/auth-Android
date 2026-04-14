package cn.ksuser.auth.android.data.repository

import cn.ksuser.auth.android.core.network.ApiEnvelope
import cn.ksuser.auth.android.core.network.ApiException
import cn.ksuser.auth.android.core.network.KsuserApiService
import cn.ksuser.auth.android.core.network.executeEnvelope
import cn.ksuser.auth.android.core.network.requireCode
import cn.ksuser.auth.android.core.session.SessionRepository
import cn.ksuser.auth.android.data.model.AuthResult
import cn.ksuser.auth.android.data.model.AuthSource
import cn.ksuser.auth.android.data.model.LoginWithCodeRequest
import cn.ksuser.auth.android.data.model.PasskeyAuthenticationPayload
import cn.ksuser.auth.android.data.model.PasskeyAuthenticationVerifyRequest
import cn.ksuser.auth.android.data.model.PasswordLoginRequest
import cn.ksuser.auth.android.data.model.PasswordRequirement
import cn.ksuser.auth.android.data.model.RegisterRequest
import cn.ksuser.auth.android.data.model.RegisterResponse
import cn.ksuser.auth.android.data.model.SendCodeRequest
import cn.ksuser.auth.android.data.model.TotpMfaVerifyRequest
import cn.ksuser.auth.android.data.model.UserProfile
import com.google.gson.Gson
import com.google.gson.JsonArray
import com.google.gson.JsonObject

class AuthRepository(
    private val api: KsuserApiService,
    private val sessionRepository: SessionRepository,
    private val gson: Gson,
) {
    suspend fun getPasswordRequirement(): PasswordRequirement {
        val envelope = executeEnvelope(gson) { api.getPasswordRequirement() }
        requireCode(envelope, 200)
        return envelope.data ?: error("缺少密码策略")
    }

    suspend fun checkUsernameAvailable(username: String): Boolean {
        val envelope = executeEnvelope(gson) { api.checkUsername(username) }
        requireCode(envelope, 200)
        return envelope.data?.exists == false
    }

    suspend fun sendLoginCode(email: String) {
        val envelope = executeEnvelope(gson) { api.sendCode(SendCodeRequest(email = email, type = "login")) }
        requireCode(envelope, 200)
    }

    suspend fun sendRegisterCode(email: String) {
        val envelope = executeEnvelope(gson) { api.sendCode(SendCodeRequest(email = email, type = "register")) }
        requireCode(envelope, 200)
    }

    suspend fun passwordLogin(email: String, password: String): AuthResult {
        val envelope = executeEnvelope(gson) { api.login(PasswordLoginRequest(email, password)) }
        return parseAuthEnvelope(envelope, AuthSource.PASSWORD)
    }

    suspend fun loginWithCode(email: String, code: String): AuthResult {
        val envelope = executeEnvelope(gson) { api.loginWithCode(LoginWithCodeRequest(email, code)) }
        return parseAuthEnvelope(envelope, AuthSource.EMAIL_CODE)
    }

    suspend fun register(
        username: String,
        email: String,
        password: String,
        code: String,
    ): RegisterResponse {
        val envelope = executeEnvelope(gson) { api.register(RegisterRequest(username, email, password, code)) }
        requireCode(envelope, 200)
        val response = envelope.data ?: error("注册响应为空")
        sessionRepository.persistAccessToken(response.accessToken)
        return response
    }

    suspend fun getCurrentUser(type: String = "details"): UserProfile {
        val envelope = executeEnvelope(gson) { api.getUserInfo(type) }
        requireCode(envelope, 200)
        return envelope.data ?: error("用户信息为空")
    }

    suspend fun logout() {
        try {
            val envelope = executeEnvelope(gson) { api.logout() }
            requireCode(envelope, 200)
        } finally {
            sessionRepository.clearSession()
        }
    }

    suspend fun logoutAll() {
        try {
            val envelope = executeEnvelope(gson) { api.logoutAll() }
            requireCode(envelope, 200)
        } finally {
            sessionRepository.clearSession()
        }
    }

    suspend fun verifyTotpMfa(
        challengeId: String,
        code: String? = null,
        recoveryCode: String? = null,
    ): String {
        val envelope = executeEnvelope(gson) {
            api.verifyTotpMfa(TotpMfaVerifyRequest(challengeId, code, recoveryCode))
        }
        requireCode(envelope, 200)
        return envelope.data?.accessToken
            ?.also(sessionRepository::persistAccessToken)
            ?: error("MFA AccessToken 缺失")
    }

    suspend fun getPasskeyAuthenticationOptions() = executeEnvelope(gson) {
        api.getPasskeyAuthenticationOptions()
    }.also { requireCode(it, 200) }.data ?: error("缺少 Passkey 认证选项")

    suspend fun verifyPasskeyLogin(
        challengeId: String,
        payload: PasskeyAuthenticationPayload,
    ): AuthResult {
        val envelope = executeEnvelope(gson) {
            api.verifyPasskeyAuthentication(
                challengeId = challengeId,
                request = PasskeyAuthenticationVerifyRequest(
                    credentialRawId = payload.credentialRawId,
                    clientDataJSON = payload.clientDataJSON,
                    authenticatorData = payload.authenticatorData,
                    signature = payload.signature,
                ),
            )
        }
        return parseAuthEnvelope(envelope, AuthSource.PASSKEY)
    }

    suspend fun verifyPasskeyMfa(
        mfaChallengeId: String,
        passkeyChallengeId: String,
        payload: PasskeyAuthenticationPayload,
    ): String {
        val envelope = executeEnvelope(gson) {
            api.verifyPasskeyMfa(
                cn.ksuser.auth.android.data.model.PasskeyMfaVerifyRequest(
                    mfaChallengeId = mfaChallengeId,
                    passkeyChallengeId = passkeyChallengeId,
                    credentialRawId = payload.credentialRawId,
                    clientDataJSON = payload.clientDataJSON,
                    authenticatorData = payload.authenticatorData,
                    signature = payload.signature,
                ),
            )
        }
        requireCode(envelope, 200)
        return envelope.data?.accessToken
            ?.also(sessionRepository::persistAccessToken)
            ?: error("Passkey MFA AccessToken 缺失")
    }

    private fun parseAuthEnvelope(
        envelope: ApiEnvelope<JsonObject>,
        source: AuthSource,
    ): AuthResult {
        val code = envelope.code ?: throw ApiException(500, "缺少状态码")
        val payload = envelope.data ?: JsonObject()
        if (code == 201 || payload.has("challengeId")) {
            val methods = payload.getAsJsonArray("methods").toStringList()
            return AuthResult.NeedsMfa(
                challengeId = payload.get("challengeId").asString,
                method = payload.get("method").asString,
                methods = methods.ifEmpty { listOf(payload.get("method").asString) },
                source = source,
            )
        }
        if (code == 200 && payload.has("accessToken")) {
            val token = payload.get("accessToken").asString
            sessionRepository.persistAccessToken(token)
            return AuthResult.Success(token)
        }
        throw ApiException(code, envelope.msg ?: "认证失败")
    }
}

private fun JsonArray?.toStringList(): List<String> {
    if (this == null) return emptyList()
    return mapNotNull { element -> element?.takeIf { !it.isJsonNull }?.asString }
}
