package com.hidra.launcher.data

import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.drawable.BitmapDrawable
import android.graphics.drawable.Drawable
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.graphics.createBitmap

/**
 * Enumerates launchable apps and folds in the user's overrides. Enumeration
 * uses only MAIN/LAUNCHER resolution, which a home app is permitted to do
 * without the QUERY_ALL_PACKAGES permission.
 */
class AppRepository(context: Context) {

    private val appContext = context.applicationContext
    private val pm: PackageManager = appContext.packageManager
    private val prefs = PrefsRepository(appContext)
    private val iconCache = HashMap<String, androidx.compose.ui.graphics.ImageBitmap>()

    fun load(): List<AppInfo> {
        val overrides = prefs.loadOverrides()
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val resolved = pm.queryIntentActivities(intent, 0)

        return resolved.mapNotNull { ri ->
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
        }.sortedBy { it.displayLabel.lowercase() }
    }

    fun launch(app: AppInfo) {
        val intent = pm.getLaunchIntentForPackage(app.packageName)?.apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        } ?: return
        appContext.startActivity(intent)
    }

    private inline fun iconFor(
        cacheKey: String,
        load: () -> Drawable?,
    ): androidx.compose.ui.graphics.ImageBitmap =
        iconCache.getOrPut(cacheKey) { (load() ?: pm.defaultActivityIcon).toImageBitmap() }

    private fun appIconOrNull(pkg: String): Drawable? =
        runCatching { pm.getApplicationIcon(pkg) }.getOrNull()
}

/** Rasterizes any Drawable (vector, adaptive, bitmap) into a Compose ImageBitmap. */
fun Drawable.toImageBitmap(size: Int = 144): androidx.compose.ui.graphics.ImageBitmap {
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
