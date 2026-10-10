package com.openbows.camera

import android.app.Application
import com.openbows.camera.data.AppContainer

class OpenbowsApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
