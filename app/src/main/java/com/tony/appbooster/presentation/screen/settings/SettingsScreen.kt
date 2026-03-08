package com.tony.appbooster.presentation.screen.settings

import android.content.res.Configuration
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.ui.Modifier
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.tony.appbooster.R
import com.tony.appbooster.domain.model.settings.AppOptimizationType
import com.tony.appbooster.domain.model.shizuku.ShizukuState
import com.tony.appbooster.presentation.screen.common.basescreen.AppBaseScreen
import com.tony.appbooster.presentation.screen.settings.components.AboutCard
import com.tony.appbooster.presentation.screen.settings.components.OptimizationTypeSelector
import com.tony.appbooster.presentation.screen.settings.components.SettingsSection
import com.tony.appbooster.presentation.screen.settings.components.ShizukuStatusCard
import com.tony.appbooster.presentation.viewmodel.base.UIState
import com.tony.appbooster.presentation.viewmodel.base.UIStatus
import com.tony.appbooster.presentation.viewmodel.settings.SettingsUiState
import com.tony.appbooster.presentation.viewmodel.settings.SettingsViewModel

/**
 * Entry point composable for the Settings screen. It wires the Hilt-provided
 * [SettingsViewModel] into the base screen wrapper and delegates concrete
 * rendering to the internal content composable.
 *
 * @param viewModel The ViewModel exposing settings state and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    viewModel: SettingsViewModel = hiltViewModel()
) {
    val uiState = viewModel.uiState.collectAsState()

    AppBaseScreen(
        uiState = uiState.value
    ) { data ->
        SettingsScreenContent(
            data = data,
            onOptimizationTypeClick = { newType ->
                viewModel.onOptimizationTypeSelected(newType)
            }
        )
    }
}

/**
 * Renders the visual structure of the Settings screen using the provided
 * [SettingsUiState], allowing the user to inspect and change optimization
 * mode as well as view the current Shizuku status and app information.
 *
 * @param data The current UI state snapshot for the Settings screen.
 * @param onOptimizationTypeClick Callback invoked when user requests a new optimization type.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreenContent(
    data: SettingsUiState,
    onOptimizationTypeClick: (AppOptimizationType) -> Unit
) {
    val scrollBehavior = TopAppBarDefaults.enterAlwaysScrollBehavior(rememberTopAppBarState())

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = {
                    Text(
                        text = stringResource(R.string.settings_top_bar_title),
                        style = MaterialTheme.typography.headlineLarge.copy(
                            fontWeight = FontWeight.Bold
                        )
                    )
                },
                scrollBehavior = scrollBehavior,
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                    scrolledContainerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        containerColor = MaterialTheme.colorScheme.surface
    ) { padding ->
        Column(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Optimization Mode Section
            SettingsSection(
                title = stringResource(R.string.settings_section_optimization_title),
                subtitle = stringResource(R.string.settings_section_optimization_subtitle)
            ) {
                OptimizationTypeSelector(
                    selectedType = data.appOptimizationType,
                    onTypeSelected = onOptimizationTypeClick
                )
            }

            // Shizuku Status Section
            SettingsSection(
                title = stringResource(R.string.settings_section_shizuku_title),
                subtitle = stringResource(R.string.settings_section_shizuku_subtitle)
            ) {
                ShizukuStatusCard(shizukuState = data.shizukuState)
            }

            // About Section
            SettingsSection(
                title = stringResource(R.string.settings_section_about_title),
                subtitle = stringResource(R.string.settings_section_about_subtitle)
            ) {
                AboutCard(
                    versionName = data.appVersionName,
                    versionChannel = data.appVersionChannel
                )
            }

            // Bottom spacing
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
}

/**
 * Preview of [SettingsScreenContent] in light mode for design validation.
 */
@Preview(showBackground = true, name = "Settings - Light")
@Composable
fun SettingsScreenContentLightPreview() {
    val uiState = SettingsUiState(
        appOptimizationType = AppOptimizationType.SPEED_PROFILE,
        appVersionName = "1.0.0",
        appVersionChannel = "Alpha",
        shizukuState = ShizukuState.Ready
    )
    val baseState = UIState(
        status = UIStatus.SUCCESS,
        data = uiState
    )
    AppBaseScreen(uiState = baseState) { data ->
        SettingsScreenContent(
            data = data,
            onOptimizationTypeClick = {}
        )
    }
}

/**
 * Preview of [SettingsScreenContent] in dark mode for design validation.
 */
@Preview(
    showBackground = true,
    name = "Settings - Dark",
    uiMode = Configuration.UI_MODE_NIGHT_YES
)
@Composable
fun SettingsScreenContentDarkPreview() {
    val uiState = SettingsUiState(
        appOptimizationType = AppOptimizationType.FULL_OPTIMIZATION,
        appVersionName = "1.0.0",
        appVersionChannel = "Beta",
        shizukuState = ShizukuState.NotRunning
    )
    val baseState = UIState(
        status = UIStatus.SUCCESS,
        data = uiState
    )
    AppBaseScreen(uiState = baseState) { data ->
        SettingsScreenContent(
            data = data,
            onOptimizationTypeClick = {}
        )
    }
}
