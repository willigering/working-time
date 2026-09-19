package de.willigering.workingtime

import android.app.Application
import android.content.Context
import de.willigering.workingtime.util.LocaleHelper

class WorkingTimeApplication : Application() {
    override fun attachBaseContext(base: Context) {
        super.attachBaseContext(LocaleHelper.wrap(base))
    }
}