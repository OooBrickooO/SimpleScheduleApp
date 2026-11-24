package com.example.simpleschedule

import android.app.Application
import com.example.simpleschedule.di.AppContainer

class SimpleScheduleApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
