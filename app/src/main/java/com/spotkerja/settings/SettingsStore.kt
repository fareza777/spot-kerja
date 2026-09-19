package com.spotkerja.settings

import android.content.Context
import com.spotkerja.ui.theme.ThemeOption

/** Preferensi app via SharedPreferences — tema, durasi, spot names/count. */
class SettingsStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("spotkerja_settings", Context.MODE_PRIVATE)

    var theme: ThemeOption
        get() = ThemeOption.entries.firstOrNull { it.name == prefs.getString("theme", null) }
            ?: ThemeOption.EMERALD
        set(v) = prefs.edit().putString("theme", v.name).apply()

    var durationSec: Int
        get() = prefs.getInt("duration", 30)
        set(v) = prefs.edit().putInt("duration", v.coerceIn(15, 60)).apply()

    var spotCount: Int
        get() = prefs.getInt("spotCount", 3)
        set(v) = prefs.edit().putInt("spotCount", v.coerceIn(2, 8)).apply()

    /** Nama custom per index spot; default "Spot A/B/C". */
    fun spotName(index: Int): String =
        prefs.getString("spotName_$index", null) ?: "Spot ${'A' + index}"

    fun setSpotName(index: Int, name: String) =
        prefs.edit().putString("spotName_$index", name.trim().ifEmpty { null }).apply()

    var adsEnabled: Boolean
        get() = prefs.getBoolean("ads", true)
        set(v) = prefs.edit().putBoolean("ads", v).apply()
}
