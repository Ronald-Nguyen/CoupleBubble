@file:Suppress("RestrictedApi")

package com.aistudio.couplebubble.qxztrw.widget

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceId
import androidx.glance.GlanceModifier
import androidx.glance.appwidget.GlanceAppWidget
import androidx.glance.appwidget.cornerRadius
import androidx.glance.appwidget.provideContent
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Box
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.unit.ColorProvider
import com.aistudio.couplebubble.qxztrw.model.RelationshipDateCalculator

class CoupleBubbleWidget : GlanceAppWidget() {

    override suspend fun provideGlance(context: Context, id: GlanceId) {
        val metrics = RelationshipDateCalculator.calculate(
            year = 2025,
            month = 6,
            day = 25,
        )

        provideContent {
            Box(
                modifier = GlanceModifier
                    .fillMaxSize()
                    .background(ColorProvider(Color(0xFF0F2137)))
                    .cornerRadius(20.dp)
                    .padding(16.dp),
                contentAlignment = Alignment.Center,
            ) {
                Column(
                    modifier = GlanceModifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        text = "Alex & Sam",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFFF8A65)),
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(4.dp))
                    Text(
                        text = metrics.totalDays.toString(),
                        style = TextStyle(
                            color = ColorProvider(Color.White),
                            fontSize = 32.sp,
                            fontWeight = FontWeight.Bold,
                        ),
                    )
                    Text(
                        text = "Tage zusammen",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFF97CBFF)),
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Medium,
                        ),
                    )
                    Spacer(modifier = GlanceModifier.height(6.dp))
                    Text(
                        text = "${metrics.nextAnniversaryTitle} in ${metrics.daysUntilNextAnniversary} Tagen",
                        style = TextStyle(
                            color = ColorProvider(Color(0xFFCBD5E1)),
                            fontSize = 11.sp,
                        ),
                    )
                }
            }
        }
    }
}
