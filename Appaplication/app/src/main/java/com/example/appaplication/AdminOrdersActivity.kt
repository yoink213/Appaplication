package com.example.appaplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class AdminOrdersActivity : AppCompatActivity() {

    private lateinit var rvAdminOrders: RecyclerView
    private var orderList = ArrayList<OrderItem>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_admin_orders)

        rvAdminOrders = findViewById(R.id.rvAdminOrders)
        rvAdminOrders.layoutManager = LinearLayoutManager(this)

        loadAllOrders()
    }

    private fun loadAllOrders() {
        lifecycleScope.launch {
            try {
                // ⚠️ 传入角色 0，代表管理员。后端会返回所有订单列表
                val response = ApiClient.api.getPendingOrders(role = 0)
                if (response.isSuccessful && response.body() != null) {
                    orderList.clear()
                    orderList.addAll(response.body()!!)
                    rvAdminOrders.adapter = AdminOrderAdapter(orderList)
                }
            } catch (e: Exception) {
                Toast.makeText(this@AdminOrdersActivity, "加载订单失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 执行修改订单接口调用（与答案保持一致）
    private fun executeUpdate(orderId: Int, newPrice: Double, newDeadline: String) {
        lifecycleScope.launch {
            try {
                val req = OrderUpdateReq(orderId, newPrice, newDeadline)
                val response = ApiClient.api.updateOrder(req)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.code == 200) {
                        Toast.makeText(this@AdminOrdersActivity, "订单修改成功！", Toast.LENGTH_SHORT).show()
                        loadAllOrders()
                    } else {
                        Toast.makeText(this@AdminOrdersActivity, result.message ?: "修改失败", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@AdminOrdersActivity, "修改失败：服务器响应异常", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AdminOrdersActivity, "网络异常: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    // 执行删除并退库的接口调用（与答案保持一致）
    private fun executeDelete(orderId: Int) {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.deleteOrder(orderId)
                if (response.isSuccessful && response.body() != null) {
                    val result = response.body()!!
                    if (result.code == 200) {
                        Toast.makeText(this@AdminOrdersActivity, "订单已安全撤销，大仓库存已返还！", Toast.LENGTH_LONG).show()
                        loadAllOrders()
                    } else {
                        Toast.makeText(this@AdminOrdersActivity, result.message ?: "撤销失败", Toast.LENGTH_LONG).show()
                    }
                } else {
                    Toast.makeText(this@AdminOrdersActivity, "撤销失败：服务器响应异常", Toast.LENGTH_LONG).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@AdminOrdersActivity, "网络异常: ${e.message}", Toast.LENGTH_LONG).show()
            }
        }
    }

    private inner class AdminOrderAdapter(val list: List<OrderItem>) : RecyclerView.Adapter<AdminOrderAdapter.VH>() {
        inner class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvOrderNo: TextView = v.findViewById(R.id.tvOrderNo)
            val tvStatus: TextView = v.findViewById(R.id.tvStatus)
            val tvDetails: TextView = v.findViewById(R.id.tvDetails)
            val tvPrice: TextView = v.findViewById(R.id.tvPrice)
            val btnEdit: Button = v.findViewById(R.id.btnEdit)
            val btnDelete: Button = v.findViewById(R.id.btnDelete)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_admin_order, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.tvOrderNo.text = item.orderNo
            holder.tvDetails.text = "商品：${item.itemName} | 数量: ${item.quantity} 件\n提货位置: ${item.location}"
            holder.tvPrice.text = "派送运费: ¥${String.format("%.2f", item.totalPrice)}"

            // 状态渲染
            val statusStr = when(item.status) {
                0 -> "待接单"
                1 -> "已接单"
                2 -> "已取货"
                3 -> "已送达"
                4 -> "已完成"
                5 -> "已拒绝"
                else -> "未知"
            }
            holder.tvStatus.text = statusStr

            // 绑定删除按钮
            holder.btnDelete.setOnClickListener {
                AlertDialog.Builder(this@AdminOrdersActivity)
                    .setTitle("撤销警告")
                    .setMessage("确定要撤销并删除 ${item.orderNo} 吗？这会全额返还大仓库存！")
                    .setPositiveButton("确定") { _, _ -> executeDelete(item.id) }
                    .setNegativeButton("取消", null)
                    .show()
            }

            // 绑定修改按钮
            holder.btnEdit.setOnClickListener {
                val dialogView = LayoutInflater.from(this@AdminOrdersActivity).inflate(R.layout.dialog_dispatch, null)
                dialogView.findViewById<TextView>(R.id.tvDialogTitle).text = "✏️ 修改当前订单"
                val etQty = dialogView.findViewById<EditText>(R.id.etDispatchQty)
                etQty.visibility = View.GONE // 修改时不建议动大仓扣数，直接隐藏该框

                val etPrice = dialogView.findViewById<EditText>(R.id.etDispatchPrice)
                etPrice.setText(item.totalPrice.toString())
                val etDeadline = dialogView.findViewById<EditText>(R.id.etDispatchDeadline)
                etDeadline.setText(item.deadline)

                AlertDialog.Builder(this@AdminOrdersActivity)
                    .setView(dialogView)
                    .setPositiveButton("保存修改") { _, _ ->
                        val price = etPrice.text.toString().toDoubleOrNull() ?: 0.0
                        val deadline = etDeadline.text.toString()
                        executeUpdate(item.id, price, deadline)
                    }
                    .setNegativeButton("取消", null)
                    .show()
            }
        }

        override fun getItemCount(): Int = list.size
    }
}
