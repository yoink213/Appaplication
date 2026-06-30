package com.example.appaplication

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class WarehouseActivity : AppCompatActivity() {

    private lateinit var rvWarehouse: RecyclerView
    private var itemsList = ArrayList<BigWarehouseItem>()
    private var userRole: Int = -1 // 0-管理员, 1-工人
    private var userId: Int = -1   // 用户ID

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_warehouse)

        // 1. 读取当前会话的用户角色
        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        userRole = sp.getInt("role", -1)
        userId = sp.getInt("id", -1)

        rvWarehouse = findViewById(R.id.rvWarehouse)
        rvWarehouse.layoutManager = LinearLayoutManager(this)

        loadWarehouseData()
    }

    private fun loadWarehouseData() {
        lifecycleScope.launch {
            try {
                val response = ApiClient.api.getWarehouseItems()
                if (response.isSuccessful && response.body() != null) {
                    itemsList.clear()
                    itemsList.addAll(response.body()!!)

                    // 2. 将用户角色和点击回调传递给适配器
                    rvWarehouse.adapter = WarehouseAdapter(itemsList, userRole) { clickedItem ->
                        // 只有管理员点击才会触发派单
                        showDispatchDialog(clickedItem)
                    }
                } else {
                    Toast.makeText(this@WarehouseActivity, "数据加载失败", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WarehouseActivity, "网络连接异常", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 3. 🔍【新增】管理员派活输入弹窗
    private fun showDispatchDialog(item: BigWarehouseItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_dispatch, null)
        val etQty = dialogView.findViewById<EditText>(R.id.etDispatchQty)
        val etPrice = dialogView.findViewById<EditText>(R.id.etDispatchPrice)
        val etDeadline = dialogView.findViewById<EditText>(R.id.etDispatchDeadline)

        AlertDialog.Builder(this)
            .setView(dialogView)
            .setPositiveButton("确认派发") { dialog, _ ->
                val qtyStr = etQty.text.toString().trim()
                val priceStr = etPrice.text.toString().trim()
                val deadlineStr = etDeadline.text.toString().trim()

                if (qtyStr.isEmpty() || priceStr.isEmpty() || deadlineStr.isEmpty()) {
                    Toast.makeText(this, "所有字段均不能为空", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                val qty = qtyStr.toInt()
                val price = priceStr.toDouble()

                if (qty > item.quantity) {
                    Toast.makeText(this, "派发数量超出大仓当前库存！", Toast.LENGTH_SHORT).show()
                    return@setPositiveButton
                }

                // 发送派活请求到后端
                executeDispatch(item.id, qty, price, deadlineStr)
                dialog.dismiss()
            }
            .setNegativeButton("取消", null)
            .show()
    }

    // 4. 🔍【新增】向后端提交派发任务事务
    private fun executeDispatch(bigWarehouseId: Int, qty: Int, price: Double, deadline: String) {
        lifecycleScope.launch {
            try {
                val req = DispatchReq(bigWarehouseId, qty, price, deadline, userId)
                val response = ApiClient.api.dispatchTask(req)
                if (response.isSuccessful) {
                    Toast.makeText(this@WarehouseActivity, "🎉 派单成功！库存扣除并已存入小仓任务池", Toast.LENGTH_LONG).show()
                    loadWarehouseData() // 刷新大仓列表（大仓库存会实时减少）
                } else {
                    Toast.makeText(this@WarehouseActivity, "指派失败：库存不足", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                Toast.makeText(this@WarehouseActivity, "网络错误，指派失败", Toast.LENGTH_SHORT).show()
            }
        }
    }

    // 5. 适配器类：支持管理员点击事件回调 (UML: WarehouseAdapter)
    private class WarehouseAdapter(
        val list: List<BigWarehouseItem>,
        val role: Int,
        val onItemClick: (BigWarehouseItem) -> Unit
    ) : RecyclerView.Adapter<WarehouseAdapter.VH>() {

        class VH(v: View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvItemName)
            val tvCode: TextView = v.findViewById(R.id.tvItemCode)
            val tvLocation: TextView = v.findViewById(R.id.tvLocation)
            val tvQty: TextView = v.findViewById(R.id.tvQuantity)
            val tvPrice: TextView = v.findViewById(R.id.tvPrice)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_warehouse, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = list[position]
            holder.tvName.text = item.name
            holder.tvCode.text = item.code
            holder.tvLocation.text = "📍 货架位置：${item.location}"
            holder.tvQty.text = "实时库存: ${item.quantity} 件"
            holder.tvPrice.text = "单价: ¥${String.format("%.2f", item.price)}"

            // 如果登录身份是管理员 (0)，绑定点击卡片事件
            if (role == 0) {
                holder.itemView.setOnClickListener {
                    onItemClick(item)
                }
            } else {
                holder.itemView.setOnClickListener(null) // 工人点击无效
            }
        }

        override fun getItemCount(): Int = list.size
    }
}