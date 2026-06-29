package com.example.appaplication

import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class SmallWhActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_small_wh)

        val etSkuId = findViewById<EditText>(R.id.etSkuId)
        val etQty = findViewById<EditText>(R.id.etQty)

        findViewById<Button>(R.id.btnOutbound).setOnClickListener {
            val skuIdStr = etSkuId.text.toString()
            val qtyStr = etQty.text.toString()

            if (skuIdStr.isEmpty()) {
                Toast.makeText(this, "商品ID不能为空！", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            val req = OutboundReq(locationId = 1L, skuId = skuIdStr.toLong(), qty = qtyStr.toInt())

            // 发起网络请求
            lifecycleScope.launch {
                try {
                    val response = ApiClient.api.smallOutbound(req)
                    if (response.isSuccessful) {
                        Toast.makeText(this@SmallWhActivity, "叮！出库成功！", Toast.LENGTH_LONG).show()
                        etSkuId.text.clear()
                    } else {
                        Toast.makeText(this@SmallWhActivity, "失败: 检查后端库存", Toast.LENGTH_SHORT).show()
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@SmallWhActivity, "网络错误: 后端没开吧？", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }
}