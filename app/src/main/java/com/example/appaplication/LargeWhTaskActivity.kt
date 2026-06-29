package com.example.appaplication

import android.graphics.Color
import android.os.Bundle
import android.widget.Button
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LargeWhTaskActivity : AppCompatActivity() {
    private lateinit var taskContainer: LinearLayout

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_large_wh)

        taskContainer = findViewById(R.id.taskContainer)
        findViewById<Button>(R.id.btnRefresh).setOnClickListener { loadTasks() }

        loadTasks()
    }

    private fun loadTasks() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.getPendingTasks()
                if (response.isSuccessful && response.body() != null) {
                    renderTasks(response.body()!!)
                }
            } catch (e: Exception) {
                Toast.makeText(this@LargeWhTaskActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun renderTasks(tasks: List<Task>) {
        val childCount = taskContainer.childCount
        if (childCount > 2) {
            taskContainer.removeViews(2, childCount - 2) // 清除旧卡片
        }

        if (tasks.isEmpty()) {
            Toast.makeText(this, "当前没有待抢单任务~", Toast.LENGTH_SHORT).show()
            return
        }

        for (task in tasks) {
            val card = LinearLayout(this).apply {
                orientation = LinearLayout.VERTICAL
                setPadding(40, 40, 40, 40)
                setBackgroundColor(Color.parseColor("#FFFFFF"))
                val params = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                params.setMargins(0, 0, 0, 30)
                layoutParams = params
            }

            val tvInfo = TextView(this).apply {
                text = "单号: ${task.taskNo}\n去库位: 【${task.locationCode}】 取货\n商品ID: ${task.skuId}  |  数量: ${task.requireQty}"
                textSize = 16f
                setTextColor(Color.BLACK)
                setPadding(0, 0, 0, 20)
            }

            val btnClaim = Button(this).apply {
                text = "抢单并完成拣货"
                setBackgroundColor(Color.parseColor("#4CAF50"))
                setTextColor(Color.WHITE)
                setOnClickListener { finishTask(task.id) }
            }

            card.addView(tvInfo)
            card.addView(btnClaim)
            taskContainer.addView(card)
        }
    }

    private fun finishTask(taskId: Long) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.finishTask(taskId)
                if (response.isSuccessful) {
                    Toast.makeText(this@LargeWhTaskActivity, "🎉 抢单成功！", Toast.LENGTH_SHORT).show()
                    loadTasks() // 刷新列表
                } else {
                    Toast.makeText(this@LargeWhTaskActivity, "手慢了，任务失败！", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@LargeWhTaskActivity, "网络错误", Toast.LENGTH_SHORT).show()
            }
        }
    }
}