package cn.ksuser.auth.android.ui

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cn.ksuser.auth.android.core.app.AppIdentityProvider
import cn.ksuser.auth.android.core.env.EnvironmentProvider
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.data.model.PasskeyAvailability
import cn.ksuser.auth.android.data.model.PasskeyListItem
import cn.ksuser.auth.android.data.model.SensitiveVerificationStatus
import cn.ksuser.auth.android.data.model.TotpRegistrationOptions
import cn.ksuser.auth.android.data.model.TotpStatus
import cn.ksuser.auth.android.data.model.UserProfile
import cn.ksuser.auth.android.ui.components.AppOutlinedField
import cn.ksuser.auth.android.ui.components.AppPagePadding
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.components.LoadingButton
import cn.ksuser.auth.android.ui.components.LoadingOutlinedButton
import cn.ksuser.auth.android.ui.components.LoadingTextButton
import cn.ksuser.auth.android.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
internal fun SecurityScreen(
    container: AppContainer,
    user: UserProfile?,
    onUserRefresh: () -> Unit,
    onLogoutAll: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val appIdentity = remember(context) { AppIdentityProvider.current(context) }
    val passkeyAvailability = remember(container) { container.passkeyManager.availability() }
    val passkeyAvailabilityMessage = remember(container) { container.passkeyManager.availabilityMessage() }
    val scope = rememberCoroutineScope()
    var settings by remember(user?.settings) { mutableStateOf(user?.settings) }
    var totpStatus by remember { mutableStateOf<TotpStatus?>(null) }
    var passkeys by remember { mutableStateOf<List<PasskeyListItem>>(emptyList()) }
    var sensitiveStatus by remember { mutableStateOf<SensitiveVerificationStatus?>(null) }
    var recoveryCodes by remember { mutableStateOf<List<String>>(emptyList()) }
    var pendingTotpSetup by remember { mutableStateOf<TotpRegistrationOptions?>(null) }
    var totpCode by rememberSaveable { mutableStateOf("") }
    var showSensitiveDialog by remember { mutableStateOf(false) }
    var showAddPasskeyDialog by remember { mutableStateOf(false) }
    var newPasskeyName by rememberSaveable { mutableStateOf("Ksuser Android") }
    var renameTarget by remember { mutableStateOf<PasskeyListItem?>(null) }
    var renameDraft by rememberSaveable { mutableStateOf("") }
    var showChangeEmailDialog by remember { mutableStateOf(false) }
    var showChangePasswordDialog by remember { mutableStateOf(false) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var busy by remember { mutableStateOf(false) }

    suspend fun reloadAll() {
        busy = true
        runCatching {
            totpStatus = container.securityRepository.getTotpStatus()
            passkeys = container.securityRepository.getPasskeyList()
            sensitiveStatus = container.securityRepository.getSensitiveVerificationStatus()
        }.onFailure { onMessage(it.message ?: "安全信息加载失败") }
        busy = false
    }

    LaunchedEffect(user?.uuid) {
        settings = user?.settings
        reloadAll()
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        OverviewCard(
            title = "环境与 Passkey",
            subtitle = "原生 Credential Manager",
            body = buildString {
                appendLine("API 前缀: ${EnvironmentProvider.current.apiBaseUrl}")
                appendLine("RP ID: ${EnvironmentProvider.current.passkeyRpId}")
                appendLine("Origin Hint: ${EnvironmentProvider.current.passkeyOriginHint}")
                appendLine("系统: Android ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})")
                appendLine("UA: ${AppIdentityProvider.userAgent()}")
                appendLine("包名: ${appIdentity.packageName}")
                appendLine("版本: ${appIdentity.versionName} (${appIdentity.versionCode})")
                append("签名 SHA-256: ${appIdentity.signingSha256.joinToString(" / ").ifBlank { "未知" }}")
            },
            actionLabel = "刷新安全数据",
            onAction = { scope.launch { reloadAll() } },
        )

        SectionCard {
            Text("安全偏好", style = MaterialTheme.typography.titleMedium)
            SettingSwitchRow("启用 MFA", settings?.mfaEnabled == true) { checked ->
                scope.launch {
                    runCatching { container.securityRepository.updateBooleanSetting("mfaEnabled", checked) }
                        .onSuccess {
                            settings = settings?.copy(
                                mfaEnabled = it.mfaEnabled,
                                preferredMfaMethod = it.preferredMfaMethod,
                                preferredSensitiveMethod = it.preferredSensitiveMethod,
                            )
                            onUserRefresh()
                            onMessage("MFA 设置已更新")
                        }
                        .onFailure { onMessage(it.message ?: "更新失败") }
                }
            }
            SettingSwitchRow("异地登录检测", settings?.detectUnusualLogin == true) { checked ->
                scope.launch {
                    runCatching { container.securityRepository.updateBooleanSetting("detectUnusualLogin", checked) }
                        .onSuccess {
                            settings = settings?.copy(detectUnusualLogin = it.detectUnusualLogin)
                            onUserRefresh()
                            onMessage("已更新")
                        }
                        .onFailure { onMessage(it.message ?: "更新失败") }
                }
            }
            SettingSwitchRow("敏感操作邮件提醒", settings?.notifySensitiveActionEmail == true) { checked ->
                scope.launch {
                    runCatching { container.securityRepository.updateBooleanSetting("notifySensitiveActionEmail", checked) }
                        .onSuccess {
                            settings = settings?.copy(notifySensitiveActionEmail = it.notifySensitiveActionEmail)
                            onUserRefresh()
                            onMessage("已更新")
                        }
                        .onFailure { onMessage(it.message ?: "更新失败") }
                }
            }
            PreferenceChips(
                title = "首选 MFA",
                options = listOf("totp", "passkey"),
                selected = settings?.preferredMfaMethod ?: "totp",
            ) { choice ->
                scope.launch {
                    runCatching { container.securityRepository.updateStringSetting("preferredMfaMethod", choice) }
                        .onSuccess {
                            settings = settings?.copy(preferredMfaMethod = it.preferredMfaMethod)
                            onUserRefresh()
                            onMessage("首选 MFA 已更新")
                        }
                        .onFailure { onMessage(it.message ?: "更新失败") }
                }
            }
            PreferenceChips(
                title = "首选敏感验证",
                options = listOf("password", "email-code", "passkey", "totp"),
                selected = settings?.preferredSensitiveMethod ?: "password",
            ) { choice ->
                scope.launch {
                    runCatching { container.securityRepository.updateStringSetting("preferredSensitiveMethod", choice) }
                        .onSuccess {
                            settings = settings?.copy(preferredSensitiveMethod = it.preferredSensitiveMethod)
                            onUserRefresh()
                            onMessage("首选敏感验证已更新")
                        }
                        .onFailure { onMessage(it.message ?: "更新失败") }
                }
            }
        }

        SectionCard {
            Text("敏感操作验证", style = MaterialTheme.typography.titleMedium)
            Text(
                if (sensitiveStatus?.verified == true) {
                    "已验证，剩余 ${sensitiveStatus?.remainingSeconds ?: 0} 秒"
                } else {
                    "未验证，涉及添加 Passkey、修改邮箱/密码、删除账户时需要先验证"
                },
            )
            val methods = sensitiveStatus?.methods.orEmpty()
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                methods.forEach { method ->
                    AssistChip(onClick = {}, label = { Text(method) })
                }
            }
            Button(onClick = { showSensitiveDialog = true }, enabled = !busy) {
                Text(if (sensitiveStatus?.verified == true) "重新验证" else "开始验证")
            }
        }

        SectionCard {
            Text("TOTP", style = MaterialTheme.typography.titleMedium)
            Text("已启用: ${totpStatus?.enabled == true}，剩余恢复码: ${totpStatus?.recoveryCodesCount ?: 0}")
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                LoadingButton(text = "生成 TOTP 注册选项", onClick = {
                    runCatching { container.securityRepository.getTotpRegistrationOptions() }
                        .onSuccess { pendingTotpSetup = it }
                        .onFailure { onMessage(it.message ?: "生成 TOTP 选项失败") }
                })
                LoadingOutlinedButton(text = "禁用", onClick = {
                    val disabled = runCatching { container.securityRepository.disableTotp() }
                        .onFailure { onMessage(it.message ?: "禁用失败") }
                        .isSuccess
                    if (disabled) {
                        reloadAll()
                        onMessage("TOTP 已禁用")
                    }
                })
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                LoadingOutlinedButton(text = "查看恢复码", onClick = {
                    recoveryCodes = runCatching { container.securityRepository.getRecoveryCodes() }
                        .getOrElse {
                            onMessage(it.message ?: "获取恢复码失败")
                            emptyList()
                        }
                })
                LoadingOutlinedButton(text = "重生恢复码", onClick = {
                    val newCodes = runCatching { container.securityRepository.regenerateRecoveryCodes() }
                        .onFailure { onMessage(it.message ?: "重生恢复码失败") }
                        .getOrNull()
                    if (newCodes != null) {
                        recoveryCodes = newCodes
                        reloadAll()
                        onMessage("恢复码已重新生成")
                    }
                })
            }
            if (recoveryCodes.isNotEmpty()) {
                Text(recoveryCodes.joinToString("  "), style = MaterialTheme.typography.bodySmall)
            }
        }

        SectionCard {
            Text("Passkey", style = MaterialTheme.typography.titleMedium)
            Text(passkeyAvailabilityMessage)
            Button(
                onClick = { showAddPasskeyDialog = true },
                enabled = passkeyAvailability == PasskeyAvailability.Available &&
                    sensitiveStatus?.verified == true &&
                    activity != null,
            ) {
                Text("新增 Passkey")
            }
            if (passkeyAvailability != PasskeyAvailability.Available) {
                Text("当前环境不满足原生 Passkey 要求，已阻止继续创建。", color = MaterialTheme.colorScheme.error)
            }
            if (sensitiveStatus?.verified != true) {
                Text("添加 Passkey 前请先完成敏感验证", color = MaterialTheme.colorScheme.error)
            }
            passkeys.forEach { passkey ->
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Text(passkey.name, style = MaterialTheme.typography.titleMedium)
                    Text(
                        "创建于 ${passkey.createdAt}\n最后使用 ${passkey.lastUsedAt ?: "暂无"}\ntransports=${passkey.transports}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                        OutlinedButton(onClick = {
                            renameTarget = passkey
                            renameDraft = passkey.name
                        }) { Text("重命名") }
                        LoadingOutlinedButton(text = "删除", onClick = {
                            val deleted = runCatching { container.securityRepository.deletePasskey(passkey.id) }
                                .onFailure { onMessage(it.message ?: "删除失败") }
                                .isSuccess
                            if (deleted) {
                                reloadAll()
                                onMessage("Passkey 已删除")
                            }
                        })
                    }
                }
            }
        }

        SectionCard {
            Text("敏感操作", style = MaterialTheme.typography.titleMedium)
            Text("修改邮箱、修改密码、删除账号要求先完成上方敏感验证")
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                Button(onClick = { showChangeEmailDialog = true }, enabled = sensitiveStatus?.verified == true) {
                    Text("修改邮箱")
                }
                Button(onClick = { showChangePasswordDialog = true }, enabled = sensitiveStatus?.verified == true) {
                    Text("修改密码")
                }
            }
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                OutlinedButton(onClick = { showDeleteDialog = true }, enabled = sensitiveStatus?.verified == true) {
                    Text("删除账号")
                }
                LoadingOutlinedButton(text = "退出全部设备", onClick = { onLogoutAll() })
            }
        }
    }

    if (pendingTotpSetup != null) {
        AlertDialog(
            onDismissRequest = { pendingTotpSetup = null },
            title = { Text("TOTP 注册") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                    Text("secret: ${pendingTotpSetup?.secret}")
                    pendingTotpSetup?.qrCodeUrl?.let { qrCode ->
                        AsyncImage(model = qrCode, contentDescription = "TOTP QR", modifier = Modifier.size(180.dp))
                    }
                    Text("恢复码: ${pendingTotpSetup?.recoveryCodes?.joinToString(" ")}")
                    AppOutlinedField(
                        value = totpCode,
                        onValueChange = { totpCode = it },
                        label = { Text("输入 6 位验证码") },
                    )
                }
            },
            confirmButton = {
                LoadingTextButton(text = "确认启用", onClick = {
                    val setup = pendingTotpSetup ?: return@LoadingTextButton
                    val created = runCatching {
                        container.securityRepository.verifyTotpRegistration(totpCode.trim(), setup.recoveryCodes)
                    }.onFailure { onMessage(it.message ?: "TOTP 启用失败") }
                        .isSuccess
                    if (created) {
                        pendingTotpSetup = null
                        totpCode = ""
                        reloadAll()
                        onMessage("TOTP 已启用")
                    }
                })
            },
            dismissButton = { TextButton(onClick = { pendingTotpSetup = null }) { Text("取消") } },
        )
    }

    if (showAddPasskeyDialog) {
        AlertDialog(
            onDismissRequest = { showAddPasskeyDialog = false },
            title = { Text("新增 Passkey") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                    Text("Passkey 需要 Android 原生凭据提供器与匹配的 RP 域配置。")
                    AppOutlinedField(
                        value = newPasskeyName,
                        onValueChange = { newPasskeyName = it },
                        label = { Text("Passkey 名称") },
                    )
                }
            },
            confirmButton = {
                LoadingTextButton(text = "创建", onClick = {
                    if (activity == null) {
                        onMessage("当前上下文不支持 Passkey")
                        return@LoadingTextButton
                    }
                    val created = runCatching {
                        val options = container.securityRepository.getPasskeyRegistrationOptions(newPasskeyName.trim())
                        val payload = container.passkeyManager.createForRegistration(activity, options)
                        container.securityRepository.verifyPasskeyRegistration(newPasskeyName.trim(), payload)
                    }.onFailure { onMessage(it.message ?: "Passkey 创建失败") }
                        .isSuccess
                    if (created) {
                        showAddPasskeyDialog = false
                        newPasskeyName = "Ksuser Android"
                        reloadAll()
                        onMessage("Passkey 已添加")
                    }
                })
            },
            dismissButton = { TextButton(onClick = { showAddPasskeyDialog = false }) { Text("取消") } },
        )
    }

    renameTarget?.let { passkey ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名 Passkey") },
            text = {
                AppOutlinedField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("新名称") },
                )
            },
            confirmButton = {
                LoadingTextButton(text = "保存", onClick = {
                    val renamed = runCatching {
                        container.securityRepository.renamePasskey(passkey.id, renameDraft.trim())
                    }.onFailure { onMessage(it.message ?: "重命名失败") }
                        .isSuccess
                    if (renamed) {
                        renameTarget = null
                        reloadAll()
                        onMessage("Passkey 已重命名")
                    }
                })
            },
            dismissButton = { TextButton(onClick = { renameTarget = null }) { Text("取消") } },
        )
    }

    if (showSensitiveDialog) {
        SensitiveVerificationDialog(
            container = container,
            onDismiss = { showSensitiveDialog = false },
            onVerified = {
                scope.launch {
                    reloadAll()
                    showSensitiveDialog = false
                    onMessage("敏感验证已通过")
                }
            },
            onMessage = onMessage,
        )
    }

    if (showChangeEmailDialog) {
        ChangeEmailDialog(
            container = container,
            onDismiss = { showChangeEmailDialog = false },
            onSuccess = {
                showChangeEmailDialog = false
                onUserRefresh()
                scope.launch { reloadAll() }
                onMessage("邮箱已更新")
            },
            onMessage = onMessage,
        )
    }

    if (showChangePasswordDialog) {
        ChangePasswordDialog(
            container = container,
            onDismiss = { showChangePasswordDialog = false },
            onSuccess = {
                showChangePasswordDialog = false
                scope.launch { reloadAll() }
                onMessage("密码已修改")
            },
            onMessage = onMessage,
        )
    }

    if (showDeleteDialog) {
        DeleteAccountDialog(
            container = container,
            onDismiss = { showDeleteDialog = false },
            onDeleted = {
                showDeleteDialog = false
                onLogoutAll()
            },
            onMessage = onMessage,
        )
    }
}

@Composable
private fun SettingSwitchRow(
    label: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        Text(label)
        Switch(checked = checked, onCheckedChange = onCheckedChange)
    }
}

@Composable
private fun PreferenceChips(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
        Text(title, style = MaterialTheme.typography.titleSmall)
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
            options.forEach { option ->
                FilterChip(
                    selected = selected == option,
                    onClick = { onSelected(option) },
                    label = { Text(option) },
                )
            }
        }
    }
}

@Composable
private fun SensitiveVerificationDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onVerified: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val activity = context as? Activity
    var status by remember { mutableStateOf<SensitiveVerificationStatus?>(null) }
    var selectedMethod by rememberSaveable { mutableStateOf("password") }
    var password by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }
    var recoveryMode by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        runCatching { container.securityRepository.getSensitiveVerificationStatus() }
            .onSuccess {
                status = it
                selectedMethod = it.preferredMethod ?: it.methods.firstOrNull() ?: "password"
            }
            .onFailure { onMessage(it.message ?: "读取敏感验证状态失败") }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("敏感操作验证") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                Text("可用方式: ${status?.methods?.joinToString().orEmpty()}")
                PreferenceChips(
                    title = "验证方式",
                    options = status?.methods.orEmpty(),
                    selected = selectedMethod,
                    onSelected = { selectedMethod = it },
                )
                when (selectedMethod) {
                    "password" -> {
                        AppOutlinedField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("当前密码") },
                        )
                    }

                    "email-code" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8), verticalAlignment = Alignment.CenterVertically) {
                            AppOutlinedField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("邮箱验证码") },
                                modifier = Modifier.weight(1f),
                            )
                            LoadingOutlinedButton(text = "发送", onClick = {
                                runCatching { container.securityRepository.sendSensitiveCode() }
                                    .onSuccess { onMessage("验证码已发送") }
                                    .onFailure { onMessage(it.message ?: "发送失败") }
                            })
                        }
                    }

                    "totp" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                            FilterChip(selected = !recoveryMode, onClick = { recoveryMode = false }, label = { Text("TOTP") })
                            FilterChip(selected = recoveryMode, onClick = { recoveryMode = true }, label = { Text("恢复码") })
                        }
                        AppOutlinedField(
                            value = code,
                            onValueChange = { code = it },
                            label = { Text(if (recoveryMode) "恢复码" else "6 位验证码") },
                        )
                    }

                    "passkey" -> {
                        Text("Passkey 将直接唤起 Android 凭据提供器完成验证。")
                        if (activity == null) {
                            Text("当前上下文不支持 Passkey", color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
            }
        },
        confirmButton = {
            LoadingTextButton(text = "验证", onClick = {
                runCatching {
                    when (selectedMethod) {
                        "password" -> container.securityRepository.verifySensitivePassword(password)
                        "email-code" -> container.securityRepository.verifySensitiveEmailCode(code.trim())
                        "totp" -> {
                            if (recoveryMode) {
                                container.securityRepository.verifySensitiveTotp(recoveryCode = code.trim().uppercase())
                            } else {
                                container.securityRepository.verifySensitiveTotp(code = code.trim())
                            }
                        }

                        "passkey" -> {
                            if (activity == null) error("当前上下文不支持 Passkey")
                            val options = container.securityRepository.getPasskeySensitiveVerificationOptions()
                            val payload = container.passkeyManager.getForAuthentication(activity, options)
                            container.securityRepository.verifySensitivePasskey(options.challengeId, payload)
                        }
                    }
                }.onSuccess { onVerified() }
                    .onFailure { onMessage(it.message ?: "验证失败") }
            })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ChangeEmailDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改邮箱") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                AppOutlinedField(value = email, onValueChange = { email = it }, label = { Text("新邮箱") })
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8), verticalAlignment = Alignment.CenterVertically) {
                    AppOutlinedField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("验证码") },
                        modifier = Modifier.weight(1f),
                    )
                    LoadingOutlinedButton(text = "发送", onClick = {
                        runCatching { container.securityRepository.sendChangeEmailCode(email.trim()) }
                            .onSuccess { onMessage("邮箱验证码已发送") }
                            .onFailure { onMessage(it.message ?: "发送失败") }
                    })
                }
            }
        },
        confirmButton = {
            LoadingTextButton(text = "提交", onClick = {
                runCatching { container.securityRepository.changeEmail(email.trim(), code.trim()) }
                    .onSuccess { onSuccess() }
                    .onFailure { onMessage(it.message ?: "修改邮箱失败") }
            })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ChangePasswordDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onSuccess: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var password by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            AppOutlinedField(
                value = password,
                onValueChange = { password = it },
                label = { Text("新密码") },
            )
        },
        confirmButton = {
            LoadingTextButton(text = "提交", onClick = {
                runCatching { container.securityRepository.changePassword(password) }
                    .onSuccess { onSuccess() }
                    .onFailure { onMessage(it.message ?: "修改密码失败") }
            })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun DeleteAccountDialog(
    container: AppContainer,
    onDismiss: () -> Unit,
    onDeleted: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var confirmText by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除账号") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                Text("输入 DELETE 以确认删除账号。")
                AppOutlinedField(value = confirmText, onValueChange = { confirmText = it }, label = { Text("确认文本") })
            }
        },
        confirmButton = {
            LoadingTextButton(text = "删除", onClick = {
                runCatching { container.securityRepository.deleteAccount(confirmText) }
                    .onSuccess { onDeleted() }
                    .onFailure { onMessage(it.message ?: "删除账号失败") }
            })
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}
