package com.example.foundit

import android.app.Application
import com.example.foundit.notify.NotificationHelper

class FoundItApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        SupabaseProvider.init(this)
        NotificationHelper.createChannel(this)
    }
}
