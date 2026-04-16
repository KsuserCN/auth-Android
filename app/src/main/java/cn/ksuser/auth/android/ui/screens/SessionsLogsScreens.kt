package cn.ksuser.auth.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Computer
import androidx.compose.material.icons.outlined.DesktopWindows
import androidx.compose.material.icons.outlined.Language
import androidx.compose.material.icons.outlined.LaptopChromebook
import androidx.compose.material.icons.outlined.LaptopMac
import androidx.compose.material.icons.outlined.LaptopWindows
import androidx.compose.material.icons.outlined.PhoneAndroid
import androidx.compose.material.icons.outlined.PhoneIphone
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AssistChip
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.vector.ImageVector
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.data.model.SensitiveLogItem
import cn.ksuser.auth.android.data.model.SessionItem
import cn.ksuser.auth.android.ui.components.AppPagePadding
import cn.ksuser.auth.android.ui.components.AppRadius
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.components.LoadingOutlinedButton
import cn.ksuser.auth.android.ui.components.SectionCard
import java.time.Instant
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import kotlinx.coroutines.launch

@Composable
internal fun SessionsScreen(
    container: AppContainer,
    onLogoutAll: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var sessions by remember { mutableStateOf(emptyList<SessionItem>()) }
    var loading by remember { mutableStateOf(true) }
    var pendingRevokeSession by remember { mutableStateOf<SessionItem?>(null) }
    val scope = rememberCoroutineScope()

    suspend fun reload() {
        loading = true
        runCatching { container.sessionsRepository.getSessions() }
            .onSuccess { sessions = it }
            .onFailure { onMessage(it.message ?: "加载会话列表失败") }
        loading = false
    }
 
    LaunchedEffect(Unit) { reload() }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Text("设备与登录", style = MaterialTheme.typography.headlineMedium)
                    Text(
                        "查看和管理您已连接的设备",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
                SmallRefreshButton(onClick = { reload() })
            }
        }

        if (loading && sessions.isEmpty()) {
            items(3) { SessionSkeletonCard() }
        } else if (sessions.isEmpty()) {
            item {
                SectionCard {
                    Text("暂无设备", style = MaterialTheme.typography.titleMedium)
                    Text(
                        "当前没有活跃会话记录",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        items(sessions, key = { it.id }) { session ->
            SessionCardWithCornerRevoke(
                session = session,
                onRequestRevoke = { pendingRevokeSession = session },
            )
        }

        item {
            SectionCard {
                Text(
                    "危险操作区域",
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.error,
                )
                Text(
                    "从所有设备退出登录将撤销所有会话（包括当前设备），需要重新登录。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                LoadingOutlinedButton(
                    text = "退出所有设备",
                    onClick = { onLogoutAll() },
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }

    pendingRevokeSession?.let { target ->
        AlertDialog(
            onDismissRequest = { pendingRevokeSession = null },
            title = { Text("确认撤销会话") },
            text = {
                Text(
                    "将撤销 ${getClientDisplayName(target)}（${target.ipAddress}）的登录状态，确定继续吗？",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        pendingRevokeSession = null
                        if (target.current) {
                            onMessage("无法撤销当前会话")
                            return@TextButton
                        }
                        scope.launch {
                            val revoked = runCatching { container.sessionsRepository.revokeSession(target.id) }
                                .onFailure { onMessage(it.message ?: "撤销会话失败") }
                                .isSuccess
                            if (revoked) {
                                reload()
                                onMessage("会话已撤销")
                            }
                        }
                    },
                ) { Text("确认") }
            },
            dismissButton = {
                TextButton(onClick = { pendingRevokeSession = null }) { Text("取消") }
            },
        )
    }
}

@Composable
internal fun LogsScreen(
    container: AppContainer,
) {
    var logs by remember { mutableStateOf(emptyList<SensitiveLogItem>()) }
    var page by rememberSaveable { mutableStateOf(1) }
    var selectedOperationType by rememberSaveable { mutableStateOf<String?>(null) }
    var selectedResult by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedOperationType by rememberSaveable { mutableStateOf<String?>(null) }
    var appliedResult by rememberSaveable { mutableStateOf<String?>(null) }
    var filterExpanded by rememberSaveable { mutableStateOf(false) }
    var totalPages by remember { mutableStateOf(0) }
    var total by remember { mutableStateOf(0L) }
    var busy by remember { mutableStateOf(true) }

    suspend fun reload() {
        busy = true
        runCatching {
            container.logsRepository.getSensitiveLogs(
                page = page,
                operationType = appliedOperationType,
                result = appliedResult,
            )
        }.onSuccess {
            logs = it.data
            totalPages = it.totalPages
            total = it.total
        }
        busy = false
    }

    LaunchedEffect(page, appliedOperationType, appliedResult) { reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                Text("近期敏感操作", style = MaterialTheme.typography.headlineMedium)
                Text(
                    "查看安全相关操作记录与风险分数",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            SmallRefreshButton(onClick = { reload() })
        }

        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(10.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text("筛选条件", style = MaterialTheme.typography.titleSmall)
                CompactOutlinedButton(
                    text = if (filterExpanded) "收起" else "展开",
                    onClick = { filterExpanded = !filterExpanded },
                )
            }
            if (filterExpanded) {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                    items(OperationTypeOptions) { option ->
                        FilterChip(
                            selected = selectedOperationType == option.value,
                            onClick = {
                                selectedOperationType = if (selectedOperationType == option.value) null else option.value
                            },
                            label = { Text(option.label) },
                        )
                    }
                }
                LazyRow(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                    items(ResultOptions) { option ->
                        FilterChip(
                            selected = selectedResult == option.value,
                            onClick = { selectedResult = if (selectedResult == option.value) null else option.value },
                            label = { Text(option.label) },
                        )
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                    CompactPrimaryButton(
                        text = "查询",
                        onClick = {
                            page = 1
                            appliedOperationType = selectedOperationType
                            appliedResult = selectedResult
                        },
                    )
                    CompactOutlinedButton(
                        text = "重置",
                        onClick = {
                            page = 1
                            selectedOperationType = null
                            selectedResult = null
                            appliedOperationType = null
                            appliedResult = null
                        },
                    )
                    AssistChip(onClick = {}, enabled = false, label = { Text("共 $total 条") })
                }
            } else {
                val operationLabel = OperationTypeOptions.firstOrNull { it.value == selectedOperationType }?.label ?: "全部操作"
                val resultLabel = ResultOptions.firstOrNull { it.value == selectedResult }?.label ?: "全部结果"
                Text(
                    "当前：$operationLabel / $resultLabel",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        if (busy && logs.isEmpty()) {
            repeat(3) { LogSkeletonCard() }
        } else if (logs.isEmpty()) {
            SectionCard {
                Text("暂无记录", style = MaterialTheme.typography.titleMedium)
                Text(
                    "当前筛选条件下没有匹配的敏感操作记录",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        LazyColumn(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
        ) {
            items(logs, key = { it.id }) { log ->
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Top,
                    ) {
                        Column(
                            modifier = Modifier.weight(1f),
                            verticalArrangement = Arrangement.spacedBy(6.dp),
                        ) {
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                Text(getOperationTitle(log.operationType), style = MaterialTheme.typography.titleMedium)
                                getOperationTags(log).forEach { tag ->
                                    StatusPill(
                                        text = tag,
                                        background = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
                                        content = MaterialTheme.colorScheme.primary,
                                    )
                                }
                            }
                            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                                StatusPill(
                                    text = if (log.result.equals("SUCCESS", true)) "成功" else "失败",
                                    background = if (log.result.equals("SUCCESS", true)) {
                                        MaterialTheme.colorScheme.tertiaryContainer
                                    } else {
                                        MaterialTheme.colorScheme.errorContainer
                                    },
                                    content = if (log.result.equals("SUCCESS", true)) {
                                        MaterialTheme.colorScheme.tertiary
                                    } else {
                                        MaterialTheme.colorScheme.error
                                    },
                                )
                                Text(
                                    "${log.ipLocation ?: "未知位置"} · ${log.ipAddress}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Text(
                                "设备：${log.deviceType ?: "未知设备"} · 浏览器：${log.browser ?: "未知浏览器"}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                            Text(
                                "时间：${formatAbsoluteTime(log.createdAt)}",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                            )
                        }
                        RiskPill(score = log.riskScore)
                    }
                    if (!log.failureReason.isNullOrBlank()) {
                        Text(
                            "失败原因：${log.failureReason}",
                            color = MaterialTheme.colorScheme.error,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
            }
        }

        if (busy && logs.isNotEmpty()) {
            Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.Center) {
                CircularProgressIndicator(modifier = Modifier.size(22.dp))
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            CompactOutlinedButton(
                text = "上一页",
                onClick = { if (page > 1) page-- },
                enabled = page > 1,
            )
            Text(
                "第 $page / ${if (totalPages == 0) 1 else totalPages} 页",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.weight(1f),
            )
            CompactOutlinedButton(
                text = "下一页",
                onClick = { if (page < totalPages) page++ },
                enabled = page < totalPages,
            )
        }
    }
}

@Composable
private fun SessionCardWithCornerRevoke(
    session: SessionItem,
    onRequestRevoke: () -> Unit,
) {
    val showAction = !session.current

    Box(modifier = Modifier.fillMaxWidth()) {
        SectionCard(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(AppSpacing.S16),
        ) {
            SessionCardContent(session = session)
        }

        if (showAction) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomEnd)
                    .size(44.dp)
                    .clip(
                        RoundedCornerShape(
                            topStart = AppRadius.R12,
                            topEnd = 0.dp,
                            bottomStart = 0.dp,
                            bottomEnd = AppRadius.R20,
                        ),
                    )
                    .background(MaterialTheme.colorScheme.error.copy(alpha = 0.78f))
                    .clickable { onRequestRevoke() },
                contentAlignment = Alignment.Center,
            ) {
                Icon(
                    imageVector = Icons.Outlined.DeleteOutline,
                    contentDescription = "撤销会话",
                    tint = MaterialTheme.colorScheme.onError,
                    modifier = Modifier.size(18.dp),
                )
            }
        }
    }
}

@Composable
private fun SessionCardContent(session: SessionItem) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(AppSpacing.S12),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        SessionLogo(session)
        Column(
            modifier = Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(2.dp),
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                Text(
                    getClientDisplayName(session),
                    style = MaterialTheme.typography.titleMedium,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    modifier = Modifier.weight(1f),
                )
                if (session.current) {
                    StatusPill(
                        text = "当前",
                        background = MaterialTheme.colorScheme.primaryContainer,
                        content = MaterialTheme.colorScheme.primary,
                    )
                }
                StatusPill(
                    text = if (session.online) "在线" else "离线",
                    background = if (session.online) {
                        MaterialTheme.colorScheme.tertiaryContainer
                    } else {
                        MaterialTheme.colorScheme.surfaceVariant
                    },
                    content = if (session.online) {
                        MaterialTheme.colorScheme.tertiary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                )
            }
            Text(
                "${getSystemDisplayName(session)} · ${session.ipAddress}",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }

    HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.7f))

    Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Text(session.ipLocation ?: "未知位置", style = MaterialTheme.typography.bodyMedium)
        Text(
            "最近活动：${formatAbsoluteTime(session.lastSeenAt)}",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun SessionLogo(session: SessionItem) {
    val (clientBg, clientFg) = when (getClientKind(session)) {
        ClientKind.DesktopApp -> MaterialTheme.colorScheme.primaryContainer to MaterialTheme.colorScheme.primary
        ClientKind.MobileApp -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
        ClientKind.Web -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    val icon = resolveSessionIcon(session)
    Box(
        modifier = Modifier
            .size(42.dp)
            .clip(CircleShape)
            .background(clientBg),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = getSystemDisplayName(session),
            tint = clientFg,
            modifier = Modifier.size(22.dp),
        )
    }
}

@Composable
private fun StatusPill(
    text: String,
    background: Color,
    content: Color,
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(999.dp))
            .background(background)
            .padding(horizontal = 8.dp, vertical = 3.dp),
    ) {
        Text(text, color = content, style = MaterialTheme.typography.labelSmall)
    }
}

@Composable
private fun RiskPill(score: Int) {
    val (bg, fg) = when {
        score >= 70 -> MaterialTheme.colorScheme.errorContainer to MaterialTheme.colorScheme.error
        score >= 40 -> MaterialTheme.colorScheme.secondaryContainer to MaterialTheme.colorScheme.secondary
        else -> MaterialTheme.colorScheme.tertiaryContainer to MaterialTheme.colorScheme.tertiary
    }
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(4.dp)) {
        Text("风险", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        StatusPill(text = score.toString(), background = bg, content = fg)
    }
}

@Composable
private fun CompactPrimaryButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    Button(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(38.dp),
        shape = RoundedCornerShape(AppRadius.R12),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun CompactOutlinedButton(
    text: String,
    onClick: () -> Unit,
    enabled: Boolean = true,
) {
    OutlinedButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier.height(38.dp),
        shape = RoundedCornerShape(AppRadius.R12),
        contentPadding = PaddingValues(horizontal = 14.dp, vertical = 0.dp),
    ) {
        Text(text, style = MaterialTheme.typography.labelMedium)
    }
}

@Composable
private fun SmallRefreshButton(
    onClick: suspend () -> Unit,
) {
    val scope = rememberCoroutineScope()
    OutlinedButton(
        onClick = { scope.launch { onClick() } },
        modifier = Modifier.height(32.dp),
        shape = RoundedCornerShape(AppRadius.R12),
        contentPadding = PaddingValues(horizontal = 10.dp, vertical = 0.dp),
    ) {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = null,
            modifier = Modifier.size(14.dp),
        )
        Text(
            "刷新",
            style = MaterialTheme.typography.labelSmall,
            modifier = Modifier.padding(start = 4.dp),
        )
    }
}

@Composable
private fun SessionSkeletonCard() {
    SectionCard {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(18.dp)
                .clip(RoundedCornerShape(AppRadius.R8))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.75f)
                .height(14.dp)
                .clip(RoundedCornerShape(AppRadius.R8))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.42f)
                .height(12.dp)
                .clip(RoundedCornerShape(AppRadius.R8))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f)),
        )
    }
}

@Composable
private fun LogSkeletonCard() {
    SectionCard {
        Box(
            modifier = Modifier
                .fillMaxWidth(0.9f)
                .height(16.dp)
                .clip(RoundedCornerShape(AppRadius.R8))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.8f)
                .height(12.dp)
                .clip(RoundedCornerShape(AppRadius.R8))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)),
        )
        Box(
            modifier = Modifier
                .fillMaxWidth(0.7f)
                .height(12.dp)
                .clip(RoundedCornerShape(AppRadius.R8))
                .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.38f)),
        )
    }
}

private enum class ClientKind {
    DesktopApp,
    MobileApp,
    Web,
}

private fun getClientKind(session: SessionItem?): ClientKind {
    val ua = (session?.userAgent ?: "").lowercase(Locale.getDefault())
    return when {
        ua.contains("ksuserauthdesktop") -> ClientKind.DesktopApp
        ua.contains("ksuserauthmobile") -> ClientKind.MobileApp
        else -> ClientKind.Web
    }
}

private fun getClientDisplayName(session: SessionItem): String {
    return when (getClientKind(session)) {
        ClientKind.DesktopApp -> "桌面端"
        ClientKind.MobileApp -> "移动端"
        ClientKind.Web -> getBrowserDisplayName(session)
    }
}

private fun getBrowserDisplayName(session: SessionItem?): String {
    val raw = (session?.browser ?: session?.userAgent ?: "").lowercase(Locale.getDefault())
    return when {
        raw.contains("edge") || raw.contains("edg/") -> "Edge"
        raw.contains("firefox") -> "Firefox"
        raw.contains("opera") || raw.contains("opr/") -> "Opera"
        raw.contains("chrome") -> "Chrome"
        raw.contains("safari") -> "Safari"
        raw.contains("ie") || raw.contains("trident") -> "Internet Explorer"
        else -> session?.browser ?: "未知浏览器"
    }
}

private fun getSystemDisplayName(session: SessionItem?): String {
    val ua = (session?.userAgent ?: session?.deviceType ?: "").lowercase(Locale.getDefault())
    return when {
        ua.contains("android") -> "Android"
        ua.contains("iphone") || ua.contains("ipad") || ua.contains("ios") -> "iOS"
        ua.contains("windows") -> "Windows"
        ua.contains("mac os") || ua.contains("macintosh") || ua.contains("mac") -> "macOS"
        ua.contains("linux") || ua.contains("x11") -> "Linux"
        ua.contains("cros") || ua.contains("chromeos") -> "ChromeOS"
        getClientKind(session) == ClientKind.DesktopApp -> "桌面端"
        getClientKind(session) == ClientKind.MobileApp -> "移动端"
        else -> session?.deviceType ?: "未知设备"
    }
}

private fun resolveSessionIcon(session: SessionItem): ImageVector {
    val system = getSystemDisplayName(session)
    val clientKind = getClientKind(session)
    return when {
        clientKind == ClientKind.DesktopApp -> Icons.Outlined.DesktopWindows
        clientKind == ClientKind.MobileApp && system == "iOS" -> Icons.Outlined.PhoneIphone
        clientKind == ClientKind.MobileApp -> Icons.Outlined.PhoneAndroid
        system == "Windows" -> Icons.Outlined.LaptopWindows
        system == "macOS" -> Icons.Outlined.LaptopMac
        system == "Android" -> Icons.Outlined.PhoneAndroid
        system == "iOS" -> Icons.Outlined.PhoneIphone
        system == "ChromeOS" -> Icons.Outlined.LaptopChromebook
        system == "Linux" -> Icons.Outlined.Computer
        clientKind == ClientKind.Web -> Icons.Outlined.Language
        else -> Icons.Outlined.Computer
    }
}

private val OperationTypeOptions = listOf(
    FilterOption(null, "全部操作"),
    FilterOption("LOGIN", "登录"),
    FilterOption("REGISTER", "注册"),
    FilterOption("SENSITIVE_VERIFY", "敏感验证"),
    FilterOption("CHANGE_PASSWORD", "修改密码"),
    FilterOption("CHANGE_EMAIL", "修改邮箱"),
    FilterOption("ADD_PASSKEY", "新增 Passkey"),
    FilterOption("DELETE_PASSKEY", "删除 Passkey"),
    FilterOption("ENABLE_TOTP", "启用 TOTP"),
    FilterOption("DISABLE_TOTP", "禁用 TOTP"),
)

private val ResultOptions = listOf(
    FilterOption(null, "全部结果"),
    FilterOption("SUCCESS", "成功"),
    FilterOption("FAILURE", "失败"),
)

private data class FilterOption(
    val value: String?,
    val label: String,
)

private fun getOperationLabel(op: String?): String {
    return when (op?.uppercase(Locale.getDefault())) {
        "REGISTER" -> "注册"
        "LOGIN" -> "登录"
        "SENSITIVE_VERIFY" -> "敏感验证"
        "CHANGE_PASSWORD" -> "修改密码"
        "CHANGE_EMAIL" -> "修改邮箱"
        "ADD_PASSKEY" -> "新增 Passkey"
        "DELETE_PASSKEY" -> "删除 Passkey"
        "ENABLE_TOTP" -> "启用 TOTP"
        "DISABLE_TOTP" -> "禁用 TOTP"
        else -> op ?: "-"
    }
}

private fun getOperationTitle(op: String?): String {
    return when (op?.uppercase(Locale.getDefault())) {
        "LOGIN" -> "登录"
        "REGISTER" -> "注册"
        else -> "敏感操作"
    }
}

private fun normalizeLoginTokenLabel(token: String): String {
    val key = token.trim().uppercase(Locale.getDefault())
    if (key.isBlank()) return ""
    return when (key) {
        "PASSKEY" -> "Passkey"
        "MFA" -> "MFA"
        "PASSWORD" -> "密码"
        "EMAIL", "EMAIL_CODE" -> "验证码"
        "TOTP" -> "TOTP"
        "QR" -> "扫码"
        "GOOGLE" -> "Google"
        "GITHUB" -> "GitHub"
        "MICROSOFT" -> "Microsoft"
        "QQ" -> "QQ"
        "WECHAT", "WEIXIN" -> "微信"
        "BRIDGE" -> "桥接登录"
        "BRIDGE_FROM_DESKTOP" -> "电脑端桥接"
        "BRIDGE_FROM_WEB" -> "网页端桥接"
        "BRIDGE_TO_MOBILE" -> "桥接到手机端"
        else -> token.trim()
    }
}

private fun getLoginMethodTags(log: SensitiveLogItem): List<String> {
    val fromArray = log.loginMethods
        .map { normalizeLoginTokenLabel(it) }
        .filter { it.isNotBlank() }
    if (fromArray.isNotEmpty()) return fromArray.distinct()

    val raw = log.loginMethod?.trim().orEmpty()
    if (raw.isBlank() || raw.equals("null", true)) return emptyList()
    val normalized = raw.uppercase(Locale.getDefault())
    if (normalized.endsWith("_MFA")) {
        return listOf(
            normalizeLoginTokenLabel(normalized.removeSuffix("_MFA")),
            "MFA",
        )
    }
    if (raw.startsWith("[") && raw.endsWith("]")) {
        return raw.removePrefix("[").removeSuffix("]")
            .split(',')
            .map { normalizeLoginTokenLabel(it) }
            .filter { it.isNotBlank() }
    }
    return listOf(normalizeLoginTokenLabel(raw)).filter { it.isNotBlank() }
}

private fun getOperationTags(log: SensitiveLogItem): List<String> {
    val op = log.operationType.uppercase(Locale.getDefault())
    return when (op) {
        "REGISTER" -> emptyList()
        "LOGIN" -> getLoginMethodTags(log)
        else -> listOf(getOperationLabel(log.operationType))
    }
}

private fun formatAbsoluteTime(value: String?): String {
    if (value.isNullOrBlank()) return "-"
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd hh:mm:ss")
    return runCatching {
        OffsetDateTime.parse(value).toLocalDateTime().format(formatter)
    }.recoverCatching {
        LocalDateTime.parse(value, DateTimeFormatter.ISO_LOCAL_DATE_TIME).format(formatter)
    }.recoverCatching {
        Instant.parse(value).atZone(ZoneId.systemDefault()).toLocalDateTime().format(formatter)
    }.getOrElse {
        value.replace('T', ' ')
    }
}
