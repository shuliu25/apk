package com.zishu.personaltrail

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RecordsActivity : AppCompatActivity() {
    private val paper = Color.rgb(247, 245, 239)
    private val ink = Color.rgb(38, 54, 49)
    private val teal = Color.rgb(53, 104, 89)
    private val paleTeal = Color.rgb(231, 240, 235)
    private val line = Color.rgb(210, 222, 216)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        setContentView(buildScreen())
    }

    override fun onResume() {
        super.onResume()
        setContentView(buildScreen())
    }

    private fun buildScreen(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(30))
            background = rounded(paper, 0)
        }
        root.addView(TextView(this).apply {
            text = "‹  个人轨迹"
            textSize = 15f
            setTextColor(teal)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(18))
            setOnClickListener { finish() }
        })
        root.addView(TextView(this).apply {
            text = "回看"
            textSize = 30f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
        })
        root.addView(body("按日期收拢碎碎念与轨迹。点开一天，细看那天留下的东西。", 15f).apply {
            setTextColor(Color.rgb(96, 117, 108))
            setPadding(0, dp(7), 0, dp(18))
        })

        val summaries = TrailStorage.summaries(this)
        if (summaries.isEmpty()) {
            root.addView(emptyCard())
        } else {
            root.addView(body("已有 ${summaries.size} 天记录", 13f).apply {
                setTextColor(teal)
                typeface = Typeface.DEFAULT_BOLD
                setPadding(dp(4), 0, dp(4), dp(8))
            })
            summaries.forEach { summary ->
                root.addView(dayCard(summary))
                root.addView(space(10))
            }
        }
        return ScrollView(this).apply { addView(root) }
    }

    private fun dayCard(summary: DaySummary): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = rounded(Color.WHITE, 20, line, 1)
        addView(TextView(this@RecordsActivity).apply {
            text = summary.day
            textSize = 19f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(space(5))
        addView(body("${summary.noteCount} 条碎碎念   ·   ${summary.timelineCount} 条时间线", 14f).apply {
            setTextColor(Color.rgb(96, 117, 108))
        })
        addView(space(12))
        addView(body("打开这一天  ›", 14f).apply {
            setTextColor(teal)
            typeface = Typeface.DEFAULT_BOLD
        })
        setOnClickListener {
            startActivity(Intent(this@RecordsActivity, RecordDayActivity::class.java).putExtra(RecordDayActivity.EXTRA_DAY, summary.day))
        }
    }

    private fun emptyCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        gravity = Gravity.CENTER_HORIZONTAL
        setPadding(dp(22), dp(30), dp(22), dp(30))
        background = rounded(paleTeal, 22)
        addView(TextView(this@RecordsActivity).apply {
            text = "还没有可回看的记录"
            textSize = 18f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(ink)
        })
        addView(body("采集一次轨迹，或留下一条碎碎念后，它会出现在这里。", 14f).apply {
            gravity = Gravity.CENTER
            setTextColor(Color.rgb(96, 117, 108))
            setPadding(0, dp(8), 0, 0)
        })
    }

    private fun body(text: String, size: Float): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(ink)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun space(height: Int): View = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(strokeWidth), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()
}
