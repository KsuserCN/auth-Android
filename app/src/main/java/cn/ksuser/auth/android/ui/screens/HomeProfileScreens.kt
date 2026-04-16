package cn.ksuser.auth.android.ui

import android.content.Intent
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.OpenInNew
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Devices
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.data.model.UserProfile
import cn.ksuser.auth.android.ui.components.AppOutlinedField
import cn.ksuser.auth.android.ui.components.AppPagePadding
import cn.ksuser.auth.android.ui.components.AppRadius
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.components.BrandHeroCard
import cn.ksuser.auth.android.ui.components.GradientPrimaryButton
import cn.ksuser.auth.android.ui.components.LoadingButton
import cn.ksuser.auth.android.ui.components.SectionCard
import kotlinx.coroutines.launch

@Composable
internal fun HomeScreen(
    user: UserProfile?,
    onRefresh: () -> Unit,
    onOpenProfile: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenSessions: () -> Unit,
) {
    val displayRealName = user?.realName.takeMeaningfulProfileText()
    val displayUsername = user?.username.takeMeaningfulProfileText()
    val displayRegion = user?.region.takeMeaningfulProfileText()
    val displayBio = user?.bio.takeMeaningfulProfileText()
    val hasAvatar = !user?.avatarUrl.isNullOrBlank()
    val completionItems = listOf(
        displayUsername != null,
        displayRealName != null,
        displayRegion != null,
        displayBio != null,
        hasAvatar,
    )
    val completedCount = completionItems.count { it }
    val completionPercent = if (completionItems.isEmpty()) 0 else completedCount * 100 / completionItems.size
    val securitySummary = when {
        user?.settings?.mfaEnabled == true && user.settings.detectUnusualLogin -> "账号保护较完整"
        user?.settings?.mfaEnabled == true -> "双重验证已开启"
        else -> "建议继续完善安全设置"
    }
    val greetingName = displayUsername
        ?: displayRealName
        ?: "你好"

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        AccountHeroCard(
            user = user,
            greetingName = greetingName,
            securitySummary = securitySummary,
            onRefresh = onRefresh,
            onOpenProfile = onOpenProfile,
        )
        SecurityOverviewCard(
            user = user,
            onOpenSecurity = onOpenSecurity,
        )
        QuickActionsCard(
            onOpenProfile = onOpenProfile,
            onOpenSecurity = onOpenSecurity,
            onOpenSessions = onOpenSessions,
        )
        ProfileCompletionCard(
            user = user,
            completionPercent = completionPercent,
            completedCount = completedCount,
            totalCount = completionItems.size,
            onOpenProfile = onOpenProfile,
        )
    }
}

@Composable
private fun AccountHeroCard(
    user: UserProfile?,
    greetingName: String,
    securitySummary: String,
    onRefresh: () -> Unit,
    onOpenProfile: () -> Unit,
) {
    BrandHeroCard(modifier = Modifier.fillMaxWidth()) {
        Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S12)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.S12),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                AvatarBadge(
                    avatarUrl = user?.avatarUrl,
                    label = greetingName.take(1),
                )
                Column(
                    modifier = Modifier.weight(1f),
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                ) {
                    Text("欢迎回来，$greetingName", style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text(
                        user?.email.orEmpty().ifBlank { "当前账号未绑定邮箱" },
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Text(
                        securitySummary,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8),
            ) {
                Button(
                    onClick = onOpenProfile,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppRadius.R12),
                ) {
                    Text("查看资料")
                }
                OutlinedButton(
                    onClick = onRefresh,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(AppRadius.R12),
                ) {
                    Icon(Icons.Outlined.Refresh, contentDescription = null)
                    Spacer(modifier = Modifier.width(AppSpacing.S8))
                    Text("刷新状态")
                }
            }
        }
    }
}

@Composable
private fun SecurityOverviewCard(
    user: UserProfile?,
    onOpenSecurity: () -> Unit,
) {
    val settings = user?.settings
    val mfaStatus = if (settings?.mfaEnabled == true) "已开启" else "未开启"
    val unusualLoginStatus = if (settings?.detectUnusualLogin == true) "已开启" else "未开启"
    val notifyStatus = if (settings?.notifySensitiveActionEmail == true) "已开启" else "未开启"

    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text("账号安全", style = MaterialTheme.typography.titleLarge)
        Text(
            if (settings?.mfaEnabled == true) {
                "你的账号已启用额外验证，安全性更高。"
            } else {
                "建议开启双重验证，避免密码泄露后被直接登录。"
            },
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
            StatusChip(title = "双重验证", value = mfaStatus, modifier = Modifier.weight(1f))
            StatusChip(title = "异常登录提醒", value = unusualLoginStatus, modifier = Modifier.weight(1f))
        }
        StatusChip(title = "敏感操作邮件提醒", value = notifyStatus, modifier = Modifier.fillMaxWidth())
        OutlinedButton(
            onClick = onOpenSecurity,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.R12),
        ) {
            Icon(Icons.Outlined.AdminPanelSettings, contentDescription = null)
            Spacer(modifier = Modifier.width(AppSpacing.S8))
            Text("前往安全设置")
        }
    }
}

@Composable
private fun QuickActionsCard(
    onOpenProfile: () -> Unit,
    onOpenSecurity: () -> Unit,
    onOpenSessions: () -> Unit,
) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text("常用功能", style = MaterialTheme.typography.titleLarge)
        Text("这些入口用于处理资料完善、账号保护和设备管理。", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        QuickActionRow(
            icon = Icons.Outlined.Person,
            title = "编辑资料",
            subtitle = "修改头像、昵称、地区和简介",
            onClick = onOpenProfile,
        )
        QuickActionRow(
            icon = Icons.Outlined.AdminPanelSettings,
            title = "安全设置",
            subtitle = "管理双重验证和敏感操作保护",
            onClick = onOpenSecurity,
        )
        QuickActionRow(
            icon = Icons.Outlined.Devices,
            title = "设备会话",
            subtitle = "查看当前登录设备并处理异常登录",
            onClick = onOpenSessions,
        )
    }
}

@Composable
private fun ProfileCompletionCard(
    user: UserProfile?,
    completionPercent: Int,
    completedCount: Int,
    totalCount: Int,
    onOpenProfile: () -> Unit,
) {
    val displayUsername = user?.username.takeMeaningfulProfileText()
    val displayRealName = user?.realName.takeMeaningfulProfileText()
    val displayRegion = user?.region.takeMeaningfulProfileText()
    val displayBio = user?.bio.takeMeaningfulProfileText()
    val hasAvatar = !user?.avatarUrl.isNullOrBlank()

    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text("资料完善度", style = MaterialTheme.typography.titleLarge)
        Text(
            "已完成 $completedCount/$totalCount 项，当前完善度 $completionPercent%。资料越完整，账号展示越清晰。",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        CompletionItem(label = "用户名", done = displayUsername != null)
        CompletionItem(label = "头像", done = hasAvatar)
        CompletionItem(label = "真实姓名", done = displayRealName != null)
        CompletionItem(label = "地区", done = displayRegion != null)
        CompletionItem(label = "个人简介", done = displayBio != null)
        OutlinedButton(
            onClick = onOpenProfile,
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(AppRadius.R12),
        ) {
            Text("继续完善资料")
        }
    }
}

private fun String?.takeMeaningfulProfileText(): String? {
    val normalized = this?.trim().orEmpty()
    if (normalized.isBlank()) return null
    if (normalized in setOf("无", "未设置", "暂无", "null", "NULL", "-")) return null
    return normalized
}

@Composable
private fun AvatarBadge(
    avatarUrl: String?,
    label: String,
) {
    Box(
        modifier = Modifier
            .size(60.dp)
            .clip(RoundedCornerShape(AppRadius.R20))
            .background(MaterialTheme.colorScheme.primaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        if (!avatarUrl.isNullOrBlank()) {
            AsyncImage(
                model = avatarUrl,
                contentDescription = "头像",
                modifier = Modifier.fillMaxSize(),
            )
        } else {
            Text(
                text = label.ifBlank { "我" },
                style = MaterialTheme.typography.titleLarge,
                color = MaterialTheme.colorScheme.primary,
                fontWeight = FontWeight.Bold,
            )
        }
    }
}

@Composable
private fun StatusChip(
    title: String,
    value: String,
    modifier: Modifier = Modifier,
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(AppRadius.R12),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(AppSpacing.S12),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun QuickActionRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(AppRadius.R12))
            .clickable(onClick = onClick)
            .padding(vertical = AppSpacing.S8),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        Box(
            modifier = Modifier
                .size(40.dp)
                .clip(RoundedCornerShape(AppRadius.R12))
                .background(MaterialTheme.colorScheme.surfaceContainerLow),
            contentAlignment = Alignment.Center,
        ) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        }
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(subtitle, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("进入", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
    }
}

@Composable
private fun CompletionItem(
    label: String,
    done: Boolean,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Text(label, style = MaterialTheme.typography.bodyLarge)
        Text(
            if (done) "已完成" else "待完善",
            style = MaterialTheme.typography.labelMedium,
            color = if (done) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant,
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
    onNavigateToAbout: () -> Unit,
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
        ProfileInfoCard(
            title = "关于应用",
            value = "查看软件名称、版本、备案号与协议文档",
            onClick = onNavigateToAbout,
        )
    }
}

@Composable
internal fun AboutScreen() {
    val context = LocalContext.current

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8),
            ) {
                Box(
                    modifier = Modifier
                        .size(48.dp)
                        .clip(androidx.compose.foundation.shape.RoundedCornerShape(AppRadius.R16))
                        .background(MaterialTheme.colorScheme.primaryContainer),
                    contentAlignment = Alignment.Center,
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Info,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                    )
                }
                Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
                    Text("Ksuser Auth Android", style = MaterialTheme.typography.titleLarge)
                    Text("版本 1.0.0", style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            AboutInfoRow(title = "软件名称", value = "Ksuser Auth Android")
            AboutInfoRow(title = "版本号", value = "1.0.0")
            AboutInfoRow(title = "备案号", value = "沪ICP备2025144703号-2")
        }
        SectionCard(modifier = Modifier.fillMaxWidth()) {
            AboutLinkRow(
                title = "服务条款",
                url = "https://www.ksuser.cn/agreement/user.html",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ksuser.cn/agreement/user.html")),
                    )
                },
            )
            AboutLinkRow(
                title = "隐私协议",
                url = "https://www.ksuser.cn/agreement/privacy.html",
                onClick = {
                    context.startActivity(
                        Intent(Intent.ACTION_VIEW, Uri.parse("https://www.ksuser.cn/agreement/privacy.html")),
                    )
                },
            )
        }
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

@Composable
private fun AboutInfoRow(
    title: String,
    value: String,
) {
    Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text(title, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(value, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
private fun AboutLinkRow(
    title: String,
    url: String,
    onClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(androidx.compose.foundation.shape.RoundedCornerShape(AppRadius.R12))
            .clickable(onClick = onClick)
            .padding(vertical = AppSpacing.S8),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Column(modifier = Modifier.weight(1f), verticalArrangement = Arrangement.spacedBy(4.dp)) {
            Text(title, style = MaterialTheme.typography.titleMedium)
            Text(url, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.primary)
        }
        Spacer(modifier = Modifier.width(AppSpacing.S8))
        Icon(
            imageVector = Icons.AutoMirrored.Outlined.OpenInNew,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
