package com.example.appaplication

import android.os.Bundle
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class DashboardActivity : AppCompatActivity() {

    private lateinit var tvTotalTypes: TextView
    private lateinit var tvTotalStock: TextView
    private lateinit var tvTodayOrders: TextView
    private lateinit var tvPendingOrders: TextView
    private lateinit var tvDeliveryRate: TextView
    private lateinit var tvLowStock: TextView
    private lateinit var tvTotalValue: TextView
    private lateinit var pieChartView: PieChartView
    private lateinit var barChartView: BarChartView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_dashboard)

        supportActionBar?.title = "📊 数据看板"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        tvTotalTypes = findViewById(R.id.tvTotalTypes)
        tvTotalStock = findViewById(R.id.tvTotalStock)
        tvTodayOrders = findViewById(R.id.tvTodayOrders)
        tvPendingOrders = findViewById(R.id.tvPendingOrders)
        tvDeliveryRate = findViewById(R.id.tvDeliveryRate)
        tvLowStock = findViewById(R.id.tvLowStock)
        tvTotalValue = findViewById(R.id.tvTotalValue)
        pieChartView = findViewById(R.id.pieChartView)
        barChartView = findViewById(R.id.barChartView)

        loadDashboardData()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadDashboardData() {
        lifecycleScope.launch {
            try {
                val summaryResp = ApiClient.api.getStatsSummary()
                if (summaryResp.isSuccessful && summaryResp.body() != null) {
                    val s = summaryResp.body()!!
                    tvTotalTypes.text = s.totalItemTypes.toString()
                    tvTotalStock.text = formatNumber(s.totalStock)
                    tvTodayOrders.text = s.todayOrders.toString()
                    tvPendingOrders.text = s.pendingOrders.toString()
                    tvDeliveryRate.text = "${s.deliveryRate}%"
                    tvLowStock.text = s.lowStockCount.toString()
                    tvTotalValue.text = "¥${formatNumber(s.totalStockValue.toInt())}"
                } else {
                    val errorBody = summaryResp.errorBody()?.string() ?: "未知错误"
                    android.util.Log.e("Dashboard", "概览数据加载失败: code=${summaryResp.code()}, body=$errorBody")
                    Toast.makeText(this@DashboardActivity, "概览数据加载失败: ${summaryResp.code()}", Toast.LENGTH_SHORT).show()
                }

                val categoryResp = ApiClient.api.getCategoryStats()
                if (categoryResp.isSuccessful && categoryResp.body() != null) {
                    pieChartView.setData(categoryResp.body()!!)
                }

                val trendResp = ApiClient.api.getOrderTrend(7)
                if (trendResp.isSuccessful && trendResp.body() != null) {
                    barChartView.setData(trendResp.body()!!)
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@DashboardActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun formatNumber(num: Int): String {
        return if (num >= 10000) {
            String.format("%.1f万", num / 10000.0)
        } else {
            num.toString()
        }
    }
}
