package com.hidra.launcher.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.hidra.launcher.data.AppInfo

private enum class Screen { Home, Hidden, Settings }

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LauncherRoot(viewModel: MainViewModel) {
    val state by viewModel.state.collectAsState()
    var screen by remember { mutableStateOf(Screen.Home) }
    var editing by remember { mutableStateOf<AppInfo?>(null) }

    // Hidden-vault unlock lives only for this composition (re-lock on leave).
    var vaultUnlocked by remember { mutableStateOf(false) }

    Scaffold(
        containerColor = MaterialTheme.colorScheme.background,
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = MaterialTheme.colorScheme.onBackground,
                ),
                title = {
                    Text(
                        when (screen) {
                            Screen.Home -> "Apps"
                            Screen.Hidden -> "Hidden"
                            Screen.Settings -> "Settings"
                        },
                    )
                },
                actions = {
                    if (screen == Screen.Home) {
                        IconButton(onClick = {
                            vaultUnlocked = false
                            screen = Screen.Hidden
                        }) {
                            Icon(Icons.Filled.VisibilityOff, contentDescription = "Hidden apps")
                        }
                        IconButton(onClick = { screen = Screen.Settings }) {
                            Icon(Icons.Filled.Settings, contentDescription = "Settings")
                        }
                    }
                },
            )
        },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            when (screen) {
                Screen.Home -> AppGrid(
                    apps = state.visibleApps,
                    emptyText = if (state.loading) "Loading apps…" else "No apps found.",
                    onClick = viewModel::launch,
                    onLongClick = { editing = it },
                )

                Screen.Hidden -> HiddenScreen(
                    viewModel = viewModel,
                    unlocked = vaultUnlocked,
                    onUnlocked = { vaultUnlocked = true },
                    hiddenApps = state.hiddenApps,
                    onClick = viewModel::launch,
                    onLongClick = { editing = it },
                    onBack = { screen = Screen.Home },
                )

                Screen.Settings -> SettingsScreen(
                    viewModel = viewModel,
                    state = state,
                    onBack = { screen = Screen.Home },
                )
            }
        }
    }

    editing?.let { app ->
        EditAppSheet(
            app = app,
            allApps = state.apps,
            onDismiss = { editing = null },
            viewModel = viewModel,
        )
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun AppGrid(
    apps: List<AppInfo>,
    emptyText: String,
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
) {
    if (apps.isEmpty()) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            Text(emptyText, color = MaterialTheme.colorScheme.onBackground)
        }
        return
    }
    LazyVerticalGrid(
        columns = GridCells.Adaptive(88.dp),
        contentPadding = androidx.compose.foundation.layout.PaddingValues(12.dp),
        verticalArrangement = Arrangement.spacedBy(8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.fillMaxSize(),
    ) {
        items(apps, key = { it.key }) { app ->
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier
                    .clip(RoundedCornerShape(16.dp))
                    .combinedClickable(
                        onClick = { onClick(app) },
                        onLongClick = { onLongClick(app) },
                    )
                    .padding(8.dp),
            ) {
                Image(
                    bitmap = app.displayIcon,
                    contentDescription = app.displayLabel,
                    modifier = Modifier
                        .size(56.dp)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color.White.copy(alpha = 0.04f)),
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    text = app.displayLabel,
                    color = MaterialTheme.colorScheme.onBackground,
                    fontSize = 12.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth(),
                )
            }
        }
    }
}
