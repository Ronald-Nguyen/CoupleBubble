package com.aistudio.couplebubble.qxztrw.ui.screens.dashboard

import android.content.res.Resources
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.pluralStringResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlaceSource
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionStatus
import com.aistudio.couplebubble.qxztrw.ui.preview.PreviewData
import com.aistudio.couplebubble.qxztrw.ui.screens.home.ReunionUiState
import com.aistudio.couplebubble.qxztrw.ui.theme.CoupleBubbleTheme
import java.text.NumberFormat
import java.time.format.DateTimeFormatter
import java.util.Locale

/** "Fr, 24. Okt.", for the reunion range. */
private val ReunionDayFormat: DateTimeFormatter = DateTimeFormatter.ofPattern("EE, d. MMM", Locale.GERMAN)

/** The countdown to the next reunion, right below the together counter; a tap opens the planning dialog. */
@Composable
internal fun ReunionCard(
    state: ReunionUiState,
    partnerName: String,
    ownColor: Color,
    partnerColor: Color,
    onClick: () -> Unit,
    onCaptureMoment: () -> Unit,
    modifier: Modifier = Modifier
) {
    val reunion = state.reunion
    val startDay = remember(reunion.startDate) { reunion.startDate.format(ReunionDayFormat) }
    val endDay = remember(reunion.endDate) { reunion.endDate.format(ReunionDayFormat) }

    Card(
        onClick = onClick,
        modifier = modifier.testTag("reunion_card"),
        shape = RoundedCornerShape(24.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.secondary.copy(alpha = 0.55f)),
        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp)
        ) {
            AnimatedContent(
                targetState = state.status,
                transitionSpec = {
                    fadeIn(spring(stiffness = Spring.StiffnessLow)).togetherWith(fadeOut(spring(stiffness = Spring.StiffnessMedium)))
                },
                label = "reunion_status"
            ) { status ->
                when (status) {
                    is ReunionStatus.Upcoming -> UpcomingHero(status, startDay, endDay)
                    is ReunionStatus.Together -> TogetherHero(status, endDay, onCaptureMoment)
                    ReunionStatus.Over -> Unit
                }
            }

            HorizontalDivider(
                modifier = Modifier.padding(vertical = 16.dp),
                color = MaterialTheme.colorScheme.outline.copy(alpha = 0.15f)
            )

            ReunionDistance(state = state, partnerName = partnerName, ownColor = ownColor, partnerColor = partnerColor)
        }
    }
}

@Composable
private fun HeroLabel(text: String) {
    Text(
        text = text.uppercase(),
        fontSize = 12.sp,
        letterSpacing = 1.5.sp,
        fontWeight = FontWeight.SemiBold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.semantics { heading() }
    )
}

@Composable
private fun HeroNumber(text: String) {
    Text(
        text = text,
        fontSize = 52.sp,
        lineHeight = 56.sp,
        fontWeight = FontWeight.ExtraBold,
        color = MaterialTheme.colorScheme.secondary,
        modifier = Modifier.padding(top = 6.dp)
    )
}

@Composable
private fun UpcomingHero(status: ReunionStatus.Upcoming, startDay: String, endDay: String) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HeroLabel(stringResource(R.string.reunion_until_label))
        HeroNumber(status.daysUntil.toString())
        Text(
            text = stringResource(
                R.string.reunion_range,
                pluralStringResource(R.plurals.reunion_days_until_unit, status.daysUntil),
                startDay,
                endDay
            ),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        LinearProgressIndicator(
            progress = { status.progress },
            modifier = Modifier
                .fillMaxWidth()
                .padding(top = 12.dp)
                .height(6.dp)
                .clip(RoundedCornerShape(6.dp)),
            color = MaterialTheme.colorScheme.secondary,
            trackColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            strokeCap = StrokeCap.Round
        )
    }
}

@Composable
private fun TogetherHero(status: ReunionStatus.Together, endDay: String, onCaptureMoment: () -> Unit) {
    Column(modifier = Modifier.fillMaxWidth()) {
        HeroLabel(stringResource(if (status.day == 1) R.string.reunion_today_label else R.string.reunion_together_label))
        HeroNumber(stringResource(R.string.reunion_day_of, status.day))
        Text(
            text = pluralStringResource(R.plurals.reunion_day_of_total, status.durationDays, status.durationDays) +
                " · " + stringResource(R.string.reunion_until_end, endDay),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        TextButton(onClick = onCaptureMoment, modifier = Modifier.padding(top = 4.dp)) {
            Icon(Icons.Default.Add, contentDescription = null, tint = MaterialTheme.colorScheme.secondary)
            Text(
                text = stringResource(R.string.reunion_capture_moment),
                color = MaterialTheme.colorScheme.secondary,
                fontWeight = FontWeight.SemiBold,
                modifier = Modifier.padding(start = 6.dp)
            )
        }
    }
}

@Composable
private fun ReunionDistance(state: ReunionUiState, partnerName: String, ownColor: Color, partnerColor: Color) {
    val resources = LocalContext.current.resources
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        val distanceKm = state.distanceKm
        if (distanceKm != null) {
            val formatted = remember(distanceKm) { NumberFormat.getIntegerInstance(Locale.GERMAN).format(distanceKm) }
            Text(
                text = stringResource(R.string.reunion_distance, formatted),
                style = MaterialTheme.typography.titleSmall,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onSurface
            )
        } else {
            Text(
                text = stringResource(R.string.reunion_distance_missing),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        SourceRow(stringResource(R.string.reunion_you), sourceLabel(resources, state.ownSource, state.nowMillis), ownColor)
        SourceRow(partnerName, sourceLabel(resources, state.partnerSource, state.nowMillis), partnerColor)
    }
}

@Composable
private fun SourceRow(name: String, source: String, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(
            modifier = Modifier
                .size(8.dp)
                .background(color, CircleShape)
        )
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            fontWeight = FontWeight.SemiBold,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.padding(start = 10.dp)
        )
        Text(
            text = source,
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(start = 8.dp)
        )
    }
}

private const val HOUR_MILLIS = 60L * 60 * 1000

internal fun sourceLabel(resources: Resources, source: ReunionPlaceSource, nowMillis: Long): String = when (source) {
    is ReunionPlaceSource.Location -> {
        val hours = ((nowMillis - source.locatedAtMillis) / HOUR_MILLIS).toInt().coerceAtLeast(0)
        when {
            hours == 0 -> resources.getString(R.string.reunion_source_location_now)
            hours < 24 -> resources.getQuantityString(R.plurals.reunion_source_location_hours, hours, hours)
            else -> (hours / 24).let { resources.getQuantityString(R.plurals.reunion_source_location_days, it, it) }
        }
    }
    ReunionPlaceSource.None -> resources.getString(R.string.reunion_source_none)
}

@Preview(name = "ReunionCard Light", showBackground = true)
@Composable
private fun ReunionCardLightPreview() {
    CoupleBubbleTheme(darkTheme = false) {
        ReunionCard(
            state = PreviewData.reunion,
            partnerName = PreviewData.space.partner2Name,
            ownColor = Color(0xFFE65D2E),
            partnerColor = Color(0xFF5B8DB8),
            onClick = {},
            onCaptureMoment = {}
        )
    }
}

@Preview(name = "ReunionCard Dark", showBackground = true)
@Composable
private fun ReunionCardDarkPreview() {
    CoupleBubbleTheme(darkTheme = true) {
        ReunionCard(
            state = PreviewData.reunionTogether,
            partnerName = PreviewData.space.partner2Name,
            ownColor = Color(0xFFE65D2E),
            partnerColor = Color(0xFF5B8DB8),
            onClick = {},
            onCaptureMoment = {}
        )
    }
}
