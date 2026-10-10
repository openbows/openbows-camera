package com.openbows.camera.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "openbows_prefs")

class AppPreferences(private val context: Context) {
    private val currentTemplateId = longPreferencesKey("current_template_id")
    private val coordSystemKey = stringPreferencesKey("coord_system")
    private val mediumFontMigratedKey = booleanPreferencesKey("font_size_medium_migrated")

    val currentTemplateIdFlow: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[currentTemplateId] ?: 0L
    }

    val coordSystemFlow: Flow<CoordSystem> = context.dataStore.data.map { prefs ->
        runCatching { CoordSystem.valueOf(prefs[coordSystemKey] ?: CoordSystem.WGS84.name) }
            .getOrDefault(CoordSystem.WGS84)
    }

    val mediumFontMigratedFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[mediumFontMigratedKey] ?: false
    }

    suspend fun setCurrentTemplateId(id: Long) {
        context.dataStore.edit { it[currentTemplateId] = id }
    }

    suspend fun setCoordSystem(system: CoordSystem) {
        context.dataStore.edit { it[coordSystemKey] = system.name }
    }

    suspend fun setMediumFontMigrated(done: Boolean) {
        context.dataStore.edit { it[mediumFontMigratedKey] = done }
    }
}
