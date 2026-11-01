package com.quickfind.core.indexing

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.os.Environment
import androidx.core.content.ContextCompat

/** The two file permission tiers of spec 5.3. */
object FilePermissions {
    /** Tier A runtime permissions to request for media search on this API level. */
    fun mediaPermissions(): Array<String> = when {
        Build.VERSION.SDK_INT >= 34 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
            Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED,
        )
        Build.VERSION.SDK_INT >= 33 -> arrayOf(
            Manifest.permission.READ_MEDIA_IMAGES,
            Manifest.permission.READ_MEDIA_VIDEO,
            Manifest.permission.READ_MEDIA_AUDIO,
        )
        else -> arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    /** True if any media is readable, including Android 14 "selected photos only". */
    fun hasMediaAccess(context: Context): Boolean = mediaPermissions().any { granted(context, it) }

    /** True when only some photos/videos were shared (Android 14+ partial access). */
    fun isPartialMediaAccess(context: Context): Boolean =
        Build.VERSION.SDK_INT >= 34 &&
            granted(context, Manifest.permission.READ_MEDIA_VISUAL_USER_SELECTED) &&
            !granted(context, Manifest.permission.READ_MEDIA_IMAGES)

    /**
     * Tier B: may we walk the whole shared storage tree?
     * - API 30+: MANAGE_EXTERNAL_STORAGE ("All files access").
     * - API 29: READ_EXTERNAL_STORAGE plus requestLegacyExternalStorage in the manifest.
     * - API 26-28: READ_EXTERNAL_STORAGE already grants full read access.
     */
    fun hasAllFilesAccess(context: Context): Boolean = if (Build.VERSION.SDK_INT >= 30) {
        Environment.isExternalStorageManager()
    } else {
        granted(context, Manifest.permission.READ_EXTERNAL_STORAGE)
    }

    private fun granted(context: Context, permission: String) =
        ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
}
