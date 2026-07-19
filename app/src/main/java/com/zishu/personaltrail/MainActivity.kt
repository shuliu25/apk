package com.zishu.personaltrail

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.view.Gravity
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
        refreshStatus("已记住这个文件夹，并开始后台采集")
    }

    private val imagePicker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri ->
        selectedImageUri = uri
        updateSelectedImage()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = TrailPreferences(this)
        setContentView(buildScreen())
        refreshStatus()
    }

    private fun buildScreen(): ScrollView {
        val pad = (20 * resources.displayMetrics.density).toInt()
        val column = LinearLayout(this).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(pad, pad * 2, pad, pad)
        }
        fun title(text: String) = TextView(this).apply {
            this.text = text
            textSize = 24f
            setPadding(0, 0, 0, pad / 2)
        }
        fun paragraph(text: String) = TextView(this).apply {
            this.text = text
            textSize = 15f
            setPadding(0, 0, 0, pad / 2)
        }
        fun button(text: String, action: () -> Unit) = Button(this).apply {
            this.text = text
            setOnClickListener { action() }
        }
        column.addView(title("个人轨迹"))
        column.addView(paragraph("不需要 Shizuku。第一次完成授权后，它会每小时左右补采手机使用轨迹。"))
        status = paragraph("")
        column.addView(status)
        column.addView(button("1. 授权使用情况访问") {
            startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
        })
        column.addView(button("2. 选择日志输出文件夹") { folderPicker.launch(null) })
        column.addView(button("现在采集一次") { captureNow() })
        column.addView(paragraph("碎碎念（可选：手动选择一张关联图片）："))
        thought = EditText(this).apply {
            hint = "留下一句话，不必整理好"
            minLines = 3
            gravity = Gravity.TOP
            layoutParams = LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT)
        }
        column.addView(thought)
        selectedImage = paragraph("未选择图片")
        column.addView(selectedImage)
        column.addView(button("选择关联图片（可选）") { imagePicker.launch("image/*") })
        column.addView(button("记录这句碎碎念") { saveThought() })
        column.addView(paragraph("提示：选择 Operit 正在同步到 GitHub 的仓库根目录；本应用负责记录，Operit 只负责推送。本应用不保存 GitHub 密钥。"))
        return ScrollView(this).apply { addView(column) }
    }

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
        selectedImage.text = if (selectedImageUri == null) "未选择图片" else "已选择一张图片，记录时会复制进当天的 screenshots 文件夹"
    }

    private fun refreshStatus(message: String? = null) {
        val folder = if (preferences.treeUri == null) "尚未选择" else "已选择"
        status.text = listOfNotNull(message, "日志文件夹：$folder｜图片在记录碎碎念时按需选择").joinToString("\n")
    }
}
