package cn.ksuser.auth.android.core.network

import cn.ksuser.auth.android.data.model.ChangeEmailRequest
import cn.ksuser.auth.android.data.model.ChangePasswordRequest
import cn.ksuser.auth.android.data.model.CheckUsernameResponse
import cn.ksuser.auth.android.data.model.CsrfTokenResponse
import cn.ksuser.auth.android.data.model.DeleteAccountRequest
import cn.ksuser.auth.android.data.model.AdaptiveAuthStatus
import cn.ksuser.auth.android.data.model.LoginWithCodeRequest
import cn.ksuser.auth.android.data.model.PasskeyAuthenticationOptions
import cn.ksuser.auth.android.data.model.PasskeyAuthenticationVerifyRequest
import cn.ksuser.auth.android.data.model.PasskeyInfo
import cn.ksuser.auth.android.data.model.PasskeyListResponse
import cn.ksuser.auth.android.data.model.PasskeyMfaVerifyRequest
import cn.ksuser.auth.android.data.model.PasskeyRegistrationOptions
import cn.ksuser.auth.android.data.model.PasskeyRegistrationOptionsRequest
import cn.ksuser.auth.android.data.model.PasskeyRegistrationVerifyRequest
import cn.ksuser.auth.android.data.model.PasskeyRenameRequest
import cn.ksuser.auth.android.data.model.PaginatedSensitiveLogs
import cn.ksuser.auth.android.data.model.PasswordLoginRequest
import cn.ksuser.auth.android.data.model.PasswordRequirement
import cn.ksuser.auth.android.data.model.AccountRecoveryTicket
import cn.ksuser.auth.android.data.model.QrApproveRequest
import cn.ksuser.auth.android.data.model.QrScanPreview
import cn.ksuser.auth.android.data.model.RegisterRequest
import cn.ksuser.auth.android.data.model.RegisterResponse
import cn.ksuser.auth.android.data.model.SendCodeRequest
import cn.ksuser.auth.android.data.model.SessionTransferExchangeRequest
import cn.ksuser.auth.android.data.model.SessionItem
import cn.ksuser.auth.android.data.model.SensitiveVerificationStatus
import cn.ksuser.auth.android.data.model.TokenPayload
import cn.ksuser.auth.android.data.model.TotpMfaVerifyRequest
import cn.ksuser.auth.android.data.model.TotpRegistrationOptions
import cn.ksuser.auth.android.data.model.TotpRegistrationVerifyRequest
import cn.ksuser.auth.android.data.model.TotpStatus
import cn.ksuser.auth.android.data.model.TotpVerifyRequest
import cn.ksuser.auth.android.data.model.TotpVerifyResponse
import cn.ksuser.auth.android.data.model.UpdateProfileRequest
import cn.ksuser.auth.android.data.model.UpdateSettingRequest
import cn.ksuser.auth.android.data.model.UserProfile
import cn.ksuser.auth.android.data.model.UserSettings
import cn.ksuser.auth.android.data.model.VerifySensitiveRequest
import com.google.gson.JsonObject
import okhttp3.MultipartBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.DELETE
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.PUT
import retrofit2.http.Part
import retrofit2.http.Path
import retrofit2.http.Query

interface KsuserApiService {
    @GET("/auth/csrf-token")
    suspend fun getCsrfToken(): Response<ApiEnvelope<CsrfTokenResponse>>

    @GET("/info/password-requirement")
    suspend fun getPasswordRequirement(): Response<ApiEnvelope<PasswordRequirement>>

    @GET("/auth/check-username")
    suspend fun checkUsername(
        @Query("username") username: String,
    ): Response<ApiEnvelope<CheckUsernameResponse>>

    @POST("/auth/send-code")
    suspend fun sendCode(
        @Body request: SendCodeRequest,
    ): Response<ApiEnvelope<Unit>>

    @POST("/auth/register")
    suspend fun register(
        @Body request: RegisterRequest,
    ): Response<ApiEnvelope<RegisterResponse>>

    @POST("/auth/login")
    suspend fun login(
        @Body request: PasswordLoginRequest,
    ): Response<ApiEnvelope<JsonObject>>

    @POST("/auth/login-with-code")
    suspend fun loginWithCode(
        @Body request: LoginWithCodeRequest,
    ): Response<ApiEnvelope<JsonObject>>

    @POST("/auth/passkey/authentication-options")
    suspend fun getPasskeyAuthenticationOptions(): Response<ApiEnvelope<PasskeyAuthenticationOptions>>

    @POST("/auth/passkey/authentication-verify")
    suspend fun verifyPasskeyAuthentication(
        @Query("challengeId") challengeId: String,
        @Body request: PasskeyAuthenticationVerifyRequest,
    ): Response<ApiEnvelope<JsonObject>>

    @POST("/auth/passkey/mfa-verify")
    suspend fun verifyPasskeyMfa(
        @Body request: PasskeyMfaVerifyRequest,
    ): Response<ApiEnvelope<TokenPayload>>

    @POST("/auth/totp/mfa-verify")
    suspend fun verifyTotpMfa(
        @Body request: TotpMfaVerifyRequest,
    ): Response<ApiEnvelope<TokenPayload>>

    @POST("/auth/refresh")
    suspend fun refresh(): Response<ApiEnvelope<TokenPayload>>

    @POST("/auth/logout")
    suspend fun logout(): Response<ApiEnvelope<Unit>>

    @POST("/auth/session-transfer/exchange")
    suspend fun exchangeSessionTransfer(
        @Body request: SessionTransferExchangeRequest,
    ): Response<ApiEnvelope<TokenPayload>>

    @POST("/auth/qr/approve")
    suspend fun approveQrChallenge(
        @Body request: QrApproveRequest,
    ): Response<ApiEnvelope<Unit>>

    @POST("/auth/account-recovery/issue")
    suspend fun issueAccountRecoveryTicket(): Response<ApiEnvelope<AccountRecoveryTicket>>

    @GET("/auth/qr/preview")
    suspend fun getQrScanPreview(
        @Query("approveCode") approveCode: String? = null,
        @Query("transferCode") transferCode: String? = null,
    ): Response<ApiEnvelope<QrScanPreview>>

    @POST("/auth/logout/all")
    suspend fun logoutAll(): Response<ApiEnvelope<Unit>>

    @GET("/auth/info")
    suspend fun getUserInfo(
        @Query("type") type: String = "details",
    ): Response<ApiEnvelope<UserProfile>>

    @POST("/auth/update/setting")
    suspend fun updateSetting(
        @Body request: UpdateSettingRequest,
    ): Response<ApiEnvelope<UserSettings>>

    @POST("/auth/update/profile")
    suspend fun updateProfile(
        @Body request: UpdateProfileRequest,
    ): Response<ApiEnvelope<UserProfile>>

    @Multipart
    @POST("/auth/upload/avatar")
    suspend fun uploadAvatar(
        @Part file: MultipartBody.Part,
    ): Response<ApiEnvelope<UserProfile>>

    @GET("/auth/check-sensitive-verification")
    suspend fun checkSensitiveVerification(): Response<ApiEnvelope<SensitiveVerificationStatus>>

    @GET("/auth/adaptive-auth/status")
    suspend fun getAdaptiveAuthStatus(): Response<ApiEnvelope<AdaptiveAuthStatus>>

    @POST("/auth/verify-sensitive")
    suspend fun verifySensitive(
        @Body request: VerifySensitiveRequest,
    ): Response<ApiEnvelope<Unit>>

    @POST("/auth/update/email")
    suspend fun changeEmail(
        @Body request: ChangeEmailRequest,
    ): Response<ApiEnvelope<UserProfile>>

    @POST("/auth/update/password")
    suspend fun changePassword(
        @Body request: ChangePasswordRequest,
    ): Response<ApiEnvelope<Unit>>

    @POST("/auth/delete")
    suspend fun deleteAccount(
        @Body request: DeleteAccountRequest,
    ): Response<ApiEnvelope<Unit>>

    @GET("/auth/sessions")
    suspend fun getSessions(): Response<ApiEnvelope<List<SessionItem>>>

    @POST("/auth/sessions/{sessionId}/revoke")
    suspend fun revokeSession(
        @Path("sessionId") sessionId: Long,
    ): Response<ApiEnvelope<Unit>>

    @GET("/auth/sensitive-logs")
    suspend fun getSensitiveLogs(
        @Query("page") page: Int?,
        @Query("pageSize") pageSize: Int?,
        @Query("startDate") startDate: String?,
        @Query("endDate") endDate: String?,
        @Query("operationType") operationType: String?,
        @Query("result") result: String?,
    ): Response<ApiEnvelope<PaginatedSensitiveLogs>>

    @GET("/auth/totp/status")
    suspend fun getTotpStatus(): Response<ApiEnvelope<TotpStatus>>

    @POST("/auth/totp/registration-options")
    suspend fun getTotpRegistrationOptions(): Response<ApiEnvelope<TotpRegistrationOptions>>

    @POST("/auth/totp/registration-verify")
    suspend fun verifyTotpRegistration(
        @Body request: TotpRegistrationVerifyRequest,
    ): Response<ApiEnvelope<Unit>>

    @POST("/auth/totp/verify")
    suspend fun verifyTotp(
        @Body request: TotpVerifyRequest,
    ): Response<ApiEnvelope<TotpVerifyResponse>>

    @GET("/auth/totp/recovery-codes")
    suspend fun getRecoveryCodes(): Response<ApiEnvelope<List<String>>>

    @POST("/auth/totp/recovery-codes/regenerate")
    suspend fun regenerateRecoveryCodes(): Response<ApiEnvelope<List<String>>>

    @POST("/auth/totp/disable")
    suspend fun disableTotp(): Response<ApiEnvelope<Unit>>

    @POST("/auth/passkey/registration-options")
    suspend fun getPasskeyRegistrationOptions(
        @Body request: PasskeyRegistrationOptionsRequest,
    ): Response<ApiEnvelope<PasskeyRegistrationOptions>>

    @POST("/auth/passkey/registration-verify")
    suspend fun verifyPasskeyRegistration(
        @Body request: PasskeyRegistrationVerifyRequest,
    ): Response<ApiEnvelope<PasskeyInfo>>

    @POST("/auth/passkey/sensitive-verification-options")
    suspend fun getPasskeySensitiveVerificationOptions(): Response<ApiEnvelope<PasskeyAuthenticationOptions>>

    @POST("/auth/passkey/sensitive-verification-verify")
    suspend fun verifyPasskeySensitiveVerification(
        @Query("challengeId") challengeId: String,
        @Body request: PasskeyAuthenticationVerifyRequest,
    ): Response<ApiEnvelope<Unit>>

    @GET("/auth/passkey/list")
    suspend fun getPasskeyList(): Response<ApiEnvelope<PasskeyListResponse>>

    @DELETE("/auth/passkey/{passkeyId}")
    suspend fun deletePasskey(
        @Path("passkeyId") passkeyId: Long,
    ): Response<ApiEnvelope<Unit>>

    @PUT("/auth/passkey/{passkeyId}/rename")
    suspend fun renamePasskey(
        @Path("passkeyId") passkeyId: Long,
        @Body request: PasskeyRenameRequest,
    ): Response<ApiEnvelope<Unit>>
}
