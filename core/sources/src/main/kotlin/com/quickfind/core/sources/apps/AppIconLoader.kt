package com.quickfind.core.sources.apps

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.LauncherApps
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.os.UserManager
import android.util.LruCache
import androidx.core.graphics.drawable.toBitmap
import com.quickfind.core.data.di.IoDispatcher
import com.quickfind.core.model.ResultIcon
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * App icons with a two-level cache (spec 5.1): an in-memory LRU of ~1/16 of the heap and a
 * disk cache of rendered bitmaps, so a cold start shows icons without touching PackageManager.
 */
@Singleton
class AppIconLoader @Inject constructor(
    @ApplicationContext private val context: Context,
    @IoDispatcher private val io: CoroutineDispatcher,
) {
    private val memory = object : LruCache<String, Bitmap>((Runtime.getRuntime().maxMemory() / 16).toInt()) {
        override fun sizeOf(key: String, value: Bitmap): Int = value.allocationByteCount
    }
    private val diskDir = File(context.cacheDir, "app_icons")
    private val sizePx = (48 * context.resources.displayMetrics.density).toInt().coerceAtLeast(48)

    fun cached(icon: ResultIcon.App): Bitmap? = memory.get(key(icon))

    suspend fun load(icon: ResultIcon.App): Bitmap? {
        val key = key(icon)
        memory.get(key)?.let { return it }
        return withContext(io) {
            val file = File(diskDir, "$key.png")
            if (file.exists()) {
                BitmapFactory.decodeFile(file.path)?.let {
                    memory.put(key, it)
                    return@withContext it
                }
            }
            val bitmap = render(icon) ?: return@withContext null
            memory.put(key, bitmap)
            runCatching {
                diskDir.mkdirs()
                file.outputStream().use { bitmap.compress(Bitmap.CompressFormat.PNG, 100, it) }
            }
            bitmap
        }
    }

    /** Drops cached icons for a package after an install/update/removal. */
    fun invalidate(packageName: String) {
        val prefix = sanitize(packageName) + "__"
        memory.snapshot().keys.filter { it.startsWith(prefix) }.forEach { memory.remove(it) }
        diskDir.listFiles()?.filter { it.name.startsWith(prefix) }?.forEach { it.delete() }
    }

    fun clearDisk() {
        memory.evictAll()
        diskDir.deleteRecursively()
    }

    private fun render(icon: ResultIcon.App): Bitmap? = runCatching {
        val userManager = context.getSystemService(UserManager::class.java)
        val launcherApps = context.getSystemService(LauncherApps::class.java)
        val user = userManager.getUserForSerialNumber(icon.userSerial) ?: return null
        val intent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
            .setComponent(ComponentName(icon.packageName, icon.activityName))
        val info = launcherApps.resolveActivity(intent, user) ?: return null
        // getBadgedIcon adds the work-profile briefcase where needed.
        info.getBadgedIcon(0).toBitmap(sizePx, sizePx, Bitmap.Config.ARGB_8888)
    }.getOrNull()

    private fun key(icon: ResultIcon.App) =
        "${sanitize(icon.packageName)}__${Integer.toHexString(icon.activityName.hashCode())}_${icon.userSerial}"

    private fun sanitize(s: String) = s.replace(Regex("[^A-Za-z0-9._]"), "_")
}
