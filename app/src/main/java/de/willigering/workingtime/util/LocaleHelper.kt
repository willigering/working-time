package de.willigering.workingtime.util

import android.content.Context
import android.content.res.Configuration
import android.os.LocaleList
import de.willigering.workingtime.BuildConfig
import java.util.Locale

object LocaleHelper {
    fun flavorLocale(): Locale =
        if (BuildConfig.FLAVOR == "de") Locale.GERMAN else Locale.ENGLISH

    fun wrap(context: Context): Context {
        val locale = flavorLocale()
        Locale.setDefault(locale)
        LocaleList.setDefault(LocaleList(locale))

        val config = Configuration(context.resources.configuration)
        config.setLocale(locale)
        config.setLocales(LocaleList(locale))
        return context.createConfigurationContext(config)
    }
}