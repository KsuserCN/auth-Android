package cn.ksuser.auth.android.ui

import android.Manifest
import android.content.pm.PackageManager
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.ArrowBack
import androidx.compose.material.icons.automirrored.outlined.Logout
import androidx.compose.material.icons.outlined.QrCodeScanner
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.NavigationBarItemDefaults
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.navigation.NavDestination.Companion.hierarchy
import androidx.navigation.NavGraph.Companion.findStartDestination
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import cn.ksuser.auth.android.data.AppContainer

private const val PROFILE_EDIT_ROUTE = "profile/edit/{fieldKey}"
private const val ABOUT_ROUTE = "profile/about"
private fun profileEditRoute(fieldKey: String): String = "profile/edit/$fieldKey"

@OptIn(ExperimentalMaterial3Api::class)
@Composable
internal fun MainShell(
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
    val context = LocalContext.current
    var showQrScanner by rememberSaveable { mutableStateOf(false) }
    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission(),
    ) { granted ->
        if (granted) {
            showQrScanner = true
        } else {
            onMessage("相机权限被拒绝，无法扫码")
        }
    }

    Scaffold(
        contentWindowInsets = WindowInsets.safeDrawing,
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            val route = currentDestination?.route.orEmpty()
            val canNavigateBack = route.startsWith("profile/edit") || route == ABOUT_ROUTE
            TopAppBar(
                title = {
                    val title = when {
                        route.startsWith("profile/edit") -> "编辑资料"
                        route == ABOUT_ROUTE -> "关于"
                        else -> destinations.firstOrNull { it.route == route }?.label ?: "Ksuser"
                    }
                    Text(
                        title,
                        style = MaterialTheme.typography.titleLarge,
                    )
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    navigationIconContentColor = MaterialTheme.colorScheme.onSurface,
                    titleContentColor = MaterialTheme.colorScheme.onSurface,
                    actionIconContentColor = MaterialTheme.colorScheme.primary,
                ),
                navigationIcon = {
                    if (canNavigateBack) {
                        IconButton(onClick = { navController.popBackStack() }) {
                            Icon(Icons.AutoMirrored.Outlined.ArrowBack, contentDescription = "返回")
                        }
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            val granted = ContextCompat.checkSelfPermission(
                                context,
                                Manifest.permission.CAMERA,
                            ) == PackageManager.PERMISSION_GRANTED
                            if (granted) {
                                showQrScanner = true
                            } else {
                                cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
                            }
                        },
                    ) {
                        Icon(Icons.Outlined.QrCodeScanner, contentDescription = "扫码授权")
                    }
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
            NavigationBar(
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 2.dp,
            ) {
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
                        colors = NavigationBarItemDefaults.colors(
                            selectedIconColor = MaterialTheme.colorScheme.primary,
                            selectedTextColor = MaterialTheme.colorScheme.primary,
                            indicatorColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.68f),
                            unselectedIconColor = MaterialTheme.colorScheme.onSurfaceVariant,
                            unselectedTextColor = MaterialTheme.colorScheme.onSurfaceVariant,
                        ),
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
                    onOpenProfile = { navController.navigate(MainDestination.PROFILE.route) },
                    onOpenSecurity = { navController.navigate(MainDestination.SECURITY.route) },
                    onOpenSessions = { navController.navigate(MainDestination.SESSIONS.route) },
                )
            }
            composable(MainDestination.PROFILE.route) {
                ProfileScreen(
                    currentUser = state.currentUser,
                    onNavigateToEdit = { fieldKey -> navController.navigate(profileEditRoute(fieldKey)) },
                    onNavigateToAbout = { navController.navigate(ABOUT_ROUTE) },
                )
            }
            composable(
                route = PROFILE_EDIT_ROUTE,
                arguments = listOf(navArgument("fieldKey") { type = NavType.StringType }),
            ) { backStackEntry ->
                val fieldKey = backStackEntry.arguments?.getString("fieldKey").orEmpty()
                ProfileEditScreen(
                    container = container,
                    currentUser = state.currentUser,
                    fieldKey = fieldKey,
                    onBack = { navController.popBackStack() },
                    onProfileUpdated = { viewModel.refreshCurrentUser() },
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
            composable(ABOUT_ROUTE) {
                AboutScreen()
            }
        }
    }

    if (showQrScanner) {
        QrScannerDialog(
            onDismiss = { showQrScanner = false },
            onDetected = { rawContent ->
                showQrScanner = false
                viewModel.handleScannedQr(rawContent)
            },
            onMessage = onMessage,
        )
    }
}
