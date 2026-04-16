package cn.ksuser.auth.android.ui

import android.Manifest
import android.app.Activity
import android.content.pm.PackageManager
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.data.model.PasskeyAvailability
import cn.ksuser.auth.android.ui.components.AppOutlinedField
import cn.ksuser.auth.android.ui.components.AppPagePadding
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.components.BrandHeroCard
import cn.ksuser.auth.android.ui.components.GradientPrimaryButton
import cn.ksuser.auth.android.ui.components.LoadingButtonContent
import cn.ksuser.auth.android.ui.components.SectionCard
import cn.ksuser.auth.android.ui.theme.GoldGradientBottomDark
import cn.ksuser.auth.android.ui.theme.GoldGradientBottomLight
import cn.ksuser.auth.android.ui.theme.GoldGradientTopDark
import cn.ksuser.auth.android.ui.theme.GoldGradientTopLight

@Composable
internal fun AuthFlowScreen(
    state: AppUiState,
    container: AppContainer,
    viewModel: AppViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var showQrScanner by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showQrScanner = true
        } else {
            Toast.makeText(context, "相机权限被拒绝，无法扫码", Toast.LENGTH_SHORT).show()
        }
    }
    var authTab by rememberSaveable { mutableStateOf(0) }
    var loginMethod by rememberSaveable { mutableStateOf(0) }
    var email by rememberSaveable { mutableStateOf("") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var username by rememberSaveable { mutableStateOf("") }
    var registerCode by rememberSaveable { mutableStateOf("") }
    var registerPassword by rememberSaveable { mutableStateOf("") }
    var mfaCode by rememberSaveable { mutableStateOf("") }
    var useRecoveryCode by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .background(
                    Brush.linearGradient(
                        listOf(GoldGradientTopLight, GoldGradientBottomLight),
                    ),
                )
                .padding(AppPagePadding),
            verticalArrangement = Arrangement.Center,
        ) {
            BrandHeroCard(modifier = Modifier.fillMaxWidth()) {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S16)) {
                    Text(
                        "Ksuser Auth Android",
                        style = MaterialTheme.typography.headlineMedium,
                    )
                    Text(
                        if (state.pendingMfa != null) "第一因子已通过，请完成多因素认证"
                        else "支持密码、邮箱验证码与原生 Passkey 登录，接口前缀来自 .env 配置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    OutlinedButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                showQrScanner = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = null)
                        androidx.compose.foundation.layout.Spacer(modifier = Modifier.padding(horizontal = AppSpacing.S8))
                        Text("扫码登录 / 扫码授权")
                    }

                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        if (state.pendingMfa != null) {
                            val challenge = state.pendingMfa!!
                            Text("可用 MFA 方式: ${challenge.methods.joinToString()}", style = MaterialTheme.typography.bodyMedium)
                            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                                FilterChip(
                                    selected = !useRecoveryCode,
                                    onClick = { useRecoveryCode = false },
                                    label = { Text("TOTP") },
                                )
                                FilterChip(
                                    selected = useRecoveryCode,
                                    onClick = { useRecoveryCode = true },
                                    label = { Text("恢复码") },
                                )
                            }
                            if (challenge.methods.contains("passkey")) {
                                GradientPrimaryButton(
                                    text = if (state.isBusy) "处理中..." else "使用 Passkey 完成 MFA",
                                    onClick = {
                                        if (activity == null) {
                                            Toast.makeText(context, "当前上下文不支持 Passkey", Toast.LENGTH_SHORT).show()
                                        } else {
                                            viewModel.verifyPasskeyMfa(activity)
                                        }
                                    },
                                    enabled = !state.isBusy,
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                            AppOutlinedField(
                                value = mfaCode,
                                onValueChange = { mfaCode = it },
                                label = { Text(if (useRecoveryCode) "恢复码" else "6 位验证码") },
                            )
                            GradientPrimaryButton(
                                text = if (state.isBusy) "处理中..." else "完成 MFA",
                                onClick = {
                                    if (useRecoveryCode) {
                                        viewModel.verifyTotpMfa(recoveryCode = mfaCode.trim().uppercase())
                                    } else {
                                        viewModel.verifyTotpMfa(code = mfaCode.trim())
                                    }
                                },
                                enabled = !state.isBusy && mfaCode.isNotBlank(),
                                modifier = Modifier.fillMaxWidth(),
                            )
                        } else {
                            TabRow(selectedTabIndex = authTab) {
                                Tab(selected = authTab == 0, onClick = { authTab = 0 }, text = { Text("登录") })
                                Tab(selected = authTab == 1, onClick = { authTab = 1 }, text = { Text("注册") })
                            }

                            if (authTab == 0) {
                                TabRow(selectedTabIndex = loginMethod) {
                                    Tab(selected = loginMethod == 0, onClick = { loginMethod = 0 }, text = { Text("密码") })
                                    Tab(selected = loginMethod == 1, onClick = { loginMethod = 1 }, text = { Text("验证码") })
                                    Tab(selected = loginMethod == 2, onClick = { loginMethod = 2 }, text = { Text("Passkey") })
                                }
                                when (loginMethod) {
                                    0 -> {
                                        AppOutlinedField(
                                            value = email,
                                            onValueChange = { email = it },
                                            label = { Text("邮箱") },
                                        )
                                        AppOutlinedField(
                                            value = password,
                                            onValueChange = { password = it },
                                            label = { Text("密码") },
                                        )
                                        GradientPrimaryButton(
                                            text = if (state.isBusy) "登录中..." else "密码登录",
                                            onClick = { viewModel.passwordLogin(email.trim(), password) },
                                            enabled = !state.isBusy && email.isNotBlank() && password.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }

                                    1 -> {
                                        AppOutlinedField(
                                            value = email,
                                            onValueChange = { email = it },
                                            label = { Text("邮箱") },
                                        )
                                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                                            AppOutlinedField(
                                                value = code,
                                                onValueChange = { code = it },
                                                label = { Text("验证码") },
                                                modifier = Modifier.weight(1f),
                                            )
                                            OutlinedButton(
                                                onClick = { viewModel.sendLoginCode(email.trim()) },
                                                enabled = !state.isBusy && email.isNotBlank(),
                                            ) {
                                                LoadingButtonContent(text = "发送", isLoading = state.isBusy)
                                            }
                                        }
                                        GradientPrimaryButton(
                                            text = if (state.isBusy) "登录中..." else "验证码登录",
                                            onClick = { viewModel.loginWithCode(email.trim(), code.trim()) },
                                            enabled = !state.isBusy && email.isNotBlank() && code.isNotBlank(),
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }

                                    else -> {
                                        PasskeyInfoBlock()
                                        val passkeyAvailability = remember(container) { container.passkeyManager.availability() }
                                        val passkeyAvailabilityMessage = remember(container) { container.passkeyManager.availabilityMessage() }
                                        if (passkeyAvailability != PasskeyAvailability.Available) {
                                            Text(
                                                passkeyAvailabilityMessage,
                                                color = MaterialTheme.colorScheme.error,
                                                style = MaterialTheme.typography.bodySmall,
                                            )
                                        }
                                        GradientPrimaryButton(
                                            text = if (state.isBusy) "处理中..." else "使用 Passkey 登录",
                                            onClick = {
                                                if (activity == null) {
                                                    Toast.makeText(context, "当前上下文不支持 Passkey", Toast.LENGTH_SHORT).show()
                                                } else {
                                                    viewModel.loginWithPasskey(activity)
                                                }
                                            },
                                            enabled = !state.isBusy && passkeyAvailability == PasskeyAvailability.Available,
                                            modifier = Modifier.fillMaxWidth(),
                                        )
                                    }
                                }
                            } else {
                                val requirement = state.passwordRequirement
                                AppOutlinedField(
                                    value = username,
                                    onValueChange = { username = it },
                                    label = { Text("用户名") },
                                )
                                AppOutlinedField(
                                    value = email,
                                    onValueChange = { email = it },
                                    label = { Text("邮箱") },
                                )
                                AppOutlinedField(
                                    value = registerPassword,
                                    onValueChange = { registerPassword = it },
                                    label = { Text("密码") },
                                )
                                requirement?.let {
                                    Text(
                                        "密码要求: ${it.minLength}-${it.maxLength} 位，数字=${it.requireDigits}",
                                        style = MaterialTheme.typography.bodySmall,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    )
                                }
                                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                                    AppOutlinedField(
                                        value = registerCode,
                                        onValueChange = { registerCode = it },
                                        label = { Text("邮箱验证码") },
                                        modifier = Modifier.weight(1f),
                                    )
                                    OutlinedButton(
                                        onClick = { viewModel.sendRegisterCode(email.trim()) },
                                        enabled = !state.isBusy && email.isNotBlank(),
                                    ) {
                                        LoadingButtonContent(text = "发送", isLoading = state.isBusy)
                                    }
                                }
                                GradientPrimaryButton(
                                    text = if (state.isBusy) "提交中..." else "创建账号",
                                    onClick = {
                                        viewModel.register(
                                            username = username.trim(),
                                            email = email.trim(),
                                            password = registerPassword,
                                            code = registerCode.trim(),
                                        )
                                    },
                                    enabled = !state.isBusy &&
                                        username.isNotBlank() &&
                                        email.isNotBlank() &&
                                        registerPassword.isNotBlank() &&
                                        registerCode.isNotBlank(),
                                    modifier = Modifier.fillMaxWidth(),
                                )
                            }
                        }
                    }
                }
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onDetected = { rawContent ->
                showQrScanner = false
                viewModel.handleScannedQr(rawContent)
            },
        )
    }
}

@Composable
private fun PasskeyInfoBlock() {
    val environment = cn.ksuser.auth.android.core.env.EnvironmentProvider.current
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text("原生 Passkey", style = MaterialTheme.typography.titleMedium)
        Text(
            "API 地址来自 .env，但 Passkey 的 RP 域由 ${environment.passkeyRpId} 决定。",
            style = MaterialTheme.typography.bodySmall,
        )
        Text(
            if (environment.passkeyOriginHint.startsWith("https://")) {
                "当前 Origin Hint: ${environment.passkeyOriginHint}"
            } else {
                "当前 Origin Hint 不满足原生 Passkey 要求，需使用已配置 Digital Asset Links 的 https 域名。"
            },
            style = MaterialTheme.typography.bodySmall,
        )
    }
}
