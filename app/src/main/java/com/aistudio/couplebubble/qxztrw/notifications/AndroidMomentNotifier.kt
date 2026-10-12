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

/**
 * Local notification for moments the partner created. Fed by the Firestore snapshot listener, so it fires while
 * the app process is alive (no push backend on the Spark plan).
 */
class AndroidMomentNotifier(context: Context) : MomentNotifier {
    private val appContext = context.applicationContext

    override fun notifyPartnerMoment(memoryId: String, partnerName: String, title: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(appContext, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) return
        ensureChannel()
        val openApp = PendingIntent.getActivity(
            appContext,
            0,
            Intent(appContext, MainActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val notification = NotificationCompat.Builder(appContext, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle(appContext.getString(R.string.notification_new_moment_title, partnerName))
            .setContentText(title.ifBlank { appContext.getString(R.string.notification_new_moment_untitled) })
            .setContentIntent(openApp)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
        try {
            NotificationManagerCompat.from(appContext).notify(memoryId.hashCode(), notification)
        } catch (_: SecurityException) {
            // Permission revoked between the check and the post
        }
    }

    private fun ensureChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            appContext.getString(R.string.notification_channel_moments),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply { description = appContext.getString(R.string.notification_channel_moments_description) }
        appContext.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
    }

    private companion object {
        const val CHANNEL_ID = "partner_moments"
    }
}
