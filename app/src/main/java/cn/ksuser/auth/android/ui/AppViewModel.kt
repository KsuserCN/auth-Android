package cn.ksuser.auth.android.ui

import android.app.Activity
import android.net.Uri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.ksuser.auth.android.core.network.ApiException
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.data.model.AuthResult
import cn.ksuser.auth.android.data.model.AuthSource
import cn.ksuser.auth.android.data.model.PasswordRequirement
import cn.ksuser.auth.android.data.model.QrScanPreview
import cn.ksuser.auth.android.data.model.UserProfile
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppUiState(
    val isBootstrapping: Boolean = true,
    val isBusy: Boolean = false,
    val isAuthenticated: Boolean = false,
    val currentUser: UserProfile? = null,
    val passwordRequirement: PasswordRequirement? = null,
    val pendingMfa: AuthResult.NeedsMfa? = null,
    val pendingMobileBridgeConfirmation: PendingMobileBridgeConfirmation? = null,
    val mobileBridgeReturnUrl: String? = null,
    val pendingQrConfirmation: PendingQrConfirmation? = null,
    val message: String? = null,
    val error: String? = null,
)

data class PendingMobileBridgeConfirmation(
    val challengeId: String,
    val returnUrl: String? = null,
    val returnOrigin: String? = null,
    val requiresLogin: Boolean = false,
)

data class PendingQrConfirmation(
    val type: QrConfirmationType,
    val code: String,
    val preview: QrScanPreview? = null,
    val operationHint: String? = null,
)

enum class QrConfirmationType {
    APPROVE_LOGIN,
    APPROVE_MFA,
    APPROVE_SENSITIVE,
    APPROVE_RECOVERY,
    LOGIN_THIS_PHONE,
    SWITCH_AND_LOGIN_THIS_PHONE,
}

class AppViewModel(
    private val container: AppContainer,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppUiState())
    val uiState: StateFlow<AppUiState> = _uiState.asStateFlow()

    init {
        bootstrap()
    }

    fun bootstrap() {
        viewModelScope.launch {
            _uiState.update { it.copy(isBootstrapping = true, error = null) }
            runCatching {
                val token = container.sessionRepository.bootstrap()
                val requirement = container.authRepository.getPasswordRequirement()
                if (token.isNullOrBlank()) {
                    _uiState.update {
                        it.copy(
                            isBootstrapping = false,
                            isAuthenticated = false,
                            pendingMobileBridgeConfirmation = null,
                            mobileBridgeReturnUrl = null,
                            pendingQrConfirmation = null,
                            passwordRequirement = requirement,
                        )
                    }
                } else {
                    val user = container.authRepository.getCurrentUser()
                    _uiState.update {
                        it.copy(
                            isBootstrapping = false,
                            isAuthenticated = true,
                            currentUser = user,
                            pendingMobileBridgeConfirmation = null,
                            mobileBridgeReturnUrl = null,
                            pendingQrConfirmation = null,
                            passwordRequirement = requirement,
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isBootstrapping = false,
                        isAuthenticated = false,
                        pendingMobileBridgeConfirmation = null,
                        mobileBridgeReturnUrl = null,
                        pendingQrConfirmation = null,
                        error = throwable.toReadableMessage(),
                    )
                }
            }
        }
    }

    fun clearTransientMessages() {
        _uiState.update { it.copy(message = null, error = null) }
    }

    fun consumeMobileBridgeReturnUrl() {
        _uiState.update { it.copy(mobileBridgeReturnUrl = null) }
    }

    fun dismissMobileBridgeForLogin() {
        _uiState.update {
            it.copy(
                pendingMobileBridgeConfirmation = null,
                message = "请先在 App 内登录，然后回到浏览器重新发起网页登录",
            )
        }
    }

    fun refreshCurrentUser() {
        viewModelScope.launch {
            runCatching {
                val user = container.authRepository.getCurrentUser()
                _uiState.update { state -> state.copy(isAuthenticated = true, currentUser = user) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(error = throwable.toReadableMessage()) }
            }
        }
    }

    fun handleIncomingDeepLink(uri: Uri) {
        val path = uri.path?.trim().orEmpty()
        if (path != "/app/bridge-login") {
            return
        }

        val challengeId = uri.getQueryParameter("challengeId")?.trim().orEmpty()
        if (challengeId.isBlank()) {
            _uiState.update { it.copy(error = "网页登录挑战参数缺失") }
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching {
                val status = container.authRepository.getMobileBridgeStatus(challengeId)
                when (status.status.lowercase()) {
                    "pending" -> PendingMobileBridgeConfirmation(
                        challengeId = challengeId,
                        returnUrl = status.returnUrl,
                        returnOrigin = status.returnOrigin,
                        requiresLogin = !_uiState.value.isAuthenticated,
                    )
                    "approved" -> {
                        if (!status.returnUrl.isNullOrBlank()) {
                            _uiState.update {
                                it.copy(
                                    isBusy = false,
                                    pendingMobileBridgeConfirmation = null,
                                    mobileBridgeReturnUrl = status.returnUrl,
                                    message = "网页登录已确认，正在返回浏览器",
                                )
                            }
                        } else {
                            _uiState.update { it.copy(isBusy = false, error = "网页登录已完成，请返回浏览器") }
                        }
                        null
                    }
                    "cancelled" -> {
                        _uiState.update { it.copy(isBusy = false, error = "当前网页登录请求已取消") }
                        null
                    }
                    else -> {
                        _uiState.update { it.copy(isBusy = false, error = "当前网页登录请求已过期，请回到浏览器重试") }
                        null
                    }
                }
            }.onSuccess { pending ->
                if (pending != null) {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingMobileBridgeConfirmation = pending,
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
            }
        }
    }

    fun confirmMobileBridgeAction() {
        val pending = _uiState.value.pendingMobileBridgeConfirmation ?: return
        if (pending.requiresLogin) {
            dismissMobileBridgeForLogin()
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching { container.authRepository.approveMobileBridge(pending.challengeId) }
                .onSuccess { response ->
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingMobileBridgeConfirmation = null,
                            mobileBridgeReturnUrl = response.returnUrl,
                            message = "网页登录已确认，正在返回浏览器",
                        )
                    }
                }
                .onFailure { throwable ->
                    val readable = if (throwable is ApiException && throwable.statusCode == 401) {
                        "请先在手机端登录"
                    } else {
                        throwable.toReadableMessage()
                    }
                    _uiState.update { it.copy(isBusy = false, error = readable) }
                }
        }
    }

    fun cancelMobileBridgeAction() {
        val pending = _uiState.value.pendingMobileBridgeConfirmation
        if (pending == null) {
            return
        }

        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching { container.authRepository.cancelMobileBridge(pending.challengeId) }
                .onSuccess {
                    _uiState.update {
                        it.copy(
                            isBusy = false,
                            pendingMobileBridgeConfirmation = null,
                            message = "网页登录请求已取消",
                        )
                    }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
                }
        }
    }

    fun passwordLogin(email: String, password: String) {
        runAuthAction {
            container.authRepository.passwordLogin(email, password)
        }
    }

    fun sendLoginCode(email: String) {
        runBusyAction(successMessage = "验证码已发送") {
            container.authRepository.sendLoginCode(email)
        }
    }

    fun loginWithCode(email: String, code: String) {
        runAuthAction {
            container.authRepository.loginWithCode(email, code)
        }
    }

    fun sendRegisterCode(email: String) {
        runBusyAction(successMessage = "注册验证码已发送") {
            container.authRepository.sendRegisterCode(email)
        }
    }

    fun register(
        username: String,
        email: String,
        password: String,
        code: String,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching {
                container.authRepository.register(username, email, password, code)
                val user = container.authRepository.getCurrentUser()
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isAuthenticated = true,
                        currentUser = user,
                        message = "注册成功",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        error = throwable.toReadableMessage(),
                    )
                }
            }
        }
    }

    fun loginWithPasskey(activity: Activity) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching {
                val options = container.authRepository.getPasskeyAuthenticationOptions()
                val payload = container.passkeyManager.getForAuthentication(activity, options)
                container.authRepository.verifyPasskeyLogin(options.challengeId, payload)
            }.onSuccess { result ->
                completeAuthResult(result, successMessage = "Passkey 登录成功")
            }.onFailure { throwable ->
                _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
            }
        }
    }

    fun verifyTotpMfa(code: String? = null, recoveryCode: String? = null) {
        val pending = _uiState.value.pendingMfa ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching {
                container.authRepository.verifyTotpMfa(
                    challengeId = pending.challengeId,
                    code = code,
                    recoveryCode = recoveryCode,
                )
                container.authRepository.getCurrentUser()
            }.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isAuthenticated = true,
                        currentUser = user,
                        pendingMfa = null,
                        message = "MFA 验证成功",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
            }
        }
    }

    fun verifyPasskeyMfa(activity: Activity) {
        val pending = _uiState.value.pendingMfa ?: return
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching {
                val options = container.authRepository.getPasskeyAuthenticationOptions()
                val payload = container.passkeyManager.getForAuthentication(activity, options)
                container.authRepository.verifyPasskeyMfa(
                    mfaChallengeId = pending.challengeId,
                    passkeyChallengeId = options.challengeId,
                    payload = payload,
                )
                container.authRepository.getCurrentUser()
            }.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isAuthenticated = true,
                        currentUser = user,
                        pendingMfa = null,
                        message = "Passkey MFA 验证成功",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
            }
        }
    }

    fun logout() {
        viewModelScope.launch {
            runCatching { container.authRepository.logout() }
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    currentUser = null,
                    pendingMfa = null,
                    pendingMobileBridgeConfirmation = null,
                    mobileBridgeReturnUrl = null,
                    pendingQrConfirmation = null,
                    message = "已退出登录",
                )
            }
        }
    }

    fun logoutAll() {
        viewModelScope.launch {
            runCatching { container.authRepository.logoutAll() }
            _uiState.update {
                it.copy(
                    isAuthenticated = false,
                    currentUser = null,
                    pendingMfa = null,
                    pendingMobileBridgeConfirmation = null,
                    mobileBridgeReturnUrl = null,
                    pendingQrConfirmation = null,
                    message = "已从所有设备退出",
                )
            }
        }
    }

    fun handleScannedQr(rawContent: String) {
        val operationHint = extractOperationHint(rawContent)
        val transferCode = extractTransferCode(rawContent)
        if (!transferCode.isNullOrBlank()) {
            previewTransferQr(transferCode, operationHint)
            return
        }

        val approveCode = extractApproveCode(rawContent)
        if (approveCode.isNullOrBlank()) {
            _uiState.update { it.copy(error = "未识别到有效二维码，请重试") }
            return
        }

        previewApproveQr(approveCode, operationHint)
    }

    fun confirmQrAction() {
        val pending = _uiState.value.pendingQrConfirmation
        if (pending == null || pending.code.isBlank()) {
            _uiState.update { it.copy(error = "二维码票据无效，请重新扫码") }
            return
        }
        when (pending.type) {
            QrConfirmationType.APPROVE_LOGIN -> approveQrChallenge(pending.code)
            QrConfirmationType.APPROVE_MFA -> approveQrChallenge(pending.code)
            QrConfirmationType.APPROVE_SENSITIVE -> approveQrChallenge(pending.code)
            QrConfirmationType.APPROVE_RECOVERY -> approveQrChallenge(pending.code)
            QrConfirmationType.LOGIN_THIS_PHONE,
            QrConfirmationType.SWITCH_AND_LOGIN_THIS_PHONE,
            -> exchangeSessionTransferAndLogin(pending.code)
        }
    }

    fun cancelQrAction() {
        _uiState.update { it.copy(pendingQrConfirmation = null) }
    }

    private fun previewApproveQr(
        approveCode: String,
        operationHint: String? = null,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null, error = null, pendingQrConfirmation = null) }
            runCatching {
                val preview = container.authRepository.getQrScanPreview(approveCode = approveCode)
                val type = when (preview.codeType) {
                    "approve_mfa" -> QrConfirmationType.APPROVE_MFA
                    "approve_sensitive" -> QrConfirmationType.APPROVE_SENSITIVE
                    "approve_recovery" -> QrConfirmationType.APPROVE_RECOVERY
                    else -> QrConfirmationType.APPROVE_LOGIN
                }
                PendingQrConfirmation(
                    type = type,
                    code = approveCode,
                    preview = preview,
                    operationHint = resolveOperationHint(
                        parsedHint = operationHint,
                        fallbackType = type,
                    ),
                )
            }.onSuccess { pending ->
                _uiState.update { it.copy(isBusy = false, pendingQrConfirmation = pending) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
            }
        }
    }

    private fun previewTransferQr(
        transferCode: String,
        operationHint: String? = null,
    ) {
        val isAuthenticated = _uiState.value.isAuthenticated
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, message = null, error = null, pendingQrConfirmation = null) }
            runCatching {
                val preview = container.authRepository.getQrScanPreview(transferCode = transferCode)
                val type = if (isAuthenticated) {
                    QrConfirmationType.SWITCH_AND_LOGIN_THIS_PHONE
                } else {
                    QrConfirmationType.LOGIN_THIS_PHONE
                }
                PendingQrConfirmation(
                    type = type,
                    code = transferCode,
                    preview = preview,
                    operationHint = resolveOperationHint(
                        parsedHint = operationHint,
                        fallbackType = type,
                    ),
                )
            }.onSuccess { pending ->
                _uiState.update { it.copy(isBusy = false, pendingQrConfirmation = pending) }
            }.onFailure { throwable ->
                _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
            }
        }
    }

    private fun approveQrChallenge(approveCode: String) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null, pendingQrConfirmation = null) }
            runCatching { container.authRepository.approveQrChallenge(approveCode) }
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false, message = "扫码授权成功") }
                }
                .onFailure { throwable ->
                    val readable = if (throwable is ApiException && throwable.statusCode == 401) {
                        "请先在手机端登录"
                    } else {
                        throwable.toReadableMessage()
                    }
                    _uiState.update { it.copy(isBusy = false, error = readable) }
                }
        }
    }

    private fun exchangeSessionTransferAndLogin(transferCode: String) {
        val previousState = _uiState.value
        viewModelScope.launch {
            _uiState.update {
                it.copy(
                    isBusy = true,
                    error = null,
                    message = null,
                    pendingMfa = null,
                    pendingMobileBridgeConfirmation = null,
                    mobileBridgeReturnUrl = null,
                    pendingQrConfirmation = null,
                )
            }
            runCatching {
                container.authRepository.exchangeSessionTransferForMobile(transferCode)
                container.authRepository.getCurrentUser()
            }.onSuccess { user ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isAuthenticated = true,
                        currentUser = user,
                        pendingMfa = null,
                        pendingMobileBridgeConfirmation = null,
                        mobileBridgeReturnUrl = null,
                        pendingQrConfirmation = null,
                        message = "扫码登录成功",
                    )
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isAuthenticated = previousState.isAuthenticated,
                        currentUser = previousState.currentUser,
                        pendingMobileBridgeConfirmation = previousState.pendingMobileBridgeConfirmation,
                        mobileBridgeReturnUrl = null,
                        pendingQrConfirmation = null,
                        error = throwable.toReadableMessage(),
                    )
                }
            }
        }
    }

    private fun runAuthAction(action: suspend () -> AuthResult) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching { action() }
                .onSuccess { result -> completeAuthResult(result, successMessage = "登录成功") }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
                }
        }
    }

    private fun completeAuthResult(result: AuthResult, successMessage: String) {
        when (result) {
            is AuthResult.Success -> {
                viewModelScope.launch {
                    runCatching { container.authRepository.getCurrentUser() }
                        .onSuccess { user ->
                            _uiState.update {
                                it.copy(
                                    isBusy = false,
                                    isAuthenticated = true,
                                    currentUser = user,
                                    pendingMfa = null,
                                    pendingMobileBridgeConfirmation = null,
                                    mobileBridgeReturnUrl = null,
                                    pendingQrConfirmation = null,
                                    message = successMessage,
                                )
                            }
                        }
                        .onFailure { throwable ->
                            _uiState.update {
                                it.copy(
                                    isBusy = false,
                                    error = throwable.toReadableMessage(),
                                )
                            }
                        }
                }
            }

            is AuthResult.NeedsMfa -> {
                val sourceLabel = when (result.source) {
                    AuthSource.PASSWORD -> "密码"
                    AuthSource.EMAIL_CODE -> "验证码"
                    AuthSource.PASSKEY -> "Passkey"
                }
                _uiState.update {
                    it.copy(
                        isBusy = false,
                        isAuthenticated = false,
                        pendingMfa = result,
                        pendingMobileBridgeConfirmation = null,
                        mobileBridgeReturnUrl = null,
                        pendingQrConfirmation = null,
                        message = "$sourceLabel 验证通过，请完成 MFA",
                    )
                }
            }
        }
    }

    private fun extractApproveCode(content: String): String? {
        val raw = content.trim()
        if (raw.isBlank()) return null

        val prefix = "KSUSER-AUTH-QR:"
        if (raw.startsWith(prefix, ignoreCase = true)) {
            return raw.substring(prefix.length).trim().takeIf { it.isNotBlank() }
        }

        val markerIndex = raw.indexOf(prefix, ignoreCase = true)
        if (markerIndex >= 0) {
            return raw.substring(markerIndex + prefix.length)
                .substringBefore('&')
                .substringBefore('#')
                .trim()
                .takeIf { it.isNotBlank() }
        }

        return runCatching {
            val uri = Uri.parse(raw)
            (uri.getQueryParameter("approveCode") ?: uri.getQueryParameter("code"))
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun extractTransferCode(content: String): String? {
        val raw = content.trim()
        if (raw.isBlank()) return null

        val prefix = "KSUSER-AUTH-XFER:v1:"
        if (raw.startsWith(prefix, ignoreCase = true)) {
            return raw.substring(prefix.length).trim().takeIf { it.isNotBlank() }
        }

        val markerIndex = raw.indexOf(prefix, ignoreCase = true)
        if (markerIndex >= 0) {
            return raw.substring(markerIndex + prefix.length)
                .substringBefore('&')
                .substringBefore('#')
                .trim()
                .takeIf { it.isNotBlank() }
        }

        return runCatching {
            val uri = Uri.parse(raw)
            uri.getQueryParameter("transferCode")
                ?.trim()
                ?.takeIf { it.isNotBlank() }
        }.getOrNull()
    }

    private fun extractOperationHint(content: String): String? {
        val raw = content.trim()
        if (raw.isBlank()) return null

        val uriHint = runCatching {
            val uri = Uri.parse(raw)
            listOf("operationType", "operation", "type", "action", "scene")
                .firstNotNullOfOrNull { key ->
                    uri.getQueryParameter(key)?.trim()?.takeIf { it.isNotBlank() }
                }
        }.getOrNull()
        canonicalizeOperationHint(uriHint)?.let { return it }

        val tokenHint = raw.split('?', '&', '#', ':', '/', '=', ';', ',', ' ')
            .asSequence()
            .map { it.trim() }
            .firstNotNullOfOrNull { token -> canonicalizeOperationHint(token) }
        return tokenHint
    }

    private fun resolveOperationHint(
        parsedHint: String?,
        fallbackType: QrConfirmationType,
    ): String {
        return canonicalizeOperationHint(parsedHint) ?: when (fallbackType) {
            QrConfirmationType.APPROVE_LOGIN -> "LOGIN"
            QrConfirmationType.APPROVE_MFA -> "MFA_VERIFY"
            QrConfirmationType.APPROVE_SENSITIVE -> "SENSITIVE_VERIFY"
            QrConfirmationType.APPROVE_RECOVERY -> "ACCOUNT_RECOVERY"
            QrConfirmationType.LOGIN_THIS_PHONE -> "LOGIN_THIS_PHONE"
            QrConfirmationType.SWITCH_AND_LOGIN_THIS_PHONE -> "SWITCH_AND_LOGIN_THIS_PHONE"
        }
    }

    private fun canonicalizeOperationHint(raw: String?): String? {
        val normalized = raw
            ?.trim()
            ?.replace('-', '_')
            ?.replace(' ', '_')
            ?.uppercase()
            ?.takeIf { it.isNotBlank() }
            ?: return null

        return when (normalized) {
            "APPROVE_LOGIN", "LOGIN", "LOGIN_WEB", "LOGIN_DESKTOP" -> "LOGIN"
            "APPROVE_MFA", "MFA", "MFA_VERIFY", "VERIFY_MFA", "LOGIN_MFA" -> "MFA_VERIFY"
            "APPROVE_SENSITIVE", "SENSITIVE", "SENSITIVE_VERIFY", "VERIFY_SENSITIVE" -> "SENSITIVE_VERIFY"
            "APPROVE_RECOVERY", "RECOVERY", "ACCOUNT_RECOVERY", "RECOVERY_ENDORSE", "RECOVERY_SPONSOR" -> "ACCOUNT_RECOVERY"
            "CHANGE_PASSWORD", "UPDATE_PASSWORD", "RESET_PASSWORD" -> "CHANGE_PASSWORD"
            "CHANGE_EMAIL", "UPDATE_EMAIL", "BIND_EMAIL" -> "CHANGE_EMAIL"
            "ADD_PASSKEY", "CREATE_PASSKEY", "REGISTER_PASSKEY" -> "ADD_PASSKEY"
            "DELETE_PASSKEY", "REMOVE_PASSKEY" -> "DELETE_PASSKEY"
            "ENABLE_TOTP", "OPEN_TOTP" -> "ENABLE_TOTP"
            "DISABLE_TOTP", "CLOSE_TOTP" -> "DISABLE_TOTP"
            "LOGIN_THIS_PHONE", "TRANSFER", "TRANSFER_LOGIN", "BRIDGE_TO_MOBILE" -> "LOGIN_THIS_PHONE"
            "SWITCH_AND_LOGIN_THIS_PHONE", "SWITCH_LOGIN", "TRANSFER_SWITCH_LOGIN" -> "SWITCH_AND_LOGIN_THIS_PHONE"
            else -> null
        }
    }

    private fun runBusyAction(
        successMessage: String,
        action: suspend () -> Unit,
    ) {
        viewModelScope.launch {
            _uiState.update { it.copy(isBusy = true, error = null, message = null) }
            runCatching { action() }
                .onSuccess {
                    _uiState.update { it.copy(isBusy = false, message = successMessage) }
                }
                .onFailure { throwable ->
                    _uiState.update { it.copy(isBusy = false, error = throwable.toReadableMessage()) }
                }
        }
    }
}

class AppViewModelFactory(
    private val container: AppContainer,
) : ViewModelProvider.Factory {
    @Suppress("UNCHECKED_CAST")
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        return AppViewModel(container) as T
    }
}

private fun Throwable.toReadableMessage(): String {
    return when (this) {
        is ApiException -> message
        else -> message ?: "操作失败"
    }
}
