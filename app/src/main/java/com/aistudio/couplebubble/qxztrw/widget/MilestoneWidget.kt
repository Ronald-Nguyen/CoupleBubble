package com.aistudio.couplebubble.qxztrw.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.provideContent
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
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

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val snapshot = AndroidWidgetPublisher.read(context)
        val metrics = snapshot?.let {
            RelationshipDateCalculator.calculate(
                year = it.anniversaryYear,
                month = it.anniversaryMonth,
                day = it.anniversaryDay,
                enabledKinds = it.enabledMilestoneKinds,
                customMilestones = it.customMilestones
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
        val emptyText = context.getString(
            if (snapshot == null) R.string.widget_not_paired else R.string.widget_milestone_none
        )
        val heading = context.getString(R.string.widget_milestone_heading)
        val progress = metrics?.progressToNextMilestone ?: 0f

        provideContent {
            WidgetFrame {
                Text(
                    text = heading.uppercase(),
                    style = TextStyle(color = ColorProvider(WidgetLabel), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                )
                Spacer(modifier = GlanceModifier.height(4.dp))
                if (milestone == null) {
                    Text(text = emptyText, style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 13.sp))
                } else {
                    Text(
                        text = countdown,
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 36.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = countdownLabel.uppercase(),
                        style = TextStyle(color = ColorProvider(WidgetLabel), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = title,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(WidgetAccent), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(text = date, style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 11.sp))
                    Spacer(modifier = GlanceModifier.height(8.dp))
                    LinearProgressIndicator(
                        progress = progress,
                        modifier = GlanceModifier.fillMaxWidth(),
                        color = ColorProvider(WidgetAccent),
                        backgroundColor = ColorProvider(Color.White.copy(alpha = 0.12f))
                    )
                }
            }
        }
    }
}
