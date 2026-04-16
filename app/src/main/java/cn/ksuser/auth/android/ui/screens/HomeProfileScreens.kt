package cn.ksuser.auth.android.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cn.ksuser.auth.android.core.app.AppIdentityProvider
import cn.ksuser.auth.android.core.env.EnvironmentProvider
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.data.model.UserProfile
import cn.ksuser.auth.android.ui.components.AppOutlinedField
import cn.ksuser.auth.android.ui.components.AppPagePadding
import cn.ksuser.auth.android.ui.components.AppRadius
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.components.GradientPrimaryButton
import cn.ksuser.auth.android.ui.components.LoadingButton
import cn.ksuser.auth.android.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(
    user: UserProfile?,
    onRefresh: () -> Unit,
) {
    val env = EnvironmentProvider.current
    val context = LocalContext.current
    val appIdentity = remember(context) { AppIdentityProvider.current(context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        OverviewCard(
            title = user?.username ?: "未命名用户",
            subtitle = user?.email ?: "暂无邮箱",
            body = buildString {
                appendLine("当前接口前缀: ${env.apiBaseUrl}")
                appendLine("Passkey RP: ${env.passkeyRpId}")
                appendLine("环境: ${env.appEnv}")
                appendLine("UA: ${AppIdentityProvider.userAgent()}")
                appendLine("包名: ${appIdentity.packageName}")
                appendLine("版本: ${appIdentity.versionName} (${appIdentity.versionCode})")
                append("签名 SHA-256: ${appIdentity.signingSha256.joinToString(" / ").ifBlank { "未知" }}")
            },
            actionLabel = "刷新账号信息",
            onAction = onRefresh,
        )
        OverviewCard(
            title = "安全偏好",
            subtitle = "来自当前账号设置",
            body = buildString {
                appendLine("MFA: ${user?.settings?.mfaEnabled}")
                appendLine("首选 MFA: ${user?.settings?.preferredMfaMethod ?: "totp"}")
                appendLine("首选敏感验证: ${user?.settings?.preferredSensitiveMethod ?: "password"}")
            },
        )
        OverviewCard(
            title = "Passkey 联调提示",
            subtitle = "API 前缀与 RP 域分离",
            body = "如果把 API 指向 localhost，但服务端 WebAuthn 仍返回 localhost/非生产 RP，Android 原生 Passkey 可能无法按生产方式直接工作。",
        )
    }
}

@Composable
internal fun OverviewCard(
    title: String,
    subtitle: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleLarge)
        Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
        Text(body, style = MaterialTheme.typography.bodyMedium)
        if (actionLabel != null && onAction != null) {
            LoadingButton(text = actionLabel, onClick = { onAction() })
        }
    }
}

@Composable
internal fun ProfileScreen(
    currentUser: UserProfile?,
    onNavigateToEdit: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        ProfileHeaderCard(
            avatarUrl = currentUser?.avatarUrl,
            username = currentUser?.username.orEmpty().ifBlank { "未设置" },
            onAvatarClick = { onNavigateToEdit("avatar") },
            onUsernameClick = { onNavigateToEdit("username") },
        )
        ProfileInfoCard(
            title = "真实姓名",
            value = currentUser?.realName.orEmpty().ifBlank { "未设置" },
            onClick = { onNavigateToEdit("realName") },
        )
        ProfileInfoCard(
            title = "地区",
            value = currentUser?.region.orEmpty().ifBlank { "未设置" },
            onClick = { onNavigateToEdit("region") },
        )
        ProfileInfoCard(
            title = "个人简介",
            value = currentUser?.bio.orEmpty().ifBlank { "未设置" },
            onClick = { onNavigateToEdit("bio") },
        )
        OverviewCard(
            title = "账号标识",
            subtitle = currentUser?.email ?: "暂无邮箱",
            body = "UUID: ${currentUser?.uuid.orEmpty()}",
        )
    }
}

@Composable
internal fun ProfileEditScreen(
    container: AppContainer,
    currentUser: UserProfile?,
    fieldKey: String,
    onBack: () -> Unit,
    onProfileUpdated: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var isBusy by remember { mutableStateOf(false) }
    val singleLine = fieldKey != "bio"
    val title = when (fieldKey) {
        "username" -> "编辑用户名"
        "realName" -> "编辑真实姓名"
        "region" -> "编辑地区"
        "bio" -> "编辑个人简介"
        "avatar" -> "编辑头像"
        else -> "编辑资料"
    }
    var value by rememberSaveable(fieldKey, currentUser?.username, currentUser?.realName, currentUser?.region, currentUser?.bio) {
        mutableStateOf(
            when (fieldKey) {
                "username" -> currentUser?.username.orEmpty()
                "realName" -> currentUser?.realName.orEmpty()
                "region" -> currentUser?.region.orEmpty()
                "bio" -> currentUser?.bio.orEmpty()
                else -> ""
            },
        )
    }
    val pickAvatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            runCatching { container.profileRepository.uploadAvatar(context.contentResolver, uri) }
                .onSuccess {
                    onProfileUpdated()
                    onMessage("头像已上传")
                    onBack()
                }
                .onFailure { onMessage(it.message ?: "头像上传失败") }
            isBusy = false
        }
    }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Text(title, style = MaterialTheme.typography.titleLarge)
            if (fieldKey == "avatar") {
                currentUser?.avatarUrl?.takeIf { it.isNotBlank() }?.let { avatarUrl ->
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier
                            .size(88.dp)
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(AppRadius.R20)),
                    )
                }
                OutlinedButton(
                    onClick = {
                        pickAvatarLauncher.launch(
                            PickVisualMediaRequest(ActivityResultContracts.PickVisualMedia.ImageOnly),
                        )
                    },
                    enabled = !isBusy,
                ) { Text("选择并上传头像") }
            } else {
                AppOutlinedField(
                    value = value,
                    onValueChange = { value = it },
                    singleLine = singleLine,
                )
                GradientPrimaryButton(
                    text = if (isBusy) "保存中..." else "保存",
                    onClick = {
                        scope.launch {
                            isBusy = true
                            runCatching { container.profileRepository.updateField(fieldKey, value) }
                                .onSuccess {
                                    onProfileUpdated()
                                    onMessage("资料已更新")
                                    onBack()
                                }
                                .onFailure { onMessage(it.message ?: "更新失败") }
                            isBusy = false
                        }
                    },
                    enabled = !isBusy,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}

@Composable
private fun ProfileHeaderCard(
    avatarUrl: String?,
    username: String,
    onAvatarClick: () -> Unit,
    onUsernameClick: () -> Unit,
) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.S12),
        ) {
            Box(
                modifier = Modifier
                    .size(76.dp)
                    .clip(androidx.compose.foundation.shape.RoundedCornerShape(AppRadius.R20))
                    .clickable(onClick = onAvatarClick),
            ) {
                if (!avatarUrl.isNullOrBlank()) {
                    AsyncImage(
                        model = avatarUrl,
                        contentDescription = "头像",
                        modifier = Modifier.fillMaxSize(),
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .clip(androidx.compose.foundation.shape.RoundedCornerShape(AppRadius.R20))
                            .background(MaterialTheme.colorScheme.surfaceVariant),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            "头像",
                            style = MaterialTheme.typography.labelMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
            Column(
                modifier = Modifier
                    .weight(1f)
                    .clickable(onClick = onUsernameClick),
                verticalArrangement = Arrangement.spacedBy(4.dp),
            ) {
                Text("用户名", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Text(username, style = MaterialTheme.typography.titleMedium)
                Text("点击头像修改头像", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text(
                "编辑",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.clickable(onClick = onUsernameClick),
            )
        }
    }
}

@Composable
private fun ProfileInfoCard(
    title: String,
    value: String,
    onClick: () -> Unit,
) {
    SectionCard(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp), modifier = Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium)
                Text(
                    value,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                "编辑",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.primary,
            )
        }
    }
}
