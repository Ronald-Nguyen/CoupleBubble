package com.aistudio.couplebubble.qxztrw.widget

import android.content.Context
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.ui.milestoneTitle
import java.time.format.DateTimeFormatter
import java.util.Locale

/**
 * Shows only the next milestone: big countdown, title, date and progress since the previous one.
 * Recomputed on every redraw (midnight alarm, space changes), so it moves on to the next milestone by itself.
 */
class MilestoneWidget : GlanceAppWidget() {

    override val sizeMode = WidgetResponsiveSizes

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = AndroidWidgetPublisher.read(context)
        val metrics = snapshot?.let {
            RelationshipDateCalculator.calculate(
                year = it.anniversaryYear,
                month = it.anniversaryMonth,
                day = it.anniversaryDay,
                enabledKinds = it.enabledMilestoneKinds,
                customMilestones = it.customMilestones,
                momentMilestones = it.momentMilestones
            )
        }
        val milestone = metrics?.nextMilestone
        val days = metrics?.daysUntilNextMilestone?.toInt() ?: 0
        val title = milestone?.let { milestoneTitle(context.resources, it) }.orEmpty()
        val date = milestone?.date?.format(DateTimeFormatter.ofPattern("d. MMMM yyyy", Locale.GERMAN)).orEmpty()
        val countdown = if (days == 0) context.getString(R.string.widget_milestone_today) else days.toString()
        val countdownLabel = if (days == 0) {
            context.getString(R.string.widget_milestone_today_label)
        } else {
            context.resources.getQuantityString(R.plurals.widget_milestone_days_left, days)
        }
        val countdownShort = if (days == 0) "" else context.resources.getQuantityString(R.plurals.widget_days_short, days)
        val emptyText = context.getString(if (snapshot == null) R.string.widget_not_paired else R.string.widget_milestone_none)
        val emptyShort = context.getString(
            if (snapshot == null) R.string.widget_not_paired_short else R.string.widget_milestone_none_short
        )
        val heading = context.getString(R.string.widget_milestone_heading)
        val progress = metrics?.progressToNextMilestone ?: 0f

        provideContent {
            val layout = widgetLayout(LocalSize.current)
            WidgetFrame(layout) {
                if (milestone == null) {
                    if (layout != WidgetLayout.COMPACT && layout != WidgetLayout.ROW) {
                        WidgetLabelText(heading)
                        WidgetGap(4)
                    }
                    WidgetEmpty(layout, emptyShort, emptyText)
                    return@WidgetFrame
                }
                when (layout) {
                    WidgetLayout.COMPACT -> WidgetCompactBody(countdown, countdownShort)
                    WidgetLayout.ROW -> WidgetRowBody(countdown, title, date)
                    WidgetLayout.REGULAR, WidgetLayout.TALL -> {
                        WidgetLabelText(heading)
                        WidgetGap(4)
                        WidgetHero(countdown, layout)
                        WidgetLabelText(countdownLabel)
                        WidgetGap(6)
                        WidgetAccentText(title)
                        if (layout == WidgetLayout.TALL) {
                            WidgetMutedText(date)
                            WidgetGap(8)
                            WidgetProgress(progress)
                        }
                    }
                }
            }
        }
    }
}
