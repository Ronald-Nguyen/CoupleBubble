package com.aistudio.couplebubble.qxztrw.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aistudio.couplebubble.qxztrw.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.model.DEFAULT_MILESTONE_KINDS
import com.aistudio.couplebubble.qxztrw.model.MilestoneKind
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.coupleDataStore: DataStore<Preferences> by preferencesDataStore(name = "couple_session_preferences")

class CoupleSessionPreferences(val context: Context) {

    companion object {
        val KEY_COUPLE_ID = stringPreferencesKey("key_active_couple_id")
        val KEY_PARTNER_ROLE = stringPreferencesKey("key_partner_role") // "A" or "B"
        // Device-only display settings; they survive clearSession()
        val KEY_COUNTER_DISPLAY_MODE = stringPreferencesKey("key_counter_display_mode")
        val KEY_ENABLED_MILESTONE_KINDS = stringSetPreferencesKey("key_enabled_milestone_kinds")
    }

    val activeCoupleIdFlow: Flow<String?> = context.coupleDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_COUPLE_ID]
        }

    suspend fun saveActiveCoupleId(coupleId: String) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_COUPLE_ID] = coupleId
        }
    }

    val partnerRoleFlow: Flow<String?> = context.coupleDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            preferences[KEY_PARTNER_ROLE]
        }

    suspend fun savePartnerRole(role: String) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_PARTNER_ROLE] = role
        }
    }

    val counterPreferencesFlow: Flow<CounterPreferences> = context.coupleDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }
        .map { preferences ->
            val mode = CounterDisplayMode.entries.firstOrNull { it.name == preferences[KEY_COUNTER_DISPLAY_MODE] }
            val kinds = preferences[KEY_ENABLED_MILESTONE_KINDS]
                ?.mapNotNull { name -> MilestoneKind.entries.firstOrNull { it.name == name } }
                ?.toSet()
            CounterPreferences(
                displayMode = mode ?: CounterDisplayMode.DAYS,
                enabledMilestoneKinds = kinds ?: DEFAULT_MILESTONE_KINDS
            )
        }

    suspend fun saveCounterDisplayMode(mode: CounterDisplayMode) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_COUNTER_DISPLAY_MODE] = mode.name
        }
    }

    suspend fun saveEnabledMilestoneKinds(kinds: Set<MilestoneKind>) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_ENABLED_MILESTONE_KINDS] = kinds.map { it.name }.toSet()
        }
    }

    suspend fun clearSession() {
        context.coupleDataStore.edit { preferences ->
            preferences.remove(KEY_COUPLE_ID)
            preferences.remove(KEY_PARTNER_ROLE)
        }
    }
}
