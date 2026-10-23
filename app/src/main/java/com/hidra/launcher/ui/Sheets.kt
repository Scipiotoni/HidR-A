package com.hidra.launcher.ui

import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.hidra.launcher.data.AppInfo
import com.hidra.launcher.data.Override

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditAppSheet(
    app: AppInfo,
    allApps: List<AppInfo>,
    onDismiss: () -> Unit,
    viewModel: MainViewModel,
) {
    var label by remember { mutableStateOf(app.override.customLabel ?: app.systemLabel) }
    var iconPkg by remember { mutableStateOf(app.override.customIconPackage) }
    var hidden by remember { mutableStateOf(app.override.hidden) }
    var pickingIcon by remember { mutableStateOf(false) }

    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.padding(horizontal = 20.dp).padding(bottom = 28.dp)) {
            Text("Customize", style = androidx.compose.material3.MaterialTheme.typography.titleLarge)
            Text(
                "Original: ${app.systemLabel}",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )
            Spacer(Modifier.height(16.dp))

            OutlinedTextField(
                value = label,
                onValueChange = { label = it },
                label = { Text("Display name") },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
            )
            Spacer(Modifier.height(12.dp))

            Row(verticalAlignment = Alignment.CenterVertically) {
                val previewPkg = iconPkg
                val preview = allApps.firstOrNull { it.packageName == previewPkg }?.displayIcon
                    ?: app.systemIcon
                Image(
                    bitmap = preview,
                    contentDescription = "Icon preview",
                    modifier = Modifier.size(44.dp).clip(RoundedCornerShape(12.dp)),
                )
                Spacer(Modifier.width(12.dp))
                OutlinedButton(onClick = { pickingIcon = true }) { Text("Change icon") }
                Spacer(Modifier.width(8.dp))
                if (iconPkg != null) {
                    TextButton(onClick = { iconPkg = null }) { Text("Reset icon") }
                }
            }

            Spacer(Modifier.height(8.dp))
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text("Hide from drawer", modifier = Modifier.weight(1f))
                Switch(checked = hidden, onCheckedChange = { hidden = it })
            }
            Text(
                "Hidden apps stay installed and are reachable from the PIN-protected " +
                    "Hidden list inside this launcher.",
                style = androidx.compose.material3.MaterialTheme.typography.bodySmall,
            )

            Spacer(Modifier.height(20.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                Button(
                    onClick = {
                        viewModel.updateOverride(
                            app,
                            Override(
                                customLabel = label.takeIf { it.isNotBlank() && it != app.systemLabel },
                                customIconPackage = iconPkg,
                                hidden = hidden,
                            ),
                        )
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Save") }
                OutlinedButton(
                    onClick = {
                        viewModel.updateOverride(app, Override())
                        onDismiss()
                    },
                    modifier = Modifier.weight(1f),
                ) { Text("Reset all") }
            }
        }
    }

    if (pickingIcon) {
        IconPickerDialog(
            apps = allApps,
            onPick = { iconPkg = it.packageName; pickingIcon = false },
            onDismiss = { pickingIcon = false },
        )
    }
}

@Composable
private fun IconPickerDialog(
    apps: List<AppInfo>,
    onPick: (AppInfo) -> Unit,
    onDismiss: () -> Unit,
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        confirmButton = {},
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancel") } },
        title = { Text("Borrow an icon") },
        text = {
            LazyColumn(Modifier.heightIn(max = 420.dp)) {
                items(apps, key = { it.key }) { a ->
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onPick(a) }
                            .padding(vertical = 8.dp),
                    ) {
                        Image(
                            bitmap = a.systemIcon,
                            contentDescription = a.systemLabel,
                            modifier = Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)),
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(a.systemLabel)
                    }
                }
            }
        },
    )
}
