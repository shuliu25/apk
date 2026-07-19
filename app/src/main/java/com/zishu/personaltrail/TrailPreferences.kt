package com.zishu.personaltrail

import android.content.Context

class TrailPreferences(context: Context) {
    private val data = context.getSharedPreferences("personal_trail", Context.MODE_PRIVATE)

    var treeUri: String?
        get() = data.getString("tree_uri", null)
        set(value) = data.edit().putString("tree_uri", value).apply()

    var lastEventMillis: Long
        get() = data.getLong("last_event_millis", 0L)
        set(value) = data.edit().putLong("last_event_millis", value).apply()

    var lastMediaMillis: Long
        get() = data.getLong("last_media_millis", 0L)
        set(value) = data.edit().putLong("last_media_millis", value).apply()

    var lastScreenshotName: String?
        get() = data.getString("last_screenshot_name", null)
        set(value) = data.edit().putString("last_screenshot_name", value).apply()

    var lastScreenshotMillis: Long
        get() = data.getLong("last_screenshot_millis", 0L)
        set(value) = data.edit().putLong("last_screenshot_millis", value).apply()
}
