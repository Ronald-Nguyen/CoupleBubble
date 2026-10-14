package com.aistudio.couplebubble.qxztrw.domain.model

import android.content.Intent
import android.net.Uri

/**
 * Destinations that can be deep-linked into from a notification, a homescreen widget, or an external link.
 */
sealed interface DeepLinkTarget {
    /** Opens the "Uns" tab, scrolls to [memoryId], and highlights it briefly. */
    data class Moment(val memoryId: String) : DeepLinkTarget

    /** Opens the milestone settings dialog on the "Uns" tab. */
    data object Milestones : DeepLinkTarget

    /** Opens the reunion planning / editing dialog on the "Uns" tab. */
    data object Reunion : DeepLinkTarget

    /** Opens the "Uns" tab (home). */
    data object Home : DeepLinkTarget
}

/** Keys and target identifiers used in Intent extras and deep-link URIs. */
object DeepLinkContracts {
    const val EXTRA_TARGET = "extra_target"
    const val EXTRA_MEMORY_ID = "extra_memory_id"

    const val TARGET_MOMENT = "moment"
    const val TARGET_MILESTONES = "milestones"
    const val TARGET_REUNION = "reunion"
    const val TARGET_HOME = "home"

    const val URI_SCHEME = "couplebubble"
}

/** Distinct request codes for PendingIntents used in notifications. */
object PendingIntentRequestCodes {
    const val NOTIFICATION_MOMENTS_SUMMARY = 1001
    const val NOTIFICATION_MILESTONES_SUMMARY = 1002
    const val NOTIFICATION_MILESTONES_BASE = 2000
    const val NOTIFICATION_MOMENTS_BASE = 10_000

    fun notificationMoment(memoryId: String): Int =
        NOTIFICATION_MOMENTS_BASE + (memoryId.hashCode() and 0x000F_FFFF)

    fun notificationMilestone(key: String): Int =
        NOTIFICATION_MILESTONES_BASE + (key.hashCode() and 0x0000_1FFF)
}

/**
 * Pure parsing function to extract a [DeepLinkTarget] from target strings or a URI string.
 */
fun parseDeepLinkTarget(
    target: String?,
    memoryId: String? = null,
    uriString: String? = null
): DeepLinkTarget? {
    if (!target.isNullOrBlank()) {
        return when (target.trim().lowercase()) {
            DeepLinkContracts.TARGET_MOMENT -> memoryId?.trim()?.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Moment(it) }
            DeepLinkContracts.TARGET_MILESTONES -> DeepLinkTarget.Milestones
            DeepLinkContracts.TARGET_REUNION -> DeepLinkTarget.Reunion
            DeepLinkContracts.TARGET_HOME -> DeepLinkTarget.Home
            else -> null
        }
    }
    if (!uriString.isNullOrBlank()) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() == DeepLinkContracts.URI_SCHEME) {
            return when (uri.host?.lowercase()) {
                DeepLinkContracts.TARGET_MOMENT -> {
                    val id = uri.lastPathSegment?.takeIf { it.lowercase() != DeepLinkContracts.TARGET_MOMENT }
                        ?: uri.getQueryParameter("id")
                    id?.trim()?.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Moment(it) }
                }
                DeepLinkContracts.TARGET_MILESTONES -> DeepLinkTarget.Milestones
                DeepLinkContracts.TARGET_REUNION -> DeepLinkTarget.Reunion
                DeepLinkContracts.TARGET_HOME -> DeepLinkTarget.Home
                else -> null
            }
        }
    }
    return null
}

/**
 * Reads Intent extras and data to parse a [DeepLinkTarget].
 */
fun parseDeepLink(intent: Intent?): DeepLinkTarget? {
    if (intent == null) return null
    val target = intent.getStringExtra(DeepLinkContracts.EXTRA_TARGET)
        ?: intent.extras?.getString(DeepLinkContracts.EXTRA_TARGET)
    val memoryId = intent.getStringExtra(DeepLinkContracts.EXTRA_MEMORY_ID)
        ?: intent.extras?.getString(DeepLinkContracts.EXTRA_MEMORY_ID)
    val uriString = intent.dataString
    return parseDeepLinkTarget(target = target, memoryId = memoryId, uriString = uriString)
}
