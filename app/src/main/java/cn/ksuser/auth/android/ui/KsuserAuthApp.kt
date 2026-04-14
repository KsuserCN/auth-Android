package cn.ksuser.auth.android.ui

import android.app.Activity
import android.content.Context
import android.os.Build
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.PickVisualMediaRequest
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.AdminPanelSettings
import androidx.compose.material.icons.outlined.Badge
import androidx.compose.material.icons.outlined.ContentCopy
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.DeviceHub
import androidx.compose.material.icons.outlined.Key
import androidx.compose.material.icons.outlined.Lock
import androidx.compose.material.icons.outlined.Password
import androidx.compose.material.icons.outlined.Person
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Security
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.icons.outlined.WarningAmber
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Badge
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import coil.compose.AsyncImage
import cn.ksuser.auth.android.KsuserAuthApplication
import cn.ksuser.auth.android.core.app.AppIdentityProvider
import cn.ksuser.auth.android.core.env.EnvironmentProvider
import cn.ksuser.auth.android.data.AppContainer
import cn.ksuser.auth.android.data.model.AuthResult
import cn.ksuser.auth.android.data.model.PasskeyAvailability
import cn.ksuser.auth.android.data.model.PasskeyListItem
import cn.ksuser.auth.android.data.model.SensitiveVerificationStatus
import cn.ksuser.auth.android.data.model.TotpRegistrationOptions
import cn.ksuser.auth.android.data.model.TotpStatus
import cn.ksuser.auth.android.data.model.UserProfile
import kotlinx.coroutines.launch

private enum class MainDestination(
    val route: String,
    val label: String,
    val icon: androidx.compose.ui.graphics.vector.ImageVector,
) {
    HOME("home", "概览", Icons.Outlined.Security),
    PROFILE("profile", "资料", Icons.Outlined.Person),
    SECURITY("security", "安全", Icons.Outlined.AdminPanelSettings),
    SESSIONS("sessions", "会话", Icons.Outlined.DeviceHub),
    LOGS("logs", "日志", Icons.Outlined.Visibility),
}

@Composable
fun KsuserAuthApp() {
    val context = LocalContext.current
    val container = remember(context) { (context.applicationContext as KsuserAuthApplication).appContainer }
    val viewModel: AppViewModel = viewModel(factory = AppViewModelFactory(container))
    val state by viewModel.uiState.collectAsState()
    val snackbarHostState = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()

    LaunchedEffect(state.message, state.error) {
        state.message?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTransientMessages()
        }
        state.error?.let {
            snackbarHostState.showSnackbar(it)
            viewModel.clearTransientMessages()
        }
    }

    Surface(modifier = Modifier.fillMaxSize()) {
        when {
            state.isBootstrapping -> LoadingScreen()
            state.pendingMfa != null || !state.isAuthenticated -> AuthFlowScreen(
                state = state,
                container = container,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
            )

            else -> MainShell(
                container = container,
                state = state,
                viewModel = viewModel,
                snackbarHostState = snackbarHostState,
                onMessage = { message -> scope.launch { snackbarHostState.showSnackbar(message) } },
            )
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFFF6F2E8), Color(0xFFE6F0F8)),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(16.dp))
            Text("正在初始化认证环境", style = MaterialTheme.typography.titleMedium)
        }
    }
}

@Composable
private fun AuthFlowScreen(
    state: AppUiState,
    container: AppContainer,
    viewModel: AppViewModel,
    snackbarHostState: SnackbarHostState,
) {
    val context = LocalContext.current
    val activity = context as? Activity
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
                        listOf(Color(0xFFF7EFE4), Color(0xFFE7F1F7)),
                    ),
                )
                .padding(20.dp),
            verticalArrangement = Arrangement.Center,
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.94f)),
                shape = RoundedCornerShape(28.dp),
            ) {
                Column(modifier = Modifier.padding(24.dp)) {
                    Text("Ksuser Auth Android", style = MaterialTheme.typography.headlineMedium, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        if (state.pendingMfa != null) "第一因子已通过，请完成多因素认证"
                        else "支持密码、邮箱验证码与原生 Passkey 登录，接口前缀来自 .env 配置",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    Spacer(modifier = Modifier.height(20.dp))

                    if (state.pendingMfa != null) {
                        val challenge = state.pendingMfa!!
                        Text(
                            "可用 MFA 方式: ${challenge.methods.joinToString()}",
                            style = MaterialTheme.typography.bodyMedium,
                            fontWeight = FontWeight.Medium,
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            challenge.methods.forEach { method ->
                                FilterChip(
                                    selected = if (method == "passkey") !useRecoveryCode else !useRecoveryCode,
                                    onClick = { useRecoveryCode = false },
                                    label = { Text(method) },
                                )
                            }
                        }
                        Spacer(modifier = Modifier.height(12.dp))
                        if (challenge.methods.contains("passkey")) {
                            Button(
                                onClick = {
                                    if (activity == null) {
                                        Toast.makeText(context, "当前上下文不支持 Passkey", Toast.LENGTH_SHORT).show()
                                    } else {
                                        viewModel.verifyPasskeyMfa(activity)
                                    }
                                },
                                enabled = !state.isBusy,
                                modifier = Modifier.fillMaxWidth(),
                            ) {
                                Text("使用 Passkey 完成 MFA")
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                        }
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
                        Spacer(modifier = Modifier.height(12.dp))
                        OutlinedTextField(
                            value = mfaCode,
                            onValueChange = { mfaCode = it },
                            modifier = Modifier.fillMaxWidth(),
                            label = { Text(if (useRecoveryCode) "恢复码" else "6 位验证码") },
                        )
                        Spacer(modifier = Modifier.height(12.dp))
                        Button(
                            onClick = {
                                if (useRecoveryCode) {
                                    viewModel.verifyTotpMfa(recoveryCode = mfaCode.trim().uppercase())
                                } else {
                                    viewModel.verifyTotpMfa(code = mfaCode.trim())
                                }
                            },
                            enabled = !state.isBusy && mfaCode.isNotBlank(),
                            modifier = Modifier.fillMaxWidth(),
                        ) {
                            Text(if (state.isBusy) "验证中..." else "完成 MFA")
                        }
                    } else {
                        TabRow(selectedTabIndex = authTab) {
                            Tab(selected = authTab == 0, onClick = { authTab = 0 }, text = { Text("登录") })
                            Tab(selected = authTab == 1, onClick = { authTab = 1 }, text = { Text("注册") })
                        }
                        Spacer(modifier = Modifier.height(16.dp))

                        if (authTab == 0) {
                            TabRow(selectedTabIndex = loginMethod) {
                                Tab(selected = loginMethod == 0, onClick = { loginMethod = 0 }, text = { Text("密码") })
                                Tab(selected = loginMethod == 1, onClick = { loginMethod = 1 }, text = { Text("验证码") })
                                Tab(selected = loginMethod == 2, onClick = { loginMethod = 2 }, text = { Text("Passkey") })
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            when (loginMethod) {
                                0 -> {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("邮箱") },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    OutlinedTextField(
                                        value = password,
                                        onValueChange = { password = it },
                                        label = { Text("密码") },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.passwordLogin(email.trim(), password) },
                                        enabled = !state.isBusy && email.isNotBlank() && password.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (state.isBusy) "登录中..." else "密码登录")
                                    }
                                }

                                1 -> {
                                    OutlinedTextField(
                                        value = email,
                                        onValueChange = { email = it },
                                        label = { Text("邮箱") },
                                        modifier = Modifier.fillMaxWidth(),
                                    )
                                    Spacer(modifier = Modifier.height(8.dp))
                                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                        OutlinedTextField(
                                            value = code,
                                            onValueChange = { code = it },
                                            label = { Text("验证码") },
                                            modifier = Modifier.weight(1f),
                                        )
                                        OutlinedButton(
                                            onClick = { viewModel.sendLoginCode(email.trim()) },
                                            enabled = !state.isBusy && email.isNotBlank(),
                                        ) {
                                            Text("发送")
                                        }
                                    }
                                    Spacer(modifier = Modifier.height(12.dp))
                                    Button(
                                        onClick = { viewModel.loginWithCode(email.trim(), code.trim()) },
                                        enabled = !state.isBusy && email.isNotBlank() && code.isNotBlank(),
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (state.isBusy) "登录中..." else "验证码登录")
                                    }
                                }

                                else -> {
                                    PasskeyInfoBlock()
                                    Spacer(modifier = Modifier.height(12.dp))
                                    val passkeyAvailability = remember(container) { container.passkeyManager.availability() }
                                    val passkeyAvailabilityMessage = remember(container) { container.passkeyManager.availabilityMessage() }
                                    if (passkeyAvailability != PasskeyAvailability.Available) {
                                        Text(
                                            passkeyAvailabilityMessage,
                                            color = MaterialTheme.colorScheme.error,
                                            style = MaterialTheme.typography.bodySmall,
                                        )
                                        Spacer(modifier = Modifier.height(8.dp))
                                    }
                                    Button(
                                        onClick = {
                                            if (activity == null) {
                                                Toast.makeText(context, "当前上下文不支持 Passkey", Toast.LENGTH_SHORT).show()
                                            } else {
                                                viewModel.loginWithPasskey(activity)
                                            }
                                        },
                                        enabled = !state.isBusy && passkeyAvailability == PasskeyAvailability.Available,
                                        modifier = Modifier.fillMaxWidth(),
                                    ) {
                                        Text(if (state.isBusy) "认证中..." else "使用 Passkey 登录")
                                    }
                                }
                            }
                        } else {
                            val requirement = state.passwordRequirement
                            OutlinedTextField(
                                value = username,
                                onValueChange = { username = it },
                                label = { Text("用户名") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = email,
                                onValueChange = { email = it },
                                label = { Text("邮箱") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = registerPassword,
                                onValueChange = { registerPassword = it },
                                label = { Text("密码") },
                                modifier = Modifier.fillMaxWidth(),
                            )
                            requirement?.let {
                                Spacer(modifier = Modifier.height(4.dp))
                                Text(
                                    "密码要求: ${it.minLength}-${it.maxLength} 位，数字=${it.requireDigits}",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedTextField(
                                    value = registerCode,
                                    onValueChange = { registerCode = it },
                                    label = { Text("邮箱验证码") },
                                    modifier = Modifier.weight(1f),
                                )
                                OutlinedButton(
                                    onClick = { viewModel.sendRegisterCode(email.trim()) },
                                    enabled = !state.isBusy && email.isNotBlank(),
                                ) {
                                    Text("发送")
                                }
                            }
                            Spacer(modifier = Modifier.height(12.dp))
                            Button(
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
                            ) {
                                Text(if (state.isBusy) "注册中..." else "创建账号")
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun PasskeyInfoBlock() {
    val environment = EnvironmentProvider.current
    Card(
        colors = CardDefaults.cardColors(containerColor = Color(0xFFF1F7FE)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("原生 Passkey", fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                "API 地址来自 .env，但 Passkey 的 RP 域由 ${environment.passkeyRpId} 决定。",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(modifier = Modifier.height(6.dp))
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
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun MainShell(
    container: AppContainer,
    state: AppUiState,
    viewModel: AppViewModel,
    snackbarHostState: SnackbarHostState,
    onMessage: (String) -> Unit,
) {
    val navController = rememberNavController()
    val currentBackStack by navController.currentBackStackEntryAsState()
    val currentDestination = currentBackStack?.destination
    val destinations = MainDestination.entries

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        currentDestination?.route?.let { route ->
                            destinations.firstOrNull { it.route == route }?.label ?: "Ksuser"
                        } ?: "Ksuser",
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(containerColor = Color(0xFFEEE5D2)),
                actions = {
                    IconButton(onClick = { viewModel.refreshCurrentUser() }) {
                        Icon(Icons.Outlined.Refresh, contentDescription = "刷新用户信息")
                    }
                    IconButton(onClick = { viewModel.logout() }) {
                        Icon(Icons.AutoMirrored.Outlined.Logout, contentDescription = "退出登录")
                    }
                },
            )
        },
        bottomBar = {
            NavigationBar {
                destinations.forEach { destination ->
                    val selected = currentDestination?.hierarchy?.any { it.route == destination.route } == true
                    NavigationBarItem(
                        selected = selected,
                        onClick = {
                            navController.navigate(destination.route) {
                                popUpTo(navController.graph.findStartDestination().id) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        },
                        icon = { Icon(destination.icon, contentDescription = destination.label) },
                        label = { Text(destination.label) },
                    )
                }
            }
        },
    ) { innerPadding ->
        NavHost(
            navController = navController,
            startDestination = MainDestination.HOME.route,
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            composable(MainDestination.HOME.route) {
                HomeScreen(
                    user = state.currentUser,
                    onRefresh = { viewModel.refreshCurrentUser() },
                )
            }
            composable(MainDestination.PROFILE.route) {
                ProfileScreen(
                    container = container,
                    currentUser = state.currentUser,
                    onProfileUpdated = {
                        viewModel.refreshCurrentUser()
                        onMessage("资料已更新")
                    },
                    onMessage = onMessage,
                )
            }
            composable(MainDestination.SECURITY.route) {
                SecurityScreen(
                    container = container,
                    user = state.currentUser,
                    onUserRefresh = { viewModel.refreshCurrentUser() },
                    onLogoutAll = { viewModel.logoutAll() },
                    onMessage = onMessage,
                )
            }
            composable(MainDestination.SESSIONS.route) {
                SessionsScreen(
                    container = container,
                    onLogoutAll = { viewModel.logoutAll() },
                    onMessage = onMessage,
                )
            }
            composable(MainDestination.LOGS.route) {
                LogsScreen(container = container)
            }
        }
    }
}

@Composable
private fun HomeScreen(
    user: UserProfile?,
    onRefresh: () -> Unit,
) {
    val env = EnvironmentProvider.current
    val context = LocalContext.current
    val appIdentity = remember(context) { AppIdentityProvider.current(context) }
    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(Brush.verticalGradient(listOf(Color(0xFFFBF6EE), Color(0xFFF4FAFD))))
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
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
        }
        item {
            OverviewCard(
                title = "安全偏好",
                subtitle = "来自当前账号设置",
                body = buildString {
                    appendLine("MFA: ${user?.settings?.mfaEnabled}")
                    appendLine("首选 MFA: ${user?.settings?.preferredMfaMethod ?: "totp"}")
                    appendLine("首选敏感验证: ${user?.settings?.preferredSensitiveMethod ?: "password"}")
                },
            )
        }
        item {
            OverviewCard(
                title = "Passkey 联调提示",
                subtitle = "API 前缀与 RP 域分离",
                body = "如果把 API 指向 localhost，但服务端 WebAuthn 仍返回 localhost/非生产 RP，Android 原生 Passkey 可能无法按生产方式直接工作。",
            )
        }
    }
}

@Composable
private fun OverviewCard(
    title: String,
    subtitle: String,
    body: String,
    actionLabel: String? = null,
    onAction: (() -> Unit)? = null,
) {
    Card(
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White.copy(alpha = 0.96f)),
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Text(title, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.Bold)
            Spacer(modifier = Modifier.height(4.dp))
            Text(subtitle, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Spacer(modifier = Modifier.height(12.dp))
            Text(body, style = MaterialTheme.typography.bodyMedium)
            if (actionLabel != null && onAction != null) {
                Spacer(modifier = Modifier.height(12.dp))
                Button(onClick = onAction) {
                    Text(actionLabel)
                }
            }
        }
    }
}

@Composable
private fun ProfileScreen(
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        currentUser?.avatarUrl?.takeIf { it.isNotBlank() }?.let { avatarUrl ->
            AsyncImage(
                model = avatarUrl,
                contentDescription = "头像",
                modifier = Modifier
                    .size(88.dp)
                    .clip(RoundedCornerShape(24.dp)),
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
            scope.launch {
                runProfileUpdate(container, "username", username, onProfileUpdated, onMessage)
            }
        }
        ProfileFieldCard("真实姓名", realName) { value ->
            realName = value
            scope.launch {
                runProfileUpdate(container, "realName", realName, onProfileUpdated, onMessage)
            }
        }
        ProfileFieldCard("地区", region) { value ->
            region = value
            scope.launch {
                runProfileUpdate(container, "region", region, onProfileUpdated, onMessage)
            }
        }
        ProfileFieldCard("个人简介", bio, singleLine = false) { value ->
            bio = value
            scope.launch {
                runProfileUpdate(container, "bio", bio, onProfileUpdated, onMessage)
            }
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
    onSave: (String) -> Unit,
) {
    var draft by rememberSaveable(value) { mutableStateOf(value) }
    Card(shape = RoundedCornerShape(20.dp)) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(title, fontWeight = FontWeight.SemiBold)
            Spacer(modifier = Modifier.height(8.dp))
            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier.fillMaxWidth(),
                singleLine = singleLine,
            )
            Spacer(modifier = Modifier.height(8.dp))
            Button(onClick = { onSave(draft) }) {
                Text("保存$title")
            }
        }
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun SecurityScreen(
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
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
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
            onAction = {
                scope.launch { reloadAll() }
            },
        )

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("安全偏好", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                SettingSwitchRow("启用 MFA", settings?.mfaEnabled == true) { checked ->
                    scope.launch {
                        runCatching { container.securityRepository.updateBooleanSetting("mfaEnabled", checked) }
                            .onSuccess {
                                settings = settings?.copy(mfaEnabled = it.mfaEnabled, preferredMfaMethod = it.preferredMfaMethod, preferredSensitiveMethod = it.preferredSensitiveMethod)
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
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("敏感操作验证", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text(
                    if (sensitiveStatus?.verified == true) {
                        "已验证，剩余 ${sensitiveStatus?.remainingSeconds ?: 0} 秒"
                    } else {
                        "未验证，涉及添加 Passkey、修改邮箱/密码、删除账户时需要先验证"
                    },
                )
                val methods = sensitiveStatus?.methods.orEmpty()
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    methods.forEach { method ->
                        AssistChip(onClick = {}, label = { Text(method) })
                    }
                }
                Button(onClick = { showSensitiveDialog = true }, enabled = !busy) {
                    Text(if (sensitiveStatus?.verified == true) "重新验证" else "开始验证")
                }
            }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("TOTP", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("已启用: ${totpStatus?.enabled == true}，剩余恢复码: ${totpStatus?.recoveryCodesCount ?: 0}")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = {
                        scope.launch {
                            runCatching { container.securityRepository.getTotpRegistrationOptions() }
                                .onSuccess { pendingTotpSetup = it }
                                .onFailure { onMessage(it.message ?: "生成 TOTP 选项失败") }
                        }
                    }) { Text("生成 TOTP 注册选项") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val disabled = runCatching { container.securityRepository.disableTotp() }
                                .onFailure { onMessage(it.message ?: "禁用失败") }
                                .isSuccess
                            if (disabled) {
                                reloadAll()
                                onMessage("TOTP 已禁用")
                            }
                        }
                    }) { Text("禁用") }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = {
                        scope.launch {
                            recoveryCodes = runCatching { container.securityRepository.getRecoveryCodes() }
                                .getOrElse {
                                    onMessage(it.message ?: "获取恢复码失败")
                                    emptyList()
                                }
                        }
                    }) { Text("查看恢复码") }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val newCodes = runCatching { container.securityRepository.regenerateRecoveryCodes() }
                                .onFailure { onMessage(it.message ?: "重生恢复码失败") }
                                .getOrNull()
                            if (newCodes != null) {
                                recoveryCodes = newCodes
                                reloadAll()
                                onMessage("恢复码已重新生成")
                            }
                        }
                    }) { Text("重生恢复码") }
                }
                if (recoveryCodes.isNotEmpty()) {
                    Text(recoveryCodes.joinToString("  "), style = MaterialTheme.typography.bodySmall)
                }
            }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("Passkey", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
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
                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        colors = CardDefaults.cardColors(containerColor = Color(0xFFF8F4EC)),
                    ) {
                        Column(modifier = Modifier.padding(14.dp)) {
                            Text(passkey.name, fontWeight = FontWeight.SemiBold)
                            Text(
                                "创建于 ${passkey.createdAt}\n最后使用 ${passkey.lastUsedAt ?: "暂无"}\ntransports=${passkey.transports}",
                                style = MaterialTheme.typography.bodySmall,
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                                OutlinedButton(onClick = {
                                    renameTarget = passkey
                                    renameDraft = passkey.name
                                }) { Text("重命名") }
                                OutlinedButton(onClick = {
                                    scope.launch {
                                        val deleted = runCatching { container.securityRepository.deletePasskey(passkey.id) }
                                            .onFailure { onMessage(it.message ?: "删除失败") }
                                            .isSuccess
                                        if (deleted) {
                                            reloadAll()
                                            onMessage("Passkey 已删除")
                                        }
                                    }
                                }) { Text("删除") }
                            }
                        }
                    }
                }
            }
        }

        Card(shape = RoundedCornerShape(20.dp)) {
            Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("敏感操作", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.Bold)
                Text("修改邮箱、修改密码、删除账号要求先完成上方敏感验证")
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(onClick = { showChangeEmailDialog = true }, enabled = sensitiveStatus?.verified == true) {
                        Text("修改邮箱")
                    }
                    Button(onClick = { showChangePasswordDialog = true }, enabled = sensitiveStatus?.verified == true) {
                        Text("修改密码")
                    }
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedButton(onClick = { showDeleteDialog = true }, enabled = sensitiveStatus?.verified == true) {
                        Text("删除账号")
                    }
                    OutlinedButton(onClick = onLogoutAll) {
                        Text("退出全部设备")
                    }
                }
            }
        }
    }

    if (pendingTotpSetup != null) {
        AlertDialog(
            onDismissRequest = { pendingTotpSetup = null },
            title = { Text("TOTP 注册") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("secret: ${pendingTotpSetup?.secret}")
                    pendingTotpSetup?.qrCodeUrl?.let { qrCode ->
                        AsyncImage(model = qrCode, contentDescription = "TOTP QR", modifier = Modifier.size(180.dp))
                    }
                    Text("恢复码: ${pendingTotpSetup?.recoveryCodes?.joinToString(" ")}")
                    OutlinedTextField(
                        value = totpCode,
                        onValueChange = { totpCode = it },
                        label = { Text("输入 6 位验证码") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    val setup = pendingTotpSetup ?: return@TextButton
                    scope.launch {
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
                    }
                }) { Text("确认启用") }
            },
            dismissButton = {
                TextButton(onClick = { pendingTotpSetup = null }) { Text("取消") }
            },
        )
    }

    if (showAddPasskeyDialog) {
        AlertDialog(
            onDismissRequest = { showAddPasskeyDialog = false },
            title = { Text("新增 Passkey") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Passkey 需要 Android 原生凭据提供器与匹配的 RP 域配置。")
                    OutlinedTextField(
                        value = newPasskeyName,
                        onValueChange = { newPasskeyName = it },
                        label = { Text("Passkey 名称") },
                    )
                }
            },
            confirmButton = {
                TextButton(onClick = {
                    if (activity == null) {
                        onMessage("当前上下文不支持 Passkey")
                        return@TextButton
                    }
                    scope.launch {
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
                    }
                }) { Text("创建") }
            },
            dismissButton = { TextButton(onClick = { showAddPasskeyDialog = false }) { Text("取消") } },
        )
    }

    renameTarget?.let { passkey ->
        AlertDialog(
            onDismissRequest = { renameTarget = null },
            title = { Text("重命名 Passkey") },
            text = {
                OutlinedTextField(
                    value = renameDraft,
                    onValueChange = { renameDraft = it },
                    label = { Text("新名称") },
                )
            },
            confirmButton = {
                TextButton(onClick = {
                    scope.launch {
                        val renamed = runCatching {
                            container.securityRepository.renamePasskey(passkey.id, renameDraft.trim())
                        }.onFailure { onMessage(it.message ?: "重命名失败") }
                            .isSuccess
                        if (renamed) {
                            renameTarget = null
                            reloadAll()
                            onMessage("Passkey 已重命名")
                        }
                    }
                }) { Text("保存") }
            },
            dismissButton = {
                TextButton(onClick = { renameTarget = null }) { Text("取消") }
            },
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

@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun PreferenceChips(
    title: String,
    options: List<String>,
    selected: String,
    onSelected: (String) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, fontWeight = FontWeight.SemiBold)
        androidx.compose.foundation.layout.FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
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
    val scope = rememberCoroutineScope()
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
            Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                Text("可用方式: ${status?.methods?.joinToString().orEmpty()}")
                PreferenceChips(
                    title = "验证方式",
                    options = status?.methods.orEmpty(),
                    selected = selectedMethod,
                    onSelected = { selectedMethod = it },
                )
                when (selectedMethod) {
                    "password" -> {
                        OutlinedTextField(
                            value = password,
                            onValueChange = { password = it },
                            label = { Text("当前密码") },
                        )
                    }

                    "email-code" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                            OutlinedTextField(
                                value = code,
                                onValueChange = { code = it },
                                label = { Text("邮箱验证码") },
                                modifier = Modifier.weight(1f),
                            )
                            OutlinedButton(onClick = {
                                scope.launch {
                                    runCatching { container.securityRepository.sendSensitiveCode() }
                                        .onSuccess { onMessage("验证码已发送") }
                                        .onFailure { onMessage(it.message ?: "发送失败") }
                                }
                            }) {
                                Text("发送")
                            }
                        }
                    }

                    "totp" -> {
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            FilterChip(selected = !recoveryMode, onClick = { recoveryMode = false }, label = { Text("TOTP") })
                            FilterChip(selected = recoveryMode, onClick = { recoveryMode = true }, label = { Text("恢复码") })
                        }
                        OutlinedTextField(
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
            TextButton(onClick = {
                scope.launch {
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
                }
            }) { Text("验证") }
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
    val scope = rememberCoroutineScope()
    var email by rememberSaveable { mutableStateOf("") }
    var code by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改邮箱") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                OutlinedTextField(value = email, onValueChange = { email = it }, label = { Text("新邮箱") })
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
                    OutlinedTextField(
                        value = code,
                        onValueChange = { code = it },
                        label = { Text("验证码") },
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedButton(onClick = {
                        scope.launch {
                            runCatching { container.securityRepository.sendChangeEmailCode(email.trim()) }
                                .onSuccess { onMessage("邮箱验证码已发送") }
                                .onFailure { onMessage(it.message ?: "发送失败") }
                        }
                    }) {
                        Text("发送")
                    }
                }
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    runCatching { container.securityRepository.changeEmail(email.trim(), code.trim()) }
                        .onSuccess { onSuccess() }
                        .onFailure { onMessage(it.message ?: "修改邮箱失败") }
                }
            }) { Text("提交") }
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
    val scope = rememberCoroutineScope()
    var password by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("修改密码") },
        text = {
            OutlinedTextField(
                value = password,
                onValueChange = { password = it },
                label = { Text("新密码") },
            )
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    runCatching { container.securityRepository.changePassword(password) }
                        .onSuccess { onSuccess() }
                        .onFailure { onMessage(it.message ?: "修改密码失败") }
                }
            }) { Text("提交") }
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
    val scope = rememberCoroutineScope()
    var confirmText by rememberSaveable { mutableStateOf("") }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("删除账号") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Text("输入 DELETE 以确认删除账号。")
                OutlinedTextField(value = confirmText, onValueChange = { confirmText = it }, label = { Text("确认文本") })
            }
        },
        confirmButton = {
            TextButton(onClick = {
                scope.launch {
                    runCatching { container.securityRepository.deleteAccount(confirmText) }
                        .onSuccess { onDeleted() }
                        .onFailure { onMessage(it.message ?: "删除账号失败") }
                }
            }) { Text("删除") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun SessionsScreen(
    container: AppContainer,
    onLogoutAll: () -> Unit,
    onMessage: (String) -> Unit,
) {
    val scope = rememberCoroutineScope()
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
        modifier = Modifier.fillMaxSize().padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        item {
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Button(onClick = { scope.launch { reload() } }) { Text("刷新") }
                OutlinedButton(onClick = onLogoutAll) { Text("退出全部设备") }
            }
        }
        if (loading) {
            item { CircularProgressIndicator() }
        }
        items(sessions) { session ->
            Card(shape = RoundedCornerShape(20.dp)) {
                Column(modifier = Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(
                        "${session.browser ?: "未知浏览器"} / ${session.deviceType ?: "未知设备"}",
                        fontWeight = FontWeight.SemiBold,
                    )
                    Text(
                        "IP: ${session.ipAddress} ${session.ipLocation.orEmpty()}\n创建: ${session.createdAt}\n最近活跃: ${session.lastSeenAt}",
                        style = MaterialTheme.typography.bodySmall,
                    )
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        if (session.current) {
                            Badge { Text("当前") }
                        }
                        if (session.online) {
                            Badge { Text("在线") }
                        }
                    }
                    OutlinedButton(onClick = {
                        scope.launch {
                            val revoked = runCatching { container.sessionsRepository.revokeSession(session.id) }
                                .onFailure { onMessage(it.message ?: "撤销失败") }
                                .isSuccess
                            if (revoked) {
                                reload()
                                onMessage("会话已撤销")
                            }
                        }
                    }) {
                        Text("撤销此会话")
                    }
                }
            }
        }
    }
}

@Composable
private fun LogsScreen(
    container: AppContainer,
) {
    val scope = rememberCoroutineScope()
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

    Column(modifier = Modifier.fillMaxSize().padding(16.dp)) {
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedTextField(
                value = operationType,
                onValueChange = { operationType = it },
                label = { Text("操作类型") },
                modifier = Modifier.weight(1f),
            )
            OutlinedTextField(
                value = result,
                onValueChange = { result = it },
                label = { Text("结果") },
                modifier = Modifier.weight(1f),
            )
            Button(onClick = { scope.launch { reload() } }) { Text("查询") }
        }
        Spacer(modifier = Modifier.height(12.dp))
        if (busy) {
            CircularProgressIndicator()
        }
        LazyColumn(verticalArrangement = Arrangement.spacedBy(12.dp)) {
            items(logs) { log ->
                Card(shape = RoundedCornerShape(18.dp)) {
                    Column(modifier = Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(4.dp)) {
                        Text(
                            "${log.operationType} / ${log.result}",
                            fontWeight = FontWeight.SemiBold,
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
        }
        Spacer(modifier = Modifier.height(8.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalAlignment = Alignment.CenterVertically) {
            OutlinedButton(onClick = { if (page > 1) page-- }, enabled = page > 1) { Text("上一页") }
            Text("第 $page / ${if (totalPages == 0) 1 else totalPages} 页")
            OutlinedButton(onClick = { if (page < totalPages) page++ }, enabled = page < totalPages) { Text("下一页") }
        }
    }
}
