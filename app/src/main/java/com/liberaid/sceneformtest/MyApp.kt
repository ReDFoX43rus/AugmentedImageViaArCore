package com.liberaid.sceneformtest

import android.app.Application
import android.os.SystemClock
import timber.log.Timber

class MyApp : Application() {

    override fun onCreate() {
        super.onCreate()

        Timber.plant(CustomTimberTree(listOf()))

        Timber.d("App start=${SystemClock.elapsedRealtime()}")
    }

}