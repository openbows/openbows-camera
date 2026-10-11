/*
 * Copyright (c) 2026 Sun, Jingwen (http://www.openbows.com)
 * Openbows-camera is licensed under Mulan PSL v2. You can use this software according to the terms and conditions of the Mulan PSL v2. You may obtain a copy of Mulan PSL v2 at: http://license.coscl.org.cn/MulanPSL2
 * THIS SOFTWARE IS PROVIDED ON AN "AS IS" BASIS, WITHOUT WARRANTIES OF ANY KIND, EITHER EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO NON-INFRINGEMENT, MERCHANTABILITY OR FIT FOR A PARTICULAR PURPOSE.
 * See the Mulan PSL v2 for more details.
 */

package com.openbows.camera.data

import android.content.Context
import androidx.room.Room
import com.openbows.camera.location.LocationTracker
import com.openbows.camera.location.TiandituRestClient
import com.openbows.camera.watermark.WatermarkRenderer

class AppContainer(context: Context) {
    private val appContext = context.applicationContext

    val database: AppDatabase = Room.databaseBuilder(
        appContext,
        AppDatabase::class.java,
        "openbows-camera.db",
    ).fallbackToDestructiveMigration().build()

    val preferences = AppPreferences(appContext)
    val templateRepository = TemplateRepository(database.templateDao(), preferences)
    val photoRepository = PhotoRepository(appContext, database.photoDao())
    val tiandituRest = TiandituRestClient()
    val locationTracker = LocationTracker(appContext, tiandituRest)
    val remarkHistoryRepository = RemarkHistoryRepository(database.remarkHistoryDao())
    val watermarkRenderer = WatermarkRenderer(appContext)
}
