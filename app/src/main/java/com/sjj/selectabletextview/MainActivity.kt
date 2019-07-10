package com.sjj.selectabletextview

import android.graphics.Color
import android.support.v7.app.AppCompatActivity
import android.os.Bundle
import com.jaeger.library.SelectableTextHelper
import kotlinx.android.synthetic.main.activity_main.*
import sjj.alog.Log

class MainActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        val selectedColor = Color.parseColor("#3D00CF7A")

        val mSelectableTextHelper = SelectableTextHelper.Builder(texts)
            .setSelectedColor(selectedColor)
            .setCursorHandleSizeInDp(20F)
            .setCursorHandleColor(Color.parseColor("#00CF7A"))
            .build()
        mSelectableTextHelper.setSelectListener {
            Log.e(it)
        }

    }
}
