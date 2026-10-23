package com.hidra.launcher.ui

import android.content.Intent
import android.net.Uri
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.hidra.launcher.data.AppInfo
import com.hidra.launcher.data.Disguise
import com.hidra.launcher.shizuku.ShizukuController

@Composable
fun HiddenScreen(
    viewModel: MainViewModel,
    unlocked: Boolean,
    onUnlocked: () -> Unit,
    hiddenApps: List<AppInfo>,
    onClick: (AppInfo) -> Unit,
    onLongClick: (AppInfo) -> Unit,
    onBack: () -> Unit,
) {
    if (!unlocked) {
        PinGate(
            isCreate = !viewModel.hasPin(),
            onCreate = { viewModel.setPin(it); onUnlocked() },
            onCheck = { if (viewModel.checkPin(it)) { onUnlocked(); true } else false },
            onBack = onBack,
        )
        return
    }
    if (hiddenApps.isEmpty()) {
        Column(
            Modifier.fillMaxSize().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("No hidden apps.", color = MaterialTheme.colorScheme.onBackground)
            Spacer(Modifier.height(8.dp))
            Text(
                "Long-press any app on the home grid and turn on \"Hide app\".",
                style = MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))
            OutlinedButton(onClick = onBack) { Text("Back") }
        }
        return
    }
    AppGrid(
        apps = hiddenApps,
        emptyText = "",
        onClick = onClick,
        onLongClick = onLongClick,
    )
}

@Composable
private fun PinGate(
    isCreate: Boolean,
    onCreate: (String) -> Unit,
    onCheck: (String) -> Boolean,
    onBack: () -> Unit,
) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }

    Column(
        Modifier.fillMaxSize().padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            if (isCreate) "Create a PIN for hidden apps" else "Enter PIN",
            style = MaterialTheme.typography.titleMedium,
        )
        Spacer(Modifier.height(16.dp))
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
            label = { Text("PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        if (isCreate) {
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(
                value = confirm,
                onValueChange = { confirm = it.filter(Char::isDigit).take(8); error = null },
                label = { Text("Confirm PIN") },
                singleLine = true,
                visualTransformation = PasswordVisualTransformation(),
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
            )
        }
        error?.let {
            Spacer(Modifier.height(8.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(20.dp))
        Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            OutlinedButton(onClick = onBack) { Text("Back") }
            Button(
                onClick = {
                    if (pin.length < 4) { error = "Use at least 4 digits."; return@Button }
                    if (isCreate) {
                        if (pin != confirm) { error = "PINs don't match."; return@Button }
                        onCreate(pin)
                    } else if (!onCheck(pin)) {
                        error = "Wrong PIN."
                    }
                },
            ) { Text(if (isCreate) "Set PIN" else "Unlock") }
        }
    }
}

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    state: UiState,
    onBack: () -> Unit,
) {
    var changingPin by remember { mutableStateOf(false) }

    Column(
        Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        ShizukuSection()

        Spacer(Modifier.height(8.dp))
        Text("App identity", style = MaterialTheme.typography.titleMedium)
        Text(
            "Change how THIS launcher appears in the app drawer. The chosen icon " +
                "and name replace HidR-A's own. (Android may take a moment to refresh " +
                "the drawer.)",
            style = MaterialTheme.typography.bodySmall,
        )
        Disguise.entries.forEach { d ->
            Row(
                Modifier
                    .fillMaxWidth()
                    .selectable(
                        selected = state.disguise == d,
                        onClick = { viewModel.applyDisguise(d) },
                    )
                    .padding(vertical = 6.dp),
                verticalAlignment = Alignment.CenterVertically,
            ) {
                RadioButton(selected = state.disguise == d, onClick = { viewModel.applyDisguise(d) })
                Spacer(Modifier.height(0.dp))
                Text(
                    when (d) {
                        Disguise.Default -> "HidR-A (default)"
                        Disguise.Calculator -> "Calculator"
                        Disguise.Notes -> "Notes"
                    },
                )
            }
        }

        Spacer(Modifier.height(8.dp))
        Text("Hidden-apps PIN", style = MaterialTheme.typography.titleMedium)
        Button(onClick = { changingPin = true }) {
            Text(if (state.hasPin) "Change PIN" else "Set PIN")
        }

        Spacer(Modifier.height(8.dp))
        Card {
            Column(Modifier.padding(14.dp)) {
                Text("What this can and can't do", style = MaterialTheme.typography.titleSmall)
                Spacer(Modifier.height(6.dp))
                Text(
                    "Renaming or re-iconing another app only changes how it looks " +
                        "inside HidR-A; its real name and icon elsewhere are unchanged. " +
                        "With Shizuku ready, hiding disables the app so it vanishes from " +
                        "every launcher and app drawer. It stays installed, and Android's " +
                        "Settings > Apps can still list it as disabled. While a hidden " +
                        "app is open it is enabled; it is disabled again when you return " +
                        "to HidR-A, which also stops it running in the background.",
                    style = MaterialTheme.typography.bodySmall,
                )
            }
        }

        OutlinedButton(onClick = onBack) { Text("Back") }
    }

    if (changingPin) {
        // Reuse the create flow to (re)set the PIN from Settings.
        androidx.compose.material3.AlertDialog(
            onDismissRequest = { changingPin = false },
            confirmButton = {},
            title = { Text(if (state.hasPin) "Change PIN" else "Set PIN") },
            text = {
                PinSetInline(onDone = { viewModel.setPin(it); changingPin = false })
            },
            dismissButton = { TextButton(onClick = { changingPin = false }) { Text("Cancel") } },
        )
    }
}

@Composable
private fun ShizukuSection() {
    val status by ShizukuController.status.collectAsState()
    val context = LocalContext.current

    Text("Hide apps everywhere", style = MaterialTheme.typography.titleMedium)
    Text(
        "To remove an app from every launcher, HidR-A disables it through Shizuku, " +
            "a free helper app. One-time setup, no root needed. After a phone restart " +
            "you must start Shizuku again before you can open hidden apps.",
        style = MaterialTheme.typography.bodySmall,
    )
    Card {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            when (status) {
                ShizukuController.Status.NotInstalled -> {
                    Text("Step 1: install Shizuku.")
                    Button(onClick = {
                        context.startActivity(
                            Intent(Intent.ACTION_VIEW, Uri.parse("https://shizuku.rikka.app/download/"))
                                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK),
                        )
                    }) { Text("Get Shizuku") }
                }

                ShizukuController.Status.NotRunning -> {
                    Text(
                        "Step 2: open Shizuku and start it. On Android 11+ choose " +
                            "\"Start via Wireless debugging\" and follow its steps.",
                    )
                    Button(onClick = {
                        context.packageManager
                            .getLaunchIntentForPackage(ShizukuController.SHIZUKU_PACKAGE)
                            ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                            ?.let(context::startActivity)
                    }) { Text("Open Shizuku") }
                }

                ShizukuController.Status.NeedsPermission -> {
                    Text("Step 3: allow HidR-A to use Shizuku.")
                    Button(onClick = { ShizukuController.requestPermission() }) {
                        Text("Allow HidR-A")
                    }
                }

                ShizukuController.Status.Connecting -> Text("Connecting to Shizuku…")

                ShizukuController.Status.Ready ->
                    Text("Ready. Apps you hide are now disabled system-wide.")
            }
            OutlinedButton(onClick = { ShizukuController.refresh() }) { Text("Re-check") }
        }
    }
}

@Composable
private fun PinSetInline(onDone: (String) -> Unit) {
    var pin by remember { mutableStateOf("") }
    var confirm by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    Column {
        OutlinedTextField(
            value = pin,
            onValueChange = { pin = it.filter(Char::isDigit).take(8); error = null },
            label = { Text("New PIN") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        Spacer(Modifier.height(8.dp))
        OutlinedTextField(
            value = confirm,
            onValueChange = { confirm = it.filter(Char::isDigit).take(8); error = null },
            label = { Text("Confirm") },
            singleLine = true,
            visualTransformation = PasswordVisualTransformation(),
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.NumberPassword),
        )
        error?.let {
            Spacer(Modifier.height(6.dp))
            Text(it, color = MaterialTheme.colorScheme.error)
        }
        Spacer(Modifier.height(12.dp))
        Button(onClick = {
            when {
                pin.length < 4 -> error = "Use at least 4 digits."
                pin != confirm -> error = "PINs don't match."
                else -> onDone(pin)
            }
        }) { Text("Save PIN") }
    }
}
