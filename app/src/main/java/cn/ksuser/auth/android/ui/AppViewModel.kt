package cn.ksuser.auth.android.ui

import android.app.Activity
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import cn.ksuser.auth.android.core.network.ApiException
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.data.model.AuthResult
import cn.ksuser.auth.android.data.model.AuthSource
import cn.ksuser.auth.android.data.model.PasswordRequirement
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
    val message: String? = null,
    val error: String? = null,
)

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
                            passwordRequirement = requirement,
                        )
                    }
                }
            }.onFailure { throwable ->
                _uiState.update {
                    it.copy(
                        isBootstrapping = false,
                        isAuthenticated = false,
                        error = throwable.toReadableMessage(),
                    )
                }
            }
        }
    }

    fun clearTransientMessages() {
        _uiState.update { it.copy(message = null, error = null) }
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
                    message = "已从所有设备退出",
                )
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
                        message = "$sourceLabel 验证通过，请完成 MFA",
                    )
                }
            }
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
