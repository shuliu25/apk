package com.zishu.personaltrail

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** 首页只承担“看见今天 + 去往下一层”的职责，所有输入都放到独立页面。 */
class MainActivity : AppCompatActivity() {
    private val paper get() = getColor(R.color.background)
    private val ink get() = getColor(R.color.on_surface)
    private val primary get() = getColor(R.color.primary)
    private val secondary get() = getColor(R.color.secondary)
    private val pale get() = getColor(R.color.primary_container)
    private val line get() = getColor(R.color.outline_variant)
    private lateinit var status: TextView
    private lateinit var feedback: TextView
    private lateinit var preferences: TrailPreferences

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = TrailPreferences(this)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        setContentView(buildScreen())
    }

    override fun onResume() {
        super.onResume()
        if (::status.isInitialized) refreshStatus()
    }

    private fun buildScreen(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(18), dp(20), dp(16))
        background = rounded(paper, 0)

        addView(hero())
        addView(space(12))
        status = TextView(this@MainActivity).apply {
            setPadding(dp(16), dp(10), dp(16), dp(10))
            textSize = 13f
            setTextColor(primary)
            typeface = Typeface.DEFAULT_BOLD
            background = rounded(pale, 16)
        }
        addView(status)
        addView(space(14))

        addView(label("现在"))
        addView(row(
            entrance("◎", "进行中的事", "开始／结束一件事") { startActivity(Intent(this@MainActivity, TaskActivity::class.java)) },
            entrance("✦", "写下想法", "文字和一张图片") { startActivity(Intent(this@MainActivity, ThoughtActivity::class.java)) }
        ))
        addView(space(10))
        addView(row(
            entrance("↻", "立即采集", "补上一段轨迹") { captureNow() },
            entrance("▣", "回看记录", "按天翻阅") { openRecords() }
        ))
        addView(space(14))
        addView(label("管理"))
        addView(row(
            entrance("◷", "采集与权限", "频率、授权、位置") { startActivity(Intent(this@MainActivity, SettingsActivity::class.java)) },
            entrance("●", "今天的总览", "轨迹与想法 ${todayCounts()}") { openRecords() }
        ))
        addView(space(10))
        feedback = TextView(this@MainActivity).apply {
            visibility = View.GONE
            gravity = Gravity.CENTER
            textSize = 13f
            setPadding(dp(10), dp(8), dp(10), dp(8))
        }
        addView(feedback)
        addView(View(this@MainActivity), LinearLayout.LayoutParams(1, 0, 1f))
        addView(TextView(this@MainActivity).apply {
            text = "个人轨迹  ·  先留住，再回看"
            gravity = Gravity.CENTER
            textSize = 12f
            setTextColor(getColor(R.color.on_surface_variant))
        })
        refreshStatus()
    }

    private fun hero(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(18), dp(20), dp(18))
        background = GradientDrawable(GradientDrawable.Orientation.TL_BR, intArrayOf(primary, secondary)).apply { cornerRadius = dp(22).toFloat() }
        elevation = dp(7).toFloat()
        addView(TextView(this@MainActivity).apply {
            text = "今天，留下什么？"
            setTextColor(Color.WHITE); textSize = 25f; typeface = Typeface.DEFAULT_BOLD
        })
        addView(TextView(this@MainActivity).apply {
            text = SimpleDateFormat("M月d日  EEEE", Locale.CHINA).format(Date())
            setTextColor(Color.rgb(231, 234, 255)); textSize = 14f
            setPadding(0, dp(4), 0, dp(13))
        })
        addView(TextView(this@MainActivity).apply {
            text = todayCounts()
            setTextColor(Color.WHITE); textSize = 13f
            setPadding(dp(11), dp(7), dp(11), dp(7))
            background = rounded(Color.argb(48, 255, 255, 255), 14)
        })
    }

    private fun row(left: View, right: View) = LinearLayout(this).apply {
        orientation = LinearLayout.HORIZONTAL
        addView(left, LinearLayout.LayoutParams(0, dp(82), 1f))
        addView(View(this@MainActivity), LinearLayout.LayoutParams(dp(10), 1))
        addView(right, LinearLayout.LayoutParams(0, dp(82), 1f))
    }

    private fun entrance(icon: String, title: String, subtitle: String, action: () -> Unit) = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; gravity = Gravity.CENTER_VERTICAL
        setPadding(dp(14), dp(10), dp(12), dp(10)); elevation = dp(3).toFloat()
        background = rounded(getColor(R.color.surface), 18, line, 1)
        isClickable = true; isFocusable = true
        setOnClickListener { action() }
        addView(TextView(this@MainActivity).apply { text = icon; textSize = 18f; setTextColor(primary) })
        addView(TextView(this@MainActivity).apply { text = title; textSize = 15f; typeface = Typeface.DEFAULT_BOLD; setTextColor(ink) })
        addView(TextView(this@MainActivity).apply { text = subtitle; textSize = 11f; setTextColor(getColor(R.color.on_surface_variant)) })
    }

    private fun label(text: String) = TextView(this).apply {
        this.text = text; textSize = 13f; typeface = Typeface.DEFAULT_BOLD; setTextColor(primary)
        letterSpacing = .08f; setPadding(dp(3), 0, 0, dp(7))
    }

    private fun captureNow() = lifecycleScope.launch {
        val result = TimelineCollector(this@MainActivity).capture("手动采集")
        feedback.visibility = View.VISIBLE
        feedback.text = if (result.success) "✓ ${result.message}" else "需要处理：${result.message}"
        feedback.setTextColor(if (result.success) primary else getColor(R.color.warning))
        feedback.background = rounded(if (result.success) pale else getColor(R.color.warning_container), 14)
        refreshStatus()
    }

    private fun openRecords() {
        if (preferences.treeUri == null) startActivity(Intent(this, SettingsActivity::class.java))
        else startActivity(Intent(this, RecordsActivity::class.java))
    }

    private fun refreshStatus() {
        if (preferences.treeUri == null) {
            status.text = "尚未准备好  ·  点「采集与权限」完成首次设置"
            return
        }
        val next = preferences.nextExpectedCaptureMillis
        val task = preferences.activeTask
        status.text = when {
            task != null -> "正在记录：$task"
            next > System.currentTimeMillis() -> "下一次自动采集  ·  ${SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(next))} 左右"
            else -> "采集已开启  ·  系统正在安排下一次"
        }
    }

    private fun todayCounts(): String {
        val day = SimpleDateFormat("yyyy-MM-dd", Locale.CHINA).format(Date())
        val item = TrailStorage.summaries(this).firstOrNull { it.day == day }
        return "${item?.timelineCount ?: 0} 段轨迹  ·  ${item?.noteCount ?: 0} 条想法"
    }
    private fun space(height: Int) = View(this).apply { layoutParams = LinearLayout.LayoutParams(1, dp(height)) }
    private fun dp(value: Int) = (value * resources.displayMetrics.density).toInt()
    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0) = GradientDrawable().apply { setColor(color); cornerRadius = dp(radius).toFloat(); if (stroke != null) setStroke(dp(strokeWidth), stroke) }
}
