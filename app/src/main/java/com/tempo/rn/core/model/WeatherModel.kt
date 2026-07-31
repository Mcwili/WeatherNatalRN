package com.tempo.rn.core.model

/**
 * Die separat abgerufenen Wettermodelle (Spez. §3.1) mit Open-Meteo-Parameter
 * und Startgewicht (Spez. §6.3). Fehlende Modelle werden toleriert.
 */
enum class WeatherModel(
    val id: String,
    val apiParam: String,
    val displayName: String,
    val startWeight: Double,
) {
    ECMWF_IFS("ecmwf_ifs", "ecmwf_ifs025", "ECMWF", 1.00),
    ECMWF_AIFS("ecmwf_aifs", "ecmwf_aifs025", "AIFS", 0.95),
    ICON("icon", "icon_seamless", "ICON", 0.90),
    GFS("gfs", "gfs_seamless", "GFS", 0.85),
    UKMO("ukmo", "ukmo_seamless", "UKMO", 0.85),
    GEM("gem", "gem_seamless", "GEM", 0.80),
    ACCESS_G("access_g", "bom_access_global", "ACCESS-G", 0.75);

    companion object {
        fun byId(id: String): WeatherModel? = entries.firstOrNull { it.id == id }
    }
}
