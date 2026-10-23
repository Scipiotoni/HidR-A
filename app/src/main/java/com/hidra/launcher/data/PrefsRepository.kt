package com.hidra.launcher.data

import android.content.Context
import org.json.JSONObject
import java.security.MessageDigest

/**
 * Lightweight persistence backed by SharedPreferences + JSON. Stores per-app
 * overrides, the hidden-vault PIN hash, and the currently selected disguise.
 * No external DB dependency, so the build stays lean.
 */
class PrefsRepository(context: Context) {

    private val prefs = context.applicationContext
        .getSharedPreferences("hidra_prefs", Context.MODE_PRIVATE)

    // ---- Per-app overrides -------------------------------------------------

    fun loadOverrides(): Map<String, Override> {
        val raw = prefs.getString(KEY_OVERRIDES, null) ?: return emptyMap()
        return runCatching {
            val root = JSONObject(raw)
            buildMap {
                for (key in root.keys()) {
                    val o = root.getJSONObject(key)
                    put(
                        key,
                        Override(
                            customLabel = o.optString("label").ifBlank { null },
                            customIconPackage = o.optString("iconPkg").ifBlank { null },
                            hidden = o.optBoolean("hidden", false),
                        ),
                    )
                }
            }
        }.getOrDefault(emptyMap())
    }

    fun saveOverride(key: String, override: Override) {
        val root = runCatching { JSONObject(prefs.getString(KEY_OVERRIDES, "{}")!!) }
            .getOrDefault(JSONObject())
        val isEmpty = override.customLabel == null &&
            override.customIconPackage == null &&
            !override.hidden
        if (isEmpty) {
            root.remove(key)
        } else {
            root.put(
                key,
                JSONObject()
                    .put("label", override.customLabel ?: "")
                    .put("iconPkg", override.customIconPackage ?: "")
                    .put("hidden", override.hidden),
            )
        }
        prefs.edit().putString(KEY_OVERRIDES, root.toString()).apply()
    }

    // ---- Hidden-vault PIN --------------------------------------------------

    fun hasPin(): Boolean = prefs.contains(KEY_PIN)

    fun setPin(pin: String) {
        prefs.edit().putString(KEY_PIN, sha256(pin)).apply()
    }

    fun checkPin(pin: String): Boolean = prefs.getString(KEY_PIN, null) == sha256(pin)

    // ---- Disguise ----------------------------------------------------------

    fun loadDisguise(): Disguise =
        runCatching { Disguise.valueOf(prefs.getString(KEY_DISGUISE, null) ?: "") }
            .getOrDefault(Disguise.Default)

    fun saveDisguise(disguise: Disguise) {
        prefs.edit().putString(KEY_DISGUISE, disguise.name).apply()
    }

    private fun sha256(input: String): String =
        MessageDigest.getInstance("SHA-256")
            .digest(input.toByteArray())
            .joinToString("") { "%02x".format(it) }

    private companion object {
        const val KEY_OVERRIDES = "overrides"
        const val KEY_PIN = "pin_hash"
        const val KEY_DISGUISE = "disguise"
    }
}
