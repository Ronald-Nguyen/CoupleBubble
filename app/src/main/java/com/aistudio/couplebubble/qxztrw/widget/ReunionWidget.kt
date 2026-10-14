package com.aistudio.couplebubble.qxztrw.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
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

    override val sizeMode = WidgetResponsiveSizes

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
        val heroShort: String
        val detail: String
        val rowDetail: String
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
                heroShort = resources.getQuantityString(R.plurals.widget_days_short, status.daysUntil)
                rowDetail = context.getString(R.string.widget_reunion_range, startDay, endDay)
                progress = status.progress
            }
            is ReunionStatus.Together -> {
                heading = context.getString(
                    if (status.day == 1) R.string.reunion_today_label else R.string.reunion_together_label
                )
                hero = context.getString(R.string.reunion_day_of, status.day)
                detail = resources.getQuantityString(R.plurals.reunion_day_of_total, status.durationDays, status.durationDays) +
                    " · " + context.getString(R.string.reunion_until_end, endDay)
                heroShort = context.getString(R.string.widget_reunion_of_total_short, status.durationDays)
                rowDetail = context.getString(R.string.reunion_until_end, endDay)
                progress = null
            }
            else -> {
                heading = context.getString(R.string.widget_reunion_heading)
                hero = ""
                heroShort = ""
                detail = context.getString(if (snapshot == null) R.string.widget_not_paired else R.string.widget_reunion_none)
                rowDetail = ""
                progress = null
            }
        }
        val emptyShort = context.getString(
            if (snapshot == null) R.string.widget_not_paired_short else R.string.widget_reunion_none_short
        )
        val distance = status?.let { reunion?.distanceKm(System.currentTimeMillis()) }?.let { km ->
            context.getString(R.string.widget_reunion_distance, NumberFormat.getIntegerInstance(Locale.GERMAN).format(km))
        }

        provideContent {
            val size = LocalSize.current
            val layout = widgetLayout(size)
            WidgetFrame(layout) {
                if (status == null) {
                    if (layout != WidgetLayout.COMPACT && layout != WidgetLayout.ROW) {
                        WidgetLabelText(heading)
                        WidgetGap(4)
                    }
                    WidgetEmpty(layout, emptyShort, detail)
                    return@WidgetFrame
                }
                when (layout) {
                    WidgetLayout.COMPACT -> WidgetCompactBody(hero, heroShort, WidgetAccent)
                    WidgetLayout.ROW -> WidgetRowBody(
                        hero = hero,
                        heading = heading,
                        detail = if (distance != null && size.width >= WideRowWidth) "$rowDetail · $distance" else rowDetail,
                        heroColor = WidgetAccent
                    )
                    WidgetLayout.REGULAR, WidgetLayout.TALL -> {
                        WidgetLabelText(heading)
                        WidgetGap(4)
                        WidgetHero(hero, layout, WidgetAccent)
                        WidgetMutedText(detail, maxLines = 2, fontSize = 12.sp)
                        if (layout == WidgetLayout.TALL && progress != null) {
                            WidgetGap(8)
                            WidgetProgress(progress)
                        }
                        if (layout == WidgetLayout.TALL && distance != null) {
                            WidgetGap(8)
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
    }
}
