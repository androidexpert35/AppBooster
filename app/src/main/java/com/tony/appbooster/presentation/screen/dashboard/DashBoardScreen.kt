package com.tony.appbooster.presentation.screen.dashboard

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.tony.appbooster.R
import com.tony.appbooster.presentation.permission.NotificationPermissionGate
import com.tony.appbooster.presentation.screen.common.basescreen.AppBaseScreen
import com.tony.appbooster.presentation.screen.common.basescreen.ErrorDialogConfig
import com.tony.appbooster.presentation.screen.dashboard.components.DashboardHeroCard
import com.tony.appbooster.presentation.screen.dashboard.components.OptimizationActivityFeed
import com.tony.appbooster.presentation.viewmodel.main.MainUiEffect
import com.tony.appbooster.presentation.viewmodel.main.MainUiEvent
import com.tony.appbooster.presentation.viewmodel.main.MainUiModel
import com.tony.appbooster.presentation.viewmodel.main.MainViewModel
import kotlinx.coroutines.flow.collectLatest

@Composable
fun DashboardScreen(viewModel: MainViewModel) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }

    NotificationPermissionGate { runWithNotificationPermission ->
        LaunchedEffect(viewModel) {
            viewModel.uiEffect.collectLatest { effect ->
                when (effect) {
                    is MainUiEffect.ShowSnackbar -> snackbarHostState.showSnackbar(effect.message)
                }
            }
        }

        AppBaseScreen(
            uiState = uiState,
            errorDialogConfig = ErrorDialogConfig(
                onCancel = { viewModel.showErrorPopup(false) }
            )
        ) { model ->
            DashboardContent(
                model = model,
                onStartOptimization = {
                    runWithNotificationPermission {
                        viewModel.onEvent(MainUiEvent.OnStartOptimizationClicked)
                    }
                },
                onForceOptimize = {
                    runWithNotificationPermission {
                        viewModel.onEvent(MainUiEvent.OnForceOptimizationClicked)
                    }
                },
                onStopOptimization = { viewModel.onEvent(MainUiEvent.OnStopOptimizationClicked) },
                onDismissResult = { viewModel.onEvent(MainUiEvent.OnDismissOptimizationResultClicked) },
                onAnalyze = { viewModel.onEvent(MainUiEvent.OnAnalyzeAppsClicked) },
                onStopAnalysis = { viewModel.onEvent(MainUiEvent.OnStopAnalysisClicked) },
                snackbarHostState = snackbarHostState
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DashboardContent(
    model: MainUiModel,
    onStartOptimization: () -> Unit,
    onForceOptimize: () -> Unit,
    onStopOptimization: () -> Unit,
    onDismissResult: () -> Unit,
    onAnalyze: () -> Unit,
    onStopAnalysis: () -> Unit,
    snackbarHostState: SnackbarHostState
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())


    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.dashboard_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                actions = {
                    // Intentionally empty: connection chip removed (Shizuku health is validated on-demand).
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        snackbarHost = { SnackbarHost(hostState = snackbarHostState) },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            DashboardHeroCard(
                model = model,
                onStartOptimization = onStartOptimization,
                onForceOptimize = onForceOptimize,
                onStopOptimization = onStopOptimization,
                onDismissResult = onDismissResult,
                onAnalyze = onAnalyze,
                onStopAnalysis = onStopAnalysis
            )

            OptimizationActivityFeed(
                entries = model.logEntries,
                isExpanded = true,
                modifier = Modifier
                    .weight(1f)
                    .padding(bottom = 16.dp)
            )
        }
    }
}
