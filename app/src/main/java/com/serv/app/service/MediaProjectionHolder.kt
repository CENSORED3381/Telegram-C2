package com.serv.app.service

import java.io.File

object MediaProjectionHolder {
    @Volatile var last: File? = null
}
