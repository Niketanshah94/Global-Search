package com.quickfind.feature.search.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.feature.search.R
import com.quickfind.feature.search.SearchScope
import com.quickfind.feature.search.SearchUiState
import com.quickfind.core.ui.R as UiR

/** Everything the search screen can ask its host to do. Keeps [SearchScreen] stateless. */
interface SearchActions {
    fun onQueryChange(text: String)
    fun onSubmit()
    fun onResultClick(result: SearchResult)
    fun onResultLongClick(result: SearchResult)
    fun onAction(result: SearchResult, action: ResultAction)
    fun onToggleSection(section: ResultSection)
    /** Filter chip: show only one kind of result (null = All). */
    fun onFilter(section: ResultSection?)
    fun onClearScope()
    fun onCopy(text: String)
    fun onPasteClipboard()
    fun onDismissClipboard()
    fun onOpenSettings()
    fun onDismiss()
}

object SearchTestTags {
    const val FIELD = "search_field"
    const val TOP_HIT = "top_hit"
    const val RESULTS = "results"
}

/**
 * "Thumb Zone": the search bar sits just above the keyboard and results grow upward from it,
 * so the best result is always the closest thing to your thumb. Filter chips sit between the
 * results and the bar. With the bar at the top (a setting) the same pieces stack downward.
 */
@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    /** Read straight from snapshot state so the field never lags the keyboard. */
    queryText: String,
    state: SearchUiState,
    barAtBottom: Boolean,
    actions: SearchActions,
    focusRequester: FocusRequester,
    icon: IconContent,
) {
    val dismissThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    // Keyed items make LazyColumn keep the old first item in view when results change.
    // Always show the best result (item 0, next to the bar) first.
    val listState = rememberLazyListState()
    LaunchedEffect(state.query, state.topHit?.id, state.filter) { listState.scrollToItem(0) }

    Box(
        Modifier
            .fillMaxSize()
            // Exposes test tags as resource ids so Macrobenchmark/UiAutomator can find the field.
            .semantics { testTagsAsResourceId = true }
            .background(MaterialTheme.colorScheme.background),
    ) {
        Column(Modifier.fillMaxSize().systemBarsPadding().imePadding()) {
            if (barAtBottom) {
                Header(actions)
            } else {
                SearchBar(queryText, state, actions, focusRequester, dismissThreshold)
                FilterChips(state, actions)
            }
            LazyColumn(
                state = listState,
                modifier = Modifier.weight(1f).fillMaxWidth().testTag(SearchTestTags.RESULTS),
                // Bar at the bottom: item 0 (the top hit) sits right above it, the rest grows up.
                reverseLayout = barAtBottom,
                verticalArrangement = Arrangement.spacedBy(10.dp, if (barAtBottom) Alignment.Bottom else Alignment.Top),
                contentPadding = PaddingValues(horizontal = 14.dp, vertical = 10.dp),
            ) {
                if (state.hasQuery) results(state, actions, icon) else emptyState(state, actions, icon)
            }
            if (barAtBottom) {
                FilterChips(state, actions)
                SearchBar(queryText, state, actions, focusRequester, dismissThreshold)
            }
        }
    }
}

@Composable
private fun Header(actions: SearchActions) {
    Row(Modifier.fillMaxWidth().padding(start = 20.dp, end = 8.dp, top = 8.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(Modifier.weight(1f)) {
            Text(
                stringResource(R.string.screen_title),
                style = MaterialTheme.typography.headlineMedium.copy(fontWeight = FontWeight.ExtraBold),
            )
            Text(
                stringResource(R.string.screen_subtitle),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        IconButton(onClick = actions::onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.open_settings))
        }
    }
}

/** White bar with a soft shadow; shows what Enter will do ("↵ Call"). Swipe down to close. */
@Composable
private fun SearchBar(
    queryText: String,
    state: SearchUiState,
    actions: SearchActions,
    focusRequester: FocusRequester,
    dismissThreshold: Float,
) {
    val scope = state.scope
    val dragged = remember { FloatArray(1) }
    val dragState = rememberDraggableState { delta -> dragged[0] += delta }
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 8.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                onDragStarted = { dragged[0] = 0f },
                onDragStopped = { if (dragged[0] > dismissThreshold) actions.onDismiss() },
            ),
    ) {
        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.padding(end = 6.dp)) {
            TextField(
                value = queryText,
                onValueChange = actions::onQueryChange,
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .testTag(SearchTestTags.FIELD),
                placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
                leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.onSurface) },
                prefix = if (scope != null) {
                    {
                        InputChip(
                            selected = true,
                            onClick = actions::onClearScope,
                            label = { Text(scopeLabel(scope)) },
                            trailingIcon = { Icon(Icons.Default.Close, stringResource(R.string.clear_scope), Modifier.size(16.dp)) },
                            modifier = Modifier.padding(end = 6.dp),
                        )
                    }
                } else {
                    null
                },
                trailingIcon = if (queryText.isNotEmpty()) {
                    { IconButton(onClick = { actions.onQueryChange("") }) { Icon(Icons.Default.Clear, stringResource(R.string.clear_query)) } }
                } else {
                    null
                },
                textStyle = MaterialTheme.typography.titleMedium,
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, autoCorrectEnabled = false),
                keyboardActions = KeyboardActions(onGo = { actions.onSubmit() }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = Color.Transparent,
                    unfocusedIndicatorColor = Color.Transparent,
                    cursorColor = MaterialTheme.colorScheme.primary,
                ),
            )
            val top = state.topHit
            if (top != null && queryText.isNotEmpty()) {
                // Tells the user what Enter does before they press it.
                Text(
                    "↵ " + shortActionLabel(top.primaryAction),
                    style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    modifier = Modifier
                        .clip(RoundedCornerShape(8.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(horizontal = 8.dp, vertical = 4.dp),
                )
            } else if (queryText.isEmpty() && state.scope == null) {
                // The header has settings when the bar is at the bottom; this keeps it reachable on top.
                Spacer(Modifier.width(4.dp))
            }
        }
    }
}

/** "All · Contacts 3 · Apps 1 · Files 2 · Web": tap to show only that kind. */
@Composable
private fun FilterChips(state: SearchUiState, actions: SearchActions) {
    if (!state.hasQuery || state.counts.size < 2 && state.filter == null) return
    Row(
        Modifier
            .fillMaxWidth()
            .horizontalScroll(rememberScrollState())
            .padding(horizontal = 14.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp),
    ) {
        Chip(stringResource(R.string.filter_all), selected = state.filter == null) { actions.onFilter(null) }
        state.counts.forEach { (section, count) ->
            val label = if (section == ResultSection.WEB) sectionTitle(section) else "${sectionTitle(section)} $count"
            Chip(label, selected = state.filter == section) { actions.onFilter(section) }
        }
    }
}

@Composable
private fun Chip(text: String, selected: Boolean, onClick: () -> Unit) {
    val bg = if (selected) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surfaceVariant
    val fg = if (selected) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurfaceVariant
    Text(
        text,
        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
        color = fg,
        maxLines = 1,
        modifier = Modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 14.dp, vertical = 8.dp),
    )
}

@Composable
private fun scopeLabel(scope: SearchScope): String = stringResource(
    when (scope) {
        SearchScope.APPS -> R.string.scope_apps
        SearchScope.CONTACTS -> R.string.scope_contacts
        SearchScope.FILES -> R.string.scope_files
        SearchScope.SETTINGS -> R.string.scope_settings
    },
)

@Composable
private fun sectionTitle(section: ResultSection): String = stringResource(
    when (section) {
        ResultSection.APPS -> R.string.section_apps
        ResultSection.CONTACTS -> R.string.section_contacts
        ResultSection.CALENDAR -> R.string.section_calendar
        ResultSection.FILES -> R.string.section_files
        ResultSection.SETTINGS -> R.string.section_settings
        ResultSection.NOTIFICATIONS -> R.string.section_notifications
        ResultSection.WEB -> R.string.section_web
    },
)

/** Item 0 is drawn next to the bar, so the order here is "most important first". */
private fun LazyListScope.results(state: SearchUiState, actions: SearchActions, icon: IconContent) {
    state.topHit?.let { top ->
        item(key = "top:${top.id}", contentType = "top") {
            TopHitCard(
                result = top,
                onClick = { actions.onResultClick(top) },
                onLongClick = { actions.onResultLongClick(top) },
                onAction = { actions.onAction(top, it) },
                icon = icon,
                modifier = Modifier.animateItem().testTag(SearchTestTags.TOP_HIT),
            )
        }
    }
    for (section in state.sections) {
        items(section.visible, key = { "${section.section}:${it.id}" }, contentType = { "row" }) { result ->
            ResultCard(result, actions, icon, Modifier.animateItem())
        }
    }
    if (state.noResults) {
        item(key = "none", contentType = "none") {
            Text(
                stringResource(R.string.no_results, state.query),
                modifier = Modifier.padding(24.dp).animateItem(),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.emptyState(state: SearchUiState, actions: SearchActions, icon: IconContent) {
    val empty = state.empty
    empty.otp?.let { otp ->
        item(key = "otp", contentType = "pill") {
            Pill(
                text = stringResource(R.string.copy_otp, otp.code, otp.source),
                iconRes = UiR.drawable.ic_key,
                emphasized = true,
                modifier = Modifier.animateItem(),
            ) { actions.onCopy(otp.code) }
        }
    }
    if (empty.clipboardAvailable) {
        item(key = "clipboard", contentType = "pill") {
            Pill(stringResource(R.string.paste_clipboard), UiR.drawable.ic_copy, emphasized = false, modifier = Modifier.animateItem()) {
                actions.onPasteClipboard()
            }
        }
    }
    item(key = "suggestions", contentType = "suggestions") { Suggestions(actions) }
    if (empty.recentApps.isNotEmpty()) {
        item(key = "recent-apps", contentType = "grid") {
            CardBlock(stringResource(R.string.section_recent_apps), Modifier.animateItem()) { AppGrid(empty.recentApps, actions, icon) }
        }
    }
    if (empty.pinned.isNotEmpty()) {
        items(empty.pinned, key = { "pinned:${it.id}" }, contentType = { "row" }) { r ->
            ResultCard(r, actions, icon, Modifier.animateItem(), badge = stringResource(R.string.section_pinned))
        }
    }
    if (empty.recentFiles.isNotEmpty()) {
        items(empty.recentFiles, key = { "recent-file:${it.id}" }, contentType = { "row" }) { r ->
            ResultCard(r, actions, icon, Modifier.animateItem(), badge = stringResource(R.string.section_recent_files))
        }
    }
}

/** One result as its own white card: icon, title, "Type · detail", and quick actions. */
@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun ResultCard(result: SearchResult, actions: SearchActions, icon: IconContent, modifier: Modifier = Modifier, badge: String? = null) {
    Surface(
        shape = RoundedCornerShape(22.dp),
        color = MaterialTheme.colorScheme.surface,
        shadowElevation = 1.dp,
        modifier = modifier.fillMaxWidth(),
    ) {
        ResultRow(
            result = result,
            onClick = { actions.onResultClick(result) },
            onLongClick = { actions.onResultLongClick(result) },
            icon = icon,
            onAction = { actions.onAction(result, it) },
            typePrefix = badge ?: typeLabel(result.type),
        )
    }
}

@Composable
private fun Pill(text: String, iconRes: Int, emphasized: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    val bg = if (emphasized) MaterialTheme.colorScheme.inverseSurface else MaterialTheme.colorScheme.surface
    val fg = if (emphasized) MaterialTheme.colorScheme.inverseOnSurface else MaterialTheme.colorScheme.onSurface
    Row(
        modifier
            .clip(CircleShape)
            .background(bg)
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(painterResource(iconRes), null, Modifier.size(18.dp), tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(8.dp))
        Text(text, color = fg, style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold), maxLines = 1, overflow = TextOverflow.Ellipsis)
    }
}

/** One-tap examples that teach what search can do. */
@OptIn(ExperimentalLayoutApi::class)
@Composable
private fun LazyItemScope.Suggestions(actions: SearchActions) {
    Column(Modifier.animateItem().padding(horizontal = 4.dp, vertical = 4.dp)) {
        Text(
            stringResource(R.string.try_these),
            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 4.dp, bottom = 8.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stringArrayResource(R.array.suggestions).forEach { suggestion ->
                Chip(suggestion, selected = false) { actions.onQueryChange(suggestion) }
            }
        }
    }
}

@Composable
private fun CardBlock(title: String, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(shape = RoundedCornerShape(22.dp), color = MaterialTheme.colorScheme.surface, shadowElevation = 1.dp, modifier = modifier.fillMaxWidth()) {
        Column(Modifier.padding(top = 12.dp, bottom = 6.dp)) {
            Text(
                title.uppercase(),
                style = MaterialTheme.typography.labelSmall.copy(fontWeight = FontWeight.SemiBold),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 16.dp),
            )
            content()
        }
    }
}

@Composable
private fun AppGrid(apps: List<SearchResult>, actions: SearchActions, icon: IconContent) {
    Column(Modifier.padding(horizontal = 6.dp)) {
        apps.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(16.dp))
                            .clickable(onClickLabel = app.title) { actions.onResultClick(app) }
                            .padding(vertical = 10.dp),
                    ) {
                        icon(app, Modifier.size(50.dp))
                        Text(
                            app.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 6.dp, start = 2.dp, end = 2.dp),
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}
