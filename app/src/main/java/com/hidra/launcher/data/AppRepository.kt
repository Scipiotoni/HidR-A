package com.hidra.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap

/**
 * Enumerates launchable apps and folds in the user's overrides. Enumeration
 * uses only MAIN/LAUNCHER resolution, which a home app is permitted to do
 * without the QUERY_ALL_PACKAGES permission.
 *
 * Apps that are disabled system-wide no longer resolve, so they are rebuilt
 * from [HiddenStore] and flagged [AppInfo.frozen].
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val prefs = PrefsRepository(appContext)
    val hiddenStore = HiddenStore(appContext)
    private val iconCache = HashMap<String, ImageBitmap>()

    fun load(): List<AppInfo> {
        val overrides = prefs.loadOverrides()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)

        val live = resolved.mapNotNull { ri ->
            val pkg = ri.activityInfo.packageName ?: return@mapNotNull null
            // Don't list ourselves in the drawer.
            if (pkg == appContext.packageName) return@mapNotNull null
            val cls = ri.activityInfo.name ?: return@mapNotNull null
            val label = ri.loadLabel(pm)?.toString().orEmpty()
            val icon = iconFor(pkg) { ri.loadIcon(pm) }
            val key = "$pkg/$cls"
            val ov = overrides[key] ?: Override()
            val displayIcon = ov.customIconPackage
                ?.let { borrowed -> iconFor(borrowed) { appIconOrNull(borrowed) } }
                ?: icon
            AppInfo(
                packageName = pkg,
                className = cls,
                systemLabel = label,
                systemIcon = icon,
                override = ov,
                displayIcon = displayIcon,
            )
        }

        val liveKeys = live.mapTo(HashSet()) { it.key }
        val frozen = hiddenStore.all().mapNotNull { (key, meta) ->
            val ov = overrides[key]
            if (key in liveKeys || ov == null || !ov.hidden) return@mapNotNull null
            val icon = hiddenStore.icon(key) ?: pm.defaultActivityIcon.toImageBitmap()
            val displayIcon = ov.customIconPackage
                ?.let { borrowed -> iconFor(borrowed) { appIconOrNull(borrowed) } }
                ?: icon
            AppInfo(
                packageName = meta.packageName,
                className = meta.className,
                systemLabel = meta.label,
                systemIcon = icon,
                override = ov,
                displayIcon = displayIcon,
                frozen = true,
            )
        }

        return (live + frozen).sortedBy { it.displayLabel.lowercase() }
    }

    /** Returns false if the app could not be started. */
    fun launch(app: AppInfo): Boolean {
        val explicit = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setClassName(app.packageName, app.className)
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        return try {
            appContext.startActivity(explicit)
            true
        } catch (e: Exception) {
            val fallback = pm.getLaunchIntentForPackage(app.packageName)
                ?.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                ?: return false
            try {
                appContext.startActivity(fallback)
                true
            } catch (e2: Exception) {
                false
            }
        }
    }

    private inline fun iconFor(
        cacheKey: String,
        load: () -> Drawable?,
    ): ImageBitmap =
        iconCache.getOrPut(cacheKey) { (load() ?: pm.defaultActivityIcon).toImageBitmap() }

    private fun appIconOrNull(pkg: String): Drawable? =
        runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
}

/** Rasterizes any Drawable (vector, adaptive, bitmap) into a Compose ImageBitmap. */
fun Drawable.toImageBitmap(size: Int = 144): ImageBitmap {
    if (this is BitmapDrawable && bitmap != null) {
        return bitmap.asImageBitmap()
    }
    val w = intrinsicWidth.takeIf { it > 0 } ?: size
    val h = intrinsicHeight.takeIf { it > 0 } ?: size
    val bmp: Bitmap = createBitmap(w, h)
    val canvas = Canvas(bmp)
    setBounds(0, 0, canvas.width, canvas.height)
    draw(canvas)
    return bmp.asImageBitmap()
}
