package com.quickfind.feature.settings.billing

import android.app.Activity
import android.content.Intent
import android.content.pm.ApplicationInfo
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import com.quickfind.core.billing.BillingAvailability
import com.quickfind.core.billing.BillingRepository
import com.quickfind.core.billing.PlanCatalog
import com.quickfind.core.billing.PlanOption
import com.quickfind.core.billing.PlanPeriod
import com.quickfind.core.billing.PurchaseEvent
import com.quickfind.core.data.Destinations
import com.quickfind.core.data.billing.Entitlement
import com.quickfind.core.data.billing.EntitlementRepository
import com.quickfind.core.model.ProFeature
import com.quickfind.core.ui.Brand
import com.quickfind.core.ui.BrandButton
import com.quickfind.core.ui.Pal
import com.quickfind.core.ui.PalMood
import com.quickfind.core.ui.QuickFindTheme
import com.quickfind.core.ui.ThemePreference
import com.quickfind.core.ui.Wordmark
import com.quickfind.feature.settings.R
import dagger.hilt.android.AndroidEntryPoint
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.NumberFormat
import java.util.Currency
import javax.inject.Inject

@HiltViewModel
class PaywallViewModel @Inject constructor(
    private val billing: BillingRepository,
    private val entitlements: EntitlementRepository,
) : ViewModel() {
    val plans: StateFlow<List<PlanOption>> = billing.plans
    val availability: StateFlow<BillingAvailability> = billing.availability
    val entitlement: StateFlow<Entitlement> = entitlements.entitlement

    private val _message = MutableStateFlow<Int?>(null)
    val message: StateFlow<Int?> = _message.asStateFlow()

    val manageUrl: String get() = billing.manageSubscriptionUrl

    init {
        billing.refresh()
        viewModelScope.launch {
            billing.events.collect { event ->
                _message.value = when (event) {
                    PurchaseEvent.Pending -> R.string.pay_pending
                    is PurchaseEvent.Failed -> R.string.pay_failed
                    PurchaseEvent.Subscribed, PurchaseEvent.Cancelled -> null
                }
            }
        }
    }

    fun buy(activity: Activity, plan: PlanOption) {
        _message.value = null
        if (!billing.purchase(activity, plan)) _message.value = R.string.pay_failed
    }

    fun restore() {
        _message.value = R.string.restore_checking
        billing.refresh()
    }

    fun setTestUnlock(on: Boolean) {
        viewModelScope.launch { entitlements.setTestUnlock(on) }
    }
}

/**
 * The FindPal Pro paywall: opened from a locked result or action (which [ProFeature]), from
 * Settings, and once at the end of onboarding. Always on the dark brand look.
 */
@AndroidEntryPoint
class PaywallActivity : ComponentActivity() {
    private val viewModel: PaywallViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        val feature = intent.getStringExtra(Destinations.EXTRA_PRO_FEATURE)
            ?.let { name -> ProFeature.entries.firstOrNull { it.name == name } } ?: ProFeature.GENERAL
        val fromOnboarding = intent.getBooleanExtra(Destinations.EXTRA_FROM_ONBOARDING, false)
        val testBuild = (applicationInfo.flags and ApplicationInfo.FLAG_DEBUGGABLE) != 0
        setContent {
            QuickFindTheme(preference = ThemePreference.DARK, dynamicColor = false) {
                Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                    PaywallScreen(viewModel, feature, fromOnboarding, testBuild, onClose = ::finish)
                }
            }
        }
    }
}

@Composable
private fun PaywallScreen(
    viewModel: PaywallViewModel,
    feature: ProFeature,
    fromOnboarding: Boolean,
    testBuild: Boolean,
    onClose: () -> Unit,
) {
    val context = LocalContext.current
    val plans by viewModel.plans.collectAsStateWithLifecycle()
    val availability by viewModel.availability.collectAsStateWithLifecycle()
    val entitlement by viewModel.entitlement.collectAsStateWithLifecycle()
    val message by viewModel.message.collectAsStateWithLifecycle()
    var selectedId by rememberSaveable { mutableStateOf<String?>(null) }
    val selected = plans.firstOrNull { it.basePlanId == selectedId } ?: plans.firstOrNull()
    val savings = PlanCatalog.yearlySavingsPercent(
        plans.firstOrNull { it.period == PlanPeriod.MONTHLY },
        plans.firstOrNull { it.period == PlanPeriod.YEARLY },
    )

    Column(
        Modifier
            .fillMaxSize()
            .systemBarsPadding()
            .verticalScroll(rememberScrollState())
            .padding(horizontal = 22.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Box(Modifier.fillMaxWidth()) {
            IconButton(onClick = onClose, modifier = Modifier.align(Alignment.TopEnd)) {
                Icon(Icons.Default.Close, contentDescription = stringResource(R.string.close))
            }
        }
        if (entitlement.isPro) {
            ProActive(entitlement, onManage = { open(context, viewModel.manageUrl) }, onDone = onClose)
        } else {

        Pal(PalMood.PRO, 116.dp)
        Row(verticalAlignment = Alignment.CenterVertically) {
            Wordmark(fontSize = 34.sp)
            Spacer(Modifier.width(8.dp))
            Text(
                "PRO",
                style = TextStyle(fontFamily = Brand.Display, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, color = Brand.Ink),
                modifier = Modifier.clip(RoundedCornerShape(8.dp)).background(Brand.PalOrange).padding(horizontal = 8.dp, vertical = 2.dp),
            )
        }
        Spacer(Modifier.height(10.dp))
        Text(
            stringResource(headline(feature)),
            style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
            textAlign = TextAlign.Center,
        )
        Text(
            stringResource(R.string.paywall_sub),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(top = 6.dp),
        )

        Column(Modifier.fillMaxWidth().padding(vertical = 18.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            listOf(
                R.string.benefit_apps, R.string.benefit_messaging, R.string.benefit_otp,
                R.string.benefit_answers, R.string.benefit_meaning, R.string.benefit_more,
            ).forEach { Benefit(stringResource(it)) }
        }

        when {
            plans.isNotEmpty() && selected != null -> {
                Column(Modifier.fillMaxWidth(), verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    plans.forEach { plan ->
                        PlanCard(
                            plan = plan,
                            selected = plan.basePlanId == selected.basePlanId,
                            badge = if (plan.period == PlanPeriod.YEARLY) savings?.let { stringResource(R.string.plan_save, it) } else null,
                            onClick = { selectedId = plan.basePlanId },
                        )
                    }
                }
                Spacer(Modifier.height(18.dp))
                BrandButton(
                    text = if (selected.hasFreeTrial) stringResource(R.string.cta_trial, selected.freeTrialDays) else stringResource(R.string.cta_subscribe),
                    modifier = Modifier.fillMaxWidth(),
                ) { (context as? Activity)?.let { viewModel.buy(it, selected) } }
                Text(
                    terms(selected),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(top = 10.dp),
                )
            }
            availability == BillingAvailability.UNAVAILABLE || (availability == BillingAvailability.READY && plans.isEmpty()) -> {
                Text(
                    stringResource(R.string.billing_unavailable),
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.padding(vertical = 8.dp),
                )
                TextButton(onClick = viewModel::restore) { Text(stringResource(R.string.retry)) }
            }
            else -> CircularProgressIndicator(Modifier.padding(16.dp))
        }

        message?.let {
            Text(
                stringResource(it),
                style = MaterialTheme.typography.bodyMedium.copy(fontWeight = FontWeight.SemiBold),
                color = Brand.Peach,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = 10.dp),
            )
        }
        if (testBuild) {
            TextButton(onClick = { viewModel.setTestUnlock(true) }) { Text(stringResource(R.string.test_unlock)) }
        }
        Row(Modifier.fillMaxWidth().padding(vertical = 8.dp), horizontalArrangement = Arrangement.SpaceBetween) {
            TextButton(onClick = viewModel::restore) { Text(stringResource(R.string.restore_purchase)) }
            TextButton(onClick = onClose) {
                Text(stringResource(if (fromOnboarding) R.string.continue_free else R.string.not_now))
            }
        }
        }
    }
}

@Composable
private fun ProActive(entitlement: Entitlement, onManage: () -> Unit, onDone: () -> Unit) {
    Spacer(Modifier.height(40.dp))
    Pal(PalMood.FOUND, 150.dp)
    Text(
        stringResource(R.string.pro_active_title),
        style = MaterialTheme.typography.headlineSmall.copy(fontWeight = FontWeight.ExtraBold),
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 12.dp),
    )
    Text(
        stringResource(if (entitlement.subscribed) R.string.pro_active_sub else R.string.pro_active_test),
        style = MaterialTheme.typography.bodyMedium,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        textAlign = TextAlign.Center,
        modifier = Modifier.padding(top = 6.dp, bottom = 24.dp),
    )
    BrandButton(stringResource(R.string.done), Modifier.fillMaxWidth(), onClick = onDone)
    if (entitlement.subscribed) {
        TextButton(onClick = onManage, modifier = Modifier.padding(top = 8.dp)) { Text(stringResource(R.string.manage_subscription)) }
    }
}

@Composable
private fun Benefit(text: String) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            Modifier.size(26.dp).clip(CircleShape).background(Brand.PalOrange),
            contentAlignment = Alignment.Center,
        ) { Icon(Icons.Default.Check, null, Modifier.size(16.dp), tint = Brand.Ink) }
        Spacer(Modifier.width(12.dp))
        Text(text, style = MaterialTheme.typography.bodyLarge.copy(fontWeight = FontWeight.SemiBold))
    }
}

@Composable
private fun PlanCard(plan: PlanOption, selected: Boolean, badge: String?, onClick: () -> Unit) {
    val border = if (selected) BorderStroke(2.dp, Brand.PalOrange) else BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant)
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = MaterialTheme.colorScheme.surface,
        border = border,
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .selectable(selected = selected, role = Role.RadioButton, onClick = onClick),
    ) {
        Row(Modifier.padding(horizontal = 16.dp, vertical = 14.dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        stringResource(if (plan.period == PlanPeriod.YEARLY) R.string.plan_yearly else R.string.plan_monthly),
                        style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.ExtraBold),
                    )
                    badge?.let {
                        Spacer(Modifier.width(8.dp))
                        Text(
                            it,
                            style = MaterialTheme.typography.labelMedium.copy(fontWeight = FontWeight.Bold),
                            color = Brand.Ink,
                            modifier = Modifier.clip(CircleShape).background(Brand.Cream).padding(horizontal = 8.dp, vertical = 2.dp),
                        )
                    }
                }
                val detail = when {
                    plan.period == PlanPeriod.YEARLY -> perMonth(plan)?.let { stringResource(R.string.plan_per_month_equiv, it) }
                    else -> null
                }
                val trial = if (plan.hasFreeTrial) stringResource(R.string.plan_trial, plan.freeTrialDays) else null
                listOfNotNull(trial, detail).joinToString(" · ").takeIf { it.isNotEmpty() }?.let {
                    Text(it, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text(
                stringResource(if (plan.period == PlanPeriod.YEARLY) R.string.price_year else R.string.price_month, plan.formattedPrice),
                style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
            )
        }
    }
}

@Composable
private fun terms(plan: PlanOption): String {
    val yearly = plan.period == PlanPeriod.YEARLY
    return when {
        plan.hasFreeTrial && yearly -> stringResource(R.string.terms_trial_year, plan.freeTrialDays, plan.formattedPrice)
        plan.hasFreeTrial -> stringResource(R.string.terms_trial_month, plan.freeTrialDays, plan.formattedPrice)
        yearly -> stringResource(R.string.terms_year, plan.formattedPrice)
        else -> stringResource(R.string.terms_month, plan.formattedPrice)
    }
}

private fun headline(feature: ProFeature): Int = when (feature) {
    ProFeature.GENERAL -> R.string.headline_general
    ProFeature.APPS -> R.string.headline_apps
    ProFeature.FILES -> R.string.headline_files
    ProFeature.MESSAGING -> R.string.headline_messaging
    ProFeature.OTP -> R.string.headline_otp
    ProFeature.QUICK_ANSWERS -> R.string.headline_answers
    ProFeature.MORE_RESULTS -> R.string.headline_more
    ProFeature.PINS -> R.string.headline_pins
}

/** "$3.33" for a $39.99 yearly plan, in the plan's own currency. */
private fun perMonth(plan: PlanOption): String? = runCatching {
    NumberFormat.getCurrencyInstance().apply { currency = Currency.getInstance(plan.currencyCode) }
        .format(plan.priceMicros / 12.0 / 1_000_000.0)
}.getOrNull()

private fun open(context: android.content.Context, url: String) {
    runCatching { context.startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url)).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)) }
}
