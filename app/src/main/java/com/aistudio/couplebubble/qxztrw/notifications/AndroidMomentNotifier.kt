package com.aistudio.couplebubble.qxztrw.notifications

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.aistudio.couplebubble.qxztrw.MainActivity
import com.aistudio.couplebubble.qxztrw.R
import com.aistudio.couplebubble.qxztrw.domain.model.DeepLinkContracts
import com.aistudio.couplebubble.qxztrw.domain.model.Milestone
import com.aistudio.couplebubble.qxztrw.domain.model.PendingIntentRequestCodes
import com.aistudio.couplebubble.qxztrw.ui.milestoneTitle

/**
 * Local notifications for moments the partner created and for milestones reached today. Fed by the Firestore snapshot listener, so it fires while
 * the app process is alive (no push backend on the Spark plan).
 */
class AndroidMomentNotifier(context: Context) : MomentNotifier {
    private val appContext = context.applicationContext

    override fun notifyPartnerMoment(memoryId: String, partnerName: String, title: String) {
        val childIntent = createPendingIntent(
            requestCode = PendingIntentRequestCodes.notificationMoment(memoryId),
            target = DeepLinkContracts.TARGET_MOMENT,
            memoryId = memoryId
        )
        val summaryIntent = createPendingIntent(
            requestCode = PendingIntentRequestCodes.NOTIFICATION_MOMENTS_SUMMARY,
            target = DeepLinkContracts.TARGET_HOME
        )
        postGrouped(
            childId = memoryId.hashCode(),
            summaryId = SUMMARY_ID_MOMENTS,
            channelId = CHANNEL_MOMENTS,
            groupKey = GROUP_MOMENTS,
            childTitle = appContext.getString(R.string.notification_new_moment_title, partnerName),
            childText = title.ifBlank { appContext.getString(R.string.notification_new_moment_untitled) },
            childIntent = childIntent,
            summaryTitle = appContext.getString(R.string.notification_channel_moments),
            summaryText = appContext.getString(R.string.notification_summary_moments),
            summaryIntent = summaryIntent
        )
    }

    override fun notifyMilestoneReached(milestone: Milestone) {
        val childIntent = createPendingIntent(
            requestCode = PendingIntentRequestCodes.notificationMilestone(milestone.date.toString()),
            target = DeepLinkContracts.TARGET_MILESTONES
        )
        val summaryIntent = createPendingIntent(
            requestCode = PendingIntentRequestCodes.NOTIFICATION_MILESTONES_SUMMARY,
            target = DeepLinkContracts.TARGET_MILESTONES
        )
        postGrouped(
            childId = "milestone:${milestone.date}".hashCode(),
            summaryId = SUMMARY_ID_MILESTONES,
            channelId = CHANNEL_MILESTONES,
            groupKey = GROUP_MILESTONES,
            childTitle = appContext.getString(R.string.notification_milestone_title, milestoneTitle(appContext.resources, milestone)),
            childText = appContext.getString(R.string.notification_milestone_text),
            childIntent = childIntent,
            summaryTitle = appContext.getString(R.string.notification_channel_milestones),
            summaryText = appContext.getString(R.string.notification_summary_milestones),
            summaryIntent = summaryIntent
        )
    }

    private fun createPendingIntent(requestCode: Int, target: String, memoryId: String? = null): PendingIntent {
        val intent = Intent(appContext, MainActivity::class.java).apply {
            action = Intent.ACTION_VIEW
            addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP)
            putExtra(DeepLinkContracts.EXTRA_TARGET, target)
            if (memoryId != null) {
                putExtra(DeepLinkContracts.EXTRA_MEMORY_ID, memoryId)
            }
        }
        return PendingIntent.getActivity(
            appContext,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }

    private fun postGrouped(
        childId: Int,
        summaryId: Int,
        channelId: String,
        groupKey: String,
        childTitle: String,
        childText: String,
        childIntent: PendingIntent,
        summaryTitle: String,
        summaryText: String,
        summaryIntent: PendingIntent
    ) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannels()

        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(childTitle)
            .setContentText(childText)
            .setStyle(NotificationCompat.BigTextStyle().bigText(childText))
            .setContentIntent(childIntent)
            .setGroup(groupKey)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        val summary = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_stat_notification)
            .setContentTitle(summaryTitle)
            .setContentText(summaryText)
            .setContentIntent(summaryIntent)
            .setGroup(groupKey)
            .setGroupSummary(true)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        try {
            val notificationManager = NotificationManagerCompat.from(appContext)
            notificationManager.notify(childId, notification)
            notificationManager.notify(summaryId, summary)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post
        }
    }

    private fun ensureChannels() {
        val manager = appContext.getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MOMENTS,
                appContext.getString(R.string.notification_channel_moments),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = appContext.getString(R.string.notification_channel_moments_description) }
        )
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_MILESTONES,
                appContext.getString(R.string.notification_channel_milestones),
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply { description = appContext.getString(R.string.notification_channel_milestones_description) }
        )
    }

    companion object {
        const val CHANNEL_MOMENTS = "partner_moments"
        const val CHANNEL_MILESTONES = "milestones"
        const val GROUP_MOMENTS = "com.aistudio.couplebubble.qxztrw.MOMENTS"
        const val GROUP_MILESTONES = "com.aistudio.couplebubble.qxztrw.MILESTONES"
        const val SUMMARY_ID_MOMENTS = 9001
        const val SUMMARY_ID_MILESTONES = 9002
    }
}
