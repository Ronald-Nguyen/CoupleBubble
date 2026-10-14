package com.aistudio.couplebubble.qxztrw.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionStatus
import java.text.NumberFormat
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Countdown to the next reunion: days until it (or the current day of the visit), the date range, the waiting
 * progress and the distance while both partners share a fresh location. Status and distance are computed at
 * render time, so the midnight refresh moves it on by itself.
 */
class ReunionWidget : GlanceAppWidget() {

    override val sizeMode = SizeMode.Responsive(setOf(SMALL, TALL))

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = AndroidWidgetPublisher.read(context)
        val reunion = snapshot?.reunion
        val status = reunion?.status(LocalDate.now())?.takeIf { it != ReunionStatus.Over }
        val resources = context.resources
        val dayFormat = DateTimeFormatter.ofPattern("EE, d. MMM", Locale.GERMAN)
        val startDay = reunion?.startDate?.format(dayFormat).orEmpty()
        val endDay = reunion?.endDate?.format(dayFormat).orEmpty()

        val heading: String
        val hero: String
        val detail: String
        val progress: Float?
        when (status) {
            is ReunionStatus.Upcoming -> {
                heading = context.getString(R.string.reunion_until_label)
                hero = status.daysUntil.toString()
                detail = context.getString(
                    R.string.reunion_range,
                    resources.getQuantityString(R.plurals.reunion_days_until_unit, status.daysUntil),
                    startDay,
                    endDay
                )
                progress = status.progress
            }
            is ReunionStatus.Together -> {
                heading = context.getString(
                    if (status.day == 1) R.string.reunion_today_label else R.string.reunion_together_label
                )
                hero = context.getString(R.string.reunion_day_of, status.day)
                detail = resources.getQuantityString(R.plurals.reunion_day_of_total, status.durationDays, status.durationDays) +
                    " · " + context.getString(R.string.reunion_until_end, endDay)
                progress = null
            }
            else -> {
                heading = context.getString(R.string.widget_reunion_heading)
                hero = ""
                detail = context.getString(if (snapshot == null) R.string.widget_not_paired else R.string.widget_reunion_none)
                progress = null
            }
        }
        val distance = status?.let { reunion?.distanceKm(System.currentTimeMillis()) }?.let { km ->
            context.getString(R.string.widget_reunion_distance, NumberFormat.getIntegerInstance(Locale.GERMAN).format(km))
        }

        provideContent {
            val isTall = LocalSize.current.height >= TALL.height
            WidgetFrame {
                Text(
                    text = heading.uppercase(),
                    maxLines = 1,
                    style = TextStyle(color = ColorProvider(WidgetLabel), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                if (status == null) {
                    Text(text = detail, style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 13.sp))
                } else {
                    Text(
                        text = hero,
                        maxLines = 1,
                        style = TextStyle(
                            color = ColorProvider(WidgetAccent),
                            fontSize = if (isTall) 48.sp else 36.sp,
                            fontWeight = FontWeight.Bold
                        )
                    )
                    Text(
                        text = detail,
                        maxLines = 2,
                        style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 12.sp)
                    )
                    if (isTall && progress != null) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        LinearProgressIndicator(
                            progress = progress,
                            modifier = GlanceModifier.fillMaxWidth(),
                            color = ColorProvider(WidgetAccent),
                            backgroundColor = ColorProvider(Color.White.copy(alpha = 0.12f))
                        )
                    }
                    if (isTall && distance != null) {
                        Spacer(modifier = GlanceModifier.height(8.dp))
                        Text(
                            text = distance,
                            maxLines = 1,
                            style = TextStyle(color = ColorProvider(Color.White), fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        )
                    }
                }
            }
        }
    }

    private companion object {
        /** Two rows: countdown and range only. */
        val SMALL = DpSize(180.dp, 110.dp)

        /** Three rows and more: adds the progress bar and the distance. */
        val TALL = DpSize(180.dp, 170.dp)
    }
}
