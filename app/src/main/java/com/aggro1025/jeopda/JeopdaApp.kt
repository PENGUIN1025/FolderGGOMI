package com.aggro1025.jeopda

import android.app.Application
import com.aggro1025.jeopda.session.SmartSession

class JeopdaApp : Application() {
    override fun onCreate() {
        super.onCreate()
        SmartSession.createChannels(this)
        com.aggro1025.jeopda.ads.UnlockAds.init(this)
    }
}
