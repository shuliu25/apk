package com.zishu.personaltrail

import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.BatteryManager
import android.provider.OpenableColumns
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import androidx.documentfile.provider.DocumentFile
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max

class TimelineCollector(private val context: Context) {
    private val prefs = TrailPreferences(context)
    private val dayFormat = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA)
    private val timeFormat = SimpleDateFormat("HH:mm:ss", Locale.CHINA)

    suspend fun capture(reason: String): CaptureResult = withContext(Dispatchers.IO) {
        val root = outputRoot() ?: return@withContext CaptureResult(false, "请先选择日志输出文件夹")
        if (!hasUsageAccess()) return@withContext CaptureResult(false, "请先授权“使用情况访问权限”")

        val now = System.currentTimeMillis()
        val day = dayFormat.format(Date(now))
        val dayDirectory = root.findFile(day) ?: root.createDirectory(day)
            ?: return@withContext CaptureResult(false, "无法创建当天文件夹")

        val eventLines = collectUsageEvents(now)
        val usageSummary = collectUsageSummary(now)
        val battery = readBattery()
        val body = buildString {
            append("\n[").append(timeFormat.format(Date(now))).append("] [").append(reason).append("]\n")
            append("电量: ").append(battery).append('\n')
            append("最近6小时使用: ").append(usageSummary.ifBlank { "暂无数据" }).append('\n')
            append("前台切换:\n")
            if (eventLines.isEmpty()) append("- 无新增事件（下次会自动补查）\n")
            else eventLines.forEach { append("- ").append(it).append('\n') }
            append("图片: 仅在记录碎碎念时手动选择后保存")
            append('\n')
        }
        appendText(dayDirectory, "timeline.txt", body)
        CaptureResult(true, "已写入 $day/timeline.txt")
    }

    suspend fun appendThought(text: String, selectedImageUri: Uri?): CaptureResult = withContext(Dispatchers.IO) {
        val root = outputRoot() ?: return@withContext CaptureResult(false, "请先选择日志输出文件夹")
        val clean = text.trim()
        if (clean.isBlank()) return@withContext CaptureResult(false, "先写下一句想法")
        val now = System.currentTimeMillis()
        val day = dayFormat.format(Date(now))
        val dayDirectory = root.findFile(day) ?: root.createDirectory(day)
            ?: return@withContext CaptureResult(false, "无法创建当天文件夹")
        val selectedImageName = selectedImageUri?.let { copySelectedImage(dayDirectory, it) }
        if (selectedImageUri != null && selectedImageName == null) {
            return@withContext CaptureResult(false, "所选图片未能保存，请重新选择")
        }
        val old = readText(dayDirectory.findFile("notes.txt"))
        val number = Regex("(?m)^#(\\d+)").findAll(old).lastOrNull()?.groupValues?.get(1)?.toIntOrNull()?.plus(1) ?: 1
        val entry = buildString {
            append("\n#").append(number).append(" [").append(timeFormat.format(Date(now))).append("]\n")
            if (selectedImageName != null) append("🖼️ 关联图片: screenshots/").append(selectedImageName).append(" (手动选择)\n")
            append("💭 内容: \"").append(clean.replace("\"", "“")).append("\"\n\n---\n")
        }
        appendText(dayDirectory, "notes.txt", entry)
        CaptureResult(true, "已写入 $day/notes.txt")
    }

    suspend fun startTask(task: String): CaptureResult = withContext(Dispatchers.IO) {
        val root = outputRoot() ?: return@withContext CaptureResult(false, "请先选择日志输出文件夹")
        val clean = task.trim()
        if (clean.isBlank()) return@withContext CaptureResult(false, "先写下要做什么")
        if (prefs.activeTask != null) return@withContext CaptureResult(false, "请先结束正在进行的“${prefs.activeTask}”")
        val now = System.currentTimeMillis()
        val day = dayFormat.format(Date(now))
        val dayDirectory = root.findFile(day) ?: root.createDirectory(day)
            ?: return@withContext CaptureResult(false, "无法创建当天文件夹")
        appendText(dayDirectory, "timeline.txt", "\n[${timeFormat.format(Date(now))}] [手动任务开始] $clean\n")
        prefs.activeTask = clean
        prefs.activeTaskStartedMillis = now
        CaptureResult(true, "已开始：$clean")
    }

    suspend fun finishTask(): CaptureResult = withContext(Dispatchers.IO) {
        val root = outputRoot() ?: return@withContext CaptureResult(false, "请先选择日志输出文件夹")
        val task = prefs.activeTask ?: return@withContext CaptureResult(false, "当前没有正在进行的任务")
        val now = System.currentTimeMillis()
        val day = dayFormat.format(Date(now))
        val dayDirectory = root.findFile(day) ?: root.createDirectory(day)
            ?: return@withContext CaptureResult(false, "无法创建当天文件夹")
        val started = if (prefs.activeTaskStartedMillis > 0L) timeFormat.format(Date(prefs.activeTaskStartedMillis)) else "稍早"
        appendText(dayDirectory, "timeline.txt", "\n[${timeFormat.format(Date(now))}] [手动任务结束] $task（开始于 $started）\n")
        prefs.activeTask = null
        prefs.activeTaskStartedMillis = 0L
        CaptureResult(true, "已结束：$task")
    }

    private fun outputRoot(): DocumentFile? = prefs.treeUri?.let { DocumentFile.fromTreeUri(context, Uri.parse(it)) }

    private fun hasUsageAccess(): Boolean {
        val manager = context.getSystemService(Context.APP_OPS_SERVICE) as android.app.AppOpsManager
        return manager.checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS, android.os.Process.myUid(), context.packageName) == android.app.AppOpsManager.MODE_ALLOWED
    }

    private fun collectUsageEvents(now: Long): List<String> {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        val start = max(0L, prefs.lastEventMillis - 5 * 60 * 1000L)
        val events = manager.queryEvents(start, now)
        val event = UsageEvents.Event()
        val lines = mutableListOf<String>()
        while (events.hasNextEvent()) {
            events.getNextEvent(event)
            val foreground = event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == UsageEvents.Event.MOVE_TO_FOREGROUND
            val background = event.eventType == UsageEvents.Event.ACTIVITY_PAUSED || event.eventType == UsageEvents.Event.MOVE_TO_BACKGROUND
            if (!foreground && !background) continue
            if (event.timeStamp <= prefs.lastEventMillis) continue
            val action = if (foreground) "进入" else "离开"
            lines += "${timeFormat.format(Date(event.timeStamp))} $action ${appName(event.packageName)}"
        }
        prefs.lastEventMillis = now
        return lines.takeLast(80)
    }

    private fun collectUsageSummary(now: Long): String {
        val manager = context.getSystemService(Context.USAGE_STATS_SERVICE) as UsageStatsManager
        return manager.queryUsageStats(UsageStatsManager.INTERVAL_BEST, now - 6 * 60 * 60 * 1000L, now)
            .filter { it.totalTimeInForeground > 0 }
            .sortedByDescending { it.totalTimeInForeground }
            .take(8)
            .joinToString("、") { "${appName(it.packageName)}(${it.totalTimeInForeground / 60000}分钟)" }
    }

    private fun readBattery(): String {
        val intent = context.registerReceiver(null, IntentFilter(Intent.ACTION_BATTERY_CHANGED))
        val level = intent?.getIntExtra(BatteryManager.EXTRA_LEVEL, -1) ?: -1
        val scale = intent?.getIntExtra(BatteryManager.EXTRA_SCALE, 100) ?: 100
        val status = intent?.getIntExtra(BatteryManager.EXTRA_STATUS, -1) ?: -1
        val charging = status == BatteryManager.BATTERY_STATUS_CHARGING || status == BatteryManager.BATTERY_STATUS_FULL
        return "${if (level >= 0) level * 100 / scale else "未知"}%(${if (charging) "充电中" else "未充电"})"
    }

    private fun copySelectedImage(dayDirectory: DocumentFile, source: Uri): String? = runCatching {
        val screenshots = dayDirectory.findFile("screenshots") ?: dayDirectory.createDirectory("screenshots") ?: return@runCatching null
        val originalName = context.contentResolver.query(source, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use { cursor ->
            if (cursor.moveToFirst()) cursor.getString(0) else null
        }.orEmpty().ifBlank { "image-${System.currentTimeMillis()}.jpg" }
        val name = uniqueName(screenshots, originalName.replace('/', '_').replace('\\', '_'))
        val target = screenshots.createFile(context.contentResolver.getType(source) ?: "image/jpeg", name) ?: return@runCatching null
        context.contentResolver.openInputStream(source)?.use { input ->
            context.contentResolver.openOutputStream(target.uri, "w")?.use { output -> input.copyTo(output) }
        } ?: return@runCatching null
        name
    }.getOrNull()

    private fun uniqueName(directory: DocumentFile, original: String): String {
        if (directory.findFile(original) == null) return original
        val dot = original.lastIndexOf('.')
        val stem = if (dot > 0) original.substring(0, dot) else original
        val extension = if (dot > 0) original.substring(dot) else ""
        var index = 2
        while (directory.findFile("$stem-$index$extension") != null) index++
        return "$stem-$index$extension"
    }

    private fun appName(packageName: String): String = runCatching {
        context.packageManager.getApplicationInfo(packageName, 0).loadLabel(context.packageManager).toString()
    }.getOrElse { packageName }

    private fun appendText(directory: DocumentFile, name: String, text: String) {
        val file = directory.findFile(name) ?: directory.createFile("text/plain", name) ?: error("无法创建 $name")
        val previous = readText(file)
        context.contentResolver.openOutputStream(file.uri, "wt")!!.bufferedWriter().use { it.write(previous + text) }
    }

    private fun readText(file: DocumentFile?): String {
        if (file == null || !file.exists()) return ""
        return context.contentResolver.openInputStream(file.uri)?.bufferedReader()?.use { it.readText() }.orEmpty()
    }

}

data class CaptureResult(val success: Boolean, val message: String)
