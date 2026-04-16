package cn.ksuser.auth.android.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
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
    val pendingQrConfirmation = state.pendingQrConfirmation
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
            pendingQrConfirmation != null -> QrLoginConfirmScreen(
                pending = pendingQrConfirmation,
                currentUser = state.currentUser,
                isBusy = state.isBusy,
                onConfirm = { viewModel.confirmQrAction() },
                onCancel = { viewModel.cancelQrAction() },
            )
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
