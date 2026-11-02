package com.quickfind.feature.settings

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.BackHandler
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.quickfind.core.data.settings.SearchBarPosition
import com.quickfind.core.data.settings.ThemeMode
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.indexing.FilePermissions
import com.quickfind.core.matching.ranking.RankingMode
import com.quickfind.core.model.SourceType
import com.quickfind.core.model.WebSearchEngine

private enum class Page { MAIN, PERMISSIONS, PRIVACY, HIDDEN, ABOUT }

/** Which explanation dialog is showing before a permission prompt. */
private enum class Disclosure { CONTACTS, FILES, ALL_FILES, NONE }

private fun granted(context: Context, permission: String) =
    ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED

/** Settings (spec 14). Permissions are requested only when the user turns a source on (spec 8). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(viewModel: SettingsViewModel, onClose: () -> Unit) {
    val context = LocalContext.current
    val activity = LocalActivity.current
    val s by viewModel.settings.collectAsStateWithLifecycle()
    val hidden by viewModel.hidden.collectAsStateWithLifecycle()
    val indexed by viewModel.indexedFiles.collectAsStateWithLifecycle()

    var page by rememberSaveable { mutableStateOf(Page.MAIN) }
    var disclosure by remember { mutableStateOf(Disclosure.NONE) }
    var showEngineDialog by remember { mutableStateOf(false) }
    var showExcludeDialog by remember { mutableStateOf(false) }
    var confirm by remember { mutableStateOf<Pair<Int, () -> Unit>?>(null) }

    // Permission state can change in system settings while we are paused.
    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val contactsGranted = remember(tick) { granted(context, Manifest.permission.READ_CONTACTS) }
    val mediaGranted = remember(tick) { FilePermissions.hasMediaAccess(context) }
    val partialMedia = remember(tick) { FilePermissions.isPartialMediaAccess(context) }
    val allFiles = remember(tick) { FilePermissions.hasAllFilesAccess(context) }
    val notificationsGranted = remember(tick) {
        Build.VERSION.SDK_INT < 33 || granted(context, Manifest.permission.POST_NOTIFICATIONS)
    }
    val callGranted = remember(tick) { granted(context, Manifest.permission.CALL_PHONE) }
    val isAssistant = remember(tick) { SystemLinks.isDefaultAssistant(context) }

    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) viewModel.update { it.copy(contactsEnabled = true) }
    }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        tick++
        if (result.values.any { it }) viewModel.updateAndReindex { it.copy(filesEnabled = true) }
    }
    val notificationLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) viewModel.update { it.copy(persistentNotification = true) }
    }
    val callLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) viewModel.update { it.copy(directCalling = true) }
    }
    val treeLauncher = rememberLauncherForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri != null) {
            context.contentResolver.takePersistableUriPermission(uri, Intent.FLAG_GRANT_READ_URI_PERMISSION)
            viewModel.updateAndReindex { it.copy(documentTreeUris = it.documentTreeUris + uri.toString(), filesEnabled = true) }
        }
    }

    BackHandler(enabled = page != Page.MAIN) {
        page = if (page == Page.HIDDEN) Page.PRIVACY else Page.MAIN
    }

    val title = when (page) {
        Page.MAIN -> R.string.settings_title
        Page.PERMISSIONS -> R.string.permissions_title
        Page.PRIVACY -> R.string.privacy_title
        Page.HIDDEN -> R.string.hidden_title
        Page.ABOUT -> R.string.about_title
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(stringResource(title)) },
                navigationIcon = {
                    IconButton(onClick = {
                        when (page) {
                            Page.MAIN -> onClose()
                            Page.HIDDEN -> page = Page.PRIVACY
                            else -> page = Page.MAIN
                        }
                    }) { Icon(Icons.AutoMirrored.Filled.ArrowBack, stringResource(R.string.back)) }
                },
            )
        },
    ) { padding ->
        LazyColumn(contentPadding = padding, modifier = Modifier.fillMaxWidth()) {
            when (page) {
                Page.MAIN -> mainPage(
                    s = s,
                    viewModel = viewModel,
                    onContacts = { on ->
                        when {
                            !on -> viewModel.update { it.copy(contactsEnabled = false) }
                            contactsGranted -> viewModel.update { it.copy(contactsEnabled = true) }
                            else -> disclosure = Disclosure.CONTACTS
                        }
                    },
                    onFiles = { on ->
                        when {
                            !on -> viewModel.update { it.copy(filesEnabled = false) }
                            mediaGranted || allFiles -> viewModel.updateAndReindex { it.copy(filesEnabled = true) }
                            else -> disclosure = Disclosure.FILES
                        }
                    },
                    onNotification = { on ->
                        if (on && !notificationsGranted) {
                            notificationLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                        } else {
                            viewModel.update { it.copy(persistentNotification = on) }
                        }
                    },
                    onDirectCalling = { on ->
                        if (on && !callGranted) callLauncher.launch(Manifest.permission.CALL_PHONE) else viewModel.update { it.copy(directCalling = on) }
                    },
                    onEngine = { showEngineDialog = true },
                    onNavigate = { page = it },
                    isAssistant = isAssistant,
                    onAssistant = { activity?.let(SystemLinks::openAssistantSettings) },
                    canPinWidget = SystemLinks.canPinWidget(context),
                    onAddWidget = { SystemLinks.requestPinWidget(context) },
                )
                Page.PERMISSIONS -> permissionsPage(
                    contactsGranted = contactsGranted,
                    mediaGranted = mediaGranted,
                    partialMedia = partialMedia,
                    allFiles = allFiles,
                    notificationsGranted = notificationsGranted,
                    callGranted = callGranted,
                    onContacts = { contactsLauncher.launch(Manifest.permission.READ_CONTACTS) },
                    onMedia = { mediaLauncher.launch(FilePermissions.mediaPermissions()) },
                    onAllFiles = { disclosure = Disclosure.ALL_FILES },
                    onAppSettings = { activity?.let(SystemLinks::openAppDetails) },
                )
                Page.PRIVACY -> privacyPage(
                    s = s,
                    indexed = indexed,
                    hiddenCount = hidden.size,
                    viewModel = viewModel,
                    onAddFolder = { treeLauncher.launch(null) },
                    onExclude = { showExcludeDialog = true },
                    onHidden = { page = Page.HIDDEN },
                    onConfirm = { msg, action -> confirm = msg to action },
                )
                Page.HIDDEN -> {
                    if (hidden.isEmpty()) {
                        item { Text(stringResource(R.string.hidden_empty), Modifier.padding(24.dp)) }
                    }
                    items(hidden, key = { it.resultId }) { h ->
                        ListItem(
                            headlineContent = { Text(h.title) },
                            supportingContent = { Text(h.type.lowercase().replaceFirstChar { it.uppercase() }) },
                            trailingContent = {
                                TextButton(onClick = { viewModel.unhide(h.resultId) }) { Text(stringResource(R.string.unhide)) }
                            },
                        )
                    }
                }
                Page.ABOUT -> aboutPage(versionName(context))
            }
        }
    }

    when (disclosure) {
        Disclosure.CONTACTS -> DisclosureDialog(
            title = stringResource(R.string.disclosure_contacts_title),
            text = stringResource(R.string.disclosure_contacts_text),
            onConfirm = {
                disclosure = Disclosure.NONE
                contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
            },
            onDismiss = { disclosure = Disclosure.NONE },
        )
        Disclosure.FILES -> DisclosureDialog(
            title = stringResource(R.string.disclosure_files_title),
            text = stringResource(R.string.disclosure_files_text),
            onConfirm = {
                disclosure = Disclosure.NONE
                mediaLauncher.launch(FilePermissions.mediaPermissions())
            },
            onDismiss = { disclosure = Disclosure.NONE },
        )
        Disclosure.ALL_FILES -> DisclosureDialog(
            title = stringResource(R.string.disclosure_all_files_title),
            text = stringResource(R.string.disclosure_all_files_text),
            onConfirm = {
                disclosure = Disclosure.NONE
                viewModel.update { it.copy(filesEnabled = true) }
                activity?.let(SystemLinks::openAllFilesAccess)
            },
            onDismiss = { disclosure = Disclosure.NONE },
        )
        Disclosure.NONE -> Unit
    }

    if (showEngineDialog) {
        EngineDialog(s, onDismiss = { showEngineDialog = false }) { engine, template ->
            showEngineDialog = false
            viewModel.update { it.copy(searchEngine = engine, customSearchTemplate = template) }
        }
    }
    if (showExcludeDialog) {
        ExcludeFolderDialog(onDismiss = { showExcludeDialog = false }) { path ->
            showExcludeDialog = false
            viewModel.updateAndReindex { it.copy(excludedFolders = it.excludedFolders + path) }
        }
    }
    confirm?.let { (message, action) ->
        AlertDialog(
            onDismissRequest = { confirm = null },
            text = { Text(stringResource(message)) },
            confirmButton = {
                TextButton(onClick = {
                    confirm = null
                    action()
                }) { Text(stringResource(R.string.confirm_delete)) }
            },
            dismissButton = { TextButton(onClick = { confirm = null }) { Text(stringResource(R.string.cancel)) } },
        )
    }
}

private fun LazyListScope.mainPage(
    s: UserSettings,
    viewModel: SettingsViewModel,
    onContacts: (Boolean) -> Unit,
    onFiles: (Boolean) -> Unit,
    onNotification: (Boolean) -> Unit,
    onDirectCalling: (Boolean) -> Unit,
    onEngine: () -> Unit,
    onNavigate: (Page) -> Unit,
    isAssistant: Boolean,
    onAssistant: () -> Unit,
    canPinWidget: Boolean,
    onAddWidget: () -> Unit,
) {
    item { SectionTitle(stringResource(R.string.section_sources)) }
    item { SwitchRow(stringResource(R.string.source_apps), null, s.appsEnabled) { on -> viewModel.update { it.copy(appsEnabled = on) } } }
    item { SwitchRow(stringResource(R.string.source_work_apps), null, s.showWorkApps, enabled = s.appsEnabled) { on -> viewModel.update { it.copy(showWorkApps = on) } } }
    item { SwitchRow(stringResource(R.string.source_contacts), stringResource(R.string.source_contacts_summary), s.contactsEnabled, onChange = onContacts) }
    item { SwitchRow(stringResource(R.string.source_files), stringResource(R.string.source_files_summary), s.filesEnabled, onChange = onFiles) }
    item { SwitchRow(stringResource(R.string.source_settings), null, s.settingsEnabled) { on -> viewModel.update { it.copy(settingsEnabled = on) } } }
    item { SwitchRow(stringResource(R.string.source_calculator), null, s.calculatorEnabled) { on -> viewModel.update { it.copy(calculatorEnabled = on) } } }
    item { SwitchRow(stringResource(R.string.source_web), null, s.webEnabled) { on -> viewModel.update { it.copy(webEnabled = on) } } }

    item { SectionTitle(stringResource(R.string.section_search)) }
    item {
        val name = if (s.searchEngine == WebSearchEngine.CUSTOM) stringResource(R.string.engine_custom) else s.searchEngine.displayName
        ClickRow(stringResource(R.string.search_engine), name, onClick = onEngine)
    }
    item {
        SwitchRow(
            stringResource(R.string.ranking_learn),
            stringResource(R.string.ranking_learn_summary),
            s.rankingMode == RankingMode.SMART,
        ) { on -> viewModel.update { it.copy(rankingMode = if (on) RankingMode.SMART else RankingMode.MATCH_ONLY) } }
    }
    item {
        SwitchRow(stringResource(R.string.prefixes), stringResource(R.string.prefixes_summary), s.prefixesEnabled) { on ->
            viewModel.update { it.copy(prefixesEnabled = on) }
        }
    }
    item {
        SwitchRow(stringResource(R.string.currency), stringResource(R.string.currency_summary), s.currencyEnabled) { on ->
            viewModel.update { it.copy(currencyEnabled = on) }
        }
    }
    item { SwitchRow(stringResource(R.string.clear_on_reopen), null, s.clearQueryOnReopen) { on -> viewModel.update { it.copy(clearQueryOnReopen = on) } } }
    item { SwitchRow(stringResource(R.string.direct_calling), stringResource(R.string.direct_calling_summary), s.directCalling, onChange = onDirectCalling) }

    item { SectionTitle(stringResource(R.string.section_priorities)) }
    for ((type, label) in listOf(
        SourceType.APP to R.string.source_apps,
        SourceType.CONTACT to R.string.source_contacts,
        SourceType.SETTING to R.string.source_settings,
        SourceType.FILE to R.string.source_files,
    )) {
        item(key = "bias-$type") {
            SliderRow(stringResource(label), s.typeBias[type] ?: 0, -50..50) { v ->
                viewModel.update { cur ->
                    val bias = cur.typeBias.toMutableMap()
                    bias[type] = v
                    if (type == SourceType.FILE) bias[SourceType.FOLDER] = v
                    cur.copy(typeBias = bias)
                }
            }
        }
    }

    item { SectionTitle(stringResource(R.string.section_appearance)) }
    item {
        Column {
            Text(stringResource(R.string.bar_position), Modifier.padding(start = 16.dp, top = 8.dp))
            RadioGroup(
                options = SearchBarPosition.entries.toList(),
                selected = s.searchBarPosition,
                label = { stringResource(if (it == SearchBarPosition.TOP) R.string.bar_top else R.string.bar_bottom) },
            ) { pos -> viewModel.update { it.copy(searchBarPosition = pos) } }
        }
    }
    item {
        Column {
            Text(stringResource(R.string.theme), Modifier.padding(start = 16.dp, top = 8.dp))
            RadioGroup(
                options = ThemeMode.entries.toList(),
                selected = s.themeMode,
                label = {
                    stringResource(
                        when (it) {
                            ThemeMode.SYSTEM -> R.string.theme_system
                            ThemeMode.LIGHT -> R.string.theme_light
                            ThemeMode.DARK -> R.string.theme_dark
                        },
                    )
                },
            ) { mode -> viewModel.update { it.copy(themeMode = mode) } }
        }
    }
    if (Build.VERSION.SDK_INT >= 31) {
        item { SwitchRow(stringResource(R.string.dynamic_color), null, s.dynamicColor) { on -> viewModel.update { it.copy(dynamicColor = on) } } }
    }

    item { SectionTitle(stringResource(R.string.section_access)) }
    if (canPinWidget) item { ClickRow(stringResource(R.string.add_widget), stringResource(R.string.add_widget_summary), onClick = onAddWidget) }
    item {
        ClickRow(
            stringResource(R.string.set_assistant),
            stringResource(if (isAssistant) R.string.set_assistant_done else R.string.set_assistant_summary),
            onClick = onAssistant,
        )
    }
    item { SwitchRow(stringResource(R.string.persistent_notification), stringResource(R.string.persistent_notification_summary), s.persistentNotification, onChange = onNotification) }

    item { SectionTitle(stringResource(R.string.section_more)) }
    item { ClickRow(stringResource(R.string.permissions_title), stringResource(R.string.permissions_summary)) { onNavigate(Page.PERMISSIONS) } }
    item { ClickRow(stringResource(R.string.privacy_title), stringResource(R.string.privacy_summary)) { onNavigate(Page.PRIVACY) } }
    // Phase 3 adds Play Billing; the row is here so the settings layout does not change later.
    item { ClickRow(stringResource(R.string.pro_title), stringResource(R.string.pro_summary), enabled = false) {} }
    item { ClickRow(stringResource(R.string.about_title), null) { onNavigate(Page.ABOUT) } }
}

private fun LazyListScope.permissionsPage(
    contactsGranted: Boolean,
    mediaGranted: Boolean,
    partialMedia: Boolean,
    allFiles: Boolean,
    notificationsGranted: Boolean,
    callGranted: Boolean,
    onContacts: () -> Unit,
    onMedia: () -> Unit,
    onAllFiles: () -> Unit,
    onAppSettings: () -> Unit,
) {
    item { Text(stringResource(R.string.permissions_intro), Modifier.padding(16.dp)) }
    item { PermissionRow(stringResource(R.string.perm_contacts), contactsGranted, onContacts) }
    item {
        val status = when {
            partialMedia -> stringResource(R.string.perm_partial)
            mediaGranted -> stringResource(R.string.perm_granted)
            else -> stringResource(R.string.perm_not_granted)
        }
        ClickRow(stringResource(R.string.perm_media), status, onClick = onMedia)
    }
    if (Build.VERSION.SDK_INT >= 30) {
        item { PermissionRow(stringResource(R.string.perm_all_files), allFiles, onAllFiles) }
    }
    if (Build.VERSION.SDK_INT >= 33) {
        item { PermissionRow(stringResource(R.string.perm_notifications), notificationsGranted, onAppSettings) }
    }
    item { PermissionRow(stringResource(R.string.perm_call), callGranted, onAppSettings) }
    item { ClickRow(stringResource(R.string.open_app_settings), null, onClick = onAppSettings) }
}

@Composable
private fun PermissionRow(title: String, granted: Boolean, onClick: () -> Unit) {
    ClickRow(
        title = title,
        summary = stringResource(if (granted) R.string.perm_granted else R.string.perm_not_granted),
        enabled = !granted,
        onClick = onClick,
    )
}

private fun LazyListScope.privacyPage(
    s: UserSettings,
    indexed: Int,
    hiddenCount: Int,
    viewModel: SettingsViewModel,
    onAddFolder: () -> Unit,
    onExclude: () -> Unit,
    onHidden: () -> Unit,
    onConfirm: (Int, () -> Unit) -> Unit,
) {
    item { Text(stringResource(R.string.privacy_intro), Modifier.padding(16.dp)) }
    item { SectionTitle(stringResource(R.string.section_index)) }
    item { ListItem(headlineContent = { Text(stringResource(R.string.indexed_files, indexed)) }) }
    item {
        SwitchRow(stringResource(R.string.pause_indexing), stringResource(R.string.pause_indexing_summary), s.indexingPaused) { on ->
            viewModel.update { it.copy(indexingPaused = on) }
        }
    }
    item {
        SwitchRow(stringResource(R.string.index_hidden), null, s.indexHiddenFolders) { on ->
            viewModel.updateAndReindex { it.copy(indexHiddenFolders = on) }
        }
    }
    item { ClickRow(stringResource(R.string.reindex_now), null, enabled = s.filesEnabled && !s.indexingPaused) { viewModel.reindex() } }
    item { ClickRow(stringResource(R.string.add_folder), stringResource(R.string.add_folder_summary), onClick = onAddFolder) }
    items(s.documentTreeUris.toList(), key = { "tree-$it" }) { uri ->
        ListItem(
            headlineContent = { Text(android.net.Uri.parse(uri).lastPathSegment ?: uri) },
            trailingContent = {
                TextButton(onClick = { viewModel.updateAndReindex { it.copy(documentTreeUris = it.documentTreeUris - uri) } }) {
                    Text(stringResource(R.string.remove))
                }
            },
        )
    }
    item { ClickRow(stringResource(R.string.exclude_folder), stringResource(R.string.exclude_folder_summary), onClick = onExclude) }
    items(s.excludedFolders.toList(), key = { "ex-$it" }) { path ->
        ListItem(
            headlineContent = { Text(path) },
            trailingContent = {
                TextButton(onClick = { viewModel.updateAndReindex { it.copy(excludedFolders = it.excludedFolders - path) } }) {
                    Text(stringResource(R.string.remove))
                }
            },
        )
    }
    item { SectionTitle(stringResource(R.string.section_data)) }
    item { ClickRow(stringResource(R.string.hidden_title), stringResource(R.string.hidden_count, hiddenCount), onClick = onHidden) }
    item { ClickRow(stringResource(R.string.clear_history), stringResource(R.string.clear_history_summary)) { onConfirm(R.string.clear_history_confirm) { viewModel.clearHistory() } } }
    item { ClickRow(stringResource(R.string.delete_index), stringResource(R.string.delete_index_summary)) { onConfirm(R.string.delete_index_confirm) { viewModel.deleteIndex() } } }
}

private fun LazyListScope.aboutPage(version: String) {
    item { ListItem(headlineContent = { Text(stringResource(R.string.app_name_settings)) }, supportingContent = { Text(stringResource(R.string.version, version)) }) }
    item { Text(stringResource(R.string.about_privacy), Modifier.padding(16.dp)) }
    item { SectionTitle(stringResource(R.string.licenses)) }
    items(OpenSourceLicenses.entries, key = { it.first }) { (name, license) ->
        ListItem(headlineContent = { Text(name) }, supportingContent = { Text(license) })
    }
}

private fun versionName(context: Context): String = runCatching {
    context.packageManager.getPackageInfo(context.packageName, 0).versionName
}.getOrNull() ?: "?"

@Composable
private fun DisclosureDialog(title: String, text: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(text) },
        confirmButton = { TextButton(onClick = onConfirm) { Text(stringResource(R.string.continue_label)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.not_now)) } },
    )
}

@Composable
private fun EngineDialog(s: UserSettings, onDismiss: () -> Unit, onSave: (WebSearchEngine, String) -> Unit) {
    var engine by remember { mutableStateOf(s.searchEngine) }
    var template by remember { mutableStateOf(s.customSearchTemplate) }
    val templateValid = engine != WebSearchEngine.CUSTOM || (template.startsWith("https://") && template.contains("%s"))
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.search_engine)) },
        text = {
            Column {
                RadioGroup(
                    options = WebSearchEngine.entries.toList(),
                    selected = engine,
                    label = { if (it == WebSearchEngine.CUSTOM) stringResource(R.string.engine_custom) else it.displayName },
                ) { engine = it }
                if (engine == WebSearchEngine.CUSTOM) {
                    OutlinedTextField(
                        value = template,
                        onValueChange = { template = it },
                        label = { Text(stringResource(R.string.engine_template)) },
                        supportingText = { Text(stringResource(R.string.engine_template_hint)) },
                        isError = !templateValid,
                        singleLine = true,
                    )
                }
            }
        },
        confirmButton = { TextButton(enabled = templateValid, onClick = { onSave(engine, template.trim()) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}

@Composable
private fun ExcludeFolderDialog(onDismiss: () -> Unit, onSave: (String) -> Unit) {
    var path by remember { mutableStateOf("") }
    val normalized = path.trim().trim('/').let { if (it.isEmpty()) "" else "$it/" }
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.exclude_folder)) },
        text = {
            OutlinedTextField(
                value = path,
                onValueChange = { path = it },
                label = { Text(stringResource(R.string.exclude_folder_path)) },
                supportingText = { Text(stringResource(R.string.exclude_folder_hint)) },
                singleLine = true,
            )
        },
        confirmButton = { TextButton(enabled = normalized.isNotEmpty(), onClick = { onSave(normalized) }) { Text(stringResource(R.string.save)) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text(stringResource(R.string.cancel)) } },
    )
}
