package cn.ksuser.auth.android.ui

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.ui.graphics.vector.ImageVector

internal enum class MainDestination(
    val route: String,
    val label: String,
    val icon: ImageVector,
) {
    HOME("home", "概览", Icons.Outlined.Security),
    PROFILE("profile", "资料", Icons.Outlined.Person),
    SECURITY("security", "安全", Icons.Outlined.AdminPanelSettings),
    SESSIONS("sessions", "会话", Icons.Outlined.DeviceHub),
    LOGS("logs", "日志", Icons.Outlined.Visibility),
}
