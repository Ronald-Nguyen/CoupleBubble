package com.aistudio.couplebubble.qxztrw.widget

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aistudio.couplebubble.qxztrw.MainActivity
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
        val daysTogetherLabel = context.getString(R.string.widget_days_together)
        val notPairedText = context.getString(R.string.widget_not_paired)

        provideContent {
            WidgetFrame {
                if (metrics == null || names == null) {
                    Text(text = notPairedText, style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 13.sp))
                } else {
                    Text(
                        text = names,
                        maxLines = 1,
                        style = TextStyle(color = ColorProvider(WidgetAccent), fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = metrics.totalDays.toString(),
                        style = TextStyle(color = ColorProvider(Color.White), fontSize = 40.sp, fontWeight = FontWeight.Bold)
                    )
                    Text(
                        text = daysTogetherLabel.uppercase(),
                        style = TextStyle(color = ColorProvider(WidgetLabel), fontSize = 11.sp, fontWeight = FontWeight.Medium)
                    )
                    if (milestoneText != null) {
                        Spacer(modifier = GlanceModifier.height(6.dp))
                        Text(
                            text = milestoneText,
                            maxLines = 2,
                            style = TextStyle(color = ColorProvider(WidgetMuted), fontSize = 11.sp)
                        )
                    }
                }
            }
        }
    }
}

/** Navy, rounded, left-aligned frame shared by both widgets; a tap opens the app. */
@Composable
internal fun WidgetFrame(content: @Composable () -> Unit) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(ColorProvider(WidgetSurface))
            .cornerRadius(20.dp)
            .padding(16.dp)
            .clickable(actionStartActivity<MainActivity>()),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        content()
    }
}
