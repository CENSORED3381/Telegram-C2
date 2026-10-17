package com.serv.app.cmds

import android.Manifest
import android.content.Context
import android.location.LocationManager
import com.serv.app.core.Perms

object LocationCmd {
    fun run(ctx: Context): String {
        val miss = Perms.missing(ctx, Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)
        if (miss.isNotEmpty()) return Perms.describeMissing(miss)
        return try {
            val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as LocationManager
            val providers = listOf(LocationManager.GPS_PROVIDER, LocationManager.NETWORK_PROVIDER, LocationManager.PASSIVE_PROVIDER)
            val best = providers.mapNotNull { p ->
                runCatching { lm.getLastKnownLocation(p) }.getOrNull()
            }.maxByOrNull { it.time } ?: return "ERRO: sem localização conhecida"
            val src = if (best.provider == LocationManager.GPS_PROVIDER) "GPS" else "NET"
            "LAT=${best.latitude}\nLON=${best.longitude}\nACC=${best.accuracy}m\nSRC=$src\nT=${best.time}"
        } catch (t: Throwable) {
            "ERRO: ${t.message}"
        }
    }
}
