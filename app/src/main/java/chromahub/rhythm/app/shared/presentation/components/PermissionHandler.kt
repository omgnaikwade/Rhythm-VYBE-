/*
 * SPDX-FileCopyrightText: 2024-2026 Anjishnu Nandi <https://github.com/cromaguy>
 * SPDX-FileCopyrightText: 2026 VYBE Contributors
 * SPDX-License-Identifier: GPL-3.0-or-later
 */

package chromahub.rhythm.app.shared.presentation.components

import android.util.Log
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import chromahub.rhythm.app.features.local.presentation.viewmodel.MusicViewModel
import chromahub.rhythm.app.features.streaming.presentation.viewmodel.StreamingMusicViewModel
import chromahub.rhythm.app.shared.data.model.AppSettings
import chromahub.rhythm.app.shared.presentation.viewmodel.AppUpdaterViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.ThemeViewModel
import chromahub.rhythm.app.shared.presentation.viewmodel.rememberAppUpdaterViewModel

/**
 * VYBE startup gate.
 *
 * Unlike upstream Rhythm, VYBE is an API-driven music streaming application with no local
 * device storage scanning dependency. It does not prompt for READ_MEDIA_AUDIO /
 * READ_EXTERNAL_STORAGE or run Rhythm's legacy local-player onboarding wizard — it connects
 * the playback service and launches directly into the main VYBE experience.
 */
@Composable
fun PermissionHandler(
    onPermissionsGranted: @Composable () -> Unit,
    themeViewModel: ThemeViewModel,
    appSettings: AppSettings,
    isLoading: Boolean,
    isInitializingApp: Boolean,
    onSetIsLoading: (Boolean) -> Unit,
    onSetIsInitializingApp: (Boolean) -> Unit,
    musicViewModel: MusicViewModel = viewModel(),
    updaterViewModel: AppUpdaterViewModel = rememberAppUpdaterViewModel(),
    streamingViewModel: StreamingMusicViewModel = viewModel(),
    showMediaScanLoader: Boolean,
    onShowMediaScanLoaderChange: (Boolean) -> Unit
) {
    LaunchedEffect(Unit) {
        if (!appSettings.onboardingCompleted.value) {
            appSettings.setOnboardingCompleted(true)
        }
        if (!appSettings.initialMediaScanCompleted.value) {
            appSettings.setInitialMediaScanCompleted(true)
        }
        onShowMediaScanLoaderChange(false)
        onSetIsInitializingApp(true)
        try {
            musicViewModel.connectToMediaService()
        } catch (e: Exception) {
            Log.w("PermissionHandler", "Failed to connect to media service", e)
        } finally {
            onSetIsInitializingApp(false)
            onSetIsLoading(false)
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        onPermissionsGranted()
    }
}
