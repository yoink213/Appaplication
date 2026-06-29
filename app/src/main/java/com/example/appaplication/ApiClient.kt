package com.example.appaplication

import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.*
import retrofit2.Response

// --- 数据模型 ---
data class OutboundReq(val locationId: Long, val skuId: Long, val qty: Int)
data class ResultMsg(val code: Int, val message: String)
data class Task(val id: Long, val taskNo: String, val locationCode: String, val skuId: Long, val requireQty: Int)

// --- API 接口 ---
interface ApiService {
    @POST("api/v1/small/outbound")
    suspend fun smallOutbound(@Body req: OutboundReq): Response<ResultMsg>

    @GET("api/v1/large/tasks/pending")
    suspend fun getPendingTasks(): Response<List<Task>>

    @POST("api/v1/large/task/finish/{taskId}")
    suspend fun finishTask(@Path("taskId") taskId: Long): Response<ResultMsg>
}

// --- Retrofit 实例 ---
object ApiClient {
    // 10.0.2.2 是 Android 模拟器访问电脑本地后端的固定 IP。如果连真机，换成电脑局域网 IP
    private const val BASE_URL = "http://10.0.2.2:8080/WmsBackend/"

    val api: ApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(ApiService::class.java)
    }
}