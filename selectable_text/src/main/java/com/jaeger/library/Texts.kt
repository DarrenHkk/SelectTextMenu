package com.jaeger.library

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.support.v7.widget.AppCompatTextView
import android.text.Spannable
import android.util.AttributeSet

class Texts @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : AppCompatTextView(context, attrs, defStyleAttr) {


    private val metrics = Paint.FontMetrics()
    override fun onDraw(canvas: Canvas?) {
        val layout = layout
        if (layout != null) {
            val text = text
            if (text is Spannable) {
                val spans = text.getSpans(0, text.length, BgColorSpan::class.java)
                spans?.forEach {
                    val paddingLeft = paddingLeft
                    val spanStart = text.getSpanStart(it)
                    val spanEnd = text.getSpanEnd(it)
                    val startLine = layout.getLineForOffset(spanStart)
                    val endLine = layout.getLineForOffset(spanEnd)
                    val startX = layout.getPrimaryHorizontal(spanStart) + paddingLeft
                    val endX = layout.getPrimaryHorizontal(spanEnd) + paddingLeft

                    val paint = paint
                    val oldColor = paint.color
                    val oldStyle = paint.style
                    paint.color = it.color
                    paint.style = Paint.Style.FILL

                    val lineHeight = paint.getFontMetrics(metrics)

                    if (startLine == endLine) {
                        val lineTop = layout.getLineTop(startLine)
                        canvas?.drawRect(startX, lineTop.toFloat(), endX, lineTop.toFloat() + lineHeight, paint)
                    } else {
                        val startLineTop = layout.getLineTop(startLine)
                        canvas?.drawRect(startX, startLineTop.toFloat(), layout.getLineRight(startLine) + paddingLeft, startLineTop + lineHeight, paint)
                        val endLineTop = layout.getLineTop(endLine)
                        canvas?.drawRect(layout.getLineLeft(endLine) + paddingLeft, endLineTop.toFloat(), endX, endLineTop + lineHeight, paint)

                        for (i in startLine + 1 until endLine) {
                            val top = layout.getLineTop(i)
                            canvas?.drawRect(layout.getLineLeft(i) + paddingLeft, top.toFloat(), layout.getLineRight(i) + paddingLeft, top + lineHeight, paint)
                        }

                    }

                    paint.color = oldColor
                    paint.style = oldStyle
                }
            }
        } else {
            post {
                invalidate()
            }
        }
        super.onDraw(canvas)

    }
//超链接点击
//    @SuppressLint("ClickableViewAccessibility")
//    override fun onTouchEvent(event: MotionEvent?): Boolean {
//        try {
//            val text = text as? Spanned ?: return false
//            val motionEvent = event ?: return false
//
//            val action = motionEvent.action
//            if (action == MotionEvent.ACTION_UP || action == MotionEvent.ACTION_DOWN) {
//                var x = motionEvent.x.toInt()
//                var y = motionEvent.y.toInt()
//                x -= totalPaddingLeft
//                y -= totalPaddingTop
//
//                x += scrollX
//                y += scrollY
//
//                val layout = layout ?: return false
//                val line = layout.getLineForVertical(y)
//                val off = layout.getOffsetForHorizontal(line, x.toFloat())
//
//                val links = text.getSpans(off, off, ClickableSpan::class.java)
//                if (links.isNotEmpty()) {
//                    if (action == MotionEvent.ACTION_UP) {
//                        links[0].onClick(this)
//                    } else if (action == MotionEvent.ACTION_DOWN) {
//                        //                    Selection.setSelection(text, text.getSpanStart(links[0]), text.getSpanEnd(links[0]))
//                    }
//                    return true
//                } else {
//                    //                Selection.removeSelection(text)
//                }
//            }
//        } catch (e: Throwable) {
//            Log.w("texts touch ", e)
//        }
//
//        return false
//    }
}