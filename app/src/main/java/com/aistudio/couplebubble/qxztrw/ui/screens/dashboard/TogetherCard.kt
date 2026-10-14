package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.core.ui.GermanDayMonthYear
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.GestureHint
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.RelationshipMetrics
import com.aistudio.couplebubble.qxztrw.ui.components.GestureHintBanner
import com.aistudio.couplebubble.qxztrw.ui.counterText
import com.aistudio.couplebubble.qxztrw.ui.milestoneCountdown
import com.aistudio.couplebubble.qxztrw.ui.milestoneTitle
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.time.LocalDate

@Composable
internal fun TogetherCard(
    metrics: RelationshipMetrics,
    displayMode: CounterDisplayMode,
    onDisplayModeSelected: (CounterDisplayMode) -> Unit,
    onMilestoneClick: () -> Unit,
    modifier: Modifier = Modifier,
    showMilestoneTapHint: Boolean = false,
    onDismissMilestoneHint: () -> Unit = {}
) {
    val haptic = LocalHapticFeedback.current
    val resources = LocalContext.current.resources
    val nextMilestoneTitle = remember(metrics.nextMilestone, resources) {
        metrics.nextMilestone?.let { milestoneTitle(resources, it) }
    }
    val nextMilestoneCountdown = remember(metrics.daysUntilNextMilestone, resources) {
        milestoneCountdown(resources, metrics.daysUntilNextMilestone)
    }

    Card(
        modifier = modifier,
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.2f)),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            Text(
                text = stringResource(R.string.together_since_date, metrics.startDate.format(GermanDayMonthYear)).uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.secondary,
                modifier = Modifier.semantics { heading() }
            )

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .horizontalScroll(rememberScrollState())
                    .padding(top = 10.dp)
            ) {
                CounterDisplayMode.entries.forEach { mode ->
                    val isSelected = mode == displayMode
                    FilterChip(
                        selected = isSelected,
                        onClick = {
                            if (!isSelected) {
                                haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                                onDisplayModeSelected(mode)
                            }
                        },
                        label = { Text(stringResource(mode.labelRes)) },
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.secondary,
                            selectedLabelColor = MaterialTheme.colorScheme.onSecondary
                        ),
                        border = FilterChipDefaults.filterChipBorder(
                            enabled = true,
                            selected = isSelected,
                            borderColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f),
                            borderWidth = 1.dp,
                            selectedBorderWidth = 1.dp
                        )
                    )
                }
            }

            AnimatedContent(
                targetState = displayMode,
                transitionSpec = {
                    (fadeIn(spring(stiffness = Spring.StiffnessLow)) +
                        slideInVertically(spring(stiffness = Spring.StiffnessLow)) { it / 4 })
                        .togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                },
                label = "counter_display_mode"
            ) { mode ->
                CounterHero(metrics = metrics, mode = mode)
            }

            if (nextMilestoneTitle != null) {
                HorizontalDivider(
                    modifier = Modifier.padding(vertical = 16.dp),
                    color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
                )

                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onMilestoneClick)
                ) {
                    Text(
                        text = stringResource(R.string.next_milestone).uppercase(),
                        fontSize = 12.sp,
                        letterSpacing = 1.5.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.secondary
                    )

                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 4.dp, bottom = 10.dp)
                    ) {
                        Text(
                            text = nextMilestoneTitle,
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f, fill = false)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Text(
                            text = nextMilestoneCountdown,
                            style = MaterialTheme.typography.labelLarge,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.secondary
                        )
                    }

                    LinearProgressIndicator(
                        progress = { metrics.progressToNextMilestone },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(6.dp)
                            .clip(RoundedCornerShape(6.dp)),
                        color = MaterialTheme.colorScheme.secondary,
                        trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
                        strokeCap = StrokeCap.Round
                    )

                    if (showMilestoneTapHint) {
                        Spacer(modifier = Modifier.height(10.dp))
                        GestureHintBanner(
                            hint = GestureHint.MILESTONE_TAP,
                            message = stringResource(R.string.hint_milestone_tap),
                            onDismiss = onDismissMilestoneHint
                        )
                    }
                }
            }
        }
    }
}

private val CounterDisplayMode.labelRes: Int
    get() = when (this) {
        CounterDisplayMode.DAYS -> R.string.counter_mode_days
        CounterDisplayMode.WEEKS -> R.string.counter_mode_weeks
        CounterDisplayMode.MONTHS -> R.string.counter_mode_months
        CounterDisplayMode.YEARS -> R.string.counter_mode_years
    }

@Composable
private fun CounterHero(
    metrics: RelationshipMetrics,
    mode: CounterDisplayMode
) {
    val resources = LocalContext.current.resources
    val counter = remember(metrics, mode) { counterText(resources, metrics, mode) }

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(modifier = Modifier.padding(top = 6.dp)) {
            Text(
                text = counter.value,
                fontSize = 52.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.onSurface,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.alignByBaseline().weight(1f, fill = false)
            )
            Spacer(modifier = Modifier.width(10.dp))
            Text(
                text = counter.unit.uppercase(),
                fontSize = 12.sp,
                letterSpacing = 1.5.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.alignByBaseline()
            )
        }

        val subline = counter.subline
        if (subline != null) {
            Text(
                text = subline,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Preview(name = "Together Card Light", showBackground = true)
@Composable
private fun TogetherCardPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        TogetherCard(
            metrics = RelationshipDateCalculator.calculate(2025, 6, 25, today = LocalDate.of(2026, 9, 25)),
            displayMode = CounterDisplayMode.DAYS,
            onDisplayModeSelected = {},
            onMilestoneClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}

@Preview(name = "Together Card Dark (Wochen)", showBackground = true)
@Composable
private fun TogetherCardDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        TogetherCard(
            metrics = RelationshipDateCalculator.calculate(
                2025, 6, 25,
                today = LocalDate.of(2026, 9, 26),
                enabledKinds = setOf(MilestoneKind.MONTHS, MilestoneKind.YEARS)
            ),
            displayMode = CounterDisplayMode.WEEKS,
            onDisplayModeSelected = {},
            onMilestoneClick = {},
            modifier = Modifier.padding(16.dp)
        )
    }
}
