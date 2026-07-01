package com.example.appaplication

import android.content.Context
import android.graphics.Color
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

class MyOrdersActivity : AppCompatActivity() {

    private lateinit var rvMyOrders: RecyclerView
    private var orderList = ArrayList<OrderItem>()
    private var workerId: Int = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_my_orders)

        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        workerId = sp.getInt("id", -1)

        rvMyOrders = findViewById(R.id.rvMyOrders)
        rvMyOrders.layoutManager = LinearLayoutManager(this)

        loadMyOrders()
    }

    private fun loadMyOrders() {
        lifecycleScope.launch {
            try {
                // ⚠️ 传入自身 id 且 role = 1，拉取自己名下进行中任务
                val response = ApiClient.api.getWorkerActiveOrders(role = 1, workerId = workerId)
                if (response.isSuccessful && response.body() != null) {
                    orderList.clear()
                    orderList.addAll(response.body()!!)
                    rvMyOrders.adapter = MyOrderAdapter(orderList)
                }
            } catch (e: Exception) {
                Toast.makeText(this@MyOrdersActivity, "获取个人订单失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun executeStageAction(action: String, orderId: Int) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.executeWorkerAction(action, orderId, workerId)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.code == 200) {
                        Toast.makeText(this@MyOrdersActivity, result.message ?: "操作成功", Toast.LENGTH_SHORT).show()
                        loadMyOrders()
                    } else {
                        Toast.makeText(this@MyOrdersActivity, result.message ?: "操作失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@MyOrdersActivity, "操作失败：服务器响应异常", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@MyOrdersActivity, "网络传输异常", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private inner class MyOrderAdapter(val list: List<OrderItem>) : RecyclerView.Adapter<MyOrderAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvOrderNo: TextView = v.findViewById(R.id.tvOrderNo)
            val tvStatus: TextView = v.findViewById(R.id.tvStatus)
            val tvDetails: TextView = v.findViewById(R.id.tvDetails)
            val tvPrice: TextView = v.findViewById(R.id.tvPrice)
            val btnAction: Button = v.findViewById(R.id.btnStageAction)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_my_order, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.tvOrderNo.text = item.orderNo
            holder.tvDetails.text = "小仓自提码: ${item.smallItemCode}\n大仓对应: ${item.itemName} (${item.itemCodeLarge})\n📍 提货位置: ${item.location}"
            holder.tvPrice.text = "配送运费: ¥${String.format("%.2f", item.totalPrice)}"

            // 根据不同的配送状态，显示对应的流程控制按钮
            when (item.status) {
                1 -> { // 已接单 -> 进行取货
                    holder.tvStatus.text = "已领单 (待取货)"
                    holder.tvStatus.setTextColor(Color.parseColor("#E67E22"))
                    holder.btnAction.text = "步骤 2：确认已到仓取到货"
                    holder.btnAction.setBackgroundColor(Color.parseColor("#3498DB"))
                    holder.btnAction.isEnabled = true
                    holder.btnAction.setOnClickListener { executeStageAction("pickup", item.id) }
                }
                2 -> { // 已取货 -> 进行配送送达
                    holder.tvStatus.text = "已提货 (配送中)"
                    holder.tvStatus.setTextColor(Color.parseColor("#3498DB"))
                    holder.btnAction.text = "步骤 3：确认已送达终端"
                    holder.btnAction.setBackgroundColor(Color.parseColor("#2ECC71"))
                    holder.btnAction.isEnabled = true
                    holder.btnAction.setOnClickListener { executeStageAction("deliver", item.id) }
                }
                3 -> { // 已送达 -> 等待审核
                    holder.tvStatus.text = "已送达 (待审核)"
                    holder.tvStatus.setTextColor(Color.parseColor("#95A5A6"))
                    holder.btnAction.text = "等待大仓管理员终审结算"
                    holder.btnAction.setBackgroundColor(Color.parseColor("#BDC3C7"))
                    holder.btnAction.isEnabled = false // 禁用按钮，等待管理确认
                }
            }
        }

        override fun getItemCount(): Int = list.size
    }
}