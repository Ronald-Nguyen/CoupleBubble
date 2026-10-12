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
import com.aistudio.couplebubble.qxztrw.model.Milestone
import com.aistudio.couplebubble.qxztrw.ui.milestoneTitle

/**
 * Local notifications for moments the partner created and for milestones reached today. Fed by the Firestore snapshot listener, so it fires while
 * the app process is alive (no push backend on the Spark plan).
 */
class AndroidMomentNotifier(context: Context) : MomentNotifier {
    private val appContext = context.applicationContext

    override fun notifyPartnerMoment(memoryId: String, partnerName: String, title: String) {
        post(
            id = memoryId.hashCode(),
            channelId = CHANNEL_MOMENTS,
            title = appContext.getString(R.string.notification_new_moment_title, partnerName),
            text = title.ifBlank { appContext.getString(R.string.notification_new_moment_untitled) }
        )
    }

    override fun notifyMilestoneReached(milestone: Milestone) {
        post(
            id = "milestone:${milestone.date}".hashCode(),
            channelId = CHANNEL_MILESTONES,
            title = appContext.getString(R.string.notification_milestone_title, milestoneTitle(appContext.resources, milestone)),
            text = appContext.getString(R.string.notification_milestone_text)
        )
    }

    private fun post(id: Int, channelId: String, title: String, text: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannels()
        val openApp = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, channelId)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(title)
            .setContentText(text)
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(appContext).notify(id, notification)
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

    private companion object {
        const val CHANNEL_MOMENTS = "partner_moments"
        const val CHANNEL_MILESTONES = "milestones"
    }
}
