package com.aistudio.couplebubble.qxztrw.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.ui.milestoneCountdown
import com.aistudio.couplebubble.qxztrw.ui.milestoneTitle

internal val WidgetSurface = Color(0xFF0F2137)
internal val WidgetAccent = Color(0xFFFF8A65)
internal val WidgetLabel = Color(0xFF97CBFF)
internal val WidgetMuted = Color(0xFFCBD5E1)

/** Counter widget: names, days together and the next milestone, read from the stored [WidgetSnapshot]. */
class CoupleBubbleWidget : GlanceAppWidget() {

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
        val names = snapshot?.let { context.getString(R.string.widget_names, it.partner1Name, it.partner2Name) }
        val milestoneText = metrics?.nextMilestone?.let { milestone ->
            context.getString(
                R.string.widget_milestone_countdown,
                milestoneTitle(context.resources, milestone),
                milestoneCountdown(context.resources, metrics.daysUntilNextMilestone)
            )
        }
        val days = metrics?.totalDays?.toString().orEmpty()
        val daysShort = metrics?.let {
            context.resources.getQuantityString(R.plurals.widget_days_short, it.totalDays.toInt().coerceAtLeast(0))
        }.orEmpty()
        val daysTogetherLabel = context.getString(R.string.widget_days_together)
        val notPairedText = context.getString(R.string.widget_not_paired)
        val notPairedShort = context.getString(R.string.widget_not_paired_short)

        provideContent {
            val layout = widgetLayout(LocalSize.current)
            WidgetFrame(layout) {
                if (metrics == null || names == null) {
                    WidgetEmpty(layout, notPairedShort, notPairedText)
                    return@WidgetFrame
                }
                when (layout) {
                    WidgetLayout.COMPACT -> WidgetCompactBody(days, daysShort)
                    WidgetLayout.ROW -> WidgetRowBody(days, daysTogetherLabel, names)
                    WidgetLayout.REGULAR, WidgetLayout.TALL -> {
                        WidgetAccentText(names)
                        WidgetGap(4)
                        WidgetHero(days, layout)
                        WidgetLabelText(daysTogetherLabel)
                        if (layout == WidgetLayout.TALL && milestoneText != null) {
                            WidgetGap(6)
                            WidgetMutedText(milestoneText, maxLines = 2)
                        }
                    }
                }
            }
        }
    }
}
