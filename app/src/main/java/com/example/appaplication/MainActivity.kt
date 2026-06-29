package com.example.appaplication

import android.content.Intent
import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import androidx.cardview.widget.CardView

class MainActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 跳转小仓
        findViewById<CardView>(R.id.cardSmallWh).setOnClickListener {
            startActivity(Intent(this, SmallWhActivity::class.java))
        }

        // 跳转大仓
        findViewById<CardView>(R.id.cardLargeWh).setOnClickListener {
            startActivity(Intent(this, LargeWhTaskActivity::class.java))
        }
    }
}