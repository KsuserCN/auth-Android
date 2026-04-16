package cn.ksuser.auth.android.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Badge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.ui.components.AppOutlinedField
import cn.ksuser.auth.android.ui.components.AppPagePadding
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.components.LoadingButton
import cn.ksuser.auth.android.ui.components.LoadingOutlinedButton
import cn.ksuser.auth.android.ui.components.SectionCard

@Composable
internal fun SessionsScreen(
    container: AppContainer,
    onLogoutAll: () -> Unit,
    onMessage: (String) -> Unit,
) {
    var sessions by remember { mutableStateOf(emptyList<cn.ksuser.auth.android.data.model.SessionItem>()) }
    var loading by remember { mutableStateOf(true) }

    suspend fun reload() {
        loading = true
        runCatching { container.sessionsRepository.getSessions() }
            .onSuccess { sessions = it }
            .onFailure { onMessage(it.message ?: "加载会话失败") }
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
            Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                LoadingButton(text = "刷新", onClick = { reload() })
                LoadingOutlinedButton(text = "退出全部设备", onClick = { onLogoutAll() })
            }
        }
        if (loading) {
            item { CircularProgressIndicator() }
        }
        items(sessions) { session ->
            SectionCard(modifier = Modifier.fillMaxWidth()) {
                Text(
                    "${session.browser ?: "未知浏览器"} / ${session.deviceType ?: "未知设备"}",
                    style = MaterialTheme.typography.titleMedium,
                )
                Text(
                    "IP: ${session.ipAddress} ${session.ipLocation.orEmpty()}\n创建: ${session.createdAt}\n最近活跃: ${session.lastSeenAt}",
                    style = MaterialTheme.typography.bodySmall,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8)) {
                    if (session.current) {
                        Badge { Text("当前") }
                    }
                    if (session.online) {
                        Badge { Text("在线") }
                    }
                }
                LoadingOutlinedButton(text = "撤销此会话", onClick = {
                    val revoked = runCatching { container.sessionsRepository.revokeSession(session.id) }
                        .onFailure { onMessage(it.message ?: "撤销失败") }
                        .isSuccess
                    if (revoked) {
                        reload()
                        onMessage("会话已撤销")
                    }
                })
            }
        }
    }
}

@Composable
internal fun LogsScreen(
    container: AppContainer,
) {
    var logs by remember { mutableStateOf(emptyList<cn.ksuser.auth.android.data.model.SensitiveLogItem>()) }
    var page by rememberSaveable { mutableStateOf(1) }
    var operationType by rememberSaveable { mutableStateOf("") }
    var result by rememberSaveable { mutableStateOf("") }
    var totalPages by remember { mutableStateOf(0) }
    var busy by remember { mutableStateOf(true) }

    suspend fun reload() {
        busy = true
        runCatching {
            container.logsRepository.getSensitiveLogs(
                page = page,
                operationType = operationType.ifBlank { null },
                result = result.ifBlank { null },
            )
        }.onSuccess {
            logs = it.data
            totalPages = it.totalPages
        }
        busy = false
    }

    LaunchedEffect(page) { reload() }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(AppPagePadding),
        verticalArrangement = Arrangement.spacedBy(AppSpacing.S12),
    ) {
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8), verticalAlignment = Alignment.CenterVertically) {
            AppOutlinedField(
                value = operationType,
                onValueChange = { operationType = it },
                label = { Text("操作类型") },
                modifier = Modifier.weight(1f),
            )
            AppOutlinedField(
                value = result,
                onValueChange = { result = it },
                label = { Text("结果") },
                modifier = Modifier.weight(1f),
            )
            LoadingButton(text = "查询", onClick = { reload() })
        }
        if (busy) {
            CircularProgressIndicator()
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(AppSpacing.S12)) {
            items(logs) { log ->
                SectionCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        "${log.operationType} / ${log.result}",
                        style = MaterialTheme.typography.titleMedium,
                    )
                    Text(
                        "方式: ${log.loginMethod ?: log.loginMethods.joinToString()}\nIP: ${log.ipAddress} ${log.ipLocation.orEmpty()}\n时间: ${log.createdAt}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    if (!log.failureReason.isNullOrBlank()) {
                        Text("失败原因: ${log.failureReason}", color = MaterialTheme.colorScheme.error)
                    }
                }
            }
        }
        Row(horizontalArrangement = Arrangement.spacedBy(AppSpacing.S8), verticalAlignment = Alignment.CenterVertically) {
            LoadingOutlinedButton(text = "上一页", onClick = { if (page > 1) page-- }, enabled = page > 1)
            Text("第 $page / ${if (totalPages == 0) 1 else totalPages} 页")
            LoadingOutlinedButton(text = "下一页", onClick = { if (page < totalPages) page++ }, enabled = page < totalPages)
        }
    }
}
