package com.aistudio.couplebubble.qxztrw.ui

import android.content.res.Resources
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.model.Milestone
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import java.text.NumberFormat
import java.util.Locale

/** Display title of a milestone, shared by the dashboard and the homescreen widget. */
fun milestoneTitle(resources: Resources, milestone: Milestone): String {
    val count = NumberFormat.getIntegerInstance(Locale.GERMANY).format(milestone.value)
    return when (milestone.kind) {
        MilestoneKind.DAYS -> resources.getQuantityString(R.plurals.milestone_title_days, milestone.value, count)
        MilestoneKind.WEEKS -> resources.getQuantityString(R.plurals.milestone_title_weeks, milestone.value, count)
        MilestoneKind.MONTHS -> resources.getString(R.string.milestone_title_monthiversary, milestone.value)
        MilestoneKind.YEARS -> resources.getString(R.string.milestone_title_anniversary, milestone.value)
        MilestoneKind.CUSTOM -> milestone.customTitle.orEmpty()
    }
}

/** "heute" or "in N Tagen" for the countdown next to a milestone. */
fun milestoneCountdown(resources: Resources, daysUntil: Long): String {
    val days = daysUntil.toInt()
    return if (days == 0) {
        resources.getString(R.string.milestone_today)
    } else {
        resources.getQuantityString(R.plurals.milestone_in_days, days, days)
    }
}
