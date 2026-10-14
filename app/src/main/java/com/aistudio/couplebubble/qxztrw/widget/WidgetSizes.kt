package com.aistudio.couplebubble.qxztrw.widget

import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.glance.GlanceModifier
import androidx.glance.action.Action
import androidx.glance.action.ActionParameters
import androidx.glance.action.actionParametersOf
import androidx.glance.action.actionStartActivity
import androidx.glance.action.clickable
import com.aistudio.couplebubble.qxztrw.domain.model.DeepLinkContracts
import androidx.glance.appwidget.LinearProgressIndicator
import androidx.glance.appwidget.SizeMode
import androidx.glance.appwidget.cornerRadius
import androidx.glance.background
import androidx.glance.layout.Alignment
import androidx.glance.layout.Column
import androidx.glance.layout.Spacer
import androidx.glance.layout.fillMaxSize
import androidx.glance.layout.fillMaxWidth
import androidx.glance.layout.height
import androidx.glance.layout.padding
import androidx.glance.text.FontWeight
import androidx.glance.text.Text
import androidx.glance.text.TextStyle
import androidx.glance.semantics.contentDescription
import androidx.glance.semantics.semantics
import androidx.glance.unit.ColorProvider
import com.aistudio.couplebubble.qxztrw.MainActivity
import kotlin.math.roundToInt

/** How much a widget shows, picked from the size the launcher gives it. */
enum class WidgetLayout {
    /** 1×1 and 1×2: the number and a tiny unit. */
    COMPACT,

    /** 2×1 to 4×1: two lines, the number and one label below it. */
    ROW,

    /** 2×2 and 3×2: heading, number and one or two lines. */
    REGULAR,

    /** Three rows and more: everything, including progress and extras. */
    TALL
}

/** Width from which a [WidgetLayout.ROW] label has room for an extra (e.g. the reunion distance). */
internal val WideRowWidth = 250.dp

fun widgetLayout(size: DpSize): WidgetLayout = when {
    size.width < 110.dp -> WidgetLayout.COMPACT
    size.height < 100.dp -> WidgetLayout.ROW
    size.height >= 170.dp -> WidgetLayout.TALL
    else -> WidgetLayout.REGULAR
}

/** The size buckets Glance renders for; each one maps to a [WidgetLayout] via [widgetLayout]. */
internal val WidgetResponsiveSizes = SizeMode.Responsive(
    setOf(
        DpSize(48.dp, 48.dp),
        DpSize(48.dp, 110.dp),
        DpSize(110.dp, 48.dp),
        DpSize(WideRowWidth, 48.dp),
        DpSize(110.dp, 110.dp),
        DpSize(180.dp, 110.dp),
        DpSize(180.dp, 170.dp)
    )
)

/** Hero number size; in a 1×1 cell longer texts ("Heute", "Tag 2") shrink so they fit. */
internal fun heroSize(layout: WidgetLayout, text: String): TextUnit = when (layout) {
    WidgetLayout.COMPACT -> if (text.length <= 3) 22.sp else 16.sp
    WidgetLayout.ROW -> 24.sp
    WidgetLayout.REGULAR -> 36.sp
    WidgetLayout.TALL -> 48.sp
}

val TargetActionParameterKey = ActionParameters.Key<String>(DeepLinkContracts.EXTRA_TARGET)

fun widgetClickAction(target: String): Action =
    actionStartActivity<MainActivity>(actionParametersOf(TargetActionParameterKey to target))

/** Rounded, left-aligned frame on the day/night surface ([WidgetColors.Surface]), shared by all widgets; a tap opens the app. */
@Composable
internal fun WidgetFrame(
    layout: WidgetLayout,
    action: Action = widgetClickAction(DeepLinkContracts.TARGET_HOME),
    content: @Composable () -> Unit
) {
    Column(
        modifier = GlanceModifier
            .fillMaxSize()
            .background(WidgetColors.Surface.provider)
            .cornerRadius(if (layout == WidgetLayout.COMPACT) 16.dp else 20.dp)
            .padding(
                horizontal = if (layout == WidgetLayout.COMPACT) 8.dp else if (layout == WidgetLayout.ROW) 12.dp else 16.dp,
                vertical = if (layout == WidgetLayout.ROW || layout == WidgetLayout.COMPACT) 6.dp else 16.dp
            )
            .clickable(action),
        verticalAlignment = Alignment.CenterVertically,
        horizontalAlignment = Alignment.Start
    ) {
        content()
    }
}

/** Small tracked uppercase label (headings and units). */
@Composable
internal fun WidgetLabelText(text: String, layout: WidgetLayout = WidgetLayout.REGULAR) {
    Text(
        text = text.uppercase(),
        maxLines = 1,
        style = TextStyle(
            color = WidgetColors.Label.provider,
            fontSize = when (layout) {
                WidgetLayout.COMPACT -> 9.sp
                WidgetLayout.ROW -> 10.sp
                else -> 11.sp
            },
            fontWeight = FontWeight.Medium
        )
    )
}

@Composable
internal fun WidgetHero(text: String, layout: WidgetLayout, color: ColorProvider = WidgetColors.Hero.provider) {
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(color = color, fontSize = heroSize(layout, text), fontWeight = FontWeight.Bold)
    )
}

@Composable
internal fun WidgetAccentText(text: String) {
    Text(
        text = text,
        maxLines = 1,
        style = TextStyle(color = WidgetColors.AccentText.provider, fontSize = 14.sp, fontWeight = FontWeight.Bold)
    )
}

@Composable
internal fun WidgetMutedText(text: String, maxLines: Int = 1, fontSize: TextUnit = 11.sp) {
    Text(text = text, maxLines = maxLines, style = TextStyle(color = WidgetColors.Muted.provider, fontSize = fontSize))
}

/** Whole percent of a 0..1 progress, for the progress bar's spoken description. */
internal fun progressPercent(progress: Float): Int = (progress.coerceIn(0f, 1f) * 100).roundToInt()

@Composable
internal fun WidgetProgress(progress: Float, description: String) {
    LinearProgressIndicator(
        progress = progress,
        modifier = GlanceModifier.fillMaxWidth().semantics { contentDescription = description },
        color = WidgetColors.Progress.provider,
        backgroundColor = WidgetColors.ProgressTrack.provider
    )
}

@Composable
internal fun WidgetGap(height: Int) {
    Spacer(modifier = GlanceModifier.height(height.dp))
}

/** 1×1 and one-row widgets: only two lines fit, the number and one label below it. */
@Composable
internal fun WidgetTwoLineBody(layout: WidgetLayout, hero: String, label: String, heroColor: ColorProvider = WidgetColors.Hero.provider) {
    WidgetHero(hero, layout, heroColor)
    if (label.isNotEmpty()) WidgetLabelText(label, layout)
}

/** Empty or not-paired state: the short text where space is tight, the full sentence otherwise. */
@Composable
internal fun WidgetEmpty(layout: WidgetLayout, short: String, long: String) {
    when (layout) {
        WidgetLayout.COMPACT, WidgetLayout.ROW -> WidgetMutedText(short, maxLines = 2)
        else -> WidgetMutedText(long, maxLines = 4, fontSize = 13.sp)
    }
}
