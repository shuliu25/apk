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

    var nextExpectedCaptureMillis: Long
        get() = data.getLong("next_expected_capture_millis", 0L)
        set(value) = data.edit().putLong("next_expected_capture_millis", value).apply()

    var activeTask: String?
        get() = data.getString("active_task", null)
        set(value) = data.edit().putString("active_task", value).apply()

    var activeTaskStartedMillis: Long
        get() = data.getLong("active_task_started_millis", 0L)
        set(value) = data.edit().putLong("active_task_started_millis", value).apply()

}
