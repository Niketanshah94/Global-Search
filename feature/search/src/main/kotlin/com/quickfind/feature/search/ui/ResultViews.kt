package com.quickfind.feature.search.ui

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.AssistChip
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import com.quickfind.core.model.ResultAction
import com.quickfind.core.model.ResultIcon
import com.quickfind.core.model.SearchResult
import com.quickfind.core.model.SourceType
import com.quickfind.core.sources.apps.AppIconLoader
import com.quickfind.feature.search.R
import com.quickfind.core.ui.R as UiR

/** Bold, primary-colored matched letters (spec 7: "matched letters highlighted"). */
@Composable
fun highlighted(text: String, ranges: List<IntRange>): AnnotatedString {
    val style = SpanStyle(fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
    return buildAnnotatedString {
        append(text)
        for (r in ranges) {
            if (r.first >= 0 && r.last < text.length && r.first <= r.last) addStyle(style, r.first, r.last + 1)
        }
    }
}

@Composable
fun typeLabel(type: SourceType): String = stringResource(
    when (type) {
        SourceType.APP -> R.string.type_app
        SourceType.CONTACT -> R.string.type_contact
        SourceType.FILE -> R.string.type_file
        SourceType.FOLDER -> R.string.type_folder
        SourceType.SETTING -> R.string.type_setting
        SourceType.CALCULATOR -> R.string.type_calculator
        SourceType.PHONE_NUMBER -> R.string.type_phone
        SourceType.URL -> R.string.type_url
        SourceType.WEB -> R.string.type_web
    },
)

@Composable
fun actionLabel(action: ResultAction): String = when (action) {
    is ResultAction.LaunchApp -> stringResource(R.string.action_open)
    is ResultAction.AppDetails -> stringResource(R.string.action_app_info)
    is ResultAction.Uninstall -> stringResource(R.string.action_uninstall)
    is ResultAction.PlayStore -> stringResource(R.string.action_play_store)
    is ResultAction.Call -> stringResource(R.string.action_call, action.number)
    is ResultAction.Sms -> stringResource(R.string.action_sms, action.number)
    is ResultAction.WhatsApp -> stringResource(R.string.action_whatsapp, action.number)
    is ResultAction.Email -> stringResource(R.string.action_email, action.address)
    is ResultAction.OpenContact -> stringResource(R.string.action_open_contact)
    is ResultAction.OpenFile -> stringResource(R.string.action_open)
    is ResultAction.ShareFile -> stringResource(R.string.action_share)
    is ResultAction.OpenFolder -> stringResource(R.string.action_open_folder, action.title)
    is ResultAction.OpenSetting -> stringResource(R.string.action_open)
    is ResultAction.CopyText -> stringResource(R.string.action_copy)
    is ResultAction.OpenUrl -> stringResource(R.string.action_open)
}

/** Icon slot used by result rows; lets tests render rows without an [AppIconLoader]. */
typealias IconContent = @Composable (SearchResult, Modifier) -> Unit

@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ResultRow(
    result: SearchResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    icon: IconContent,
    modifier: Modifier = Modifier,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .combinedClickable(
                onClickLabel = actionLabel(result.primaryAction),
                onLongClickLabel = stringResource(R.string.more_actions),
                onClick = onClick,
                onLongClick = onLongClick,
            )
            .semantics(mergeDescendants = true) {}
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        icon(result, Modifier.size(40.dp))
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(
                text = highlighted(result.title, result.matchedRanges),
                style = MaterialTheme.typography.bodyLarge,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
            )
            result.subtitle?.let {
                Text(
                    text = it,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
        if (result.isWorkProfile) {
            Icon(
                painter = painterResource(UiR.drawable.ic_work),
                contentDescription = stringResource(R.string.work_profile),
                modifier = Modifier.size(16.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

/** The single best result; Enter opens it (spec 4.7). */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun TopHitCard(
    result: SearchResult,
    onClick: () -> Unit,
    onLongClick: () -> Unit,
    onAction: (ResultAction) -> Unit,
    icon: IconContent,
    modifier: Modifier = Modifier,
) {
    Card(
        modifier = modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 4.dp),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.secondaryContainer),
    ) {
        Column(
            Modifier
                .combinedClickable(
                    onClickLabel = actionLabel(result.primaryAction),
                    onLongClickLabel = stringResource(R.string.more_actions),
                    onClick = onClick,
                    onLongClick = onLongClick,
                )
                .padding(16.dp),
        ) {
            Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.semantics(mergeDescendants = true) {}) {
                icon(result, Modifier.size(48.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        text = stringResource(R.string.top_hit),
                        style = MaterialTheme.typography.labelSmall,
                        color = MaterialTheme.colorScheme.onSecondaryContainer,
                    )
                    Text(
                        text = highlighted(result.title, result.matchedRanges),
                        style = MaterialTheme.typography.titleLarge,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    result.subtitle?.let {
                        Text(it, style = MaterialTheme.typography.bodyMedium, maxLines = 2, overflow = TextOverflow.Ellipsis)
                    }
                }
            }
            val quick = result.secondaryActions.take(3)
            if (quick.isNotEmpty()) {
                Row(Modifier.padding(top = 8.dp), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    quick.forEach { action ->
                        AssistChip(onClick = { onAction(action) }, label = { Text(actionLabel(action), maxLines = 1, overflow = TextOverflow.Ellipsis) })
                    }
                }
            }
        }
    }
}

@Composable
fun GlyphIcon(type: SourceType, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier.clip(CircleShape).background(MaterialTheme.colorScheme.secondaryContainer),
        contentAlignment = Alignment.Center,
    ) {
        val tint = MaterialTheme.colorScheme.onSecondaryContainer
        val m = Modifier.size(22.dp)
        when (type) {
            SourceType.CONTACT -> Icon(Icons.Default.Person, null, m, tint)
            SourceType.FILE -> Icon(painterResource(UiR.drawable.ic_file), null, m, tint)
            SourceType.FOLDER -> Icon(painterResource(UiR.drawable.ic_folder), null, m, tint)
            SourceType.SETTING -> Icon(Icons.Default.Settings, null, m, tint)
            SourceType.CALCULATOR -> Text("=", style = MaterialTheme.typography.titleLarge, color = tint)
            SourceType.PHONE_NUMBER -> Icon(Icons.Default.Phone, null, m, tint)
            SourceType.URL -> Icon(painterResource(UiR.drawable.ic_globe), null, m, tint)
            SourceType.WEB -> Icon(Icons.Default.Search, null, m, tint)
            SourceType.APP -> Icon(Icons.Default.Star, null, m, tint)
        }
    }
}

/** Real icons: app icons from the two-level cache, contact photos and thumbnails via Coil. */
@Composable
fun ResultIconView(result: SearchResult, loader: AppIconLoader, modifier: Modifier) {
    when (val icon = result.icon) {
        is ResultIcon.App -> {
            val bitmap by produceState(initialValue = loader.cached(icon), icon) {
                if (value == null) value = loader.load(icon)
            }
            val b = bitmap
            if (b != null) {
                Image(b.asImageBitmap(), contentDescription = typeLabel(result.type), modifier = modifier)
            } else {
                Box(modifier.clip(CircleShape).background(MaterialTheme.colorScheme.surfaceVariant))
            }
        }
        is ResultIcon.ContactPhoto -> Box(modifier, contentAlignment = Alignment.Center) {
            Box(
                Modifier.fillMaxSize().clip(CircleShape).background(MaterialTheme.colorScheme.tertiaryContainer),
                contentAlignment = Alignment.Center,
            ) {
                if (icon.initials.isNotEmpty()) {
                    Text(icon.initials, color = MaterialTheme.colorScheme.onTertiaryContainer, style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Default.Person, typeLabel(result.type), tint = MaterialTheme.colorScheme.onTertiaryContainer)
                }
            }
            if (icon.photoUri != null) {
                AsyncImage(
                    model = icon.photoUri,
                    contentDescription = null,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().clip(CircleShape),
                )
            }
        }
        is ResultIcon.FileThumbnail -> Box(modifier) {
            GlyphIcon(SourceType.FILE, Modifier.fillMaxSize())
            AsyncImage(
                model = icon.uri,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(8.dp)),
            )
        }
        is ResultIcon.Glyph -> GlyphIcon(icon.type, modifier)
    }
}
