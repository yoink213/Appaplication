// ⚠️ 记得保留或修改成你自己的包名
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


    @PUT("api/orders")
    suspend fun updateOrder(@Body body: Map<String, Any>): Response<ResultMsg>

    @DELETE("api/orders")
    suspend fun deleteOrder(@Query("orderId") orderId: Int): Response<ResultMsg>
}


// ==========================================================
// 3. Retrofit 网络请求实例单例
// ==========================================================
object ApiClient {
    // 如果你 Eclipse 的项目名字（Context Root）叫 softwareproject
    private const val BASE_URL = "http://10.0.2.2:8080/WmsBackend/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}