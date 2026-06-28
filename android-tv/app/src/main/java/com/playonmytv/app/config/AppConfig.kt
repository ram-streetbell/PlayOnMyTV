package com.playonmytv.app.config

import com.playonmytv.BuildConfig

object AppConfig {
    const val pairingPollIntervalMillis: Long = 5_000
    val apiBaseUrl: String = BuildConfig.API_BASE_URL.trimEnd('/')
}
