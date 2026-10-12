package com.aistudio.couplebubble.qxztrw.widget

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

/** Redraws the widgets after midnight, a time/zone change or a reboot, then schedules the next midnight. */
class WidgetRefreshReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val pending = goAsync()
        CoroutineScope(SupervisorJob() + Dispatchers.Default).launch {
            try {
                AndroidWidgetPublisher.refreshWidgets(context.applicationContext)
            } finally {
                pending.finish()
            }
        }
    }
}
