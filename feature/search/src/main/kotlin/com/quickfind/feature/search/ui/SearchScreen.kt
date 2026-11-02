package com.quickfind.feature.search.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyItemScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.SuggestionChip
import androidx.compose.material3.SuggestionChipDefaults
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.quickfind.core.ui.Aurora
import com.quickfind.core.ui.AuroraBackground
import com.quickfind.core.ui.GlowBorderBox
import com.quickfind.core.ui.rememberReducedMotion
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
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.foundation.lazy.LazyListScope
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
    /** Read straight from snapshot state so the field never lags the keyboard. */
    queryText: String,
    state: SearchUiState,
    barAtBottom: Boolean,
    actions: SearchActions,
    focusRequester: FocusRequester,
    icon: IconContent,
) {
    val dismissThreshold = with(LocalDensity.current) { 96.dp.toPx() }
    val reducedMotion = rememberReducedMotion()
    // Keyed items make LazyColumn keep the old first item in view when results change, which
    // pushed the new top hit off screen. Always show the best result first.
    val listState = rememberLazyListState()
    LaunchedEffect(state.query, state.topHit?.id) { listState.scrollToItem(0) }
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
            .background(Color.Black.copy(alpha = 0.45f))
            .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) { actions.onDismiss() },
    ) {
        // Static aurora: a full-screen animated gradient costs frames while typing on mid-range
        // phones. The glowing search ring carries the motion instead.
        AuroraBackground(Modifier.fillMaxSize(), animated = false)
        Surface(
            modifier = Modifier
                .fillMaxSize()
                .systemBarsPadding()
                .imePadding()
                .padding(10.dp),
            shape = RoundedCornerShape(32.dp),
            // Frosted glass: the aurora shows through the panel.
            color = MaterialTheme.colorScheme.surface.copy(alpha = 0.86f),
            contentColor = MaterialTheme.colorScheme.onSurface,
            border = BorderStroke(1.dp, Color.White.copy(alpha = 0.12f)),
        ) {
            Column(Modifier.fillMaxSize()) {
                if (!barAtBottom) {
                    SearchField(queryText, state, actions, focusRequester, dismissThreshold, pullDownCloses = true, reducedMotion = reducedMotion)
                }
                LazyColumn(
                    state = listState,
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxWidth()
                        .testTag(SearchTestTags.RESULTS)
                        .then(if (barAtBottom) Modifier else Modifier.nestedScroll(pullToDismiss)),
                    // With the bar at the bottom the top hit sits next to it, under the thumb.
                    reverseLayout = barAtBottom,
                    verticalArrangement = if (barAtBottom) Arrangement.Bottom else Arrangement.Top,
                ) {
                    if (state.hasQuery) results(state, actions, icon, reducedMotion) else emptyState(state, actions, icon)
                }
                if (barAtBottom) {
                    SearchField(queryText, state, actions, focusRequester, dismissThreshold, pullDownCloses = true, reducedMotion = reducedMotion)
                }
            }
        }
    }
}

@Composable
private fun SearchField(
    queryText: String,
    state: SearchUiState,
    actions: SearchActions,
    focusRequester: FocusRequester,
    dismissThreshold: Float,
    pullDownCloses: Boolean,
    reducedMotion: Boolean,
) {
    val scope = state.scope
    val dragged = remember { FloatArray(1) }
    val dragState = rememberDraggableState { delta -> dragged[0] += delta }
    GlowBorderBox(
        shape = CircleShape,
        intense = state.isSearching,
        animated = !reducedMotion,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 12.dp, vertical = 12.dp),
    ) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier
            .fillMaxWidth()
            .padding(end = 4.dp)
            .draggable(
                state = dragState,
                orientation = Orientation.Vertical,
                enabled = pullDownCloses,
                onDragStarted = { dragged[0] = 0f },
                onDragStopped = { if (dragged[0] > dismissThreshold) actions.onDismiss() },
            ),
    ) {
        TextField(
            value = queryText,
            onValueChange = actions::onQueryChange,
            modifier = Modifier
                .weight(1f)
                .focusRequester(focusRequester)
                .testTag(SearchTestTags.FIELD),
            placeholder = { Text(stringResource(R.string.search_hint), maxLines = 1, overflow = TextOverflow.Ellipsis) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = MaterialTheme.colorScheme.primary) },
            textStyle = MaterialTheme.typography.titleMedium,
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
        ResultSection.FILES -> R.string.section_files
        ResultSection.SETTINGS -> R.string.section_settings
        ResultSection.WEB -> R.string.section_web
    },
)

private fun LazyListScope.results(state: SearchUiState, actions: SearchActions, icon: IconContent, reducedMotion: Boolean) {
    state.topHit?.let { top ->
        item(key = "top:${top.id}", contentType = "top") {
            TopHitCard(
                result = top,
                onClick = { actions.onResultClick(top) },
                onLongClick = { actions.onResultLongClick(top) },
                onAction = { actions.onAction(top, it) },
                icon = icon,
                reducedMotion = reducedMotion,
                modifier = Modifier.animateItem().testTag(SearchTestTags.TOP_HIT),
            )
        }
    }
    for (section in state.sections) {
        item(key = "header:${section.section}", contentType = "header") {
            SectionHeader(sectionTitle(section.section), Modifier.animateItem())
        }
        // Each section is one frosted card; rows inside are separated by thin dividers.
        item(key = "group:${section.section}", contentType = "group") {
            GlassGroup(Modifier.animateItem()) {
                section.visible.forEachIndexed { index, result ->
                    if (index > 0) RowDivider()
                    ResultRow(
                        result = result,
                        onClick = { actions.onResultClick(result) },
                        onLongClick = { actions.onResultLongClick(result) },
                        icon = icon,
                        onAction = { actions.onAction(result, it) },
                    )
                }
                if (section.moreCount > 0 || section.expanded) {
                    RowDivider()
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
    item(key = "bottom-space", contentType = "space") { Spacer(Modifier.height(12.dp)) }
}

private fun LazyListScope.emptyState(state: SearchUiState, actions: SearchActions, icon: IconContent) {
    val empty = state.empty
    item(key = "greeting", contentType = "greeting") { Greeting(actions) }
    if (empty.clipboardAvailable) {
        item(key = "clipboard", contentType = "clipboard") {
            Row(Modifier.padding(horizontal = 16.dp, vertical = 4.dp).animateItem(), verticalAlignment = Alignment.CenterVertically) {
                AssistChip(
                    onClick = actions::onPasteClipboard,
                    label = { Text(stringResource(R.string.paste_clipboard)) },
                    leadingIcon = { Text("✦", color = MaterialTheme.colorScheme.secondary) },
                    shape = CircleShape,
                )
                IconButton(onClick = actions::onDismissClipboard) {
                    Icon(Icons.Default.Close, stringResource(R.string.dismiss), Modifier.size(18.dp))
                }
            }
        }
    }
    if (empty.recentApps.isNotEmpty()) {
        item(key = "recent-apps-header", contentType = "header") { SectionHeader(stringResource(R.string.section_recent_apps), Modifier.animateItem()) }
        item(key = "recent-apps", contentType = "grid") { AppGrid(empty.recentApps, actions, icon, Modifier.animateItem()) }
    }
    if (empty.pinned.isNotEmpty()) {
        item(key = "pinned-header", contentType = "header") { SectionHeader(stringResource(R.string.section_pinned), Modifier.animateItem()) }
        item(key = "pinned", contentType = "group") {
            GlassGroup(Modifier.animateItem()) {
                empty.pinned.forEachIndexed { i, r ->
                    if (i > 0) RowDivider()
                    ResultRow(r, { actions.onResultClick(r) }, { actions.onResultLongClick(r) }, icon, onAction = { actions.onAction(r, it) })
                }
            }
        }
    }
    if (empty.recentFiles.isNotEmpty()) {
        item(key = "recent-files-header", contentType = "header") { SectionHeader(stringResource(R.string.section_recent_files), Modifier.animateItem()) }
        item(key = "recent-files", contentType = "group") {
            GlassGroup(Modifier.animateItem()) {
                empty.recentFiles.forEachIndexed { i, r ->
                    if (i > 0) RowDivider()
                    ResultRow(r, { actions.onResultClick(r) }, { actions.onResultLongClick(r) }, icon, onAction = { actions.onAction(r, it) })
                }
            }
        }
    }
}

/** Big gradient greeting plus one-tap suggestion chips that show off what search can do. */
@OptIn(ExperimentalTextApi::class, ExperimentalLayoutApi::class)
@Composable
private fun LazyItemScope.Greeting(actions: SearchActions) {
    Column(Modifier.padding(start = 20.dp, end = 20.dp, top = 12.dp, bottom = 4.dp).animateItem()) {
        Text(
            stringResource(R.string.greeting),
            style = MaterialTheme.typography.headlineMedium.copy(
                brush = Aurora.horizontal(),
                fontWeight = FontWeight.Bold,
            ),
        )
        Text(
            stringResource(R.string.greeting_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 12.dp),
        )
        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            stringArrayResource(R.array.suggestions).forEach { suggestion ->
                SuggestionChip(
                    onClick = { actions.onQueryChange(suggestion) },
                    label = { Text(suggestion) },
                    shape = CircleShape,
                    colors = SuggestionChipDefaults.suggestionChipColors(
                        containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f),
                        labelColor = MaterialTheme.colorScheme.onPrimaryContainer,
                    ),
                    border = null,
                )
            }
        }
    }
}

@Composable
private fun AppGrid(apps: List<SearchResult>, actions: SearchActions, icon: IconContent, modifier: Modifier = Modifier) {
    GlassGroup(modifier) {
        Column(Modifier.padding(vertical = 6.dp)) {
            apps.chunked(4).forEach { row ->
                Row(Modifier.fillMaxWidth()) {
                    row.forEach { app ->
                        Column(
                            horizontalAlignment = Alignment.CenterHorizontally,
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(18.dp))
                                .clickable(onClickLabel = app.title) { actions.onResultClick(app) }
                                .padding(vertical = 10.dp),
                        ) {
                            icon(app, Modifier.size(52.dp))
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
}

/** A frosted card that groups rows, iOS-settings style but with the aurora tint. */
@Composable
private fun GlassGroup(modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 4.dp),
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.55f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.08f)),
    ) {
        Column(Modifier.padding(vertical = 4.dp)) { content() }
    }
}

@Composable
private fun RowDivider() {
    HorizontalDivider(
        Modifier.padding(start = 70.dp, end = 16.dp),
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f),
    )
}

@Composable
private fun SectionHeader(title: String, modifier: Modifier = Modifier) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier.padding(start = 24.dp, top = 16.dp, bottom = 6.dp),
    ) {
        Box(Modifier.size(8.dp).clip(CircleShape).background(Aurora.horizontal()))
        Spacer(Modifier.width(8.dp))
        Text(
            title.uppercase(),
            style = MaterialTheme.typography.labelMedium.copy(letterSpacing = 1.4.sp, fontWeight = FontWeight.SemiBold),
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
