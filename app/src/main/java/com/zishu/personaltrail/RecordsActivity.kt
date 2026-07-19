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

class RecordsActivity : AppCompatActivity() {
    private val paper = Color.rgb(247, 245, 239)
    private val ink = Color.rgb(38, 54, 49)
    private val teal = Color.rgb(53, 104, 89)
    private val line = Color.rgb(210, 222, 216)
    private var position = 0
    private var summaries = emptyList<DaySummary>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        summaries = TrailStorage.summaries(this)
        setContentView(buildScreen())
    }

    private fun buildScreen(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(22), dp(20), dp(24))
        background = rounded(paper, 0)
        addView(backButton("‹  个人轨迹") { finish() })
        addView(title("回看"))
        addView(body("先选一天，再进入当天的不同层。", 15f).apply { setPadding(0, dp(6), 0, dp(22)) })
        if (summaries.isEmpty()) {
            addView(body("还没有记录。采集一次轨迹或留下一句碎碎念，它就会出现在这里。", 16f).apply {
                gravity = Gravity.CENTER
                setPadding(dp(22), dp(42), dp(22), dp(42))
                background = rounded(Color.WHITE, 22, line, 1)
            })
        } else {
            addView(dayCard(summaries[position]))
            addView(space(14))
            addView(navigation())
        }
    }

    private fun dayCard(summary: DaySummary): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(22), dp(22), dp(22))
        background = rounded(Color.WHITE, 24, line, 1)
        addView(title(summary.day, 26f))
        addView(body("${summary.noteCount} 条碎碎念  ·  ${summary.timelineCount} 次轨迹采集", 15f).apply {
            setPadding(0, dp(8), 0, dp(22))
        })
        addView(primaryButton("打开这一天") {
            startActivity(Intent(this@RecordsActivity, RecordDayActivity::class.java).putExtra(RecordDayActivity.EXTRA_DAY, summary.day))
        })
    }

    private fun navigation(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(secondaryButton("‹ 更新一天") { move(1) }, LinearLayout.LayoutParams(0, dp(50), 1f))
        addView(spaceWidth(10))
        addView(secondaryButton("更早一天 ›") { move(-1) }, LinearLayout.LayoutParams(0, dp(50), 1f))
        getChildAt(0).isEnabled = position < summaries.lastIndex
        getChildAt(2).isEnabled = position > 0
    }

    private fun move(delta: Int) {
        position = (position + delta).coerceIn(0, summaries.lastIndex)
        setContentView(buildScreen())
    }

    private fun backButton(text: String, action: () -> Unit) = TextView(this).apply {
        this.text = text; textSize = 15f; setTextColor(teal); typeface = Typeface.DEFAULT_BOLD
        setPadding(0, 0, 0, dp(18)); setOnClickListener { action() }
    }
    private fun title(text: String, size: Float = 30f) = TextView(this).apply { this.text = text; textSize = size; setTextColor(ink); typeface = Typeface.DEFAULT_BOLD }
    private fun body(text: String, size: Float) = TextView(this).apply { this.text = text; textSize = size; setTextColor(Color.rgb(96, 117, 108)); gravity = Gravity.CENTER_VERTICAL }
    private fun primaryButton(text: String, action: () -> Unit) = button(text, true, action)
    private fun secondaryButton(text: String, action: () -> Unit) = button(text, false, action)
    private fun button(text: String, primary: Boolean, action: () -> Unit) = Button(this).apply {
        this.text = text; textSize = 15f; isAllCaps = false; typeface = Typeface.DEFAULT_BOLD
        setTextColor(if (primary) Color.WHITE else teal); background = rounded(if (primary) teal else Color.WHITE, 16, if (primary) null else line, if (primary) 0 else 1)
        setOnClickListener { action() }
    }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun spaceWidth(width: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(dp(width), 1) }
    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != null) setStroke(dp(strokeWidth), stroke) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
}
