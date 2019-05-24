package com.tonyodev.dispatcherapp

import android.app.Application
import com.tonyodev.dispatchandroid.initAndroidDispatchQueues

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        initAndroidDispatchQueues()
    }

}