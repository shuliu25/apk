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

class MainActivity : AppCompatActivity() {
    private val paper = Color.rgb(247, 245, 239)
    private val ink = Color.rgb(38, 54, 49)
    private val teal = Color.rgb(53, 104, 89)
    private val paleTeal = Color.rgb(231, 240, 235)
    private val line = Color.rgb(210, 222, 216)
    private val amber = Color.rgb(217, 154, 59)

    private lateinit var preferences: TrailPreferences
    private lateinit var status: TextView
    private lateinit var thought: EditText
    private lateinit var selectedImage: TextView
    private var selectedImageUri: Uri? = null

    private val folderPicker = registerForActivityResult(ActivityResultContracts.OpenDocumentTree()) { uri ->
        if (uri == null) return@registerForActivityResult
        val flags = Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        contentResolver.takePersistableUriPermission(uri, flags)
        preferences.treeUri = uri.toString()
        TrailWorker.schedule(this)
        refreshStatus("输出位置已设置，后台采集已开始")
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
        setContentView(buildScreen())
        refreshStatus()
    }

    private fun buildScreen(): ScrollView {
        val root = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(dp(20), dp(22), dp(20), dp(32))
            background = solid(paper, 0)
        }

        root.addView(hero())
        root.addView(space(14))
        status = bodyText("").apply {
            setPadding(dp(16), dp(13), dp(16), dp(13))
            background = rounded(paleTeal, 16)
        }
        root.addView(status)

        root.addView(sectionTitle("准备好记录"))
        root.addView(card("只需完成一次设置", "系统记录使用轨迹，Operit 负责推送到 GitHub") {
            addView(actionButton("授权使用情况访问", false) {
                startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            })
            addView(space(10))
            addView(actionButton("选择日志输出文件夹", false) { folderPicker.launch(null) })
        })

        root.addView(sectionTitle("此刻"))
        root.addView(card("让今天有一条可回看的线", "每小时补采一次；也可以现在手动采集") {
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

        root.addView(bodyText("你的内容只写入你选择的文件夹。应用不保存 GitHub 密钥；由 Operit 负责同步。", 13f).apply {
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

    private fun bodyText(text: String, size: Float = 14f): TextView = TextView(this).apply {
        this.text = text
        textSize = size
        setTextColor(ink)
        gravity = Gravity.CENTER_VERTICAL
    }

    private fun space(height: Int): View = View(this).apply {
        layoutParams = LinearLayout.LayoutParams(1, dp(height))
    }

    private fun rounded(color: Int, radius: Int, stroke: Int? = null, strokeWidth: Int = 0): GradientDrawable = GradientDrawable().apply {
        setColor(color)
        cornerRadius = dp(radius).toFloat()
        if (stroke != null) setStroke(dp(strokeWidth), stroke)
    }

    private fun solid(color: Int, radius: Int): GradientDrawable = rounded(color, radius)

    private fun dp(value: Int): Int = (value * resources.displayMetrics.density).toInt()

    private fun captureNow() {
        lifecycleScope.launch {
            val result = TimelineCollector(this@MainActivity).capture("手动采集")
            refreshStatus(result.message)
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
            refreshStatus(result.message)
        }
    }

    private fun updateSelectedImage() {
        selectedImage.text = if (selectedImageUri == null) {
            "可选：附上一张当下的图片"
        } else {
            "已选择一张图片，保存时会写入当天的 screenshots 文件夹"
        }
    }

    private fun refreshStatus(message: String? = null) {
        val folder = if (preferences.treeUri == null) "还未设置输出文件夹" else "输出位置已设置"
        status.text = listOfNotNull(message, "状态：$folder").joinToString("\n")
    }
}
