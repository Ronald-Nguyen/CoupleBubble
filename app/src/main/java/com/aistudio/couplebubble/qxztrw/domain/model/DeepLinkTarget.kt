package com.aistudio.couplebubble.qxztrw.domain.model

import android.content.Intent
import android.net.Uri

import android.os.Build

/**
 * Destinations that can be deep-linked into from a notification, a homescreen widget, an external link,
 * or an ACTION_SEND share from another app.
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

    /** Opens the "Uns" tab and opens the "Neuer Moment" dialog. */
    data object NewMoment : DeepLinkTarget

    /** Opens the "Notizen" tab and creates and opens a new text note. */
    data object NewNote : DeepLinkTarget

    /** Opens the "Notizen" tab and creates and opens a new checklist. */
    data object NewList : DeepLinkTarget

    /** Opens the "Uns" tab and opens the "Neuer Moment" dialog with [photoUri] in the user's photo slot. */
    data class ShareImage(val photoUri: Uri) : DeepLinkTarget

    /** Opens the "Notizen" tab and creates a new note (or checklist if multiple bullet lines) with [text] and optional [title]. */
    data class ShareText(val text: String, val title: String = "") : DeepLinkTarget
}

/** Keys and target identifiers used in Intent extras and deep-link URIs. */
object DeepLinkContracts {
    const val EXTRA_TARGET = "extra_target"
    const val EXTRA_MEMORY_ID = "extra_memory_id"

    const val TARGET_MOMENT = "moment"
    const val TARGET_MILESTONES = "milestones"
    const val TARGET_REUNION = "reunion"
    const val TARGET_HOME = "home"
    const val TARGET_NEW_MOMENT = "new_moment"
    const val TARGET_NEW_NOTE = "new_note"
    const val TARGET_NEW_LIST = "new_list"

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
        val normalized = target.trim().lowercase().replace('-', '_')
        return when (normalized) {
            DeepLinkContracts.TARGET_MOMENT -> memoryId?.trim()?.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Moment(it) }
            DeepLinkContracts.TARGET_MILESTONES -> DeepLinkTarget.Milestones
            DeepLinkContracts.TARGET_REUNION -> DeepLinkTarget.Reunion
            DeepLinkContracts.TARGET_HOME -> DeepLinkTarget.Home
            DeepLinkContracts.TARGET_NEW_MOMENT -> DeepLinkTarget.NewMoment
            DeepLinkContracts.TARGET_NEW_NOTE -> DeepLinkTarget.NewNote
            DeepLinkContracts.TARGET_NEW_LIST -> DeepLinkTarget.NewList
            else -> null
        }
    }
    if (!uriString.isNullOrBlank()) {
        val uri = runCatching { Uri.parse(uriString) }.getOrNull() ?: return null
        if (uri.scheme?.lowercase() == DeepLinkContracts.URI_SCHEME) {
            val normalizedHost = uri.host?.lowercase()?.replace('-', '_')
            return when (normalizedHost) {
                DeepLinkContracts.TARGET_MOMENT -> {
                    val id = uri.lastPathSegment?.takeIf { it.lowercase() != DeepLinkContracts.TARGET_MOMENT }
                        ?: uri.getQueryParameter("id")
                    id?.trim()?.takeIf { it.isNotBlank() }?.let { DeepLinkTarget.Moment(it) }
                }
                DeepLinkContracts.TARGET_MILESTONES -> DeepLinkTarget.Milestones
                DeepLinkContracts.TARGET_REUNION -> DeepLinkTarget.Reunion
                DeepLinkContracts.TARGET_HOME -> DeepLinkTarget.Home
                DeepLinkContracts.TARGET_NEW_MOMENT -> DeepLinkTarget.NewMoment
                DeepLinkContracts.TARGET_NEW_NOTE -> DeepLinkTarget.NewNote
                DeepLinkContracts.TARGET_NEW_LIST -> DeepLinkTarget.NewList
                else -> null
            }
        }
    }
    return null
}

/**
 * Reads Intent extras and data to parse a [DeepLinkTarget].
 * Supports ACTION_SEND for images and plain text, as well as custom scheme deep-links and app shortcuts.
 */
fun parseDeepLink(intent: Intent?): DeepLinkTarget? {
    if (intent == null) return null
    // Extras come from other apps: unreadable or mistyped ones must not crash the activity
    return runCatching { parseIntent(intent) }.getOrNull()
}

private fun parseIntent(intent: Intent): DeepLinkTarget? {
    if (intent.action == Intent.ACTION_SEND) {
        val mimeType = intent.type?.lowercase()
        if (mimeType != null && mimeType.startsWith("image/")) {
            val uri = extractShareImageUri(intent)
            if (uri != null) return DeepLinkTarget.ShareImage(uri)
        }
        if (mimeType != null && (mimeType.startsWith("text/") || mimeType == "text/plain")) {
            val shareText = extractShareText(intent)
            if (shareText != null) return DeepLinkTarget.ShareText(text = shareText.first, title = shareText.second)
        }
        val uri = extractShareImageUri(intent)
        if (uri != null && (mimeType == null || mimeType.startsWith("image/") || mimeType == "*/*")) {
            return DeepLinkTarget.ShareImage(uri)
        }
        val shareText = extractShareText(intent)
        if (shareText != null) {
            return DeepLinkTarget.ShareText(text = shareText.first, title = shareText.second)
        }
        return null
    }

    val target = intent.getStringExtra(DeepLinkContracts.EXTRA_TARGET)
        ?: intent.extras?.getString(DeepLinkContracts.EXTRA_TARGET)
    val memoryId = intent.getStringExtra(DeepLinkContracts.EXTRA_MEMORY_ID)
        ?: intent.extras?.getString(DeepLinkContracts.EXTRA_MEMORY_ID)
    val uriString = intent.dataString
    return parseDeepLinkTarget(target = target, memoryId = memoryId, uriString = uriString)
}

/** Extracts the image [Uri] from an incoming share [Intent], checking ClipData, EXTRA_STREAM, and data. */
fun extractShareImageUri(intent: Intent): Uri? {
    val clipUri = intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.uri
    if (clipUri != null) return clipUri

    val streamUri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        intent.getParcelableExtra(Intent.EXTRA_STREAM, Uri::class.java)
    } else {
        @Suppress("DEPRECATION")
        intent.getParcelableExtra(Intent.EXTRA_STREAM)
    }
    return streamUri ?: intent.data
}

/** Extracts text and an optional title/subject from an incoming share [Intent]. */
fun extractShareText(intent: Intent): Pair<String, String>? {
    val text = intent.getStringExtra(Intent.EXTRA_TEXT)
        ?: intent.getCharSequenceExtra(Intent.EXTRA_TEXT)?.toString()
        ?: intent.clipData?.takeIf { it.itemCount > 0 }?.getItemAt(0)?.text?.toString()
    if (text.isNullOrBlank()) return null

    val title = intent.getStringExtra(Intent.EXTRA_SUBJECT)
        ?: intent.getCharSequenceExtra(Intent.EXTRA_SUBJECT)?.toString()
        ?: intent.getStringExtra(Intent.EXTRA_TITLE)
        ?: ""
    return text to title
}
