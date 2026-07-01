package com.example.appaplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class TaskPoolActivity : AppCompatActivity() {

    private lateinit var rvTaskPool: RecyclerView
    private var orderList = ArrayList<OrderItem>()
    private var currentWorkerId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_task_pool)

        // 读取本地会话缓存以获取当前抢单工人ID
        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        currentWorkerId = sp.getInt("id", -1)

        rvTaskPool = findViewById(R.id.rvTaskPool)
        rvTaskPool.layoutManager = LinearLayoutManager(this)

        loadPendingTasks()
    }

    private fun loadPendingTasks() {
        lifecycleScope.launch {
            try {
                // ⚠️ 传入角色 1，代表工人身份。后端将自动过滤只显示 0-待接单 和 5-已拒绝 的任务
                val response = ApiClient.api.getPendingOrders(role = 1)
                if (response.isSuccessful && response.body() != null) {
                    orderList.clear()
                    orderList.addAll(response.body()!!)
                    rvTaskPool.adapter = TaskPoolAdapter(orderList)

                    if (orderList.isEmpty()) {
                        Toast.makeText(this@TaskPoolActivity, "小仓当前很干净，没有待接订单~", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@TaskPoolActivity, "Tomcat 响应失败，代码: ${response.code()}", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@TaskPoolActivity, "网络连接异常，请确认服务器已开且 IP 正确", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun executeClaimOrder(orderId: Int) {
        lifecycleScope.launch {
            try {
                // 阶段1抢单流转动作
                val response = ApiClient.api.executeWorkerAction("accept", orderId, currentWorkerId)
                if (response.isSuccessful) {
                    Toast.makeText(this@TaskPoolActivity, "抢单成功，请前往小仓提货！", Toast.LENGTH_SHORT).show()
                    loadPendingTasks() // 刷新可领取的任务池
                } else {
                    val errorMsg = response.errorBody()?.string() ?: ""
                    if (errorMsg.contains("3个进行中")) {
                        Toast.makeText(this@TaskPoolActivity, "接单失败！您手头已有3单未完成", Toast.LENGTH_LONG).show()
                    } else {
                        Toast.makeText(this@TaskPoolActivity, "抢单失败", Toast.LENGTH_SHORT).show()
                    }
                }
            } catch (e: Exception) {
                Toast.makeText(this@TaskPoolActivity, "网络传输异常", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 内部数据适配器 (UML: TaskPoolAdapter)
    private inner class TaskPoolAdapter(val list: List<OrderItem>) : RecyclerView.Adapter<TaskPoolAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvOrderNo: TextView = v.findViewById(R.id.tvOrderNo)
            val tvSmallCode: TextView = v.findViewById(R.id.tvSmallCode)
            val tvName: TextView = v.findViewById(R.id.tvTaskGoodsName)
            val tvLocation: TextView = v.findViewById(R.id.tvTaskLocation)
            val tvPrice: TextView = v.findViewById(R.id.tvPrice)
            val btnClaim: Button = v.findViewById(R.id.btnClaim)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_task_pool, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.tvOrderNo.text = item.orderNo
            holder.tvSmallCode.text = "小仓提货号: ${item.smallItemCode}"
            holder.tvName.text = "大仓对应：${item.itemName} (${item.itemCodeLarge})"
            holder.tvLocation.text = "📍 提货货架：${item.location}"
            holder.tvPrice.text = "任务运费: ¥${String.format("%.2f", item.totalPrice)}"

            holder.btnClaim.setOnClickListener {
                executeClaimOrder(item.id)
            }
        }

        override fun getItemCount(): Int = list.size
    }
}