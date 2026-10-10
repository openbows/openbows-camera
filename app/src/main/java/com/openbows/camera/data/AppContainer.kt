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
