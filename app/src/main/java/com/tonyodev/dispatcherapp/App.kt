package com.tonyodev.dispatcherapp

import android.app.Application
import com.tonyodev.dispatchandroid.init

class App: Application() {

    override fun onCreate() {
        super.onCreate()
        init()
    }

}