package com.zishu.personaltrail

import android.content.Intent
import android.graphics.Typeface
import android.graphics.drawable.GradientDrawable
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Button
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.TextView
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class ThoughtActivity : AppCompatActivity() {
    private var image: Uri? = null
    private lateinit var imageState: TextView
    private lateinit var thought: EditText
    private lateinit var feedback: TextView
    private val picker = registerForActivityResult(ActivityResultContracts.GetContent()) { uri -> image=uri; imageState.text=if(uri==null) "可选：关联一张图片" else "已选择图片，保存时会一并放进今天的记录" }
    override fun onCreate(savedInstanceState: Bundle?) { super.onCreate(savedInstanceState); setContentView(screen()) }
    private fun screen()=LinearLayout(this).apply {
        orientation=LinearLayout.VERTICAL; setPadding(dp(22),dp(22),dp(22),dp(22)); setBackgroundColor(getColor(R.color.background))
        addView(TextView(this@ThoughtActivity).apply{text="碎碎念";textSize=26f;typeface=Typeface.DEFAULT_BOLD})
        addView(TextView(this@ThoughtActivity).apply{text="不必整理成结论，先把它留住。";textSize=14f;setTextColor(getColor(R.color.on_surface_variant));setPadding(0,5,0,20)})
        thought=EditText(this@ThoughtActivity).apply{hint="写下一句话，哪怕它还不完整…";textSize=17f;gravity=Gravity.TOP;minLines=7;setPadding(dp(16),dp(14),dp(16),dp(14));background=box(getColor(R.color.surface),18,getColor(R.color.outline_variant))};addView(thought,LinearLayout.LayoutParams(-1,0,1f))
        addView(space(14)); imageState=TextView(this@ThoughtActivity).apply{text="可选：关联一张图片";textSize=13f;setPadding(dp(14),dp(12),dp(14),dp(12));background=box(getColor(R.color.primary_container),14)};addView(imageState)
        addView(space(8));addView(button("选择图片",false){picker.launch("image/*")},LinearLayout.LayoutParams(-1,dp(48)));addView(space(9));addView(button("留住这条想法",true){save()},LinearLayout.LayoutParams(-1,dp(52)))
        feedback=TextView(this@ThoughtActivity).apply{visibility=View.GONE;gravity=Gravity.CENTER;setPadding(8,8,8,8)};addView(feedback)
    }
    private fun save()=lifecycleScope.launch{val r=TimelineCollector(this@ThoughtActivity).appendThought(thought.text.toString(),image);feedback.visibility=View.VISIBLE;feedback.text=r.message;if(r.success){thought.setText("");image=null;imageState.text="可选：关联一张图片"}}
    private fun button(t:String,primary:Boolean,a:()->Unit)=Button(this).apply{text=t;isAllCaps=false;textSize=16f;setTextColor(if(primary)getColor(R.color.on_primary) else getColor(R.color.primary));background=box(if(primary)getColor(R.color.primary) else getColor(R.color.surface),16,if(primary)null else getColor(R.color.outline_variant));setOnClickListener{a()}}
    private fun space(n:Int)=View(this).apply{layoutParams=LinearLayout.LayoutParams(1,dp(n))};private fun dp(n:Int)=(n*resources.displayMetrics.density).toInt();private fun box(c:Int,r:Int,s:Int?=null)=GradientDrawable().apply{setColor(c);cornerRadius=dp(r).toFloat();if(s!=null)setStroke(dp(1),s)}
}
