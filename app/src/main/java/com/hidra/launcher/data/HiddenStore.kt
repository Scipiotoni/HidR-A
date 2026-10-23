package com.hidra.launcher.data

import android.content.Context
import android.graphics.BitmapFactory
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asAndroidBitmap
import androidx.compose.ui.graphics.asImageBitmap
import org.json.JSONObject
import java.io.File

/**
 * Remembers the name, entry activity and icon of every hidden app. A disabled
 * app no longer shows up in PackageManager queries, so this cache is how the
 * Hidden list can still display it and know what to launch.
 */
class HiddenStore(context: Context) {

    data class Meta(val packageName: String, val className: String, val label: String)

    private val prefs = context.applicationContext
        .getSharedPreferences("hidra_hidden", Context.MODE_PRIVATE)
    private val iconDir = File(context.applicationContext.filesDir, "hidden_icons").apply { mkdirs() }

    fun save(app: AppInfo) {
        val root = readRoot()
        root.put(
            app.key,
            JSONObject()
                .put("pkg", app.packageName)
                .put("cls", app.className)
                .put("label", app.systemLabel),
        )
        prefs.edit().putString(KEY_META, root.toString()).apply()
        runCatching {
            iconFile(app.key).outputStream().use {
                app.systemIcon.asAndroidBitmap()
                    .compress(android.graphics.Bitmap.CompressFormat.PNG, 100, it)
            }
        }
    }

    fun remove(key: String) {
        val root = readRoot()
        root.remove(key)
        prefs.edit().putString(KEY_META, root.toString()).apply()
        iconFile(key).delete()
    }

    fun all(): Map<String, Meta> {
        val root = readRoot()
        return buildMap {
            for (key in root.keys()) {
                val o = root.optJSONObject(key) ?: continue
                put(key, Meta(o.optString("pkg"), o.optString("cls"), o.optString("label")))
            }
        }
    }

    fun icon(key: String): ImageBitmap? =
        runCatching { BitmapFactory.decodeFile(iconFile(key).path)?.asImageBitmap() }.getOrNull()

    private fun iconFile(key: String) = File(iconDir, key.replace('/', '_') + ".png")

    private fun readRoot(): JSONObject =
        runCatching { JSONObject(prefs.getString(KEY_META, "{}")!!) }.getOrDefault(JSONObject())

    private companion object {
        const val KEY_META = "meta"
    }
}
