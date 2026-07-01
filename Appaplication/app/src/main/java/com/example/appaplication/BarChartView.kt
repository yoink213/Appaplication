package com.example.appaplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.View

class BarChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val barPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#3498DB")
        style = Paint.Style.FILL
    }
    private val gridPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#ECF0F1")
        strokeWidth = 2f
    }
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        color = Color.parseColor("#7F8C8D")
    }
    private val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 22f
        color = Color.parseColor("#2C3E50")
        textAlign = Paint.Align.CENTER
    }

    private var data: List<DailyOrderStats> = emptyList()

    fun setData(data: List<DailyOrderStats>) {
        this.data = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (data.isEmpty()) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("暂无数据", width / 2f, height / 2f, textPaint)
            return
        }

        val paddingTop = 30f
        val paddingBottom = 50f
        val chartHeight = height - paddingTop - paddingBottom
        val barWidth = (width - 40f) / data.size * 0.6f
        val gap = (width - 40f) / data.size * 0.4f

        val maxVal = maxOf(data.maxOfOrNull { it.orderCount } ?: 1, 1)

        for (i in 0..3) {
            val y = paddingTop + chartHeight * i / 3
            canvas.drawLine(20f, y, width - 20f, y, gridPaint)
        }

        data.forEachIndexed { index, item ->
            val barHeight = (item.orderCount.toFloat() / maxVal) * (chartHeight - 20f)
            val left = 20f + index * (barWidth + gap) + gap / 2
            val top = paddingTop + chartHeight - barHeight
            val right = left + barWidth
            val bottom = paddingTop + chartHeight

            val gradient = android.graphics.LinearGradient(
                left, top, left, bottom,
                Color.parseColor("#5DADE2"),
                Color.parseColor("#2E86C1"),
                android.graphics.Shader.TileMode.CLAMP
            )
            barPaint.shader = gradient
            canvas.drawRoundRect(left, top, right, bottom, 6f, 6f, barPaint)
            barPaint.shader = null

            canvas.drawText(item.orderCount.toString(), (left + right) / 2, top - 8f, valuePaint)

            textPaint.textAlign = Paint.Align.CENTER
            val dateShort = item.date.takeLast(5).replace("-", "/")
            canvas.drawText(dateShort, (left + right) / 2, bottom + 25f, textPaint)
        }
    }
}
