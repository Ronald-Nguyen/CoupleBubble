package com.aistudio.couplebubble.qxztrw.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.aistudio.couplebubble.qxztrw.CoupleBubbleApplication
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Redraws the widgets after midnight, a time/zone change or a reboot, then schedules the next midnight. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        val dispatchers = (context.applicationContext as CoupleBubbleApplication).container.dispatchers
        CoroutineScope(SupervisorJob() + dispatchers.default).launch {
            try {
                AndroidWidgetPublisher.refreshWidgets(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
