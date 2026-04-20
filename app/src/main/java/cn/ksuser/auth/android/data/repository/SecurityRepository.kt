package cn.ksuser.auth.android.data.repository

import cn.ksuser.auth.android.core.network.KsuserApiService
import cn.ksuser.auth.android.core.network.executeEnvelope
import cn.ksuser.auth.android.core.network.requireCode
import cn.ksuser.auth.android.data.model.ChangeEmailRequest
import cn.ksuser.auth.android.data.model.ChangePasswordRequest
import cn.ksuser.auth.android.data.model.DeleteAccountRequest
import cn.ksuser.auth.android.data.model.AccountRecoveryTicket
import cn.ksuser.auth.android.data.model.PasskeyAuthenticationPayload
import cn.ksuser.auth.android.data.model.PasskeyInfo
import cn.ksuser.auth.android.data.model.PasskeyListItem
import cn.ksuser.auth.android.data.model.PasskeyRegistrationOptions
import cn.ksuser.auth.android.data.model.PasskeyRegistrationOptionsRequest
import cn.ksuser.auth.android.data.model.PasskeyRegistrationPayload
import cn.ksuser.auth.android.data.model.PasskeyRegistrationVerifyRequest
import cn.ksuser.auth.android.data.model.PasskeyRenameRequest
import cn.ksuser.auth.android.data.model.SensitiveVerificationStatus
import cn.ksuser.auth.android.data.model.TotpRegistrationOptions
import cn.ksuser.auth.android.data.model.TotpStatus
import cn.ksuser.auth.android.data.model.TotpVerifyResponse
import cn.ksuser.auth.android.data.model.UpdateSettingRequest
import cn.ksuser.auth.android.data.model.UserProfile
import cn.ksuser.auth.android.data.model.UserSettings
import cn.ksuser.auth.android.data.model.VerifySensitiveRequest
import com.google.gson.Gson

class SecurityRepository(
    private val api: KsuserApiService,
    private val gson: Gson,
) {
    suspend fun issueAccountRecoveryTicket(): AccountRecoveryTicket {
        val envelope = executeEnvelope(gson) { api.issueAccountRecoveryTicket() }
        requireCode(envelope, 200)
        return envelope.data ?: error("恢复授权响应为空")
    }

    suspend fun updateBooleanSetting(field: String, value: Boolean): UserSettings {
        val envelope = executeEnvelope(gson) { api.updateSetting(UpdateSettingRequest(field, value = value)) }
        requireCode(envelope, 200)
        return envelope.data ?: error("更新设置失败")
    }

    suspend fun updateStringSetting(field: String, value: String): UserSettings {
        val envelope = executeEnvelope(gson) {
            api.updateSetting(UpdateSettingRequest(field, stringValue = value))
        }
        requireCode(envelope, 200)
        return envelope.data ?: error("更新设置失败")
    }

    suspend fun getTotpStatus(): TotpStatus {
        val envelope = executeEnvelope(gson) { api.getTotpStatus() }
        requireCode(envelope, 200)
        return envelope.data ?: error("TOTP 状态为空")
    }

    suspend fun getTotpRegistrationOptions(): TotpRegistrationOptions {
        val envelope = executeEnvelope(gson) { api.getTotpRegistrationOptions() }
        requireCode(envelope, 200)
        return envelope.data ?: error("TOTP 注册选项为空")
    }

    suspend fun verifyTotpRegistration(
        code: String,
        recoveryCodes: List<String>,
    ) {
        val envelope = executeEnvelope(gson) {
            api.verifyTotpRegistration(
                cn.ksuser.auth.android.data.model.TotpRegistrationVerifyRequest(code, recoveryCodes),
            )
        }
        requireCode(envelope, 200)
    }

    suspend fun verifyTotp(
        code: String? = null,
        recoveryCode: String? = null,
    ): TotpVerifyResponse {
        val envelope = executeEnvelope(gson) {
            api.verifyTotp(cn.ksuser.auth.android.data.model.TotpVerifyRequest(code, recoveryCode))
        }
        requireCode(envelope, 200)
        return envelope.data ?: error("TOTP 验证响应为空")
    }

    suspend fun getRecoveryCodes(): List<String> {
        val envelope = executeEnvelope(gson) { api.getRecoveryCodes() }
        requireCode(envelope, 200)
        return envelope.data.orEmpty()
    }

    suspend fun regenerateRecoveryCodes(): List<String> {
        val envelope = executeEnvelope(gson) { api.regenerateRecoveryCodes() }
        requireCode(envelope, 200)
        return envelope.data.orEmpty()
    }

    suspend fun disableTotp() {
        val envelope = executeEnvelope(gson) { api.disableTotp() }
        requireCode(envelope, 200)
    }

    suspend fun getPasskeyRegistrationOptions(passkeyName: String): PasskeyRegistrationOptions {
        val envelope = executeEnvelope(gson) {
            api.getPasskeyRegistrationOptions(PasskeyRegistrationOptionsRequest(passkeyName))
        }
        requireCode(envelope, 200)
        return envelope.data ?: error("Passkey 注册选项为空")
    }

    suspend fun verifyPasskeyRegistration(
        passkeyName: String,
        payload: PasskeyRegistrationPayload,
    ): PasskeyInfo {
        val envelope = executeEnvelope(gson) {
            api.verifyPasskeyRegistration(
                PasskeyRegistrationVerifyRequest(
                    credentialRawId = payload.credentialRawId,
                    clientDataJSON = payload.clientDataJSON,
                    attestationObject = payload.attestationObject,
                    passkeyName = passkeyName,
                    transports = payload.transports,
                ),
            )
        }
        requireCode(envelope, 200)
        return envelope.data ?: error("Passkey 注册返回为空")
    }

    suspend fun getPasskeyList(): List<PasskeyListItem> {
        val envelope = executeEnvelope(gson) { api.getPasskeyList() }
        requireCode(envelope, 200)
        return envelope.data?.passkeys.orEmpty()
    }

    suspend fun renamePasskey(passkeyId: Long, newName: String) {
        val envelope = executeEnvelope(gson) {
            api.renamePasskey(passkeyId, PasskeyRenameRequest(newName))
        }
        requireCode(envelope, 200)
    }

    suspend fun deletePasskey(passkeyId: Long) {
        val envelope = executeEnvelope(gson) { api.deletePasskey(passkeyId) }
        requireCode(envelope, 200)
    }

    suspend fun getSensitiveVerificationStatus(): SensitiveVerificationStatus {
        val envelope = executeEnvelope(gson) { api.checkSensitiveVerification() }
        requireCode(envelope, 200)
        return envelope.data ?: error("敏感验证状态为空")
    }

    suspend fun sendSensitiveCode() {
        val envelope = executeEnvelope(gson) { api.sendCode(cn.ksuser.auth.android.data.model.SendCodeRequest(type = "sensitive-verification")) }
        requireCode(envelope, 200)
    }

    suspend fun sendChangeEmailCode(email: String) {
        val envelope = executeEnvelope(gson) { api.sendCode(cn.ksuser.auth.android.data.model.SendCodeRequest(email = email, type = "change-email")) }
        requireCode(envelope, 200)
    }

    suspend fun verifySensitivePassword(password: String) {
        val envelope = executeEnvelope(gson) { api.verifySensitive(VerifySensitiveRequest(method = "password", password = password)) }
        requireCode(envelope, 200)
    }

    suspend fun verifySensitiveEmailCode(code: String) {
        val envelope = executeEnvelope(gson) { api.verifySensitive(VerifySensitiveRequest(method = "email-code", code = code)) }
        requireCode(envelope, 200)
    }

    suspend fun verifySensitiveTotp(code: String? = null, recoveryCode: String? = null) {
        val envelope = executeEnvelope(gson) {
            api.verifySensitive(
                VerifySensitiveRequest(method = "totp", code = code, recoveryCode = recoveryCode),
            )
        }
        requireCode(envelope, 200)
    }

    suspend fun getPasskeySensitiveVerificationOptions() = executeEnvelope(gson) {
        api.getPasskeySensitiveVerificationOptions()
    }.also { requireCode(it, 200) }.data ?: error("敏感 Passkey 选项为空")

    suspend fun verifySensitivePasskey(
        challengeId: String,
        payload: PasskeyAuthenticationPayload,
    ) {
        val envelope = executeEnvelope(gson) {
            api.verifyPasskeySensitiveVerification(
                challengeId = challengeId,
                request = cn.ksuser.auth.android.data.model.PasskeyAuthenticationVerifyRequest(
                    credentialRawId = payload.credentialRawId,
                    clientDataJSON = payload.clientDataJSON,
                    authenticatorData = payload.authenticatorData,
                    signature = payload.signature,
                ),
            )
        }
        requireCode(envelope, 200)
    }

    suspend fun changeEmail(
        newEmail: String,
        code: String,
    ): UserProfile {
        val envelope = executeEnvelope(gson) { api.changeEmail(ChangeEmailRequest(newEmail, code)) }
        requireCode(envelope, 200)
        return envelope.data ?: error("更新邮箱失败")
    }

    suspend fun changePassword(newPassword: String) {
        val envelope = executeEnvelope(gson) { api.changePassword(ChangePasswordRequest(newPassword)) }
        requireCode(envelope, 200)
    }

    suspend fun deleteAccount(confirmText: String) {
        val envelope = executeEnvelope(gson) { api.deleteAccount(DeleteAccountRequest(confirmText)) }
        requireCode(envelope, 200)
    }
}
