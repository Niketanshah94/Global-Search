package com.quickfind.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.draggable
import androidx.compose.foundation.gestures.rememberDraggableState
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.AssistChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.InputChip
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.nestedscroll.NestedScrollConnection
import androidx.compose.ui.input.nestedscroll.NestedScrollSource
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Velocity
import androidx.compose.ui.unit.dp
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultSection
import com.quickfind.core.model.SearchResult
import com.quickfind.feature.search.R
import com.quickfind.feature.search.SearchScope
import com.quickfind.feature.search.SearchUiState

/** Everything the search screen can ask its host to do. Keeps [SearchScreen] stateless. */
interface SearchActions {
    fun onQueryChange(text: String)
    fun onSubmit()
    fun onResultClick(result: SearchResult)
    fun onResultLongClick(result: SearchResult)
    fun onAction(result: SearchResult, action: ResultAction)
    fun onToggleSection(section: ResultSection)
    fun onClearScope()
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

@OptIn(ExperimentalComposeUiApi::class)
@Composable
fun SearchScreen(
    state: SearchUiState,
    barAtBottom: Boolean,
    actions: SearchActions,
    focusRequester: FocusRequester,
    icon: IconContent,
) {
    val dismissThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    // Pulling down past the top of the results (or on the bar) closes search (spec 7).
    val pullToDismiss = remember(actions) {
        object : NestedScrollConnection {
            var pulled = 0f
            override fun onPostScroll(consumed: Offset, available: Offset, source: NestedScrollSource): Offset {
                if (source == NestedScrollSource.UserInput && available.y > 0f) {
                    pulled += available.y
                    if (pulled > dismissThreshold) {
                        pulled = 0f
                        actions.onDismiss()
                    }
                } else if (consumed.y != 0f) {
                    pulled = 0f
                }
                return Offset.Zero
            }

            override suspend fun onPreFling(available: Velocity): Velocity {
                pulled = 0f
                return Velocity.Zero
            }
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            // Exposes test tags as resource ids so Macrobenchmark/UiAutomator can find the field.
            .semantics { testTagsAsResourceId = true }
            .background(Color.Black.copy(alpha = 0.32f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actions.onDismiss() },
    ) {
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(8.dp),
            shape = RoundedCornerShape(28.dp),
            tonalElevation = 3.dp,
            shadowElevation = 6.dp,
        ) {
            Column(Modifier.fillMaxSize()) {
                if (!barAtBottom) {
                    SearchField(state, actions, focusRequester, dismissThreshold, pullDownCloses = true)
                    ProgressLine(state.isSearching)
                }
                LazyColumn(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag(SearchTestTags.RESULTS)
                        .then(if (barAtBottom) Modifier else Modifier.nestedScroll(pullToDismiss)),
                    // With the bar at the bottom the top hit sits next to it, under the thumb.
                    reverseLayout = barAtBottom,
                    verticalArrangement = if (barAtBottom) Arrangement.Bottom else Arrangement.Top,
                ) {
                    if (state.hasQuery) results(state, actions, icon) else emptyState(state, actions, icon)
                }
                if (barAtBottom) {
                    ProgressLine(state.isSearching)
                    SearchField(state, actions, focusRequester, dismissThreshold, pullDownCloses = true)
                }
            }
        }
    }
}

@Composable
private fun ProgressLine(visible: Boolean) {
    if (visible) {
        LinearProgressIndicator(Modifier.fillMaxWidth().height(2.dp))
    } else {
        Spacer(Modifier.height(2.dp))
    }
}

@Composable
private fun SearchField(
    state: SearchUiState,
    actions: SearchActions,
    focusRequester: FocusRequester,
    dismissThreshold: Float,
    pullDownCloses: Boolean,
) {
    val scope = state.scope
    val dragged = remember { FloatArray(1) }
    val dragState = rememberDraggableState { delta -> dragged[0] += delta }
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 4.dp, vertical = 4.dp)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                enabled = pullDownCloses,
                onDragStarted = { dragged[0] = 0f },
                onDragStopped = { if (dragged[0] > dismissThreshold) actions.onDismiss() },
            ),
    ) {
        TextField(
            value = state.query,
            onValueChange = actions::onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag(SearchTestTags.FIELD),
            placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
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
            trailingIcon = if (state.query.isNotEmpty()) {
                { IconButton(onClick = { actions.onQueryChange("") }) { Icon(Icons.Default.Clear, stringResource(R.string.clear_query)) } }
            } else {
                null
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Go, autoCorrectEnabled = false),
            keyboardActions = KeyboardActions(onGo = { actions.onSubmit() }),
            colors = TextFieldDefaults.colors(
                focusedContainerColor = Color.Transparent,
                unfocusedContainerColor = Color.Transparent,
                focusedIndicatorColor = Color.Transparent,
                unfocusedIndicatorColor = Color.Transparent,
            ),
        )
        IconButton(onClick = actions::onOpenSettings) {
            Icon(Icons.Default.Settings, contentDescription = stringResource(R.string.open_settings))
        }
    }
}

@Composable
private fun scopeLabel(scope: SearchScope): String = stringResource(
    when (scope) {
        SearchScope.APPS -> R.string.scope_apps
        SearchScope.CONTACTS -> R.string.scope_contacts
        SearchScope.FILES -> R.string.scope_files
    },
)

@Composable
private fun sectionTitle(section: ResultSection): String = stringResource(
    when (section) {
        ResultSection.APPS -> R.string.section_apps
        ResultSection.CONTACTS -> R.string.section_contacts
        ResultSection.FILES -> R.string.section_files
        ResultSection.SETTINGS -> R.string.section_settings
        ResultSection.WEB -> R.string.section_web
    },
)

private fun LazyListScope.results(state: SearchUiState, actions: SearchActions, icon: IconContent) {
    state.topHit?.let { top ->
        item(key = "top:${top.id}", contentType = "top") {
            TopHitCard(
                result = top,
                onClick = { actions.onResultClick(top) },
                onLongClick = { actions.onResultLongClick(top) },
                onAction = { actions.onAction(top, it) },
                icon = icon,
                modifier = Modifier.testTag(SearchTestTags.TOP_HIT),
            )
        }
    }
    for (section in state.sections) {
        item(key = "header:${section.section}", contentType = "header") {
            SectionHeader(sectionTitle(section.section))
        }
        items(section.visible, key = { "${section.section}:${it.id}" }, contentType = { "row" }) { result ->
            ResultRow(
                result = result,
                onClick = { actions.onResultClick(result) },
                onLongClick = { actions.onResultLongClick(result) },
                icon = icon,
            )
        }
        if (section.moreCount > 0 || section.expanded) {
            item(key = "more:${section.section}", contentType = "more") {
                TextButton(onClick = { actions.onToggleSection(section.section) }, modifier = Modifier.padding(start = 8.dp)) {
                    Text(
                        if (section.expanded) {
                            stringResource(R.string.show_less)
                        } else {
                            stringResource(R.string.show_more, section.moreCount)
                        },
                    )
                }
            }
        }
    }
    if (state.noResults) {
        item(key = "none", contentType = "none") {
            Text(
                stringResource(R.string.no_results, state.query),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

private fun LazyListScope.emptyState(state: SearchUiState, actions: SearchActions, icon: IconContent) {
    val empty = state.empty
    if (empty.clipboardAvailable) {
        item(key = "clipboard", contentType = "clipboard") {
            Row(Modifier.padding(horizontal = 12.dp, vertical = 4.dp), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(onClick = actions::onPasteClipboard, label = { Text(stringResource(R.string.paste_clipboard)) })
                IconButton(onClick = actions::onDismissClipboard) {
                    Icon(Icons.Default.Close, stringResource(R.string.dismiss), Modifier.size(18.dp))
                }
            }
        }
    }
    if (empty.pinned.isNotEmpty()) {
        item(key = "pinned-header", contentType = "header") { SectionHeader(stringResource(R.string.section_pinned)) }
        items(empty.pinned, key = { "pinned:${it.id}" }, contentType = { "row" }) { r ->
            ResultRow(r, { actions.onResultClick(r) }, { actions.onResultLongClick(r) }, icon)
        }
    }
    if (empty.recentApps.isNotEmpty()) {
        item(key = "recent-apps-header", contentType = "header") { SectionHeader(stringResource(R.string.section_recent_apps)) }
        item(key = "recent-apps", contentType = "grid") { AppGrid(empty.recentApps, actions, icon) }
    }
    if (empty.recentFiles.isNotEmpty()) {
        item(key = "recent-files-header", contentType = "header") { SectionHeader(stringResource(R.string.section_recent_files)) }
        items(empty.recentFiles, key = { "recent-file:${it.id}" }, contentType = { "row" }) { r ->
            ResultRow(r, { actions.onResultClick(r) }, { actions.onResultLongClick(r) }, icon)
        }
    }
    if (!empty.clipboardAvailable && empty.pinned.isEmpty() && empty.recentApps.isEmpty() && empty.recentFiles.isEmpty()) {
        item(key = "hint", contentType = "hint") {
            Text(
                stringResource(R.string.empty_hint),
                modifier = Modifier.padding(24.dp),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AppGrid(apps: List<SearchResult>, actions: SearchActions, icon: IconContent) {
    Column(Modifier.padding(horizontal = 8.dp)) {
        apps.chunked(4).forEach { row ->
            Row(Modifier.fillMaxWidth()) {
                row.forEach { app ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier
                            .weight(1f)
                            .clickable(onClickLabel = app.title) { actions.onResultClick(app) }
                            .padding(vertical = 8.dp),
                    ) {
                        icon(app, Modifier.size(48.dp))
                        Text(
                            app.title,
                            style = MaterialTheme.typography.labelMedium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(top = 4.dp, start = 2.dp, end = 2.dp),
                        )
                    }
                }
                repeat(4 - row.size) { Spacer(Modifier.weight(1f)) }
            }
        }
    }
}

@Composable
private fun SectionHeader(title: String) {
    Column {
        HorizontalDivider(Modifier.padding(horizontal = 16.dp), color = MaterialTheme.colorScheme.outlineVariant)
        Text(
            title,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(start = 16.dp, top = 12.dp, bottom = 4.dp),
        )
    }
}
