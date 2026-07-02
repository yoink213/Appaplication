// ⚠️ Spring Boot 版本 API 客户端
// BASE_URL 已更新为 Spring Boot 端口 8080
package com.example.appaplication

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// ==========================================================
// 1. 全局数据传输 DTO 实体定义
// ==========================================================

// 登录响应模型
data class LoginResponse(val code: Int, val message: String, val data: UserData?)

// 登录成功后的用户信息数据
data class UserData(val id: Int, val username: String, val role: Int, val realName: String, var balance: Double)

// 大仓货物项模型
data class BigWarehouseItem(val id: Int, val code: String, val name: String, val category: String, val quantity: Int, val price: Double, val location: String, val description: String)

// 统一的通用返回提示模型
data class ResultMsg(val code: Int, val message: String)

// 获取下一个货物编号的响应
data class NextCodeResp(val code: Int, val data: String)

// 派单/小仓任务统一模型
data class OrderItem(
    val id: Int,
    val orderNo: String,
    val smallItemCode: String,
    val bigWarehouseId: Int,
    val quantity: Int,
    val totalPrice: Double,
    val deadline: String,
    val status: Int,
    val itemName: String,
    val itemCodeLarge: String,
    val location: String
)

// 管理员派活请求载体
data class DispatchReq(
    val bigWarehouseId: Int,
    val quantity: Int,
    val totalPrice: Double,
    val deadline: String,
    val adminId: Int
)

// 订单修改请求载体
data class OrderUpdateReq(
    val orderId: Int,
    val totalPrice: Double,
    val deadline: String
)

// 添加货物请求载体
data class AddWarehouseItemReq(
    val code: String,
    val name: String,
    val category: String,
    val quantity: Int,
    val price: Double,
    val location: String,
    val description: String
)

// 数据看板 - 概览统计
data class DashboardSummary(
    val totalItemTypes: Int,
    val totalStock: Int,
    val totalStockValue: Double,
    val todayOrders: Int,
    val pendingOrders: Int,
    val completedOrders: Int,
    val lowStockCount: Int,
    val workerCount: Int,
    val deliveryRate: Double
)

// 数据看板 - 分类统计
data class CategoryStats(
    val category: String,
    val totalQuantity: Int,
    val itemCount: Int
)

// 数据看板 - 每日订单趋势
data class DailyOrderStats(
    val date: String,
    val orderCount: Int
)

// 智能补货建议
data class ReplenishSuggestion(
    val id: Int,
    val code: String,
    val name: String,
    val category: String,
    val currentQuantity: Int,
    val suggestedQuantity: Int,
    val estimatedCost: Double,
    val reason: String
)

// 补货建议响应
data class ReplenishResp(val code: Int, val data: List<ReplenishSuggestion>?, val message: String?)


// ==========================================================
// 2. 核心网络 API 请求契约接口
// ==========================================================
interface ApiService {

    // [A] 用户登录
    @POST("api/login")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    // [B] 大仓：查询大仓所有的实时库存
    @GET("api/warehouse")
    suspend fun getWarehouseItems(): Response<List<BigWarehouseItem>>

    // [B2] 获取下一个货物编号
    @GET("api/warehouse/nextCode")
    suspend fun getNextCode(): Response<NextCodeResp>

    // [C] 管理员：创建派单指拨任务 (扣减大仓 -> 流入小仓)
    @POST("api/warehouse")
    suspend fun dispatchTask(@Body req: DispatchReq): Response<ResultMsg>

    // [D] 订单查询 (工人：获取小仓任务池)
    @GET("api/orders")
    suspend fun getPendingOrders(@Query("role") role: Int): Response<List<OrderItem>>

    // [E] 订单查询 (工人：获取自己进行中的任务列表)
    @GET("api/orders")
    suspend fun getWorkerActiveOrders(
        @Query("role") role: Int,
        @Query("workerId") workerId: Int
    ): Response<List<OrderItem>>

    // [F] 工人：配送三阶段流转 (action 可传入 "accept", "pickup", "deliver")
    @POST("api/worker/action")
    suspend fun executeWorkerAction(
        @Query("action") action: String,
        @Query("orderId") orderId: Int,
        @Query("workerId") workerId: Int
    ): Response<ResultMsg>

    // [G] 管理员：终审结算订单 (action 传入 "ACCEPT" 确认同意, "REJECT" 拒绝打回)
    @POST("api/admin/audit")
    suspend fun auditOrder(
        @Query("orderId") orderId: Int,
        @Query("action") action: String
    ): Response<ResultMsg>

    // [H] 修改订单
    @POST("api/orders/update")
    suspend fun updateOrder(@Body body: OrderUpdateReq): Response<ResultMsg>

    // [I] 删除/撤销订单
    @POST("api/orders/delete")
    suspend fun deleteOrder(@Query("orderId") orderId: Int): Response<ResultMsg>

    // [J] 添加新货物
    @POST("api/warehouse/add")
    suspend fun addWarehouseItem(@Body body: AddWarehouseItemReq): Response<ResultMsg>

    // [K] 数据看板 - 概览
    @GET("api/stats/summary")
    suspend fun getStatsSummary(): Response<DashboardSummary>

    // [L] 数据看板 - 分类库存分布
    @GET("api/stats/category")
    suspend fun getCategoryStats(): Response<List<CategoryStats>>

    // [M] 数据看板 - 近7天订单趋势
    @GET("api/stats/trend")
    suspend fun getOrderTrend(@Query("days") days: Int): Response<List<DailyOrderStats>>

    // [N] 数据看板 - 低库存预警
    @GET("api/stats/lowStock")
    suspend fun getLowStockItems(): Response<List<BigWarehouseItem>>

    // [O] 智能补货建议
    @GET("api/stats/replenish")
    suspend fun getReplenishSuggestions(): Response<ReplenishResp>
}


// ==========================================================
// 3. Retrofit 网络请求实例单例
// ==========================================================
object ApiClient {
    // ⚠️ Spring Boot 版本的 BASE_URL
    // 传统模拟器: 10.0.2.2 表示宿主机的 localhost
    // 真机调试: 需要改为电脑的实际局域网 IP 地址
    private const val BASE_URL = "http://10.0.2.2:8080/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}
