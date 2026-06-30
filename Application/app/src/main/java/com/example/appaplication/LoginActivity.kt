package com.example.appaplication

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.widget.Button
import android.widget.EditText
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import kotlinx.coroutines.launch

class LoginActivity : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_login)

        val etUser = findViewById<EditText>(R.id.etUser)
        val etPass = findViewById<EditText>(R.id.etPass)
        val btnLogin = findViewById<Button>(R.id.btnLogin)

        btnLogin.setOnClickListener {
            val username = etUser.text.toString().trim()
            val password = etPass.text.toString().trim()

            if (username.isEmpty() || password.isEmpty()) {
                Toast.makeText(this, "请输入账号或密码", Toast.LENGTH_SHORT).show()
                return@setOnClickListener
            }

            lifecycleScope.launch {
                try {
                    val map = mapOf("username" to username, "password" to password)
                    val response = ApiClient.api.login(map)

                    if (response.isSuccessful && response.body() != null) {
                        val loginResponse = response.body()!!
                        val user = loginResponse.data!!

                        val sp = getSharedPreferences("user_session", Context.MODE_PRIVATE)
                        sp.edit().apply {
                            putInt("id", user.id)
                            putString("username", user.username)
                            putInt("role", user.role)
                            putString("realName", user.realName)
                            putFloat("balance", user.balance.toFloat())
                            apply()
                        }

                        Toast.makeText(this@LoginActivity, loginResponse.message, Toast.LENGTH_SHORT).show()
                        startActivity(Intent(this@LoginActivity, MainActivity::class.java))
                        finish()
                    } else {
                        // 🔍【新增：根据状态码精准弹窗提示】
                        val code = response.code()
                        if (code == 401) {
                            Toast.makeText(this@LoginActivity, "❌ 用户名或密码错误，请检查！", Toast.LENGTH_SHORT).show()
                        } else if (code == 500) {
                            Toast.makeText(this@LoginActivity, "💥 服务器数据库连接失败！请去 Eclipse 控制台查看红字报错", Toast.LENGTH_LONG).show()
                        } else {
                            Toast.makeText(this@LoginActivity, "未知服务器错误，代码: $code", Toast.LENGTH_SHORT).show()
                        }
                    }
                } catch (e: Exception) {
                    Toast.makeText(this@LoginActivity, "⚠️ 网络错误: 无法连通 Tomcat，请确认连接地址与 IP", Toast.LENGTH_LONG).show()
                }
            }
        }
    }
}