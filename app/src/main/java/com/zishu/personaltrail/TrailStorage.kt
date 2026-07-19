package com.zishu.personaltrail

import android.content.Context
import android.net.Uri
import androidx.documentfile.provider.DocumentFile

data class DaySummary(val day: String, val noteCount: Int, val timelineCount: Int)

object TrailStorage {
    private val dayPattern = Regex("\\d{4}-\\d{2}-\\d{2}")

    fun root(context: Context): DocumentFile? = TrailPreferences(context).treeUri?.let {
        DocumentFile.fromTreeUri(context, Uri.parse(it))
    }

    fun summaries(context: Context): List<DaySummary> = root(context)?.listFiles()
        ?.filter { it.isDirectory && dayPattern.matches(it.name.orEmpty()) }
        ?.map { day ->
            val notes = readText(context, day.findFile("notes.txt"))
            val timeline = readText(context, day.findFile("timeline.txt"))
            DaySummary(
                day.name.orEmpty(),
                Regex("(?m)^#\\d+").findAll(notes).count(),
                Regex("(?m)^\\[\\d{2}:\\d{2}:\\d{2}\\]").findAll(timeline).count()
            )
        }
        ?.sortedByDescending { it.day }
        .orEmpty()

    fun readText(context: Context, file: DocumentFile?): String {
        if (file == null || !file.exists()) return ""
        return context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
    }
}
