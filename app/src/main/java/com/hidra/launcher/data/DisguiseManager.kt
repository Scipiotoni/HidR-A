package com.hidra.launcher.data

import android.content.ComponentName
import android.content.Context
import android.content.pm.PackageManager

/**
 * Changes the launcher's own name + icon by enabling exactly one manifest
 * activity-alias and disabling the rest. This is the only supported, root-free
 * way for an app to change how it appears in the app drawer / home screen.
 */
class DisguiseManager(context: Context) {

    private val appContext = context.applicationContext
    private val pm = appContext.packageManager
    private val prefs = PrefsRepository(appContext)

    fun current(): Disguise = prefs.loadDisguise()

    fun apply(target: Disguise) {
        Disguise.entries.forEach { disguise ->
            val state = if (disguise == target) {
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED
            } else {
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED
            }
            pm.setComponentEnabledSetting(
                ComponentName(appContext, disguise.aliasComponent),
                state,
                PackageManager.DONT_KILL_APP,
            )
        }
        prefs.saveDisguise(target)
    }
}
