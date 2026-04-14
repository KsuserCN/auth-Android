package cn.ksuser.auth.android

import android.app.Application
import cn.ksuser.auth.android.data.AppContainer

class KsuserAuthApplication : Application() {
    lateinit var appContainer: AppContainer
        private set

    override fun onCreate() {
        super.onCreate()
        appContainer = AppContainer(this)
    }
}
