package com.quickfind.feature.search

import android.Manifest
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.ClipData
import android.content.ClipboardManager
import android.content.ComponentName
import android.content.Intent
import android.content.pm.LauncherApps
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.UserManager
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.core.content.FileProvider
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.matching.PhoneNumbers
import com.quickfind.core.model.ResultAction
import com.quickfind.core.sources.instant.InstalledApps
import com.quickfind.feature.search.folder.FolderActivity
import java.io.File
import javax.inject.Inject

enum class ActionOutcome {
    /** Something else is now in front: close the search overlay. */
    LAUNCHED,

    /** Done in place (copied, opened an in-app screen): keep search open. */
    HANDLED,

    FAILED,
}

/** Turns [ResultAction]s into Intents. All Android specifics of acting on a result live here. */
class ActionExecutor @Inject constructor(
    private val settings: SettingsRepository,
    private val installedApps: InstalledApps,
) {
    fun execute(activity: Activity, action: ResultAction): ActionOutcome = try {
        run(activity, action)
    } catch (_: ActivityNotFoundException) {
        toast(activity, R.string.error_no_app)
        ActionOutcome.FAILED
    } catch (_: SecurityException) {
        toast(activity, R.string.error_cannot_open)
        ActionOutcome.FAILED
    } catch (_: IllegalArgumentException) {
        // FileProvider rejects paths outside its roots; LauncherApps rejects stale components.
        toast(activity, R.string.error_cannot_open)
        ActionOutcome.FAILED
    }

    private fun run(activity: Activity, action: ResultAction): ActionOutcome {
        when (action) {
            is ResultAction.LaunchApp -> {
                val launcherApps = activity.getSystemService(LauncherApps::class.java)
                val userManager = activity.getSystemService(UserManager::class.java)
                val user = userManager.getUserForSerialNumber(action.userSerial) ?: return fail(activity)
                if (Build.VERSION.SDK_INT >= 28 && userManager.isQuietModeEnabled(user)) {
                    // Paused work profile: ask the system to resume it instead of failing silently.
                    userManager.requestQuietModeEnabled(false, user)
                    return ActionOutcome.HANDLED
                }
                launcherApps.startMainActivity(ComponentName(action.packageName, action.activityName), user, null, null)
            }
            is ResultAction.AppDetails -> {
                val launcherApps = activity.getSystemService(LauncherApps::class.java)
                val userManager = activity.getSystemService(UserManager::class.java)
                val user = userManager.getUserForSerialNumber(action.userSerial) ?: return fail(activity)
                val component = launcherApps.getActivityList(action.packageName, user).firstOrNull()?.componentName
                if (component != null) {
                    launcherApps.startAppDetailsActivity(component, user, null, null)
                } else {
                    activity.startActivity(
                        Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS, Uri.fromParts("package", action.packageName, null)),
                    )
                }
            }
            is ResultAction.Uninstall -> activity.startActivity(
                Intent(Intent.ACTION_DELETE, Uri.fromParts("package", action.packageName, null)),
            )
            is ResultAction.PlayStore -> try {
                activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=${action.packageName}")))
            } catch (_: ActivityNotFoundException) {
                openUrl(activity, "https://play.google.com/store/apps/details?id=${action.packageName}")
            }
            is ResultAction.Call -> {
                val direct = settings.settings.value.directCalling &&
                    ContextCompat.checkSelfPermission(activity, Manifest.permission.CALL_PHONE) == PackageManager.PERMISSION_GRANTED
                val uri = Uri.fromParts("tel", action.number, null)
                activity.startActivity(Intent(if (direct) Intent.ACTION_CALL else Intent.ACTION_DIAL, uri))
            }
            is ResultAction.Sms -> activity.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("smsto:" + Uri.encode(action.number))),
            )
            is ResultAction.WhatsApp -> {
                // wa.me needs the full international number. Contacts saved without a country code
                // (e.g. "098765 43210") cannot be fixed reliably here; WhatsApp shows its own error.
                val url = "https://wa.me/" + PhoneNumbers.digitsOnly(action.number)
                val intent = Intent(Intent.ACTION_VIEW, Uri.parse(url))
                installedApps.whatsAppPackage?.let { intent.setPackage(it) }
                try {
                    activity.startActivity(intent)
                } catch (_: ActivityNotFoundException) {
                    openUrl(activity, url)
                }
            }
            is ResultAction.Email -> activity.startActivity(
                Intent(Intent.ACTION_SENDTO, Uri.parse("mailto:" + Uri.encode(action.address))),
            )
            is ResultAction.OpenContact -> activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(action.contactUri)))
            is ResultAction.OpenFile -> {
                val uri = shareableUri(activity, action.uri)
                val intent = Intent(Intent.ACTION_VIEW)
                    .setDataAndType(uri, action.mimeType ?: "*/*")
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                activity.startActivity(intent)
            }
            is ResultAction.ShareFile -> {
                val uri = shareableUri(activity, action.uri)
                val send = Intent(Intent.ACTION_SEND)
                    .setType(action.mimeType ?: "*/*")
                    .putExtra(Intent.EXTRA_STREAM, uri)
                    .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
                send.clipData = ClipData.newRawUri(null, uri)
                activity.startActivity(Intent.createChooser(send, null))
            }
            is ResultAction.OpenFolder -> {
                activity.startActivity(FolderActivity.intent(activity, action.relativePath, action.title))
                return ActionOutcome.HANDLED
            }
            is ResultAction.OpenSetting -> activity.startActivity(Intent(action.intentAction))
            is ResultAction.CopyText -> {
                val clipboard = activity.getSystemService(ClipboardManager::class.java)
                clipboard.setPrimaryClip(ClipData.newPlainText(activity.getString(R.string.clip_label), action.text))
                // Android 13+ shows its own copy confirmation.
                if (Build.VERSION.SDK_INT < 33) toast(activity, R.string.copied)
                return ActionOutcome.HANDLED
            }
            is ResultAction.OpenUrl -> openUrl(activity, action.url)
        }
        return ActionOutcome.LAUNCHED
    }

    private fun openUrl(activity: Activity, url: String) {
        activity.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addCategory(Intent.CATEGORY_BROWSABLE))
    }

    /** Files found by the full walk are stored as file:// and must go through our FileProvider. */
    private fun shareableUri(activity: Activity, uri: String): Uri {
        val parsed = Uri.parse(uri)
        if (parsed.scheme != "file") return parsed
        return FileProvider.getUriForFile(activity, activity.packageName + ".fileprovider", File(requireNotNull(parsed.path)))
    }

    private fun fail(activity: Activity): ActionOutcome {
        toast(activity, R.string.error_cannot_open)
        return ActionOutcome.FAILED
    }

    private fun toast(activity: Activity, res: Int) = Toast.makeText(activity, res, Toast.LENGTH_SHORT).show()
}
