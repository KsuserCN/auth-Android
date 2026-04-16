package cn.ksuser.auth.android.ui
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
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
    container: AppContainer,
    currentUser: UserProfile?,
    onProfileUpdated: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    var username by rememberSaveable(currentUser?.username) { mutableStateOf(currentUser?.username.orEmpty()) }
    var realName by rememberSaveable(currentUser?.realName) { mutableStateOf(currentUser?.realName.orEmpty()) }
    var region by rememberSaveable(currentUser?.region) { mutableStateOf(currentUser?.region.orEmpty()) }
    var bio by rememberSaveable(currentUser?.bio) { mutableStateOf(currentUser?.bio.orEmpty()) }
    var isBusy by remember { mutableStateOf(false) }
    val pickAvatarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.PickVisualMedia()) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        scope.launch {
            isBusy = true
            runCatching { container.profileRepository.uploadAvatar(context.contentResolver, uri) }
                .onSuccess {
                    onProfileUpdated()
                    onMessage("头像已上传")
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
        ) {
            Text("选择并上传头像")
        }

        ProfileFieldCard("用户名", username) { value ->
            username = value
            runProfileUpdate(container, "username", username, onProfileUpdated, onMessage)
        }
        ProfileFieldCard("真实姓名", realName) { value ->
            realName = value
            runProfileUpdate(container, "realName", realName, onProfileUpdated, onMessage)
        }
        ProfileFieldCard("地区", region) { value ->
            region = value
            runProfileUpdate(container, "region", region, onProfileUpdated, onMessage)
        }
        ProfileFieldCard("个人简介", bio, singleLine = false) { value ->
            bio = value
            runProfileUpdate(container, "bio", bio, onProfileUpdated, onMessage)
        }
        OverviewCard(
            title = "账号标识",
            subtitle = currentUser?.email ?: "暂无邮箱",
            body = "UUID: ${currentUser?.uuid.orEmpty()}",
        )
    }
}

private suspend fun runProfileUpdate(
    container: AppContainer,
    key: String,
    value: String,
    onProfileUpdated: () -> Unit,
    onMessage: (String) -> Unit,
) {
    runCatching { container.profileRepository.updateField(key, value) }
        .onSuccess {
            onProfileUpdated()
            onMessage("$key 已更新")
        }
        .onFailure { onMessage(it.message ?: "更新失败") }
}

@Composable
private fun ProfileFieldCard(
    title: String,
    value: String,
    singleLine: Boolean = true,
    onSave: suspend (String) -> Unit,
) {
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text(title, style = MaterialTheme.typography.titleMedium)
        AppOutlinedField(
            value = draft,
            onValueChange = { draft = it },
            singleLine = singleLine,
        )
        LoadingButton(text = "保存$title", onClick = { onSave(draft) })
    }
}
