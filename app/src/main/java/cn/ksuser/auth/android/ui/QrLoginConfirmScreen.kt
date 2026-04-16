package cn.ksuser.auth.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import cn.ksuser.auth.android.data.model.UserProfile
import cn.ksuser.auth.android.ui.components.AppPagePadding
import cn.ksuser.auth.android.ui.components.AppRadius
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.components.GradientPrimaryButton
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
    val content = rememberConfirmationContent(pending.type, currentUser)

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(GoldGradientTopLight, GoldGradientBottomLight),
                ),
            )
            .safeDrawingPadding(),
    ) {
        IconButton(
            onClick = onCancel,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(start = 6.dp, top = 4.dp),
        ) {
            Icon(
                imageVector = Icons.Outlined.Close,
                contentDescription = "关闭",
            )
        }

        Text(
            text = content.pageTitle,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .padding(top = 18.dp),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )

        Surface(
            modifier = Modifier
                .align(Alignment.Center)
                .padding(horizontal = AppPagePadding)
                .fillMaxWidth(),
            shape = RoundedCornerShape(28.dp),
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            shadowElevation = 8.dp,
            border = CardDefaults.outlinedCardBorder().copy(
                brush = Brush.linearGradient(
                    listOf(
                        BrandButtonGradientStart.copy(alpha = 0.48f),
                        BrandButtonGradientEnd.copy(alpha = 0.24f),
                    ),
                ),
            ),
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
            ) {
                Surface(
                    modifier = Modifier.padding(top = 6.dp),
                    shape = RoundedCornerShape(AppRadius.R20),
                    color = MaterialTheme.colorScheme.surfaceContainerLow,
                ) {
                    Box(
                        modifier = Modifier
                            .padding(horizontal = 18.dp, vertical = 16.dp),
                        contentAlignment = Alignment.Center,
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.DesktopWindows,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.height(44.dp),
                        )
                    }
                }

                Spacer(modifier = Modifier.height(6.dp))

                Text(
                    text = content.title,
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center,
                )

                Text(
                    text = content.subtitle,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                pending.preview?.let { preview ->
                    QrPreviewDetails(preview = preview)
                }

                Surface(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 6.dp),
                    shape = RoundedCornerShape(AppRadius.R16),
                    color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.42f),
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalArrangement = Arrangement.spacedBy(4.dp),
                    ) {
                        Text(
                            text = "安全提醒",
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.primary,
                        )
                        Text(
                            text = content.securityHint,
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                GradientPrimaryButton(
                    text = if (isBusy) content.loadingLabel else content.confirmLabel,
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
                    Text(content.cancelLabel)
                }
            }
        }
    }
}

@Composable
private fun QrPreviewDetails(
    preview: cn.ksuser.auth.android.data.model.QrScanPreview,
) {
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(AppRadius.R16),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text(
                text = "本次请求设备",
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
            )
            QrPreviewRow("客户端", preview.clientName)
            QrPreviewRow("浏览器", preview.browser)
            QrPreviewRow("系统", preview.system)
            QrPreviewRow("IP 地址", preview.ipAddress)
            QrPreviewRow("IP 属地", preview.ipLocation)
        }
    }
}

@Composable
private fun QrPreviewRow(
    label: String,
    value: String?,
) {
    val displayValue = value?.trim().orEmpty().ifBlank { "未知" }
    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        Text(
            text = displayValue,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurface,
        )
    }
}

private data class QrConfirmationContent(
    val pageTitle: String,
    val title: String,
    val subtitle: String,
    val securityHint: String,
    val confirmLabel: String,
    val cancelLabel: String,
    val loadingLabel: String,
)

@Composable
private fun rememberConfirmationContent(
    type: QrConfirmationType,
    currentUser: UserProfile?,
): QrConfirmationContent {
    val username = currentUser?.username?.takeIf { it.isNotBlank() } ?: "当前账号"

    return when (type) {
        QrConfirmationType.APPROVE_LOGIN -> QrConfirmationContent(
            pageTitle = "登录确认",
            title = "是否允许网页端或桌面端登录",
            subtitle = "检测到新的登录请求。确认后，对应浏览器、网页端或桌面客户端将使用账号 $username 登录。",
            securityHint = "请确认这是你本人正在操作。若非本人发起，请立即取消，避免账号被其他设备登录。",
            confirmLabel = "允许登录",
            cancelLabel = "取消登录",
            loadingLabel = "提交中...",
        )

        QrConfirmationType.APPROVE_MFA -> QrConfirmationContent(
            pageTitle = "验证确认",
            title = "是否确认本次登录验证",
            subtitle = "检测到网页端或桌面端正在进行二次验证。确认后，该设备将继续完成账号 $username 的登录流程。",
            securityHint = "仅当这是你本人正在登录时才继续。若不是你发起的操作，请立即取消，避免账号被他人通过验证。",
            confirmLabel = "确认验证",
            cancelLabel = "取消",
            loadingLabel = "提交中...",
        )

        QrConfirmationType.APPROVE_SENSITIVE -> QrConfirmationContent(
            pageTitle = "操作确认",
            title = "是否允许本次敏感操作",
            subtitle = "检测到网页端或桌面端正在执行敏感操作。确认后，该设备将继续进行需要额外验证的账号操作。",
            securityHint = "请确认这是你本人正在执行的设置修改、账号安全或其他敏感操作。若非本人操作，请取消。",
            confirmLabel = "允许操作",
            cancelLabel = "取消",
            loadingLabel = "提交中...",
        )

        QrConfirmationType.LOGIN_THIS_PHONE -> QrConfirmationContent(
            pageTitle = "登录确认",
            title = "是否登录此手机",
            subtitle = "确认后，此手机将使用二维码对应账号完成登录，并进入账号主页。",
            securityHint = "请确认二维码来自你信任的设备或网页登录页，避免在陌生场景下登录他人账号。",
            confirmLabel = "确认登录",
            cancelLabel = "取消",
            loadingLabel = "登录中...",
        )

        QrConfirmationType.SWITCH_AND_LOGIN_THIS_PHONE -> QrConfirmationContent(
            pageTitle = "登录确认",
            title = "是否切换并登录此手机",
            subtitle = "当前手机已登录其他账号。确认后，将切换为二维码对应账号并继续使用此设备。",
            securityHint = "切换后，当前账号会被替换。若这不是你本人操作，请取消，避免账号混用或误登录。",
            confirmLabel = "继续登录",
            cancelLabel = "暂不切换",
            loadingLabel = "切换中...",
        )
    }
}
