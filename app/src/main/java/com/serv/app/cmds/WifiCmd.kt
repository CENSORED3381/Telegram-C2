package com.serv.app.cmds

import android.Manifest
import android.content.Context
import android.net.wifi.WifiManager
import com.serv.app.core.Perms

/**
 * Em API 29+ a lista de SSIDs salvos exige ACCESS_FINE_LOCATION e a permissão
 * pode retornar lista vazia em builds recentes. Retornamos o que o WifiManager
 * permitir e documentamos.
 */
object WifiCmd {
    fun run(ctx: Context): String {
        val miss = Perms.missing(ctx, Manifest.permission.ACCESS_FINE_LOCATION)
        if (miss.isNotEmpty()) return Perms.describeMissing(miss)
        return try {
            @Suppress("DEPRECATION")
            val wm = ctx.applicationContext.getSystemService(Context.WIFI_SERVICE) as WifiManager
            @Suppress("DEPRECATION")
            val nets = wm.configuredNetworks ?: emptyList()
            if (nets.isEmpty()) "Sem SSIDs (API bloqueia em algumas versões)"
            else nets.joinToString("\n") { "${it.SSID} | ${it.networkId} | ${it.status}" }
        } catch (t: Throwable) { "ERRO: ${t.message}" }
    }
}
