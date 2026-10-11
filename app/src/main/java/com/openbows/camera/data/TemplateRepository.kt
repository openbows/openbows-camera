/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.openbows.camera.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.first

class TemplateRepository(
    private val dao: TemplateDao,
    private val preferences: AppPreferences,
) {
    val templates: Flow<List<WatermarkTemplate>> = dao.observeAll()

    val currentTemplate: Flow<WatermarkTemplate?> = combine(
        dao.observeAll(),
        preferences.currentTemplateIdFlow,
    ) { list, currentId ->
        list.firstOrNull { it.id == currentId }
            ?: list.firstOrNull { it.name == "现场巡检" }
            ?: list.firstOrNull()
    }.distinctUntilChanged()

    suspend fun ensureDefault() {
        val names = listOf("现场巡检", "取证拍照", "施工验收", "安全检查")
        val all = dao.getAll()
        names.forEach { name ->
            if (all.none { it.name == name }) {
                dao.insert(builtinTemplate(name))
            }
        }
        val inspection = dao.getAll().firstOrNull { it.name == "现场巡检" }
        val current = preferences.currentTemplateIdFlow.first()
        val ids = dao.getAll().map { it.id }
        if (current == 0L || current !in ids) {
            preferences.setCurrentTemplateId(inspection?.id ?: 0L)
        }
        if (!preferences.mediumFontMigratedFlow.first()) {
            dao.getAll().forEach { template ->
                if (template.fontSize != FontSizeChoice.MEDIUM) {
                    dao.update(template.copy(fontSize = FontSizeChoice.MEDIUM))
                }
            }
            preferences.setMediumFontMigrated(true)
        }
    }

    private fun builtinTemplate(name: String) = WatermarkTemplate(
        name = name,
        showTime = true,
        showAddress = true,
        showLatLng = true,
        showCoordSystem = true,
        showAltitude = true,
        showWeather = true,
        showRemark = true,
        remark = "",
        fontSize = FontSizeChoice.MEDIUM,
    )

    suspend fun get(id: Long): WatermarkTemplate? = dao.getById(id)

    suspend fun save(template: WatermarkTemplate): Long {
        return if (template.id == 0L) {
            dao.insert(template.copy(updatedAt = System.currentTimeMillis()))
        } else {
            dao.update(template.copy(updatedAt = System.currentTimeMillis()))
            template.id
        }
    }

    suspend fun setCurrent(id: Long) {
        preferences.setCurrentTemplateId(id)
    }

    suspend fun delete(template: WatermarkTemplate) {
        dao.delete(template)
        val remaining = dao.getAll()
        val current = preferences.currentTemplateIdFlow.first()
        if (remaining.isEmpty()) {
            ensureDefault()
        } else if (current == template.id) {
            val next = remaining.firstOrNull { it.name == "现场巡检" } ?: remaining.first()
            preferences.setCurrentTemplateId(next.id)
        }
    }

    suspend fun duplicate(template: WatermarkTemplate): Long {
        return dao.insert(
            template.copy(
                id = 0,
                name = template.name + " 副本",
                updatedAt = System.currentTimeMillis(),
            ),
        )
    }

    suspend fun create(name: String = "未命名模板"): Long {
        return dao.insert(
            WatermarkTemplate(name = name, updatedAt = System.currentTimeMillis()),
        )
    }
}
