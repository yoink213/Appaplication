package com.example.appaplication

import android.os.Bundle
import android.view.LayoutInflater
import android.view.ViewGroup
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.coroutines.launch

class ReplenishActivity : AppCompatActivity() {

    private lateinit var rvReplenish: RecyclerView
    private var list = ArrayList<ReplenishSuggestion>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_replenish)

        supportActionBar?.title = "💡 智能补货建议"
        supportActionBar?.setDisplayHomeAsUpEnabled(true)

        rvReplenish = findViewById(R.id.rvReplenish)
        rvReplenish.layoutManager = LinearLayoutManager(this)

        loadData()
    }

    override fun onSupportNavigateUp(): Boolean {
        finish()
        return true
    }

    private fun loadData() {
        lifecycleScope.launch {
            try {
                val resp = ApiClient.api.getReplenishSuggestions()
                if (resp.isSuccessful && resp.body()?.code == 200 && resp.body()?.data != null) {
                    list.clear()
                    list.addAll(resp.body()!!.data!!)
                    rvReplenish.adapter = ReplenishAdapter(list)
                } else {
                    Toast.makeText(this@ReplenishActivity, "暂无补货建议", Toast.LENGTH_SHORT).show()
                }
            } catch (e: Exception) {
                e.printStackTrace()
                Toast.makeText(this@ReplenishActivity, "加载失败: ${e.message}", Toast.LENGTH_SHORT).show()
            }
        }
    }

    private class ReplenishAdapter(val data: List<ReplenishSuggestion>) :
        RecyclerView.Adapter<ReplenishAdapter.VH>() {

        class VH(v: android.view.View) : RecyclerView.ViewHolder(v) {
            val tvName: TextView = v.findViewById(R.id.tvItemName)
            val tvCode: TextView = v.findViewById(R.id.tvItemCode)
            val tvCurrent: TextView = v.findViewById(R.id.tvCurrentQty)
            val tvSuggested: TextView = v.findViewById(R.id.tvSuggestedQty)
            val tvCost: TextView = v.findViewById(R.id.tvEstCost)
            val tvReason: TextView = v.findViewById(R.id.tvReason)
        }

        override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): VH {
            val v = LayoutInflater.from(parent.context).inflate(R.layout.item_replenish, parent, false)
            return VH(v)
        }

        override fun onBindViewHolder(holder: VH, position: Int) {
            val item = data[position]
            holder.tvName.text = item.name
            holder.tvCode.text = item.code
            holder.tvCurrent.text = "${item.currentQuantity} 件"
            holder.tvSuggested.text = "${item.suggestedQuantity} 件"
            holder.tvCost.text = "¥${String.format("%.2f", item.estimatedCost)}"
            holder.tvReason.text = "💡 ${item.reason}"
        }

        override fun getItemCount(): Int = data.size
    }
}
