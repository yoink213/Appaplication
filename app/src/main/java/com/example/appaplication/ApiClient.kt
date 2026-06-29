package com.example.appaplication

import retrofit2.Response
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*

// 1. 数据传递对象定义
data class LoginResponse(val code: Int, val message: String, val data: UserData?)
data class UserData(val id: Int, val username: String, val role: Int, val realName: String, var balance: Double)
data class BigWarehouseItem(val id: Int, val code: String, val name: String, val category: String, val quantity: Int, val price: Double, val location: String, val description: String)
data class ResultMsg(val code: Int, val message: String)

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

data class DispatchReq(
    val bigWarehouseId: Int,
    val quantity: Int,
    val totalPrice: Double,
    val deadline: String,
    val adminId: Int
)

// 2. Retrofit 契约契合学校 Tomcat 部署路径
interface ApiService {
    @POST("api/login")
    suspend fun login(@Body body: Map<String, String>): Response<LoginResponse>

    @GET("api/warehouse")
    suspend fun getWarehouseItems(): Response<List<BigWarehouseItem>>

    @GET("api/orders")
    suspend fun getPendingOrders(@Query("role") role: Int): Response<List<OrderItem>>

    @PUT("api/orders")
    suspend fun updateOrder(@Body body: Map<String, Any>): Response<ResultMsg>

    @DELETE("api/orders")
    suspend fun deleteOrder(@Query("orderId") orderId: Int): Response<ResultMsg>

    @POST("api/warehouse")
    suspend fun dispatchTask(@Body req: DispatchReq): Response<ResultMsg>

    @POST("api/worker/action")
    suspend fun executeWorkerAction(
        @Query("action") action: String,
        @Query("orderId") orderId: Int,
        @Query("workerId") workerId: Int
    ): Response<ResultMsg>
}

// 3. 网络单例
object ApiClient {
    private const val BASE_URL = "http://10.0.2.2:8080/WmsBackend/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}

