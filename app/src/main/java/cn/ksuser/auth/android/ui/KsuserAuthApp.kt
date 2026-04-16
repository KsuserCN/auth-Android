package cn.ksuser.auth.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import cn.ksuser.auth.android.KsuserAuthApplication
import cn.ksuser.auth.android.ui.components.AppSpacing
import cn.ksuser.auth.android.ui.theme.GoldGradientBottomDark
import cn.ksuser.auth.android.ui.theme.GoldGradientBottomLight
import cn.ksuser.auth.android.ui.theme.GoldGradientTopDark
import cn.ksuser.auth.android.ui.theme.GoldGradientTopLight
import kotlinx.coroutines.launch

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

    if (!state.pendingTransferCode.isNullOrBlank()) {
        AlertDialog(
            onDismissRequest = { viewModel.cancelTransferLogin() },
            title = { Text("确认切换账号") },
            text = { Text("检测到跨端登录二维码。继续后将使用二维码对应账号登录当前手机端。") },
            confirmButton = {
                TextButton(onClick = { viewModel.confirmTransferLogin() }) {
                    Text("继续登录")
                }
            },
            dismissButton = {
                TextButton(onClick = { viewModel.cancelTransferLogin() }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(GoldGradientTopLight, GoldGradientBottomLight),
                ),
            ),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
            Spacer(modifier = Modifier.height(AppSpacing.S16))
            Text("正在初始化认证环境", style = MaterialTheme.typography.titleMedium)
        }
    }
}
