package com.quickfind.feature.search.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.SearchResult
import com.quickfind.feature.search.R

/** Long-press sheet: secondary actions, plus "Pin" and "Hide from results" (spec 7). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ResultActionSheet(
    result: SearchResult,
    isPinned: Boolean,
    showPinAndHide: Boolean,
    onAction: (ResultAction) -> Unit,
    onTogglePin: () -> Unit,
    onHide: () -> Unit,
    onDismiss: () -> Unit,
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(Modifier.navigationBarsPadding().padding(bottom = 8.dp)) {
            Text(
                result.title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 8.dp),
            )
            (listOf(result.primaryAction) + result.secondaryActions).distinct().forEach { action ->
                ListItem(
                    headlineContent = { Text(actionLabel(action)) },
                    modifier = Modifier.clickable { onAction(action) },
                )
            }
            if (showPinAndHide) {
                HorizontalDivider()
                ListItem(
                    headlineContent = { Text(stringResource(if (isPinned) R.string.action_unpin else R.string.action_pin)) },
                    modifier = Modifier.clickable(onClick = onTogglePin),
                )
                ListItem(
                    headlineContent = { Text(stringResource(R.string.action_hide)) },
                    supportingContent = { Text(stringResource(R.string.action_hide_hint)) },
                    modifier = Modifier.clickable(onClick = onHide),
                )
            }
        }
    }
}
