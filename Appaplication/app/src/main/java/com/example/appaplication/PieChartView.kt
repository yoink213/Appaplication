package com.example.appaplication

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class PieChartView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {

    private val paint = Paint(Paint.ANTI_ALIAS_FLAG)
    private val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 28f
        color = Color.parseColor("#2C3E50")
    }
    private val legendPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        textSize = 24f
        color = Color.parseColor("#2C3E50")
    }

    private var categories: List<CategoryStats> = emptyList()
    private val colors = listOf(
        "#3498DB", "#E74C3C", "#2ECC71", "#F39C12", "#9B59B6",
        "#1ABC9C", "#E67E22", "#34495E", "#16A085", "#D35400"
    )

    fun setData(data: List<CategoryStats>) {
        this.categories = data
        invalidate()
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        if (categories.isEmpty()) {
            textPaint.textAlign = Paint.Align.CENTER
            canvas.drawText("暂无数据", width / 2f, height / 2f, textPaint)
            return
        }

        val total = categories.sumOf { it.totalQuantity.toDouble() }
        val pieSize = minOf(width * 0.45f, height * 0.85f)
        val pieLeft = 20f
        val pieTop = (height - pieSize) / 2f
        val rectF = RectF(pieLeft, pieTop, pieLeft + pieSize, pieTop + pieSize)

        var startAngle = -90f
        categories.forEachIndexed { index, cat ->
            val sweepAngle = (cat.totalQuantity / total * 360).toFloat()
            paint.color = Color.parseColor(colors[index % colors.size])
            paint.style = Paint.Style.FILL
            canvas.drawArc(rectF, startAngle, sweepAngle, true, paint)
            startAngle += sweepAngle
        }

        val legendX = pieLeft + pieSize + 30f
        var legendY = 40f
        val legendSize = 24f

        categories.forEachIndexed { index, cat ->
            if (legendY > height - 20f) return@forEachIndexed

            paint.color = Color.parseColor(colors[index % colors.size])
            paint.style = Paint.Style.FILL
            canvas.drawRect(legendX, legendY, legendX + legendSize, legendY + legendSize, paint)

            val percent = String.format("%.1f%%", cat.totalQuantity / total * 100)
            canvas.drawText(
                "${cat.category} ($percent)",
                legendX + legendSize + 10f,
                legendY + legendSize * 0.75f,
                legendPaint
            )
            legendY += legendSize + 16f
        }
    }
}
