package com.gseterminal.app

import android.app.Application
import android.webkit.WebView

class GSEApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            WebView.setWebContentsDebuggingEnabled(true)
        }
    }
}
