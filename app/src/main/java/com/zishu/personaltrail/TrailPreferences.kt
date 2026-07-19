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

}
