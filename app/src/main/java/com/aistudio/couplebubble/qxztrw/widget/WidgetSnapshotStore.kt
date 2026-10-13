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
import com.aistudio.couplebubble.qxztrw.domain.model.CustomMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import java.io.IOException
import java.time.LocalDate
import java.time.ZoneId
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import org.json.JSONArray
import org.json.JSONObject

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

        // JSON keys of the stored snapshot; renaming one makes widgets lose their data until the app runs again
        private const val KEY_PARTNER_1 = "p1"
        private const val KEY_PARTNER_2 = "p2"
        private const val KEY_YEAR = "y"
        private const val KEY_MONTH = "m"
        private const val KEY_DAY = "d"
        private const val KEY_KINDS = "kinds"
        private const val KEY_CUSTOM = "custom"
        private const val KEY_ID = "id"
        private const val KEY_TITLE = "title"
        private const val KEY_DATE = "date"
        private const val KEY_YEARLY = "yearly"
        private const val KEY_ENABLED = "enabled"

        private fun encode(snapshot: WidgetSnapshot): String = JSONObject()
            .put(KEY_PARTNER_1, snapshot.partner1Name)
            .put(KEY_PARTNER_2, snapshot.partner2Name)
            .put(KEY_YEAR, snapshot.anniversaryYear)
            .put(KEY_MONTH, snapshot.anniversaryMonth)
            .put(KEY_DAY, snapshot.anniversaryDay)
            .put(KEY_KINDS, JSONArray(snapshot.enabledMilestoneKinds.map { it.name }.sorted()))
            .put(KEY_CUSTOM,
                JSONArray(
                    snapshot.customMilestones.map { custom ->
                        JSONObject()
                            .put(KEY_ID, custom.id)
                            .put(KEY_TITLE, custom.title)
                            .put(KEY_DATE, custom.date.toString())
                            .put(KEY_YEARLY, custom.recurringYearly)
                            .put(KEY_ENABLED, custom.enabled)
                    }
                )
            )
            .toString()

        private fun decode(json: String): WidgetSnapshot {
            val root = JSONObject(json)
            val kinds = root.getJSONArray(KEY_KINDS).let { array ->
                (0 until array.length()).mapNotNull { index ->
                    MilestoneKind.entries.firstOrNull { it.name == array.getString(index) }
                }.toSet()
            }
            val custom = root.getJSONArray(KEY_CUSTOM).let { array ->
                (0 until array.length()).map { index ->
                    val item = array.getJSONObject(index)
                    CustomMilestone(
                        id = item.getString(KEY_ID),
                        title = item.getString(KEY_TITLE),
                        date = LocalDate.parse(item.getString(KEY_DATE)),
                        recurringYearly = item.getBoolean(KEY_YEARLY),
                        enabled = item.getBoolean(KEY_ENABLED)
                    )
                }
            }
            return WidgetSnapshot(
                partner1Name = root.getString(KEY_PARTNER_1),
                partner2Name = root.getString(KEY_PARTNER_2),
                anniversaryYear = root.getInt(KEY_YEAR),
                anniversaryMonth = root.getInt(KEY_MONTH),
                anniversaryDay = root.getInt(KEY_DAY),
                enabledMilestoneKinds = kinds,
                customMilestones = custom
            )
        }
    }
}
