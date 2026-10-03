package com.mh.chat

import android.content.Context

/** Persists endpoint URL, API key and model on the device. The key is never logged. */
class Prefs(ctx: Context) {
    private val sp = ctx.getSharedPreferences("mh_chat", Context.MODE_PRIVATE)

    var endpoint: String
        get() = sp.getString("endpoint", DEFAULT_ENDPOINT).orEmpty()
        set(v) = sp.edit().putString("endpoint", v).apply()

    var apiKey: String
        get() = sp.getString("api_key", "").orEmpty()
        set(v) = sp.edit().putString("api_key", v).apply()

    var model: String
        get() = sp.getString("model", "").orEmpty()
        set(v) = sp.edit().putString("model", v).apply()

    companion object {
        const val DEFAULT_ENDPOINT = "https://omniroute-es3t.onrender.com"
    }
}
