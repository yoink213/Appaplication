package com.example.appaplication

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

class AdminAuditActivity : AppCompatActivity() {

    private lateinit var rvAdminAudit: RecyclerView
    private var auditList = ArrayList<OrderItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_audit)

        rvAdminAudit = findViewById(R.id.rvAdminAudit)
        rvAdminAudit.layoutManager = LinearLayoutManager(this)

        loadAuditOrders()
    }

    private fun loadAuditOrders() {
        lifecycleScope.launch {
            try {
                // ⚠️ 传入 role = 0 (管理员模式)，拉取全部订单
                val response = ApiClient.api.getPendingOrders(role = 0)
                if (response.isSuccessful && response.body() != null) {
                    auditList.clear()
                    // 仅显示 3-已送达 状态的订单用于终审
                    val filtered = response.body()!!.filter { it.status == 3 }
                    auditList.addAll(filtered)
                    rvAdminAudit.adapter = AuditAdapter(auditList)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminAuditActivity, "加载审核列表失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private fun executeAudit(orderId: Int, action: String) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.auditOrder(orderId, action)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.code == 200) {
                        Toast.makeText(this@AdminAuditActivity, result.message ?: "操作成功", Toast.LENGTH_SHORT).show()
                        loadAuditOrders()
                    } else {
                        Toast.makeText(this@AdminAuditActivity, result.message ?: "操作失败", Toast.LENGTH_SHORT).show()
                    }
                } else {
                    Toast.makeText(this@AdminAuditActivity, "审核处理失败：服务器响应异常", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminAuditActivity, "审核处理失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private inner class AuditAdapter(val list: List<OrderItem>) : RecyclerView.Adapter<AuditAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvOrderNo: TextView = v.findViewById(R.id.tvOrderNo)
            val tvDetails: TextView = v.findViewById(R.id.tvDetails)
            val tvPrice: TextView = v.findViewById(R.id.tvPrice)
            val btnAccept: Button = v.findViewById(R.id.btnAccept)
            val btnReject: Button = v.findViewById(R.id.btnReject)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_audit, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.tvOrderNo.text = item.orderNo
            holder.tvDetails.text = "小仓自提码: ${item.smallItemCode}\n货物：${item.itemName} | 数量: ${item.quantity} 件\n大仓源头货位: ${item.location}"
            holder.tvPrice.text = "待发放佣金: ¥${String.format("%.2f", item.totalPrice)}"

            // 同意并结算 -> 添加金额至该工人账户，订单设为 4
            holder.btnAccept.setOnClickListener {
                executeAudit(item.id, "ACCEPT")
            }

            // 驳回打回 -> 将订单设回 1 (已接单)，原工人名下需要重新取货配送
            holder.btnReject.setOnClickListener {
                executeAudit(item.id, "REJECT")
            }
        }

        override fun getItemCount(): Int = list.size
    }
}