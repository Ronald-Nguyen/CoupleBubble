package com.aistudio.couplebubble.qxztrw.widget

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import androidx.glance.appwidget.updateAll
import com.aistudio.couplebubble.qxztrw.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId

private val Context.widgetDataStore by preferencesDataStore(name = "widget_snapshot")

/** Persists the [WidgetSnapshot] for the widget process and refreshes all widgets. */
class AndroidWidgetPublisher(context: Context) : WidgetPublisher {

    private val appContext = context.applicationContext

    override suspend fun publish(snapshot: WidgetSnapshot?) {
        val json = snapshot?.let(::encode)
        val current = appContext.widgetDataStore.data.catchIo().first()[KEY_SNAPSHOT]
        if (current == json) return
        appContext.widgetDataStore.edit { preferences ->
            if (json == null) preferences.remove(KEY_SNAPSHOT) else preferences[KEY_SNAPSHOT] = json
        }
        refreshWidgets(appContext)
    }

    companion object {
        private val KEY_SNAPSHOT = stringPreferencesKey("key_widget_snapshot")
        private const val MIDNIGHT_REQUEST_CODE = 4711

        suspend fun read(context: Context): WidgetSnapshot? =
            context.applicationContext.widgetDataStore.data.catchIo().first()[KEY_SNAPSHOT]
                ?.let { runCatching { decode(it) }.getOrNull() }

        suspend fun refreshWidgets(context: Context) {
            CoupleBubbleWidget().updateAll(context)
            MilestoneWidget().updateAll(context)
            scheduleMidnightRefresh(context)
        }

        /** Inexact alarm shortly after midnight so counters and countdowns roll over without opening the app. */
        fun scheduleMidnightRefresh(context: Context) {
            val alarmManager = context.getSystemService(AlarmManager::class.java) ?: return
            val zone = ZoneId.systemDefault()
            val triggerAt = LocalDate.now(zone).plusDays(1).atStartOfDay(zone).toInstant().toEpochMilli() + 60_000L
            val intent = Intent(context, WidgetRefreshReceiver::class.java)
            val pendingIntent = PendingIntent.getBroadcast(
                context,
                MIDNIGHT_REQUEST_CODE,
                intent,
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            alarmManager.set(AlarmManager.RTC, triggerAt, pendingIntent)
        }

        private fun kotlinx.coroutines.flow.Flow<Preferences>.catchIo() =
            catch { if (it is IOException) emit(emptyPreferences()) else throw it }

        private fun encode(snapshot: WidgetSnapshot): String = JSONObject()
            .put("p1", snapshot.partner1Name)
            .put("p2", snapshot.partner2Name)
            .put("y", snapshot.anniversaryYear)
            .put("m", snapshot.anniversaryMonth)
            .put("d", snapshot.anniversaryDay)
            .put("kinds", JSONArray(snapshot.enabledMilestoneKinds.map { it.name }.sorted()))
            .put(
                "custom",
                JSONArray(
                    snapshot.customMilestones.map { custom ->
                        JSONObject()
                            .put("id", custom.id)
                            .put("title", custom.title)
                            .put("date", custom.date.toString())
                            .put("yearly", custom.recurringYearly)
                            .put("enabled", custom.enabled)
                    }
                )
            )
            .toString()

        private fun decode(json: String): WidgetSnapshot {
            val root = JSONObject(json)
            val kinds = root.getJSONArray("kinds").let { array ->
                (0 until array.length()).mapNotNull { index ->
                    MilestoneKind.entries.firstOrNull { it.name == array.getString(index) }
                }.toSet()
            }
            val custom = root.getJSONArray("custom").let { array ->
                (0 until array.length()).map { index ->
                    val item = array.getJSONObject(index)
                    CustomMilestone(
                        id = item.getString("id"),
                        title = item.getString("title"),
                        date = LocalDate.parse(item.getString("date")),
                        recurringYearly = item.getBoolean("yearly"),
                        enabled = item.getBoolean("enabled")
                    )
                }
            }
            return WidgetSnapshot(
                partner1Name = root.getString("p1"),
                partner2Name = root.getString("p2"),
                anniversaryYear = root.getInt("y"),
                anniversaryMonth = root.getInt("m"),
                anniversaryDay = root.getInt("d"),
                enabledMilestoneKinds = kinds,
                customMilestones = custom
            )
        }
    }
}
