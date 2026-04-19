package cn.ksuser.auth.android.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.AlternateEmail
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Shield
import androidx.compose.material.icons.outlined.SyncAlt
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.ksuser.auth.android.data.model.QrScanPreview
import cn.ksuser.auth.android.data.model.UserProfile
import cn.ksuser.auth.android.ui.components.AppPagePadding
import cn.ksuser.auth.android.ui.components.AppRadius
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.components.GradientPrimaryButton
import cn.ksuser.auth.android.ui.components.SectionCard
import cn.ksuser.auth.android.ui.theme.BrandButtonGradientEnd
import cn.ksuser.auth.android.ui.theme.BrandButtonGradientStart
import cn.ksuser.auth.android.ui.theme.GoldGradientBottomLight
import cn.ksuser.auth.android.ui.theme.GoldGradientTopLight

@Composable
internal fun QrLoginConfirmScreen(
    pending: PendingQrConfirmation,
    currentUser: UserProfile?,
    isBusy: Boolean,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
) {
    val visual = rememberConfirmationVisual(pending, currentUser)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(
                        GoldGradientTopLight,
                        GoldGradientBottomLight,
                    ),
                ),
            )
            .safeDrawingPadding(),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = AppPagePadding, vertical = AppSpacing.S12),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.S16),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                IconButton(onClick = onCancel) {
                    Icon(
                        imageVector = Icons.Outlined.Close,
                        contentDescription = "关闭",
                    )
                }

                Column(
                    modifier = Modifier.weight(1f),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "扫码确认",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                    Text(
                        text = visual.pageTitle,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                }

                Spacer(modifier = Modifier.width(48.dp))
            }

            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(30.dp),
                color = MaterialTheme.colorScheme.surface.copy(alpha = 0.96f),
                tonalElevation = 4.dp,
                shadowElevation = 10.dp,
                border = CardDefaults.outlinedCardBorder().copy(
                    brush = Brush.linearGradient(
                        listOf(
                            BrandButtonGradientStart.copy(alpha = 0.45f),
                            BrandButtonGradientEnd.copy(alpha = 0.20f),
                        ),
                    ),
                ),
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(18.dp),
                    verticalArrangement = Arrangement.spacedBy(AppSpacing.S16),
                ) {
                    ConfirmationHero(visual = visual)

                    pending.preview?.let { preview ->
                        PreviewDetailsCard(preview = preview)
                    }

                    SectionCard(modifier = Modifier.fillMaxWidth()) {
                        Text(
                            text = "安全提醒",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.SemiBold,
                        )
                        Text(
                            text = visual.securityHint,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }

                    Column(verticalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                        GradientPrimaryButton(
                            text = if (isBusy) visual.loadingLabel else visual.confirmLabel,
                            onClick = onConfirm,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                        )

                        OutlinedButton(
                            onClick = onCancel,
                            enabled = !isBusy,
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(AppRadius.R12),
                        ) {
                            Text(visual.cancelLabel)
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun ConfirmationHero(
    visual: QrConfirmationVisual,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(24.dp),
        color = Color.Transparent,
    ) {
        Box(
            modifier = Modifier
                .background(
                    Brush.linearGradient(
                        listOf(
                            visual.accentSoft,
                            MaterialTheme.colorScheme.surface,
                        ),
                    ),
                    shape = RoundedCornerShape(24.dp),
                )
                .padding(horizontal = 18.dp, vertical = 20.dp),
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.S8),
            ) {
                Surface(
                    modifier = Modifier.size(68.dp),
                    shape = CircleShape,
                    color = visual.accent.copy(alpha = 0.14f),
                    border = BorderStroke(1.dp, visual.accent.copy(alpha = 0.18f)),
                ) {
                    Box(contentAlignment = Alignment.Center) {
                        Icon(
                            imageVector = visual.icon,
                            contentDescription = null,
                            tint = visual.accent,
                            modifier = Modifier.size(32.dp),
                        )
                    }
                }

                Surface(
                    shape = RoundedCornerShape(999.dp),
                    color = visual.accent.copy(alpha = 0.12f),
                ) {
                    Text(
                        text = visual.operationLabel,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp),
                        style = MaterialTheme.typography.labelLarge,
                        color = visual.accent,
                    )
                }

                Text(
                    text = visual.title,
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = visual.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                )
            }
        }
    }
}

@Composable
private fun PreviewDetailsCard(
    preview: QrScanPreview,
) {
    SectionCard(modifier = Modifier.fillMaxWidth()) {
        Text(
            text = "请求来源",
            style = MaterialTheme.typography.titleSmall,
            fontWeight = FontWeight.SemiBold,
        )
        PreviewRow("客户端", preview.clientName)
        PreviewRow("浏览器", preview.browser)
        PreviewRow("系统", preview.system)
        PreviewRow("IP 地址", preview.ipAddress)
        PreviewRow("IP 属地", preview.ipLocation)
    }
}

@Composable
private fun PreviewRow(
    label: String,
    value: String?,
) {
    val displayValue = value?.trim().orEmpty().ifBlank { "未知" }
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.Top,
    ) {
        Text(
            text = label,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            text = displayValue,
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.End,
        )
    }
}

private data class QrConfirmationVisual(
    val pageTitle: String,
    val operationLabel: String,
    val title: String,
    val subtitle: String,
    val securityHint: String,
    val confirmLabel: String,
    val cancelLabel: String,
    val loadingLabel: String,
    val accountLabel: String,
    val icon: ImageVector,
    val accent: Color,
    val accentSoft: Color,
)

@Composable
private fun rememberConfirmationVisual(
    pending: PendingQrConfirmation,
    currentUser: UserProfile?,
): QrConfirmationVisual {
    val username = remember(currentUser) {
        currentUser?.username?.takeIf { it.isNotBlank() } ?: "当前账号"
    }
    val operation = pending.operationHint ?: defaultOperationHint(pending.type)
    val loginCopy = remember(pending.preview) { resolveLoginCopy(pending.preview) }

    return when (operation) {
        "CHANGE_PASSWORD" -> buildVisual(
            pageTitle = "修改密码确认",
            operationLabel = "修改密码",
            title = "是否允许本次密码修改",
            subtitle = "检测到网页端或桌面端正在尝试修改账号 $username 的登录密码。",
            securityHint = "确认前请先核对设备信息。若不是你本人正在修改密码，请立即取消并检查账号安全。",
            confirmLabel = "允许修改",
            loadingLabel = "提交中...",
            icon = Icons.Outlined.Password,
            accent = MaterialTheme.colorScheme.error,
            accentSoft = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
            accountLabel = username,
        )

        "CHANGE_EMAIL" -> buildVisual(
            pageTitle = "修改邮箱确认",
            operationLabel = "修改邮箱",
            title = "是否允许本次邮箱变更",
            subtitle = "检测到网页端或桌面端正在尝试修改账号 $username 绑定的邮箱地址。",
            securityHint = "邮箱会影响验证码接收和账号找回。若不是你本人操作，请不要继续。",
            confirmLabel = "允许修改",
            loadingLabel = "提交中...",
            icon = Icons.Outlined.AlternateEmail,
            accent = MaterialTheme.colorScheme.secondary,
            accentSoft = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
            accountLabel = username,
        )

        "ADD_PASSKEY" -> buildVisual(
            pageTitle = "新增 Passkey 确认",
            operationLabel = "新增 Passkey",
            title = "是否允许新增 Passkey",
            subtitle = "检测到网页端或桌面端正在为账号 $username 绑定新的 Passkey 凭证。",
            securityHint = "只有在你本人正在添加新的安全凭证时才应继续，避免他人向账号写入可登录凭证。",
            confirmLabel = "允许添加",
            loadingLabel = "提交中...",
            icon = Icons.Outlined.Key,
            accent = MaterialTheme.colorScheme.primary,
            accentSoft = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            accountLabel = username,
        )

        "DELETE_PASSKEY" -> buildVisual(
            pageTitle = "删除 Passkey 确认",
            operationLabel = "删除 Passkey",
            title = "是否允许删除 Passkey",
            subtitle = "检测到网页端或桌面端正在删除账号 $username 的某个 Passkey。",
            securityHint = "删除后可能影响你后续无密码登录。请确认这是你本人在管理安全凭证。",
            confirmLabel = "允许删除",
            loadingLabel = "提交中...",
            icon = Icons.Outlined.Key,
            accent = MaterialTheme.colorScheme.tertiary,
            accentSoft = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
            accountLabel = username,
        )

        "ENABLE_TOTP" -> buildVisual(
            pageTitle = "启用 TOTP 确认",
            operationLabel = "启用 TOTP",
            title = "是否允许启用 TOTP",
            subtitle = "检测到网页端或桌面端正在为账号 $username 开启动态验证码验证。",
            securityHint = "启用后将提升账号安全性，但请确保这是你本人在配置新的验证方式。",
            confirmLabel = "允许启用",
            loadingLabel = "提交中...",
            icon = Icons.Outlined.Shield,
            accent = MaterialTheme.colorScheme.primary,
            accentSoft = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            accountLabel = username,
        )

        "DISABLE_TOTP" -> buildVisual(
            pageTitle = "禁用 TOTP 确认",
            operationLabel = "禁用 TOTP",
            title = "是否允许禁用 TOTP",
            subtitle = "检测到网页端或桌面端正在尝试关闭账号 $username 的动态验证码验证。",
            securityHint = "关闭二次验证会降低账号安全性。若不是你本人在调整安全设置，请立即取消。",
            confirmLabel = "允许关闭",
            loadingLabel = "提交中...",
            icon = Icons.Outlined.AdminPanelSettings,
            accent = MaterialTheme.colorScheme.error,
            accentSoft = MaterialTheme.colorScheme.error.copy(alpha = 0.08f),
            accountLabel = username,
        )

        "MFA_VERIFY" -> buildVisual(
            pageTitle = "验证确认",
            operationLabel = "二次验证",
            title = "是否确认本次登录验证",
            subtitle = "检测到网页端或桌面端正在进行二次验证。确认后，该设备将继续完成账号 $username 的登录流程。",
            securityHint = "仅当这是你本人正在登录时才继续。若不是你发起的操作，请立即取消。",
            confirmLabel = "确认验证",
            loadingLabel = "提交中...",
            icon = Icons.Outlined.Shield,
            accent = MaterialTheme.colorScheme.secondary,
            accentSoft = MaterialTheme.colorScheme.secondary.copy(alpha = 0.10f),
            accountLabel = username,
        )

        "SENSITIVE_VERIFY" -> buildVisual(
            pageTitle = "敏感操作确认",
            operationLabel = "敏感操作",
            title = "是否允许本次敏感操作",
            subtitle = "检测到网页端或桌面端正在执行需要额外验证的账号操作。",
            securityHint = "请确认这是你本人正在执行的设置修改、账号安全或其他敏感操作。若非本人操作，请取消。",
            confirmLabel = "允许操作",
            loadingLabel = "提交中...",
            icon = Icons.Outlined.Security,
            accent = MaterialTheme.colorScheme.tertiary,
            accentSoft = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
            accountLabel = username,
        )

        "LOGIN_THIS_PHONE" -> buildVisual(
            pageTitle = "登录此手机",
            operationLabel = "跨端登录",
            title = "是否登录此手机",
            subtitle = "确认后，此手机将使用二维码对应账号完成登录，并进入账号主页。",
            securityHint = "请确认二维码来自你信任的网页登录页或设备，避免在陌生场景下登录他人账号。",
            confirmLabel = "确认登录",
            loadingLabel = "登录中...",
            icon = Icons.Outlined.PhoneAndroid,
            accent = MaterialTheme.colorScheme.primary,
            accentSoft = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            accountLabel = "将登录到本机",
        )

        "SWITCH_AND_LOGIN_THIS_PHONE" -> buildVisual(
            pageTitle = "切换并登录",
            operationLabel = "切换账号登录",
            title = "是否切换并登录此手机",
            subtitle = "当前手机已登录其他账号。确认后，将切换为二维码对应账号并继续使用此设备。",
            securityHint = "切换后，当前账号会被替换。若这不是你本人操作，请取消，避免账号混用或误登录。",
            confirmLabel = "继续登录",
            cancelLabel = "暂不切换",
            loadingLabel = "切换中...",
            icon = Icons.Outlined.SyncAlt,
            accent = MaterialTheme.colorScheme.tertiary,
            accentSoft = MaterialTheme.colorScheme.tertiary.copy(alpha = 0.10f),
            accountLabel = username,
        )

        else -> buildVisual(
            pageTitle = loginCopy.pageTitle,
            operationLabel = loginCopy.operationLabel,
            title = loginCopy.title,
            subtitle = loginCopy.subtitlePrefix + "确认后，对应设备将使用账号 $username 登录。",
            securityHint = loginCopy.securityHint,
            confirmLabel = "允许登录",
            loadingLabel = "提交中...",
            icon = loginCopy.icon,
            accent = MaterialTheme.colorScheme.primary,
            accentSoft = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f),
            accountLabel = username,
        )
    }
}

@Composable
private fun buildVisual(
    pageTitle: String,
    operationLabel: String,
    title: String,
    subtitle: String,
    securityHint: String,
    confirmLabel: String,
    loadingLabel: String,
    icon: ImageVector,
    accent: Color,
    accentSoft: Color,
    accountLabel: String,
    cancelLabel: String = "取消",
): QrConfirmationVisual {
    return QrConfirmationVisual(
        pageTitle = pageTitle,
        operationLabel = operationLabel,
        title = title,
        subtitle = subtitle,
        securityHint = securityHint,
        confirmLabel = confirmLabel,
        cancelLabel = cancelLabel,
        loadingLabel = loadingLabel,
        accountLabel = accountLabel,
        icon = icon,
        accent = accent,
        accentSoft = accentSoft,
    )
}

private fun defaultOperationHint(type: QrConfirmationType): String {
    return when (type) {
        QrConfirmationType.APPROVE_LOGIN -> "LOGIN"
        QrConfirmationType.APPROVE_MFA -> "MFA_VERIFY"
        QrConfirmationType.APPROVE_SENSITIVE -> "SENSITIVE_VERIFY"
        QrConfirmationType.LOGIN_THIS_PHONE -> "LOGIN_THIS_PHONE"
        QrConfirmationType.SWITCH_AND_LOGIN_THIS_PHONE -> "SWITCH_AND_LOGIN_THIS_PHONE"
    }
}

private data class LoginCopy(
    val pageTitle: String,
    val operationLabel: String,
    val title: String,
    val subtitlePrefix: String,
    val securityHint: String,
    val icon: ImageVector,
)

private fun resolveLoginCopy(preview: QrScanPreview?): LoginCopy {
    val clientName = preview?.clientName.orEmpty().trim()
    val browser = preview?.browser.orEmpty().trim()
    val isDesktop = clientName.contains("桌面", ignoreCase = true) ||
        browser.startsWith("Ksuser Auth Desktop", ignoreCase = true)

    return if (isDesktop) {
        LoginCopy(
            pageTitle = "桌面登录确认",
            operationLabel = "桌面端登录",
            title = "确认这次桌面登录吗",
            subtitlePrefix = "检测到一台桌面设备正在请求登录。 ",
            securityHint = "请确认这是你本人正在使用的桌面设备。若非本人发起，请立即取消，避免账号被其他电脑登录。",
            icon = Icons.Outlined.DesktopWindows,
        )
    } else {
        LoginCopy(
            pageTitle = "网页登录确认",
            operationLabel = "网页登录",
            title = "确认这次网页登录吗",
            subtitlePrefix = "检测到浏览器发起新的登录请求。 ",
            securityHint = "请确认这是你本人正在访问的网页。若非本人发起，请立即取消，避免账号被其他浏览器登录。",
            icon = Icons.Outlined.Security,
        )
    }
}
