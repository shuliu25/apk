package com.zishu.personaltrail

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.os.Bundle
import android.provider.Settings
import android.view.View
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

class SettingsActivity : AppCompatActivity() {
    private lateinit var prefs: TrailPreferences
    private lateinit var state: TextView
    private val folderPicker=registerForActivityResult(ActivityResultContracts.OpenDocumentTree()){uri->if(uri!=null){val flags=Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION;contentResolver.takePersistableUriPermission(uri,flags);prefs.treeUri=uri.toString();TrailWorker.schedule(this);refresh()}}
    override fun onCreate(savedInstanceState: Bundle?){super.onCreate(savedInstanceState);prefs=TrailPreferences(this);setContentView(screen())}
    override fun onResume(){super.onResume();if(::state.isInitialized)refresh()}
    private fun screen()=LinearLayout(this).apply{orientation=LinearLayout.VERTICAL;setPadding(dp(22),dp(22),dp(22),dp(22));setBackgroundColor(getColor(R.color.background));addView(TextView(this@SettingsActivity).apply{text="采集与权限";textSize=26f;typeface=Typeface.DEFAULT_BOLD});addView(TextView(this@SettingsActivity).apply{text="只需在这里完成一次准备。";textSize=14f;setTextColor(getColor(R.color.on_surface_variant));setPadding(0,5,0,20)});state=TextView(this@SettingsActivity).apply{textSize=15f;setPadding(dp(18),dp(18),dp(18),dp(18));background=box(getColor(R.color.primary_container),18)};addView(state);addView(space(16));addView(button("授权查看应用使用情况"){startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))},LinearLayout.LayoutParams(-1,dp(52)));addView(space(10));addView(button("选择 Operit 的生活日志文件夹"){folderPicker.launch(null)},LinearLayout.LayoutParams(-1,dp(52)));addView(space(10));addView(button("重新安排每小时自动采集"){TrailWorker.schedule(this@SettingsActivity);refresh()},LinearLayout.LayoutParams(-1,dp(52)))}
    private fun refresh(){val usage=(getSystemService(APP_OPS_SERVICE) as android.app.AppOpsManager).checkOpNoThrow(android.app.AppOpsManager.OPSTR_GET_USAGE_STATS,android.os.Process.myUid(),packageName)==android.app.AppOpsManager.MODE_ALLOWED;val folder=prefs.treeUri!=null;val next=prefs.nextExpectedCaptureMillis;state.text="使用记录：${if(usage)"已授权" else "未授权"}\n日志位置：${if(folder)"已选择" else "未选择"}\n${if(folder&&next>0)"下一次预计：${SimpleDateFormat("HH:mm",Locale.CHINA).format(Date(next))} 左右" else "准备好后会显示下一次采集时间"}"}
    private fun button(t:String,a:()->Unit)=Button(this).apply{text=t;isAllCaps=false;textSize=15f;setTextColor(getColor(R.color.primary));background=box(getColor(R.color.surface),16,getColor(R.color.outline_variant));setOnClickListener{a()}};private fun space(n:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(n))};private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt();private fun box(c:Int,r:Int,s:Int?=null)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();if(s!=null)setStroke(dp(1),s)}
}
