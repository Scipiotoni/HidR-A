package com.hidra.launcher.data

import androidx.compose.ui.graphics.ImageBitmap

/** A per-app override the user has configured inside the launcher. */
data class Override(
    val customLabel: String? = null,
    /** Package whose icon is borrowed as this app's display icon, or null for original. */
    val customIconPackage: String? = null,
    val hidden: Boolean = false,
)

/**
 * A launchable app plus how this launcher should display it.
 *
 * [systemLabel]/[systemIcon] are what the OS reports; [displayLabel]/[displayIcon]
 * fold in the user's override. Changing those only affects this launcher's UI —
 * the other app's real icon and name on the device are never touched.
 */
data class AppInfo(
    val packageName: String,
    val className: String,
    val systemLabel: String,
    val systemIcon: ImageBitmap,
    val override: Override = Override(),
    val displayIcon: ImageBitmap = systemIcon,
    /** True when the app is disabled system-wide and only known from the hidden cache. */
    val frozen: Boolean = false,
) {
    val key: String get() = "$packageName/$className"
    val displayLabel: String get() = override.customLabel?.takeIf { it.isNotBlank() } ?: systemLabel
    val isHidden: Boolean get() = override.hidden
}

/** The identities the launcher can present for itself on the home screen / app drawer. */
enum class Disguise(val aliasComponent: String) {
    Default("com.hidra.launcher.alias.Default"),
    Calculator("com.hidra.launcher.alias.Calculator"),
    Notes("com.hidra.launcher.alias.Notes"),
}
