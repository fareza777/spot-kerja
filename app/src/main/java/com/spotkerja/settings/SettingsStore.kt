package com.spotkerja.settings

import android.content.Context
import com.spotkerja.ui.theme.ThemeOption
import kotlinx.serialization.Serializable

enum class ThemeMode { SYSTEM, LIGHT, DARK }

/** Which sensors/metrics a scan collects — user-customizable. */
@Serializable
data class ScanOptions(
    val wifi: Boolean = true,
    val ping: Boolean = true,
    val light: Boolean = true,
    val noise: Boolean = true,
    val orientation: Boolean = true,
    val cellular: Boolean = true,
    val pingHost: String = "", // blank = Wi-Fi gateway
)

/** Preferensi app via SharedPreferences — tema, durasi, spot names/count, scan options. */
class SettingsStore(ctx: Context) {
    private val prefs = ctx.getSharedPreferences("spotwise_settings", Context.MODE_PRIVATE)

    var theme: ThemeOption
        get() = ThemeOption.entries.firstOrNull { it.name == prefs.getString("theme", null) }
            ?: ThemeOption.EMERALD
        set(v) = prefs.edit().putString("theme", v.name).apply()

    var themeMode: ThemeMode
        get() = ThemeMode.entries.firstOrNull { it.name == prefs.getString("themeMode", null) }
            ?: ThemeMode.DARK
        set(v) = prefs.edit().putString("themeMode", v.name).apply()

    var durationSec: Int
        get() = prefs.getInt("duration", 30)
        set(v) = prefs.edit().putInt("duration", v.coerceIn(15, 60)).apply()

    var fastDurationSec: Int
        get() = prefs.getInt("fastDuration", 15)
        set(v) = prefs.edit().putInt("fastDuration", v.coerceIn(10, 30)).apply()

    var spotCount: Int
        get() = prefs.getInt("spotCount", 3)
        set(v) = prefs.edit().putInt("spotCount", v.coerceIn(2, 8)).apply()

    /** Nama custom per index spot; default "Spot A/B/C". */
    fun spotName(index: Int): String =
        prefs.getString("spotName_$index", null) ?: "Spot ${'A' + index}"

    fun setSpotName(index: Int, name: String) =
        prefs.edit().putString("spotName_$index", name.trim().ifEmpty { null }).apply()

    var onboarded: Boolean
        get() = prefs.getBoolean("onboarded", false)
        set(v) = prefs.edit().putBoolean("onboarded", v).apply()

    var adsEnabled: Boolean
        get() = prefs.getBoolean("ads", true)
        set(v) = prefs.edit().putBoolean("ads", v).apply()

    var scanOptions: ScanOptions
        get() = ScanOptions(
            wifi = prefs.getBoolean("opt_wifi", true),
            ping = prefs.getBoolean("opt_ping", true),
            light = prefs.getBoolean("opt_light", true),
            noise = prefs.getBoolean("opt_noise", true),
            orientation = prefs.getBoolean("opt_orientation", true),
            cellular = prefs.getBoolean("opt_cellular", true),
            pingHost = prefs.getString("pingHost", "") ?: "",
        )
        set(v) = prefs.edit()
            .putBoolean("opt_wifi", v.wifi)
            .putBoolean("opt_ping", v.ping)
            .putBoolean("opt_light", v.light)
            .putBoolean("opt_noise", v.noise)
            .putBoolean("opt_orientation", v.orientation)
            .putBoolean("opt_cellular", v.cellular)
            .putString("pingHost", v.pingHost.trim())
            .apply()
}
