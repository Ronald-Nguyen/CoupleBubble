package com.aistudio.couplebubble.qxztrw.ui

import android.content.res.Resources
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.RelationshipMetrics
import java.text.NumberFormat
import java.util.Locale

/** The together counter in one [CounterDisplayMode]: "412" + "Tage", or "1" + "Jahr" + "1 Monat · 17 Tage". */
data class CounterText(val value: String, val unit: String, val subline: String?)

/** Shared by the dashboard's together card and the homescreen widget, so both follow the chosen mode. */
fun counterText(resources: Resources, metrics: RelationshipMetrics, mode: CounterDisplayMode): CounterText {
    val (value, unitRes) = when (mode) {
        CounterDisplayMode.DAYS -> metrics.totalDays to R.plurals.counter_unit_days
        CounterDisplayMode.WEEKS -> metrics.totalWeeks to R.plurals.counter_unit_weeks
        CounterDisplayMode.MONTHS -> metrics.totalMonths to R.plurals.counter_unit_months
        CounterDisplayMode.YEARS -> metrics.years.toLong() to R.plurals.counter_unit_years
    }
    val subline = when (mode) {
        CounterDisplayMode.DAYS -> null
        CounterDisplayMode.WEEKS -> metrics.weekRemainderDays.takeIf { it > 0 }?.let {
            resources.getQuantityString(R.plurals.counter_and_days, it, it)
        }
        CounterDisplayMode.MONTHS -> metrics.monthRemainderDays.takeIf { it > 0 }?.let {
            resources.getQuantityString(R.plurals.counter_and_days, it, it)
        }
        CounterDisplayMode.YEARS -> listOf(
            resources.getQuantityString(R.plurals.months_count, metrics.months, metrics.months),
            resources.getQuantityString(R.plurals.days_count, metrics.days, metrics.days)
        ).joinToString(separator = " · ")
    }
    return CounterText(
        value = NumberFormat.getIntegerInstance(Locale.GERMANY).format(value),
        unit = resources.getQuantityString(unitRes, value.toInt()),
        subline = subline
    )
}
