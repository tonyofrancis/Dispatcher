package com.tonyodev.dispatcherapp

import android.app.Application
import leakcanary.LeakSentry

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        LeakSentry.manualInstall(this)
    }

}