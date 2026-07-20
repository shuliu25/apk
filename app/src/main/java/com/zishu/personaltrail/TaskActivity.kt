package com.zishu.personaltrail

import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class TaskActivity : AppCompatActivity() {
    private val prefs by lazy { TrailPreferences(this) }
    private lateinit var state: TextView
    private lateinit var input: EditText
    private lateinit var action: Button
    private lateinit var feedback: TextView
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(screen()); refresh() }
    override fun onResume() { super.onResume(); if (::state.isInitialized) refresh() }
    private fun screen() = LinearLayout(this).apply {
        orientation = LinearLayout.VERTICAL; setPadding(dp(22), dp(22), dp(22), dp(22)); setBackgroundColor(getColor(R.color.background))
        addView(title("进行中的事", "给当前这一段命名，它会和应用切换一起写入轨迹。"))
        addView(space(24)); state = TextView(this@TaskActivity).apply { textSize=18f; typeface=Typeface.DEFAULT_BOLD; setPadding(dp(18),dp(18),dp(18),dp(18)); background=box(getColor(R.color.primary_container),20) }; addView(state)
        addView(space(16)); input = EditText(this@TaskActivity).apply { hint="例如：读书、学习方剂、出门办事"; textSize=16f; setPadding(dp(16),0,dp(16),0); background=box(getColor(R.color.surface),16,getColor(R.color.outline_variant)); layoutParams=LinearLayout.LayoutParams(-1,dp(54)) }; addView(input)
        addView(space(12)); action = Button(this@TaskActivity).apply { isAllCaps=false; textSize=16f; setTextColor(getColor(R.color.on_primary)); background=box(getColor(R.color.primary),16); setOnClickListener { toggle() } }; addView(action, LinearLayout.LayoutParams(-1,dp(54)))
        addView(space(12)); feedback = TextView(this@TaskActivity).apply { gravity=Gravity.CENTER; visibility=View.GONE; setPadding(8,8,8,8) }; addView(feedback)
    }
    private fun toggle() = lifecycleScope.launch { val start=prefs.activeTask==null; val r=if(start) TimelineCollector(this@TaskActivity).startTask(input.text.toString()) else TimelineCollector(this@TaskActivity).finishTask(); feedback.visibility=View.VISIBLE; feedback.text=r.message; refresh(); if(r.success&&start) input.setText("") }
    private fun refresh(){ val task=prefs.activeTask; if(task==null){state.text="还没有开始记录"; input.visibility=View.VISIBLE; action.text="开始记录这件事"}else{state.text="正在进行：$task\n${SimpleDateFormat("HH:mm",Locale.CHINA).format(Date(prefs.activeTaskStartedMillis))} 开始"; input.visibility=View.GONE; action.text="结束「$task」"} }
    private fun title(t:String,s:String)=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL; addView(TextView(this@TaskActivity).apply{text=t;textSize=26f;typeface=Typeface.DEFAULT_BOLD});addView(TextView(this@TaskActivity).apply{text=s;textSize=14f;setTextColor(getColor(R.color.on_surface_variant));setPadding(0,5,0,0)})}
    private fun space(n:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(n))}; private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt(); private fun box(c:Int,r:Int,stroke:Int?=null)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();if(stroke!=null)setStroke(dp(1),stroke)}
}
