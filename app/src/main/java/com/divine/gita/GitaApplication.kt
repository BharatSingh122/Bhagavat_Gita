package com.divine.gita

import android.app.Application
import com.divine.gita.di.AppContainer

class GitaApplication : Application() {
    lateinit var container: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        container = AppContainer(this)
    }
}
