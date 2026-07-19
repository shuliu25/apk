package com.zishu.personaltrail

import android.content.Intent
import android.graphics.Color
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class MainActivity : AppCompatActivity() {
    private val paper = Color.rgb(247, 245, 239)
    private val ink = Color.rgb(38, 54, 49)
    private val teal = Color.rgb(53, 104, 89)
    private val paleTeal = Color.rgb(231, 240, 235)
    private val line = Color.rgb(210, 222, 216)
    private val amberPale = Color.rgb(253, 242, 219)

    private lateinit var preferences: TrailPreferences
    private lateinit var scheduleTitle: TextView
    private lateinit var scheduleTime: TextView
    private lateinit var scheduleDetail: TextView
    private lateinit var notice: TextView
    private lateinit var thought: EditText
    private lateinit var selectedImage: TextView
    private var selectedImageUri: Uri? = null

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, flags)
        preferences.treeUri = uri.toString()
        TrailWorker.schedule(this)
        refreshSchedule()
        showFeedback("输出位置已设置，后台采集已开始", true)
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        updateSelectedImage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.statusBarColor = paper
        window.navigationBarColor = paper
        preferences = TrailPreferences(this)
        if (preferences.treeUri != null && preferences.nextExpectedCaptureMillis == 0L) TrailWorker.schedule(this)
        setContentView(buildScreen())
        refreshSchedule()
    }

    override fun onResume() {
        super.onResume()
        if (::scheduleTime.isInitialized) refreshSchedule()
    }

    private fun buildScreen(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(32))
            background = rounded(paper, 0)
        }

        root.addView(hero())
        root.addView(space(14))
        root.addView(scheduleCard())
        root.addView(space(10))
        notice = bodyText("").apply {
            visibility = View.GONE
            setPadding(dp(16), dp(14), dp(16), dp(14))
        }
        root.addView(notice)

        root.addView(sectionTitle("今天"))
        root.addView(card("补上一段轨迹", "现在手动采集一次；后台仍会按小时继续") {
            addView(actionButton("现在采集一次", true) { captureNow() })
        })

        root.addView(sectionTitle("留住一闪而过的东西"))
        root.addView(card("碎碎念", "无需整理成结论，先让它留下来") {
            thought = EditText(this@MainActivity).apply {
                hint = "写下一句话，不必完整，也不必漂亮"
                textSize = 16f
                setTextColor(ink)
                setHintTextColor(Color.rgb(126, 142, 136))
                minLines = 4
                gravity = Gravity.TOP
                setPadding(dp(15), dp(14), dp(15), dp(14))
                background = rounded(Color.WHITE, 16, line, 1)
                layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
            }
            addView(thought)
            addView(space(12))
            selectedImage = bodyText("可选：附上一张当下的图片").apply {
                setPadding(dp(14), dp(12), dp(14), dp(12))
                background = rounded(Color.rgb(250, 247, 239), 14)
            }
            addView(selectedImage)
            addView(space(8))
            addView(actionButton("选择关联图片", false) { imagePicker.launch("image/*") })
            addView(space(10))
            addView(actionButton("记录这句碎碎念", true) { saveThought() })
        })

        root.addView(sectionTitle("首次使用时设置"))
        root.addView(compactSettingsCard())
        root.addView(bodyText("应用只写入你选择的文件夹；Operit 负责同步到 GitHub。", 13f).apply {
            setTextColor(Color.rgb(102, 120, 112))
            setPadding(dp(4), dp(18), dp(4), 0)
        })

        return ScrollView(this).apply {
            isFillViewport = true
            addView(root)
        }
    }

    private fun hero(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(22), dp(24), dp(22), dp(22))
        background = rounded(teal, 28)
        addView(TextView(this@MainActivity).apply {
            text = "个人轨迹"
            setTextColor(Color.WHITE)
            textSize = 30f
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(space(7))
        addView(TextView(this@MainActivity).apply {
            text = "让一天留下可回看的痕迹"
            setTextColor(Color.rgb(224, 237, 230))
            textSize = 16f
        })
        addView(space(18))
        addView(TextView(this@MainActivity).apply {
            text = "✦  每小时补采  ·  Operit 推送"
            setTextColor(Color.WHITE)
            textSize = 13f
            setPadding(dp(12), dp(8), dp(12), dp(8))
            background = rounded(Color.rgb(73, 124, 106), 14)
        })
    }

    private fun scheduleCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(16), dp(18), dp(16))
        background = rounded(paleTeal, 22)
        scheduleTitle = bodyText("采集节奏", 14f).apply {
            setTextColor(teal)
            typeface = Typeface.DEFAULT_BOLD
        }
        addView(scheduleTitle)
        addView(space(4))
        scheduleTime = TextView(this@MainActivity).apply {
            textSize = 22f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
        }
        addView(scheduleTime)
        addView(space(4))
        scheduleDetail = bodyText("", 13f).apply { setTextColor(Color.rgb(88, 112, 101)) }
        addView(scheduleDetail)
    }

    private fun compactSettingsCard(): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(16), dp(15), dp(16), dp(15))
        background = rounded(Color.WHITE, 20, line, 1)
        addView(bodyText("只需设置一次", 16f).apply { typeface = Typeface.DEFAULT_BOLD })
        addView(bodyText("允许系统读取使用记录，并选择 Operit 同步的目录。", 13f).apply {
            setTextColor(Color.rgb(102, 120, 112))
            setPadding(0, dp(4), 0, dp(12))
        })
        addView(LinearLayout(this@MainActivity).apply {
            orientation = LinearLayout.HORIZONTAL
            addView(compactButton("授权使用") { startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)) }, LinearLayout.LayoutParams(0, dp(44), 1f))
            addView(spaceWidth(8))
            addView(compactButton("选择文件夹") { folderPicker.launch(null) }, LinearLayout.LayoutParams(0, dp(44), 1f))
        })
    }

    private fun card(title: String, subtitle: String, content: LinearLayout.() -> Unit): LinearLayout = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL
        setPadding(dp(18), dp(18), dp(18), dp(18))
        background = rounded(Color.WHITE, 22, line, 1)
        addView(TextView(this@MainActivity).apply {
            text = title
            textSize = 18f
            setTextColor(ink)
            typeface = Typeface.DEFAULT_BOLD
        })
        addView(bodyText(subtitle, 14f).apply {
            setTextColor(Color.rgb(102, 120, 112))
            setPadding(0, dp(5), 0, dp(15))
        })
        content()
    }

    private fun sectionTitle(text: String): TextView = TextView(this).apply {
        this.text = text
        textSize = 14f
        typeface = Typeface.DEFAULT_BOLD
        setTextColor(teal)
        letterSpacing = 0.06f
        setPadding(dp(4), dp(24), dp(4), dp(9))
    }

    private fun actionButton(text: String, primary: Boolean, action: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 15f
        isAllCaps = false
        setTextColor(if (primary) Color.WHITE else teal)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        minHeight = dp(50)
        minimumHeight = dp(50)
        setPadding(dp(14), 0, dp(14), 0)
        background = if (primary) rounded(teal, 16) else rounded(Color.WHITE, 16, line, 1)
        setOnClickListener { action() }
    }

    private fun compactButton(text: String, action: () -> Unit): Button = Button(this).apply {
        this.text = text
        textSize = 14f
        isAllCaps = false
        setTextColor(teal)
        typeface = Typeface.DEFAULT_BOLD
        gravity = Gravity.CENTER
        setPadding(dp(4), 0, dp(4), 0)
        background = rounded(Color.rgb(250, 252, 250), 14, line, 1)
        setOnClickListener { action() }
    }

    private fun bodyText(text: String, size: Float = 14f): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(ink)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun space(height: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun spaceWidth(width: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(dp(width), 1)
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(strokeWidth), stroke)
    }

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun captureNow() {
        lifecycleScope.launch {
            val result = TimelineCollector(this@MainActivity).capture("手动采集")
            refreshSchedule()
            showFeedback(result.message, result.success)
        }
    }

    private fun saveThought() {
        lifecycleScope.launch {
            val result = TimelineCollector(this@MainActivity).appendThought(thought.text.toString(), selectedImageUri)
            if (result.success) {
                thought.setText("")
                selectedImageUri = null
                updateSelectedImage()
            }
            showFeedback(result.message, result.success)
        }
    }

    private fun updateSelectedImage() {
        selectedImage.text = if (selectedImageUri == null) {
            "可选：附上一张当下的图片"
        } else {
            "已选择一张图片，保存时会写入当天的 screenshots 文件夹"
        }
    }

    private fun refreshSchedule() {
        if (preferences.treeUri == null) {
            scheduleTitle.text = "采集节奏"
            scheduleTime.text = "还未安排"
            scheduleDetail.text = "先在下方选择日志输出文件夹，系统才会开始每小时采集。"
            return
        }
        val next = preferences.nextExpectedCaptureMillis
        if (next <= 0L) {
            TrailWorker.schedule(this)
            refreshSchedule()
            return
        }
        if (next < System.currentTimeMillis()) {
            scheduleTitle.text = "采集节奏"
            scheduleTime.text = "正在等待系统补采"
            scheduleDetail.text = "系统省电时会推迟执行；恢复后会从上次位置补查，不会直接跳过中间轨迹。"
            return
        }
        val clock = SimpleDateFormat("HH:mm", Locale.CHINA).format(Date(next))
        scheduleTitle.text = "下一次预计采集"
        scheduleTime.text = "$clock 左右"
        scheduleDetail.text = "每小时一次；系统省电时可能延后，但会从上次位置补查。"
    }

    private fun showFeedback(message: String, success: Boolean) {
        notice.visibility = View.VISIBLE
        notice.text = if (success) "✓  $message" else "需要处理：$message"
        notice.setTextColor(if (success) teal else Color.rgb(132, 84, 24))
        notice.background = rounded(if (success) paleTeal else amberPale, 16)
    }
}
