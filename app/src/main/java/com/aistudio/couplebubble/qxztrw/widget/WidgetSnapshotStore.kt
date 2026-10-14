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
import com.aistudio.couplebubble.qxztrw.domain.model.GeoPoint
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.MomentMilestone
import com.aistudio.couplebubble.qxztrw.domain.model.Reunion
import com.aistudio.couplebubble.qxztrw.domain.model.ReunionPlace
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
            ReunionWidget().updateAll(context)
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
        private const val KEY_MOMENTS = "moments"
        private const val KEY_REUNION = "reunion"
        private const val KEY_START = "start"
        private const val KEY_END = "end"
        private const val KEY_PLANNED = "planned"
        private const val KEY_PLACES = "places"
        private const val KEY_SHARES = "shares"
        private const val KEY_LAT = "lat"
        private const val KEY_LNG = "lng"
        private const val KEY_LOCATED_AT = "at"

        internal fun encode(snapshot: WidgetSnapshot): String = JSONObject()
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
            .put(KEY_MOMENTS,
                JSONArray(
                    snapshot.momentMilestones.map { moment ->
                        JSONObject()
                            .put(KEY_ID, moment.memoryId)
                            .put(KEY_TITLE, moment.title)
                            .put(KEY_DATE, moment.date.toString())
                    }
                )
            )
            .putOpt(KEY_REUNION, snapshot.reunion?.let(::encodeReunion))
            .toString()

        /** Places go without their UIDs: the widget only needs them for the (symmetric) distance. */
        private fun encodeReunion(reunion: Reunion): JSONObject = JSONObject()
            .put(KEY_START, reunion.startDate.toString())
            .put(KEY_END, reunion.endDate.toString())
            .put(KEY_PLANNED, reunion.plannedAtDate.toString())
            .put(KEY_PLACES,
                JSONArray(
                    reunion.places.values.map { place ->
                        JSONObject()
                            .put(KEY_SHARES, place.sharesLocation)
                            .putOpt(KEY_LAT, place.location?.latitude)
                            .putOpt(KEY_LNG, place.location?.longitude)
                            .putOpt(KEY_LOCATED_AT, place.locatedAtMillis)
                    }
                )
            )

        private fun decodeReunion(item: JSONObject): Reunion {
            val places = item.getJSONArray(KEY_PLACES).let { array ->
                (0 until array.length()).associate { index ->
                    val place = array.getJSONObject(index)
                    val location = if (place.has(KEY_LAT) && place.has(KEY_LNG)) {
                        GeoPoint(place.getDouble(KEY_LAT), place.getDouble(KEY_LNG))
                    } else {
                        null
                    }
                    index.toString() to ReunionPlace(
                        sharesLocation = place.getBoolean(KEY_SHARES),
                        location = location,
                        locatedAtMillis = if (place.has(KEY_LOCATED_AT)) place.getLong(KEY_LOCATED_AT) else null
                    )
                }
            }
            return Reunion(
                startDate = LocalDate.parse(item.getString(KEY_START)),
                endDate = LocalDate.parse(item.getString(KEY_END)),
                plannedAtDate = LocalDate.parse(item.getString(KEY_PLANNED)),
                places = places
            )
        }

        internal fun decode(json: String): WidgetSnapshot {
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
            // Snapshots written before moment milestones existed have no list
            val moments = root.optJSONArray(KEY_MOMENTS)?.let { array ->
                (0 until array.length()).map { index ->
                    val item = array.getJSONObject(index)
                    MomentMilestone(
                        memoryId = item.getString(KEY_ID),
                        title = item.getString(KEY_TITLE),
                        date = LocalDate.parse(item.getString(KEY_DATE))
                    )
                }
            }.orEmpty()
            return WidgetSnapshot(
                partner1Name = root.getString(KEY_PARTNER_1),
                partner2Name = root.getString(KEY_PARTNER_2),
                anniversaryYear = root.getInt(KEY_YEAR),
                anniversaryMonth = root.getInt(KEY_MONTH),
                anniversaryDay = root.getInt(KEY_DAY),
                enabledMilestoneKinds = kinds,
                customMilestones = custom,
                momentMilestones = moments,
                // Snapshots written before the reunion widget have none
                reunion = root.optJSONObject(KEY_REUNION)?.let(::decodeReunion)
            )
        }
    }
}
