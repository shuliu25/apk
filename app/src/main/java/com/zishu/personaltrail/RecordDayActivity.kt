package com.zishu.personaltrail

import android.content.Intent
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

class RecordDayActivity : AppCompatActivity() {
    companion object { const val EXTRA_DAY = "day" }
    private val paper = Color.rgb(247, 245, 239)
    private val ink = Color.rgb(38, 54, 49)
    private val teal = Color.rgb(53, 104, 89)
    private val line = Color.rgb(210, 222, 216)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        setContentView(buildScreen())
    }

    private fun buildScreen(): LinearLayout {
        val day = intent.getStringExtra(EXTRA_DAY).orEmpty()
        val directory = TrailStorage.root(this)?.findFile(day)
        val notes = TrailStorage.noteEntries(this, directory)
        val timeline = TrailStorage.timelineEntries(this, directory)
        return LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(24))
            background = rounded(paper, 0)
            addView(backButton())
            addView(title(day))
            addView(body("这一天的记录被分成不同层，不在这里铺开。", 15f).apply { setPadding(0, dp(7), 0, dp(22)) })
            addView(openButton("碎碎念", "${notes.size} 条 · 图片和当时的话", EntryPagerActivity.TYPE_NOTES, notes.size, day))
            addView(space(12))
            addView(openButton("轨迹与任务", "${timeline.size} 次 · 每次采集单独查看", EntryPagerActivity.TYPE_TIMELINE, timeline.size, day))
        }
    }

    private fun openButton(title: String, detail: String, type: String, count: Int, day: String) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(20), dp(19), dp(20), dp(19)); background = rounded(Color.WHITE, 22, line, 1)
        addView(title(title, 19f)); addView(body(detail, 14f).apply { setPadding(0, dp(6), 0, dp(14)) })
        addView(button(if (count == 0) "还没有内容" else "打开  ›") {
            if (count > 0) startActivity(Intent(this@RecordDayActivity, EntryPagerActivity::class.java).putExtra(EntryPagerActivity.EXTRA_DAY, day).putExtra(EntryPagerActivity.EXTRA_TYPE, type))
        }.apply { isEnabled = count > 0 })
    }

    private fun backButton() = TextView(this).apply { text = "‹  回看"; textSize = 15f; setTextColor(teal); typeface = Typeface.DEFAULT_BOLD; setPadding(0, 0, 0, dp(18)); setOnClickListener { finish() } }
    private fun title(text: String, size: Float = 30f) = TextView(this).apply { this.text = text; textSize = size; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD }
    private fun body(text: String, size: Float) = TextView(this).apply { this.text = text; textSize = size; setTextColor(Color.rgb(96, 117, 108)); gravity = Gravity.CENTER_VERTICAL }
    private fun button(text: String, action: () -> Unit) = Button(this).apply { this.text = text; isAllCaps = false; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(Color.WHITE); background = rounded(teal, 16); setOnClickListener { action() } }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != null) setStroke(dp(strokeWidth), stroke) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
