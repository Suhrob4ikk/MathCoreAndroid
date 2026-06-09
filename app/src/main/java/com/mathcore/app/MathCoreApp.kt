package com.mathcore.app

import android.app.Application
import com.mathcore.app.data.repository.SessionManager
import com.mathcore.app.util.AppHttpClient
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class MathCoreApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Восстанавливаем сессию при старте приложения
        SessionManager.init(this)
    }

    override fun onTerminate() {
        super.onTerminate()
        // Release the shared Ktor clients (closes connection pools / threads).
        // onTerminate() is called in emulator/test environments; on real devices
        // the process is killed directly, so the OS reclaims resources anyway.
        AppHttpClient.close()
    }
}
