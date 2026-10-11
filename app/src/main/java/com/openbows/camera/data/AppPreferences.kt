/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

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
