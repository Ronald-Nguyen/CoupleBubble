package com.aistudio.couplebubble.qxztrw.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.glance.GlanceId
import androidx.glance.LocalSize
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.provideContent
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.calendar.RelationshipDateCalculator
import com.aistudio.couplebubble.qxztrw.domain.model.DeepLinkContracts
import com.aistudio.couplebubble.qxztrw.ui.counterText
import com.aistudio.couplebubble.qxztrw.ui.milestoneCountdown
import com.aistudio.couplebubble.qxztrw.ui.milestoneTitle

internal val WidgetSurface = Color(0xFF0F2137)
internal val WidgetAccent = Color(0xFFFF8A65)
internal val WidgetLabel = Color(0xFF97CBFF)
internal val WidgetMuted = Color(0xFFCBD5E1)

/**
 * Counter widget: time together in the counter mode chosen in the app, plus names and the next milestone when
 * there is room; read from the stored [WidgetSnapshot].
 */
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
        val counter = metrics?.let { counterText(context.resources, it, snapshot.counterDisplayMode) }
        val togetherLabel = counter?.let { context.getString(R.string.widget_together_unit, it.unit) }.orEmpty()
        val notPairedText = context.getString(R.string.widget_not_paired)
        val notPairedShort = context.getString(R.string.widget_not_paired_short)

        provideContent {
            val layout = widgetLayout(LocalSize.current)
            WidgetFrame(layout, action = widgetClickAction(DeepLinkContracts.TARGET_HOME)) {
                if (counter == null || names == null) {
                    WidgetEmpty(layout, notPairedShort, notPairedText)
                    return@WidgetFrame
                }
                when (layout) {
                    WidgetLayout.COMPACT -> WidgetTwoLineBody(layout, counter.value, counter.unit)
                    WidgetLayout.ROW -> WidgetTwoLineBody(layout, counter.value, togetherLabel)
                    WidgetLayout.REGULAR, WidgetLayout.TALL -> {
                        if (layout == WidgetLayout.TALL) {
                            WidgetAccentText(names)
                            WidgetGap(4)
                        }
                        WidgetHero(counter.value, layout)
                        WidgetLabelText(togetherLabel)
                        counter.subline?.let { WidgetMutedText(it) }
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
