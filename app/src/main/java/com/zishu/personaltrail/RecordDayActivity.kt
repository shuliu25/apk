package com.zishu.personaltrail

import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity

class RecordDayActivity : AppCompatActivity() {
    companion object { const val EXTRA_DAY = "day" }

    private val paper = Color.rgb(247, 245, 239)
    private val ink = Color.rgb(38, 54, 49)
    private val teal = Color.rgb(53, 104, 89)
    private val line = Color.rgb(210, 222, 216)
    private val paleTeal = Color.rgb(231, 240, 235)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        setContentView(buildScreen())
    }

    private fun buildScreen(): ScrollView {
        val day = intent.getStringExtra(EXTRA_DAY).orEmpty()
        val directory = TrailStorage.root(this)?.findFile(day)
        val notes = TrailStorage.readText(this, directory?.findFile("notes.txt"))
        val timeline = TrailStorage.readText(this, directory?.findFile("timeline.txt"))
        val noteEntries = notes.split("\n---").map { it.trim() }.filter { it.startsWith("#") }.asReversed()
        val timelineEntries = timeline.trim().split(Regex("(?=\\[\\d{2}:\\d{2}:\\d{2}\\])")).map { it.trim() }.filter { it.isNotBlank() }.asReversed()

        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(20), dp(20), dp(30))
            background = rounded(paper, 0)
        }
        root.addView(TextView(this).apply {
            text = "‹  回看"
            textSize = 15f
            setTextColor(teal)
            typeface = Typeface.DEFAULT_BOLD
            setPadding(0, 0, 0, dp(18))
            setOnClickListener { finish() }
        })
        root.addView(dayHero(day, noteEntries.size, timelineEntries.size))

        root.addView(sectionTitle("碎碎念 · ${noteEntries.size}"))
        if (noteEntries.isEmpty()) root.addView(emptyLine("这一天还没有留下碎碎念。"))
        else noteEntries.forEach { entry ->
            root.addView(noteCard(entry, directory))
            root.addView(space(10))
        }

        root.addView(sectionTitle("轨迹与任务 · ${timelineEntries.size}"))
        if (timelineEntries.isEmpty()) root.addView(emptyLine("这一天还没有采集日志。"))
        else timelineEntries.forEach { entry ->
            root.addView(timelineCard(entry))
            root.addView(space(10))
        }
        return ScrollView(this).apply { addView(root) }
    }

    private fun dayHero(day: String, notes: Int, captures: Int): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(20), dp(19), dp(20), dp(19))
        background = rounded(teal, 24)
        addView(TextView(this@RecordDayActivity).apply {
            text = day
            textSize = 25f
            typeface = Typeface.DEFAULT_BOLD
            setTextColor(Color.WHITE)
        })
        addView(space(7))
        addView(TextView(this@RecordDayActivity).apply {
            text = "$notes 条碎碎念  ·  $captures 条时间线"
            textSize = 14f
            setTextColor(Color.rgb(224, 237, 230))
        })
    }

    private fun noteCard(entry: String, directory: androidx.documentfile.provider.DocumentFile?): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(17), dp(16), dp(17), dp(16))
        background = rounded(Color.WHITE, 20, line, 1)
        val time = Regex("^#\\d+ \\[(.*?)\\]").find(entry)?.groupValues?.get(1).orEmpty()
        addView(body(if (time.isBlank()) "碎碎念" else time, 13f).apply {
            setTextColor(teal)
            typeface = Typeface.DEFAULT_BOLD
        })
        val content = entry.lineSequence().filterNot { it.startsWith("#") || it.startsWith("🖼️") || it.startsWith("💭") }.joinToString("\n").trim()
        val original = Regex("💭 内容: \"(.*)\"").find(entry)?.groupValues?.get(1).orEmpty()
        addView(body(if (original.isNotBlank()) original else content, 16f).apply {
            setPadding(0, dp(8), 0, 0)
        })
        val imageName = Regex("关联图片: screenshots/(.+?) \\(手动选择\\)").find(entry)?.groupValues?.get(1)
        if (imageName != null) {
            val image = directory?.findFile("screenshots")?.findFile(imageName)
            if (image != null) {
                addView(space(12))
                addView(ImageView(this@RecordDayActivity).apply {
                    adjustViewBounds = true
                    maxHeight = dp(260)
                    scaleType = ImageView.ScaleType.CENTER_CROP
                    setImageURI(image.uri)
                    background = rounded(paleTeal, 14)
                })
            }
        }
    }

    private fun timelineCard(entry: String): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(Color.WHITE, 18, line, 1)
        val lines = entry.lines()
        addView(body(lines.firstOrNull().orEmpty(), 14f).apply {
            setTextColor(teal)
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(body(lines.drop(1).joinToString("\n"), 13f).apply {
            setTextColor(Color.rgb(82, 101, 93))
            setPadding(0, dp(7), 0, 0)
        })
    }

    private fun emptyLine(text: String): TextView = body(text, 14f).apply {
        setTextColor(Color.rgb(96, 117, 108))
        setPadding(dp(6), dp(6), dp(6), dp(6))
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(teal)
        letterSpacing = 0.06f
        setPadding(dp(4), dp(24), dp(4), dp(9))
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
