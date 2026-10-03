package com.mh.chat

import android.app.Application
import androidx.appcompat.app.AppCompatDelegate

class MhChatApp : Application() {
    override fun onCreate() {
        super.onCreate()
        // Always dark theme.
        AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
    }
}
