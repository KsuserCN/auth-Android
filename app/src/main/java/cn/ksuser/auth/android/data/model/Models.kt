package cn.ksuser.auth.android.data.model

import com.google.gson.JsonObject

data class CsrfTokenResponse(
    val csrfToken: String = "",
)

data class PasswordRequirement(
    val minLength: Int,
    val maxLength: Int,
    val requireUppercase: Boolean,
    val requireLowercase: Boolean,
    val requireDigits: Boolean,
    val requireSpecialChars: Boolean,
    val rejectCommonWeakPasswords: Boolean = true,
    val requirementMessage: String = "",
)

data class CheckUsernameResponse(
    val exists: Boolean,
)

data class UserSettings(
    val mfaEnabled: Boolean = false,
    val detectUnusualLogin: Boolean = true,
    val notifySensitiveActionEmail: Boolean = true,
    val subscribeNewsEmail: Boolean = false,
    val preferredMfaMethod: String? = null,
    val preferredSensitiveMethod: String? = null,
)

data class UserProfile(
    val uuid: String,
    val username: String,
    val email: String,
    val avatarUrl: String? = null,
    val realName: String? = null,
    val gender: String? = null,
    val birthDate: String? = null,
    val region: String? = null,
    val bio: String? = null,
    val verificationType: String? = null,
    val updatedAt: String? = null,
    val settings: UserSettings? = null,
)

data class TokenPayload(
    val accessToken: String,
)

data class RegisterResponse(
    val uuid: String,
    val username: String,
    val email: String,
    val accessToken: String,
    val createdAt: String,
)

data class MfaChallengePayload(
    val challengeId: String,
    val method: String,
    val methods: List<String> = emptyList(),
)

enum class AuthSource {
    PASSWORD,
    EMAIL_CODE,
    PASSKEY,
}

sealed interface AuthResult {
    data class Success(
        val accessToken: String,
    ) : AuthResult

    data class NeedsMfa(
        val challengeId: String,
        val method: String,
        val methods: List<String>,
        val source: AuthSource,
    ) : AuthResult
}

data class PasskeyAuthenticationOptions(
    val challenge: String,
    val challengeId: String,
    val timeout: String,
    val rpId: String,
    val userVerification: String,
    val allowCredentials: String? = null,
)

data class PasskeyRegistrationOptions(
    val challenge: String,
    val rp: String,
    val user: String,
    val pubKeyCredParams: String,
    val timeout: String,
    val attestation: String,
    val authenticatorSelection: String,
)

data class PasskeyInfo(
    val passkeyId: Long,
    val passkeyName: String,
    val createdAt: String,
)

data class PasskeyListItem(
    val id: Long,
    val name: String,
    val transports: String,
    val lastUsedAt: String?,
    val createdAt: String,
)

data class PasskeyListResponse(
    val passkeys: List<PasskeyListItem>,
)

sealed interface PasskeyAvailability {
    data object Available : PasskeyAvailability
    data object UnsupportedDevice : PasskeyAvailability
    data object MissingProvider : PasskeyAvailability
    data object RpMismatch : PasskeyAvailability
    data object DomainNotReady : PasskeyAvailability
}

data class PasskeyRegistrationPayload(
    val credentialRawId: String,
    val clientDataJSON: String,
    val attestationObject: String,
    val transports: String,
)

data class PasskeyAuthenticationPayload(
    val credentialRawId: String,
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
)

data class TotpStatus(
    val enabled: Boolean,
    val recoveryCodesCount: Long,
)

data class TotpRegistrationOptions(
    val secret: String,
    val qrCodeUrl: String,
    val recoveryCodes: List<String>,
)

data class TotpVerifyResponse(
    val success: Boolean,
    val message: String,
)

data class SensitiveVerificationStatus(
    val verified: Boolean,
    val remainingSeconds: Long,
    val preferredMethod: String? = null,
    val methods: List<String> = emptyList(),
)

data class AdaptiveAuthStatus(
    val sessionId: Long? = null,
    val riskScore: Int = 0,
    val riskLevel: String = "low",
    val policyDecision: String = "ALLOW",
    val policyVersion: String = "1.0.0",
    val trusted: Boolean = false,
    val requiresStepUp: Boolean = false,
    val sessionFrozen: Boolean = false,
    val sensitiveVerified: Boolean = false,
    val sensitiveVerificationRemainingSeconds: Long = 0,
    val authAgeSeconds: Long = 0,
    val idleSeconds: Long = 0,
    val currentIp: String? = null,
    val currentLocation: String? = null,
    val sessionIp: String? = null,
    val sessionLocation: String? = null,
    val browser: String? = null,
    val deviceType: String? = null,
    val multiEndpointAlert: Boolean = false,
    val alertLevel: String? = null,
    val alertTitle: String? = null,
    val alertMessage: String? = null,
    val alertRemainingSeconds: Long = 0,
    val recommendedAction: String = "",
    val reasons: List<String> = emptyList(),
)

data class SessionItem(
    val id: Long,
    val ipAddress: String,
    val ipLocation: String?,
    val userAgent: String?,
    val browser: String?,
    val deviceType: String?,
    val createdAt: String,
    val lastSeenAt: String,
    val expiresAt: String,
    val revokedAt: String?,
    val online: Boolean,
    val current: Boolean,
)

data class SensitiveLogItem(
    val id: Long,
    val operationType: String,
    val loginMethod: String? = null,
    val loginMethods: List<String> = emptyList(),
    val ipAddress: String,
    val ipLocation: String? = null,
    val browser: String? = null,
    val deviceType: String? = null,
    val result: String,
    val failureReason: String? = null,
    val riskScore: Int = 0,
    val actionTaken: String? = null,
    val triggeredMultiErrorLock: Boolean = false,
    val triggeredRateLimitLock: Boolean = false,
    val durationMs: Int = 0,
    val createdAt: String,
)

data class PaginatedSensitiveLogs(
    val data: List<SensitiveLogItem>,
    val page: Int,
    val pageSize: Int,
    val total: Long,
    val totalPages: Int,
)

data class PasswordLoginRequest(
    val email: String,
    val password: String,
)

data class LoginWithCodeRequest(
    val email: String,
    val code: String,
)

data class SendCodeRequest(
    val email: String? = null,
    val type: String,
)

data class QrApproveRequest(
    val approveCode: String,
)

data class QrScanPreview(
    val codeType: String,
    val clientName: String? = null,
    val browser: String? = null,
    val system: String? = null,
    val ipAddress: String? = null,
    val ipLocation: String? = null,
    val expiresInSeconds: Long = 0,
)

data class AccountRecoveryTicket(
    val recoveryCode: String,
    val expiresInSeconds: Long,
    val username: String,
    val maskedEmail: String,
    val sponsorClientName: String,
    val sponsorBrowser: String,
    val sponsorSystem: String,
    val sponsorIpLocation: String,
)

data class RegisterRequest(
    val username: String,
    val email: String,
    val password: String,
    val code: String,
)

data class PasskeyRegistrationOptionsRequest(
    val passkeyName: String,
    val authenticatorType: String = "auto",
)

data class PasskeyRegistrationVerifyRequest(
    val credentialRawId: String,
    val clientDataJSON: String,
    val attestationObject: String,
    val passkeyName: String,
    val transports: String,
)

data class PasskeyAuthenticationVerifyRequest(
    val credentialRawId: String,
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
)

data class PasskeyMfaVerifyRequest(
    val mfaChallengeId: String,
    val passkeyChallengeId: String,
    val credentialRawId: String,
    val clientDataJSON: String,
    val authenticatorData: String,
    val signature: String,
)

data class TotpMfaVerifyRequest(
    val challengeId: String,
    val code: String? = null,
    val recoveryCode: String? = null,
)

data class SessionTransferExchangeRequest(
    val transferCode: String,
    val target: String,
)

data class UpdateSettingRequest(
    val field: String,
    val value: Boolean? = null,
    val stringValue: String? = null,
)

data class UpdateProfileRequest(
    val key: String,
    val value: String,
)

data class VerifySensitiveRequest(
    val method: String,
    val password: String? = null,
    val code: String? = null,
    val recoveryCode: String? = null,
)

data class ChangeEmailRequest(
    val newEmail: String,
    val code: String,
)

data class ChangePasswordRequest(
    val newPassword: String,
)

data class DeleteAccountRequest(
    val confirmText: String,
)

data class TotpRegistrationVerifyRequest(
    val code: String,
    val recoveryCodes: List<String>,
)

data class TotpVerifyRequest(
    val code: String? = null,
    val recoveryCode: String? = null,
)

data class PasskeyRenameRequest(
    val newName: String,
)

data class AuthResponseData(
    val raw: JsonObject,
)
