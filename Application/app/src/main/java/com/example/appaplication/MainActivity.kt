package com.example.appaplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.view.MenuItem
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.ActionBarDrawerToggle
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.Toolbar
import androidx.core.view.GravityCompat
import androidx.drawerlayout.widget.DrawerLayout
import com.google.android.material.navigation.NavigationView

class MainActivity : AppCompatActivity(), NavigationView.OnNavigationItemSelectedListener {

    private lateinit var drawerLayout: DrawerLayout
    private lateinit var navigationView: NavigationView
    private lateinit var toolbar: Toolbar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        // 1. 获取本地存储的用户会话
        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val id = sp.getInt("id", -1)
        val role = sp.getInt("role", -1)
        val realName = sp.getString("realName", "")
        val balance = sp.getFloat("balance", 0.0f)

        // 拦截机制：如未正常登录强制退回登录界面
        if (id == -1) {
            startActivity(Intent(this, LoginActivity::class.java))
            finish()
            return
        }

        // 2. 初始化 Toolbar
        toolbar = findViewById(R.id.toolbar)
        setSupportActionBar(toolbar)

        // 3. 配置侧边栏抽屉 Toggle
        drawerLayout = findViewById(R.id.drawerLayout)
        navigationView = findViewById(R.id.navigationView)
        navigationView.setNavigationItemSelectedListener(this)

        val toggle = ActionBarDrawerToggle(
            this, drawerLayout, toolbar,
            R.string.navigation_drawer_open, R.string.navigation_drawer_close
        )
        drawerLayout.addDrawerListener(toggle)
        toggle.syncState()

        // 4. 动态绑定侧边栏头部信息
        val headerView = navigationView.getHeaderView(0)
        val tvNavName = headerView.findViewById<TextView>(R.id.tvNavName)
        val tvNavRole = headerView.findViewById<TextView>(R.id.tvNavRole)
        val tvNavBalance = headerView.findViewById<TextView>(R.id.tvNavBalance)

        tvNavName.text = realName
        if (role == 0) {
            tvNavRole.text = "身份：管理员"
            tvNavBalance.visibility = TextView.GONE // 管理员隐藏余额
        } else {
            tvNavRole.text = "身份：配送工人"
            tvNavBalance.text = "账户余额：¥${String.format("%.2f", balance)}"
        }

        // 1. 创建一个默认关闭（false）的返回键拦截回调
        val onBackPressedCallback = object : androidx.activity.OnBackPressedCallback(false) {
            override fun handleOnBackPressed() {
                // 当侧边栏开启时被触发，执行关闭侧边栏操作
                drawerLayout.closeDrawer(androidx.core.view.GravityCompat.START)
            }
        }

        // 2. 将回调注册到系统的返回键分发器中
        onBackPressedDispatcher.addCallback(this, onBackPressedCallback)

        // 3. 监听侧边栏的开合状态，动态启用/禁用该返回键拦截回调
        drawerLayout.addDrawerListener(object : androidx.drawerlayout.widget.DrawerLayout.SimpleDrawerListener() {
            override fun onDrawerOpened(drawerView: android.view.View) {
                // 侧边栏打开时：启用拦截器，此时按下返回键会执行关闭侧边栏
                onBackPressedCallback.isEnabled = true
            }

            override fun onDrawerClosed(drawerView: android.view.View) {
                // 侧边栏完全关闭时：禁用拦截器，此时按下返回键会执行系统默认行为（退出应用）
                onBackPressedCallback.isEnabled = false
            }
        })

        val menu = navigationView.menu
        if (role == 0) {
            // 管理员：显示大仓、管理、审核；隐藏工人抢单、工人进行中
            menu.findItem(R.id.menu_warehouse).isVisible = true
            menu.findItem(R.id.menu_admin_orders).isVisible = true
            menu.findItem(R.id.menu_admin_audit).isVisible = true
            menu.findItem(R.id.menu_tasks).isVisible = false
            menu.findItem(R.id.menu_worker_active).isVisible = false
        } else {
            // 工人：显示大仓、抢单、进行中；隐藏管理员项目
            menu.findItem(R.id.menu_warehouse).isVisible = true
            menu.findItem(R.id.menu_admin_orders).isVisible = false
            menu.findItem(R.id.menu_admin_audit).isVisible = false
            menu.findItem(R.id.menu_tasks).isVisible = true
            menu.findItem(R.id.menu_worker_active).isVisible = true
        }

    }

    override fun onNavigationItemSelected(item: MenuItem): Boolean {
        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
        val role = sp.getInt("role", -1)

        when (item.itemId) {
            R.id.menu_warehouse -> {
                // 1. 跳转到大仓库货架概览页面
                val intent = Intent(this, WarehouseActivity::class.java)
                startActivity(intent)
            }
            R.id.menu_tasks -> {
                if (role == 0) {
                    Toast.makeText(this, "管理员无需抢单！请使用管理后台派活", Toast.LENGTH_SHORT).show()
                } else {
                    // 2. 工人跳转到小仓任务抢单池页面
                    val intent = Intent(this, TaskPoolActivity::class.java)
                    startActivity(intent)
                }
            }
            R.id.menu_logout -> {
                sp.edit().clear().apply()
                startActivity(Intent(this, LoginActivity::class.java))
                finish()
            }
            R.id.menu_admin_orders -> {
                // 跳转到管理员订单管理页面
                startActivity(Intent(this, AdminOrdersActivity::class.java))
            }
            R.id.menu_admin_audit -> {
                // 跳转到管理员订单终审页面
                startActivity(Intent(this, AdminAuditActivity::class.java))
            }
            R.id.menu_worker_active -> {
                // 跳转到工人进行中订单列表页面
                startActivity(Intent(this, MyOrdersActivity::class.java))
            }
        }
        drawerLayout.closeDrawer(GravityCompat.START)
        return true
    }
}