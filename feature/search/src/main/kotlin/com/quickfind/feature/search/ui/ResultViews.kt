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
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.Build
import androidx.compose.material.icons.filled.Call
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.ShoppingCart
import androidx.compose.material3.FilledTonalIconButton
import androidx.compose.material3.IconButtonDefaults
import androidx.compose.material3.LocalContentColor
import androidx.compose.ui.unit.Dp
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Phone
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
import com.quickfind.core.ui.Aurora
import com.quickfind.core.ui.GlowBorderBox
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
        SourceType.TOOL -> R.string.type_tool
        SourceType.CALENDAR -> R.string.type_calendar
        SourceType.NOTIFICATION -> R.string.type_notification
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
    is ResultAction.Telegram -> stringResource(R.string.action_telegram, action.number)
    is ResultAction.Signal -> stringResource(R.string.action_signal, action.number)
    is ResultAction.Navigate -> stringResource(R.string.action_navigate)
    is ResultAction.SetTimer -> stringResource(R.string.action_start_timer)
    is ResultAction.SetAlarm -> stringResource(R.string.action_set_alarm)
    is ResultAction.OpenCalendarEvent -> stringResource(R.string.action_open_event)
    is ResultAction.OpenNotification -> stringResource(R.string.action_open)
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
    onAction: ((ResultAction) -> Unit)? = null,
) {
    val quick = if (onAction != null) rowQuickActions(result) else emptyList()
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
        if (quick.isNotEmpty() && onAction != null) {
            Spacer(Modifier.width(8.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                quick.forEach { action -> QuickActionButton(action, size = 36.dp) { onAction(action) } }
            }
        }
    }
}

/** Contacts and numbers get one-tap Call / Message / WhatsApp buttons right on the row. */
fun rowQuickActions(result: SearchResult): List<ResultAction> = when (result.type) {
    SourceType.CONTACT, SourceType.PHONE_NUMBER -> allActions(result)
        .filter { it is ResultAction.Call || it is ResultAction.Sms || it is ResultAction.WhatsApp }
        .distinctBy { it::class }
        .take(3)
    else -> emptyList()
}

/** Icon row under the top hit: up to four actions, contact actions first. */
fun topHitQuickActions(result: SearchResult): List<ResultAction> = when (result.type) {
    SourceType.CONTACT, SourceType.PHONE_NUMBER -> allActions(result).distinctBy { it::class }.take(4)
    else -> result.secondaryActions.distinctBy { it::class }.take(4)
}

private fun allActions(result: SearchResult) = listOf(result.primaryAction) + result.secondaryActions

private val WhatsAppGreen = Color(0xFF25D366)
private val TelegramBlue = Color(0xFF2AABEE)
private val SignalBlue = Color(0xFF3A76F0)

/** The glyph for an action. WhatsApp is drawn as a green bubble with a phone, not the trademarked logo. */
@Composable
fun ActionGlyph(action: ResultAction, modifier: Modifier = Modifier, tint: Color = LocalContentColor.current) {
    val description = actionLabel(action)
    when (action) {
        is ResultAction.WhatsApp -> Box(modifier, contentAlignment = Alignment.Center) {
            Icon(painterResource(UiR.drawable.ic_bubble_round), description, Modifier.fillMaxSize(), tint = WhatsAppGreen)
            Icon(Icons.Default.Call, null, Modifier.fillMaxSize(0.5f), tint = Color.White)
        }
        is ResultAction.Telegram -> Box(modifier.clip(CircleShape).background(TelegramBlue), contentAlignment = Alignment.Center) {
            Icon(Icons.AutoMirrored.Filled.Send, description, Modifier.fillMaxSize(0.55f), tint = Color.White)
        }
        is ResultAction.Signal -> Box(modifier, contentAlignment = Alignment.Center) {
            Icon(painterResource(UiR.drawable.ic_bubble_round), description, Modifier.fillMaxSize(), tint = SignalBlue)
        }
        is ResultAction.Navigate -> Icon(Icons.Default.LocationOn, description, modifier, tint)
        is ResultAction.SetTimer -> Icon(Icons.Default.PlayArrow, description, modifier, tint)
        is ResultAction.SetAlarm -> Icon(Icons.Default.Notifications, description, modifier, tint)
        is ResultAction.OpenCalendarEvent -> Icon(Icons.Default.DateRange, description, modifier, tint)
        is ResultAction.OpenNotification -> Icon(painterResource(UiR.drawable.ic_open), description, modifier, tint)
        is ResultAction.Call -> Icon(Icons.Default.Call, description, modifier, tint)
        is ResultAction.Sms -> Icon(painterResource(UiR.drawable.ic_chat), description, modifier, tint)
        is ResultAction.Email -> Icon(Icons.Default.Email, description, modifier, tint)
        is ResultAction.OpenContact -> Icon(Icons.Default.Person, description, modifier, tint)
        is ResultAction.AppDetails -> Icon(Icons.Default.Info, description, modifier, tint)
        is ResultAction.Uninstall -> Icon(Icons.Default.Delete, description, modifier, tint)
        is ResultAction.PlayStore -> Icon(Icons.Default.ShoppingCart, description, modifier, tint)
        is ResultAction.ShareFile -> Icon(Icons.Default.Share, description, modifier, tint)
        is ResultAction.OpenFolder -> Icon(painterResource(UiR.drawable.ic_folder), description, modifier, tint)
        is ResultAction.CopyText -> Icon(painterResource(UiR.drawable.ic_copy), description, modifier, tint)
        is ResultAction.LaunchApp, is ResultAction.OpenFile, is ResultAction.OpenSetting, is ResultAction.OpenUrl ->
            Icon(painterResource(UiR.drawable.ic_open), description, modifier, tint)
    }
}

@Composable
fun QuickActionButton(action: ResultAction, size: Dp = 44.dp, onClick: () -> Unit) {
    FilledTonalIconButton(
        onClick = onClick,
        modifier = Modifier.size(size),
        colors = IconButtonDefaults.filledTonalIconButtonColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.6f),
            contentColor = MaterialTheme.colorScheme.onPrimaryContainer,
        ),
    ) {
        ActionGlyph(action, Modifier.size(size * 0.5f))
    }
}

/** One- or two-word captions for the top-hit icon row. */
@Composable
fun shortActionLabel(action: ResultAction): String = stringResource(
    when (action) {
        is ResultAction.Call -> R.string.short_call
        is ResultAction.Sms -> R.string.short_message
        is ResultAction.WhatsApp -> R.string.short_whatsapp
        is ResultAction.Email -> R.string.short_email
        is ResultAction.Telegram -> R.string.short_telegram
        is ResultAction.Signal -> R.string.short_signal
        is ResultAction.Navigate -> R.string.short_navigate
        is ResultAction.SetTimer -> R.string.short_timer
        is ResultAction.SetAlarm -> R.string.short_alarm
        is ResultAction.OpenCalendarEvent -> R.string.short_event
        is ResultAction.OpenNotification -> R.string.action_open
        is ResultAction.OpenContact -> R.string.short_contact
        is ResultAction.AppDetails -> R.string.short_info
        is ResultAction.Uninstall -> R.string.action_uninstall
        is ResultAction.PlayStore -> R.string.short_play_store
        is ResultAction.ShareFile -> R.string.action_share
        is ResultAction.OpenFolder -> R.string.short_folder
        is ResultAction.CopyText -> R.string.action_copy
        is ResultAction.LaunchApp, is ResultAction.OpenFile, is ResultAction.OpenSetting, is ResultAction.OpenUrl -> R.string.action_open
    },
)

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
    reducedMotion: Boolean = false,
) {
    GlowBorderBox(
        shape = RoundedCornerShape(28.dp),
        animated = !reducedMotion,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 6.dp),
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                // Soft aurora wash behind the top hit.
                .background(
                    Brush.linearGradient(
                        listOf(
                            Aurora.Violet.copy(alpha = 0.18f),
                            Aurora.Magenta.copy(alpha = 0.10f),
                            Aurora.Cyan.copy(alpha = 0.14f),
                        ),
                    ),
                ),
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
                icon(result, Modifier.size(56.dp))
                Spacer(Modifier.width(16.dp))
                Column(Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            painterResource(UiR.drawable.ic_sparkle),
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(14.dp),
                        )
                        Spacer(Modifier.width(4.dp))
                        Text(
                            text = stringResource(R.string.top_hit),
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.SemiBold),
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    Text(
                        text = highlighted(result.title, result.matchedRanges),
                        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.Bold),
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                    )
                    result.subtitle?.let {
                        Text(
                            it,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                        )
                    }
                }
            }
            val quick = topHitQuickActions(result)
            if (quick.isNotEmpty()) {
                Row(
                    Modifier.fillMaxWidth().padding(top = 14.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly,
                ) {
                    quick.forEach { action ->
                        Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.weight(1f)) {
                            QuickActionButton(action, size = 48.dp) { onAction(action) }
                            Text(
                                shortActionLabel(action),
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                modifier = Modifier.padding(top = 4.dp),
                            )
                        }
                    }
                }
            }
        }
        }
    }
}

@Composable
fun GlyphIcon(type: SourceType, modifier: Modifier = Modifier) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(30))
            .background(Brush.linearGradient(glyphColors(type))),
        contentAlignment = Alignment.Center,
    ) {
        val tint = Color.White
        val m = Modifier.size(22.dp)
        when (type) {
            SourceType.CONTACT -> Icon(Icons.Default.Person, null, m, tint)
            SourceType.FILE -> Icon(painterResource(UiR.drawable.ic_file), null, m, tint)
            SourceType.FOLDER -> Icon(painterResource(UiR.drawable.ic_folder), null, m, tint)
            SourceType.SETTING -> Icon(Icons.Default.Settings, null, m, tint)
            SourceType.CALCULATOR -> Text("=", style = MaterialTheme.typography.titleLarge, color = tint)
            SourceType.TOOL -> Icon(Icons.Default.Build, null, m, tint)
            SourceType.CALENDAR -> Icon(Icons.Default.DateRange, null, m, tint)
            SourceType.NOTIFICATION -> Icon(Icons.Default.Notifications, null, m, tint)
            SourceType.PHONE_NUMBER -> Icon(Icons.Default.Phone, null, m, tint)
            SourceType.URL -> Icon(painterResource(UiR.drawable.ic_globe), null, m, tint)
            SourceType.WEB -> Icon(Icons.Default.Search, null, m, tint)
            SourceType.APP -> Icon(Icons.Default.Star, null, m, tint)
        }
    }
}

/** Each kind of result gets its own slice of the aurora. */
private fun glyphColors(type: SourceType): List<Color> = when (type) {
    SourceType.CONTACT, SourceType.PHONE_NUMBER -> listOf(Aurora.Magenta, Aurora.Violet)
    SourceType.FILE, SourceType.FOLDER -> listOf(Aurora.Amber, Aurora.Magenta)
    SourceType.SETTING -> listOf(Aurora.Cyan, Aurora.Violet)
    SourceType.CALENDAR -> listOf(Aurora.Cyan, Aurora.Magenta)
    SourceType.NOTIFICATION -> listOf(Aurora.Amber, Aurora.Violet)
    SourceType.CALCULATOR, SourceType.TOOL -> listOf(Aurora.Violet, Aurora.Cyan)
    SourceType.URL, SourceType.WEB, SourceType.APP -> listOf(Aurora.Violet, Aurora.Magenta)
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
                Modifier.fillMaxSize().clip(CircleShape).background(Brush.linearGradient(listOf(Aurora.Magenta, Aurora.Violet))),
                contentAlignment = Alignment.Center,
            ) {
                if (icon.initials.isNotEmpty()) {
                    Text(icon.initials, color = Color.White, style = MaterialTheme.typography.titleMedium)
                } else {
                    Icon(Icons.Default.Person, typeLabel(result.type), tint = Color.White)
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
                modifier = Modifier.fillMaxSize().clip(RoundedCornerShape(30)),
            )
        }
        is ResultIcon.Glyph -> GlyphIcon(icon.type, modifier)
        is ResultIcon.Symbol -> Box(
            modifier = modifier.clip(RoundedCornerShape(30)).background(Brush.linearGradient(glyphColors(SourceType.TOOL))),
            contentAlignment = Alignment.Center,
        ) {
            Text(icon.text, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold), maxLines = 1)
        }
    }
}
