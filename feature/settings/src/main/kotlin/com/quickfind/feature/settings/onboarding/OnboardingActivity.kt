package com.quickfind.feature.settings.onboarding

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Person
import androidx.compose.ui.draw.drawWithCache
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.Dp
import com.quickfind.core.ui.R as UiR
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringArrayResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.ExperimentalTextApi
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.LifecycleResumeEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.quickfind.core.data.settings.SettingsRepository
import com.quickfind.core.data.settings.UserSettings
import com.quickfind.core.indexing.FilePermissions
import com.quickfind.core.indexing.IndexScheduler
import com.quickfind.core.ui.Aurora
import com.quickfind.core.ui.AuroraBackground
import com.quickfind.core.ui.GlowBorderBox
import com.quickfind.core.ui.QuickFindTheme
import com.quickfind.core.ui.rememberReducedMotion
import com.quickfind.feature.settings.R
import com.quickfind.feature.settings.SystemLinks
import com.quickfind.feature.settings.toPreference
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class OnboardingViewModel @Inject constructor(
    private val repository: SettingsRepository,
    private val scheduler: IndexScheduler,
) : ViewModel() {
    val settings: StateFlow<UserSettings> = repository.settings

    fun update(transform: (UserSettings) -> UserSettings) {
        viewModelScope.launch { repository.update(transform) }
    }

    fun enableFiles() {
        viewModelScope.launch {
            repository.update { it.copy(filesEnabled = true) }
            scheduler.requestFullScan()
        }
    }

    fun finish(then: () -> Unit) {
        viewModelScope.launch {
            repository.update { it.copy(onboardingDone = true) }
            then()
        }
    }
}

/**
 * Four short, skippable screens: what it does, how privacy works, permissions (asked here,
 * each with a reason), and how to open search from anywhere.
 */
@AndroidEntryPoint
class OnboardingActivity : ComponentActivity() {
    private val viewModel: OnboardingViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        setContent {
            val s by viewModel.settings.collectAsStateWithLifecycle()
            QuickFindTheme(preference = s.themeMode.toPreference(), dynamicColor = s.dynamicColor) {
                // Back counts as "skip": onboarding should not come back on the next open.
                BackHandler { viewModel.finish(::finish) }
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    OnboardingScreen(viewModel, s, onDone = { viewModel.finish(::finish) })
                }
            }
        }
    }
}

@Composable
private fun OnboardingScreen(viewModel: OnboardingViewModel, s: UserSettings, onDone: () -> Unit) {
    val context = LocalContext.current
    val pager = rememberPagerState { PAGES }
    val scope = rememberCoroutineScope()
    val reducedMotion = rememberReducedMotion()

    var tick by remember { mutableIntStateOf(0) }
    LifecycleResumeEffect(Unit) {
        tick++
        onPauseOrDispose { }
    }
    val contactsGranted = remember(tick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CONTACTS) == PackageManager.PERMISSION_GRANTED
    }
    val calendarGranted = remember(tick) {
        ContextCompat.checkSelfPermission(context, Manifest.permission.READ_CALENDAR) == PackageManager.PERMISSION_GRANTED
    }
    val isAssistant = remember(tick) { SystemLinks.isDefaultAssistant(context) }
    val mediaGranted = remember(tick) { FilePermissions.hasMediaAccess(context) }
    val notificationAccess = remember(tick) { SystemLinks.hasNotificationAccess(context) }

    val contactsLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) viewModel.update { it.copy(contactsEnabled = true) }
    }
    val calendarLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { ok ->
        tick++
        if (ok) viewModel.update { it.copy(calendarEnabled = true) }
    }
    val mediaLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        tick++
        if (result.values.any { it }) viewModel.enableFiles()
    }
    // "Allow & continue": every runtime permission in one system sequence, then turn on
    // exactly the sources that were allowed and move on.
    val allLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { result ->
        tick++
        val contacts = result[Manifest.permission.READ_CONTACTS] == true || contactsGranted
        val calendar = result[Manifest.permission.READ_CALENDAR] == true || calendarGranted
        val media = FilePermissions.mediaPermissions().any { result[it] == true } || FilePermissions.hasMediaAccess(context)
        viewModel.update { it.copy(contactsEnabled = contacts, calendarEnabled = calendar) }
        if (media) viewModel.enableFiles()
        scope.launch { pager.animateScrollToPage(PAGE_ACCESS + 1) }
    }

    Box(Modifier.fillMaxSize()) {
        AuroraBackground(Modifier.fillMaxSize(), animated = !reducedMotion)
        Column(Modifier.fillMaxSize().safeDrawingPadding()) {
            Row(
                Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                PageDots(count = PAGES, current = pager.currentPage)
                TextButton(onClick = onDone) { Text(stringResource(R.string.skip)) }
            }
            HorizontalPager(state = pager, modifier = Modifier.weight(1f)) { page ->
                Column(
                    Modifier.fillMaxSize().verticalScroll(rememberScrollState()).padding(horizontal = 24.dp, vertical = 8.dp),
                    verticalArrangement = Arrangement.spacedBy(18.dp, Alignment.CenterVertically),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    when (page) {
                        0 -> {
                            HeroOrb(rememberVectorPainter(Icons.Default.Search), animated = !reducedMotion)
                            GradientTitle(stringResource(R.string.onboarding_1_title))
                            Body(stringResource(R.string.onboarding_1_text))
                            DemoSearchBar(animated = !reducedMotion)
                            if (SystemLinks.canPinWidget(context)) {
                                OutlinedButton(onClick = { SystemLinks.requestPinWidget(context) }, shape = CircleShape) {
                                    Text(stringResource(R.string.add_widget))
                                }
                            }
                        }
                        PAGE_PRIVACY -> {
                            HeroOrb(painterResource(UiR.drawable.ic_shield), animated = !reducedMotion, size = 112.dp)
                            GradientTitle(stringResource(R.string.privacy_page_title))
                            PrivacyPoint(painterResource(UiR.drawable.ic_smartphone), listOf(Aurora.Violet, Aurora.Cyan), stringResource(R.string.privacy_point_device_title), stringResource(R.string.privacy_point_device_text))
                            PrivacyPoint(painterResource(UiR.drawable.ic_visibility), listOf(Aurora.Cyan, Aurora.Violet), stringResource(R.string.privacy_point_names_title), stringResource(R.string.privacy_point_names_text))
                            PrivacyPoint(painterResource(UiR.drawable.ic_cloud_off), listOf(Aurora.Magenta, Aurora.Violet), stringResource(R.string.privacy_point_nothing_sent_title), stringResource(R.string.privacy_point_nothing_sent_text))
                            PrivacyPoint(painterResource(UiR.drawable.ic_tune), listOf(Aurora.Amber, Aurora.Magenta), stringResource(R.string.privacy_point_control_title), stringResource(R.string.privacy_point_control_text))
                        }
                        PAGE_ACCESS -> {
                            HeroOrb(painterResource(UiR.drawable.ic_key), animated = !reducedMotion, size = 96.dp)
                            GradientTitle(stringResource(R.string.access_page_title))
                            Body(stringResource(R.string.access_page_text))
                            PermissionCard(
                                icon = rememberVectorPainter(Icons.Default.Person),
                                colors = listOf(Aurora.Magenta, Aurora.Violet),
                                title = stringResource(R.string.source_contacts),
                                why = stringResource(R.string.access_contacts_why),
                                granted = contactsGranted && s.contactsEnabled,
                            ) {
                                if (contactsGranted) viewModel.update { it.copy(contactsEnabled = true) } else contactsLauncher.launch(Manifest.permission.READ_CONTACTS)
                            }
                            PermissionCard(
                                icon = painterResource(UiR.drawable.ic_folder),
                                colors = listOf(Aurora.Amber, Aurora.Magenta),
                                title = stringResource(R.string.source_files),
                                why = stringResource(R.string.access_files_why),
                                granted = mediaGranted && s.filesEnabled,
                            ) {
                                if (mediaGranted) viewModel.enableFiles() else mediaLauncher.launch(FilePermissions.mediaPermissions())
                            }
                            PermissionCard(
                                icon = rememberVectorPainter(Icons.Default.DateRange),
                                colors = listOf(Aurora.Cyan, Aurora.Violet),
                                title = stringResource(R.string.source_calendar),
                                why = stringResource(R.string.access_calendar_why),
                                granted = calendarGranted && s.calendarEnabled,
                            ) {
                                if (calendarGranted) viewModel.update { it.copy(calendarEnabled = true) } else calendarLauncher.launch(Manifest.permission.READ_CALENDAR)
                            }
                            PermissionCard(
                                icon = painterResource(UiR.drawable.ic_key),
                                colors = listOf(Aurora.Violet, Aurora.Cyan),
                                title = stringResource(R.string.source_notifications),
                                why = stringResource(R.string.access_notifications_why),
                                granted = notificationAccess && s.notificationsEnabled,
                            ) {
                                viewModel.update { it.copy(notificationsEnabled = true) }
                                (context as? ComponentActivity)?.let(SystemLinks::openNotificationAccess)
                            }
                            Text(
                                stringResource(R.string.access_page_footer),
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant,
                                textAlign = TextAlign.Center,
                            )
                        }
                        else -> {
                            HeroOrb(painterResource(UiR.drawable.ic_sparkle), animated = !reducedMotion, size = 112.dp)
                            GradientTitle(stringResource(R.string.onboarding_3_title))
                            Body(stringResource(R.string.onboarding_3_text))
                            EntryCard(
                                icon = painterResource(UiR.drawable.ic_sparkle),
                                colors = listOf(Aurora.Violet, Aurora.Magenta),
                                title = stringResource(if (isAssistant) R.string.set_assistant_done else R.string.set_assistant),
                                text = stringResource(R.string.set_assistant_summary),
                                enabled = !isAssistant,
                                onClick = { (context as? ComponentActivity)?.let(SystemLinks::openAssistantSettings) },
                            )
                            EntryCard(painterResource(UiR.drawable.ic_widgets), listOf(Aurora.Cyan, Aurora.Violet), stringResource(R.string.add_widget), stringResource(R.string.add_widget_summary), SystemLinks.canPinWidget(context)) {
                                SystemLinks.requestPinWidget(context)
                            }
                            EntryCard(painterResource(UiR.drawable.ic_toggle), listOf(Aurora.Amber, Aurora.Magenta), stringResource(R.string.onboarding_tile_title), stringResource(R.string.onboarding_tile_text), enabled = false) {}
                        }
                    }
                }
            }
            val page = pager.currentPage
            val last = page == PAGES - 1
            val needsAsk = page == PAGE_ACCESS && !(contactsGranted && mediaGranted && calendarGranted)
            GlowButton(
                text = stringResource(
                    when {
                        last -> R.string.onboarding_start
                        needsAsk -> R.string.access_allow_continue
                        else -> R.string.next
                    },
                ),
                animated = !reducedMotion,
                modifier = Modifier.fillMaxWidth().padding(start = 24.dp, end = 24.dp, top = 12.dp),
            ) {
                when {
                    last -> onDone()
                    needsAsk -> allLauncher.launch(
                        (listOf(Manifest.permission.READ_CONTACTS, Manifest.permission.READ_CALENDAR) + FilePermissions.mediaPermissions())
                            .toTypedArray(),
                    )
                    else -> scope.launch { pager.animateScrollToPage(page + 1) }
                }
            }
            // Always a way past the permission step without granting anything.
            Box(Modifier.fillMaxWidth().padding(bottom = 8.dp), contentAlignment = Alignment.Center) {
                if (needsAsk) {
                    TextButton(onClick = { scope.launch { pager.animateScrollToPage(page + 1) } }) {
                        Text(stringResource(R.string.not_now))
                    }
                } else {
                    Spacer(Modifier.height(48.dp))
                }
            }
        }
    }
}

/** Active page is a gradient pill, others are dots; width animates between them. */
@Composable
private fun PageDots(count: Int, current: Int) {
    Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
        repeat(count) { i ->
            val width by animateDpAsState(if (i == current) 24.dp else 8.dp, label = "dot")
            Box(
                Modifier
                    .size(width = width, height = 8.dp)
                    .clip(CircleShape)
                    .then(
                        if (i == current) {
                            Modifier.background(Aurora.horizontal())
                        } else {
                            Modifier.background(MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f))
                        },
                    ),
            )
        }
    }
}

/** The hero: a glowing ring with a breathing gradient icon inside. */
@Composable
private fun HeroOrb(icon: Painter, animated: Boolean, size: Dp = 150.dp) {
    val scale = if (animated) {
        val t = rememberInfiniteTransition(label = "orb")
        t.animateFloat(
            initialValue = 0.95f,
            targetValue = 1.04f,
            animationSpec = infiniteRepeatable(tween(1_800), RepeatMode.Reverse),
            label = "orb-scale",
        ).value
    } else {
        1f
    }
    GlowBorderBox(
        shape = CircleShape,
        borderWidth = 4.dp,
        animated = animated,
        containerColor = MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
        modifier = Modifier.size(size).scale(scale),
    ) {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
            // Paint the icon with the aurora gradient instead of a flat tint.
            Icon(
                painter = icon,
                contentDescription = null,
                tint = Color.Unspecified,
                modifier = Modifier
                    .size(size * 0.42f)
                    .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                    .drawWithCache {
                        val brush = Brush.linearGradient(listOf(Aurora.Violet, Aurora.Magenta, Aurora.Cyan))
                        onDrawWithContent {
                            drawContent()
                            drawRect(brush, blendMode = BlendMode.SrcIn)
                        }
                    },
            )
        }
    }
}

/** A fake search bar that types example queries and shows the answer, to teach by showing. */
@Composable
private fun DemoSearchBar(animated: Boolean) {
    val examples = stringArrayResource(R.array.onboarding_demo)
    var shown by remember { mutableStateOf(if (animated) "" else examples.first().substringBefore('|')) }
    var answer by remember { mutableStateOf(if (animated) "" else examples.first().substringAfter('|')) }
    if (animated) {
        LaunchedEffect(Unit) {
            var i = 0
            while (true) {
                val (query, result) = examples[i % examples.size].split('|', limit = 2)
                answer = ""
                for (n in 1..query.length) {
                    shown = query.take(n)
                    delay(85)
                }
                delay(250)
                answer = result
                delay(1_600)
                i++
            }
        }
    }
    GlowBorderBox(
        shape = CircleShape,
        animated = animated,
        containerColor = MaterialTheme.colorScheme.surface,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Default.Search, null, tint = MaterialTheme.colorScheme.primary)
            Spacer(Modifier.width(12.dp))
            Text(shown + if (animated) "▏" else "", style = MaterialTheme.typography.titleMedium, modifier = Modifier.weight(1f), maxLines = 1)
            AnimatedVisibility(answer.isNotEmpty(), enter = fadeIn(), exit = fadeOut()) {
                Text(
                    "→ $answer",
                    style = MaterialTheme.typography.titleSmall.copy(fontWeight = FontWeight.SemiBold),
                    color = MaterialTheme.colorScheme.secondary,
                    maxLines = 1,
                )
            }
        }
    }
}

@Composable
private fun SourceCard(icon: Painter, colors: List<Color>, title: String, text: String, checked: Boolean, onChange: (Boolean) -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier
            .fillMaxWidth()
            .toggleable(value = checked, role = Role.Switch, onValueChange = onChange),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, colors)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            Switch(checked = checked, onCheckedChange = null)
        }
    }
}

@Composable
private fun EntryCard(icon: Painter, colors: List<Color>, title: String, text: String, enabled: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth().clickable(enabled = enabled, onClick = onClick),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, colors)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(text, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

/** A white icon on a soft gradient squircle, with a faint glow behind it. */
@Composable
private fun IconTile(icon: Painter, colors: List<Color>) {
    Box(
        Modifier
            .size(48.dp)
            .shadow(10.dp, RoundedCornerShape(30), ambientColor = colors.first(), spotColor = colors.first())
            .clip(RoundedCornerShape(30))
            .background(Brush.linearGradient(colors)),
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Color.White, modifier = Modifier.size(24.dp))
    }
}

/** Primary button: gradient fill inside the animated glow ring. */
@Composable
private fun GlowButton(text: String, animated: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    GlowBorderBox(shape = CircleShape, animated = animated, containerColor = Color.Transparent, modifier = modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .background(Aurora.horizontal())
                .clickable(role = Role.Button, onClick = onClick)
                .padding(vertical = 16.dp),
            contentAlignment = Alignment.Center,
        ) {
            Text(text, color = Color.White, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold))
        }
    }
}

@OptIn(ExperimentalTextApi::class)
@Composable
private fun GradientTitle(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.headlineLarge.copy(brush = Aurora.horizontal(), fontWeight = FontWeight.ExtraBold),
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun Body(text: String) {
    Text(
        text,
        style = MaterialTheme.typography.bodyLarge,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
    )
}

private const val PAGES = 4
private const val PAGE_PRIVACY = 1
private const val PAGE_ACCESS = 2

@Composable
private fun PrivacyPoint(icon: Painter, colors: List<Color>, title: String, text: String) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Top) {
        IconTile(icon, colors)
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
            Text(text, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

/** One permission: what it is for, and an Allow button that turns into "Allowed ✓". */
@Composable
private fun PermissionCard(icon: Painter, colors: List<Color>, title: String, why: String, granted: Boolean, onAllow: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(24.dp),
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        contentColor = MaterialTheme.colorScheme.onSurface,
        border = BorderStroke(1.dp, Color.White.copy(alpha = 0.1f)),
        modifier = Modifier.fillMaxWidth(),
    ) {
        Row(Modifier.padding(16.dp), verticalAlignment = Alignment.CenterVertically) {
            IconTile(icon, colors)
            Spacer(Modifier.width(14.dp))
            Column(Modifier.weight(1f)) {
                Text(title, style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.SemiBold))
                Text(why, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Spacer(Modifier.width(8.dp))
            if (granted) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.tertiary,
                        modifier = Modifier.size(20.dp),
                    )
                    Spacer(Modifier.width(4.dp))
                    Text(
                        stringResource(R.string.access_allowed),
                        style = MaterialTheme.typography.labelLarge.copy(fontWeight = FontWeight.SemiBold),
                        color = MaterialTheme.colorScheme.tertiary,
                    )
                }
            } else {
                FilledTonalButton(onClick = onAllow, shape = CircleShape) { Text(stringResource(R.string.access_allow)) }
            }
        }
    }
}
