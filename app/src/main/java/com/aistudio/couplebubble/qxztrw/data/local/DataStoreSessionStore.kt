package com.aistudio.couplebubble.qxztrw.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aistudio.couplebubble.qxztrw.domain.model.CounterDisplayMode
import com.aistudio.couplebubble.qxztrw.domain.model.CounterPreferences
import com.aistudio.couplebubble.qxztrw.domain.model.DEFAULT_MILESTONE_KINDS
import com.aistudio.couplebubble.qxztrw.domain.model.GestureHint
import com.aistudio.couplebubble.qxztrw.domain.model.MilestoneKind
import com.aistudio.couplebubble.qxztrw.domain.model.PartnerRole
import java.io.IOException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map

val Context.coupleDataStore: DataStore<Preferences> by preferencesDataStore(name = "couple_session_preferences")

class DataStoreSessionStore(private val context: Context) : SessionStore {

    companion object {
        val KEY_COUPLE_ID = stringPreferencesKey("key_active_couple_id")
        val KEY_PARTNER_ROLE = stringPreferencesKey("key_partner_role") // "1" or "2" (legacy installs: "A" or "B"), see PartnerRole
        // Device-only display settings; they survive clearSession()
        val KEY_ONBOARDING_COMPLETED = booleanPreferencesKey("key_onboarding_completed")
        val KEY_COUNTER_DISPLAY_MODE = stringPreferencesKey("key_counter_display_mode")
        val KEY_ENABLED_MILESTONE_KINDS = stringSetPreferencesKey("key_enabled_milestone_kinds")
        val KEY_LAST_NOTIFIED_MILESTONE = stringPreferencesKey("key_last_notified_milestone")
        fun keyNotesLastSeen(coupleId: String) = longPreferencesKey("key_notes_last_seen_$coupleId")
        val KEY_HINT_NOTE_LONG_PRESS = booleanPreferencesKey("key_hint_note_long_press_dismissed")
        val KEY_HINT_FULLSCREEN_SWIPE = booleanPreferencesKey("key_hint_fullscreen_swipe_dismissed")
        val KEY_HINT_MILESTONE_TAP = booleanPreferencesKey("key_hint_milestone_tap_dismissed")
        val KEY_HINT_CHECKLIST_DRAG = booleanPreferencesKey("key_hint_checklist_drag_dismissed")
    }

    // A corrupt or unreadable file reads as empty instead of crashing the collectors
    private val safeData: Flow<Preferences> = context.coupleDataStore.data
        .catch { exception ->
            if (exception is IOException) {
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }

    override val activeCoupleIdFlow: Flow<String?> = safeData
        .map { preferences ->
            preferences[KEY_COUPLE_ID]
        }

    override suspend fun saveActiveCoupleId(coupleId: String) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_COUPLE_ID] = coupleId
        }
    }

    override val partnerRoleFlow: Flow<String?> = safeData
        .map { preferences ->
            preferences[KEY_PARTNER_ROLE]
        }

    override suspend fun savePartnerRole(role: PartnerRole) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_PARTNER_ROLE] = role.storedValue
        }
    }

    override val onboardingCompletedFlow: Flow<Boolean> = safeData
        .map { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] ?: false
        }

    override suspend fun saveOnboardingCompleted(completed: Boolean) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_ONBOARDING_COMPLETED] = completed
        }
    }

    override val counterPreferencesFlow: Flow<CounterPreferences> = safeData
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

    override suspend fun saveCounterDisplayMode(mode: CounterDisplayMode) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_COUNTER_DISPLAY_MODE] = mode.name
        }
    }

    override suspend fun saveEnabledMilestoneKinds(kinds: Set<MilestoneKind>) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_ENABLED_MILESTONE_KINDS] = kinds.map { it.name }.toSet()
        }
    }

    override val lastNotifiedMilestoneFlow: Flow<String?> = safeData.map { it[KEY_LAST_NOTIFIED_MILESTONE] }

    override suspend fun saveLastNotifiedMilestone(key: String) {
        context.coupleDataStore.edit { preferences ->
            preferences[KEY_LAST_NOTIFIED_MILESTONE] = key
        }
    }

    override fun notesLastSeenFlow(coupleId: String): Flow<Long> = safeData.map { preferences ->
        preferences[keyNotesLastSeen(coupleId)] ?: 0L
    }

    override suspend fun saveNotesLastSeen(coupleId: String, timestamp: Long) {
        context.coupleDataStore.edit { preferences ->
            preferences[keyNotesLastSeen(coupleId)] = timestamp
        }
    }

    override val dismissedGestureHintsFlow: Flow<Set<GestureHint>> = safeData.map { preferences ->
        buildSet {
            if (preferences[KEY_HINT_NOTE_LONG_PRESS] == true) add(GestureHint.NOTE_LONG_PRESS)
            if (preferences[KEY_HINT_FULLSCREEN_SWIPE] == true) add(GestureHint.FULLSCREEN_PHOTO_SWIPE)
            if (preferences[KEY_HINT_MILESTONE_TAP] == true) add(GestureHint.MILESTONE_TAP)
            if (preferences[KEY_HINT_CHECKLIST_DRAG] == true) add(GestureHint.CHECKLIST_DRAG)
        }
    }

    override suspend fun dismissGestureHint(hint: GestureHint) {
        context.coupleDataStore.edit { preferences ->
            val key = when (hint) {
                GestureHint.NOTE_LONG_PRESS -> KEY_HINT_NOTE_LONG_PRESS
                GestureHint.FULLSCREEN_PHOTO_SWIPE -> KEY_HINT_FULLSCREEN_SWIPE
                GestureHint.MILESTONE_TAP -> KEY_HINT_MILESTONE_TAP
                GestureHint.CHECKLIST_DRAG -> KEY_HINT_CHECKLIST_DRAG
            }
            preferences[key] = true
        }
    }

    override suspend fun clearSession() {
        context.coupleDataStore.edit { preferences ->
            preferences.remove(KEY_COUPLE_ID)
            preferences.remove(KEY_PARTNER_ROLE)
        }
    }
}
