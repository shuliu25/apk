package com.zishu.personaltrail

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class EntryPagerActivity : AppCompatActivity() {
    companion object {
        const val EXTRA_DAY = "day"
        const val EXTRA_TYPE = "type"
        const val TYPE_NOTES = "notes"
        const val TYPE_TIMELINE = "timeline"
    }

    private val paper = Color.rgb(247, 245, 239)
    private val ink = Color.rgb(38, 54, 49)
    private val teal = Color.rgb(53, 104, 89)
    private val line = Color.rgb(210, 222, 216)
    private var position = 0
    private lateinit var entries: List<String>
    private var type = TYPE_NOTES

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        type = intent.getStringExtra(EXTRA_TYPE) ?: TYPE_NOTES
        val directory = TrailStorage.root(this)?.findFile(intent.getStringExtra(EXTRA_DAY).orEmpty())
        entries = if (type == TYPE_NOTES) TrailStorage.noteEntries(this, directory) else TrailStorage.timelineEntries(this, directory)
        setContentView(buildScreen())
    }

    private fun buildScreen(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(22), dp(20), dp(24))
        background = rounded(paper, 0)
        addView(backButton())
        addView(title(if (type == TYPE_NOTES) "碎碎念" else "轨迹与任务"))
        addView(body("${position + 1} / ${entries.size}", 14f).apply { setPadding(0, dp(7), 0, dp(18)) })
        addView(entryCard(entries.getOrElse(position) { "" }))
        addView(space(14))
        addView(navigation())
    }

    private fun entryCard(entry: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(20), dp(20), dp(20))
        background = rounded(Color.WHITE, 22, line, 1)
        if (type == TYPE_NOTES) {
            val time = Regex("^#\\d+ \\[(.*?)\\]").find(entry)?.groupValues?.get(1).orEmpty()
            val content = Regex("💭 内容: \"(.*)\"").find(entry)?.groupValues?.get(1).orEmpty()
            addView(title(if (time.isBlank()) "碎碎念" else time, 14f).apply { setTextColor(teal) })
            addView(body(content.ifBlank { entry }, 18f).apply { setTextColor(ink); setPadding(0, dp(12), 0, 0) })
        } else {
            val lines = entry.lines()
            addView(title(lines.firstOrNull().orEmpty(), 15f).apply { setTextColor(teal) })
            val battery = lines.firstOrNull { it.startsWith("电量:") }.orEmpty()
            val usage = lines.firstOrNull { it.startsWith("最近6小时使用:") }.orEmpty()
            val switches = lines.dropWhile { !it.startsWith("前台切换:") }.drop(1).takeWhile { !it.startsWith("图片:") }.count { it.startsWith("-") }
            val task = lines.firstOrNull { it.contains("手动任务") }.orEmpty()
            val compact = listOf(battery, usage, if (switches > 0) "前台切换：$switches 次" else "", task).filter { it.isNotBlank() }.joinToString("\n")
            addView(body(compact.ifBlank { "这是一条任务或采集记录。" }, 16f).apply { setTextColor(ink); setPadding(0, dp(12), 0, 0); maxLines = 9 })
        }
    }

    private fun navigation(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(button("‹ 更新一条", false) { move(1) }, LinearLayout.LayoutParams(0, dp(50), 1f))
        addView(spaceWidth(10))
        addView(button("更早一条 ›", false) { move(-1) }, LinearLayout.LayoutParams(0, dp(50), 1f))
        getChildAt(0).isEnabled = position < entries.lastIndex
        getChildAt(2).isEnabled = position > 0
    }

    private fun move(delta: Int) { position = (position + delta).coerceIn(0, entries.lastIndex); setContentView(buildScreen()) }
    private fun backButton() = TextView(this).apply { text = "‹  当天总览"; textSize = 15f; setTextColor(teal); typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, dp(18)); setOnClickListener { finish() } }
    private fun title(text: String, size: Float = 30f) = TextView(this).apply { this.text = text; textSize = size; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD }
    private fun body(text: String, size: Float) = TextView(this).apply { this.text = text; textSize = size; setTextColor(Color.rgb(96, 117, 108)); gravity = Gravity.CENTER_VERTICAL }
    private fun button(text: String, primary: Boolean, action: () -> Unit) = Button(this).apply { this.text = text; textSize = 15f; isAllCaps = false; typeface = Typeface.DEFAULT_BOLD; setTextColor(if (primary) Color.WHITE else teal); background = rounded(if (primary) teal else Color.WHITE, 16, if (primary) null else line, if (primary) 0 else 1); setOnClickListener { action() } }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun spaceWidth(width: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }
    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != null) setStroke(dp(strokeWidth), stroke) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
