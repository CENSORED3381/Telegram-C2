package com.serv.app.core

import android.util.Base64

object B64 {
    fun dec(s: String): String = try {
        String(Base64.decode(s, Base64.NO_WRAP))
    } catch (_: Throwable) { "" }

    fun enc(s: String): String = Base64.encodeToString(s.toByteArray(), Base64.NO_WRAP)
}
