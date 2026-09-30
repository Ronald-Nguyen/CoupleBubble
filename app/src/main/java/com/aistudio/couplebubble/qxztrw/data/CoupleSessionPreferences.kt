package com.aistudio.couplebubble.qxztrw.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.map
import java.io.IOException

val Context.coupleDataStore: DataStore<Preferences> by preferencesDataStore(name = "couple_session_preferences")

class CoupleSessionPreferences(val context: Context) {

    companion object {
        val KEY_COUPLE_ID = stringPreferencesKey("key_active_couple_id")
        val KEY_PARTNER_ROLE = stringPreferencesKey("key_partner_role") // "A" or "B"
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

    suspend fun clearSession() {
        context.coupleDataStore.edit { preferences ->
            preferences.remove(KEY_COUPLE_ID)
            preferences.remove(KEY_PARTNER_ROLE)
        }
    }
}
